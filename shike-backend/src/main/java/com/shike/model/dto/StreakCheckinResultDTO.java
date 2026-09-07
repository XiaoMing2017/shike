package com.shike.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreakCheckinResultDTO {
    private Integer currentStreak;
    private Integer cycleDay; // 1-7
    private Integer rewardPoints;
    private String itemReward;
    private String itemRewardName;
    private String itemRewardIcon;
    private Integer totalUserPoints;
    private Integer nextDayPoints;
    private String message;
}
