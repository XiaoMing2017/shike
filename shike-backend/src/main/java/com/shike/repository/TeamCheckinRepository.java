package com.shike.repository;

import com.shike.model.entity.TeamCheckin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TeamCheckinRepository extends JpaRepository<TeamCheckin, Long> {
    List<TeamCheckin> findByTeamId(Long teamId);
    List<TeamCheckin> findByTeamIdAndCheckinDate(Long teamId, LocalDate date);
    List<TeamCheckin> findByTeamIdAndUserId(Long teamId, Long userId);
}
