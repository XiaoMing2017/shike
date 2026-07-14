package com.shike.service;

import com.shike.model.entity.WaterRecord;

import java.time.LocalDate;

public interface WaterService {
    WaterRecord getDailyRecord(Long userId, LocalDate date);
    WaterRecord addWater(Long userId, LocalDate date, int amount);
    WaterRecord reduceWater(Long userId, LocalDate date, int amount);
}
