package com.shike.repository;

import com.shike.model.entity.TeamDailySettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamDailySettlementRepository extends JpaRepository<TeamDailySettlement, Long> {
    Optional<TeamDailySettlement> findByTeamIdAndSettlementDate(Long teamId, LocalDate settlementDate);
    List<TeamDailySettlement> findByTeamIdOrderBySettlementDateDesc(Long teamId);
}
