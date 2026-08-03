package com.shike.controller;

import com.shike.common.ResultDTO;
import com.shike.model.dto.WaterReminderDTO;
import com.shike.model.entity.WaterRecord;
import com.shike.service.WaterService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/water")
@RequiredArgsConstructor
public class WaterController {

    private final WaterService waterService;

    @GetMapping("/daily")
    public ResultDTO<WaterRecord> getDailyRecord(
            @RequestParam("userId") Long userId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        WaterRecord record = waterService.getDailyRecord(userId, date);
        return ResultDTO.success(record);
    }

    @PostMapping("/add")
    public ResultDTO<WaterRecord> addWater(
            @RequestParam("userId") Long userId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("amount") int amount) {
        WaterRecord record = waterService.addWater(userId, date, amount);
        return ResultDTO.success(record);
    }

    @PostMapping("/reduce")
    public ResultDTO<WaterRecord> reduceWater(
            @RequestParam("userId") Long userId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("amount") int amount) {
        WaterRecord record = waterService.reduceWater(userId, date, amount);
        return ResultDTO.success(record);
    }

    @GetMapping("/reminder-check")
    public ResultDTO<WaterReminderDTO> checkWaterReminder(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "targetAmount", defaultValue = "2000") int targetAmount) {
        WaterReminderDTO dto = waterService.getWaterReminderStatus(userId, LocalDate.now(), targetAmount);
        return ResultDTO.success(dto);
    }
}
