package com.shike.repository;

import com.shike.model.entity.PointLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointLogRepository extends JpaRepository<PointLog, Long> {
    List<PointLog> findByUserIdOrderByCreatedAtDesc(Long userId);
}
