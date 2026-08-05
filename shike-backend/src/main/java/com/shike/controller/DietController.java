package com.shike.controller;

import com.shike.common.ResultDTO;
import com.shike.model.dto.DietRecordDTO;
import com.shike.model.dto.MonthSummaryDTO;
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
    public ResultDTO<DietRecord> recognizeMeal(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "hint", required = false) String hint,
            @RequestParam(value = "userId", required = false) Long userId) {
        System.out.println("[DEBUG-CONTROLLER] recognizeMeal called. Original hint: " + hint);
        String decodedHint = resolveMultipartString(hint);
        System.out.println("[DEBUG-CONTROLLER] recognizeMeal resolved hint: " + decodedHint);
        DietRecord record = dietService.recognizeMeal(file, decodedHint, userId);
        return ResultDTO.success(record);
    }

    private String resolveMultipartString(String input) {
        System.out.println("[DEBUG-DECODE] input: " + input);
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        String decoded = input;
        if (input.contains("%")) {
            try {
                decoded = java.net.URLDecoder.decode(input, java.nio.charset.StandardCharsets.UTF_8);
                System.out.println("[DEBUG-DECODE] URL-decoded to: " + decoded);
            } catch (Exception e) {
                System.out.println("[DEBUG-DECODE] URLDecoder failed: " + e.getMessage());
            }
        }
        boolean isIso = true;
        for (int i = 0; i < decoded.length(); i++) {
            if (decoded.charAt(i) > 255) {
                isIso = false;
                break;
            }
        }
        System.out.println("[DEBUG-DECODE] isIso: " + isIso);
        if (isIso) {
            try {
                String utf8 = new String(decoded.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1), java.nio.charset.StandardCharsets.UTF_8);
                System.out.println("[DEBUG-DECODE] ISO to UTF8: " + utf8);
                return utf8;
            } catch (Exception e) {
                System.out.println("[DEBUG-DECODE] ISO conversion failed: " + e.getMessage());
                return decoded;
            }
        }
        return decoded;
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

    @GetMapping("/month-summary")
    public ResultDTO<List<MonthSummaryDTO>> getMonthSummary(
            @RequestParam("userId") Long userId,
            @RequestParam("year") int year,
            @RequestParam("month") int month) {
        List<MonthSummaryDTO> summary = dietService.getMonthSummary(userId, year, month);
        return ResultDTO.success(summary);
    }

    @PostMapping("/diagnose")
    public ResultDTO<java.util.Map<String, Object>> diagnoseDiet(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        java.util.Map<String, Object> diagnosis = dietService.diagnoseDiet(userId, date);
        return ResultDTO.success(diagnosis);
    }
}
