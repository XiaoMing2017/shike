package com.shike.service.impl;

import com.shike.model.entity.WaterRecord;
import com.shike.repository.WaterRecordRepository;
import com.shike.service.WaterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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
}
