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
@Table(name = "tb_team_daily_settlement", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"team_id", "settlement_date"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamDailySettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "total_members", nullable = false)
    private Integer totalMembers;

    @Column(name = "success_members", nullable = false)
    private Integer successMembers;

    @Column(name = "failed_members", nullable = false)
    private Integer failedMembers;

    @Column(name = "penalty_pool", nullable = false)
    private Integer penaltyPool; // 当日违约没收的总底金

    @Column(name = "per_person_reward", nullable = false)
    private Integer perPersonReward; // 每位达标者基础瓜分积分

    @Column(name = "summary_text", length = 255)
    private String summaryText; // 战报简述

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
