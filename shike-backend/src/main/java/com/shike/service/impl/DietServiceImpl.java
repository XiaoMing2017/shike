package com.shike.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shike.common.BizException;
import com.shike.model.dto.MonthSummaryDTO;
import com.shike.model.entity.DietRecord;
import com.shike.model.entity.User;
import com.shike.model.entity.PointsRecord;
import com.shike.repository.DietRecordRepository;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DietServiceImpl implements DietService {

    private final DietRecordRepository dietRecordRepository;
    private final UserRepository userRepository;
    private final PointsRecordRepository pointsRecordRepository;
    private final ObjectMapper objectMapper;

    @Value("${ai.provider:OPENAI}")
    private String aiProvider;

    @Value("${ai.api-key:MOCK_KEY}")
    private String aiApiKey;

    @Value("${ai.endpoint:https://api.openai.com/v1/chat/completions}")
    private String aiEndpoint;

    @Value("${ai.model:gpt-4o-mini}")
    private String aiModel;

    @Value("${ai.timeout-ms:15000}")
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
        if (file.isEmpty()) {
            throw new BizException(400, "Uploaded file cannot be empty");
        }
        
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
                                "1. 判断【" + dishName + "】是否是食物或饮料。如果它明显不是食物（例如属于电子产品、人物、风景、车辆、普通物件等），或者无法确定是食物，请必须返回空数组 []。\n" +
                                "2. 如果是食物，结合常识和用户提示估算这餐的分量。请只返回一个包含单个食物对象的 JSON 数组（不要包含任何 markdown 代码块标记，如 ```json，不要包含任何额外文字），格式必须严格为：\n" +
                                "[\n" +
                                "  {\n" +
                                "    \"name\": \"" + dishName + "\",\n" +
                                "    \"weight\": 估算克数,\n" +
                                "    \"calories\": 估算热量kcal,\n" +
                                "    \"protein\": 蛋白质克数,\n" +
                                "    \"fat\": 脂肪克数,\n" +
                                "    \"carbs\": 碳水化合物克数\n" +
                                "  }\n" +
                                "]";

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
            // 1. Convert MultipartFile to Base64 (compress/resize image to speed up AI recognition and prevent timeout)
            byte[] fileBytes = compressImage(file);
            String base64Data = java.util.Base64.getEncoder().encodeToString(fileBytes);
            String mimeType = "image/jpeg"; // Output of compression is JPEG
            String dataUrl = "data:" + mimeType + ";base64," + base64Data;

            String prompt = "你是一个专业的中国膳食营养分析师，擅长通过视觉特征精确识别食物。请按以下步骤分析图片：\n" +
                    "\n" +
                    "## 第一步：观察与思考\n" +
                    "请仔细观察图片，在识别过程中详细分析以下特征，再做出判断：\n" +
                    "- **颜色与形状**：区分相似特征的食材。例如：\n" +
                    "  * 肥肠 (Pork Intestine)：呈中空的圈状、管状或折叠皱褶状，表面有褶皱，内壁通常可见油脂感。通常与青椒、洋葱、辣椒或干辣椒丝搭配烹饪（如尖椒肥肠、干锅肥肠）。\n" +
                    "  * 鸡肉 (Chicken)：呈实心的、不规则块状或带骨块状，表面可见明显的肉质纤维纹理，切面为实心。\n" +
                    "  * 猪肉 (Pork)：多为片状、丝状或条状，与配菜均匀混合。\n" +
                    "  * 土豆丁 (Potato) vs 豆腐块 (Tofu) vs 年糕 (Rice cake)\n" +
                    "- **烹饪方式与配料**：是否带酱汁？是否搭配了洋葱、青椒、辣椒等中餐常见配菜？\n" +
                    "\n" +
                    "## 第二步：输出格式\n" +
                    "你必须只返回一个符合 JSON 格式的单个对象（不要包含 any markdown 代码块标记，不要包含 any 其他文字），格式必须严格为：\n" +
                    "{\n" +
                    "  \"thinking\": \"你的详细分析思考过程，简述对食材的颜色、中空圈状与实心块状等视觉特征的对比判断，解释判定是某种食物而非易混淆食物的理由\",\n" +
                    "  \"foodItems\": [\n" +
                    "    {\n" +
                    "      \"name\": \"食物名称，尽量准确具体（例如 '尖椒肥肠' 而非 '炒肉'）\",\n" +
                    "      \"weight\": 估算重量克数,\n" +
                    "      \"calories\": 估算热量 kcal,\n" +
                    "      \"protein\": 蛋白质克数,\n" +
                    "      \"fat\": 脂肪克数,\n" +
                    "      \"carbs\": 碳水化合物克数\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}\n" +
                    "\n" +
                    "如果图片中没有任何食物或饮品，请在 foodItems 中返回空数组 []。";

            if (hint != null && !hint.trim().isEmpty()) {
                prompt += "\n【特别说明：用户为本餐提供了补充文字提示：\"" + hint.trim() + "\"。请务必优先结合此提示词对图片中的对应食物进行正名和精准识别。如果提示词中提到了某些食物或饮品（例如：“一杯牛奶”、“生椰拿铁”、“可乐”等），即使这些食物或饮品在图片中没有出现、看不清或不明显，你必须强行将其作为独立的食材项（FoodItem）加入到最终的 `foodItems` 列表或 JSON 数组中，并根据提示常识合理估算其重量与热量营养素！绝对不能漏掉用户补充提示中提到的任何食物或饮品！】\n";
            }

            prompt += "\n## 第三步：估算营养数据\n" +
                    "根据识别出的每种食材，估算其重量（克）和营养成分。\n" +
                    "\n" +
                    "## 输出格式\n" +
                    "你必须只返回一个符合 JSON 格式的数组（例如：[{\"name\": \"食物名称\", \"weight\": 200, \"calories\": 300, \"protein\": 10, \"fat\": 8, \"carbs\": 20}]），不要包含 any markdown 代码块标记（不要 ```json 或 ```），不要包含 any 其他文字。\n" +
                    "如果图片中没有任何食物或饮品，必须且只能返回标准的空数组 []。\n" +
                    "JSON 数组的每个对象包含以下字段：\n" +
                    "- name: 食物名称，尽量准确具体（例如 '火腿青菜炒饭' 而非 '炒饭'）\n" +
                    "- weight: 估算重量克数\n" +
                    "- calories: 估算热量 kcal\n" +
                    "- protein: 蛋白质克数\n" +
                    "- fat: 脂肪克数\n" +
                    "- carbs: 碳水化合物克数";

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
                log.info("Calling OpenAI compatible vision API: {}, model: {}", aiEndpoint, aiModel);
                try {
                    responseBody = callOpenAiVision(aiModel, prompt, dataUrl, mimeType);
                } catch (Exception e) {
                    if (!"glm-4v-flash".equalsIgnoreCase(aiModel)) {
                        log.warn("Primary model {} failed: {}. Falling back to stable model glm-4v-flash...", aiModel, e.getMessage());
                        try {
                            responseBody = callOpenAiVision("glm-4v-flash", prompt, dataUrl, mimeType);
                        } catch (Exception ex) {
                            log.error("Fallback model glm-4v-flash also failed", ex);
                            throw ex;
                        }
                    } else {
                        throw e;
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
                if (totalCal.compareTo(budget) <= 0) {
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
        } else {
            payload = java.util.Map.of(
                    "model", model,
                    "messages", java.util.List.of(userMessage),
                    "temperature", 0.1,
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
}
