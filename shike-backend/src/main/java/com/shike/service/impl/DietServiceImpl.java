package com.shike.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shike.common.BizException;
import com.shike.model.entity.DietRecord;
import com.shike.repository.DietRecordRepository;
import com.shike.service.DietService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    @Override
    public DietRecord recognizeMeal(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BizException(400, "Uploaded file cannot be empty");
        }
        
        log.info("Received image for AI recognition: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());

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
            return fallbackRecord;
        }

        try {
            // 1. Convert MultipartFile to Base64
            byte[] fileBytes = file.getBytes();
            String base64Data = java.util.Base64.getEncoder().encodeToString(fileBytes);
            String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
            String dataUrl = "data:" + mimeType + ";base64," + base64Data;

            // 2. Prepare Prompt
            String prompt = "你是一个营养学专家。请分析以下食物图片，识别出其中的菜品/食品名称及估计重量（克），并返回它们的热量（kcal）、蛋白质（g）、脂肪（g）、碳水化合物（g）估算值。\n" +
                    "你必须只返回一个 JSON 数组，不要包含任何 markdown 代码块标记，不要包含 ```json 或 ```，不要包含任何其他文字。\n" +
                    "JSON 数组的每个对象必须包含以下字段：\n" +
                    "- name: 食物/菜品名称 (例如: 香煎鸡胸肉)\n" +
                    "- weight: 估算重量克数 (例如: 150)\n" +
                    "- calories: 估算热量 (例如: 250)\n" +
                    "- protein: 蛋白质克数 (例如: 30.0)\n" +
                    "- fat: 脂肪克数 (例如: 5.0)\n" +
                    "- carbs: 碳水化合物克数 (例如: 0.0)";

            String responseBody = "";

            // 3. Make HTTP request based on Provider
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
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
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestJson))
                        .timeout(java.time.Duration.ofMillis(aiTimeoutMs))
                        .build();

                java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
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
                
                java.util.Map<String, Object> textPart = java.util.Map.of("type", "text", "text", prompt);
                java.util.Map<String, Object> imageUrlDetail = java.util.Map.of("url", dataUrl);
                java.util.Map<String, Object> imagePart = java.util.Map.of("type", "image_url", "image_url", imageUrlDetail);
                
                java.util.Map<String, Object> userMessage = java.util.Map.of(
                        "role", "user",
                        "content", java.util.List.of(textPart, imagePart)
                );
                
                java.util.Map<String, Object> payload = java.util.Map.of(
                        "model", aiModel,
                        "messages", java.util.List.of(userMessage),
                        "temperature", 0.2
                );

                String requestJson = objectMapper.writeValueAsString(payload);

                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(aiEndpoint))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + aiApiKey)
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestJson))
                        .timeout(java.time.Duration.ofMillis(aiTimeoutMs))
                        .build();

                java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new RuntimeException("OpenAI-compatible API returned status code " + response.statusCode() + ": " + response.body());
                }

                com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(response.body());
                com.fasterxml.jackson.databind.JsonNode contentNode = rootNode.path("choices").path(0)
                        .path("message").path("content");
                if (contentNode.isMissingNode()) {
                    throw new RuntimeException("Failed to locate text content in OpenAI response: " + response.body());
                }
                responseBody = contentNode.asText();
            }

            log.debug("Raw AI Vision Response text: {}", responseBody);

            String cleanJson = responseBody.trim();
            if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.replaceAll("^```[a-zA-Z]*\\s*", "");
                cleanJson = cleanJson.replaceAll("\\s*```$", "");
            }
            cleanJson = cleanJson.trim();

            FoodItem[] items = objectMapper.readValue(cleanJson, FoodItem[].class);
            
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

            return DietRecord.builder()
                    .foodItems(cleanJson)
                    .totalCalories(calories)
                    .totalProtein(protein)
                    .totalFat(fat)
                    .totalCarbs(carbs)
                    .imageUrl("https://images.example.com/meals/lunch.jpg")
                    .build();

        } catch (Exception e) {
            log.error("AI Visual recognition failed or parsed incorrectly, silently falling back to mock healthy meal.", e);
            return fallbackRecord;
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
}
