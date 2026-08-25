package com.shike.service.impl;

import com.shike.model.dto.WaterReminderDTO;
import com.shike.model.entity.WaterRecord;
import com.shike.repository.WaterRecordRepository;
import com.shike.service.WaterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@Slf4j
public class WaterServiceImpl implements WaterService {

    @Autowired
    private WaterRecordRepository waterRecordRepository;

    @Override
    @Transactional(readOnly = true)
    public WaterRecord getDailyRecord(Long userId, LocalDate date) {
        return waterRecordRepository.findByUserIdAndRecordDate(userId, date)
                .orElseGet(() -> WaterRecord.builder()
                        .userId(userId)
                        .recordDate(date)
                        .amount(0)
                        .build());
    }

    @Override
    @Transactional
    public WaterRecord addWater(Long userId, LocalDate date, int amount) {
        log.info("Adding {}ml water for user {} on date {}", amount, userId, date);
        WaterRecord record = waterRecordRepository.findByUserIdAndRecordDate(userId, date)
                .orElseGet(() -> WaterRecord.builder()
                        .userId(userId)
                        .recordDate(date)
                        .amount(0)
                        .build());
        record.setAmount(record.getAmount() + amount);
        return waterRecordRepository.save(record);
    }

    @Override
    @Transactional
    public WaterRecord reduceWater(Long userId, LocalDate date, int amount) {
        log.info("Reducing {}ml water for user {} on date {}", amount, userId, date);
        WaterRecord record = waterRecordRepository.findByUserIdAndRecordDate(userId, date)
                .orElseGet(() -> WaterRecord.builder()
                        .userId(userId)
                        .recordDate(date)
                        .amount(0)
                        .build());
        int newAmount = Math.max(0, record.getAmount() - amount);
        record.setAmount(newAmount);
        return waterRecordRepository.save(record);
    }

    @Override
    @Transactional(readOnly = true)
    public WaterReminderDTO getWaterReminderStatus(Long userId, LocalDate date, int targetAmount) {
        WaterRecord record = getDailyRecord(userId, date);
        int current = record.getAmount();
        int remaining = Math.max(0, targetAmount - current);

        LocalTime now = LocalTime.now();
        int hour = now.getHour();

        boolean need = false;
        String msg = "";
        int timeSlot = 0;

        if (hour >= 8 && hour < 11) {
            timeSlot = 1;
            if (current < 500) {
                need = true;
                msg = "🥤 晨间补水提醒：开启活力的第一杯水！今日已喝 " + current + "ml";
            }
        } else if (hour >= 14 && hour < 17) {
            timeSlot = 2;
            if (current < 1200) {
                need = true;
                msg = "🥤 下午茶提神提醒：喝杯水缓解疲劳，促进代谢！已喝 " + current + "ml";
            }
        } else if (hour >= 19 && hour < 22) {
            timeSlot = 3;
            if (current < targetAmount) {
                need = true;
                msg = "🥤 晚间补水提醒：距离今日目标 " + targetAmount + "ml 还差 " + remaining + "ml！";
            }
        }

        return WaterReminderDTO.builder()
                .needReminder(need)
                .reminderMsg(msg)
                .currentAmount(current)
                .targetAmount(targetAmount)
                .remainingAmount(remaining)
                .timeSlot(timeSlot)
                .build();
    }
}
