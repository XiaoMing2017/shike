package com.shike.service.impl;

import com.shike.common.BizException;
import com.shike.model.dto.UserLoginDTO;
import com.shike.model.entity.User;
import com.shike.repository.UserRepository;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

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
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
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
    public User updateProfile(Long userId, Integer age, Integer gender, BigDecimal height, BigDecimal weight, String activityLevel, String goal) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "User not found"));

        user.setAge(age);
        user.setGender(gender);
        user.setHeight(height);
        user.setWeight(weight);
        user.setActivityLevel(activityLevel);
        user.setGoal(goal);

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
        }
        double targetCalVal = tdeeVal + goalOffset;
        user.setTargetCalories(BigDecimal.valueOf(targetCalVal).setScale(1, RoundingMode.HALF_UP));
    }
}
