package com.shike.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_streak_record", indexes = {
        @Index(name = "idx_user_date", columnList = "user_id, checkin_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreakRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "checkin_date", nullable = false)
    private LocalDate checkinDate;

    @Column(name = "streak_day", nullable = false)
    private Integer streakDay; // 1-7

    @Column(name = "reward_points", nullable = false)
    private Integer rewardPoints;

    @Column(name = "item_reward", length = 50)
    private String itemReward; // e.g. CHEAT_SHIELD, SERUM_REVIVAL, GOLDEN_CHEST

    @Column(name = "is_makeup", columnDefinition = "tinyint(1) default 0")
    private Boolean isMakeup;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
