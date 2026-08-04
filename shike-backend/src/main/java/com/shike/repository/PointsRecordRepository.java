package com.shike.repository;

import com.shike.model.entity.PointsRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PointsRecordRepository extends JpaRepository<PointsRecord, Long> {
    
    List<PointsRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    boolean existsByUserIdAndTypeAndCreatedAtAfter(Long userId, String type, LocalDateTime startOfDay);
    
    boolean existsByUserIdAndType(Long userId, String type);
}
