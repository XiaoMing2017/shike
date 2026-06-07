package com.shike.scheduler;

import com.shike.model.entity.DietRecord;
import com.shike.model.entity.Team;
import com.shike.model.entity.TeamCheckin;
import com.shike.model.entity.TeamMember;
import com.shike.model.entity.User;
import com.shike.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TeamSettleScheduler {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final DietRecordRepository dietRecordRepository;
    private final TeamCheckinRepository teamCheckinRepository;

    /**
     * Runs every day at 00:05 AM to settle the checkins for the previous day.
     * We check the previous day's calorie logs.
     */
    @Scheduled(cron = "0 5 0 * * ?")
    @Transactional
    public void settleDailyChallenge() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Starting team checkin settlement for date: {}", yesterday);

        // 1. Get all active teams
        List<Team> activeTeams = teamRepository.findAll().stream()
                .filter(t -> "ACTIVE".equals(t.getStatus()))
                .toList();

        for (Team team : activeTeams) {
            log.info("Settling team: {} (ID: {})", team.getTeamName(), team.getId());

            // 2. Get all members of the team
            List<TeamMember> members = teamMemberRepository.findByTeamId(team.getId());

            for (TeamMember member : members) {
                User user = userRepository.findById(member.getUserId()).orElse(null);
                if (user == null) continue;

                // 3. Query all diet records for the user yesterday
                List<DietRecord> records = dietRecordRepository.findByUserIdAndRecordDate(member.getUserId(), yesterday);

                BigDecimal totalCalories = records.stream()
                        .map(DietRecord::getTotalCalories)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Check-in is successful if the user logged their diet and did not exceed their calorie budget
                BigDecimal budget = user.getTargetCalories() != null ? user.getTargetCalories() : BigDecimal.valueOf(2000.0);
                boolean isSuccess = !records.isEmpty() && totalCalories.compareTo(budget) <= 0;

                // 4. Save checkin record
                TeamCheckin checkin = TeamCheckin.builder()
                        .teamId(team.getId())
                        .userId(member.getUserId())
                        .checkinDate(yesterday)
                        .isSuccess(isSuccess)
                        .build();
                teamCheckinRepository.save(checkin);

                log.info("Member {} checkin result: success={}, total={}/{} kcal", 
                        user.getNickname(), isSuccess, totalCalories, budget);
            }

            // 5. Check if challenge has completed
            long daysActive = ChronoUnit.DAYS.between(team.getCreatedAt().toLocalDate(), LocalDate.now());
            if (daysActive >= team.getTargetDays()) {
                // Query all checkins for this team to check if anyone failed
                List<TeamCheckin> allCheckins = teamCheckinRepository.findByTeamId(team.getId());
                boolean anyFailures = allCheckins.stream().anyMatch(c -> !c.getIsSuccess());

                if (anyFailures) {
                    team.setStatus("FAILED");
                    log.info("Team {} challenge failed!", team.getTeamName());
                } else {
                    team.setStatus("SUCCESS");
                    log.info("Team {} challenge succeeded!", team.getTeamName());
                }
                teamRepository.save(team);
            }
        }
        log.info("Team checkin settlement finished.");
    }
}
