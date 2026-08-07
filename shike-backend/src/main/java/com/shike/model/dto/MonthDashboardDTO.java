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
public class MonthDashboardDTO {
    private Integer year;
    private Integer month;
    private String yearMonthStr;
    private BigDecimal targetCalories;
    private Integer totalIntake;
    private Integer totalBurned;
    private Integer avgDailyIntake;
    private Integer avgDailyBurned;
    private Integer accumulatedDeficit;
    private Integer checkinCount;
    private Integer daysInMonth;
    private Integer checkinRate; // %
    private BigDecimal fatLossKg;
    private BigDecimal avgProtein;
    private BigDecimal avgCarbs;
    private BigDecimal avgFat;
    private Integer proteinRatio;
    private Integer carbsRatio;
    private Integer fatRatio;
    private String healthRating;
    private String evaluationMessage;
    private List<WeeklyTrendItem> weeklyTrends;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyTrendItem {
        private String weekName; // "第1周", "第2周", etc.
        private String weekPeriod; // "08.01-08.07"
        private Integer avgIntake;
        private Integer avgBurned;
        private Integer target;
        private String status; // DEFICIT, SURPLUS, NORMAL
    }
}
