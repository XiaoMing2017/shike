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

    @Override
    @Transactional(readOnly = true)
    public PetDTO getMyPet(Long userId) {
        Pet pet = petRepository.findByUserId(userId).orElse(null);
        if (pet == null) {
            return null;
        }
        return convertToDTO(pet, userId);
    }

    @Override
    @Transactional
    public PetDTO createPet(Long userId, PetCreateDTO dto) {
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
                .foodCount(1) // 初始赠送 1 份食物，提升初次体验
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
        Pet pet = petRepository.findByUserId(userId)
                .orElseThrow(() -> new BizException(404, "尚未领养自律搭子，请先领养一只吧！"));

        int currentFood = pet.getFoodCount() != null ? pet.getFoodCount() : 0;
        if (currentFood <= 0) {
            throw new BizException(400, "暂无可投喂的食物，去完成一次运动打卡带回健康食物吧！");
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
        Pet pet = petRepository.findByUserId(userId).orElse(null);
        if (pet == null) {
            return false;
        }

        LocalDate targetDate = date != null ? date : LocalDate.now();
        if (pet.getLastExerciseDate() != null && pet.getLastExerciseDate().equals(targetDate)) {
            log.info("User {} already claimed exercise food reward for today: {}", userId, targetDate);
            return false; // 今日已发过食物
        }

        pet.setFoodCount((pet.getFoodCount() != null ? pet.getFoodCount() : 0) + 1);
        pet.setLastExerciseDate(targetDate);
        petRepository.save(pet);
        log.info("Awarded 1 exercise food to pet of userId={}", userId);
        return true;
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

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(submitUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiApiKey)
                .header("X-DashScope-Async", "enable")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.warn("Wanx submit failed: code={}, body={}", response.statusCode(), response.body());
            return null;
        }

        Map<String, Object> resMap = objectMapper.readValue(response.body(), Map.class);
        Map<String, Object> output = (Map<String, Object>) resMap.get("output");
        if (output == null || output.get("task_id") == null) {
            return null;
        }

        String taskId = (String) output.get("task_id");
        String taskUrl = "https://dashscope.aliyuncs.com/api/v1/tasks/" + taskId;

        // Poll task for up to 30s
        for (int i = 0; i < 15; i++) {
            Thread.sleep(2000);
            HttpRequest pollReq = HttpRequest.newBuilder()
                    .uri(URI.create(taskUrl))
                    .header("Authorization", "Bearer " + aiApiKey)
                    .GET()
                    .build();
            HttpResponse<String> pollRes = client.send(pollReq, HttpResponse.BodyHandlers.ofString());
            if (pollRes.statusCode() == 200) {
                Map<String, Object> pollMap = objectMapper.readValue(pollRes.body(), Map.class);
                Map<String, Object> pollOutput = (Map<String, Object>) pollMap.get("output");
                if (pollOutput != null) {
                    String status = (String) pollOutput.get("task_status");
                    if ("SUCCEEDED".equals(status)) {
                        List<Map<String, Object>> results = (List<Map<String, Object>>) pollOutput.get("results");
                        if (results != null && !results.isEmpty()) {
                            return (String) results.get(0).get("url");
                        }
                    } else if ("FAILED".equals(status) || "CANCELED".equals(status)) {
                        break;
                    }
                }
            }
        }
        return null;
    }

    private PetDTO convertToDTO(Pet pet, Long userId) {
        LocalDate today = LocalDate.now();
        boolean fedToday = pet.getLastFeedDate() != null && pet.getLastFeedDate().equals(today);
        boolean exercisedToday = exerciseRecordRepository.existsByUserIdAndRecordDate(userId, today);

        // 动态计算心情状态
        String mood;
        String moodText;
        if (fedToday) {
            mood = "HAPPY";
            moodText = "它吃得饱饱的，正在陪你一起变轻变强！✨";
        } else if (pet.getFullness() != null && pet.getFullness() < 30) {
            mood = "HUNGRY";
            moodText = "咕噜噜～小肚子饿了，快去运动带回食物吧～🍖";
        } else if (!exercisedToday) {
            mood = "WANT_EXERCISE";
            moodText = "今天还没动一动呢，带我一起去活动一下吧！🏃";
        } else {
            mood = "NORMAL";
            moodText = "元气满满，期待今天的健康挑战！🌱";
        }

        // 挑选台词
        String dialogue = getRandomDialogue(pet.getPetType(), mood, fedToday, exercisedToday);

        int level = pet.getLevel() != null ? pet.getLevel() : 1;
        int maxExp = level * 50;

        return PetDTO.builder()
                .id(pet.getId())
                .userId(pet.getUserId())
                .name(pet.getName())
                .petType(pet.getPetType())
                .avatarUrl(pet.getAvatarUrl())
                .prompt(pet.getPrompt())
                .level(level)
                .exp(pet.getExp() != null ? pet.getExp() : 0)
                .maxExp(maxExp)
                .fullness(pet.getFullness() != null ? pet.getFullness() : 60)
                .intimacy(pet.getIntimacy() != null ? pet.getIntimacy() : 10)
                .foodCount(pet.getFoodCount() != null ? pet.getFoodCount() : 0)
                .mood(mood)
                .moodText(moodText)
                .dialogue(dialogue)
                .streakDays(pet.getStreakDays() != null ? pet.getStreakDays() : 1)
                .fedToday(fedToday)
                .exercisedToday(exercisedToday)
                .lastFeedDate(pet.getLastFeedDate())
                .lastExerciseDate(pet.getLastExerciseDate())
                .build();
    }

    private String getRandomDialogue(String petType, String mood, boolean fedToday, boolean exercisedToday) {
        List<String> pool = new ArrayList<>();
        if ("HAPPY".equals(mood)) {
            pool.add("吃得饱饱，今天陪你一起燃脂！💪");
            pool.add("本搭子宣布：你今天超自律！🌟");
            pool.add("今天又多消耗了卡路里，我们都在变强！✨");
        } else if ("HUNGRY".equals(mood)) {
            pool.add("咕噜噜～肚子好饿，快去运动带回健康粮吧！🍖");
            pool.add("我不运动，小家伙就没饭吃啦！快走两圈～🏃");
        } else if ("WANT_EXERCISE".equals(mood)) {
            pool.add("今天还没去运动呢，带我一起去公园散散步吧！🐾");
            pool.add("深蹲还是跑步？只要动起来，我就能吃到水果啦！🍎");
        } else {
            pool.add("自律最酷啦，今天也要一起加油哦！🔥");
            pool.add("少油少盐多喝水，体态越来越棒啦！💧");
        }
        return pool.get(new Random().nextInt(pool.size()));
    }
}
