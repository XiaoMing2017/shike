package com.shike.repository;

import com.shike.model.entity.StreakRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StreakRecordRepository extends JpaRepository<StreakRecord, Long> {

    Optional<StreakRecord> findByUserIdAndCheckinDate(Long userId, LocalDate checkinDate);

    Optional<StreakRecord> findTopByUserIdOrderByCheckinDateDesc(Long userId);

    List<StreakRecord> findByUserIdAndCheckinDateBetweenOrderByCheckinDateAsc(Long userId, LocalDate start, LocalDate end);

    List<StreakRecord> findByUserIdOrderByCheckinDateDesc(Long userId);
}
