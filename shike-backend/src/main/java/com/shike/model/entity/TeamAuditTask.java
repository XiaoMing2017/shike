package com.shike.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_team_audit_task")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamAuditTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING"; // PENDING, COMPLETED, EXPIRED, DEFLECTED

    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;

    @Column(name = "reward_points", nullable = false)
    @Builder.Default
    private Integer rewardPoints = 20;

    @Column(name = "diet_record_id")
    private Long dietRecordId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
