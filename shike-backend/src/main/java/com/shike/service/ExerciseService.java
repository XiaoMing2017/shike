package com.shike.service;

import com.shike.model.entity.ExerciseRecord;

import java.time.LocalDate;
import java.util.List;

public interface ExerciseService {
    List<ExerciseRecord> getDailyRecords(Long userId, LocalDate date);
    double getDailyTotalCalories(Long userId, LocalDate date);
    ExerciseRecord addExercise(Long userId, LocalDate date, String activityName, int durationMinutes, Double caloriesBurned);
    void deleteExercise(Long id);
}
