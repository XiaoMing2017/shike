package com.shike.service;

import com.shike.model.dto.MonthDashboardDTO;
import com.shike.model.dto.MonthSummaryDTO;
import com.shike.model.dto.WeekDashboardDTO;
import com.shike.model.entity.DietRecord;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

import java.util.Map;

public interface DietService {
    DietRecord recognizeMeal(MultipartFile file, String hint, Long userId);
    DietRecord recordMeal(Long userId, String mealType, String foodItemsJson, String oilLevel, String imageUrl);
    List<DietRecord> getDailyRecords(Long userId, LocalDate date);
    List<MonthSummaryDTO> getMonthSummary(Long userId, int year, int month);
    Map<String, Object> diagnoseDiet(Long userId, LocalDate date);
    WeekDashboardDTO getWeekDashboard(Long userId, String dateStr);
    MonthDashboardDTO getMonthDashboard(Long userId, Integer year, Integer month);
}
