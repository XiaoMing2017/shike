package com.shike.repository;

import com.shike.model.entity.TeamAiRoast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamAiRoastRepository extends JpaRepository<TeamAiRoast, Long> {
    Optional<TeamAiRoast> findByTeamIdAndRoastDate(Long teamId, LocalDate roastDate);
    List<TeamAiRoast> findByTeamIdOrderByRoastDateDesc(Long teamId);
}
