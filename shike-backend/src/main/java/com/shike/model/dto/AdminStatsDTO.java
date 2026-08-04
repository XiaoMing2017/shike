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
    private Long totalAiTokens;
    private Long todayMealAiRecognitions;
    private Long todayPlanAiGenerations;
    private String aiCostFormula;
    private List<AiTrendItem> aiTrend;
    private List<AiTrendItem> userRegistrationTrend;
    private Integer globalAiLimit;
    private List<RetentionItem> retentionStats;
    private List<AiTrendItem> dauTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiTrendItem {
        private String date;
        private Long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetentionItem {
        private String date;
        private Long regCount;
        private Long day1Count;
        private Double day1Rate;
        private Long day7Count;
        private Double day7Rate;
    }
}
