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
            settleTeamDaily(team.getId(), yesterday);
            settleTeamFinal(team.getId());
        }
        log.info("Team checkin settlement finished.");
    }

    @Transactional
    public void settleTeamDaily(Long teamId, LocalDate date) {
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) return;
        
        log.info("Settling team daily: {} (ID: {}) for date: {}", team.getTeamName(), team.getId(), date);
        List<TeamMember> members = teamMemberRepository.findByTeamId(team.getId());

        for (TeamMember member : members) {
            User user = userRepository.findById(member.getUserId()).orElse(null);
            if (user == null) continue;

            // Query all diet records for the user on this date
            List<DietRecord> records = dietRecordRepository.findByUserIdAndRecordDate(member.getUserId(), date);
            BigDecimal totalCalories = records.stream()
                    .map(DietRecord::getTotalCalories)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Check-in is successful if the user logged their diet and did not exceed their calorie budget
            BigDecimal budget = user.getTargetCalories() != null ? user.getTargetCalories() : BigDecimal.valueOf(2000.0);
            boolean isSuccess = !records.isEmpty() && totalCalories.compareTo(budget) <= 0;

            // Remove existing checkin for this date to prevent duplicate records (useful for repeated testing)
            List<TeamCheckin> existing = teamCheckinRepository.findByTeamIdAndUserId(team.getId(), member.getUserId());
            existing.stream()
                    .filter(c -> c.getCheckinDate().equals(date))
                    .forEach(teamCheckinRepository::delete);

            // Save new checkin record
            TeamCheckin checkin = TeamCheckin.builder()
                    .teamId(team.getId())
                    .userId(member.getUserId())
                    .checkinDate(date)
                    .isSuccess(isSuccess)
                    .build();
            teamCheckinRepository.save(checkin);

            log.info("Member {} checkin result for {}: success={}, total={}/{} kcal", 
                    user.getNickname(), date, isSuccess, totalCalories, budget);
        }
    }

    @Transactional
    public void settleTeamFinal(Long teamId) {
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null || !"ACTIVE".equals(team.getStatus())) return;

        long daysActive = ChronoUnit.DAYS.between(team.getCreatedAt().toLocalDate(), LocalDate.now());
        if (daysActive >= team.getTargetDays()) {
            performFinalSettlement(team);
        }
    }

    @Transactional
    public void performFinalSettlement(Team team) {
        log.info("Team {} (ID: {}) challenge reached target days. Commencing final settlement...", team.getTeamName(), team.getId());

        int depPoints = team.getDepositPoints() != null ? team.getDepositPoints() : 100;
        List<TeamMember> teamMembers = teamMemberRepository.findByTeamId(team.getId());
        
        java.util.List<TeamMember> successMembers = new java.util.ArrayList<>();
        java.util.List<TeamMember> failedMembers = new java.util.ArrayList<>();

        for (TeamMember member : teamMembers) {
            List<TeamCheckin> checkins = teamCheckinRepository.findByTeamIdAndUserId(team.getId(), member.getUserId());
            // 检查这期间是否有任何一天的 isSuccess 是 false，或者成功的打卡次数是否达到了目标天数
            long successCount = checkins.stream().filter(TeamCheckin::getIsSuccess).count();
            if (successCount == team.getTargetDays()) {
                successMembers.add(member);
            } else {
                failedMembers.add(member);
            }
        }

        log.info("Settlement stats for team {}: total={}, success={}, failed={}", 
                team.getTeamName(), teamMembers.size(), successMembers.size(), failedMembers.size());

        if (failedMembers.isEmpty()) {
            // 方案 1: 全员成功，全额返还本金
            log.info("All members succeeded in team {}. Refunding all deposit points.", team.getTeamName());
            for (TeamMember member : successMembers) {
                User user = userRepository.findById(member.getUserId()).orElse(null);
                if (user != null) {
                    user.setPoints((user.getPoints() != null ? user.getPoints() : 0) + depPoints);
                    userRepository.save(user);
                }
            }
            team.setStatus("SUCCESS");
        } else if (!successMembers.isEmpty()) {
            // 方案 2: 部分人成功，部分人失败。失败者的积分平分给成功者
            int totalFailedPoints = failedMembers.size() * depPoints;
            int reward = totalFailedPoints / successMembers.size();
            log.info("Partial failure in team {}. Total pot from failures: {} pts, reward per winner: {} pts", 
                    team.getTeamName(), totalFailedPoints, reward);

            for (TeamMember member : successMembers) {
                User user = userRepository.findById(member.getUserId()).orElse(null);
                if (user != null) {
                    user.setPoints((user.getPoints() != null ? user.getPoints() : 0) + depPoints + reward);
                    userRepository.save(user);
                }
            }
            team.setStatus("FAILED");
        } else {
            // 方案 3: 全员失败，全部积分没收，不退还
            log.info("All members failed in team {}. All deposit points forfeited.", team.getTeamName());
            team.setStatus("FAILED");
        }
        teamRepository.save(team);
    }
}
