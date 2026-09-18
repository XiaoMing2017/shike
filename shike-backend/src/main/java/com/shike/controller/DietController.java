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

import java.math.BigDecimal;
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
            @RequestParam(value = "lang", required = false, defaultValue = "zh") String lang,
            @RequestParam(value = "userId", required = false) Long userId) {
        System.out.println("[DEBUG-CONTROLLER] recognizeMeal called. Original hint: " + hint + ", lang: " + lang);
        String decodedHint = resolveMultipartString(hint);
        System.out.println("[DEBUG-CONTROLLER] recognizeMeal resolved hint: " + decodedHint);
        DietRecord record = dietService.recognizeMeal(file, decodedHint, userId, lang);
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

    @GetMapping("/today")
    public ResultDTO<java.util.Map<String, Object>> getTodaySummary(
            @RequestParam(value = "userId", required = false) String userIdParam) {
        Long userId = 1L;
        if (userIdParam != null && !userIdParam.isEmpty()) {
            try {
                String numeric = userIdParam.replaceAll("\\D+", "");
                if (!numeric.isEmpty()) {
                    userId = Long.parseLong(numeric);
                }
            } catch (Exception ignored) {}
        }
        LocalDate today = LocalDate.now();
        List<DietRecord> records = dietService.getDailyRecords(userId, today);
        double totalCal = 0;
        double totalProtein = 0;
        double totalCarbs = 0;
        double totalFat = 0;
        for (DietRecord r : records) {
            double rCal = r.getTotalCalories() != null ? r.getTotalCalories().doubleValue() : 0;
            double rProtein = r.getTotalProtein() != null ? r.getTotalProtein().doubleValue() : 0;
            double rCarbs = r.getTotalCarbs() != null ? r.getTotalCarbs().doubleValue() : 0;
            double rFat = r.getTotalFat() != null ? r.getTotalFat().doubleValue() : 0;

            if (rCal > 0 && rProtein == 0 && rCarbs == 0 && rFat == 0) {
                rProtein = Math.round((rCal * 0.25) / 4.0);
                rCarbs = Math.round((rCal * 0.45) / 4.0);
                rFat = Math.round((rCal * 0.30) / 9.0);
                r.setTotalProtein(BigDecimal.valueOf(rProtein));
                r.setTotalCarbs(BigDecimal.valueOf(rCarbs));
                r.setTotalFat(BigDecimal.valueOf(rFat));
            }
            totalCal += rCal;
            totalProtein += rProtein;
            totalCarbs += rCarbs;
            totalFat += rFat;
        }
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("totalCalories", Math.round(totalCal));
        map.put("totalProtein", Math.round(totalProtein));
        map.put("totalCarbs", Math.round(totalCarbs));
        map.put("totalFat", Math.round(totalFat));
        map.put("records", records);
        return ResultDTO.success(map);
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

    @GetMapping("/week-dashboard")
    public ResultDTO<com.shike.model.dto.WeekDashboardDTO> getWeekDashboard(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "date", required = false) String dateStr) {
        com.shike.model.dto.WeekDashboardDTO dashboard = dietService.getWeekDashboard(userId, dateStr);
        return ResultDTO.success(dashboard);
    }

    @GetMapping("/month-dashboard")
    public ResultDTO<com.shike.model.dto.MonthDashboardDTO> getMonthDashboard(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month) {
        com.shike.model.dto.MonthDashboardDTO dashboard = dietService.getMonthDashboard(userId, year, month);
        return ResultDTO.success(dashboard);
    }

    @PostMapping("/record-weight")
    public ResultDTO<Void> recordWeight(
            @RequestParam("userId") Long userId,
            @RequestParam("weight") java.math.BigDecimal weight,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        dietService.recordWeight(userId, weight, date);
        return ResultDTO.success(null);
    }

    @DeleteMapping("/records")
    public ResultDTO<Void> clearRecords(@RequestParam(value = "userId", required = false) Long userId) {
        dietService.clearRecords(userId);
        return ResultDTO.success(null);
    }
}
