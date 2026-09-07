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
@Table(name = "tb_team_ai_roast", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"team_id", "roast_date"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamAiRoast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "roast_date", nullable = false)
    private LocalDate roastDate;

    @Column(name = "mvp_user_id")
    private Long mvpUserId;

    @Column(name = "mvp_name", length = 50)
    private String mvpName;

    @Column(name = "slacker_user_id")
    private Long slackerUserId;

    @Column(name = "slacker_name", length = 50)
    private String slackerName;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "quote", length = 255)
    private String quote;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
