package com.shike.controller;

import com.shike.common.BizException;
import com.shike.common.ResultDTO;
import com.shike.model.dto.WebAuthResponseDTO;
import com.shike.model.dto.WebLoginDTO;
import com.shike.model.dto.WebRegisterDTO;
import com.shike.model.entity.User;
import com.shike.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/user/web")
@RequiredArgsConstructor
@Slf4j
public class WebAuthController {

    private final UserRepository userRepository;

    @PostMapping("/register")
    @Transactional
    public ResultDTO<WebAuthResponseDTO> register(@RequestBody @Valid WebRegisterDTO dto) {
        String email = dto.getEmail().trim().toLowerCase();
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            throw new BizException("Email is already registered. Please sign in instead.");
        }

        // Calculate BMR and TDEE using international Mifflin-St Jeor formula
        double weight = (dto.getWeight() != null) ? dto.getWeight().doubleValue() : 70.0;
        double height = (dto.getHeight() != null) ? dto.getHeight().doubleValue() : 175.0;
        int age = (dto.getAge() != null) ? dto.getAge() : 25;
        int gender = (dto.getGender() != null) ? dto.getGender() : 1; // 1-male, 2-female

        double bmrVal;
        if (gender == 2) {
            bmrVal = 10 * weight + 6.25 * height - 5 * age - 161;
        } else {
            bmrVal = 10 * weight + 6.25 * height - 5 * age + 5;
        }

        double multiplier = 1.2;
        String act = (dto.getActivityLevel() != null) ? dto.getActivityLevel().toUpperCase() : "SEDENTARY";
        switch (act) {
            case "LIGHT" -> multiplier = 1.375;
            case "MODERATE" -> multiplier = 1.55;
            case "ACTIVE" -> multiplier = 1.725;
            default -> multiplier = 1.2;
        }

        double tdeeVal = bmrVal * multiplier;
        double targetCal;
        String goal = (dto.getGoal() != null) ? dto.getGoal().toUpperCase() : "LOSE_WEIGHT";
        switch (goal) {
            case "LOSE_WEIGHT" -> targetCal = tdeeVal - 500;
            case "GAIN_MUSCLE" -> targetCal = tdeeVal + 300;
            default -> targetCal = tdeeVal;
        }

        // Safety floor
        double safetyFloor = (gender == 2) ? 1200.0 : 1500.0;
        if (targetCal < safetyFloor) {
            targetCal = safetyFloor;
        }

        String openid = "email:" + email;
        String nickname = (dto.getNickname() != null && !dto.getNickname().trim().isEmpty())
                ? dto.getNickname().trim()
                : email.split("@")[0];

        User user = User.builder()
                .openid(openid)
                .email(email)
                .password(dto.getPassword()) // Can be hashed
                .nickname(nickname)
                .gender(gender)
                .age(age)
                .height(BigDecimal.valueOf(height).setScale(1, RoundingMode.HALF_UP))
                .weight(BigDecimal.valueOf(weight).setScale(1, RoundingMode.HALF_UP))
                .customGoalWeight(dto.getTargetWeight())
                .activityLevel(act)
                .goal(goal)
                .bmr(BigDecimal.valueOf(bmrVal).setScale(1, RoundingMode.HALF_UP))
                .tdee(BigDecimal.valueOf(tdeeVal).setScale(1, RoundingMode.HALF_UP))
                .targetCalories(BigDecimal.valueOf(targetCal).setScale(0, RoundingMode.HALF_UP))
                .vipType("NORMAL")
                .points(200)
                .currentStreak(1)
                .status("ENABLED")
                .build();

        user = userRepository.save(user);
        String token = "jwt_" + UUID.randomUUID().toString().replace("-", "");

        log.info("Registered web user: {}, id: {}, targetCalories: {}", email, user.getId(), user.getTargetCalories());
        return ResultDTO.success(new WebAuthResponseDTO(token, user));
    }

    @PostMapping("/login")
    public ResultDTO<WebAuthResponseDTO> login(@RequestBody @Valid WebLoginDTO dto) {
        String email = dto.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BizException("Account with this email does not exist."));

        if (!dto.getPassword().equals(user.getPassword())) {
            throw new BizException("Invalid password. Please try again.");
        }

        String token = "jwt_" + UUID.randomUUID().toString().replace("-", "");
        return ResultDTO.success(new WebAuthResponseDTO(token, user));
    }
}
