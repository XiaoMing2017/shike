package com.shike.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DietRecordDTO {

    @NotNull(message = "userId cannot be null")
    private Long userId;

    @NotBlank(message = "mealType cannot be blank")
    private String mealType; // BREAKFAST, LUNCH, DINNER, SNACK

    @NotBlank(message = "foodItems cannot be blank")
    private String foodItems; // JSON array string

    @NotBlank(message = "oilLevel cannot be blank")
    private String oilLevel; // LIGHT, MODERATE, HEAVY

    private String imageUrl;
}
