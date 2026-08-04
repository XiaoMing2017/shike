package com.shike.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_admin_audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_username", length = 50, nullable = false)
    private String adminUsername;

    @Column(length = 64, nullable = false)
    private String action; // BAN_USER, UNBAN_USER, ADJUST_POINTS, UPDATE_AI_LIMIT, DISBAND_TEAM

    @Column(length = 128)
    private String target; // 用户ID, 战队ID等

    @Column(length = 255)
    private String details;

    @Column(length = 64)
    private String ip;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
