package com.shike.repository;

import com.shike.model.entity.WeightRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeightRecordRepository extends JpaRepository<WeightRecord, Long> {

    Optional<WeightRecord> findByUserIdAndRecordDate(Long userId, LocalDate recordDate);

    List<WeightRecord> findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(Long userId, LocalDate startDate, LocalDate endDate);

    List<WeightRecord> findByUserIdOrderByRecordDateAsc(Long userId);
}
