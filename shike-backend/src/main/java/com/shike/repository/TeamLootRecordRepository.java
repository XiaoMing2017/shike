package com.shike.repository;

import com.shike.model.entity.TeamLootRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamLootRecordRepository extends JpaRepository<TeamLootRecord, Long> {
    Optional<TeamLootRecord> findByUserIdAndTeamIdAndSettlementDate(Long userId, Long teamId, LocalDate settlementDate);
    List<TeamLootRecord> findByUserIdAndStatusOrderBySettlementDateDesc(Long userId, String status);
    List<TeamLootRecord> findByUserIdAndTeamIdAndStatusOrderBySettlementDateDesc(Long userId, Long teamId, String status);
    List<TeamLootRecord> findByTeamIdAndSettlementDate(Long teamId, LocalDate settlementDate);
}
