package com.shike.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shike.common.BizException;
import com.shike.model.dto.PetCreateDTO;
import com.shike.model.dto.PetDTO;
import com.shike.model.dto.PetInteractDTO;
import com.shike.model.entity.ExerciseRecord;
import com.shike.model.entity.Pet;
import com.shike.model.entity.WaterRecord;
import com.shike.model.vo.PetInteractVO;
import com.shike.repository.DietRecordRepository;
import com.shike.repository.ExerciseRecordRepository;
import com.shike.repository.PetRepository;
import com.shike.repository.UserRepository;
import com.shike.repository.WaterRecordRepository;
import com.shike.service.AdminService;
import com.shike.service.PetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final DietRecordRepository dietRecordRepository;
    private final WaterRecordRepository waterRecordRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AdminService adminService;

    @Value("${ai.api-key:sk-ws-H.EDLLDHH.13Vh.MEUCIQCd-Whyz9sUcrs2stiBRtDQmCdalFSF2Igm9p_OIF80tgIgTLbsTfpaWGgUcncGzCS7Dbsx5eEPy0mcT-wC5WMxApk}")
    private String aiApiKey;

    @Value("${ai.model:qwen-turbo}")
    private String aiModel;

    @Value("${ai.endpoint:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}")
    private String aiEndpoint;

    private static final Map<String, String> PRESET_AVATARS = Map.of(
            "DRAGON", "/images/pets/pet_dragon_stage1.png",
            "TOTORO", "/images/pets/pet_totoro_stage1.png",
            "CAT", "/images/pets/pet_cat_stage1.png",
            "DOG", "/images/pets/pet_dog_stage1.png",
            "QILIN", "/images/pets/pet_qilin_stage1.png"
    );

    private void checkPetFeatureEnabled() {
        Map<String, Boolean> toggles = adminService.getPublicFeatureToggles("test");
        if (Boolean.FALSE.equals(toggles.get("pet_system"))) {
            throw new BizException(403, "自律搭子功能当前已在云端下架维护中");
        }
    }

    @Override
    @Transactional
    public PetDTO getMyPet(Long userId) {
        checkPetFeatureEnabled();
        Pet pet = petRepository.findByUserId(userId).orElse(null);
        if (pet == null) {
            return null;
        }
        // 核心：基于真实时间结算自然饱食度消耗，去惩罚化（永不倒扣等级与经验！）
        applyFullnessDecayNoPenalty(pet);
        return convertToDTO(pet, userId);
    }

    /**
     * 去惩罚化生命系统（心理减负设计）：
     * 1. 饱食度自然代谢：每小时 -3.5 点（过夜 8~10 小时约消耗 28~35 点）。
     * 2. 饱食度 < 30 进入 HUNGRY 状态。
     * 3. 彻底移除扣经验、扣亲密度与 SICK 生病等挫败感机制！
     *    用户哪怕忙碌断更 1 个月，努力积累的等级与形态 100% 永久保留。
     * 4. 超过 48 小时未打开进入 WAITING（思念休眠）状态，温暖守候用户归来。
     */
    private void applyFullnessDecayNoPenalty(Pet pet) {
        if (pet == null) return;
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        LocalDateTime lastCalc = pet.getLastFullnessCalcTime();
        if (lastCalc == null) {
            lastCalc = pet.getUpdatedAt() != null ? pet.getUpdatedAt() : pet.getCreatedAt();
        }

        if (lastCalc != null) {
            long minutesPassed = Duration.between(lastCalc, now).toMinutes();
            if (minutesPassed >= 15) { // 每 15 分钟平滑结算一次
                double hoursPassed = minutesPassed / 60.0;
                int decay = (int) Math.round(hoursPassed * 3.5);
                if (decay > 0) {
                    int currentFullness = pet.getFullness() != null ? pet.getFullness() : 60;
                    int newFullness = Math.max(0, currentFullness - decay);
                    pet.setFullness(newFullness);
                    pet.setLastFullnessCalcTime(now);

                    // 心情状态判定：去负罪感化
                    LocalDate lastFeed = pet.getLastFeedDate();
                    if (lastFeed != null && ChronoUnit.DAYS.between(lastFeed, today) >= 2) {
                        pet.setMood("WAITING"); // 思念休眠，而非生病虚弱
                    } else if (newFullness < 30) {
                        pet.setMood("HUNGRY");
                    } else if ("HUNGRY".equals(pet.getMood()) && newFullness >= 30) {
                        pet.setMood("NORMAL");
                    }

                    // 连续陪伴天数判定
                    if (lastFeed != null && !lastFeed.equals(today) && !lastFeed.equals(today.minusDays(1))) {
                        pet.setStreakDays(1);
                    }

                    petRepository.save(pet);
                }
            }
        } else {
            pet.setLastFullnessCalcTime(now);
            petRepository.save(pet);
        }
    }

    @Override
    @Transactional
    public PetDTO createPet(Long userId, PetCreateDTO dto) {
        checkPetFeatureEnabled();
        log.info("Creating pet for userId={}, name={}, type={}", userId, dto.getName(), dto.getPetType());

        Optional<Pet> existingOpt = petRepository.findByUserId(userId);
        if (existingOpt.isPresent()) {
            Pet existing = existingOpt.get();
            existing.setName(dto.getName());
            existing.setPetType(dto.getPetType());
            if (dto.getAvatarUrl() != null && !dto.getAvatarUrl().isBlank()) {
                existing.setAvatarUrl(dto.getAvatarUrl());
            }
            if (dto.getPrompt() != null && !dto.getPrompt().isBlank()) {
                existing.setPrompt(dto.getPrompt());
            }
            petRepository.save(existing);
            return convertToDTO(existing, userId);
        }

        String typeUpper = dto.getPetType() != null ? dto.getPetType().toUpperCase() : "DRAGON";
        String avatarUrl = dto.getAvatarUrl();
        if (avatarUrl == null || avatarUrl.isBlank()) {
            avatarUrl = PRESET_AVATARS.getOrDefault(typeUpper, "/images/pets/pet_dragon_stage1.png");
        }

        Pet pet = Pet.builder()
                .userId(userId)
                .name(dto.getName())
                .petType(typeUpper)
                .avatarUrl(avatarUrl)
                .prompt(dto.getPrompt())
                .level(1)
                .exp(0)
                .fullness(60)
                .intimacy(10)
                .foodCount(1) // 初始赠送 1 份食物
                .mood("NORMAL")
                .streakDays(1)
                .lastFullnessCalcTime(LocalDateTime.now())
                .build();

        Pet saved = petRepository.save(pet);
        log.info("Pet created successfully: id={}, userId={}, name={}", saved.getId(), userId, saved.getName());
        return convertToDTO(saved, userId);
    }

    @Override
    @Transactional
    public PetDTO feedPet(Long userId) {
        checkPetFeatureEnabled();
        Pet pet = petRepository.findByUserId(userId)
                .orElseThrow(() -> new BizException(404, "尚未领养自律搭子，请先领养一只吧！"));

        int currentFood = pet.getFoodCount() != null ? pet.getFoodCount() : 0;
        if (currentFood <= 0) {
            throw new BizException(400, "暂无可投喂的食物，完成运动、记餐或每日签到赚取食物吧！");
        }

        applyFullnessDecayNoPenalty(pet);

        LocalDate today = LocalDate.now();
        pet.setFoodCount(currentFood - 1);

        // 饱食度 +30 (上限 100)
        int currentFullness = pet.getFullness() != null ? pet.getFullness() : 60;
        int newFullness = Math.min(100, currentFullness + 30);
        pet.setFullness(newFullness);
        pet.setLastFullnessCalcTime(LocalDateTime.now());

        // 亲密度 +5
        int currentIntimacy = pet.getIntimacy() != null ? pet.getIntimacy() : 10;
        pet.setIntimacy(currentIntimacy + 5);

        // 经验 +10 与升级判断 (每级所需经验: level * 50)
        int currentExp = pet.getExp() != null ? pet.getExp() : 0;
        int currentLevel = pet.getLevel() != null ? pet.getLevel() : 1;
        int newExp = currentExp + 10;
        int neededExp = currentLevel * 50;

        if (newExp >= neededExp) {
            pet.setLevel(currentLevel + 1);
            pet.setExp(newExp - neededExp);
            pet.setFullness(Math.min(100, pet.getFullness() + 10));
            pet.setIntimacy(pet.getIntimacy() + 10);
            log.info("Pet upgraded! userId={}, newLevel={}", userId, pet.getLevel());
        } else {
            pet.setExp(newExp);
        }

        // 连续陪伴天数计算
        LocalDate lastFeed = pet.getLastFeedDate();
        int streak = pet.getStreakDays() != null ? pet.getStreakDays() : 0;
        if (lastFeed != null && lastFeed.equals(today.minusDays(1))) {
            pet.setStreakDays(streak + 1);
        } else if (lastFeed == null || !lastFeed.equals(today)) {
            pet.setStreakDays(Math.max(1, streak));
        }

        pet.setLastFeedDate(today);
        pet.setMood(newFullness >= 60 ? "HAPPY" : "NORMAL");

        Pet updated = petRepository.save(pet);
        return convertToDTO(updated, userId);
    }

    @Override
    @Transactional
    public boolean awardExerciseFood(Long userId, LocalDate date) {
        return awardPetFood(userId, "EXERCISE", date);
    }

    @Override
    @Transactional
    public boolean awardPetFood(Long userId, String source, LocalDate date) {
        Pet pet = petRepository.findByUserId(userId).orElse(null);
        if (pet == null) {
            return false;
        }

        LocalDate targetDate = date != null ? date : LocalDate.now();
        String sourceUpper = source != null ? source.toUpperCase() : "GENERAL";

        switch (sourceUpper) {
            case "CHECKIN":
                if (pet.getLastCheckinDate() != null && pet.getLastCheckinDate().equals(targetDate)) {
                    return false;
                }
                pet.setLastCheckinDate(targetDate);
                break;
            case "EXERCISE":
                if (pet.getLastExerciseDate() != null && pet.getLastExerciseDate().equals(targetDate)) {
                    return false;
                }
                pet.setLastExerciseDate(targetDate);
                break;
            case "DIET":
                if (pet.getLastDietDate() != null && pet.getLastDietDate().equals(targetDate)) {
                    return false;
                }
                pet.setLastDietDate(targetDate);
                break;
            case "WATER":
                if (pet.getLastWaterDate() != null && pet.getLastWaterDate().equals(targetDate)) {
                    return false;
                }
                pet.setLastWaterDate(targetDate);
                break;
            case "WEIGHT":
                if (pet.getLastWeightDate() != null && pet.getLastWeightDate().equals(targetDate)) {
                    return false;
                }
                pet.setLastWeightDate(targetDate);
                break;
            default:
                break;
        }

        pet.setFoodCount((pet.getFoodCount() != null ? pet.getFoodCount() : 0) + 1);
        petRepository.save(pet);
        log.info("Awarded 1 pet food to userId={} from source={}", userId, sourceUpper);
        return true;
    }

    @Override
    @Transactional
    public Map<String, Object> checkin(Long userId) {
        checkPetFeatureEnabled();
        Pet pet = petRepository.findByUserId(userId)
                .orElseThrow(() -> new BizException(404, "尚未领养自律搭子，请先领养一只吧！"));

        LocalDate today = LocalDate.now();
        boolean rewarded = awardPetFood(userId, "CHECKIN", today);

        Map<String, Object> res = new HashMap<>();
        res.put("success", rewarded);
        res.put("message", rewarded ? "签到成功！已获得 1 份营养粮 🍎" : "今天已经签到过啦，明天继续哦！");
        res.put("foodCount", pet.getFoodCount());
        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getTodayFoodTasks(Long userId) {
        LocalDate today = LocalDate.now();
        Map<String, Object> res = new HashMap<>();
        Map<String, Boolean> taskStatus = new HashMap<>();

        Pet pet = petRepository.findByUserId(userId).orElse(null);
        if (pet != null) {
            taskStatus.put("checkin", today.equals(pet.getLastCheckinDate()));
            taskStatus.put("exercise", today.equals(pet.getLastExerciseDate()));
            taskStatus.put("diet", today.equals(pet.getLastDietDate()));
            taskStatus.put("water", today.equals(pet.getLastWaterDate()));
            taskStatus.put("weight", today.equals(pet.getLastWeightDate()));
        } else {
            taskStatus.put("checkin", false);
            taskStatus.put("exercise", false);
            taskStatus.put("diet", false);
            taskStatus.put("water", false);
            taskStatus.put("weight", false);
        }

        res.put("tasks", taskStatus);
        res.put("date", today.toString());
        return res;
    }

    /**
     * 🌟 核心新功能：AI 动态拟人互动与自律树洞对话
     */
    @Override
    @Transactional
    public PetInteractVO interactWithAi(Long userId, PetInteractDTO dto) {
        checkPetFeatureEnabled();
        Pet pet = petRepository.findByUserId(userId)
                .orElseThrow(() -> new BizException(404, "尚未领养自律搭子，请先领养一只吧！"));

        LocalDate today = LocalDate.now();

        // 1. 上下文健康数据聚合
        int waterMl = 0;
        Optional<WaterRecord> waterOpt = waterRecordRepository.findByUserIdAndRecordDate(userId, today);
        if (waterOpt.isPresent()) {
            waterMl = waterOpt.get().getAmount() != null ? waterOpt.get().getAmount() : 0;
        }

        double burnedCal = 0.0;
        List<ExerciseRecord> exercises = exerciseRecordRepository.findByUserIdAndRecordDate(userId, today);
        if (exercises != null && !exercises.isEmpty()) {
            for (ExerciseRecord r : exercises) {
                if (r.getCaloriesBurned() != null) {
                    burnedCal += r.getCaloriesBurned();
                }
            }
        }

        Long dietCountLong = dietRecordRepository.countByUserIdAndRecordDate(userId, today);
        int dietCount = dietCountLong != null ? dietCountLong.intValue() : 0;

        int streak = pet.getStreakDays() != null ? pet.getStreakDays() : 1;
        int level = pet.getLevel() != null ? pet.getLevel() : 1;

        // 时段判断
        int hour = LocalTime.now().getHour();
        String timeSlot = hour < 9 ? "清晨" : (hour < 12 ? "上午" : (hour < 14 ? "中午" : (hour < 18 ? "下午" : (hour < 23 ? "晚上" : "深夜"))));

        String stageTitle = level >= 10 ? "究极体 · 传奇守护神" : (level >= 5 ? "成长期 · 进阶神兽" : "幼年期 · 萌新搭子");
        String persona = getPetPersona(pet.getPetType());
        String actionType = dto.getActionType() != null ? dto.getActionType() : "TOUCH";
        String userMsg = dto.getUserMessage() != null ? dto.getUserMessage().trim() : "";

        // 2. 组装 System Prompt
        String systemPrompt = String.format(
                "你现在是用户的专属自律搭子【%s】。\n" +
                "【性格特征】：%s\n" +
                "【当前阶段】：等级 Lv.%d，形态【%s】\n" +
                "【当前时段】：%s\n" +
                "【用户今日真实健康数据】：\n" +
                "- 今日饮水：%d ml\n" +
                "- 运动消耗：%.0f kcal\n" +
                "- 记录三餐：%d 次\n" +
                "- 连续自律陪伴：%d 天\n" +
                "- 触发动作：%s\n" +
                "- 用户对你说的话：%s\n\n" +
                "【输出要求】：\n" +
                "1. 必须完全符合你的性格人设和口癖。\n" +
                "2. 敏锐结合用户今日的数据进行回应（如喝水达标给予赞扬、深夜劝早睡、刚运动完给予极致的情绪激励、用户诉苦表达疲惫/嘴馋时给予心理学包容感的治愈与引导）。\n" +
                "3. 字数严格控制在 20 ~ 45 个汉字以内，适合展示在移动端气泡中。\n" +
                "4. 只输出宠物说的话，不要带任何前缀、引号或解释说明。",
                pet.getName(), persona, level, stageTitle, timeSlot,
                waterMl, burnedCal, dietCount, streak, actionType, userMsg.isEmpty() ? "（用户摸了摸你的脑袋）" : userMsg
        );

        // 3. 调用大语言模型 Qwen (设置 1.5s 极速超时)
        String reply = null;
        try {
            reply = callQwenLLM(systemPrompt);
        } catch (Exception e) {
            log.warn("LLM dynamic interact call failed: {}, fallback to persona preset", e.getMessage());
        }

        if (reply == null || reply.isBlank()) {
            reply = getFallbackDialogue(pet.getPetType(), actionType, timeSlot, waterMl, burnedCal);
        }

        // 清洗文案
        reply = reply.replace("\"", "").replace("“", "").replace("”", "").trim();

        return PetInteractVO.builder()
                .dialogue(reply)
                .mood(pet.getMood())
                .actionAnim("jellySquishPop")
                .soundEffect("purr")
                .foodCount(pet.getFoodCount())
                .isHealed(false)
                .build();
    }

    private String getPetPersona(String petType) {
        if (petType == null) return "元气可爱的自律搭子。";
        return switch (petType.toUpperCase()) {
            case "DRAGON" -> "热血傲娇的青玉幼龙，嘴硬心软，最关注主人的运动燃脂，口癖是‘嗷呜’、‘哼’、‘冲鸭’。";
            case "TOTORO" -> "温吞治愈的大龙猫，说话慢条斯理，极度关心主人吃得健不健康、喝水够不够，充满治愈与包容，口癖是‘呼噜噜’。";
            case "CAT"    -> "灵动傲娇的元气小橘猫，注重体态与轻盈感，爱撒娇，口癖是‘喵呜’、‘喵~’。";
            case "DOG"    -> "忠诚阳光的自律柴犬，精力充沛，最喜欢户外慢跑，口癖是‘汪汪！’、‘主人最棒！’。";
            case "QILIN"  -> "优雅仙气的小天麟，相信自律会吸引好运与祥瑞，给主人送上温暖的祝福与正向能量，口癖是‘吉星高照’。";
            default       -> "元气可爱的自律搭子。";
        };
    }

    private String getFallbackDialogue(String petType, String actionType, String timeSlot, int waterMl, double burnedCal) {
        String type = petType != null ? petType.toUpperCase() : "DRAGON";
        if ("CHAT".equals(actionType)) {
            return switch (type) {
                case "DRAGON" -> "嗷呜！虽然很不容易，但你已经做得超级棒了，本龙一直陪着你冲鸭！🔥";
                case "TOTORO" -> "呼噜噜～抱抱你，累了就歇一歇，慢慢走也能走很远呢。🍃";
                case "CAT"    -> "喵呜～蹭蹭主人的手，不管怎样小橘都是你最坚定的支持者喵！✨";
                case "DOG"    -> "汪汪！主人不要灰心，甩甩尾巴重新出发，你是最棒的！🐶";
                default       -> "祥瑞随行，抱抱你～小小的坚持都在慢慢发光哦！🌟";
            };
        }
        if (burnedCal > 200) {
            return switch (type) {
                case "DRAGON" -> "嗷呜！刚才消耗了好多热量，龙鳞都燃起来了，太帅啦！🔥";
                case "CAT"    -> "喵呜！运动后的主人体态超棒超轻盈，小橘超喜欢喵～✨";
                default       -> "刚才的运动超给力！自律的汗水都在闪闪发光哦～💪";
            };
        }
        if (waterMl >= 1500) {
            return "咕嘟咕嘟～今日饮水达标，整个人都在发光呢，继续保持！💧";
        }
        if ("深夜".equals(timeSlot)) {
            return "夜深啦，早点休息养足精神，明天我们再一起打卡变强！🌙";
        }
        return switch (type) {
            case "DRAGON" -> "嗷呜！摸头好舒服～今天也要一起燃脂变强哦！🔥";
            case "TOTORO" -> "呼噜噜～吃好喝好休息好，有我陪着你呢～🍃";
            case "CAT"    -> "喵呜～摸摸下巴好舒服，今天也要保持轻盈体态喵！✨";
            case "DOG"    -> "汪汪！最喜欢主人啦，随时准备陪你出去跑两圈！🐾";
            default       -> "摸摸仙角，祥瑞好运全给你，今天也要元气满满哦！🌟";
        };
    }

    private String callQwenLLM(String systemPrompt) throws Exception {
        Map<String, Object> payload = Map.of(
                "model", "qwen-turbo",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", "请以自律搭子的口吻做出回应。")
                ),
                "max_tokens", 80,
                "temperature", 0.7
        );

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(1500)).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiEndpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiApiKey)
                .timeout(Duration.ofMillis(1800))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            Map<?, ?> resMap = objectMapper.readValue(response.body(), Map.class);
            List<?> choices = (List<?>) resMap.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
                Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> generateAvatar(Long userId, String petType, String promptHint) {
        String typeUpper = petType != null ? petType.toUpperCase() : "DRAGON";
        String prompt = buildPromptForType(typeUpper, promptHint);
        log.info("Generating AI avatar for userId={}, type={}, prompt={}", userId, typeUpper, prompt);

        String generatedUrl = null;
        try {
            generatedUrl = callWanxTextToImage(prompt);
        } catch (Exception e) {
            log.warn("Wanx image generation failed: {}, fallback to preset", e.getMessage());
        }

        if (generatedUrl == null || generatedUrl.isBlank()) {
            generatedUrl = PRESET_AVATARS.getOrDefault(typeUpper, "/images/pets/pet_dragon_stage1.png");
        }

        Map<String, Object> res = new HashMap<>();
        res.put("imageUrl", generatedUrl);
        res.put("petType", typeUpper);
        res.put("prompt", prompt);
        return res;
    }

    private String buildPromptForType(String petType, String userHint) {
        String base;
        switch (petType) {
            case "TOTORO":
                base = "A 3D CGI render of an extremely cute fluffy baby Totoro chinchilla, the size of a chubby puppy. Round chubby face with puffed cheeks, giant watery innocent dark eyes, tiny pink nose, soft rounded furry ears, plush warm grey and cream fur, holding green lotus leaf and hugging fresh kiwi fruit. Pixar style, highly detailed ultra-soft fur texture, warm natural sunlight, 8k";
                break;
            case "CAT":
                base = "A 3D CGI render of an extremely cute fluffy baby kitten, the size of a chubby puppy. Round chubby face with puffed cheeks, giant watery innocent dark emerald eyes, tiny pink nose, plush apricot orange and snow-white fur, sitting on mossy log, hugging a big ripe sweet orange. Pixar style, highly detailed soft fur texture, warm natural sunlight, 8k";
                break;
            case "DOG":
                base = "A 3D CGI render of an extremely cute fluffy baby golden Shiba Inu puppy, the size of a chubby puppy. Round chubby face with puffed cheeks, giant watery innocent dark eyes, tiny wet nose, plush golden honey and white fur, hugging a shiny red strawberry. Pixar style, highly detailed soft fur texture, warm natural sunlight, 8k";
                break;
            case "QILIN":
                base = "A 3D CGI render of an extremely cute fluffy baby mythical Qilin beast, the size of a chubby puppy. Round chubby face with puffed cheeks, giant watery dark eyes, tiny pink nose, soft velvet golden antlers, plush lavender-blue and pearl-white fur with gentle shimmer, hugging a ripe pink peach. Pixar style, soft fur texture, warm natural sunlight, 8k";
                break;
            default: // DRAGON
                base = "A 3D CGI render of an extremely cute fluffy baby forest dragon, the size of a chubby puppy. Round chubby face with puffed cheeks, giant watery dark eyes, tiny pink nose, soft velvet mossy twig antlers, tiny soft wings, plush sage-green and cream fur, hugging a big shiny red apple. Pixar style, highly detailed soft fur texture, warm natural sunlight, 8k";
                break;
        }

        if (userHint != null && !userHint.isBlank()) {
            base += ", " + userHint;
        }
        return base;
    }

    private String callWanxTextToImage(String prompt) throws Exception {
        String submitUrl = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis";
        Map<String, Object> payload = Map.of(
                "model", "wanx-v1",
                "input", Map.of("prompt", prompt),
                "parameters", Map.of(
                        "style", "<3d cartoon>",
                        "size", "1024*1024",
                        "n", 1
                )
        );

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(submitUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiApiKey)
                .header("X-DashScope-Async", "enable")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Wanx task submission failed: " + response.body());
        }

        Map<?, ?> resMap = objectMapper.readValue(response.body(), Map.class);
        Map<?, ?> output = (Map<?, ?>) resMap.get("output");
        String taskId = (String) output.get("task_id");

        String taskUrl = "https://dashscope.aliyuncs.com/api/v1/tasks/" + taskId;
        for (int i = 0; i < 20; i++) {
            Thread.sleep(1500);
            HttpRequest queryReq = HttpRequest.newBuilder()
                    .uri(URI.create(taskUrl))
                    .header("Authorization", "Bearer " + aiApiKey)
                    .GET()
                    .build();
            HttpResponse<String> queryRes = client.send(queryReq, HttpResponse.BodyHandlers.ofString());
            Map<?, ?> queryMap = objectMapper.readValue(queryRes.body(), Map.class);
            Map<?, ?> queryOutput = (Map<?, ?>) queryMap.get("output");
            String taskStatus = (String) queryOutput.get("task_status");

            if ("SUCCEEDED".equals(taskStatus)) {
                List<?> results = (List<?>) queryOutput.get("results");
                if (results != null && !results.isEmpty()) {
                    Map<?, ?> firstRes = (Map<?, ?>) results.get(0);
                    return (String) firstRes.get("url");
                }
            } else if ("FAILED".equals(taskStatus)) {
                throw new RuntimeException("Wanx task failed: " + queryRes.body());
            }
        }
        return null;
    }

    private PetDTO convertToDTO(Pet pet, Long userId) {
        int exp = pet.getExp() != null ? pet.getExp() : 0;
        int level = pet.getLevel() != null ? pet.getLevel() : 1;
        int maxExp = level * 50;

        String dialogue = "今天也是充满活力的一天！一起来自律打卡吧～";
        if ("WAITING".equals(pet.getMood())) {
            dialogue = "主人忙碌时小家伙在乖乖守候，随时等你回来打卡哦～✨";
        } else if ("HUNGRY".equals(pet.getMood()) || (pet.getFullness() != null && pet.getFullness() < 30)) {
            dialogue = "肚子咕咕叫啦，记得打卡给我带点好吃的哦～";
        } else if ("HAPPY".equals(pet.getMood())) {
            dialogue = "吃饱饱超满足！今天也要元气满满哦～";
        }

        return PetDTO.builder()
                .id(pet.getId())
                .userId(pet.getUserId())
                .name(pet.getName())
                .petType(pet.getPetType())
                .avatarUrl(pet.getAvatarUrl())
                .level(level)
                .exp(exp)
                .maxExp(maxExp)
                .fullness(pet.getFullness() != null ? pet.getFullness() : 60)
                .intimacy(pet.getIntimacy() != null ? pet.getIntimacy() : 10)
                .foodCount(pet.getFoodCount() != null ? pet.getFoodCount() : 0)
                .mood(pet.getMood() != null ? pet.getMood() : "NORMAL")
                .moodText(getMoodText(pet.getMood(), pet.getFullness()))
                .streakDays(pet.getStreakDays() != null ? pet.getStreakDays() : 1)
                .dialogue(dialogue)
                .build();
    }

    private String getMoodText(String mood, Integer fullness) {
        if ("WAITING".equals(mood)) return "思念等待";
        if (fullness != null && fullness < 30) return "饥肠辘辘";
        if (mood == null) return "悠然自得";
        return switch (mood) {
            case "HAPPY" -> "开心雀跃";
            case "HUNGRY" -> "饥肠辘辘";
            case "WAITING" -> "思念等待";
            case "WANT_EXERCISE" -> "渴望运动";
            default -> "悠然自得";
        };
    }
}
