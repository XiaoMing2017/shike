package com.shike.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsDTO {
    private Long totalUsers;
    private Long todayNewUsers;
    private Long todayAiRecognitions;
    private Long todayDietRecords;
    private Long todayActiveUsers;
    private Long activeTeams;
    private Long totalPoints;
}
