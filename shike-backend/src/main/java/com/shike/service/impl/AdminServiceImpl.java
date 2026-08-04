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
    private final PointLogRepository pointLogRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;
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
        long todayMealAiCount = 0;
        long todayPlanAiCount = 0;
        long totalHistoricalMealCount = 0;
        long totalHistoricalPlanCount = 0;
        List<AdminStatsDTO.AiTrendItem> aiTrendList = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            long dayMealCount = 0;
            long dayPlanCount = 0;

            try {
                Set<String> keys = stringRedisTemplate.keys("shike:ai:limit:*:" + dateStr);
                if (keys != null && !keys.isEmpty()) {
                    for (String key : keys) {
                        String val = stringRedisTemplate.opsForValue().get(key);
                        if (val != null) {
                            dayMealCount += Long.parseLong(val);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to query Redis AI recognition keys for {}: {}", dateStr, e.getMessage());
            }

            try {
                String planVal = stringRedisTemplate.opsForValue().get("shike:ai:plan:count:" + dateStr);
                if (planVal != null) {
                    dayPlanCount = Long.parseLong(planVal);
                }
            } catch (Exception ignored) {}

            if (dayMealCount == 0) {
                dayMealCount = dietRecordRepository.countByRecordDate(date);
            }

            if (i == 0) {
                todayMealAiCount = dayMealCount;
                todayPlanAiCount = dayPlanCount;
            }

            totalHistoricalMealCount += dayMealCount;
            totalHistoricalPlanCount += dayPlanCount;

            aiTrendList.add(AdminStatsDTO.AiTrendItem.builder()
                    .date(date.format(DateTimeFormatter.ofPattern("MM-dd")))
                    .count(dayMealCount + dayPlanCount)
                    .build());
        }

        long todayAiRecognitions = todayMealAiCount + todayPlanAiCount;
        long totalHistoricalAiCount = totalHistoricalMealCount + totalHistoricalPlanCount;
        long totalAiTokens = (totalHistoricalMealCount * 2000L) + (totalHistoricalPlanCount * 3500L);

        // 估算成本 (按平均 1,000 Tokens ￥0.015 算法)
        double estimatedCost = Math.round((totalAiTokens / 1000.0 * 0.015) * 100.0) / 100.0;
        if (estimatedCost == 0.0 && totalHistoricalAiCount > 0) {
            estimatedCost = Math.round(totalHistoricalAiCount * 0.02 * 100.0) / 100.0;
        }

        // ========== 近7天注册用户趋势 & DAU趋势 ==========
        List<AdminStatsDTO.AiTrendItem> registrationTrendList = new ArrayList<>();
        List<AdminStatsDTO.AiTrendItem> dauTrendList = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateLabel = date.format(DateTimeFormatter.ofPattern("MM-dd"));
            String dateFullStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // 每日注册用户数
            long dayRegistrations = allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().toLocalDate().equals(date))
                    .count();
            registrationTrendList.add(AdminStatsDTO.AiTrendItem.builder()
                    .date(dateLabel)
                    .count(dayRegistrations)
                    .build());

            // 每日活跃用户数 (DAU) - 合并饮食/运动/饮水/打卡/AI使用
            Set<Long> dayActiveIds = new HashSet<>();
            // 当天注册
            allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().toLocalDate().equals(date))
                    .forEach(u -> dayActiveIds.add(u.getId()));
            // 当天更新
            allUsers.stream()
                    .filter(u -> u.getUpdatedAt() != null && u.getUpdatedAt().toLocalDate().equals(date))
                    .forEach(u -> dayActiveIds.add(u.getId()));
            // 饮食记录
            dietRecordRepository.findByRecordDate(date).forEach(d -> dayActiveIds.add(d.getUserId()));
            // 运动记录
            exerciseRecordRepository.findByRecordDate(date).forEach(e -> dayActiveIds.add(e.getUserId()));
            // 饮水记录
            waterRecordRepository.findByRecordDate(date).forEach(w -> dayActiveIds.add(w.getUserId()));
            // 小队打卡
            teamCheckinRepository.findByCheckinDate(date).forEach(tc -> dayActiveIds.add(tc.getUserId()));
            // Redis AI使用记录
            try {
                Set<String> aiKeys = stringRedisTemplate.keys("shike:ai:limit:*:" + dateFullStr);
                if (aiKeys != null) {
                    for (String key : aiKeys) {
                        String[] parts = key.split(":");
                        if (parts.length >= 4) {
                            try {
                                dayActiveIds.add(Long.parseLong(parts[3]));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            } catch (Exception ignored) {}

            dauTrendList.add(AdminStatsDTO.AiTrendItem.builder()
                    .date(dateLabel)
                    .count((long) dayActiveIds.size())
                    .build());
        }

        // 今日DAU取最后一天的结果
        Long todayActiveUsers = dauTrendList.isEmpty() ? 0L : dauTrendList.get(dauTrendList.size() - 1).getCount();

        // ========== 留存率 Cohort 分析 ==========
        List<AdminStatsDTO.RetentionItem> retentionList = new ArrayList<>();
        int defaultAiLimit = 10;
        try {
            String limitVal = stringRedisTemplate.opsForValue().get("shike:sys:config:ai_daily_limit");
            if (limitVal != null) {
                defaultAiLimit = Integer.parseInt(limitVal);
            }
        } catch (Exception ignored) {}

        for (int i = 6; i >= 0; i--) {
            LocalDate regDate = today.minusDays(i);
            String regDateLabel = regDate.format(DateTimeFormatter.ofPattern("MM-dd"));

            List<User> regUsers = allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().toLocalDate().equals(regDate))
                    .toList();
            long regCount = regUsers.size();

            long day1Count = 0;
            long day7Count = 0;

            if (regCount > 0) {
                LocalDate day1Date = regDate.plusDays(1);
                LocalDate day7Date = regDate.plusDays(7);

                Set<Long> regUserIds = new HashSet<>();
                regUsers.forEach(u -> regUserIds.add(u.getId()));

                if (!day1Date.isAfter(today)) {
                    Set<Long> activeD1 = getActiveUserIdsForDate(day1Date);
                    day1Count = regUserIds.stream().filter(activeD1::contains).count();
                }

                if (!day7Date.isAfter(today)) {
                    Set<Long> activeD7 = getActiveUserIdsForDate(day7Date);
                    day7Count = regUserIds.stream().filter(activeD7::contains).count();
                }
            }

            double day1Rate = regCount > 0 ? Math.round((day1Count * 100.0 / regCount) * 10.0) / 10.0 : 0.0;
            double day7Rate = regCount > 0 ? Math.round((day7Count * 100.0 / regCount) * 10.0) / 10.0 : 0.0;

            retentionList.add(AdminStatsDTO.RetentionItem.builder()
                    .date(regDateLabel)
                    .regCount(regCount)
                    .day1Count(day1Count)
                    .day1Rate(day1Rate)
                    .day7Count(day7Count)
                    .day7Rate(day7Rate)
                    .build());
        }

        return AdminStatsDTO.builder()
                .totalUsers(totalUsers)
                .todayNewUsers(todayNewUsers)
                .todayAiRecognitions(todayAiRecognitions)
                .todayMealAiRecognitions(todayMealAiCount)
                .todayPlanAiGenerations(todayPlanAiCount)
                .todayDietRecords(todayDietRecords)
                .todayActiveUsers(todayActiveUsers)
                .activeTeams(activeTeams)
                .totalPoints(totalPoints)
                .totalAiTokens(totalAiTokens)
                .estimatedAiCost(estimatedCost)
                .aiCostFormula("膳食识别~2K Token/次，AI计划生成~3.5K Token/次 (按￥0.015/千Token估算)")
                .aiTrend(aiTrendList)
                .userRegistrationTrend(registrationTrendList)
                .dauTrend(dauTrendList)
                .globalAiLimit(defaultAiLimit)
                .retentionStats(retentionList)
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

    private Set<Long> getActiveUserIdsForDate(LocalDate date) {
        Set<Long> activeUserIds = new HashSet<>();
        dietRecordRepository.findByRecordDate(date).forEach(d -> activeUserIds.add(d.getUserId()));
        exerciseRecordRepository.findByRecordDate(date).forEach(e -> activeUserIds.add(e.getUserId()));
        waterRecordRepository.findByRecordDate(date).forEach(w -> activeUserIds.add(w.getUserId()));
        teamCheckinRepository.findByCheckinDate(date).forEach(tc -> activeUserIds.add(tc.getUserId()));
        return activeUserIds;
    }

    @Override
    public void updateUserStatus(Long userId, String status, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        user.setStatus(status);
        userRepository.save(user);
        logAudit(adminUsername, "BAN_UNBAN_USER", String.valueOf(userId), "修改用户 [" + (user.getNickname() != null ? user.getNickname() : "微信用户") + "] 账号状态为: " + status);
    }

    @Override
    public void updateUserPoints(Long userId, Integer pointsDelta, String remark, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        int oldPoints = user.getPoints() != null ? user.getPoints() : 0;
        int newPoints = Math.max(0, oldPoints + pointsDelta);
        user.setPoints(newPoints);
        userRepository.save(user);

        pointLogRepository.save(com.shike.model.entity.PointLog.builder()
                .userId(userId)
                .amount(pointsDelta)
                .type("ADMIN_ADJUST")
                .remark(remark != null && !remark.isBlank() ? remark : "管理员手动调整积分")
                .build());

        logAudit(adminUsername, "ADJUST_POINTS", String.valueOf(userId), "调整用户 [" + (user.getNickname() != null ? user.getNickname() : "微信用户") + "] 积分: " + (pointsDelta >= 0 ? "+" : "") + pointsDelta + "，备注: " + remark);
    }

    @Override
    public void updateGlobalAiLimit(Integer limit, String adminUsername) {
        if (limit == null || limit < 1) limit = 10;
        stringRedisTemplate.opsForValue().set("shike:sys:config:ai_daily_limit", String.valueOf(limit));
        logAudit(adminUsername, "UPDATE_AI_LIMIT", "GLOBAL", "修改全局每日 AI 调用上限为: " + limit + " 次");
    }

    @Override
    public List<com.shike.model.entity.PointLog> getUserPointLogs(Long userId) {
        return pointLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<com.shike.model.entity.AdminAuditLog> getAuditLogs() {
        return adminAuditLogRepository.findAllByOrderByCreatedAtDesc();
    }

    private void logAudit(String adminUsername, String action, String target, String details) {
        try {
            adminAuditLogRepository.save(com.shike.model.entity.AdminAuditLog.builder()
                    .adminUsername(adminUsername != null ? adminUsername : "admin")
                    .action(action)
                    .target(target)
                    .details(details)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.getMessage());
        }
    }
}
