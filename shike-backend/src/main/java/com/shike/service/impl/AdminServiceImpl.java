package com.shike.service.impl;

import com.shike.common.BizException;
import com.shike.model.dto.AdminLoginDTO;
import com.shike.model.dto.AdminStatsDTO;
import com.shike.model.dto.AdminTeamDTO;
import com.shike.model.dto.AdminUserDTO;
import com.shike.model.dto.UserDetailRecordsDTO;
import com.shike.model.entity.DietRecord;
import com.shike.model.entity.PointsRecord;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private final com.shike.repository.FeatureToggleRepository featureToggleRepository;
    private final com.shike.repository.AnnouncementConfigRepository announcementConfigRepository;
    private final com.shike.repository.ContactConfigRepository contactConfigRepository;
    private final PointsRecordRepository pointsRecordRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private static final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

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
        return getDashboardStats(7);
    }

    @Override
    public AdminStatsDTO getDashboardStats(Integer days) {
        int rangeDays = (days != null && days > 0) ? Math.min(days, 180) : 7;
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

        // Sum AI recognitions across all users today & compute trend over rangeDays
        long todayMealAiCount = 0;
        long todayPlanAiCount = 0;
        long totalHistoricalMealCount = 0;
        long totalHistoricalPlanCount = 0;
        List<AdminStatsDTO.AiTrendItem> aiTrendList = new ArrayList<>();

        for (int i = rangeDays - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long dayMealCount = 0;
            long dayPlanCount = 0;
            java.time.LocalDateTime startOfDay = date.atStartOfDay();
            java.time.LocalDateTime endOfDay = date.atTime(23, 59, 59);

            // 1. 精准查询 DB 中非测试账号 (userId!=2) 的计划生成数
            dayPlanCount = pointsRecordRepository.countByTypeAndUserIdNotAndCreatedAtBetween("PLAN_GEN", 2L, startOfDay, endOfDay);

            // 2. 精准查询 DB 中非测试账号 (userId!=2) 的膳食识别数 (结合扣积分诊断与餐饮日志)
            long dbDiagnosisCount = pointsRecordRepository.countByTypeAndUserIdNotAndCreatedAtBetween("DIET_DIAGNOSIS", 2L, startOfDay, endOfDay);
            long dbDietRecordCount = dietRecordRepository.countByRecordDateAndUserIdNot(date, 2L);
            dayMealCount = Math.max(dbDiagnosisCount, dbDietRecordCount);

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

        // ========== 动态时间范围注册用户趋势 & DAU趋势 ==========
        List<AdminStatsDTO.AiTrendItem> registrationTrendList = new ArrayList<>();
        List<AdminStatsDTO.AiTrendItem> dauTrendList = new ArrayList<>();

        for (int i = rangeDays - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateLabel = date.format(DateTimeFormatter.ofPattern("MM-dd"));

            // 每日注册用户数
            long dayRegistrations = allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().toLocalDate().equals(date))
                    .count();
            registrationTrendList.add(AdminStatsDTO.AiTrendItem.builder()
                    .date(dateLabel)
                    .count(dayRegistrations)
                    .build());

            // 每日活跃用户数 (DAU)
            Set<Long> dayActiveIds = getActiveUserIdsForDate(date, allUsers);

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
                    Set<Long> activeD1 = getActiveUserIdsForDate(day1Date, allUsers);
                    day1Count = regUserIds.stream().filter(activeD1::contains).count();
                }

                if (!day7Date.isAfter(today)) {
                    Set<Long> activeD7 = getActiveUserIdsForDate(day7Date, allUsers);
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
        Set<Long> todayActiveIds = getActiveUserIdsForDate(today, users);
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

            // 数据库保底计算：如果 Redis 丢失或未计数，回落计算今日用户产生的膳食打卡记录
            Long todayDbDietCount = dietRecordRepository.countByUserIdAndRecordDate(u.getId(), today);
            if (todayDbDietCount != null && todayDbDietCount > aiCount) {
                aiCount = todayDbDietCount.intValue();
            }

            boolean isActiveToday = todayActiveIds.contains(u.getId()) || aiCount > 0;

            result.add(AdminUserDTO.builder()
                    .user(u)
                    .todayAiCount(aiCount)
                    .totalDietCount(dietCount)
                    .totalExerciseCount(exerciseCount)
                    .activeToday(isActiveToday)
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
                    java.math.BigDecimal maxAllowed = budget.multiply(java.math.BigDecimal.valueOf(1.25));
                    todayChecked = totalTodayCalories.compareTo(maxAllowed) <= 0;
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
                    java.math.BigDecimal maxAllowed = budget.multiply(java.math.BigDecimal.valueOf(1.25));
                    todayChecked = totalTodayCalories.compareTo(maxAllowed) <= 0;
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

    private Set<Long> getActiveUserIdsForDate(LocalDate date, List<User> allUsers) {
        Set<Long> activeUserIds = new HashSet<>();
        dietRecordRepository.findByRecordDate(date).forEach(d -> activeUserIds.add(d.getUserId()));
        exerciseRecordRepository.findByRecordDate(date).forEach(e -> activeUserIds.add(e.getUserId()));
        waterRecordRepository.findByRecordDate(date).forEach(w -> activeUserIds.add(w.getUserId()));
        teamCheckinRepository.findByCheckinDate(date).forEach(tc -> activeUserIds.add(tc.getUserId()));

        if (allUsers != null && !allUsers.isEmpty()) {
            allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().toLocalDate().equals(date))
                    .forEach(u -> activeUserIds.add(u.getId()));
            allUsers.stream()
                    .filter(u -> u.getUpdatedAt() != null && u.getUpdatedAt().toLocalDate().equals(date))
                    .forEach(u -> activeUserIds.add(u.getId()));
        }

        try {
            String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Set<String> aiKeys = stringRedisTemplate.keys("shike:ai:limit:*:" + dateStr);
            if (aiKeys != null) {
                for (String key : aiKeys) {
                    String[] parts = key.split(":");
                    if (parts.length >= 4) {
                        try {
                            activeUserIds.add(Long.parseLong(parts[3]));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch Redis AI active keys for date {}: {}", date, e.getMessage());
        }

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

    @jakarta.annotation.PostConstruct
    public void initFeatureToggles() {
        // 🎛️ 初始化全站功能开关 (包含反馈系统)
        try {
            initSingleToggle("user_feedback", "用户意见与BUG反馈", "互动与反馈", "PROD_AND_TEST", true, "小程序端悬浮反馈胶囊按钮与意见/BUG提报入口");
            initSingleToggle("ai_plan", "AI 专属定制计划", "AI大模型功能", "TEST_ONLY", false, "基于档案由 AI 推算周运动与 28 餐食谱");
            initSingleToggle("diet_diagnosis", "AI 膳食深度诊断", "AI大模型功能", "PROD_AND_TEST", true, "Qwen 大模型每日三餐深度复盘点评 (15积分/次)");
            initSingleToggle("photo_recognize", "AI 拍照识图算卡", "AI大模型功能", "PROD_AND_TEST", true, "Qwen-VL 多模态识别食物照片热量、重量与三大营养素");
            initSingleToggle("poster_share", "晒餐/打卡海报生成", "社交与分享", "PROD_AND_TEST", true, "生成拍立得、大餐救急等精美海报与打卡图");
            initSingleToggle("team_challenge", "契约小队对赌打卡", "互动与挑战", "PROD_AND_TEST", true, "组队习惯养成打卡与积分对赌池");
            initSingleToggle("water_log", "饮水追踪与记录", "健康追踪", "PROD_AND_TEST", true, "每日饮水量实时目标进度追踪");
            initSingleToggle("week_dashboard", "周看板视图与对比", "看板与分析", "PROD_AND_TEST", true, "支持切换到近7天热量/三大营养素趋势与周看板");
            initSingleToggle("month_dashboard", "月看板视图与趋势", "看板与分析", "PROD_AND_TEST", true, "支持切换到月度热量赤字、4周趋势对比与月看板");
            
            // 清理 Redis 缓存，防止旧乱码缓存驻留
            if (stringRedisTemplate != null) {
                stringRedisTemplate.delete("shike:sys:feature_toggles");
                stringRedisTemplate.delete("shike:sys:feature_toggles:test");
                stringRedisTemplate.delete("shike:sys:feature_toggles:prod");
                stringRedisTemplate.delete("shike:sys:feature_toggles:release");
            }
        } catch (Exception e) {
            log.warn("Failed to initialize feature toggles: {}", e.getMessage());
        }
    }

    private void initSingleToggle(String key, String name, String category, String defaultEnvMode, boolean defaultEnabled, String desc) {
        com.shike.model.entity.FeatureToggle toggle = featureToggleRepository.findByFeatureKey(key).orElse(null);
        if (toggle == null) {
            featureToggleRepository.save(com.shike.model.entity.FeatureToggle.builder()
                    .featureKey(key)
                    .featureName(name)
                    .category(category)
                    .envMode(defaultEnvMode != null ? defaultEnvMode : "PROD_AND_TEST")
                    .enabled(defaultEnabled)
                    .description(desc)
                    .build());
            log.info("Initialized default feature toggle [{}]: envMode={}, enabled={}", key, defaultEnvMode, defaultEnabled);
        } else {
            toggle.setFeatureName(name);
            toggle.setCategory(category);
            toggle.setDescription(desc);
            featureToggleRepository.save(toggle);
        }
    }

    @Override
    public List<com.shike.model.entity.FeatureToggle> getAllFeatureToggles() {
        initFeatureToggles();
        List<com.shike.model.entity.FeatureToggle> list = featureToggleRepository.findAll();
        for (com.shike.model.entity.FeatureToggle ft : list) {
            if (ft.getEnvMode() == null || ft.getEnvMode().isBlank()) {
                ft.setEnvMode(Boolean.TRUE.equals(ft.getEnabled()) ? "PROD_AND_TEST" : "DISABLED");
            }
            sanitizeFeatureToggleText(ft);
        }
        return list;
    }

    private void sanitizeFeatureToggleText(com.shike.model.entity.FeatureToggle ft) {
        if (ft == null || ft.getFeatureKey() == null) return;
        switch (ft.getFeatureKey()) {
            case "user_feedback":
                ft.setFeatureName("用户意见与BUG反馈");
                ft.setCategory("互动与反馈");
                ft.setDescription("小程序端悬浮反馈胶囊按钮与意见/BUG提报入口");
                break;
            case "ai_plan":
                ft.setFeatureName("AI 专属定制计划");
                ft.setCategory("AI大模型功能");
                ft.setDescription("基于档案由 AI 推算周运动与 28 餐食谱");
                break;
            case "diet_diagnosis":
                ft.setFeatureName("AI 膳食深度诊断");
                ft.setCategory("AI大模型功能");
                ft.setDescription("Qwen 大模型每日三餐深度复盘点评 (15积分/次)");
                break;
            case "photo_recognize":
                ft.setFeatureName("AI 拍照识图算卡");
                ft.setCategory("AI大模型功能");
                ft.setDescription("Qwen-VL 多模态识别食物照片热量、重量与三大营养素");
                break;
            case "poster_share":
                ft.setFeatureName("晒餐/打卡海报生成");
                ft.setCategory("社交与分享");
                ft.setDescription("生成拍立得、大餐救急等精美海报与打卡图");
                break;
            case "team_challenge":
                ft.setFeatureName("契约小队对赌打卡");
                ft.setCategory("互动与挑战");
                ft.setDescription("组队习惯养成打卡与积分对赌池");
                break;
            case "water_log":
                ft.setFeatureName("饮水追踪与记录");
                ft.setCategory("健康追踪");
                ft.setDescription("每日饮水量实时目标进度追踪");
                break;
            case "week_dashboard":
                ft.setFeatureName("周看板视图与对比");
                ft.setCategory("看板与分析");
                ft.setDescription("支持切换到近7天热量/三大营养素趋势与周看板");
                break;
            case "month_dashboard":
                ft.setFeatureName("月看板视图与趋势");
                ft.setCategory("看板与分析");
                ft.setDescription("支持切换到月度热量赤字、4周趋势对比与月看板");
                break;
        }
    }

    @Override
    public void updateFeatureToggle(String featureKey, String envMode, Boolean enabled, String adminUsername) {
        com.shike.model.entity.FeatureToggle toggle = featureToggleRepository.findByFeatureKey(featureKey)
                .orElseThrow(() -> new com.shike.common.BizException(404, "找不到功能开关: " + featureKey));

        if (envMode != null && !envMode.isBlank()) {
            toggle.setEnvMode(envMode);
            if ("DISABLED".equals(envMode)) {
                toggle.setEnabled(false);
            } else {
                toggle.setEnabled(true);
            }
        } else if (enabled != null) {
            toggle.setEnabled(enabled);
            toggle.setEnvMode(enabled ? "PROD_AND_TEST" : "DISABLED");
        }

        featureToggleRepository.save(toggle);

        // 清除 Redis 缓存（全量与分环境缓存）
        try {
            stringRedisTemplate.delete("shike:sys:feature_toggles");
            stringRedisTemplate.delete("shike:sys:feature_toggles:test");
            stringRedisTemplate.delete("shike:sys:feature_toggles:prod");
        } catch (Exception e) {
            log.warn("Failed to clear feature toggles redis cache: {}", e.getMessage());
        }

        String modeDesc = "PROD_AND_TEST".equals(toggle.getEnvMode()) ? "全量线上(正式+测试)" : ("TEST_ONLY".equals(toggle.getEnvMode()) ? "仅测试环境(开发者工具)" : "全网下架隐藏");
        logAudit(adminUsername, "UPDATE_FEATURE_TOGGLE", featureKey, "设置功能 [" + toggle.getFeatureName() + "] 部署环境为: " + modeDesc);
    }

    @Override
    public java.util.Map<String, Boolean> getPublicFeatureToggles(String env) {
        String isTestEnv = ("develop".equalsIgnoreCase(env) || "trial".equalsIgnoreCase(env)) ? "test" : "prod";
        String cacheKey = "shike:sys:feature_toggles:" + isTestEnv;

        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isBlank()) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(cached, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Boolean>>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to read feature toggles cache from redis", e);
        }

        List<com.shike.model.entity.FeatureToggle> list = featureToggleRepository.findAll();
        if (list.isEmpty()) {
            initFeatureToggles();
            list = featureToggleRepository.findAll();
        }

        boolean isTest = "test".equals(isTestEnv);
        java.util.Map<String, Boolean> res = new java.util.HashMap<>();

        for (com.shike.model.entity.FeatureToggle ft : list) {
            String mode = ft.getEnvMode();
            if (mode == null || mode.isBlank()) {
                mode = Boolean.TRUE.equals(ft.getEnabled()) ? "PROD_AND_TEST" : "DISABLED";
            }

            boolean active = false;
            if ("PROD_AND_TEST".equals(mode)) {
                active = true;
            } else if ("TEST_ONLY".equals(mode)) {
                active = isTest; // 仅测试/开发环境开启！正式版隐藏！
            } else {
                active = false; // DISABLED
            }

            res.put(ft.getFeatureKey(), active);
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            stringRedisTemplate.opsForValue().set(cacheKey, mapper.writeValueAsString(res), 1, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to write feature toggles to redis cache", e);
        }

        return res;
    }

    @Override
    public com.shike.model.dto.AnnouncementConfigDTO getAnnouncementConfig() {
        com.shike.model.entity.AnnouncementConfig config = announcementConfigRepository.findFirstByOrderByIdAsc().orElse(null);
        if (config == null) {
            // Default initial config matching the current onboarding modal
            List<com.shike.model.dto.AnnouncementConfigDTO.Item> defaultItems = List.of(
                    com.shike.model.dto.AnnouncementConfigDTO.Item.builder()
                            .icon("🤖")
                            .title("AI 专属运动与食谱")
                            .desc("精准推荐每日3~4个动作与接地气食谱")
                            .build(),
                    com.shike.model.dto.AnnouncementConfigDTO.Item.builder()
                            .icon("💡")
                            .title("AI 专家级营养健康诊断")
                            .desc("评估三大营养占比，针对性给出指导")
                            .build(),
                    com.shike.model.dto.AnnouncementConfigDTO.Item.builder()
                            .icon("📸")
                            .title("高颜值晒餐海报")
                            .desc("一键生成精美饮食打卡图，分享朋友圈")
                            .build()
            );

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = "[]";
            try {
                json = mapper.writeValueAsString(defaultItems);
            } catch (Exception ignored) {}

            config = com.shike.model.entity.AnnouncementConfig.builder()
                    .enabled(true)
                    .badgeText("v2.0 重磅升级")
                    .title("🎉 专属 AI 助手全新上线")
                    .subtitle("精细化膳食、运动打卡与诊断全面开启")
                    .itemsJson(json)
                    .buttonText("✨ 立即体验 AI 计划")
                    .buttonAction("AI_PLAN")
                    .build();
            config = announcementConfigRepository.save(config);
        }

        List<com.shike.model.dto.AnnouncementConfigDTO.Item> items = new java.util.ArrayList<>();
        if (config.getItemsJson() != null && !config.getItemsJson().isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                items = mapper.readValue(config.getItemsJson(), new com.fasterxml.jackson.core.type.TypeReference<List<com.shike.model.dto.AnnouncementConfigDTO.Item>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse itemsJson from AnnouncementConfig", e);
            }
        }

        return com.shike.model.dto.AnnouncementConfigDTO.builder()
                .enabled(config.getEnabled())
                .badgeText(config.getBadgeText())
                .title(config.getTitle())
                .subtitle(config.getSubtitle())
                .items(items)
                .buttonText(config.getButtonText())
                .buttonAction(config.getButtonAction())
                .build();
    }

    @Override
    public void updateAnnouncementConfig(com.shike.model.dto.AnnouncementConfigDTO dto, String adminUsername) {
        if (dto == null) throw new BizException(400, "配置数据不能为空");

        com.shike.model.entity.AnnouncementConfig config = announcementConfigRepository.findFirstByOrderByIdAsc()
                .orElse(new com.shike.model.entity.AnnouncementConfig());

        config.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        config.setBadgeText(dto.getBadgeText());
        config.setTitle(dto.getTitle());
        config.setSubtitle(dto.getSubtitle());
        config.setButtonText(dto.getButtonText());
        config.setButtonAction(dto.getButtonAction() != null ? dto.getButtonAction() : "AI_PLAN");

        if (dto.getItems() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                config.setItemsJson(mapper.writeValueAsString(dto.getItems()));
            } catch (Exception e) {
                throw new BizException(500, "序列化功能亮点列表失败: " + e.getMessage());
            }
        }

        announcementConfigRepository.save(config);
        logAudit(adminUsername, "UPDATE_ANNOUNCEMENT_CONFIG", "ANNOUNCEMENT", "更新了客户端版本公告与引导弹窗配置: " + config.getTitle());
    }

    @Override
    public com.shike.model.dto.ServerStatusDTO getServerStatus() {
        java.lang.management.OperatingSystemMXBean osBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        java.lang.management.RuntimeMXBean runtimeBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
        java.lang.management.ThreadMXBean threadBean = java.lang.management.ManagementFactory.getThreadMXBean();

        String osName = osBean.getName() + " " + osBean.getVersion();
        String osArch = osBean.getArch();
        int cpuCores = osBean.getAvailableProcessors();
        String jvmVersion = System.getProperty("java.version");
        String jvmVendor = System.getProperty("java.vendor");

        long startMs = runtimeBean.getStartTime();
        String startTime = java.time.Instant.ofEpochMilli(startMs)
                .atZone(java.time.ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        long uptimeMs = runtimeBean.getUptime();
        long hours = TimeUnit.MILLISECONDS.toHours(uptimeMs);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMs) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(uptimeMs) % 60;
        String uptimeStr = String.format("%d小时 %d分 %d秒", hours, minutes, seconds);

        double cpuUsage = 0.0;
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            double load = sunOsBean.getCpuLoad();
            if (load >= 0) {
                cpuUsage = Math.round(load * 1000.0) / 10.0;
            }
        }

        Runtime runtime = Runtime.getRuntime();
        long maxMemMb = runtime.maxMemory() / (1024 * 1024);
        long totalMemMb = runtime.totalMemory() / (1024 * 1024);
        long freeMemMb = runtime.freeMemory() / (1024 * 1024);
        long usedMemMb = totalMemMb - freeMemMb;
        double memUsagePct = maxMemMb > 0 ? Math.round((usedMemMb * 100.0 / maxMemMb) * 10.0) / 10.0 : 0.0;

        java.io.File rootFile = new java.io.File("/");
        long totalDiskBytes = rootFile.getTotalSpace();
        long freeDiskBytes = rootFile.getFreeSpace();
        long usedDiskBytes = totalDiskBytes - freeDiskBytes;

        double totalDiskGb = Math.round((totalDiskBytes / (1024.0 * 1024 * 1024)) * 10.0) / 10.0;
        double usedDiskGb = Math.round((usedDiskBytes / (1024.0 * 1024 * 1024)) * 10.0) / 10.0;
        double freeDiskGb = Math.round((freeDiskBytes / (1024.0 * 1024 * 1024)) * 10.0) / 10.0;
        double diskUsagePct = totalDiskBytes > 0 ? Math.round((usedDiskBytes * 100.0 / totalDiskBytes) * 10.0) / 10.0 : 0.0;

        com.shike.model.dto.ServerStatusDTO.ComponentStatus mysqlStatus;
        long startMysql = System.currentTimeMillis();
        try {
            userRepository.count();
            long costMysql = System.currentTimeMillis() - startMysql;
            mysqlStatus = com.shike.model.dto.ServerStatusDTO.ComponentStatus.builder()
                    .name("MySQL 8.0 数据库服务")
                    .status("HEALTHY")
                    .latencyMs(costMysql)
                    .details("数据库通信正常 (SELECT 探针响应: " + costMysql + "ms)")
                    .build();
        } catch (Exception e) {
            mysqlStatus = com.shike.model.dto.ServerStatusDTO.ComponentStatus.builder()
                    .name("MySQL 8.0 数据库服务")
                    .status("DOWN")
                    .latencyMs(-1L)
                    .details("通信异常: " + e.getMessage())
                    .build();
        }

        com.shike.model.dto.ServerStatusDTO.ComponentStatus redisStatus;
        long startRedis = System.currentTimeMillis();
        try {
            String pingResult = stringRedisTemplate.execute((org.springframework.data.redis.core.RedisCallback<String>) connection -> connection.ping());
            long costRedis = System.currentTimeMillis() - startRedis;
            redisStatus = com.shike.model.dto.ServerStatusDTO.ComponentStatus.builder()
                    .name("Redis 7.0 内存数据库")
                    .status("HEALTHY")
                    .latencyMs(costRedis)
                    .details("PING 响应正常: " + pingResult + " (" + costRedis + "ms)")
                    .build();
        } catch (Exception e) {
            redisStatus = com.shike.model.dto.ServerStatusDTO.ComponentStatus.builder()
                    .name("Redis 7.0 内存数据库")
                    .status("DOWN")
                    .latencyMs(-1L)
                    .details("连接失败: " + e.getMessage())
                    .build();
        }

        return com.shike.model.dto.ServerStatusDTO.builder()
                .osName(osName)
                .osArch(osArch)
                .cpuCores(cpuCores)
                .jvmVersion(jvmVersion)
                .jvmVendor(jvmVendor)
                .startTime(startTime)
                .uptime(uptimeStr)
                .cpuUsage(cpuUsage)
                .jvmTotalMemoryMb(totalMemMb)
                .jvmUsedMemoryMb(usedMemMb)
                .jvmFreeMemoryMb(freeMemMb)
                .jvmMaxMemoryMb(maxMemMb)
                .jvmMemoryUsagePercent(memUsagePct)
                .diskTotalGb(totalDiskGb)
                .diskUsedGb(usedDiskGb)
                .diskFreeGb(freeDiskGb)
                .diskUsagePercent(diskUsagePct)
                .threadCount(threadBean.getThreadCount())
                .daemonThreadCount(threadBean.getDaemonThreadCount())
                .peakThreadCount(threadBean.getPeakThreadCount())
                .mysqlStatus(mysqlStatus)
                .redisStatus(redisStatus)
                .build();
    }

    @Override
    public java.util.Map<String, String> getAiModelConfig() {
        String planModel = stringRedisTemplate.opsForValue().get("shike:sys:config:ai_model_plan");
        String dietModel = stringRedisTemplate.opsForValue().get("shike:sys:config:ai_model_diet");
        
        if (planModel == null || planModel.isBlank()) {
            planModel = "qwen3.8-max";
        }
        if (dietModel == null || dietModel.isBlank()) {
            dietModel = "qwen3.6-plus";
        }
        
        java.util.Map<String, String> map = new java.util.HashMap<>();
        map.put("planModel", planModel);
        map.put("dietModel", dietModel);
        return map;
    }

    @Override
    public void updateAiModelConfig(String planModel, String dietModel, String adminUsername) {
        if (planModel != null && !planModel.isBlank()) {
            stringRedisTemplate.opsForValue().set("shike:sys:config:ai_model_plan", planModel.trim());
        }
        if (dietModel != null && !dietModel.isBlank()) {
            stringRedisTemplate.opsForValue().set("shike:sys:config:ai_model_diet", dietModel.trim());
        }
        logAudit(adminUsername, "UPDATE_AI_MODELS", "GLOBAL", "独立修改 AI 调度模型 -> 7天计划: " + planModel + ", 识图诊断: " + dietModel);
    }

    @Override
    public java.util.Map<String, Object> getAiModelConfigDetails() {
        java.util.Map<String, String> current = getAiModelConfig();
        
        java.util.Set<String> planOptions = stringRedisTemplate.opsForSet().members("shike:sys:config:ai_model_plan_options");
        if (planOptions == null || planOptions.isEmpty()) {
            planOptions = new java.util.LinkedHashSet<>(java.util.List.of("qwen3.8-max", "qwen3.6-plus", "qwen3.6-flash", "qwen-max", "gpt-4o", "deepseek-chat"));
            for (String opt : planOptions) {
                stringRedisTemplate.opsForSet().add("shike:sys:config:ai_model_plan_options", opt);
            }
        }
        
        java.util.Set<String> dietOptions = stringRedisTemplate.opsForSet().members("shike:sys:config:ai_model_diet_options");
        if (dietOptions == null || dietOptions.isEmpty()) {
            dietOptions = new java.util.LinkedHashSet<>(java.util.List.of("qwen3.6-plus", "qwen3.6-flash", "qwen-vl-max", "qwen3.8-max", "glm-4v-flash"));
            for (String opt : dietOptions) {
                stringRedisTemplate.opsForSet().add("shike:sys:config:ai_model_diet_options", opt);
            }
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("planModel", current.get("planModel"));
        result.put("dietModel", current.get("dietModel"));
        result.put("planOptions", planOptions);
        result.put("dietOptions", dietOptions);
        return result;
    }

    @Override
    public void addCustomAiModelOption(String moduleKey, String modelName, String adminUsername) {
        if (modelName == null || modelName.isBlank()) {
            throw new BizException(400, "模型名称不能为空");
        }
        String cleanName = modelName.trim();
        if ("plan".equalsIgnoreCase(moduleKey)) {
            stringRedisTemplate.opsForSet().add("shike:sys:config:ai_model_plan_options", cleanName);
            stringRedisTemplate.opsForValue().set("shike:sys:config:ai_model_plan", cleanName);
            logAudit(adminUsername, "ADD_AI_MODEL_OPTION", "PLAN", "新增并切至自定义计划生成模型: " + cleanName);
        } else if ("diet".equalsIgnoreCase(moduleKey)) {
            stringRedisTemplate.opsForSet().add("shike:sys:config:ai_model_diet_options", cleanName);
            stringRedisTemplate.opsForValue().set("shike:sys:config:ai_model_diet", cleanName);
            logAudit(adminUsername, "ADD_AI_MODEL_OPTION", "DIET", "新增并切至自定义识图诊断模型: " + cleanName);
        } else {
            throw new BizException(400, "模块参数类型不合法");
        }
    }

    @Override
    public com.shike.model.dto.AdminFeatureUsageDTO getFeatureUsageOverview() {
        LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        Long testUserId = 2L;

        // 1. AI 诊断统计 (排除测试账号 ID 2)
        long totalDiagnosisCount = pointsRecordRepository.countByTypeAndUserIdNot("DIET_DIAGNOSIS", testUserId);
        long todayDiagnosisCount = pointsRecordRepository.countByTypeAndUserIdNotAndCreatedAtAfter("DIET_DIAGNOSIS", testUserId, startOfToday);

        // 2. 晒惨海报/分享统计 (排除测试账号 ID 2)
        List<String> posterTypes = List.of("SHARE_POSTER", "NUDGE_POSTER", "SHARE_FRIEND", "SHARE_TIMELINE");
        long totalPosterCount = pointsRecordRepository.countByTypeInAndUserIdNot(posterTypes, testUserId);
        long todayPosterCount = pointsRecordRepository.countByTypeInAndUserIdNotAndCreatedAtAfter(posterTypes, testUserId, startOfToday);

        // 3. AI 运动与饮食计划生成统计 (排除测试账号 ID 2)
        long totalPlanCount = pointsRecordRepository.countByTypeAndUserIdNot("PLAN_GEN", testUserId);
        long todayPlanCount = pointsRecordRepository.countByTypeAndUserIdNotAndCreatedAtAfter("PLAN_GEN", testUserId, startOfToday);

        // 4. 明细流水 (排除测试账号 ID 2)
        List<PointsRecord> rawDiagnosisRecords = pointsRecordRepository.findByTypeAndUserIdNotOrderByCreatedAtDesc("DIET_DIAGNOSIS", testUserId);
        List<com.shike.model.dto.AdminFeatureUsageDTO.UsageRecordItem> diagnosisRecords = mapToUsageRecordItems(rawDiagnosisRecords, "AI 深度营养诊断");

        List<PointsRecord> rawPosterRecords = pointsRecordRepository.findByTypeInAndUserIdNotOrderByCreatedAtDesc(posterTypes, testUserId);
        List<com.shike.model.dto.AdminFeatureUsageDTO.UsageRecordItem> posterRecords = mapToUsageRecordItems(rawPosterRecords, "晒惨/裂变海报");

        List<PointsRecord> rawPlanRecords = pointsRecordRepository.findByTypeAndUserIdNotOrderByCreatedAtDesc("PLAN_GEN", testUserId);
        List<com.shike.model.dto.AdminFeatureUsageDTO.UsageRecordItem> planRecords = mapToUsageRecordItems(rawPlanRecords, "AI 专属运动与膳食计划");

        return com.shike.model.dto.AdminFeatureUsageDTO.builder()
                .totalDiagnosisCount(totalDiagnosisCount)
                .todayDiagnosisCount(todayDiagnosisCount)
                .totalPosterCount(totalPosterCount)
                .todayPosterCount(todayPosterCount)
                .totalPlanCount(totalPlanCount)
                .todayPlanCount(todayPlanCount)
                .diagnosisRecords(diagnosisRecords)
                .posterRecords(posterRecords)
                .planRecords(planRecords)
                .build();
    }

    private List<com.shike.model.dto.AdminFeatureUsageDTO.UsageRecordItem> mapToUsageRecordItems(List<PointsRecord> records, String defaultTypeName) {
        if (records == null || records.isEmpty()) return List.of();

        List<Long> uids = records.stream().map(PointsRecord::getUserId).distinct().toList();
        Map<Long, User> userMap = userRepository.findAllById(uids).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return records.stream().map(r -> {
            User u = userMap.get(r.getUserId());
            String nickname = u != null && u.getNickname() != null ? u.getNickname() : "用户 " + r.getUserId();
            String avatar = u != null ? u.getAvatarUrl() : null;
            String phone = null;

            String typeName = defaultTypeName;
            if ("SHARE_FRIEND".equals(r.getType())) typeName = "分享好友海报";
            else if ("SHARE_TIMELINE".equals(r.getType())) typeName = "朋友圈炫耀海报";
            else if ("NUDGE_POSTER".equals(r.getType())) typeName = "晒惨督促海报";
            else if ("SHARE_POSTER".equals(r.getType())) typeName = "打卡成绩海报";
            else if ("PLAN_GEN".equals(r.getType())) typeName = "AI 专属运动与膳食计划";

            return com.shike.model.dto.AdminFeatureUsageDTO.UsageRecordItem.builder()
                    .id(r.getId())
                    .userId(r.getUserId())
                    .nickname(nickname)
                    .avatar(avatar)
                    .phone(phone)
                    .type(r.getType())
                    .typeName(typeName)
                    .amount(r.getAmount())
                    .remark(r.getRemark())
                    .createdAt(r.getCreatedAt())
                    .build();
        }).toList();
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

    @Override
    public com.shike.model.dto.ContactConfigDTO getContactConfig() {
        com.shike.model.entity.ContactConfig config = contactConfigRepository.findFirstByOrderByIdAsc().orElse(null);
        if (config == null) {
            List<com.shike.model.dto.ContactConfigDTO.ContactItem> defaultItems = List.of(
                    com.shike.model.dto.ContactConfigDTO.ContactItem.builder()
                            .id("item_wx")
                            .label("客服微信号")
                            .value("shike_helper")
                            .actionType("COPY")
                            .btnText("复制微信号")
                            .build(),
                    com.shike.model.dto.ContactConfigDTO.ContactItem.builder()
                            .id("item_phone")
                            .label("客服电话")
                            .value("13800138000")
                            .actionType("CALL")
                            .btnText("拨打电话")
                            .build()
            );

            String defaultJson = "";
            try {
                defaultJson = mapper.writeValueAsString(defaultItems);
            } catch (Exception e) {
                log.warn("Failed to serialize default contact items", e);
            }

            config = com.shike.model.entity.ContactConfig.builder()
                    .enabled(true)
                    .title("联系客服 & 意见反馈")
                    .wxId("shike_helper")
                    .phone("13800138000")
                    .notice("遇到 Bug 或有改进建议？长按复制微信号添加客服，采纳有契约积分奖励哦！")
                    .openType("MODAL")
                    .itemsJson(defaultJson)
                    .build();
            config = contactConfigRepository.save(config);
        }

        String cleanTitle = config.getTitle();
        if (cleanTitle != null) {
            cleanTitle = cleanTitle.replace("💬", "").replace("🎧", "").trim();
        }
        if (cleanTitle == null || cleanTitle.isBlank()) {
            cleanTitle = "联系客服 & 意见反馈";
        }

        List<com.shike.model.dto.ContactConfigDTO.ContactItem> items = new java.util.ArrayList<>();
        if (config.getItemsJson() != null && !config.getItemsJson().isBlank()) {
            try {
                items = mapper.readValue(config.getItemsJson(), new com.fasterxml.jackson.core.type.TypeReference<List<com.shike.model.dto.ContactConfigDTO.ContactItem>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse itemsJson from ContactConfig", e);
            }
        }

        if (items.isEmpty()) {
            if (config.getWxId() != null && !config.getWxId().isBlank()) {
                items.add(com.shike.model.dto.ContactConfigDTO.ContactItem.builder()
                        .id("item_wx")
                        .label("客服微信号")
                        .value(config.getWxId())
                        .actionType("COPY")
                        .btnText("复制微信号")
                        .build());
            }
            if (config.getPhone() != null && !config.getPhone().isBlank()) {
                items.add(com.shike.model.dto.ContactConfigDTO.ContactItem.builder()
                        .id("item_phone")
                        .label("客服电话")
                        .value(config.getPhone())
                        .actionType("CALL")
                        .btnText("拨打电话")
                        .build());
            }
        }

        return com.shike.model.dto.ContactConfigDTO.builder()
                .enabled(config.getEnabled() != null ? config.getEnabled() : true)
                .title(cleanTitle)
                .wxId(config.getWxId())
                .phone(config.getPhone())
                .notice(config.getNotice())
                .openType(config.getOpenType() != null ? config.getOpenType() : "MODAL")
                .items(items)
                .build();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void updateContactConfig(com.shike.model.dto.ContactConfigDTO dto, String adminUsername) {
        if (dto == null) return;
        com.shike.model.entity.ContactConfig config = contactConfigRepository.findFirstByOrderByIdAsc()
                .orElse(new com.shike.model.entity.ContactConfig());

        config.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        if (dto.getTitle() != null) config.setTitle(dto.getTitle().trim());
        if (dto.getWxId() != null) config.setWxId(dto.getWxId().trim());
        if (dto.getPhone() != null) config.setPhone(dto.getPhone().trim());
        if (dto.getNotice() != null) config.setNotice(dto.getNotice().trim());
        if (dto.getOpenType() != null) config.setOpenType(dto.getOpenType().trim());

        if (dto.getItems() != null) {
            try {
                config.setItemsJson(mapper.writeValueAsString(dto.getItems()));
            } catch (Exception e) {
                log.warn("Failed to serialize ContactItem list", e);
            }
        }

        contactConfigRepository.save(config);
        logAudit(adminUsername, "UPDATE_CONTACT_CONFIG", "GLOBAL", "更新动态客服与联系方式配置: item_count=" + (dto.getItems() != null ? dto.getItems().size() : 0));
    }
}
