package com.shike.scheduler;

import com.shike.model.entity.DietRecord;
import com.shike.model.entity.Team;
import com.shike.model.entity.TeamCheckin;
import com.shike.model.entity.TeamMember;
import com.shike.model.entity.User;
import com.shike.model.entity.PointsRecord;
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
    private final PointsRecordRepository pointsRecordRepository;

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

            if (isSuccess) {
                int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
                user.setPoints(currentPoints + 20);
                userRepository.save(user);

                PointsRecord pRecord = PointsRecord.builder()
                        .userId(user.getId())
                        .amount(20)
                        .type("DAILY_CHECKIN")
                        .remark("契约小队 [" + team.getTeamName() + "] 每日打卡达标奖励")
                        .build();
                pointsRecordRepository.save(pRecord);
            }

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

        // 统计各成员的成功打卡天数，并找出其中的最大成功天数
        java.util.Map<Long, Long> memberSuccessCounts = new java.util.HashMap<>();
        long maxSuccessCount = 0;
        for (TeamMember member : teamMembers) {
            List<TeamCheckin> checkins = teamCheckinRepository.findByTeamIdAndUserId(team.getId(), member.getUserId());
            long successCount = checkins.stream().filter(TeamCheckin::getIsSuccess).count();
            memberSuccessCounts.put(member.getUserId(), successCount);
            if (successCount > maxSuccessCount) {
                maxSuccessCount = successCount;
            }
        }

        // 计算最少达标天数门槛（对赌天数的 60% 以上，向上取整）
        int minRequiredDays = (int) Math.ceil(team.getTargetDays() * 0.6);
        log.info("Team {} (ID: {}) requires at least {} successful checkin days (60% of {} target days) to qualify.", 
                team.getTeamName(), team.getId(), minRequiredDays, team.getTargetDays());

        // 根据最大成功打卡天数判定赢家和输家，且最大成功天数必须达到 60% 门槛
        if (maxSuccessCount >= minRequiredDays) {
            for (TeamMember member : teamMembers) {
                long successCount = memberSuccessCounts.getOrDefault(member.getUserId(), 0L);
                if (successCount == maxSuccessCount) {
                    successMembers.add(member);
                } else {
                    failedMembers.add(member);
                }
            }
        } else {
            // 没有人达到最少打卡天数门槛，则全员失败
            failedMembers.addAll(teamMembers);
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

                    PointsRecord pRecord = PointsRecord.builder()
                            .userId(user.getId())
                            .amount(depPoints)
                            .type("TEAM_REWARD")
                            .remark("对赌挑战小队 [" + team.getTeamName() + "] 成功返还本金")
                            .build();
                    pointsRecordRepository.save(pRecord);
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

                    PointsRecord pRecord = PointsRecord.builder()
                            .userId(user.getId())
                            .amount(depPoints + reward)
                            .type("TEAM_REWARD")
                            .remark("对赌挑战小队 [" + team.getTeamName() + "] 胜利平分积分(返还本金 " + depPoints + " + 奖金 " + reward + ")")
                            .build();
                    pointsRecordRepository.save(pRecord);
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
