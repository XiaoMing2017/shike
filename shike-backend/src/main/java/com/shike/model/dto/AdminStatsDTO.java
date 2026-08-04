package com.shike.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

    private Double estimatedAiCost;
    private List<AiTrendItem> aiTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiTrendItem {
        private String date;
        private Long count;
    }
}
