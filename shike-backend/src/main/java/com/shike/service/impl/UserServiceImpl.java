package com.shike.service.impl;

import com.shike.common.BizException;
import com.shike.model.dto.UserLoginDTO;
import com.shike.model.entity.User;
import com.shike.repository.UserRepository;
import com.shike.repository.PointsRecordRepository;
import com.shike.model.entity.PointsRecord;
import com.shike.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PointsRecordRepository pointsRecordRepository;

    @Value("${wx.mock}")
    private boolean wxMock;

    @Value("${wx.appid}")
    private String wxAppid;

    @Value("${wx.secret}")
    private String wxSecret;

    @Override
    @Transactional
    public User loginOrRegister(UserLoginDTO loginDTO) {
        String openid = null;
        String code = loginDTO.getCode();

        if (code != null && !code.trim().isEmpty()) {
            try {
                openid = getOpenIdFromWx(code);
            } catch (Exception e) {
                if (wxMock) {
                    log.warn("Wx login via code failed (probably due to mock appid/secret), falling back to mock openid. Error: {}", e.getMessage());
                    openid = loginDTO.getOpenid();
                    if (openid == null || openid.trim().isEmpty()) {
                        openid = "mock_user_openid_123";
                    }
                } else {
                    throw e;
                }
            }
        } else {
            if (wxMock) {
                openid = loginDTO.getOpenid();
                if (openid == null || openid.trim().isEmpty()) {
                    openid = "mock_user_openid_123";
                }
            } else {
                throw new BizException(400, "code must be provided for login");
            }
        }

        if (openid == null || openid.trim().isEmpty()) {
            throw new BizException(400, "openid cannot be empty");
        }

        Optional<User> userOpt = userRepository.findByOpenid(openid);
        if (userOpt.isPresent()) {
            log.info("User login success, openid: {}", openid);
            return userOpt.get();
        } else {
            log.info("User not found, registering new user, openid: {}", openid);
            User newUser = User.builder()
                    .openid(openid)
                    .nickname("微信用户_" + openid.substring(Math.max(0, openid.length() - 6)))
                    .gender(0)
                    .points(1000) // 初始契约分 1000
                    .activityLevel("SEDENTARY")
                    .goal("MAINTAIN")
                    .trainingLevel("BEGINNER")
                    .build();
            return userRepository.save(newUser);
        }
    }

    private String getOpenIdFromWx(String code) {
        String url = String.format("https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                wxAppid, wxSecret, code);
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofMillis(5000))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            String body = response.body();
            log.info("Wx login response: {}", body);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(body);
            if (node.has("errcode") && node.get("errcode").asInt() != 0) {
                throw new BizException(500, "Wx login error: " + node.get("errmsg").asText());
            }
            if (node.has("openid")) {
                return node.get("openid").asText();
            } else {
                throw new BizException(500, "Wx response does not contain openid");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call wx api", e);
            throw new BizException(500, "Failed to call WeChat auth server: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, Integer age, Integer gender, BigDecimal height, BigDecimal weight, 
                               String activityLevel, String goal, String nickname, String avatarUrl,
                               String customGoalType, Integer customGoalDays, BigDecimal customGoalWeight,
                               BigDecimal currentBodyFat, String trainingLevel) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "User not found"));

        user.setAge(age);
        user.setGender(gender);
        user.setHeight(height);
        user.setWeight(weight);
        user.setActivityLevel(activityLevel);
        user.setGoal(goal);
        user.setCustomGoalType(customGoalType);
        user.setCustomGoalDays(customGoalDays);
        user.setCustomGoalWeight(customGoalWeight);
        user.setCurrentBodyFat(currentBodyFat);
        if (trainingLevel != null && !trainingLevel.trim().isEmpty()) {
            user.setTrainingLevel(trainingLevel);
        }

        if (nickname != null && !nickname.trim().isEmpty()) {
            user.setNickname(nickname);
        }
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            user.setAvatarUrl(avatarUrl);
        }

        // Perform BMR and TDEE calculations
        calculateMetabolism(user);

        log.info("Updated profile for user: {}, BMR: {}, TDEE: {}, Target Cal: {}", 
                userId, user.getBmr(), user.getTdee(), user.getTargetCalories());
        return userRepository.save(user);
    }

    @Override
    public User getUserInfo(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "User not found"));
    }

    private void calculateMetabolism(User user) {
        if (user.getWeight() == null || user.getHeight() == null || user.getAge() == null) {
            return;
        }

        double w = user.getWeight().doubleValue();
        double h = user.getHeight().doubleValue();
        int age = user.getAge();

        // Mifflin-St Jeor Formula
        // Male: BMR = 10 * w + 6.25 * h - 5 * age + 5
        // Female: BMR = 10 * w + 6.25 * h - 5 * age - 161
        double bmrVal;
        if (user.getGender() != null && user.getGender() == 2) {
            bmrVal = 10 * w + 6.25 * h - 5 * age - 161;
        } else {
            // Default to male or unknown
            bmrVal = 10 * w + 6.25 * h - 5 * age + 5;
        }
        user.setBmr(BigDecimal.valueOf(bmrVal).setScale(1, RoundingMode.HALF_UP));

        // TDEE activity multipliers
        double multiplier = 1.2; // default SEDENTARY
        if ("LIGHT".equalsIgnoreCase(user.getActivityLevel())) {
            multiplier = 1.375;
        } else if ("MODERATE".equalsIgnoreCase(user.getActivityLevel())) {
            multiplier = 1.55;
        } else if ("ACTIVE".equalsIgnoreCase(user.getActivityLevel())) {
            multiplier = 1.725;
        }
        double tdeeVal = bmrVal * multiplier;
        user.setTdee(BigDecimal.valueOf(tdeeVal).setScale(1, RoundingMode.HALF_UP));

        // Goal offset
        double goalOffset = 0.0; // default MAINTAIN
        if ("LOSE_WEIGHT".equalsIgnoreCase(user.getGoal())) {
            goalOffset = -500.0;
        } else if ("GAIN_MUSCLE".equalsIgnoreCase(user.getGoal())) {
            goalOffset = 300.0;
        } else if ("PERIOD".equalsIgnoreCase(user.getGoal()) || "CUSTOM".equalsIgnoreCase(user.getGoal())) {
            // Custom periodic weight goal: 1 jin = 3850 kcal
            if (user.getCustomGoalDays() != null && user.getCustomGoalWeight() != null && user.getCustomGoalDays() > 0) {
                double targetDays = user.getCustomGoalDays();
                double weightChangeInJin = user.getCustomGoalWeight().doubleValue();
                double calculatedOffset = (weightChangeInJin * 3850.0) / targetDays;
                
                // Safety weekly rate cap
                double ratePerWeek = Math.abs(weightChangeInJin) / targetDays * 7.0;
                if (weightChangeInJin < 0) { // Loss
                    if (ratePerWeek > 2.0) { // Max 2 jin per week
                        calculatedOffset = -1000.0;
                    }
                } else { // Gain
                    if (ratePerWeek > 1.0) { // Max 1 jin per week
                        calculatedOffset = 500.0;
                    }
                }
                goalOffset = calculatedOffset;
            }
        } else if ("ABS".equalsIgnoreCase(user.getGoal())) {
            // Abs visualization goal based on body fat percentage
            if (user.getCustomGoalDays() != null && user.getCustomGoalDays() > 0) {
                double currentFatRate = user.getCurrentBodyFat() != null ? user.getCurrentBodyFat().doubleValue() : (user.getGender() != null && user.getGender() == 2 ? 26.0 : 20.0);
                double targetFatRate = user.getGender() != null && user.getGender() == 2 ? 18.0 : 12.0;
                
                double fatMass = w * (currentFatRate / 100.0);
                double leanMass = w - fatMass;
                double targetWeight = leanMass / (1.0 - (targetFatRate / 100.0));
                
                double fatLossNeededInKg = w - targetWeight;
                if (fatLossNeededInKg > 0) {
                    double fatLossNeededInJin = fatLossNeededInKg * 2.0;
                    double targetDays = user.getCustomGoalDays();
                    double calculatedOffset = (-fatLossNeededInJin * 3850.0) / targetDays;
                    
                    // Safety weekly rate cap: max 2 jin per week
                    double ratePerWeek = fatLossNeededInJin / targetDays * 7.0;
                    if (ratePerWeek > 2.0) {
                        calculatedOffset = -1000.0;
                    }
                    goalOffset = calculatedOffset;
                }
            }
        }
        
        double targetCalVal = tdeeVal + goalOffset;
        if (targetCalVal < bmrVal) {
            targetCalVal = bmrVal; // Lock at BMR Floor
        }
        
        user.setTargetCalories(BigDecimal.valueOf(targetCalVal).setScale(1, RoundingMode.HALF_UP));
    }

    @Override
    @Transactional
    public User signIn(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "User not found"));
        
        LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        boolean alreadySignedIn = pointsRecordRepository.existsByUserIdAndTypeAndCreatedAtAfter(
                userId, "SIGN_IN", startOfToday);
        
        if (alreadySignedIn) {
            throw new BizException(400, "您今天已经签过到了，明天再来吧！");
        }
        
        int originalPoints = user.getPoints() != null ? user.getPoints() : 0;
        user.setPoints(originalPoints + 20);
        userRepository.save(user);
        
        PointsRecord record = PointsRecord.builder()
                .userId(userId)
                .amount(20)
                .type("SIGN_IN")
                .remark("每日签到奖励")
                .build();
        pointsRecordRepository.save(record);
        
        log.info("User {} signed in successfully and earned 20 points. New balance: {}", userId, user.getPoints());
        return user;
    }

    @Override
    @Transactional
    public java.util.Map<String, Object> rewardSharePoints(Long userId, String shareType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "User not found"));

        LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        boolean alreadyShared = pointsRecordRepository.existsByUserIdAndTypeAndCreatedAtAfter(
                userId, shareType, startOfToday);

        java.util.Map<String, Object> result = new java.util.HashMap<>();

        if (alreadyShared) {
            log.info("User {} already rewarded for share type {} today", userId, shareType);
            result.put("user", user);
            result.put("rewarded", false);
            result.put("points", user.getPoints() != null ? user.getPoints() : 0);
            return result;
        }

        int originalPoints = user.getPoints() != null ? user.getPoints() : 0;
        int newPoints = originalPoints + 10;
        user.setPoints(newPoints);
        userRepository.save(user);

        String remark = "SHARE_FRIEND".equals(shareType) ? "分享给好友奖励" :
                       ("SHARE_TIMELINE".equals(shareType) ? "分享到朋友圈奖励" : "分享奖励");

        PointsRecord record = PointsRecord.builder()
                .userId(userId)
                .amount(10)
                .type(shareType)
                .remark(remark)
                .build();
        pointsRecordRepository.save(record);

        log.info("User {} shared with type {} and earned 10 points. New balance: {}", userId, shareType, user.getPoints());

        result.put("user", user);
        result.put("rewarded", true);
        result.put("points", user.getPoints());
        return result;
    }

    @Override
    public java.util.List<PointsRecord> getPointsRecords(Long userId) {
        return pointsRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}

