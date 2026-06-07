package com.shike.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_team")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_name", nullable = false, length = 50)
    private String teamName;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "invite_code", nullable = false, unique = true, length = 10)
    private String inviteCode;

    @Column(name = "target_days", nullable = false)
    private Integer targetDays;

    @Column(nullable = false, length = 20)
    private String status; // ACTIVE, SUCCESS, FAILED

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
