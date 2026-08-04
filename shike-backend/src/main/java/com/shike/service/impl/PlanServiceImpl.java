package com.shike.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shike.common.BizException;
import com.shike.model.entity.User;
import com.shike.repository.UserRepository;
import com.shike.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanServiceImpl implements PlanService {

    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.provider:OPENAI}")
    private String aiProvider;

    @Value("${ai.api-key:MOCK_KEY}")
    private String aiApiKey;

    @Value("${ai.endpoint:https://api.openai.com/v1/chat/completions}")
    private String aiEndpoint;

    @Value("${ai.model:gpt-4o-mini}")
    private String aiModel;

    @Value("${ai.timeout-ms:120000}")
    private Integer aiTimeoutMs;

    private static final String REDIS_PLAN_KEY_PREFIX = "shike:user:plan:";

    @Override
    public Map<String, Object> generateOrGetPlan(Long userId, Boolean forceRefresh, Boolean createIfAbsent) {
        String cacheKey = REDIS_PLAN_KEY_PREFIX + userId;

        // 1. 检查 Redis 缓存
        if (!Boolean.TRUE.equals(forceRefresh)) {
            String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null && !cachedJson.trim().isEmpty()) {
                try {
                    log.info("Returning cached AI plan for userId {}", userId);
                    return objectMapper.readValue(cachedJson, new TypeReference<Map<String, Object>>() {});
                } catch (Exception e) {
                    log.warn("Failed to parse cached plan JSON, will regenerate", e);
                }
            }
        }

        // 如果未指示生成 (createIfAbsent=false) 且不需要强制刷新，则不触发 AI 模型，直接返回 null
        if (!Boolean.TRUE.equals(createIfAbsent) && !Boolean.TRUE.equals(forceRefresh)) {
            log.info("No cached plan for userId {} and createIfAbsent is false. Skip AI invocation.", userId);
            return null;
        }

        // 2. 获取用户档案
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "找不到该用户档案"));

        // 3. 构建专属 AI Prompt (融合专家知识库准则)
        Map<String, Object> planMap;
        try {
            String prompt = buildExpertPrompt(user);
            String aiResponseJson = callTextLlm(prompt);
            planMap = parseAndCleanJson(aiResponseJson);
            recordPlanAiUsage();
        } catch (Exception e) {
            log.error("AI Generation failed for user {}, fallback to template plan: {}", userId, e.getMessage());
            planMap = generateScientificFallbackPlan(user);
        }

        // 4. 写入缓存 (保留 7 天)
        try {
            String planJson = objectMapper.writeValueAsString(planMap);
            stringRedisTemplate.opsForValue().set(cacheKey, planJson, 7, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("Failed to cache plan to Redis", e);
        }

        return planMap;
    }

    private String buildExpertPrompt(User user) {
        String genderStr = (user.getGender() != null && user.getGender() == 2) ? "女" : "男";
        int age = (user.getAge() != null) ? user.getAge() : 25;
        double height = (user.getHeight() != null) ? user.getHeight().doubleValue() : 175.0;
        double weight = (user.getWeight() != null) ? user.getWeight().doubleValue() : 70.0;
        double bodyFat = (user.getCurrentBodyFat() != null) ? user.getCurrentBodyFat().doubleValue() : (genderStr.equals("男") ? 18.0 : 24.0);
        double bmr = (user.getBmr() != null) ? user.getBmr().doubleValue() : 1600.0;
        double tdee = (user.getTdee() != null) ? user.getTdee().doubleValue() : 2200.0;
        double targetCal = (user.getTargetCalories() != null) ? user.getTargetCalories().doubleValue() : 2000.0;

        String goalLabel = translateGoal(user.getGoal(), user.getCustomGoalType());
        String activityLabel = translateActivity(user.getActivityLevel());

        // 根据三大营养素推算
        int proteinG = (int) Math.round(weight * (goalLabel.contains("增肌") || goalLabel.contains("腹肌") ? 2.0 : 1.8));
        int fatG = (int) Math.round((targetCal * 0.25) / 9.0);
        int carbsG = (int) Math.round((targetCal - (proteinG * 4) - (fatG * 9)) / 4.0);

        StringBuilder sb = new StringBuilder();
        sb.append("你是一位拥有 15 年经验的资深注册营养师 (RD) 和国际体能训练专家 (CSCS)。\n");
        sb.append("请根据以下用户的身体数据及专家知识库准则，生成【7天运动周计划】与【每日4餐膳食建议】。\n\n");
        sb.append("【用户基本档案】:\n");
        sb.append("- 性别: ").append(genderStr).append(", 年龄: ").append(age).append("岁, 身高: ").append(height).append("cm, 体重: ").append(weight).append("kg, 体脂率: ").append(bodyFat).append("%\n");
        sb.append("- 当前活动水平: ").append(activityLabel).append("\n");
        sb.append("- 基础代谢 BMR: ").append(bmr).append(" kcal, 每日消耗 TDEE: ").append(tdee).append(" kcal\n");
        sb.append("- 设定目标: ").append(goalLabel).append("\n");
        sb.append("- 每日目标摄入热量: ").append((int) targetCal).append(" kcal (推荐三大营养素: 蛋白质 ").append(proteinG).append("g, 碳水 ").append(carbsG).append("g, 脂肪 ").append(fatG).append("g)\n\n");

        sb.append("【专家知识库核心准则 (必须严格遵守)】:\n");
        sb.append("1. 【Zone 2 有氧与抗阻结合】(出处: Peter Attia / Dr. Huberman): 减脂/维持目标中，有氧优先推荐 Zone 2 心率稳态慢跑/快走；力量训练必须占主体以保肌。\n");
        sb.append("2. 【同肌群恢复期】(出处: Huberman Lab): 同一大肌群（如胸/腿/背）训练后至少间隔 48-72 小时，每周安排 1-2 天休息/主动恢复。\n");
        sb.append("3. 【可选运动类型】(必须从以下名称中挑选): 跑步 🏃, 慢跑 🏃‍♂️, 快走 🚶‍♂️, 散步 🚶, 动感单车 🚲, 游泳 🏊, 力量训练 💪, 瑜伽/普拉提 🧘, HIIT/有氧操 ⚡, 篮球/足球/球类 🏀。\n");
        sb.append("4. 【三大营养素分配】(出处: Precision Nutrition / AARR): 蛋白质均匀分布在4餐中；碳水集中在训练前后；推荐普通超市易得天然食材（如无糖豆浆、水煮蛋、鸡胸肉、紫米饭、西兰花）。\n");
        sb.append("5. 【手掌估算比喻】(出处: Precision Nutrition): 在膳食介绍中给出手掌大小比喻（如 1掌心蛋白质、1拳头蔬菜、1手心碳水）。\n\n");

        sb.append("【请严格按以下 JSON 格式输出，不要包含任何 markdown 代码块标记或多余文字】:\n");
        sb.append("{\n");
        sb.append("  \"summary\": \"针对您的目标简明扼要的专业专家点评与建议（150字以内）\",\n");
        sb.append("  \"nutritionOverview\": {\n");
        sb.append("    \"targetCal\": ").append((int) targetCal).append(",\n");
        sb.append("    \"proteinG\": ").append(proteinG).append(",\n");
        sb.append("    \"carbsG\": ").append(carbsG).append(",\n");
        sb.append("    \"fatG\": ").append(fatG).append("\n");
        sb.append("  },\n");
        sb.append("  \"workoutPlan\": [\n");
        sb.append("    {\n");
        sb.append("      \"day\": \"周一\",\n");
        sb.append("      \"focus\": \"训练部位与焦点（如：胸肌与三头力量 + Zone2有氧）\",\n");
        sb.append("      \"isRestDay\": false,\n");
        sb.append("      \"items\": [\n");
        sb.append("        { \"name\": \"力量训练 💪\", \"duration\": 35, \"calories\": 210, \"detail\": \"哑铃卧推/俯卧撑 4组x12次 (RIR 2)\" },\n");
        sb.append("        { \"name\": \"慢跑 🏃‍♂️\", \"duration\": 25, \"calories\": 180, \"detail\": \"保持心率在 Zone 2 燃脂区间\" }\n");
        sb.append("      ]\n");
        sb.append("    }\n");
        sb.append("    // ... 周二至周日共7天\n");
        sb.append("  ],\n");
        sb.append("  \"dietPlan\": {\n");
        sb.append("    \"breakfast\": { \"title\": \"高蛋白唤醒早餐\", \"calories\": 400, \"protein\": 28, \"carbs\": 45, \"fat\": 12, \"portionHint\": \"约1掌心蛋白质+1手心碳水\", \"foods\": [\"无糖豆浆 300ml\", \"水煮蛋 2个\", \"全麦切片 2片\"] },\n");
        sb.append("    \"lunch\": { \"title\": \"优质复合碳水午餐\", \"calories\": 650, \"protein\": 42, \"carbs\": 65, \"fat\": 18, \"portionHint\": \"约1.5掌心蛋白+1拳头蔬菜+1手心紫米\", \"foods\": [\"香煎鸡胸肉 150g\", \"紫米饭 150g\", \"蒜蓉西兰花 200g\"] },\n");
        sb.append("    \"dinner\": { \"title\": \"轻盈低碳晚餐\", \"calories\": 450, \"protein\": 35, \"carbs\": 40, \"fat\": 14, \"portionHint\": \"约1掌心海鲜+1.5拳头蔬菜\", \"foods\": [\"清蒸三文鱼/虾仁 120g\", \"蒸南瓜 150g\", \"凉拌黄瓜 200g\"] },\n");
        sb.append("    \"snack\": { \"title\": \"练前/练后加餐\", \"calories\": 200, \"protein\": 15, \"carbs\": 25, \"fat\": 4, \"portionHint\": \"练前补充1根香蕉或100g无糖酸奶\", \"foods\": [\"低脂无糖酸奶 150g\", \"香蕉 1根\"] }\n");
        sb.append("  }\n");
        sb.append("}\n");

        return sb.toString();
    }

    private String callTextLlm(String prompt) throws Exception {
        log.info("Calling Text LLM endpoint: {}, model: {}", aiEndpoint, aiModel);

        Map<String, Object> systemMsg = Map.of("role", "system", "content", "你是一位严格输出标准JSON的专业营养师与健身教练AI。");
        Map<String, Object> userMsg = Map.of("role", "user", "content", prompt);

        Map<String, Object> payload;
        if (aiModel.contains("qwen")) {
            payload = Map.of(
                    "model", aiModel,
                    "messages", List.of(systemMsg, userMsg),
                    "temperature", 0.6,
                    "max_tokens", 2500,
                    "enable_thinking", false
            );
        } else {
            payload = Map.of(
                    "model", aiModel,
                    "messages", List.of(systemMsg, userMsg),
                    "temperature", 0.7,
                    "max_tokens", 2500
            );
        }

        String requestBodyJson = objectMapper.writeValueAsString(payload);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiEndpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .timeout(Duration.ofMillis(aiTimeoutMs != null ? aiTimeoutMs : 60000))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("LLM API returned code " + response.statusCode() + ": " + response.body());
        }

        String body = response.body();
        Map<String, Object> respMap = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        List<Map<String, Object>> choices = (List<Map<String, Object>>) respMap.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("No choices returned in LLM response: " + body);
        }

        Map<String, Object> choice0 = choices.get(0);
        Map<String, Object> message = (Map<String, Object>) choice0.get("message");
        return (String) message.get("content");
    }

    private Map<String, Object> parseAndCleanJson(String rawText) throws Exception {
        String jsonText = rawText.trim();
        if (jsonText.startsWith("```json")) {
            jsonText = jsonText.substring(7);
        } else if (jsonText.startsWith("```")) {
            jsonText = jsonText.substring(3);
        }
        if (jsonText.endsWith("```")) {
            jsonText = jsonText.substring(0, jsonText.length() - 3);
        }
        jsonText = jsonText.trim();

        return objectMapper.readValue(jsonText, new TypeReference<Map<String, Object>>() {});
    }

    private String translateGoal(String goal, String customGoalType) {
        if (goal == null) return "保持体重";
        switch (goal) {
            case "LOSE_WEIGHT": return "科学减脂 (热量赤字)";
            case "GAIN_MUSCLE": return "增肌塑形 (热量盈余)";
            case "ABS": return "极致腹肌塑形 (体脂率管理)";
            case "PERIOD": return "周期定量冲刺 (" + (customGoalType != null ? customGoalType : "限时管理") + ")";
            case "MAINTAIN":
            default: return "保持体重与体能重构";
        }
    }

    private String translateActivity(String activity) {
        if (activity == null) return "久坐不运动";
        switch (activity) {
            case "LIGHT": return "轻度运动 (每周1-3次)";
            case "MODERATE": return "中度运动 (每周3-5次)";
            case "ACTIVE": return "重度运动 (每天高强度)";
            case "SEDENTARY":
            default: return "久坐不运动 (办公室族)";
        }
    }

    /**
     * 当 AI 接口超时或不可用时的保底科学计划，确保功能永不断供
     */
    private Map<String, Object> generateScientificFallbackPlan(User user) {
        double weight = (user.getWeight() != null) ? user.getWeight().doubleValue() : 70.0;
        double targetCal = (user.getTargetCalories() != null) ? user.getTargetCalories().doubleValue() : 2000.0;
        String goal = (user.getGoal() != null) ? user.getGoal() : "MAINTAIN";

        int proteinG = (int) Math.round(weight * (goal.equals("GAIN_MUSCLE") || goal.equals("ABS") ? 2.0 : 1.8));
        int fatG = (int) Math.round((targetCal * 0.25) / 9.0);
        int carbsG = (int) Math.round((targetCal - (proteinG * 4) - (fatG * 9)) / 4.0);

        Map<String, Object> res = new HashMap<>();
        res.put("summary", "根据您的档案与RD/CSCS专家知识库，为您制订了科学循序渐进的周运动与饮食分配方案。");

        Map<String, Object> overview = new HashMap<>();
        overview.put("targetCal", (int) targetCal);
        overview.put("proteinG", proteinG);
        overview.put("carbsG", carbsG);
        overview.put("fatG", fatG);
        res.put("nutritionOverview", overview);

        List<Map<String, Object>> workoutList = new ArrayList<>();
        String[] days = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        String[] focuses = {
                "胸大肌与三头力量 + Zone2慢跑",
                "下肢力量 (深蹲腿举) + 拉伸",
                "主动恢复与轻度散步",
                "背肌与二头力量 + 动感单车",
                "肩部与核心力量 + 快走",
                "全身 HIIT 燃脂 / 游泳",
                "完全休息日 (神经恢复)"
        };

        for (int i = 0; i < 7; i++) {
            Map<String, Object> dayMap = new HashMap<>();
            dayMap.put("day", days[i]);
            dayMap.put("focus", focuses[i]);
            dayMap.put("isRestDay", i == 2 || i == 6);

            List<Map<String, Object>> items = new ArrayList<>();
            if (i == 0) {
                items.add(Map.of("name", "力量训练 💪", "duration", 35, "calories", 210, "detail", "哑铃卧推 4组x12次 (RIR 2)"));
                items.add(Map.of("name", "慢跑 🏃‍♂️", "duration", 25, "calories", 180, "detail", "心率维持 Zone 2 稳态"));
            } else if (i == 1) {
                items.add(Map.of("name", "力量训练 💪", "duration", 40, "calories", 240, "detail", "深蹲/箭步蹲 4组x10次"));
                items.add(Map.of("name", "快走 🚶‍♂️", "duration", 20, "calories", 110, "detail", "放松下肢"));
            } else if (i == 2) {
                items.add(Map.of("name", "散步 🚶", "duration", 30, "calories", 100, "detail", "户外低速散步，吸收阳光"));
            } else if (i == 3) {
                items.add(Map.of("name", "力量训练 💪", "duration", 40, "calories", 230, "detail", "高位下拉/高位划船 4组x12次"));
                items.add(Map.of("name", "动感单车 🚲", "duration", 20, "calories", 160, "detail", "中等阻力骑行"));
            } else if (i == 4) {
                items.add(Map.of("name", "力量训练 💪", "duration", 30, "calories", 180, "detail", "推举与平板支撑核心"));
                items.add(Map.of("name", "快走 🚶‍♂️", "duration", 30, "calories", 150, "detail", "保持基础消耗"));
            } else if (i == 5) {
                items.add(Map.of("name", "游泳 🏊", "duration", 40, "calories", 300, "detail", "自由泳/蛙泳交替"));
            } else {
                items.add(Map.of("name", "瑜伽/普拉提 🧘", "duration", 20, "calories", 60, "detail", "全身筋膜松解与冥想"));
            }
            dayMap.put("items", items);
            workoutList.add(dayMap);
        }
        res.put("workoutPlan", workoutList);

        Map<String, Object> dietPlan = new HashMap<>();
        dietPlan.put("breakfast", Map.of("title", "高蛋白唤醒早餐", "calories", 400, "protein", 28, "carbs", 45, "fat", 12, "portionHint", "约1掌心蛋白+1手心碳水", "foods", List.of("无糖豆浆 300ml", "水煮蛋 2个", "全麦切片 2片")));
        dietPlan.put("lunch", Map.of("title", "优质复合碳水午餐", "calories", 650, "protein", 42, "carbs", 65, "fat", 18, "portionHint", "约1.5掌心蛋白+1拳头蔬菜+1手心紫米", "foods", List.of("香煎鸡胸肉 150g", "紫米饭 150g", "蒜蓉西兰花 200g")));
        dietPlan.put("dinner", Map.of("title", "轻盈低碳晚餐", "calories", 450, "protein", 35, "carbs", 40, "fat", 14, "portionHint", "约1掌心海鲜+1.5拳头蔬菜", "foods", List.of("清蒸三文鱼/虾仁 120g", "蒸南瓜 150g", "凉拌黄瓜 200g")));
        dietPlan.put("snack", Map.of("title", "练前/练后加餐", "calories", 200, "protein", 15, "carbs", 25, "fat", 4, "portionHint", "练前补充1根香蕉或100g无糖酸奶", "foods", List.of("低脂无糖酸奶 150g", "香蕉 1根")));

        res.put("dietPlan", dietPlan);
        return res;
    }

    private void recordPlanAiUsage() {
        try {
            String todayStr = java.time.LocalDate.now().toString();
            stringRedisTemplate.opsForValue().increment("shike:ai:plan:count:" + todayStr);
            stringRedisTemplate.opsForValue().increment("shike:ai:plan:tokens:" + todayStr, 3500); // 平均约 3500 Tokens/次
        } catch (Exception e) {
            log.warn("Failed to record plan AI usage in Redis: {}", e.getMessage());
        }
    }
}
