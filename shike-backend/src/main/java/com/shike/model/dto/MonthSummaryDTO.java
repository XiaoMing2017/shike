package com.shike.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthSummaryDTO {
    private LocalDate date;
    private String status; // SUCCESS, EXCEEDED, EMPTY
    private BigDecimal totalCalories;
    private BigDecimal targetCalories;
}
