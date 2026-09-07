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
@Table(name = "tb_team_spy_vote", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"team_id", "voter_user_id", "vote_date"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamSpyVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "voter_user_id", nullable = false)
    private Long voterUserId;

    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @Column(name = "vote_date", nullable = false)
    private LocalDate voteDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
