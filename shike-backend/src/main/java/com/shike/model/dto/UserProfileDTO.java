package com.shike.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserProfileDTO {

    @NotNull(message = "userId cannot be null")
    private Long userId;

    @Min(value = 1, message = "age must be positive")
    @Max(value = 120, message = "invalid age")
    private Integer age;

    private Integer gender; // 1-male, 2-female

    @DecimalMin(value = "50.0", message = "invalid height")
    @DecimalMax(value = "250.0", message = "invalid height")
    private BigDecimal height;

    @DecimalMin(value = "20.0", message = "invalid weight")
    @DecimalMax(value = "300.0", message = "invalid weight")
    private BigDecimal weight;

    @NotBlank(message = "activityLevel cannot be blank")
    private String activityLevel; // SEDENTARY, LIGHT, MODERATE, ACTIVE

    @NotBlank(message = "goal cannot be blank")
    private String goal; // LOSE_WEIGHT, MAINTAIN, GAIN_MUSCLE
}
