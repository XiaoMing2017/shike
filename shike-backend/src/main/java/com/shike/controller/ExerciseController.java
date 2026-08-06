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
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "activityName", required = false) String activityName,
            @RequestParam(value = "exerciseType", required = false) String exerciseType,
            @RequestParam(value = "durationMinutes", required = false) Integer durationMinutes,
            @RequestParam(value = "caloriesBurned", required = false) Double caloriesBurned,
            jakarta.servlet.http.HttpServletRequest request) {

        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> body = mapper.readValue(request.getInputStream(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                if (body != null) {
                    if (userId == null && body.containsKey("userId")) userId = Long.valueOf(body.get("userId").toString());
                    if (date == null && body.containsKey("date")) {
                        try { date = LocalDate.parse(body.get("date").toString()); } catch (Exception ignored) {}
                    }
                    if (activityName == null && body.containsKey("activityName")) activityName = body.get("activityName").toString();
                    if (activityName == null && body.containsKey("exerciseType")) activityName = body.get("exerciseType").toString();
                    if (durationMinutes == null && body.containsKey("durationMinutes")) durationMinutes = Integer.valueOf(body.get("durationMinutes").toString());
                    if (caloriesBurned == null && body.containsKey("caloriesBurned")) caloriesBurned = Double.valueOf(body.get("caloriesBurned").toString());
                }
            } catch (Exception ignored) {}
        }

        if (date == null) date = LocalDate.now();
        if (activityName == null || activityName.isBlank()) {
            activityName = (exerciseType != null && !exerciseType.isBlank()) ? exerciseType : "训练动作";
        }
        activityName = activityName.replaceAll("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF\\s]", "");
        if (durationMinutes == null) durationMinutes = 15;

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
