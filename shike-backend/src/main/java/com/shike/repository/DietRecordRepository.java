package com.shike.repository;

import com.shike.model.entity.DietRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DietRecordRepository extends JpaRepository<DietRecord, Long> {
    List<DietRecord> findByRecordDate(LocalDate date);
    List<DietRecord> findByUserIdAndRecordDate(Long userId, LocalDate date);
    List<DietRecord> findByUserIdAndRecordDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    List<DietRecord> findByUserIdOrderByRecordDateDesc(Long userId);
    Long countByRecordDate(LocalDate date);
    Long countByUserId(Long userId);
}
