package com.shike.model.dto;

import com.shike.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDTO {
    private User user;
    private Integer todayAiCount; // AI recognition count today (0-10)
    private Long totalDietCount;  // Total diet records
    private Long totalExerciseCount; // Total exercise records
}
