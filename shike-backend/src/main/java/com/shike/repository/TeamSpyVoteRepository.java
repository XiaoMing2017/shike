package com.shike.repository;

import com.shike.model.entity.TeamSpyVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamSpyVoteRepository extends JpaRepository<TeamSpyVote, Long> {
    List<TeamSpyVote> findByTeamIdAndVoteDate(Long teamId, LocalDate voteDate);
    Optional<TeamSpyVote> findByTeamIdAndVoterUserIdAndVoteDate(Long teamId, Long voterUserId, LocalDate voteDate);
}
