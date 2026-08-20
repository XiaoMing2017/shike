package com.shike.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetInteractDTO {
    private Long userId;
    private String actionType; // TOUCH, FEED, EXERCISE_DONE, DIET_RECORDED, WATER_RECORDED, CHAT
    private String userMessage;
}
