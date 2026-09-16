package com.shike.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class WebRegisterDTO {

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String nickname;

    private Integer gender; // 1-male, 2-female

    private Integer age;

    private BigDecimal height;

    private BigDecimal weight;

    private BigDecimal targetWeight;

    private String activityLevel; // SEDENTARY, LIGHT, MODERATE, ACTIVE

    private String goal; // LOSE_WEIGHT, MAINTAIN, GAIN_MUSCLE

    private BigDecimal targetCalories;

    private BigDecimal bmr;

    private BigDecimal tdee;
}
