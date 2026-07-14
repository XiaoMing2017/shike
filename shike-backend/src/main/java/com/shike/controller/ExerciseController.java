package com.shike.controller;

import com.shike.common.ResultDTO;
import com.shike.model.entity.ExerciseRecord;
import com.shike.service.ExerciseService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/exercise")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;

    @GetMapping("/daily")
    public ResultDTO<Map<String, Object>> getDailyRecords(
            @RequestParam("userId") Long userId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<ExerciseRecord> records = exerciseService.getDailyRecords(userId, date);
        double totalCalories = exerciseService.getDailyTotalCalories(userId, date);

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("totalCalories", totalCalories);

        return ResultDTO.success(data);
    }

    @PostMapping("/add")
    public ResultDTO<ExerciseRecord> addExercise(
            @RequestParam("userId") Long userId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("activityName") String activityName,
            @RequestParam("durationMinutes") int durationMinutes,
            @RequestParam(value = "caloriesBurned", required = false) Double caloriesBurned) {
        ExerciseRecord record = exerciseService.addExercise(userId, date, activityName, durationMinutes, caloriesBurned);
        return ResultDTO.success(record);
    }

    @DeleteMapping("/delete/{id}")
    public ResultDTO<Void> deleteExercise(@PathVariable("id") Long id) {
        exerciseService.deleteExercise(id);
        return ResultDTO.success(null);
    }

    @PostMapping("/delete")
    public ResultDTO<Void> deleteExercisePost(@RequestParam("id") Long id) {
        exerciseService.deleteExercise(id);
        return ResultDTO.success(null);
    }
}
