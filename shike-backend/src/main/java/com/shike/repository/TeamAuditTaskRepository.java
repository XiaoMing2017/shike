package com.shike.repository;

import com.shike.model.entity.TeamAuditTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamAuditTaskRepository extends JpaRepository<TeamAuditTask, Long> {
    List<TeamAuditTask> findByTeamIdOrderByCreatedAtDesc(Long teamId);
    List<TeamAuditTask> findByTargetIdAndStatus(Long targetId, String status);
    List<TeamAuditTask> findByStatusAndExpireAtBefore(String status, LocalDateTime now);
    Optional<TeamAuditTask> findTopByTargetIdAndStatusOrderByCreatedAtDesc(Long targetId, String status);
}
