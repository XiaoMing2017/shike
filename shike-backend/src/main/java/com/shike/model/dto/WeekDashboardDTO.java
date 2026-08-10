package com.shike.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeekDashboardDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal targetCalories;
    private Integer totalIntake;
    private Integer totalBurned;
    private Integer avgDailyIntake;
    private Integer avgDailyBurned;
    private Integer accumulatedDeficit;
    private Integer checkinCount;
    private BigDecimal avgProtein;
    private BigDecimal avgCarbs;
    private BigDecimal avgFat;
    private Integer proteinRatio;
    private Integer carbsRatio;
    private Integer fatRatio;
    private String healthRating;
    private String evaluationMessage;
    private BigDecimal weightStart;
    private BigDecimal weightLatest;
    private BigDecimal weightChange;
    private List<DailyItem> dailyDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyItem {
        private LocalDate date;
        private String dayName; // "周一", "周二", etc.
        private Boolean isToday;
        private Integer intake;
        private Integer burned;
        private Integer target;
        private String status; // DEFICIT (缺口/达标), SURPLUS (超标), NORMAL
        private BigDecimal weight;
    }
}
