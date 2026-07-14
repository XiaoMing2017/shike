package com.shike.service;

import com.shike.model.dto.MonthSummaryDTO;
import com.shike.model.entity.DietRecord;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface DietService {
    DietRecord recognizeMeal(MultipartFile file, String hint, Long userId);
    DietRecord recordMeal(Long userId, String mealType, String foodItemsJson, String oilLevel, String imageUrl);
    List<DietRecord> getDailyRecords(Long userId, LocalDate date);
    List<MonthSummaryDTO> getMonthSummary(Long userId, int year, int month);
}
