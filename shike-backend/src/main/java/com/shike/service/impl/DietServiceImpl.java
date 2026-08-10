package com.shike.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shike.common.BizException;
import com.shike.model.dto.MonthDashboardDTO;
import com.shike.model.dto.MonthSummaryDTO;
import com.shike.model.dto.WeekDashboardDTO;
import com.shike.model.entity.DietRecord;
import com.shike.model.entity.ExerciseRecord;
import com.shike.model.entity.User;
import com.shike.model.entity.PointsRecord;
import com.shike.repository.DietRecordRepository;
import com.shike.repository.ExerciseRecordRepository;
import com.shike.repository.UserRepository;
import com.shike.repository.PointsRecordRepository;
import com.shike.service.DietService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DietServiceImpl implements DietService {

    private final DietRecordRepository dietRecordRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final UserRepository userRepository;
    private final PointsRecordRepository pointsRecordRepository;
    private final com.shike.repository.WeightRecordRepository weightRecordRepository;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final com.shike.service.AdminService adminService;

    @Value("${ai.provider:OPENAI}")
    private String aiProvider;

    @Value("${ai.api-key:MOCK_KEY}")
    private String aiApiKey;

    @Value("${ai.endpoint:https://api.openai.com/v1/chat/completions}")
    private String aiEndpoint;

    @Value("${ai.diet-model:${ai.model:qwen3.6-plus}}")
    private String aiModel;

    @Value("${ai.timeout-ms:120000}")
    private Integer aiTimeoutMs;

    @Value("${baidu.api-key:}")
    private String baiduApiKey;

    @Value("${baidu.secret-key:}")
    private String baiduSecretKey;

    private String baiduAccessToken = null;
    private long baiduTokenExpiryTime = 0;

    @Override
    @Transactional
    public DietRecord recognizeMeal(MultipartFile file, String hint, Long userId) {
        java.util.Map<String, Boolean> toggles = adminService.getPublicFeatureToggles("release");
        if (toggles != null && Boolean.FALSE.equals(toggles.get("photo_recognize"))) {
            throw new BizException(400, "AI 拍照识图算卡功能暂未开放或在维护中");
        }

        if (file.isEmpty()) {
            throw new BizException(400, "Uploaded file cannot be empty");
        }
        
        checkDailyAiLimit(userId);
        checkPointsBalance(userId);
        
        log.info("Received image for AI recognition: {}, size: {} bytes, hint: {}", file.getOriginalFilename(), file.getSize(), hint);

        if ("BAIDU".equalsIgnoreCase(aiProvider)) {
            log.info("Using Baidu Food Recognition API...");
            try {
                // 1. Compress and encode image
                byte[] fileBytes = compressImage(file);
                String base64Data = java.util.Base64.getEncoder().encodeToString(fileBytes);
                String requestBody = "image=" + java.net.URLEncoder.encode(base64Data, java.nio.charset.StandardCharsets.UTF_8);

                // 2. Get Access Token
                String token = getBaiduAccessToken();

                java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                        .version(java.net.http.HttpClient.Version.HTTP_1_1)
                        .connectTimeout(java.time.Duration.ofMillis(aiTimeoutMs))
                        .build();

                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("https://aip.baidubce.com/rest/2.0/image-classify/v2/dish?access_token=" + token))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Connection", "close")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody, java.nio.charset.StandardCharsets.UTF_8))
                        .timeout(java.time.Duration.ofMillis(aiTimeoutMs))
                        .build();

                java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Baidu API returned status code " + response.statusCode() + ": " + response.body());
                }

                com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(response.body());
                if (rootNode.has("error_code")) {
                    throw new RuntimeException("Baidu API Error: " + rootNode.path("error_msg").asText());
                }

                com.fasterxml.jackson.databind.JsonNode results = rootNode.path("result");
                if (results.isMissingNode() || results.size() == 0) {
                    throw new BizException(400, "图片中未识别到食物");
                }

                // Get top candidate
                String dishName = results.get(0).path("name").asText();
                double caloriePer100g = results.get(0).path("calorie").asDouble(0.0);
                double probability = results.get(0).path("probability").asDouble(0.0);
                log.info("Baidu recognized food: {} with probability {}, caloriePer100g: {}", dishName, probability, caloriePer100g);

                if (probability < 0.05 || "非食物".equals(dishName) || "其他".equals(dishName)) {
                    throw new BizException(400, "图片中未识别到食物");
                }

                String cleanJson = "";

                // 4. Estimate nutrition details using text AI if available (and if not mocked)
                try {
                    if (aiApiKey != null && !aiApiKey.trim().isEmpty() && !"MOCK_KEY".equals(aiApiKey) && !"MOCK".equalsIgnoreCase(aiProvider)) {
                        log.info("Calling text AI ({}) to estimate nutrition for: {}", aiProvider, dishName);
                        String textPrompt = "你是一个专业的膳食营养估算模型。用户通过图片识别出吃了一餐：【" + dishName + "】。";
                        if (hint != null && !hint.trim().isEmpty()) {
                            textPrompt += "用户补充提示是：\"" + hint.trim() + "\"。";
                        }
                        textPrompt += "\n请进行以下处理：\n" +
                                "1. 判断【" + dishName + "】是否是食物或饮料。如果它明显不是食物（例如属于电子产品、人物、风景、车辆、普通物件等），请必须返回空数组 []。\n" +
                                "2. 如果是食物，按中国一人份常规分量估算重量（例如：一碗米饭约200g，一份炒菜约200-300g）。\n" +
                                "3. 根据估算重量计算营养数据，并用交叉校验公式检查：calories 应约等于 protein*4 + fat*9 + carbs*4（允许±15%误差）。\n" +
                                "4. 炒菜请计入 5-15g 烹饪用油的脂肪和热量。\n" +
                                "请只返回一个 JSON 数组（不要包含 markdown 代码块标记，不要包含额外文字），格式严格为：\n" +
                                "[{\"name\": \"" + dishName + "\", \"weight\": 估算克数, \"calories\": 估算热量kcal, \"protein\": 蛋白质克数, \"fat\": 脂肪克数, \"carbs\": 碳水化合物克数}]";

                        java.util.Map<String, Object> textMessage = java.util.Map.of(
                                "role", "user",
                                "content", textPrompt
                        );
                        java.util.Map<String, Object> payload = java.util.Map.of(
                                "model", aiModel,
                                "messages", java.util.List.of(textMessage),
                                "temperature", 0.2
                        );
                        String requestJson = objectMapper.writeValueAsString(payload);

                        java.net.http.HttpClient textHttpClient = java.net.http.HttpClient.newBuilder()
                                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                                .connectTimeout(java.time.Duration.ofMillis(aiTimeoutMs))
                                .build();

                        java.net.http.HttpRequest textRequest = java.net.http.HttpRequest.newBuilder()
                                .uri(java.net.URI.create(aiEndpoint))
                                .header("Content-Type", "application/json")
                                .header("Authorization", "Bearer " + aiApiKey)
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                                .header("Connection", "close")
                                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestJson, java.nio.charset.StandardCharsets.UTF_8))
                                .timeout(java.time.Duration.ofMillis(aiTimeoutMs))
                                .build();

                        java.net.http.HttpResponse<String> textResponse = textHttpClient.send(textRequest, java.net.http.HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
                        if (textResponse.statusCode() == 200) {
                            com.fasterxml.jackson.databind.JsonNode textRootNode = objectMapper.readTree(textResponse.body());
                            com.fasterxml.jackson.databind.JsonNode usageNode = textRootNode.path("usage");
                            if (!usageNode.isMissingNode()) {
                                int promptTok = usageNode.path("prompt_tokens").asInt(0);
                                int compTok = usageNode.path("completion_tokens").asInt(0);
                                int totalTok = usageNode.path("total_tokens").asInt(0);
                                log.info("[AI Token Audit] Module: [膳食拍照/估算] | Model: {} | Prompt Tokens: {} | Completion Tokens: {} | Total Tokens: {}",
                                        aiModel, promptTok, compTok, totalTok);
                            }

                            String content = textRootNode.path("choices").path(0).path("message").path("content").asText().trim();
                            int startIdx = content.indexOf('[');
                            int endIdx = content.lastIndexOf(']');
                            if (startIdx != -1 && endIdx != -1 && startIdx < endIdx) {
                                cleanJson = content.substring(startIdx, endIdx + 1).trim();
                            } else {
                                if (content.startsWith("```")) {
                                    content = content.replaceAll("^```[a-zA-Z]*\\s*", "");
                                    content = content.replaceAll("\\s*```$", "");
                                }
                                cleanJson = content.trim();
                            }
                        } else {
                            log.warn("Text AI API returned non-200 status code: {}, response: {}", textResponse.statusCode(), textResponse.body());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Text AI estimation failed, will fallback to local formula: {}", e.getMessage());
                }
                if (cleanJson != null && !cleanJson.isEmpty()) {
                    cleanJson = cleanJson.replaceAll("//.*", "").trim();
                }

                // 5. Fallback to mathematical estimation if text AI was not used or failed
                if (cleanJson.isEmpty() || "[]".equals(cleanJson)) {
                    if ("[]".equals(cleanJson)) {
                        log.info("Text AI filtered this out as non-food: {}", dishName);
                        throw new BizException(400, "图片中未识别到食物");
                    }
                    log.info("Using local fallback formula for: {}", dishName);
                    cleanJson = getFallbackNutritionJson(dishName, caloriePer100g);
                }

                FoodItem[] items = objectMapper.readValue(cleanJson, FoodItem[].class);
                if (items == null || items.length == 0) {
                    throw new BizException(400, "图片中未识别到食物");
                }

                BigDecimal calories = BigDecimal.ZERO;
                BigDecimal protein = BigDecimal.ZERO;
                BigDecimal fat = BigDecimal.ZERO;
                BigDecimal carbs = BigDecimal.ZERO;

                for (FoodItem item : items) {
                    if (item.getCalories() != null) calories = calories.add(BigDecimal.valueOf(item.getCalories()));
                    if (item.getProtein() != null) protein = protein.add(BigDecimal.valueOf(item.getProtein()));
                    if (item.getFat() != null) fat = fat.add(BigDecimal.valueOf(item.getFat()));
                    if (item.getCarbs() != null) carbs = carbs.add(BigDecimal.valueOf(item.getCarbs()));
                }

                DietRecord resultRecord = DietRecord.builder()
                        .foodItems(cleanJson)
                        .totalCalories(calories)
                        .totalProtein(protein)
                        .totalFat(fat)
                        .totalCarbs(carbs)
                        .imageUrl("https://images.example.com/meals/lunch.jpg")
                        .build();
                incrementDailyAiCount(userId);
            deductPointsForAi(userId);
                return resultRecord;

            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                log.error("Baidu recognition flow failed", e);
                throw new BizException(500, "AI 识别失败，请重新拍摄清晰的食物照片");
            }
        }

        // Default mock diet record to fall back to in case of errors
        String defaultMockFoodItems = "[" +
                "{\"name\": \"香煎鸡胸肉\", \"weight\": 150, \"calories\": 250.0, \"protein\": 30.0, \"fat\": 5.0, \"carbs\": 0.0}, " +
                "{\"name\": \"水煮西蓝花\", \"weight\": 100, \"calories\": 35.0, \"protein\": 3.0, \"fat\": 0.5, \"carbs\": 7.0}, " +
                "{\"name\": \"糙米饭\", \"weight\": 120, \"calories\": 165.0, \"protein\": 5.0, \"fat\": 1.0, \"carbs\": 35.0}" +
                "]";
        DietRecord fallbackRecord = DietRecord.builder()
                .foodItems(defaultMockFoodItems)
                .totalCalories(BigDecimal.valueOf(450.0))
                .totalProtein(BigDecimal.valueOf(38.0))
                .totalFat(BigDecimal.valueOf(6.5))
                .totalCarbs(BigDecimal.valueOf(42.0))
                .imageUrl("https://mock-image-url.com/chicken_salad.jpg")
                .build();

        if ("MOCK".equalsIgnoreCase(aiProvider) || "MOCK_KEY".equals(aiApiKey) || aiApiKey == null || aiApiKey.trim().isEmpty()) {
            log.info("Running in MOCK mode for AI recognition (provider: {}, api key is mock/empty)", aiProvider);
            deductPointsForAi(userId);
            return fallbackRecord;
        }

        try {
            // 1. Convert MultipartFile to Base64 (use original bytes directly to keep full resolution for text reading)
            byte[] fileBytes = file.getBytes();
            String base64Data = java.util.Base64.getEncoder().encodeToString(fileBytes);
            String mimeType = file.getContentType();
            if (mimeType == null || mimeType.isEmpty()) {
                mimeType = "image/jpeg";
            }
            String dataUrl = "data:" + mimeType + ";base64," + base64Data;

            String prompt = "你是一个顶级的中国膳食营养视觉分析师。请严格按照以下步骤分析用户上传的食物照片。\n" +
                    "\n" +
                    "## 分析步骤\n" +
                    "\n" +
                    "### 第一步：逐一识别所有食物\n" +
                    "仔细扫描整张图片，识别画面中出现的**每一种独立的食物或饮品**，包括主食、配菜、汤品、饮料、水果等，不要遗漏任何一种。\n" +
                    "- 食物命名尽量准确具体（例如写 '尖椒肥肠' 而非 '炒肉'，写 '番茄炒蛋' 而非 '炒菜'）。\n" +
                    "- 区分易混淆食材：\n" +
                    "  * 肥肠：中空圈状/管状/褶皱状，表面有油脂感；鸡肉：实心块状，有纤维纹理；猪肉：多为片状或丝状。\n" +
                    "  * 土豆丁 vs 豆腐块 vs 年糕：注意颜色、光泽和切面质感差异。\n" +
                    "- 注意分析烹饪方式（炒、煮、蒸、炸、凉拌）和酱汁（红烧酱、辣椒油、蚝油等），这些会显著影响热量。\n" +
                    "\n" +
                    "### 第二步：体积与重量估算（关键！）\n" +
                    "这是最影响准确度的环节，请特别认真：\n" +
                    "- **寻找参照物**：在图片中寻找碗、盘子、筷子、勺子、手、杯子等可以作为尺寸参照的物体。\n" +
                    "  * 标准家用饭碗（直径约 12cm），装满约 200g 米饭。\n" +
                    "  * 标准家用菜盘（直径约 20-25cm）。\n" +
                    "  * 一双筷子长约 24cm，可以用来估算食物堆叠的厚度和面积。\n" +
                    "- **根据参照物推算面积和厚度**，从而估算每种食物的体积和重量（克数）。\n" +
                    "- 如果没有明确参照物，则按中国餐饮的**一人份常规分量**估算（例如：一碗米饭约 200g，一份炒菜约 200-300g，一份汤面约 500g）。\n" +
                    "\n" +
                    "### 第三步：计算营养数据并交叉校验\n" +
                    "- 根据第二步估算的重量（克数），结合食物的营养成分密度（每100g含多少热量/蛋白质/脂肪/碳水），计算每种食物的营养数据。\n" +
                    "- **交叉校验公式**：确保每种食物的 calories 约等于 protein*4 + fat*9 + carbs*4（允许正负15%误差），如果差异过大，请调整数值使其一致。\n" +
                    "- 烹饪用油：炒菜通常额外增加 5-15g 食用油（约 45-135kcal），请合理计入该菜品的 fat 和 calories 中。\n" +
                    "\n" +
                    "## 输出要求\n" +
                    "你必须且只能返回一个标准 JSON 数组，不要包含任何 markdown 代码块标记（禁止 ```json 或 ```），不要包含任何额外文字或解释。\n" +
                    "JSON 数组中每个对象的字段如下：\n" +
                    "- name: 食物名称（中文，尽量准确具体）\n" +
                    "- weight: 估算重量（整数，单位克）\n" +
                    "- calories: 估算热量（数值，单位 kcal）\n" +
                    "- protein: 蛋白质（数值，单位克）\n" +
                    "- fat: 脂肪（数值，单位克）\n" +
                    "- carbs: 碳水化合物（数值，单位克）\n" +
                    "\n" +
                    "示例输出：\n" +
                    "[{\"name\": \"番茄炒蛋\", \"weight\": 250, \"calories\": 220, \"protein\": 12, \"fat\": 14, \"carbs\": 10}, {\"name\": \"白米饭\", \"weight\": 200, \"calories\": 232, \"protein\": 5, \"fat\": 0.6, \"carbs\": 52}]\n" +
                    "\n" +
                    "如果图片中完全没有任何食物或饮品，必须且只能返回空数组 []。";

            if (hint != null && !hint.trim().isEmpty()) {
                prompt += "\n\n【用户补充提示：\"" + hint.trim() + "\"。请务必优先结合此提示词对图片中的食物进行正名和精准识别。如果提示词中提到了某些食物或饮品（例如：一杯牛奶、生椰拿铁、可乐等），即使在图片中看不清或不明显，你也必须将其作为独立食材项加入 JSON 数组，并根据常识合理估算其重量与营养素。绝对不能漏掉用户补充提示中提到的任何食物或饮品！】\n";
            }

            String responseBody = "";

            // 3. Make HTTP request based on Provider
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                    .version(java.net.http.HttpClient.Version.HTTP_1_1)
                    .connectTimeout(java.time.Duration.ofMillis(aiTimeoutMs))
                    .build();

            if ("GEMINI".equalsIgnoreCase(aiProvider)) {
                log.info("Calling Gemini vision API, model: {}", aiModel);
                String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + aiModel + ":generateContent?key=" + aiApiKey;
                
                java.util.Map<String, Object> inlineData = java.util.Map.of(
                        "mimeType", mimeType,
                        "data", base64Data
                );
                java.util.Map<String, Object> partText = java.util.Map.of("text", prompt);
                java.util.Map<String, Object> partImage = java.util.Map.of("inlineData", inlineData);
                java.util.Map<String, Object> content = java.util.Map.of("parts", java.util.List.of(partText, partImage));
                java.util.Map<String, Object> payload = java.util.Map.of("contents", java.util.List.of(content));
                
                String requestJson = objectMapper.writeValueAsString(payload);

                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(geminiUrl))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Connection", "close")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestJson))
                        .timeout(java.time.Duration.ofMillis(aiTimeoutMs))
                        .build();

                java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Gemini API returned status code " + response.statusCode() + ": " + response.body());
                }
                
                com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(response.body());
                com.fasterxml.jackson.databind.JsonNode textNode = rootNode.path("candidates").path(0)
                        .path("content").path("parts").path(0).path("text");
                if (textNode.isMissingNode()) {
                    throw new RuntimeException("Failed to locate text content in Gemini response: " + response.body());
                }
                responseBody = textNode.asText();
            } else {
                log.info("Calling OpenAI compatible vision API: {}, base model: {}", aiEndpoint, aiModel);
                String visionModel = aiModel;
                if (aiModel.contains("qwen") && !aiModel.contains("vl")) {
                    visionModel = "qwen-vl-max";
                }
                log.info("Using Vision LLM model for image analysis: {}", visionModel);
                try {
                    responseBody = callOpenAiVision(visionModel, prompt, dataUrl, mimeType);
                } catch (Exception e) {
                    String fallbackModel = "qwen-vl-plus";
                    log.warn("Primary vision model {} failed: {}. Falling back to stable model {}...", visionModel, e.getMessage(), fallbackModel);
                    try {
                        responseBody = callOpenAiVision(fallbackModel, prompt, dataUrl, mimeType);
                    } catch (Exception ex) {
                        log.error("Fallback vision model " + fallbackModel + " also failed", ex);
                        throw ex;
                    }
                }
            }

            log.debug("Raw AI Vision Response text: {}", responseBody);

            if (responseBody == null || responseBody.trim().isEmpty()) {
                log.info("AI response body is empty or null");
                throw new BizException(400, "图片中未识别到食物");
            }

            String cleanJson = responseBody.trim();
            if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.replaceAll("^```[a-zA-Z]*\\s*", "");
                cleanJson = cleanJson.replaceAll("\\s*```$", "");
            }
            cleanJson = cleanJson.trim();
            // Remove double-slash comments (//...) often generated by AI to prevent JSON parsing errors
            cleanJson = cleanJson.replaceAll("//.*", "").trim();

            FoodItem[] items = null;
            try {
                com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(cleanJson);
                com.fasterxml.jackson.databind.JsonNode itemsNode = rootNode;
                
                if (rootNode.isObject()) {
                    if (rootNode.has("foodItems")) {
                        itemsNode = rootNode.path("foodItems");
                    } else if (rootNode.has("items")) {
                        itemsNode = rootNode.path("items");
                    }
                }
                
                if (itemsNode.isArray()) {
                    items = objectMapper.treeToValue(itemsNode, FoodItem[].class);
                } else if (itemsNode.isObject()) {
                    FoodItem singleItem = objectMapper.treeToValue(itemsNode, FoodItem.class);
                    if (singleItem != null) {
                        items = new FoodItem[]{singleItem};
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse AI response using JsonNode tree: {}. Attempting regex fallback.", cleanJson, e);
                try {
                    int startIdx = cleanJson.indexOf('[');
                    int endIdx = cleanJson.lastIndexOf(']');
                    if (startIdx != -1 && endIdx != -1 && startIdx < endIdx) {
                        String arrayJson = cleanJson.substring(startIdx, endIdx + 1).trim();
                        items = objectMapper.readValue(arrayJson, FoodItem[].class);
                    }
                } catch (Exception ex) {
                    log.error("Regex/Substring fallback also failed", ex);
                }
            }

            // 如果解析后的列表为空，或未识别出有效菜品名称，则抛出未识别到食物异常
            if (items == null || items.length == 0 || items[0] == null || items[0].getName() == null || items[0].getName().trim().isEmpty() || "非食物".equals(items[0].getName()) || "其他".equals(items[0].getName())) {
                log.info("AI returned empty array or invalid food items - no food detected in image");
                throw new BizException(400, "图片中未识别到食物");
            }
            
            String finalFoodItemsJson = objectMapper.writeValueAsString(items);
            
            // Post-process to merge hint if missing
            finalFoodItemsJson = mergeHintIfMissing(hint, finalFoodItemsJson);
            
            // Re-parse the items array after merging to recalculate total nutrients
            try {
                FoodItem[] mergedItems = objectMapper.readValue(finalFoodItemsJson, FoodItem[].class);
                if (mergedItems != null && mergedItems.length > 0) {
                    items = mergedItems;
                }
            } catch (Exception e) {
                log.warn("Failed to parse merged food items JSON: {}, keeping original items", finalFoodItemsJson);
            }

            BigDecimal calories = BigDecimal.ZERO;
            BigDecimal protein = BigDecimal.ZERO;
            BigDecimal fat = BigDecimal.ZERO;
            BigDecimal carbs = BigDecimal.ZERO;

            for (FoodItem item : items) {
                if (item.getCalories() != null) calories = calories.add(BigDecimal.valueOf(item.getCalories()));
                if (item.getProtein() != null) protein = protein.add(BigDecimal.valueOf(item.getProtein()));
                if (item.getFat() != null) fat = fat.add(BigDecimal.valueOf(item.getFat()));
                if (item.getCarbs() != null) carbs = carbs.add(BigDecimal.valueOf(item.getCarbs()));
            }

            log.info("AI Recognition success! Food items parsed: {}, calories: {} kcal", items.length, calories);

            DietRecord resultRecord = DietRecord.builder()
                    .foodItems(finalFoodItemsJson)
                    .totalCalories(calories)
                    .totalProtein(protein)
                    .totalFat(fat)
                    .totalCarbs(carbs)
                    .imageUrl("https://images.example.com/meals/lunch.jpg")
                    .build();
            deductPointsForAi(userId);
            return resultRecord;

        } catch (BizException e) {
            // 业务异常（如未识别到食物）直接向上抛，不走 fallback
            throw e;
        } catch (Exception e) {
            log.error("AI Visual recognition failed or parsed incorrectly.", e);
            throw new BizException(500, "AI 识别失败，请重新拍摄清晰的食物照片");
        }
    }

    private byte[] compressImage(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            BufferedImage originalImage = ImageIO.read(is);
            if (originalImage == null) {
                log.warn("Failed to read image via ImageIO, using original bytes");
                return file.getBytes();
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            
            // 智能限制最大分辨率为 1024px，既保留了超高清细节，又防范了超大文件导致的传输延迟和超时
            int maxDimension = 1024;
            if (originalWidth <= maxDimension && originalHeight <= maxDimension) {
                return file.getBytes();
            }

            int targetWidth = originalWidth;
            int targetHeight = originalHeight;
            if (originalWidth > originalHeight) {
                targetWidth = maxDimension;
                targetHeight = (int) (originalHeight * ((double) maxDimension / originalWidth));
            } else {
                targetHeight = maxDimension;
                targetWidth = (int) (originalWidth * ((double) maxDimension / originalHeight));
            }

            log.info("Resizing image from {}x{} to {}x{}", originalWidth, originalHeight, targetWidth, targetHeight);
            
            BufferedImage resizedImage = new BufferedImage(
                    targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            
            Graphics2D g2d = resizedImage.createGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, targetWidth, targetHeight);
            g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "jpg", baos);
            byte[] compressedBytes = baos.toByteArray();
            log.info("Image compressed from {} to {} bytes", file.getSize(), compressedBytes.length);
            return compressedBytes;
        } catch (Exception e) {
            log.warn("Image compression failed: {}, returning original bytes", e.getMessage());
            try {
                return file.getBytes();
            } catch (IOException ioe) {
                throw new BizException(500, "Failed to read original image bytes");
            }
        }
    }

    private String getFallbackNutritionJson(String dishName, double caloriePer100g) {
        double defaultWeight = 150.0;
        double caloriePerG = (caloriePer100g > 0) ? caloriePer100g / 100.0 : 2.0;
        double calories = defaultWeight * caloriePerG;
        double protein = (calories * 0.15) / 4.0;
        double fat = (calories * 0.25) / 9.0;
        double carbs = (calories * 0.60) / 4.0;
        
        java.math.BigDecimal weightBD = java.math.BigDecimal.valueOf(defaultWeight).setScale(1, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal caloriesBD = java.math.BigDecimal.valueOf(calories).setScale(1, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal proteinBD = java.math.BigDecimal.valueOf(protein).setScale(1, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal fatBD = java.math.BigDecimal.valueOf(fat).setScale(1, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal carbsBD = java.math.BigDecimal.valueOf(carbs).setScale(1, java.math.RoundingMode.HALF_UP);
        
        return String.format(
            "[{\"name\":\"%s\",\"weight\":%s,\"calories\":%s,\"protein\":%s,\"fat\":%s,\"carbs\":%s}]",
            dishName, weightBD, caloriesBD, proteinBD, fatBD, carbsBD
        );
    }

    private String getBaiduAccessToken() {
        if (baiduAccessToken != null && System.currentTimeMillis() < baiduTokenExpiryTime) {
            return baiduAccessToken;
        }
        synchronized (this) {
            if (baiduAccessToken != null && System.currentTimeMillis() < baiduTokenExpiryTime) {
                return baiduAccessToken;
            }
            try {
                log.info("Fetching new Baidu Access Token...");
                String authUrl = "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=" 
                        + baiduApiKey + "&client_secret=" + baiduSecretKey;
                
                java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                        .version(java.net.http.HttpClient.Version.HTTP_1_1)
                        .build();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(authUrl))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Connection", "close")
                        .POST(java.net.http.HttpRequest.BodyPublishers.noBody())
                        .timeout(java.time.Duration.ofMillis(5000))
                        .build();
                        
                java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(response.body());
                    String token = rootNode.path("access_token").asText();
                    long expiresIn = rootNode.path("expires_in").asLong(2592000);
                    if (token != null && !token.isEmpty()) {
                        baiduAccessToken = token;
                        baiduTokenExpiryTime = System.currentTimeMillis() + (expiresIn - 60) * 1000;
                        log.info("Baidu Access Token fetched successfully.");
                        return baiduAccessToken;
                    }
                }
                throw new RuntimeException("Failed to get Baidu access token: " + response.body());
            } catch (Exception e) {
                log.error("Error fetching Baidu access token", e);
                throw new BizException(500, "Baidu 认证失败，请检查 API Key 和 Secret Key");
            }
        }
    }

    @Data
    public static class FoodItem {
        private String name;
        private Double weight;
        private Double calories;
        private Double protein;
        private Double fat;
        private Double carbs;
    }

    @Override
    @Transactional
    public DietRecord recordMeal(Long userId, String mealType, String foodItemsJson, String oilLevel, String imageUrl) {
        log.info("Recording meal for user: {}, type: {}, oil: {}", userId, mealType, oilLevel);
        
        BigDecimal calories = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        BigDecimal carbs = BigDecimal.ZERO;

        try {
            // Parse foodItemsJson: [{"name":"苹果", "weight":150, "calories":78, "protein":0.3, "fat":0.2, "carbs":20.6}]
            FoodItem[] items = objectMapper.readValue(foodItemsJson, FoodItem[].class);
            for (FoodItem item : items) {
                if (item.getCalories() != null) {
                    calories = calories.add(BigDecimal.valueOf(item.getCalories()));
                }
                if (item.getProtein() != null) {
                    protein = protein.add(BigDecimal.valueOf(item.getProtein()));
                }
                if (item.getFat() != null) {
                    fat = fat.add(BigDecimal.valueOf(item.getFat()));
                }
                if (item.getCarbs() != null) {
                    carbs = carbs.add(BigDecimal.valueOf(item.getCarbs()));
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse food items JSON, raw content: {}", foodItemsJson, e);
            throw new BizException(400, "Invalid food items format");
        }

        // Apply oil coefficient adjustment
        // LIGHT: -15% fat, MODERATE: 1.0, HEAVY: +20% fat
        if ("LIGHT".equalsIgnoreCase(oilLevel)) {
            fat = fat.multiply(BigDecimal.valueOf(0.85)).setScale(1, RoundingMode.HALF_UP);
            calories = calories.multiply(BigDecimal.valueOf(0.9)).setScale(1, RoundingMode.HALF_UP);
        } else if ("HEAVY".equalsIgnoreCase(oilLevel)) {
            fat = fat.multiply(BigDecimal.valueOf(1.2)).setScale(1, RoundingMode.HALF_UP);
            calories = calories.multiply(BigDecimal.valueOf(1.15)).setScale(1, RoundingMode.HALF_UP);
        } else {
            fat = fat.setScale(1, RoundingMode.HALF_UP);
            calories = calories.setScale(1, RoundingMode.HALF_UP);
        }
        protein = protein.setScale(1, RoundingMode.HALF_UP);
        carbs = carbs.setScale(1, RoundingMode.HALF_UP);

        DietRecord record = DietRecord.builder()
                .userId(userId)
                .recordDate(LocalDate.now())
                .mealType(mealType)
                .foodItems(foodItemsJson)
                .oilLevel(oilLevel)
                .totalCalories(calories)
                .totalProtein(protein)
                .totalFat(fat)
                .totalCarbs(carbs)
                .imageUrl(imageUrl)
                .build();

        return dietRecordRepository.save(record);
    }

    @Override
    public List<DietRecord> getDailyRecords(Long userId, LocalDate date) {
        return dietRecordRepository.findByUserIdAndRecordDate(userId, date);
    }

    @Override
    public List<MonthSummaryDTO> getMonthSummary(Long userId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        User user = userRepository.findById(userId).orElse(null);
        BigDecimal budget = (user != null && user.getTargetCalories() != null) 
                ? user.getTargetCalories() 
                : BigDecimal.valueOf(2000.0);

        List<DietRecord> records = dietRecordRepository.findByUserIdAndRecordDateBetween(userId, startDate, endDate);

        java.util.Map<LocalDate, BigDecimal> dailyCalories = records.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        DietRecord::getRecordDate,
                        java.util.stream.Collectors.reducing(
                                BigDecimal.ZERO,
                                DietRecord::getTotalCalories,
                                BigDecimal::add
                        )
                ));

        java.util.List<MonthSummaryDTO> summaryList = new java.util.ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            BigDecimal totalCal = dailyCalories.get(date);
            String status;
            if (totalCal != null) {
                BigDecimal maxAllowed = budget.multiply(BigDecimal.valueOf(1.25));
                if (totalCal.compareTo(maxAllowed) <= 0) {
                    status = "SUCCESS";
                } else {
                    status = "EXCEEDED";
                }
            } else {
                status = "EMPTY";
                totalCal = BigDecimal.ZERO;
            }
            summaryList.add(MonthSummaryDTO.builder()
                    .date(date)
                    .status(status)
                    .totalCalories(totalCal)
                    .targetCalories(budget)
                    .build());
        }
        return summaryList;
    }

    private void checkPointsBalance(Long userId) {
        if (userId == null) return;
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
            if (currentPoints < 5) {
                throw new BizException(400, "积分余额不足（需 5 积分，当前仅有 " + currentPoints + " 积分），请完成每日打卡或签到赚取积分！");
            }
        }
    }

    private void deductPointsForAi(Long userId) {
        if (userId == null) return;
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;
        int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
        user.setPoints(Math.max(0, currentPoints - 5));
        userRepository.save(user);
        
        PointsRecord record = PointsRecord.builder()
                .userId(userId)
                .amount(-5)
                .type("AI_RECOGNITION")
                .remark("AI食物热量识别")
                .build();
        pointsRecordRepository.save(record);
        log.info("Deducted 5 points from user {} for AI recognition. Remaining points: {}", userId, user.getPoints());
    }

    private String callOpenAiVision(String model, String prompt, String dataUrl, String mimeType) throws Exception {
        java.util.Map<String, Object> textPart = java.util.Map.of("type", "text", "text", prompt);
        java.util.Map<String, Object> imageUrlDetail = java.util.Map.of("url", dataUrl);
        java.util.Map<String, Object> imagePart = java.util.Map.of("type", "image_url", "image_url", imageUrlDetail);
        
        java.util.Map<String, Object> userMessage = java.util.Map.of(
                "role", "user",
                "content", java.util.List.of(textPart, imagePart)
        );
        
        java.util.Map<String, Object> payload;
        if (model.contains("glm-4.6v")) {
            payload = java.util.Map.of(
                    "model", model,
                    "messages", java.util.List.of(userMessage),
                    "temperature", 0.1,
                    "max_tokens", 2048,
                    "thinking", java.util.Map.of("type", "disabled")
            );
        } else if (model.contains("qwen")) {
            payload = java.util.Map.of(
                    "model", model,
                    "messages", java.util.List.of(userMessage),
                    "temperature", 0.7,
                    "top_p", 0.8,
                    "max_tokens", 2048,
                    "enable_thinking", true,
                    "thinking_budget", 4000
            );
        } else {
            payload = java.util.Map.of(
                    "model", model,
                    "messages", java.util.List.of(userMessage),
                    "temperature", 0.7,
                    "max_tokens", 1000
            );
        }

        String requestJson = objectMapper.writeValueAsString(payload);

        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .connectTimeout(java.time.Duration.ofMillis(aiTimeoutMs))
                .build();

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(aiEndpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiApiKey)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Connection", "close")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestJson, java.nio.charset.StandardCharsets.UTF_8))
                .timeout(java.time.Duration.ofMillis(aiTimeoutMs))
                .build();

        java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        log.info("API Response Body for model {}: {}", model, response.body());
        if (response.statusCode() != 200) {
            throw new RuntimeException("API returned status code " + response.statusCode() + ": " + response.body());
        }

        com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(response.body());
        com.fasterxml.jackson.databind.JsonNode contentNode = rootNode.path("choices").path(0)
                .path("message").path("content");
        if (contentNode.isMissingNode()) {
            throw new RuntimeException("Failed to locate text content in response: " + response.body());
        }
        
        String responseText = contentNode.asText();
        if (responseText == null || responseText.trim().isEmpty()) {
            throw new RuntimeException("API returned empty or null content text");
        }
        return responseText;
    }

    private String mergeHintIfMissing(String hint, String currentFoodItemsJson) {
        if (hint == null || hint.trim().isEmpty()) {
            return currentFoodItemsJson;
        }
        try {
            log.info("Checking and merging hint '{}' into food items JSON if missing...", hint);
            String textPrompt = "分析用户的餐后补充提示：\"" + hint.trim() + "\"\n" +
                    "和已识别的食物列表 JSON 数组：\n" +
                    currentFoodItemsJson + "\n\n" +
                    "请问已识别的列表中是否已经包含了提示中提到的所有食物和饮品？\n" +
                    "1. 如果已包含，请直接原样返回已识别的列表 JSON 数组（不要做任何修改）。\n" +
                    "2. 如果未包含，请将漏掉的提示中的食物/饮品估算重量和营养成分，并以相同的 JSON 格式追加到列表中一并返回。\n" +
                    "注意：你必须且只能返回标准的 JSON 数组格式，不要包含 ``` 标记，不要包含任何 markdown 格式，不要包含任何其他解释文字。";

            String modelToUse = "glm-4-flash"; // Fast and cheap text model
            String responseText = callTextAi(modelToUse, textPrompt);
            if (responseText != null && !responseText.trim().isEmpty()) {
                String cleanJson = responseText.trim();
                if (cleanJson.startsWith("```")) {
                    cleanJson = cleanJson.replaceAll("^```[a-zA-Z]*\\s*", "");
                    cleanJson = cleanJson.replaceAll("\\s*```$", "");
                }
                cleanJson = cleanJson.replaceAll("//.*", "").trim();
                log.info("Merged hint result: {}", cleanJson);
                return cleanJson;
            }
        } catch (Exception e) {
            log.warn("Failed to merge hint into food items: {}", e.getMessage());
        }
        return currentFoodItemsJson;
    }

    private String callTextAi(String model, String prompt) throws Exception {
        java.util.Map<String, Object> message = java.util.Map.of(
                "role", "user",
                "content", prompt
        );
        java.util.Map<String, Object> payload = java.util.Map.of(
                "model", model,
                "messages", java.util.List.of(message),
                "temperature", 0.1
        );
        String requestJson = objectMapper.writeValueAsString(payload);

        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .connectTimeout(java.time.Duration.ofMillis(aiTimeoutMs))
                .build();

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(aiEndpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiApiKey)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Connection", "close")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestJson, java.nio.charset.StandardCharsets.UTF_8))
                .timeout(java.time.Duration.ofMillis(aiTimeoutMs))
                .build();

        java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException("Text API returned status code " + response.statusCode() + ": " + response.body());
        }

        com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(response.body());
        return rootNode.path("choices").path(0).path("message").path("content").asText();
    }

    private void checkDailyAiLimit(Long userId) {
        try {
            String key = "shike:ai:limit:" + userId + ":" + LocalDate.now();
            String val = stringRedisTemplate.opsForValue().get(key);
            if (val != null) {
                int count = Integer.parseInt(val);
                if (count >= 10) {
                    throw new BizException(400, "您今天已达到每日 10 次 AI 识别上限，请明天再来哦～");
                }
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis is unavailable for checking AI limit. Proceeding without limit check.", e);
        }
    }

    private void incrementDailyAiCount(Long userId) {
        try {
            String key = "shike:ai:limit:" + userId + ":" + LocalDate.now();
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                stringRedisTemplate.expire(key, java.time.Duration.ofHours(24));
            }
        } catch (Exception e) {
            log.error("Failed to increment daily AI count in Redis", e);
        }
    }

    @Override
    @Transactional
    public java.util.Map<String, Object> diagnoseDiet(Long userId, LocalDate date) {
        java.util.Map<String, Boolean> toggles = adminService.getPublicFeatureToggles("release");
        if (toggles != null && Boolean.FALSE.equals(toggles.get("diet_diagnosis"))) {
            throw new BizException(400, "AI 营养诊断功能暂未开放或在维护中");
        }

        if (userId == null) {
            throw new BizException(400, "用户ID不能为空");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "用户不存在"));

        boolean hasDiagnosisBefore = pointsRecordRepository.existsByUserIdAndType(userId, "DIET_DIAGNOSIS");
        if (hasDiagnosisBefore) {
            int currentPoints = (user.getPoints() != null) ? user.getPoints() : 0;
            if (currentPoints < 15) {
                throw new BizException(400, "积分不足！生成 AI 深度营养诊断需要 15 积分，您当前剩余 " + currentPoints + " 积分。");
            }
            user.setPoints(currentPoints - 15);
            userRepository.save(user);

            PointsRecord record = PointsRecord.builder()
                    .userId(userId)
                    .amount(-15)
                    .type("DIET_DIAGNOSIS")
                    .remark("AI 膳食深度诊断 (消耗 15 积分)")
                    .build();
            pointsRecordRepository.save(record);
            log.info("Deducted 15 points for user {} for AI diet diagnosis. Remaining: {}", userId, user.getPoints());
        } else {
            PointsRecord record = PointsRecord.builder()
                    .userId(userId)
                    .amount(0)
                    .type("DIET_DIAGNOSIS")
                    .remark("AI 膳食深度诊断 (首次生成免费)")
                    .build();
            pointsRecordRepository.save(record);
            log.info("First time AI diet diagnosis for user {}, free of charge.", userId);
        }

        List<DietRecord> dailyRecords = getDailyRecords(userId, date);
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位拥有 15 年经验的资深注册营养师 (RD)。请根据用户今日的实际三餐打卡菜品与摄入数据，进行专业、温和且极其落地可操作的【AI 营养深度诊断点评】。\n\n");
        sb.append("【用户基本档案】:\n");
        sb.append("- 性别: ").append((user.getGender() != null && user.getGender() == 2) ? "女" : "男")
                .append(", 年龄: ").append(user.getAge() != null ? user.getAge() : 25)
                .append("岁, 体重: ").append(user.getWeight() != null ? user.getWeight() : 70).append("kg")
                .append(", 目标热量: ").append(user.getTargetCalories() != null ? user.getTargetCalories() : 2000).append(" kcal\n\n");

        sb.append("【今日已打卡餐食记录】:\n");
        if (dailyRecords == null || dailyRecords.isEmpty()) {
            sb.append("（用户今日尚未打卡任何餐食）\n");
        } else {
            for (DietRecord r : dailyRecords) {
                sb.append("- ").append(r.getMealType()).append(": ").append(r.getFoodItems()).append(" (摄入热量约 ").append(r.getTotalCalories() != null ? r.getTotalCalories() : 0).append(" kcal)\n");
            }
        }

        sb.append("\n【请输出标准的 JSON，包含专家点评与3条具体的调优建议，不要包含 markdown 标记】:\n");
        sb.append("{\n");
        sb.append("  \"expertComment\": \"150字以内的专业点评与总结，点评今日菜品搭配的优缺点，并给出温和鼓励。\",\n");
        sb.append("  \"aiInsights\": [\n");
        sb.append("    { \"badge\": \"🥩 蛋白诊断\", \"badgeClass\": \"badge-yellow\", \"title\": \"蛋白质达标点评\", \"suggestion\": \"结合今日吃过的菜品，给出下一餐具体补充食材(如 200g 鸡胸肉/2颗水煮蛋)\" },\n");
        sb.append("    { \"badge\": \"🍚 碳水评估\", \"badgeClass\": \"badge-blue\", \"title\": \"优质碳水占比\", \"suggestion\": \"具体主食替换或调整建议\" },\n");
        sb.append("    { \"badge\": \"💡 综合调优\", \"badgeClass\": \"badge-green\", \"title\": \"后续饮食策略\", \"suggestion\": \"针对性的饮食或运动搭配建议\" }\n");
        sb.append("  ]\n");
        sb.append("}\n");

        java.util.Map<String, Object> resMap = new java.util.HashMap<>();
        try {
            String textModel = (aiModel != null && !aiModel.isEmpty()) ? aiModel : "qwen3.6-flash";
            String aiResponseJson = callTextAi(textModel, sb.toString());
            String jsonText = aiResponseJson.trim();
            if (jsonText.startsWith("```json")) jsonText = jsonText.substring(7);
            if (jsonText.startsWith("```")) jsonText = jsonText.substring(3);
            if (jsonText.endsWith("```")) jsonText = jsonText.substring(0, jsonText.length() - 3);
            jsonText = jsonText.trim();

            resMap = objectMapper.readValue(jsonText, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to generate AI diet diagnosis for user {}, fallback", userId, e);
            resMap.put("expertComment", "根据您今日的录入情况，总体饮食结构控制得当。建议后半天保持充足饮水并优先选择少油高蛋白食材！");
        }

        resMap.put("userPoints", user.getPoints() != null ? user.getPoints() : 0);
        return resMap;
    }

    @Override
    public WeekDashboardDTO getWeekDashboard(Long userId, String dateStr) {
        LocalDate targetDate;
        if (dateStr != null && !dateStr.isBlank()) {
            try {
                targetDate = LocalDate.parse(dateStr.trim());
            } catch (Exception e) {
                targetDate = LocalDate.now();
            }
        } else {
            targetDate = LocalDate.now();
        }

        LocalDate monday = targetDate.with(java.time.DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);

        User user = userRepository.findById(userId).orElse(null);
        BigDecimal targetCalories = (user != null && user.getTargetCalories() != null)
                ? user.getTargetCalories()
                : BigDecimal.valueOf(2000.0);
        int targetInt = targetCalories.setScale(0, RoundingMode.HALF_UP).intValue();

        List<DietRecord> dietRecords = dietRecordRepository.findByUserIdAndRecordDateBetween(userId, monday, sunday);
        List<ExerciseRecord> exerciseRecords = exerciseRecordRepository.findByUserIdAndRecordDateBetween(userId, monday, sunday);

        String[] dayNames = new String[]{"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        List<WeekDashboardDTO.DailyItem> dailyDetails = new java.util.ArrayList<>();

        int totalIntake = 0;
        int totalBurned = 0;
        int checkinCount = 0;

        BigDecimal totalProtein = BigDecimal.ZERO;
        BigDecimal totalCarbs = BigDecimal.ZERO;
        BigDecimal totalFat = BigDecimal.ZERO;

        LocalDate today = LocalDate.now();

        List<com.shike.model.entity.WeightRecord> weekWeightRecords = weightRecordRepository.findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(userId, monday, sunday);
        java.util.Map<LocalDate, BigDecimal> weekWeightMap = new java.util.HashMap<>();
        if (weekWeightRecords != null) {
            for (com.shike.model.entity.WeightRecord r : weekWeightRecords) {
                weekWeightMap.put(r.getRecordDate(), r.getWeight());
            }
        }

        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = monday.plusDays(i);
            String dayName = dayNames[i];
            boolean isToday = currentDate.equals(today);

            int dayIntake = 0;
            int dayBurned = 0;

            if (dietRecords != null) {
                for (DietRecord r : dietRecords) {
                    if (currentDate.equals(r.getRecordDate())) {
                        if (r.getTotalCalories() != null) {
                            dayIntake += r.getTotalCalories().setScale(0, RoundingMode.HALF_UP).intValue();
                        }
                        if (r.getTotalProtein() != null) {
                            totalProtein = totalProtein.add(r.getTotalProtein());
                        }
                        if (r.getTotalCarbs() != null) {
                            totalCarbs = totalCarbs.add(r.getTotalCarbs());
                        }
                        if (r.getTotalFat() != null) {
                            totalFat = totalFat.add(r.getTotalFat());
                        }
                    }
                }
            }

            if (exerciseRecords != null) {
                for (ExerciseRecord r : exerciseRecords) {
                    if (currentDate.equals(r.getRecordDate())) {
                        if (r.getCaloriesBurned() != null) {
                            dayBurned += (int) Math.round(r.getCaloriesBurned());
                        }
                    }
                }
            }

            totalIntake += dayIntake;
            totalBurned += dayBurned;

            if (dayIntake > 0) {
                checkinCount++;
            }

            String status;
            if (dayIntake > targetInt + dayBurned) {
                status = "SURPLUS";
            } else if (dayIntake > 0) {
                status = "DEFICIT";
            } else {
                status = "NORMAL";
            }

            dailyDetails.add(WeekDashboardDTO.DailyItem.builder()
                    .date(currentDate)
                    .dayName(dayName)
                    .isToday(isToday)
                    .intake(dayIntake)
                    .burned(dayBurned)
                    .target(targetInt)
                    .status(status)
                    .build());
        }

        int avgDailyIntake = checkinCount > 0 ? totalIntake / checkinCount : 0;
        int avgDailyBurned = checkinCount > 0 ? totalBurned / checkinCount : 0;

        int activeDays = Math.max(1, checkinCount);
        int targetActiveDays = targetCalories.multiply(BigDecimal.valueOf(activeDays)).setScale(0, RoundingMode.HALF_UP).intValue();
        int accumulatedDeficit = targetActiveDays + totalBurned - totalIntake;

        BigDecimal avgProtein = totalProtein.divide(BigDecimal.valueOf(7), 1, RoundingMode.HALF_UP);
        BigDecimal avgCarbs = totalCarbs.divide(BigDecimal.valueOf(7), 1, RoundingMode.HALF_UP);
        BigDecimal avgFat = totalFat.divide(BigDecimal.valueOf(7), 1, RoundingMode.HALF_UP);

        double proteinKcal = avgProtein.doubleValue() * 4;
        double carbsKcal = avgCarbs.doubleValue() * 4;
        double fatKcal = avgFat.doubleValue() * 9;
        double totalKcal = proteinKcal + carbsKcal + fatKcal;
        if (totalKcal <= 0) {
            totalKcal = 1.0;
        }

        int proteinRatio = (int) Math.round(proteinKcal / totalKcal * 100);
        int carbsRatio = (int) Math.round(carbsKcal / totalKcal * 100);
        int fatRatio = (int) Math.round(fatKcal / totalKcal * 100);

        String healthRating;
        String evaluationMessage;

        if (accumulatedDeficit >= 1000) {
            healthRating = "A+ 控卡先锋";
            evaluationMessage = "本周累计成功打造热量缺口 " + accumulatedDeficit + " kcal！相当于减少纯脂肪约 " + String.format(java.util.Locale.US, "%.2f", accumulatedDeficit / 7700.0) + " kg，减脂成效斐然！";
        } else if (accumulatedDeficit >= 0) {
            healthRating = "A 热量平衡";
            evaluationMessage = "本周热量维持平衡，累计创缺口 " + accumulatedDeficit + " kcal，保持得很好！";
        } else {
            healthRating = "B 盈余提醒";
            evaluationMessage = "本周热量稍有超标（盈余 " + Math.abs(accumulatedDeficit) + " kcal），建议下周适当增加有氧运动或控制晚餐。加油！";
        }

        return WeekDashboardDTO.builder()
                .startDate(monday)
                .endDate(sunday)
                .targetCalories(targetCalories)
                .totalIntake(totalIntake)
                .totalBurned(totalBurned)
                .avgDailyIntake(avgDailyIntake)
                .avgDailyBurned(avgDailyBurned)
                .accumulatedDeficit(accumulatedDeficit)
                .checkinCount(checkinCount)
                .avgProtein(avgProtein)
                .avgCarbs(avgCarbs)
                .avgFat(avgFat)
                .proteinRatio(proteinRatio)
                .carbsRatio(carbsRatio)
                .fatRatio(fatRatio)
                .healthRating(healthRating)
                .evaluationMessage(evaluationMessage)
                .dailyDetails(dailyDetails)
                .build();
    }

    @Override
    public MonthDashboardDTO getMonthDashboard(Long userId, Integer year, Integer month) {
        LocalDate now = LocalDate.now();
        if (year == null) {
            year = now.getYear();
        }
        if (month == null) {
            month = now.getMonthValue();
        }

        String yearMonthStr = String.format(java.util.Locale.US, "%d年%02d月", year, month);
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.with(TemporalAdjusters.lastDayOfMonth());
        int daysInMonth = lastDay.getDayOfMonth();

        User user = userRepository.findById(userId).orElse(null);
        BigDecimal targetCalories = (user != null && user.getTargetCalories() != null)
                ? user.getTargetCalories()
                : BigDecimal.valueOf(2000.0);
        int targetInt = targetCalories.setScale(0, RoundingMode.HALF_UP).intValue();

        List<DietRecord> dietRecords = dietRecordRepository.findByUserIdAndRecordDateBetween(userId, firstDay, lastDay);
        List<ExerciseRecord> exerciseRecords = exerciseRecordRepository.findByUserIdAndRecordDateBetween(userId, firstDay, lastDay);

        java.util.Map<LocalDate, Integer> dailyIntakeMap = new java.util.HashMap<>();
        BigDecimal totalProteinBD = BigDecimal.ZERO;
        BigDecimal totalCarbsBD = BigDecimal.ZERO;
        BigDecimal totalFatBD = BigDecimal.ZERO;

        if (dietRecords != null) {
            for (DietRecord r : dietRecords) {
                if (r.getRecordDate() != null) {
                    if (r.getTotalCalories() != null) {
                        int cal = r.getTotalCalories().setScale(0, RoundingMode.HALF_UP).intValue();
                        dailyIntakeMap.merge(r.getRecordDate(), cal, Integer::sum);
                    }
                    if (r.getTotalProtein() != null) {
                        totalProteinBD = totalProteinBD.add(r.getTotalProtein());
                    }
                    if (r.getTotalCarbs() != null) {
                        totalCarbsBD = totalCarbsBD.add(r.getTotalCarbs());
                    }
                    if (r.getTotalFat() != null) {
                        totalFatBD = totalFatBD.add(r.getTotalFat());
                    }
                }
            }
        }

        java.util.Map<LocalDate, Integer> dailyBurnedMap = new java.util.HashMap<>();
        if (exerciseRecords != null) {
            for (ExerciseRecord r : exerciseRecords) {
                if (r.getRecordDate() != null && r.getCaloriesBurned() != null) {
                    int burned = (int) Math.round(r.getCaloriesBurned());
                    dailyBurnedMap.merge(r.getRecordDate(), burned, Integer::sum);
                }
            }
        }

        int totalIntake = dailyIntakeMap.values().stream().mapToInt(Integer::intValue).sum();
        int totalBurned = dailyBurnedMap.values().stream().mapToInt(Integer::intValue).sum();
        int checkinCount = (int) dailyIntakeMap.entrySet().stream().filter(e -> e.getValue() > 0).count();
        int checkinRate = (int) Math.round((double) checkinCount / daysInMonth * 100);

        int avgDailyIntake = checkinCount > 0 ? totalIntake / checkinCount : 0;
        int avgDailyBurned = checkinCount > 0 ? totalBurned / checkinCount : 0;

        int activeDays = Math.max(1, checkinCount);
        int targetActiveDays = targetCalories.multiply(BigDecimal.valueOf(activeDays)).setScale(0, RoundingMode.HALF_UP).intValue();
        int accumulatedDeficit = targetActiveDays + totalBurned - totalIntake;

        BigDecimal fatLossKg = BigDecimal.valueOf(accumulatedDeficit / 7700.0).setScale(2, RoundingMode.HALF_UP);

        BigDecimal avgProtein = totalProteinBD.divide(BigDecimal.valueOf(daysInMonth), 1, RoundingMode.HALF_UP);
        BigDecimal avgCarbs = totalCarbsBD.divide(BigDecimal.valueOf(daysInMonth), 1, RoundingMode.HALF_UP);
        BigDecimal avgFat = totalFatBD.divide(BigDecimal.valueOf(daysInMonth), 1, RoundingMode.HALF_UP);

        double proteinKcal = avgProtein.doubleValue() * 4;
        double carbsKcal = avgCarbs.doubleValue() * 4;
        double fatKcal = avgFat.doubleValue() * 9;
        double totalKcal = proteinKcal + carbsKcal + fatKcal;
        if (totalKcal <= 0) {
            totalKcal = 1.0;
        }

        int proteinRatio = (int) Math.round(proteinKcal / totalKcal * 100);
        int carbsRatio = (int) Math.round(carbsKcal / totalKcal * 100);
        int fatRatio = (int) Math.round(fatKcal / totalKcal * 100);

        List<MonthDashboardDTO.WeeklyTrendItem> weeklyTrends = new java.util.ArrayList<>();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("M.d");

        int numWeeks = (daysInMonth + 6) / 7;
        for (int w = 1; w <= numWeeks; w++) {
            int startDayNum = (w - 1) * 7 + 1;
            int endDayNum = Math.min(w * 7, daysInMonth);
            LocalDate startOfWeek = LocalDate.of(year, month, startDayNum);
            LocalDate endOfWeek = LocalDate.of(year, month, endDayNum);

            int weekTotalIntake = 0;
            int weekTotalBurned = 0;
            int weekCheckinCount = 0;

            for (int d = startDayNum; d <= endDayNum; d++) {
                LocalDate date = LocalDate.of(year, month, d);
                int dayIntake = dailyIntakeMap.getOrDefault(date, 0);
                int dayBurned = dailyBurnedMap.getOrDefault(date, 0);
                weekTotalIntake += dayIntake;
                weekTotalBurned += dayBurned;
                if (dayIntake > 0) {
                    weekCheckinCount++;
                }
            }

            int weekAvgIntake = weekCheckinCount > 0 ? weekTotalIntake / weekCheckinCount : 0;
            int weekAvgBurned = weekCheckinCount > 0 ? weekTotalBurned / weekCheckinCount : 0;

            String status;
            if (weekAvgIntake > targetInt + weekAvgBurned) {
                status = "SURPLUS";
            } else if (weekAvgIntake > 0) {
                status = "DEFICIT";
            } else {
                status = "NORMAL";
            }

            weeklyTrends.add(MonthDashboardDTO.WeeklyTrendItem.builder()
                    .weekName("第" + w + "周")
                    .weekPeriod(startOfWeek.format(fmt) + "-" + endOfWeek.format(fmt))
                    .avgIntake(weekAvgIntake)
                    .avgBurned(weekAvgBurned)
                    .target(targetInt)
                    .status(status)
                    .build());
        }

        String healthRating;
        String evaluationMessage;

        if (accumulatedDeficit >= 4000) {
            healthRating = "A+ 30天蜕变先锋";
            evaluationMessage = "本月已坚持打卡 " + checkinCount + " 天，累计创热量缺口 " + accumulatedDeficit + " kcal！相当于减少纯脂肪约 " + String.format(java.util.Locale.US, "%.2f", fatLossKg) + " kg，坚持出肉眼可见的蜕变！";
        } else if (accumulatedDeficit >= 0) {
            healthRating = "A 月度控制优良";
            evaluationMessage = "本月累计打卡 " + checkinCount + " 天，热量维持平衡，累计创缺口 " + accumulatedDeficit + " kcal，表现稳定！";
        } else {
            healthRating = "B 盈余预警";
            evaluationMessage = "本月累计热量盈余 " + Math.abs(accumulatedDeficit) + " kcal，建议下个月适当控制高油高糖食物并增加有氧运动。";
        }

        List<com.shike.model.entity.WeightRecord> monthWeightRecords = weightRecordRepository.findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(userId, firstDay, lastDay);
        List<MonthDashboardDTO.DailyWeightPoint> dailyWeightPoints = new java.util.ArrayList<>();
        BigDecimal weightStart = null;
        BigDecimal weightLatest = null;
        BigDecimal maxWeight = null;
        BigDecimal minWeight = null;
        BigDecimal totalWeightChange = null;

        if (monthWeightRecords != null && !monthWeightRecords.isEmpty()) {
            java.time.format.DateTimeFormatter dayFmt = java.time.format.DateTimeFormatter.ofPattern("MM.dd");
            weightStart = monthWeightRecords.get(0).getWeight();
            weightLatest = monthWeightRecords.get(monthWeightRecords.size() - 1).getWeight();

            for (com.shike.model.entity.WeightRecord r : monthWeightRecords) {
                BigDecimal w = r.getWeight();
                if (w != null) {
                    if (maxWeight == null || w.compareTo(maxWeight) > 0) maxWeight = w;
                    if (minWeight == null || w.compareTo(minWeight) < 0) minWeight = w;

                    dailyWeightPoints.add(MonthDashboardDTO.DailyWeightPoint.builder()
                            .date(r.getRecordDate())
                            .dayStr(r.getRecordDate().format(dayFmt))
                            .weight(w)
                            .build());
                }
            }

            if (weightStart != null && weightLatest != null) {
                totalWeightChange = weightLatest.subtract(weightStart).setScale(2, RoundingMode.HALF_UP);
            }
        }

        return MonthDashboardDTO.builder()
                .year(year)
                .month(month)
                .yearMonthStr(yearMonthStr)
                .targetCalories(targetCalories)
                .totalIntake(totalIntake)
                .totalBurned(totalBurned)
                .avgDailyIntake(avgDailyIntake)
                .avgDailyBurned(avgDailyBurned)
                .accumulatedDeficit(accumulatedDeficit)
                .checkinCount(checkinCount)
                .daysInMonth(daysInMonth)
                .checkinRate(checkinRate)
                .fatLossKg(fatLossKg)
                .avgProtein(avgProtein)
                .avgCarbs(avgCarbs)
                .avgFat(avgFat)
                .proteinRatio(proteinRatio)
                .carbsRatio(carbsRatio)
                .fatRatio(fatRatio)
                .healthRating(healthRating)
                .evaluationMessage(evaluationMessage)
                .weightStart(weightStart)
                .weightLatest(weightLatest)
                .maxWeight(maxWeight)
                .minWeight(minWeight)
                .totalWeightChange(totalWeightChange)
                .dailyWeightRecords(dailyWeightPoints)
                .weeklyTrends(weeklyTrends)
                .build();
    }

    @Override
    @Transactional
    public void recordWeight(Long userId, BigDecimal weight, LocalDate date) {
        if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(400, "体重数值不合法");
        }
        if (date == null) {
            date = LocalDate.now();
        }

        com.shike.model.entity.WeightRecord record = weightRecordRepository.findByUserIdAndRecordDate(userId, date)
                .orElse(com.shike.model.entity.WeightRecord.builder()
                        .userId(userId)
                        .recordDate(date)
                        .build());
        record.setWeight(weight);
        weightRecordRepository.save(record);

        if (date.equals(LocalDate.now())) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                user.setWeight(weight);
                userRepository.save(user);
            }
        }
    }
}
