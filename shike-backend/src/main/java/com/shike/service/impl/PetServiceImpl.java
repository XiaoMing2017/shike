package com.shike.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shike.common.BizException;
import com.shike.model.dto.PetCreateDTO;
import com.shike.model.dto.PetDTO;
import com.shike.model.entity.Pet;
import com.shike.repository.ExerciseRecordRepository;
import com.shike.repository.PetRepository;
import com.shike.repository.UserRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AdminService adminService;

    @Value("${ai.api-key:sk-ws-H.EDLLDHH.13Vh.MEUCIQCd-Whyz9sUcrs2stiBRtDQmCdalFSF2Igm9p_OIF80tgIgTLbsTfpaWGgUcncGzCS7Dbsx5eEPy0mcT-wC5WMxApk}")
    private String aiApiKey;

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
        // 核心：基于真实时间结算自然饥饿、怠惰惩罚、经验流失与生命状态
        applyLifeCycleAndSlackPenalty(pet);
        return convertToDTO(pet, userId);
    }

    /**
     * 核心：搭子生命周期与怠惰衰减体系（Natural Hunger & Slack Penalties）
     * 1. 饱食度自然消耗：每小时自然代谢 -3.5 点（8小时过夜消耗 28点，24小时消耗 84点）
     * 2. 饱食度 < 30 -> 进入 HUNGRY 状态
     * 3. 连续 > 48 小时未打卡投喂：
     *    - 饱食度降为 0
     *    - 进入 虚弱生病 SICK 状态（台词虚弱求救，立绘虚脱滤镜）
     *    - 亲密度每日流失 -5 点（保底 0 点）
     *    - 当前级经验 EXP 每日流失 -10 点（设有 Lv.1 / Lv.5 / Lv.10 阶段形态保底，绝不退化外观形态）
     * 4. 连续自律天数判定：若昨日未打卡投喂，Streak 自动重置为 1
     */
    private void applyLifeCycleAndSlackPenalty(Pet pet) {
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

                    // 1. 饱食度与心情判定
                    if (newFullness <= 0) {
                        // 如果饱食度归零，检查是否已饥饿超过 24 小时
                        LocalDate lastFeed = pet.getLastFeedDate();
                        if (lastFeed != null && ChronoUnit.DAYS.between(lastFeed, today) >= 2) {
                            pet.setMood("SICK"); // 虚弱生病
                        } else {
                            pet.setMood("HUNGRY");
                        }
                    } else if (newFullness < 30) {
                        pet.setMood("HUNGRY");
                    } else if ("HUNGRY".equals(pet.getMood()) && newFullness >= 30) {
                        pet.setMood("NORMAL");
                    }

                    // 2. 连续 48 小时未自律投喂 -> 触发经验与亲密度怠惰流失 (带有 Lv.1 / Lv.5 / Lv.10 形态保底)
                    LocalDate lastFeed = pet.getLastFeedDate();
                    if (lastFeed != null && ChronoUnit.DAYS.between(lastFeed, today) >= 2) {
                        pet.setMood("SICK");
                        long daysNeglected = ChronoUnit.DAYS.between(lastFeed, today) - 1;
                        if (daysNeglected > 0) {
                            // 亲密度流失
                            int currentIntimacy = pet.getIntimacy() != null ? pet.getIntimacy() : 10;
                            pet.setIntimacy((int) Math.max(0, currentIntimacy - daysNeglected * 5));

                            // 经验流失（保底当前阶段初始等级）
                            int currentExp = pet.getExp() != null ? pet.getExp() : 0;
                            int currentLvl = pet.getLevel() != null ? pet.getLevel() : 1;
                            int expLoss = (int) (daysNeglected * 10);
                            
                            int newExp = currentExp - expLoss;
                            if (newExp < 0) {
                                // 经验扣减至 0，如果有降级余地且不在保底形态节点（Lv.1, Lv.5, Lv.10）
                                if (currentLvl > 1 && currentLvl != 5 && currentLvl != 10) {
                                    pet.setLevel(currentLvl - 1);
                                    pet.setExp(Math.max(0, (currentLvl - 1) * 50 + newExp));
                                } else {
                                    pet.setExp(0);
                                }
                            } else {
                                pet.setExp(newExp);
                            }
                            log.warn("Pet neglected for {} days! userId={}, applied EXP/Intimacy decay. New level={}, exp={}",
                                    daysNeglected, pet.getUserId(), pet.getLevel(), pet.getExp());
                        }
                    }

                    // 3. 断签判定：昨日未投喂且今日未投喂，连签归 1
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

        // 先计算截至当前的自然消耗与怠惰流失
        applyLifeCycleAndSlackPenalty(pet);

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

        // 核心：若此前处于虚弱生病 SICK 状态，投喂立即触发【自律治愈复苏】
        if ("SICK".equals(pet.getMood())) {
            log.info("Pet recovered from sickness via feeding! userId={}", userId);
            pet.setMood(newFullness >= 60 ? "HAPPY" : "NORMAL");
        } else {
            pet.setMood(newFullness >= 60 ? "HAPPY" : "NORMAL");
        }

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

        // 数据库级别严格防重：每天每种自律行为只能领取 1 次食物
        switch (sourceUpper) {
            case "CHECKIN":
                if (pet.getLastCheckinDate() != null && pet.getLastCheckinDate().equals(targetDate)) {
                    log.info("User {} already checkin today: {}", userId, targetDate);
                    return false;
                }
                pet.setLastCheckinDate(targetDate);
                break;
            case "EXERCISE":
                if (pet.getLastExerciseDate() != null && pet.getLastExerciseDate().equals(targetDate)) {
                    log.info("User {} already got exercise food today: {}", userId, targetDate);
                    return false;
                }
                pet.setLastExerciseDate(targetDate);
                break;
            case "DIET":
                if (pet.getLastDietDate() != null && pet.getLastDietDate().equals(targetDate)) {
                    log.info("User {} already got diet food today: {}", userId, targetDate);
                    return false;
                }
                pet.setLastDietDate(targetDate);
                break;
            case "WATER":
                if (pet.getLastWaterDate() != null && pet.getLastWaterDate().equals(targetDate)) {
                    log.info("User {} already got water food today: {}", userId, targetDate);
                    return false;
                }
                pet.setLastWaterDate(targetDate);
                break;
            case "WEIGHT":
                if (pet.getLastWeightDate() != null && pet.getLastWeightDate().equals(targetDate)) {
                    log.info("User {} already got weight food today: {}", userId, targetDate);
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
        if ("SICK".equals(pet.getMood())) {
            dialogue = "好几天没见到主人啦，小家伙生病虚弱中……快去运动打卡救救它吧！💔";
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
        if ("SICK".equals(mood)) return "虚弱生病";
        if (fullness != null && fullness < 30) return "饥肠辘辘";
        if (mood == null) return "悠然自得";
        return switch (mood) {
            case "HAPPY" -> "开心雀跃";
            case "HUNGRY" -> "饥肠辘辘";
            case "SICK" -> "虚弱生病";
            case "WANT_EXERCISE" -> "渴望运动";
            default -> "悠然自得";
        };
    }
}
