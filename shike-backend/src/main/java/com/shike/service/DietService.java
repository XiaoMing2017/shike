package com.shike.service;

import com.shike.model.entity.DietRecord;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface DietService {
    DietRecord recognizeMeal(MultipartFile file);
    DietRecord recordMeal(Long userId, String mealType, String foodItemsJson, String oilLevel, String imageUrl);
    List<DietRecord> getDailyRecords(Long userId, LocalDate date);
}
