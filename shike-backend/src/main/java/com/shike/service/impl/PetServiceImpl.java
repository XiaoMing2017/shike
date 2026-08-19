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
            "DRAGON", "/images/pets/pet_dragon.png",
            "TOTORO", "/images/pets/pet_totoro.png",
            "CAT", "/images/pets/pet_cat.png",
            "DOG", "/images/pets/pet_dog.png",
            "QILIN", "/images/pets/pet_qilin.png"
    );

    private void checkPetFeatureEnabled() {
        Map<String, Boolean> toggles = adminService.getPublicFeatureToggles("test");
        if (Boolean.FALSE.equals(toggles.get("pet_system"))) {
            throw new BizException(403, "自律搭子功能当前已在云端下架维护中");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PetDTO getMyPet(Long userId) {
        checkPetFeatureEnabled();
        Pet pet = petRepository.findByUserId(userId).orElse(null);
        if (pet == null) {
            return null;
        }
        return convertToDTO(pet, userId);
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
            avatarUrl = PRESET_AVATARS.getOrDefault(typeUpper, "/images/pets/pet_dragon.png");
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
            throw new BizException(400, "暂无可投喂的食物，去完成运动、记录饮食或每日签到赚取食物吧！");
        }

        LocalDate today = LocalDate.now();
        pet.setFoodCount(currentFood - 1);

        // 饱食度 +30 (上限 100)
        int currentFullness = pet.getFullness() != null ? pet.getFullness() : 60;
        pet.setFullness(Math.min(100, currentFullness + 30));

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

        // 连续天数计算
        LocalDate lastFeed = pet.getLastFeedDate();
        int streak = pet.getStreakDays() != null ? pet.getStreakDays() : 0;
        if (lastFeed != null && lastFeed.equals(today.minusDays(1))) {
            pet.setStreakDays(streak + 1);
        } else if (lastFeed == null || !lastFeed.equals(today)) {
            pet.setStreakDays(Math.max(1, streak));
        }

        pet.setLastFeedDate(today);
        pet.setMood("HAPPY");

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
        String redisKey = "pet:food_reward:" + userId + ":" + sourceUpper + ":" + targetDate;

        // 检查今日是否已通过该渠道获得过食物
        try {
            Boolean isNew = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, "1", Duration.ofDays(2));
            if (Boolean.FALSE.equals(isNew)) {
                log.info("User {} already claimed {} food reward for today: {}", userId, sourceUpper, targetDate);
                return false;
            }
        } catch (Exception e) {
            log.warn("Redis check failed, fallback: {}", e.getMessage());
        }

        pet.setFoodCount((pet.getFoodCount() != null ? pet.getFoodCount() : 0) + 1);
        if ("EXERCISE".equals(sourceUpper)) {
            pet.setLastExerciseDate(targetDate);
        }
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
    public Map<String, Object> getTodayFoodTasks(Long userId) {
        LocalDate today = LocalDate.now();
        Map<String, Object> res = new HashMap<>();

        String[] sources = {"CHECKIN", "EXERCISE", "DIET", "WATER", "WEIGHT"};
        Map<String, Boolean> taskStatus = new HashMap<>();

        for (String s : sources) {
            String key = "pet:food_reward:" + userId + ":" + s + ":" + today;
            boolean completed = false;
            try {
                completed = Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
            } catch (Exception e) {}
            taskStatus.put(s.toLowerCase(), completed);
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
            generatedUrl = PRESET_AVATARS.getOrDefault(typeUpper, "/images/pets/pet_dragon.png");
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
        if ("HUNGRY".equals(pet.getMood())) {
            dialogue = "肚子有点咕咕叫啦，记得运动打卡给我带点好吃的哦～";
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
                .moodText(getMoodText(pet.getMood()))
                .streakDays(pet.getStreakDays() != null ? pet.getStreakDays() : 1)
                .dialogue(dialogue)
                .build();
    }

    private String getMoodText(String mood) {
        if (mood == null) return "悠然自得";
        return switch (mood) {
            case "HAPPY" -> "开心雀跃";
            case "HUNGRY" -> "饥肠辘辘";
            case "WANT_EXERCISE" -> "渴望运动";
            default -> "悠然自得";
        };
    }
}
