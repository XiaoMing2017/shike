package com.shike.service.impl;

import com.shike.common.BizException;
import com.shike.model.entity.User;
import com.shike.repository.UserRepository;
import com.shike.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public User loginOrRegister(String openid) {
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
                    .activityLevel("SEDENTARY")
                    .goal("MAINTAIN")
                    .build();
            return userRepository.save(newUser);
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
