package com.shike.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shike.common.BizException;
import com.shike.model.entity.PointsRecord;
import com.shike.model.entity.User;
import com.shike.repository.PointsRecordRepository;
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
    private final PointsRecordRepository pointsRecordRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final com.shike.service.AdminService adminService;

    @Value("${ai.provider:OPENAI}")
    private String aiProvider;

    @Value("${ai.api-key:MOCK_KEY}")
    private String aiApiKey;

    @Value("${ai.endpoint:https://api.openai.com/v1/chat/completions}")
    private String aiEndpoint;

    @Value("${ai.plan-model:${ai.model:qwen3.8-max}}")
    private String aiModel;

    @Value("${ai.timeout-ms:120000}")
    private Integer aiTimeoutMs;

    private static final String REDIS_PLAN_KEY_PREFIX = "shike:user:plan:";

    @Override
    public Map<String, Object> getPlanStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "找不到该用户档案"));
        String cacheKey = REDIS_PLAN_KEY_PREFIX + userId;
        boolean hasCachedPlan = Boolean.TRUE.equals(stringRedisTemplate.hasKey(cacheKey));
        boolean hasGeneratedBefore = pointsRecordRepository.existsByUserIdAndType(userId, "PLAN_GEN");

        Map<String, Object> statusMap = new HashMap<>();
        statusMap.put("hasPlan", hasCachedPlan);
        statusMap.put("isFirstTime", !hasGeneratedBefore);
        statusMap.put("userPoints", user.getPoints() != null ? user.getPoints() : 0);
        return statusMap;
    }

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

        // 3. 校验并扣除积分 (首次免费，以后每次扣除 100 积分)
        boolean hasGeneratedBefore = pointsRecordRepository.existsByUserIdAndType(userId, "PLAN_GEN");
        if (hasGeneratedBefore) {
            int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
            if (currentPoints < 100) {
                throw new BizException(400, "契约积分不足 100 分（当前可用余额: " + currentPoints + " 分）。可以通过每日签到或参与挑战小队赚取积分！");
            }
            user.setPoints(currentPoints - 100);
            userRepository.save(user);

            PointsRecord record = PointsRecord.builder()
                    .userId(userId)
                    .amount(-100)
                    .type("PLAN_GEN")
                    .remark("AI 定制运动与饮食计划 (消耗 100 积分)")
                    .build();
            pointsRecordRepository.save(record);
            log.info("Deducted 100 points for user {} for AI plan generation. Remaining balance: {}", userId, user.getPoints());
        } else {
            // 首次免费记录
            PointsRecord record = PointsRecord.builder()
                    .userId(userId)
                    .amount(0)
                    .type("PLAN_GEN")
                    .remark("AI 定制运动与饮食计划 (首次生成免费)")
                    .build();
            pointsRecordRepository.save(record);
            log.info("First time AI plan generation for user {}, free of charge.", userId);
        }

        // 4. 构建专属 AI Prompt (融合专家知识库准则)
        Map<String, Object> planMap;
        try {
            String prompt = buildExpertPrompt(user);
            String aiResponseJson = callTextLlm(prompt);
            planMap = parseAndCleanJson(aiResponseJson);
        } catch (Exception e) {
            log.error("AI Generation failed for user {}, fallback to template plan: {}", userId, e.getMessage());
            planMap = generateScientificFallbackPlan(user);
        }
        recordPlanAiUsage(userId);

        // 附带返回用户最新积分信息，方便前端实时同步
        planMap.put("userPoints", user.getPoints() != null ? user.getPoints() : 0);

        // 5. 写入缓存 (保留 7 天)
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
        boolean isFemale = genderStr.equals("女");
        int age = (user.getAge() != null) ? user.getAge() : 25;
        double height = (user.getHeight() != null) ? user.getHeight().doubleValue() : 175.0;
        double weight = (user.getWeight() != null) ? user.getWeight().doubleValue() : 70.0;
        double bodyFat = (user.getCurrentBodyFat() != null) ? user.getCurrentBodyFat().doubleValue() : (isFemale ? 24.0 : 18.0);
        double bmr = (user.getBmr() != null) ? user.getBmr().doubleValue() : 1600.0;
        double tdee = (user.getTdee() != null) ? user.getTdee().doubleValue() : 2200.0;
        double targetCal = (user.getTargetCalories() != null) ? user.getTargetCalories().doubleValue() : 2000.0;

        String goal = (user.getGoal() != null) ? user.getGoal() : "MAINTAIN";
        String goalLabel = translateGoal(goal, user.getCustomGoalType());
        String activityLabel = translateActivity(user.getActivityLevel());

        String trainingLevel = (user.getTrainingLevel() != null) ? user.getTrainingLevel() : "BEGINNER";
        String trainingLevelLabel;
        String trainingLevelGuidance;
        switch (trainingLevel.toUpperCase()) {
            case "NOVICE":
                trainingLevelLabel = "新手进阶 (规律健身 6个月~1.5年)";
                trainingLevelGuidance = "【训练经验-新手进阶】采用上下肢分化(Upper/Lower) 3-4天/周，注重动作规范与线性渐进超载。周训练容量控制在 10-14 组/肌群，RIR 2-3 (保留2-3次次次数)。";
                break;
            case "INTERMEDIATE":
                trainingLevelLabel = "中级玩家 (规律健身 1.5~3年)";
                trainingLevelGuidance = "【训练经验-中级玩家】采用推拉腿(PPL)或四分化 4-5天/周，注重容量累积与强度提升。周训练容量控制在 14-18 组/肌群(MAV甜蜜区)，RIR 1-2。";
                break;
            case "ADVANCED":
                trainingLevelLabel = "资深老炮 (规律健身 3年以上)";
                trainingLevelGuidance = "【训练经验-资深老炮】采用高容量部位专项分化 5-6天/周，注重肌群雕刻与周期化调控。周训练容量可达 16-20+ 组/肌群(MAV~MRV)，RIR 0-2 (允许部分动作达到完全力竭)。";
                break;
            default:
                trainingLevelLabel = "小白零基础 (未规律健身或 <6个月)";
                trainingLevelGuidance = "【训练经验-小白零基础】必须采用全身训练(Full Body) 3天/周，以基础复合动作(深蹲/卧推/划船/快走)建立动作模式。周训练容量严格控制在 8-10 组/肌群(最小有效容量MEV)，RIR 3-4 (保留较多体力，防酸痛过重与受伤)。";
                break;
        }

        // === 1. 基于体脂率的蛋白质系数与有氧比例动态调节 (出处: Eric Helms / RP) ===
        double proteinCoeff;
        String bodyFatTier;
        String cardioGuidance;
        String trainingVolumeGuidance;
        String trainingSplitGuidance;

        if (isFemale) {
            if (bodyFat > 35) {
                bodyFatTier = "高体脂减脂期";
                proteinCoeff = 1.6;
                cardioGuidance = "有氧占比35%，力量占比65%。优先低冲击有氧(快走/椭圆机/游泳)，每次25-35分钟，3-4次/周";
                trainingVolumeGuidance = "每肌群8-10组/周(维持容量MV)，全身或上下肢分化，RIR 3(保留3次)";
                trainingSplitGuidance = "全身训练3天/周 或 上下肢分化3-4天/周";
            } else if (bodyFat > 25) {
                bodyFatTier = "标准减脂期";
                proteinCoeff = 1.8;
                cardioGuidance = "有氧占比30%，力量占比70%。Zone 2稳态有氧3-4次/周×25-35分钟";
                trainingVolumeGuidance = "每肌群10-14组/周(MEV~MAV)，推拉腿或上下肢分化，RIR 2";
                trainingSplitGuidance = "上下肢分化4天/周 或 推拉腿5天/周";
            } else if (bodyFat > 20) {
                bodyFatTier = "体重重构/精雕期";
                proteinCoeff = 1.8;
                cardioGuidance = "有氧占比25%，力量占比75%。Zone 2有氧2-3次/周×20-30分钟";
                trainingVolumeGuidance = "每肌群12-16组/周(MAV)，推拉腿分化，RIR 1-2";
                trainingSplitGuidance = "推拉腿(PPL)5天/周";
            } else {
                bodyFatTier = "低体脂维持/增肌期";
                proteinCoeff = 2.0;
                cardioGuidance = "有氧占比15%，力量占比85%。有氧仅用于心血管健康，每周2次×20分钟";
                trainingVolumeGuidance = "每肌群14-20组/周(MAV~MRV)，高容量分化训练，RIR 0-2";
                trainingSplitGuidance = "推拉腿(PPL)或肌群专项5-6天/周";
            }
        } else {
            if (bodyFat > 25) {
                bodyFatTier = "高体脂减脂期";
                proteinCoeff = 1.8;
                cardioGuidance = "有氧占比35%，力量占比65%。优先Zone 2稳态(快走/慢跑/单车)，每次25-40分钟，3-4次/周";
                trainingVolumeGuidance = "每肌群8-10组/周(维持容量MV~MEV)，复合动作为主，RIR 2-3";
                trainingSplitGuidance = "全身训练3天/周 或 上下肢分化4天/周";
            } else if (bodyFat > 18) {
                bodyFatTier = "标准减脂期";
                proteinCoeff = 1.8;
                cardioGuidance = "有氧占比30%，力量占比70%。Zone 2稳态3-4次/周×25-35分钟，可选1次HIIT";
                trainingVolumeGuidance = "每肌群10-14组/周(MEV~MAV)，推拉腿或上下肢分化，RIR 2";
                trainingSplitGuidance = "上下肢分化4天/周 或 推拉腿5天/周";
            } else if (bodyFat > 13) {
                bodyFatTier = "体重重构/精雕期";
                proteinCoeff = 2.0;
                cardioGuidance = "有氧占比25%，力量占比75%。Zone 2有氧2-3次/周×20-30分钟";
                trainingVolumeGuidance = "每肌群14-18组/周(MAV)，推拉腿分化，RIR 1-2";
                trainingSplitGuidance = "推拉腿(PPL)5天/周";
            } else {
                bodyFatTier = "低体脂维持/增肌期";
                proteinCoeff = 2.2;
                cardioGuidance = "有氧占比15%，力量占比85%。有氧仅用于心血管健康，每周2次×20分钟";
                trainingVolumeGuidance = "每肌群16-20+组/周(MAV~MRV)，高容量分化训练，RIR 0-2";
                trainingSplitGuidance = "推拉腿(PPL)或肌群专项5-6天/周";
            }
        }

        // === 2. 基于年龄段的训练强度与安全调节 (出处: Peter Attia / NSCA) ===
        String ageGuidance;
        if (age < 30) {
            ageGuidance = "可承受较高训练密度与容量，优先建立正确动作模式与渐进超载习惯";
        } else if (age < 40) {
            ageGuidance = "增加热身时间至8-10分钟，注意肌腱恢复，同肌群严格间隔48h以上";
        } else if (age < 50) {
            ageGuidance = "减少1RM最大力量冲刺，以8-12RM容量训练为主。增加关节预热与动态拉伸。优先稳定性与平衡训练";
        } else {
            ageGuidance = "优先功能性训练+平衡+柔韧+骨密度保护。动作选择偏向低冲击关节友好型。降低爆发性运动比例，增加核心稳定训练";
        }

        // === 3. 基于目标类型的专项规则 (出处: Eric Helms / Stronger By Science / NSCA) ===
        String goalSpecificRules;
        switch (goal) {
            case "LOSE_WEIGHT":
                goalSpecificRules = "【减脂专项】保肌减脂 > 疯狂燃脂。力量训练重量不主动降低(维持肌肉信号)，容量可略降至MV~MEV。有氧以Zone 2稳态为主，避免过度HIIT导致皮质醇飙升。每日步数目标8000-10000步以锁定NEAT";
                break;
            case "GAIN_MUSCLE":
                goalSpecificRules = "【增肌专项】渐进超载+充足恢复 > 追求酸痛。训练容量从MEV逐周递增至MAV。有氧最小化(避免干扰效应)，每周≤3次×20分钟低强度。碳水集中在训练前1h与训练后2h内";
                break;
            case "ABS":
                goalSpecificRules = "【腹肌塑形专项】腹肌=低体脂+腹直肌肥大，两者缺一不可。大肌群维持容量训练保肌，额外增加每周3次直接腹肌专项(悬垂举腿3组×15次+死虫式/平板撑3组)。Zone 2有氧4-5次/周×30分钟深度燃脂";
                break;
            case "PERIOD":
                goalSpecificRules = "【周期冲刺专项】采用波动周期化(Undulating Periodization)，每周内交替安排力量日(低次数高重量)与肌耐力日(高次数中重量)。严格控制每周减重速度≤体重的1%";
                break;
            default:
                goalSpecificRules = "【维持专项】一致性 > 强度。每周3天全身训练维持容量(MV)，搭配2-3次低强度有氧。防止退步是第一优先级";
                break;
        }

        // === 4. Skinny-Fat 特殊检测 (出处: Eric Helms) ===
        double bmi = weight / Math.pow(height / 100.0, 2);
        boolean isSkinnyFat = bmi < 24 && ((isFemale && bodyFat > 28) || (!isFemale && bodyFat > 20));
        String skinnyFatOverride = "";
        if (isSkinnyFat) {
            skinnyFatOverride = "\n⚠️【Skinny-Fat体型特殊处理】: 该用户BMI正常但体脂偏高，属于典型Skinny-Fat体型。策略：优先增肌重构(轻微盈余TDEE×1.05)而非激进减脂。以复合力量训练为核心(深蹲/硬拉/卧推)，避免过度有氧导致肌肉量进一步流失。";
        }

        // === 5. 性别差异化微调 (出处: Menno Henselmans / NSCA) ===
        String genderAdjustment = "";
        if (isFemale) {
            proteinCoeff = Math.max(proteinCoeff - 0.2, 1.6);
            genderAdjustment = "\n【女性训练微调】: 女性中枢疲劳恢复更快，可适当提高训练频率(每肌群2-3次/周)。组间休息可缩短至60-90秒。有氧类型优先推荐关节友好型(快走/椭圆机/游泳)。";
        }

        // 最终计算三大营养素
        int proteinG = (int) Math.round(weight * proteinCoeff);
        int fatG = (int) Math.round((targetCal * 0.25) / 9.0);
        int carbsG = (int) Math.round((targetCal - (proteinG * 4) - (fatG * 9)) / 4.0);
        if (carbsG < 80) carbsG = 80; // 碳水最低安全线

        StringBuilder sb = new StringBuilder();
        sb.append("你是一位拥有 15 年经验的资深注册营养师 (RD) 和国际体能训练专家 (CSCS)，同时精通 Renaissance Periodization 容量管理体系。\n");
        sb.append("请根据以下用户的身体数据、人群分层标签及专家知识库准则，生成高度个性化的【7天运动周计划】与【每日4餐膳食建议】。\n\n");

        sb.append("【用户基本档案】:\n");
        sb.append("- 性别: ").append(genderStr).append(", 年龄: ").append(age).append("岁, 身高: ").append(height).append("cm, 体重: ").append(weight).append("kg, 体脂率: ").append(bodyFat).append("%, BMI: ").append(String.format("%.1f", bmi)).append("\n");
        sb.append("- 训练经验等级: ").append(trainingLevelLabel).append("\n");
        sb.append("- 当前活动水平: ").append(activityLabel).append("\n");
        sb.append("- 基础代谢 BMR: ").append(bmr).append(" kcal, 每日消耗 TDEE: ").append(tdee).append(" kcal\n");
        sb.append("- 设定目标: ").append(goalLabel).append("\n");
        sb.append("- 每日目标摄入热量: ").append((int) targetCal).append(" kcal (推荐三大营养素: 蛋白质 ").append(proteinG).append("g [").append(proteinCoeff).append("g/kg], 碳水 ").append(carbsG).append("g, 脂肪 ").append(fatG).append("g)\n\n");

        sb.append("【用户人群分层标签 (已由系统自动判定)】:\n");
        sb.append("- 训练经验准则: ").append(trainingLevelGuidance).append("\n");
        sb.append("- 体脂分群: ").append(bodyFatTier).append("\n");
        sb.append("- 训练分化建议: ").append(trainingSplitGuidance).append("\n");
        sb.append("- 训练容量指导: ").append(trainingVolumeGuidance).append("\n");
        sb.append("- 有氧配比指导: ").append(cardioGuidance).append("\n");
        sb.append("- 年龄段注意事项: ").append(ageGuidance).append("\n");
        sb.append(skinnyFatOverride);
        sb.append(genderAdjustment);
        sb.append("\n\n");

        sb.append("【目标专项规则 (必须严格遵守)】:\n");
        sb.append(goalSpecificRules).append("\n\n");

        sb.append("【1. 统一减脂期训练容量与强度规则 (RP Volume Landmarks)】:\n");
        sb.append("- 大肌群容量: 每周 10-14 个有效训练组 (控制在 MAV 内保肌与保证恢复)\n");
        sb.append("- 小肌群容量: 每周 6-10 个有效训练组\n");
        sb.append("- 训练强度: 70%-85% 1RM，力量训练保持重量不主动降低\n");
        sb.append("- RIR (保留次数): 复合动作 RIR 2 (保留2次)，孤立动作 RIR 1-2 (保留1-2次)\n\n");

        sb.append("【2. 训练动作生成规则 (严禁笼统描述)】:\n");
        sb.append("- 每个训练日必须拆分为 3-5 个具体动作\n");
        sb.append("- 每个动作必须明确: 1.动作名称 2.训练组数 3.次数范围 4.RIR 5.时长(分钟) 6.热量消耗(kcal)\n");
        sb.append("- 严禁出现 '力量训练'、'胸部训练'、'腿部训练' 等笼统文字，必须具体到如 '哑铃卧推 💪 4组x12次 (RIR 2)'！\n\n");

        sb.append("【3. 渐进超负荷与恢复管理规则】:\n");
        sb.append("- 渐进超负荷: 完成目标次数且 RIR≥2 时，上肢下次 +1-2kg，下肢 +2.5-5kg；若未完成最低次数，保持重量或降低 5%\n");
        sb.append("- 恢复管理: 力量训练 4-5天/周，Zone2 有氧 3-4次/周 (单次25-35min)，安排 1-2天 休息/主动恢复\n");
        sb.append("- 训练时长控制: 力量训练 45-70min，Zone2 25-35min，单次总时长 ≤ 90分钟\n");
        sb.append("- 减脂速度控制: 每周减重目标 0.5%-1%，每日热量缺口控制在 500-700kcal\n\n");

        sb.append("【4. 饮食与食材替换规则】:\n");
        sb.append("- 每日 4 餐: 早餐、午餐、下午加餐、晚餐，每餐包含 蛋白+碳水+蔬菜+热量\n");
        sb.append("- 手掌估算: 蛋白质 1-2掌心，碳水 1手心，蔬菜 1拳头\n");
        sb.append("- 禁止连续 7 天出现相同主食和蛋白质；每餐在说明中给出食材替换方案 (如 鸡蛋=虾仁=牛肉=鸡胸肉; 米饭=红薯=紫米饭=燕麦)\n\n");

        sb.append("【请严格按以下固定 JSON 格式输出，不允许输出任何解释文字或 markdown 标记】:\n");
        sb.append("{\n");
        sb.append("  \"user_summary\": {\n");
        sb.append("    \"goal\": \"").append(goalLabel).append("\",\n");
        sb.append("    \"target_calories\": ").append((int) targetCal).append(",\n");
        sb.append("    \"protein\": ").append(proteinG).append(",\n");
        sb.append("    \"carbs\": ").append(carbsG).append(",\n");
        sb.append("    \"fat\": ").append(fatG).append("\n");
        sb.append("  },\n");
        sb.append("  \"weekly_training\": [\n");
        sb.append("    {\n");
        sb.append("      \"day\": \"周一\",\n");
        sb.append("      \"training_type\": \"胸大肌与三头力量 + Zone2慢跑\",\n");
        sb.append("      \"total_duration\": 60,\n");
        sb.append("      \"items\": [\n");
        sb.append("        { \"name\": \"哑铃卧推 💪\", \"duration_min\": 15, \"calorie_kcal\": 90, \"sets_reps_rir\": \"4组x12次 (RIR 2)\" },\n");
        sb.append("        { \"name\": \"上斜哑铃推举 💪\", \"duration_min\": 12, \"calorie_kcal\": 70, \"sets_reps_rir\": \"3组x12次 (RIR 2)\" },\n");
        sb.append("        { \"name\": \"双杠臂屈伸 💪\", \"duration_min\": 10, \"calorie_kcal\": 60, \"sets_reps_rir\": \"3组x10次 (RIR 1-2)\" },\n");
        sb.append("        { \"name\": \"Zone 2 慢跑 🏃‍♂️\", \"duration_min\": 25, \"calorie_kcal\": 180, \"sets_reps_rir\": \"心率维持 60-70% 最大心率\" }\n");
        sb.append("      ]\n");
        sb.append("    }\n");
        sb.append("    // ... 周二至周日共7天\n");
        sb.append("  ],\n");
        sb.append("  \"weekly_diet\": [\n");
        sb.append("    {\n");
        sb.append("      \"day\": \"周一\",\n");
        sb.append("      \"total_calories\": ").append((int) targetCal).append(",\n");
        sb.append("      \"meals\": [\n");
        sb.append("        { \"meal\": \"早餐\", \"foods\": \"小黄米粥 1碗 + 水煮蛋 1个 + 凉拌黄瓜 100g (替换: 燕麦/黑豆浆)\", \"hand_size_reference\": \"1掌心蛋白+1手心碳水\" },\n");
        sb.append("        { \"meal\": \"午餐\", \"foods\": \"芹菜炒牛肉 150g + 杂粮饭 150g + 蒜蓉西兰花 150g (替换: 清蒸鲈鱼/鸡胸肉)\", \"hand_size_reference\": \"1.5掌心蛋白+1拳头蔬菜+1手心碳水\" },\n");
        sb.append("        { \"meal\": \"下午加餐\", \"foods\": \"无糖酸奶 150g + 香蕉 1根\", \"hand_size_reference\": \"1根香蕉或100g酸奶\" },\n");
        sb.append("        { \"meal\": \"晚餐\", \"foods\": \"清蒸鲈鱼 150g + 蒸红薯 100g + 番茄豆腐汤 1碗 (替换: 豆腐/虾仁)\", \"hand_size_reference\": \"1掌心蛋白+1.5拳头蔬菜\" }\n");
        sb.append("      ]\n");
        sb.append("    }\n");
        sb.append("    // ... 周二至周日共7天，主食与蛋白不重样\n");
        sb.append("  ]\n");
        sb.append("}\n");

        return sb.toString();
    }

    private String callTextLlm(String prompt) throws Exception {
        String activeModel = aiModel;
        try {
            if (adminService != null && adminService.getAiModelConfig() != null) {
                String dynamicModel = adminService.getAiModelConfig().get("planModel");
                if (dynamicModel != null && !dynamicModel.isBlank()) {
                    activeModel = dynamicModel;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch dynamic plan model, using default: {}", e.getMessage());
        }

        log.info("Calling Text LLM endpoint: {}, active model: {}", aiEndpoint, activeModel);

        Map<String, Object> systemMsg = Map.of("role", "system", "content", "你是一位严格输出标准JSON的专业营养师与健身教练AI。");
        Map<String, Object> userMsg = Map.of("role", "user", "content", prompt);

        Map<String, Object> payload;
        if (activeModel.contains("qwen")) {
            payload = Map.of(
                    "model", activeModel,
                    "messages", List.of(systemMsg, userMsg),
                    "temperature", 0.5,
                    "max_tokens", 3500
            );
        } else {
            payload = Map.of(
                    "model", activeModel,
                    "messages", List.of(systemMsg, userMsg),
                    "temperature", 0.6,
                    "max_tokens", 3500
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

        Map<String, Object> usage = (Map<String, Object>) respMap.get("usage");
        if (usage != null) {
            Object promptTok = usage.get("prompt_tokens");
            Object compTok = usage.get("completion_tokens");
            Object totalTok = usage.get("total_tokens");
            log.info("[AI Token Audit] Module: [专属AI运动与饮食计划生成] | Model: {} | Prompt Tokens: {} | Completion Tokens: {} | Total Tokens: {}",
                    activeModel, promptTok, compTok, totalTok);
        }

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

        Map<String, Object> map = objectMapper.readValue(jsonText, new TypeReference<Map<String, Object>>() {});
        
        // 自动适配固定 JSON 输出结构与前端字段映射
        if (map.containsKey("user_summary") && !map.containsKey("nutritionOverview")) {
            Map<String, Object> userSum = (Map<String, Object>) map.get("user_summary");
            Map<String, Object> overview = new HashMap<>();
            overview.put("targetCal", userSum.get("target_calories"));
            overview.put("proteinG", userSum.get("protein"));
            overview.put("carbsG", userSum.get("carbs"));
            overview.put("fatG", userSum.get("fat"));
            map.put("nutritionOverview", overview);
            map.put("summary", "【" + userSum.get("goal") + "】每日目标热量 " + userSum.get("target_calories") + " kcal (蛋白质 " + userSum.get("protein") + "g, 碳水 " + userSum.get("carbs") + "g, 脂肪 " + userSum.get("fat") + "g)");
        }
        
        if (map.containsKey("weekly_training") && !map.containsKey("workoutPlan")) {
            List<Map<String, Object>> weeklyTraining = (List<Map<String, Object>>) map.get("weekly_training");
            List<Map<String, Object>> workoutPlan = new ArrayList<>();
            for (Map<String, Object> day : weeklyTraining) {
                Map<String, Object> dayMap = new HashMap<>();
                dayMap.put("day", day.get("day"));
                String tType = (String) day.get("training_type");
                dayMap.put("focus", tType);
                dayMap.put("isRestDay", "休息日".equals(tType) || "主动恢复".equals(tType) || "完全休息".equals(tType));
                
                List<Map<String, Object>> items = new ArrayList<>();
                List<Map<String, Object>> rawItems = (List<Map<String, Object>>) day.get("items");
                if (rawItems != null) {
                    for (Map<String, Object> rawItem : rawItems) {
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("name", rawItem.get("name"));
                        itemMap.put("duration", rawItem.get("duration_min"));
                        itemMap.put("calories", rawItem.get("calorie_kcal"));
                        itemMap.put("detail", rawItem.get("sets_reps_rir"));
                        items.add(itemMap);
                    }
                }
                dayMap.put("items", items);
                workoutPlan.add(dayMap);
            }
            map.put("workoutPlan", workoutPlan);
        }

        if (map.containsKey("weekly_diet") && !map.containsKey("dietPlan")) {
            List<Map<String, Object>> weeklyDiet = (List<Map<String, Object>>) map.get("weekly_diet");
            List<Map<String, Object>> dietPlan = new ArrayList<>();
            for (Map<String, Object> day : weeklyDiet) {
                Map<String, Object> dayMap = new HashMap<>();
                dayMap.put("day", day.get("day"));
                List<Map<String, Object>> meals = (List<Map<String, Object>>) day.get("meals");
                if (meals != null) {
                    for (Map<String, Object> meal : meals) {
                        String mealName = (String) meal.get("meal");
                        Map<String, Object> mealObj = new HashMap<>();
                        mealObj.put("title", mealName);
                        mealObj.put("portionHint", meal.get("hand_size_reference"));
                        mealObj.put("foods", List.of((String) meal.get("foods")));
                        mealObj.put("calories", 400);
                        if (mealName.contains("早")) dayMap.put("breakfast", mealObj);
                        else if (mealName.contains("午")) dayMap.put("lunch", mealObj);
                        else if (mealName.contains("晚")) dayMap.put("dinner", mealObj);
                        else dayMap.put("snack", mealObj);
                    }
                }
                dietPlan.add(dayMap);
            }
            map.put("dietPlan", dietPlan);
        }

        return map;
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
     * 当 AI 接口超时或不可用时的保底科学计划，确保功能永不断供 (全7天中式不重样食谱)
     */
    private Map<String, Object> generateScientificFallbackPlan(User user) {
        double weight = (user.getWeight() != null) ? user.getWeight().doubleValue() : 70.0;
        double targetCal = (user.getTargetCalories() != null) ? user.getTargetCalories().doubleValue() : 2000.0;
        String goal = (user.getGoal() != null) ? user.getGoal() : "MAINTAIN";

        int proteinG = (int) Math.round(weight * (goal.equals("GAIN_MUSCLE") || goal.equals("ABS") ? 2.0 : 1.8));
        int fatG = (int) Math.round((targetCal * 0.25) / 9.0);
        int carbsG = (int) Math.round((targetCal - (proteinG * 4) - (fatG * 9)) / 4.0);

        Map<String, Object> res = new HashMap<>();
        res.put("summary", "根据您的档案与RD/CSCS专家知识库，为您制订了7天接地气中式家常健身膳食与运动方案。");

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
                items.add(Map.of("name", "哑铃卧推 💪", "duration", 12, "calories", 70, "detail", "4组x12次 (RIR 2) 目标胸大肌"));
                items.add(Map.of("name", "上斜哑铃推举 💪", "duration", 10, "calories", 60, "detail", "3组x12次 专注上胸"));
                items.add(Map.of("name", "双杠臂屈伸 💪", "duration", 10, "calories", 50, "detail", "3组x10次 下胸与三头"));
                items.add(Map.of("name", "慢跑 🏃‍♂️", "duration", 25, "calories", 180, "detail", "心率维持 Zone 2 稳态"));
            } else if (i == 1) {
                items.add(Map.of("name", "杠铃深蹲 💪", "duration", 15, "calories", 90, "detail", "4组x10次 股四头肌与臀肌"));
                items.add(Map.of("name", "罗马尼亚硬拉 💪", "duration", 12, "calories", 80, "detail", "3组x12次 腘绳肌与臀部"));
                items.add(Map.of("name", "坐姿腿屈伸 💪", "duration", 10, "calories", 50, "detail", "3组x15次 目标股四孤立"));
                items.add(Map.of("name", "下肢拉伸 🧘", "duration", 10, "calories", 30, "detail", "静态放松腘绳肌与髂胫束"));
            } else if (i == 2) {
                items.add(Map.of("name", "户外散步 🚶", "duration", 30, "calories", 100, "detail", "低速散步，促进血液循环与恢复"));
                items.add(Map.of("name", "筋膜松解 🧘", "duration", 15, "calories", 40, "detail", "泡沫轴滚压全身大肌群"));
            } else if (i == 3) {
                items.add(Map.of("name", "高位下拉 💪", "duration", 12, "calories", 70, "detail", "4组x10-12次 背阔肌上束"));
                items.add(Map.of("name", "坐姿划船 💪", "duration", 12, "calories", 60, "detail", "3组x12次 中背厚度"));
                items.add(Map.of("name", "哑铃弯举 💪", "duration", 10, "calories", 50, "detail", "3组x12次 肱二头肌"));
                items.add(Map.of("name", "动感单车 🚲", "duration", 20, "calories", 160, "detail", "中等阻力稳态骑行"));
            } else if (i == 4) {
                items.add(Map.of("name", "哑铃推举 💪", "duration", 12, "calories", 60, "detail", "4组x10-12次 三角肌前中束"));
                items.add(Map.of("name", "哑铃侧平举 💪", "duration", 10, "calories", 50, "detail", "4组x15次 打造肩宽"));
                items.add(Map.of("name", "悬垂举腿 🧘", "duration", 10, "calories", 40, "detail", "3组x15次 核心腹直肌"));
                items.add(Map.of("name", "坡度快走 🚶‍♂️", "duration", 25, "calories", 140, "detail", "坡度5% 速度5.5km/h 燃脂"));
            } else if (i == 5) {
                items.add(Map.of("name", "水下游泳 🏊", "duration", 30, "calories", 220, "detail", "自由泳/蛙泳交替 全身关节低冲击"));
                items.add(Map.of("name", "波比跳HIIT ⚡", "duration", 15, "calories", 120, "detail", "45秒运动+15秒间歇 4轮"));
            } else {
                items.add(Map.of("name", "神经系统恢复 😴", "duration", 0, "calories", 0, "detail", "完全休息日 充分睡眠准备下周超载"));
            }
            dayMap.put("items", items);
            workoutList.add(dayMap);
        }
        res.put("workoutPlan", workoutList);

        // 7天中式不重样家常膳食保底方案
        List<Map<String, Object>> dietList = new ArrayList<>();
        
        // 周一
        dietList.add(Map.of(
                "day", "周一",
                "breakfast", Map.of("title", "小米粥蛋餐", "calories", 380, "protein", 22, "carbs", 45, "fat", 10, "portionHint", "1掌心蛋白+1手心碳水", "foods", List.of("小黄米粥 1碗", "水煮蛋 1个", "凉拌黄瓜 100g")),
                "lunch", Map.of("title", "芹菜牛肉餐", "calories", 650, "protein", 42, "carbs", 65, "fat", 18, "portionHint", "1.5掌心蛋白+1拳头蔬菜+1手心杂粮饭", "foods", List.of("芹菜炒牛肉 150g", "杂粮饭 150g", "蒜蓉西兰花 150g")),
                "dinner", Map.of("title", "清蒸鲈鱼餐", "calories", 450, "protein", 35, "carbs", 40, "fat", 12, "portionHint", "1掌心鱼肉+1.5拳头蔬菜", "foods", List.of("清蒸鲈鱼 150g", "蒸红薯 100g", "番茄豆腐汤 1碗")),
                "snack", Map.of("title", "练前酸奶", "calories", 180, "protein", 10, "carbs", 22, "fat", 4, "portionHint", "1根香蕉或100g酸奶", "foods", List.of("无糖酸奶 150g", "香蕉 1根"))
        ));

        // 周二
        dietList.add(Map.of(
                "day", "周二",
                "breakfast", Map.of("title", "燕麦黑豆浆", "calories", 390, "protein", 24, "carbs", 48, "fat", 11, "portionHint", "1掌心蛋白+1手心燕麦", "foods", List.of("无糖黑豆浆 300ml", "煮玉米 1根", "煎蛋 1个")),
                "lunch", Map.of("title", "彩椒鸡丁餐", "calories", 640, "protein", 40, "carbs", 60, "fat", 16, "portionHint", "1.5掌心蛋白+1拳头彩椒+1手心紫米饭", "foods", List.of("少油彩椒炒鸡丁 150g", "紫米饭 150g", "手撕包菜 150g")),
                "dinner", Map.of("title", "木耳炒肉片", "calories", 460, "protein", 32, "carbs", 38, "fat", 15, "portionHint", "1掌心瘦肉+1.5拳头木耳菌菇", "foods", List.of("木耳炒瘦肉片 130g", "蒸南瓜 120g", "清炒空心菜 150g")),
                "snack", Map.of("title", "坚果水果", "calories", 190, "protein", 6, "carbs", 20, "fat", 8, "portionHint", "1小把坚果", "foods", List.of("混合坚果 15g", "苹果 0.5个"))
        ));

        // 周三
        dietList.add(Map.of(
                "day", "周三",
                "breakfast", Map.of("title", "全麦红豆粥", "calories", 370, "protein", 20, "carbs", 50, "fat", 9, "portionHint", "1掌心蛋白+1手心红豆粥", "foods", List.of("红豆薏米粥 1碗", "茶叶蛋 1个", "小咸菜少许")),
                "lunch", Map.of("title", "番茄炒蛋牛肉餐", "calories", 660, "protein", 38, "carbs", 68, "fat", 19, "portionHint", "1.5掌心蛋白+1拳头番茄+1手心糙米饭", "foods", List.of("少油番茄炒蛋 1.5份", "卤牛肉 80g", "糙米饭 150g")),
                "dinner", Map.of("title", "家常豆腐鲜虾", "calories", 440, "protein", 36, "carbs", 35, "fat", 13, "portionHint", "1掌心虾仁豆腐+1.5拳头小白菜", "foods", List.of("鲜虾炖豆腐 150g", "煮土豆块 100g", "清炒小白菜 150g")),
                "snack", Map.of("title", "清爽黄瓜豆浆", "calories", 160, "protein", 12, "carbs", 15, "fat", 3, "portionHint", "1杯豆浆", "foods", List.of("无糖豆浆 250ml", "水果黄瓜 1根"))
        ));

        // 周四
        dietList.add(Map.of(
                "day", "周四",
                "breakfast", Map.of("title", "蒸薯蛋奶餐", "calories", 400, "protein", 25, "carbs", 46, "fat", 12, "portionHint", "1掌心蛋白+1手心紫薯", "foods", List.of("鲜牛奶 250ml", "蒸紫薯 100g", "水煮蛋 1个")),
                "lunch", Map.of("title", "香菇炖鸡餐", "calories", 650, "protein", 41, "carbs", 62, "fat", 17, "portionHint", "1.5掌心去皮鸡肉+1手心黑米饭", "foods", List.of("香菇炖去皮鸡块 150g", "黑米饭 150g", "上汤娃娃菜 150g")),
                "dinner", Map.of("title", "白灼虾高纤餐", "calories", 430, "protein", 34, "carbs", 36, "fat", 11, "portionHint", "1掌心白灼虾+1.5拳头西兰花", "foods", List.of("白灼鲜虾 120g", "蒸山药 100g", "白灼西兰花 150g")),
                "snack", Map.of("title", "低脂全脂奶", "calories", 170, "protein", 9, "carbs", 18, "fat", 5, "portionHint", "1杯牛奶", "foods", List.of("低脂牛奶 200ml", "猕猴桃 1个"))
        ));

        // 周五
        dietList.add(Map.of(
                "day", "周五",
                "breakfast", Map.of("title", "南瓜粥煎蛋", "calories", 385, "protein", 21, "carbs", 49, "fat", 10, "portionHint", "1掌心蛋白+1手心南瓜", "foods", List.of("老南瓜粥 1碗", "少油煎蛋白 2个", "凉拌木耳 80g")),
                "lunch", Map.of("title", "洋葱炒猪瘦肉", "calories", 655, "protein", 39, "carbs", 64, "fat", 18, "portionHint", "1.5掌心瘦肉+1拳头洋葱+1手心红豆饭", "foods", List.of("洋葱炒瘦猪肉 140g", "红豆饭 150g", "清炒菠菜 150g")),
                "dinner", Map.of("title", "鲫鱼豆腐汤", "calories", 420, "protein", 33, "carbs", 32, "fat", 14, "portionHint", "1掌心鱼肉豆腐+1.5拳头冬瓜", "foods", List.of("鲫鱼豆腐汤 1碗", "蒸芋头 100g", "清炒冬瓜 150g")),
                "snack", Map.of("title", "番茄小圣女果", "calories", 150, "protein", 5, "carbs", 22, "fat", 2, "portionHint", "1把圣女果", "foods", List.of("圣女果 150g", "无糖绿茶 1杯"))
        ));

        // 周六
        dietList.add(Map.of(
                "day", "周六",
                "breakfast", Map.of("title", "全麦馒头豆浆", "calories", 410, "protein", 23, "carbs", 52, "fat", 11, "portionHint", "1掌心蛋白+1手心全麦馒头", "foods", List.of("无糖豆浆 300ml", "全麦小馒头 1个", "蒸蛋羹 1碗")),
                "lunch", Map.of("title", "青椒炒鸡胸肉", "calories", 630, "protein", 43, "carbs", 61, "fat", 15, "portionHint", "1.5掌心鸡胸+1手心玉米饭", "foods", List.of("青椒炒鸡胸肉片 150g", "玉米饭 150g", "炒凉拌西葫芦 150g")),
                "dinner", Map.of("title", "清蒸龙利鱼餐", "calories", 440, "protein", 35, "carbs", 38, "fat", 12, "portionHint", "1掌心龙利鱼+1.5拳头生菜", "foods", List.of("清蒸龙利鱼 140g", "蒸甜玉米 0.5根", "耗油生菜 150g")),
                "snack", Map.of("title", "无糖酸奶蓝莓", "calories", 180, "protein", 11, "carbs", 20, "fat", 4, "portionHint", "1盒酸奶", "foods", List.of("无糖酸奶 140g", "蓝莓 50g"))
        ));

        // 周日
        dietList.add(Map.of(
                "day", "周日",
                "breakfast", Map.of("title", "燕麦牛奶餐", "calories", 395, "protein", 25, "carbs", 46, "fat", 11, "portionHint", "1掌心蛋白+1手心燕麦片", "foods", List.of("热牛奶冲燕麦片 1碗", "水煮蛋 1个", "苹果 0.5个")),
                "lunch", Map.of("title", "蒜苔炒牛肉餐", "calories", 665, "protein", 41, "carbs", 66, "fat", 19, "portionHint", "1.5掌心牛肉+1手心五谷饭", "foods", List.of("蒜苔炒牛肉 150g", "五谷杂粮饭 150g", "清炒四季豆 150g")),
                "dinner", Map.of("title", "海带豆腐炖排骨", "calories", 470, "protein", 31, "carbs", 35, "fat", 17, "portionHint", "1掌心精瘦排骨+1.5拳头海带", "foods", List.of("海带豆腐炖精排 130g", "蒸红薯 100g", "白灼菜心 150g")),
                "snack", Map.of("title", "橙子全麦饼干", "calories", 170, "protein", 5, "carbs", 24, "fat", 4, "portionHint", "1个水果", "foods", List.of("鲜橙 1个", "无糖全麦饼干 2片"))
        ));

        res.put("dietPlan", dietList);
        return res;
    }

    private void recordPlanAiUsage(Long userId) {
        try {
            String todayStr = java.time.LocalDate.now().toString();
            stringRedisTemplate.opsForValue().increment("shike:ai:plan:count:" + todayStr);
            stringRedisTemplate.opsForValue().increment("shike:ai:plan:tokens:" + todayStr, 3500); // 平均约 3500 Tokens/次
            if (userId != null) {
                String limitKey = "shike:ai:limit:" + userId + ":" + todayStr;
                Long count = stringRedisTemplate.opsForValue().increment(limitKey);
                if (count != null && count == 1) {
                    stringRedisTemplate.expire(limitKey, java.time.Duration.ofHours(24));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to record plan AI usage in Redis: {}", e.getMessage());
        }
    }
}
