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

    long countByTypeAndCreatedAtBetween(String type, LocalDateTime start, LocalDateTime end);

    long countByType(String type);

    long countByTypeAndCreatedAtAfter(String type, LocalDateTime startOfDay);

    List<PointsRecord> findByTypeOrderByCreatedAtDesc(String type);

    long countByTypeIn(List<String> types);

    long countByTypeInAndCreatedAtAfter(List<String> types, LocalDateTime startOfDay);

    List<PointsRecord> findByTypeInOrderByCreatedAtDesc(List<String> types);
}
