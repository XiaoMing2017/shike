package com.shike.repository;

import com.shike.model.entity.ExerciseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExerciseRecordRepository extends JpaRepository<ExerciseRecord, Long> {
    List<ExerciseRecord> findByRecordDate(LocalDate date);
    List<ExerciseRecord> findByUserIdAndRecordDate(Long userId, LocalDate date);
    List<ExerciseRecord> findByUserIdAndRecordDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    List<ExerciseRecord> findByUserIdOrderByRecordDateDesc(Long userId);
    Long countByRecordDate(LocalDate date);
    Long countByUserId(Long userId);
}
