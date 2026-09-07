package com.shike.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_team_loot_record", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "team_id", "settlement_date"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamLootRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "base_reward", nullable = false)
    private Integer baseReward; // 基础瓜分积分

    @Column(name = "multiplier", nullable = false)
    @Builder.Default
    private Double multiplier = 1.0; // 暴击倍率 (1.0, 1.5, 2.0, 5.0)

    @Column(name = "final_reward", nullable = false)
    private Integer finalReward; // 最终获得的积分

    @Column(name = "item_reward", length = 50)
    private String itemReward; // 抽到的道具 (如: SERUM_CARD, SHIELD_FRAGMENT, AVATAR_FRAME, NONE)

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "UNCLAIMED"; // UNCLAIMED, CLAIMED

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
