package com.shike.service.impl;

import com.shike.common.BizException;
import com.shike.model.dto.AdminLoginDTO;
import com.shike.model.dto.AdminStatsDTO;
import com.shike.model.dto.AdminTeamDTO;
import com.shike.model.dto.AdminUserDTO;
import com.shike.model.dto.UserDetailRecordsDTO;
import com.shike.model.entity.DietRecord;
import com.shike.model.entity.Team;
import com.shike.model.entity.TeamCheckin;
import com.shike.model.entity.TeamMember;
import com.shike.model.entity.User;
import com.shike.repository.*;
import com.shike.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final DietRecordRepository dietRecordRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final WaterRecordRepository waterRecordRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamCheckinRepository teamCheckinRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public AdminLoginDTO login(AdminLoginDTO loginDTO) {
        if (loginDTO == null || !("admin".equals(loginDTO.getUsername()) && "shike123456".equals(loginDTO.getPassword()))) {
            throw new BizException(401, "管理员账号或密码错误");
        }

        String token = "shike-admin-" + UUID.randomUUID().toString().replace("-", "");
        try {
            stringRedisTemplate.opsForValue().set("shike:admin:token:" + token, "admin", 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to set admin token in Redis: {}", e.getMessage());
        }

        return AdminLoginDTO.builder()
                .username("admin")
                .token(token)
                .adminName("超级管理员")
                .build();
    }

    @Override
    public AdminStatsDTO getDashboardStats() {
        LocalDate today = LocalDate.now();
        String todayStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Long totalUsers = userRepository.count();
        
        List<User> allUsers = userRepository.findAll();
        Long todayNewUsers = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().toLocalDate().equals(today))
                .count();

        Long totalPoints = allUsers.stream()
                .mapToLong(u -> u.getPoints() != null ? u.getPoints() : 0)
                .sum();

        Long todayDietRecords = dietRecordRepository.countByRecordDate(today);
        
        // Auto-clean ghost active teams with 0 members
        List<Team> activeList = teamRepository.findByStatus("ACTIVE");
        for (Team t : activeList) {
            List<TeamMember> mList = teamMemberRepository.findByTeamId(t.getId());
            if (mList == null || mList.isEmpty()) {
                t.setStatus("DISBANDED");
                teamRepository.save(t);
            }
        }
        Long activeTeams = teamRepository.countByStatus("ACTIVE");

        // Sum AI recognitions across all users today & compute last 7 days trend
        long todayAiRecognitions = 0;
        List<AdminStatsDTO.AiTrendItem> aiTrendList = new ArrayList<>();
        long totalHistoricalAiCount = 0;

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            long dayAiCount = 0;

            try {
                Set<String> keys = stringRedisTemplate.keys("shike:ai:limit:*:" + dateStr);
                if (keys != null && !keys.isEmpty()) {
                    for (String key : keys) {
                        String val = stringRedisTemplate.opsForValue().get(key);
                        if (val != null) {
                            dayAiCount += Long.parseLong(val);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to query Redis AI recognition keys for {}: {}", dateStr, e.getMessage());
            }

            // Fallback: If Redis is cleared, estimate from DB diet records
            if (dayAiCount == 0) {
                dayAiCount = dietRecordRepository.countByRecordDate(date);
            }

            if (i == 0) {
                todayAiRecognitions = dayAiCount;
            }

            totalHistoricalAiCount += dayAiCount;
            aiTrendList.add(AdminStatsDTO.AiTrendItem.builder()
                    .date(date.format(DateTimeFormatter.ofPattern("MM-dd")))
                    .count(dayAiCount)
                    .build());
        }

        // Estimated Cost (0.02 CNY per AI Vision Request)
        double estimatedCost = Math.round(totalHistoricalAiCount * 0.02 * 100.0) / 100.0;

        // Calculate distinct active users today (DAU)
        Set<Long> activeUserIds = new HashSet<>();
        allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().toLocalDate().equals(today))
                .forEach(u -> activeUserIds.add(u.getId()));

        allUsers.stream()
                .filter(u -> u.getUpdatedAt() != null && u.getUpdatedAt().toLocalDate().equals(today))
                .forEach(u -> activeUserIds.add(u.getId()));

        dietRecordRepository.findByRecordDate(today).forEach(d -> activeUserIds.add(d.getUserId()));
        exerciseRecordRepository.findByRecordDate(today).forEach(e -> activeUserIds.add(e.getUserId()));
        waterRecordRepository.findByRecordDate(today).forEach(w -> activeUserIds.add(w.getUserId()));
        teamCheckinRepository.findByCheckinDate(today).forEach(tc -> activeUserIds.add(tc.getUserId()));

        try {
            Set<String> keys = stringRedisTemplate.keys("shike:ai:limit:*:" + todayStr);
            if (keys != null) {
                for (String key : keys) {
                    String[] parts = key.split(":");
                    if (parts.length >= 4) {
                        try {
                            activeUserIds.add(Long.parseLong(parts[3]));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse Redis active user keys: {}", e.getMessage());
        }

        Long todayActiveUsers = (long) activeUserIds.size();

        return AdminStatsDTO.builder()
                .totalUsers(totalUsers)
                .todayNewUsers(todayNewUsers)
                .todayAiRecognitions(todayAiRecognitions)
                .todayDietRecords(todayDietRecords)
                .todayActiveUsers(todayActiveUsers)
                .activeTeams(activeTeams)
                .totalPoints(totalPoints)
                .estimatedAiCost(estimatedCost)
                .aiTrend(aiTrendList)
                .build();
    }

    @Override
    public List<AdminUserDTO> getAllUsers() {
        LocalDate today = LocalDate.now();
        String todayStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        List<AdminUserDTO> result = new ArrayList<>();

        for (User u : users) {
            int aiCount = 0;
            try {
                String redisKey = "shike:ai:limit:" + u.getId() + ":" + todayStr;
                String val = stringRedisTemplate.opsForValue().get(redisKey);
                if (val != null) {
                    aiCount = Integer.parseInt(val);
                }
            } catch (Exception e) {
                log.warn("Failed to get Redis AI limit for user {}: {}", u.getId(), e.getMessage());
            }

            Long dietCount = dietRecordRepository.countByUserId(u.getId());
            Long exerciseCount = exerciseRecordRepository.countByUserId(u.getId());

            result.add(AdminUserDTO.builder()
                    .user(u)
                    .todayAiCount(aiCount)
                    .totalDietCount(dietCount)
                    .totalExerciseCount(exerciseCount)
                    .build());
        }

        return result;
    }

    @Override
    public UserDetailRecordsDTO getUserDetailRecords(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "User not found"));

        return UserDetailRecordsDTO.builder()
                .user(user)
                .dietRecords(dietRecordRepository.findByUserIdOrderByRecordDateDesc(userId))
                .exerciseRecords(exerciseRecordRepository.findByUserIdOrderByRecordDateDesc(userId))
                .waterRecords(waterRecordRepository.findByUserIdOrderByRecordDateDesc(userId))
                .teamCheckins(teamCheckinRepository.findByUserIdOrderByCheckinDateDesc(userId))
                .build();
    }

    @Override
    public List<AdminTeamDTO> getAllTeams() {
        List<Team> teams = teamRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        List<AdminTeamDTO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Team t : teams) {
            User creator = userRepository.findById(t.getCreatorId()).orElse(null);
            String creatorName = creator != null && creator.getNickname() != null ? creator.getNickname() : "微信用户";
            String creatorAvatar = creator != null && creator.getAvatarUrl() != null ? creator.getAvatarUrl() : "";

            LocalDate teamStartDate = t.getCreatedAt().toLocalDate();
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(teamStartDate, today);
            int currentDay = (int) daysBetween + 1;
            if (currentDay < 1) currentDay = 1;
            if (currentDay > t.getTargetDays()) currentDay = t.getTargetDays();

            List<TeamMember> members = teamMemberRepository.findByTeamId(t.getId());
            List<AdminTeamDTO.MemberItem> memberItems = new ArrayList<>();

            for (TeamMember m : members) {
                User u = userRepository.findById(m.getUserId()).orElse(null);
                String nickname = u != null && u.getNickname() != null ? u.getNickname() : "微信用户";
                String avatarUrl = u != null && u.getAvatarUrl() != null ? u.getAvatarUrl() : "";
                int points = u != null && u.getPoints() != null ? u.getPoints() : 0;
                boolean isCreator = m.getUserId().equals(t.getCreatorId());

                boolean todayChecked = false;
                List<DietRecord> todayDiets = dietRecordRepository.findByUserIdAndRecordDate(m.getUserId(), today);
                if (!todayDiets.isEmpty()) {
                    java.math.BigDecimal totalTodayCalories = todayDiets.stream()
                            .map(DietRecord::getTotalCalories)
                            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                    java.math.BigDecimal budget = (u != null && u.getTargetCalories() != null) 
                            ? u.getTargetCalories() 
                            : java.math.BigDecimal.valueOf(2000.0);
                    todayChecked = totalTodayCalories.compareTo(budget) <= 0;
                }

                List<TeamCheckin> checkins = teamCheckinRepository.findByTeamIdAndUserId(t.getId(), m.getUserId());
                int successCount = (int) checkins.stream().filter(c -> Boolean.TRUE.equals(c.getIsSuccess())).count();
                if (todayChecked && checkins.stream().noneMatch(c -> c.getCheckinDate().equals(today))) {
                    successCount++;
                }

                memberItems.add(AdminTeamDTO.MemberItem.builder()
                        .userId(m.getUserId())
                        .nickname(nickname)
                        .avatarUrl(avatarUrl)
                        .isCreator(isCreator)
                        .points(points)
                        .todayChecked(todayChecked)
                        .successCount(successCount)
                        .joinedAt(m.getJoinedAt())
                        .build());
            }

            // Fallback: If creator is not in tb_team_member table for this team, automatically include creator as a member
            if (memberItems.stream().noneMatch(m -> m.getUserId().equals(t.getCreatorId())) && creator != null) {
                boolean todayChecked = false;
                List<DietRecord> todayDiets = dietRecordRepository.findByUserIdAndRecordDate(creator.getId(), today);
                if (!todayDiets.isEmpty()) {
                    java.math.BigDecimal totalTodayCalories = todayDiets.stream()
                            .map(DietRecord::getTotalCalories)
                            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                    java.math.BigDecimal budget = creator.getTargetCalories() != null 
                            ? creator.getTargetCalories() 
                            : java.math.BigDecimal.valueOf(2000.0);
                    todayChecked = totalTodayCalories.compareTo(budget) <= 0;
                }

                List<TeamCheckin> checkins = teamCheckinRepository.findByTeamIdAndUserId(t.getId(), creator.getId());
                int successCount = (int) checkins.stream().filter(c -> Boolean.TRUE.equals(c.getIsSuccess())).count();
                if (todayChecked && checkins.stream().noneMatch(c -> c.getCheckinDate().equals(today))) {
                    successCount++;
                }

                memberItems.add(0, AdminTeamDTO.MemberItem.builder()
                        .userId(creator.getId())
                        .nickname(creatorName)
                        .avatarUrl(creatorAvatar)
                        .isCreator(true)
                        .points(creator.getPoints() != null ? creator.getPoints() : 0)
                        .todayChecked(todayChecked)
                        .successCount(successCount)
                        .joinedAt(t.getCreatedAt())
                        .build());
            }

            result.add(AdminTeamDTO.builder()
                    .id(t.getId())
                    .teamName(t.getTeamName())
                    .creatorId(t.getCreatorId())
                    .creatorName(creatorName)
                    .creatorAvatar(creatorAvatar)
                    .inviteCode(t.getInviteCode())
                    .targetDays(t.getTargetDays())
                    .currentDay(currentDay)
                    .depositPoints(t.getDepositPoints())
                    .status(t.getStatus())
                    .createdAt(t.getCreatedAt())
                    .memberCount(memberItems.size())
                    .members(memberItems)
                    .build());
        }

        return result;
    }

    @Override
    public void updateTeamStatus(Long teamId, String status) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "Team not found"));
        team.setStatus(status);
        teamRepository.save(team);
        log.info("Admin updated team {} status to {}", teamId, status);
    }

    @Override
    public String exportUsersCsv() {
        StringBuilder csv = new StringBuilder();
        // UTF-8 BOM to prevent Excel encoding issue
        csv.append("\uFEFF");
        csv.append("用户ID,微信OpenID,用户昵称,性别,年龄,身高(cm),体重(kg),BMR(kcal),TDEE(kcal),目标摄入(kcal),契约积分,注册时间\n");

        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        for (User u : users) {
            String genderStr = u.getGender() != null && u.getGender() == 2 ? "女" : (u.getGender() != null && u.getGender() == 1 ? "男" : "未设置");
            String createdAt = u.getCreatedAt() != null ? u.getCreatedAt().toString().replace("T", " ") : "";

            csv.append(u.getId()).append(",")
                    .append(escapeCsv(u.getOpenid())).append(",")
                    .append(escapeCsv(u.getNickname() != null ? u.getNickname() : "微信用户")).append(",")
                    .append(genderStr).append(",")
                    .append(u.getAge() != null ? u.getAge() : "").append(",")
                    .append(u.getHeight() != null ? u.getHeight() : "").append(",")
                    .append(u.getWeight() != null ? u.getWeight() : "").append(",")
                    .append(u.getBmr() != null ? u.getBmr() : "").append(",")
                    .append(u.getTdee() != null ? u.getTdee() : "").append(",")
                    .append(u.getTargetCalories() != null ? u.getTargetCalories() : "").append(",")
                    .append(u.getPoints() != null ? u.getPoints() : 0).append(",")
                    .append(createdAt).append("\n");
        }

        return csv.toString();
    }

    @Override
    public String exportDietsCsv() {
        StringBuilder csv = new StringBuilder();
        // UTF-8 BOM to prevent Excel encoding issue
        csv.append("\uFEFF");
        csv.append("记录ID,打卡日期,用户ID,餐别,摄入总热量(kcal),碳水(g),蛋白质(g),脂肪(g),餐品解析明细\n");

        List<DietRecord> records = dietRecordRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        for (DietRecord r : records) {
            csv.append(r.getId()).append(",")
                    .append(r.getRecordDate() != null ? r.getRecordDate().toString() : "").append(",")
                    .append(r.getUserId()).append(",")
                    .append(escapeCsv(r.getMealType())).append(",")
                    .append(r.getTotalCalories() != null ? r.getTotalCalories() : 0).append(",")
                    .append(r.getTotalCarbs() != null ? r.getTotalCarbs() : 0).append(",")
                    .append(r.getTotalProtein() != null ? r.getTotalProtein() : 0).append(",")
                    .append(r.getTotalFat() != null ? r.getTotalFat() : 0).append(",")
                    .append(escapeCsv(r.getFoodItems())).append("\n");
        }

        return csv.toString();
    }

    private String escapeCsv(String input) {
        if (input == null) return "";
        String escaped = input.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
