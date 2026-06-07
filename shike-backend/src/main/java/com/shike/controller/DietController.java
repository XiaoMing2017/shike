package com.shike.controller;

import com.shike.common.ResultDTO;
import com.shike.model.dto.DietRecordDTO;
import com.shike.model.entity.DietRecord;
import com.shike.service.DietService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/diet")
@RequiredArgsConstructor
public class DietController {

    private final DietService dietService;

    @PostMapping("/recognize")
    public ResultDTO<DietRecord> recognizeMeal(@RequestParam("file") MultipartFile file) {
        DietRecord record = dietService.recognizeMeal(file);
        return ResultDTO.success(record);
    }

    @PostMapping("/record")
    public ResultDTO<DietRecord> recordMeal(@RequestBody @Valid DietRecordDTO recordDTO) {
        DietRecord record = dietService.recordMeal(
                recordDTO.getUserId(),
                recordDTO.getMealType(),
                recordDTO.getFoodItems(),
                recordDTO.getOilLevel(),
                recordDTO.getImageUrl()
        );
        return ResultDTO.success(record);
    }

    @GetMapping("/daily")
    public ResultDTO<List<DietRecord>> getDailyRecords(
            @RequestParam("userId") Long userId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<DietRecord> records = dietService.getDailyRecords(userId, date);
        return ResultDTO.success(records);
    }
}
