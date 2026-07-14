package com.shike.repository;

import com.shike.model.entity.WaterRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface WaterRecordRepository extends JpaRepository<WaterRecord, Long> {
    Optional<WaterRecord> findByUserIdAndRecordDate(Long userId, LocalDate date);
}
