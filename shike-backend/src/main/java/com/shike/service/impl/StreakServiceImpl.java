package com.shike.service.impl;

import com.shike.common.BizException;
import com.shike.model.dto.StreakCheckinResultDTO;
import com.shike.model.dto.StreakStatusDTO;
import com.shike.model.entity.DietRecord;
import com.shike.model.entity.PointsRecord;
import com.shike.model.entity.StreakRecord;
import com.shike.model.entity.User;
import com.shike.model.entity.UserItem;
import com.shike.repository.DietRecordRepository;
import com.shike.repository.PointsRecordRepository;
import com.shike.repository.StreakRecordRepository;
import com.shike.repository.UserItemRepository;
import com.shike.repository.UserRepository;
import com.shike.service.StreakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shike.model.entity.Team;
import com.shike.model.entity.TeamCheckin;
import com.shike.model.entity.TeamMember;
import com.shike.repository.TeamCheckinRepository;
import com.shike.repository.TeamMemberRepository;
import com.shike.repository.TeamRepository;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreakServiceImpl implements StreakService {

    private final UserRepository userRepository;
    private final StreakRecordRepository streakRecordRepository;
    private final PointsRecordRepository pointsRecordRepository;
    private final UserItemRepository userItemRepository;
    private final DietRecordRepository dietRecordRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final TeamCheckinRepository teamCheckinRepository;
    private final Set<String> abandonedRescueUserIds = java.util.concurrent.ConcurrentHashMap.newKeySet();

    // 7天阶梯奖励配置表（精算平衡模型：7天累计128积分，日常保本，大节点给高光与稀有道具）
    private static final int[] LADDER_POINTS = {5, 8, 15, 10, 25, 15, 50};
    private static final String[] LADDER_ITEMS = {null, null, "CHEAT_SHIELD", null, "SERUM_REVIVAL", null, "GOLDEN_CHEST"};
    private static final String[] LADDER_ITEM_NAMES = {null, null, "欺骗餐护盾", null, "血清补签卡", null, "金色神秘宝箱"};
    private static final String[] LADDER_ITEM_ICONS = {null, null, "🛡️", null, "💉", null, "🎁"};

    @Override
    @Transactional
    public StreakStatusDTO getStreakStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "User not found"));

        // 自动校验并修复连击连续性，杜绝历史数据因补卡未同步而脱节
        recalculateAndSaveUserStreak(user);

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate lastDate = user.getLastCheckinDate();

        int currentStreak = user.getCurrentStreak() != null ? user.getCurrentStreak() : 0;
        int maxStreak = user.getMaxStreak() != null ? user.getMaxStreak() : currentStreak;

        boolean todayChecked = lastDate != null && lastDate.equals(today);
        boolean isBroken = false;
        int brokenStreak = 0;

        List<StreakRecord> records = streakRecordRepository.findByUserIdOrderByCheckinDateDesc(userId);
        Set<LocalDate> checkinDates = (records != null) ? records.stream()
                .map(StreakRecord::getCheckinDate)
                .collect(Collectors.toSet()) : java.util.Collections.emptySet();

        if (!todayChecked) {
            // 如果昨天漏打，但前天有打卡记录，且用户未主动放弃挽救，则判定为处于断签挽救窗口期
            if (!checkinDates.contains(yesterday) && checkinDates.contains(yesterday.minusDays(1))) {
                String abandonKey = userId + ":" + yesterday;
                boolean isAbandoned = abandonedRescueUserIds.contains(abandonKey);
                try {
                    if (stringRedisTemplate != null && Boolean.TRUE.equals(stringRedisTemplate.hasKey("shike:streak:abandon:" + abandonKey))) {
                        isAbandoned = true;
                    }
                } catch (Exception ignored) {}

                if (!isAbandoned) {
                    LocalDate cursor = yesterday.minusDays(1);
                    while (checkinDates.contains(cursor)) {
                        brokenStreak++;
                        cursor = cursor.minusDays(1);
                    }
                    if (brokenStreak > 0) {
                        isBroken = true;
                    }
                }
            }
        }

        // 当前处于7天循环的第几天（1~7）
        int currentCycleDay;
        if (todayChecked) {
            currentCycleDay = ((currentStreak - 1) % 7) + 1;
        } else {
            if (isBroken) {
                currentCycleDay = 1; // 若不挽救，今日将从Day 1开始
            } else if (lastDate != null && lastDate.equals(yesterday)) {
                currentCycleDay = (currentStreak % 7) + 1;
            } else {
                currentCycleDay = 1;
            }
        }

        int todayPoints = LADDER_POINTS[currentCycleDay - 1];

        // 统计背包中血清补签卡数量
        int serumCount = userItemRepository.findByUserIdAndItemType(userId, "SERUM_REVIVAL")
                .map(UserItem::getQuantity)
                .orElse(0);

        // 20:00 之后未打卡提示预警
        boolean isLateWarning = !todayChecked && LocalTime.now().isAfter(LocalTime.of(20, 0));

        // 构造7天节点视图
        List<StreakStatusDTO.DayNodeDTO> days = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            String status;
            if (todayChecked) {
                if (i <= currentCycleDay) {
                    status = "COMPLETED";
                } else {
                    status = "UPCOMING";
                }
            } else {
                if (i < currentCycleDay) {
                    status = "COMPLETED";
                } else if (i == currentCycleDay) {
                    status = "TODAY_PENDING";
                } else {
                    status = "UPCOMING";
                }
            }

            days.add(StreakStatusDTO.DayNodeDTO.builder()
                    .day(i)
                    .points(LADDER_POINTS[i - 1])
                    .itemReward(LADDER_ITEMS[i - 1])
                    .itemRewardName(LADDER_ITEM_NAMES[i - 1])
                    .itemRewardIcon(LADDER_ITEM_ICONS[i - 1])
                    .status(status)
                    .build());
        }

        StreakCheckinResultDTO todayCheckinResult = null;
        if (todayChecked) {
            int rewardPoints = LADDER_POINTS[currentCycleDay - 1];
            String itemReward = LADDER_ITEMS[currentCycleDay - 1];
            String itemRewardName = LADDER_ITEM_NAMES[currentCycleDay - 1];
            String itemRewardIcon = LADDER_ITEM_ICONS[currentCycleDay - 1];
            int nextCycle = (currentCycleDay % 7) + 1;
            int nextDayPoints = LADDER_POINTS[nextCycle - 1];

            todayCheckinResult = StreakCheckinResultDTO.builder()
                    .currentStreak(currentStreak)
                    .cycleDay(currentCycleDay)
                    .rewardPoints(rewardPoints)
                    .itemReward(itemReward)
                    .itemRewardName(itemRewardName)
                    .itemRewardIcon(itemRewardIcon)
                    .totalUserPoints(user.getPoints())
                    .nextDayPoints(nextDayPoints)
                    .message("今日自律已达标！")
                    .build();
        }

        return StreakStatusDTO.builder()
                .currentStreak(currentStreak)
                .maxStreak(maxStreak)
                .todayChecked(todayChecked)
                .currentCycleDay(currentCycleDay)
                .todayPoints(todayPoints)
                .isBroken(isBroken)
                .brokenStreak(brokenStreak)
                .serumCount(serumCount)
                .userPoints(user.getPoints() != null ? user.getPoints() : 0)
                .isLateWarning(isLateWarning)
                .todayCheckinResult(todayCheckinResult)
                .days(days)
                .build();
    }

    @Override
    @Transactional
    public StreakCheckinResultDTO performCheckin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "User not found"));

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate lastDate = user.getLastCheckinDate();

        if (lastDate != null && lastDate.equals(today)) {
            throw new BizException(400, "今日已完成自律打卡，明天再来领取阶梯奖励吧！");
        }

        int currentStreak = user.getCurrentStreak() != null ? user.getCurrentStreak() : 0;
        int nextStreak;

        if (lastDate != null && lastDate.equals(yesterday)) {
            nextStreak = currentStreak + 1;
        } else {
            // 断签或新开启，从1开始
            nextStreak = 1;
        }

        int cycleDay = ((nextStreak - 1) % 7) + 1; // 1 ~ 7
        int rewardPoints = LADDER_POINTS[cycleDay - 1];
        String itemReward = LADDER_ITEMS[cycleDay - 1];
        String itemRewardName = LADDER_ITEM_NAMES[cycleDay - 1];
        String itemRewardIcon = LADDER_ITEM_ICONS[cycleDay - 1];

        // 1. 积分奖励入账
        int userPoints = user.getPoints() != null ? user.getPoints() : 0;
        user.setPoints(userPoints + rewardPoints);

        PointsRecord pRecord = PointsRecord.builder()
                .userId(userId)
                .amount(rewardPoints)
                .type("STREAK_CHECKIN")
                .remark("连续自律打卡 Day " + nextStreak + " 奖励")
                .build();
        pointsRecordRepository.save(pRecord);

        // 2. 特殊道具掉落发放至背包
        if (itemReward != null) {
            grantItemReward(userId, itemReward);
        }

        // 3. 记录打卡流水
        StreakRecord streakRecord = StreakRecord.builder()
                .userId(userId)
                .checkinDate(today)
                .streakDay(cycleDay)
                .rewardPoints(rewardPoints)
                .itemReward(itemReward)
                .isMakeup(false)
                .build();
        streakRecordRepository.save(streakRecord);

        // 4. 更新用户连击状态
        user.setCurrentStreak(nextStreak);
        int maxStreak = user.getMaxStreak() != null ? user.getMaxStreak() : 0;
        if (nextStreak > maxStreak) {
            user.setMaxStreak(nextStreak);
        }
        user.setLastCheckinDate(today);
        userRepository.save(user);

        // 计算明天可获得的积分
        int nextCycleDay = (cycleDay % 7) + 1;
        int nextDayPoints = LADDER_POINTS[nextCycleDay - 1];

        String message = "🎉 连续自律 " + nextStreak + " 天！已获得 +" + rewardPoints + " 积分";
        if (itemRewardName != null) {
            message += "，并掉落【" + itemRewardName + "】×1！";
        } else {
            message += "！明天打卡可领 +" + nextDayPoints + " 分！";
        }

        log.info("[STREAK] User {} completed checkin. Streak: {}, cycleDay: {}, points: +{}, item: {}",
                userId, nextStreak, cycleDay, rewardPoints, itemReward);

        return StreakCheckinResultDTO.builder()
                .currentStreak(nextStreak)
                .cycleDay(cycleDay)
                .rewardPoints(rewardPoints)
                .itemReward(itemReward)
                .itemRewardName(itemRewardName)
                .itemRewardIcon(itemRewardIcon)
                .totalUserPoints(user.getPoints())
                .nextDayPoints(nextDayPoints)
                .message(message)
                .build();
    }

    @Override
    @Transactional
    public Map<String, Object> recoverStreak(Long userId, String method) {
        return recoverStreak(userId, method, null);
    }

    @Override
    @Transactional
    public Map<String, Object> recoverStreak(Long userId, String method, String dateStr) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "User not found"));

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate lastDate = user.getLastCheckinDate();

        LocalDate targetDate;
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            try {
                targetDate = LocalDate.parse(dateStr.trim());
            } catch (Exception e) {
                throw new BizException(400, "日期格式错误，请使用 YYYY-MM-DD");
            }
            if (!targetDate.isBefore(today)) {
                throw new BizException(400, "仅支持补签今天之前的漏打日期");
            }
            if (streakRecordRepository.findByUserIdAndCheckinDate(userId, targetDate).isPresent()) {
                throw new BizException(400, "该日期已完成打卡，无需补签");
            }
        } else {
            targetDate = yesterday;
            // 默认挽救昨日断签，校验是否在断签挽救窗口期（前天打卡，昨天漏打）
            if (lastDate == null || !lastDate.equals(yesterday.minusDays(1))) {
                throw new BizException(400, "当前不处于断签挽救窗口期");
            }
        }

        if ("SERUM_CARD".equalsIgnoreCase(method)) {
            // 消耗 1 张血清补签卡
            UserItem serum = userItemRepository.findByUserIdAndItemType(userId, "SERUM_REVIVAL")
                    .orElseThrow(() -> new BizException(400, "背包中没有【血清补签卡】道具"));
            if (serum.getQuantity() <= 0) {
                throw new BizException(400, "【血清补签卡】数量不足");
            }
            serum.setQuantity(serum.getQuantity() - 1);
            userItemRepository.save(serum);
        } else if ("POINTS".equalsIgnoreCase(method)) {
            // 消耗 50 契约积分直接补签
            int cost = 50;
            int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
            if (currentPoints < cost) {
                throw new BizException(400, "积分不足以补签(需 " + cost + " 积分，当前仅有 " + currentPoints + " 积分)");
            }
            user.setPoints(currentPoints - cost);
            userRepository.save(user);

            PointsRecord pr = PointsRecord.builder()
                    .userId(userId)
                    .amount(-cost)
                    .type("STREAK_RECOVER")
                    .remark("单人补签消耗: " + targetDate)
                    .build();
            pointsRecordRepository.save(pr);
        } else if ("SHARE".equalsIgnoreCase(method)) {
            // 每周限免费分享拯救 1 次
            String shareKey = "shike:streak:recover:share:" + userId;
            if (stringRedisTemplate != null && Boolean.TRUE.equals(stringRedisTemplate.hasKey(shareKey))) {
                throw new BizException(400, "本周已使用过好友分享拯救特权，请使用【血清补签卡】或【50积分】补签");
            }
            if (stringRedisTemplate != null) {
                stringRedisTemplate.opsForValue().set(shareKey, "1", java.time.Duration.ofDays(7));
            }
        } else {
            throw new BizException(400, "未知的断签挽救方式: " + method);
        }

        // 补录打卡流水记录（先占位，随后根据流水连续性精确更新）
        StreakRecord makeupRecord = StreakRecord.builder()
                .userId(userId)
                .checkinDate(targetDate)
                .streakDay(1)
                .rewardPoints(0)
                .itemReward(null)
                .isMakeup(true)
                .build();
        streakRecordRepository.save(makeupRecord);

        // 补录一条默认自律膳食记录，确保日历状态与详情显示为打卡达标 (450 kcal)
        DietRecord makeupDiet = DietRecord.builder()
                .userId(userId)
                .recordDate(targetDate)
                .mealType("LUNCH")
                .oilLevel("LESS_OIL")
                .totalCalories(new java.math.BigDecimal("450.0"))
                .totalCarbs(new java.math.BigDecimal("55.0"))
                .totalProtein(new java.math.BigDecimal("25.0"))
                .totalFat(new java.math.BigDecimal("12.0"))
                .foodItems("[{\"name\":\"自律补签营养餐\",\"calories\":450,\"weight\":200}]")
                .imageUrl("https://images.example.com/meals/lunch.jpg")
                .build();
        dietRecordRepository.save(makeupDiet);

        // 重新精确计算并持久化用户的连击数、最大连击数与最后打卡日期
        recalculateAndSaveUserStreak(user);

        // 更新本次补卡流水记录的循环天数
        int cycleDay = ((user.getCurrentStreak() - 1) % 7) + 1;
        makeupRecord.setStreakDay(cycleDay);
        streakRecordRepository.save(makeupRecord);

        // 同步契约小队当天的打卡达标状态
        syncTeamCheckinOnRecover(userId, targetDate);

        log.info("[STREAK] User {} recovered streak for date {} via {}. Current streak: {}", userId, targetDate, method, user.getCurrentStreak());

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("currentStreak", user.getCurrentStreak());
        resp.put("remainingPoints", user.getPoints());
        resp.put("targetDate", targetDate.toString());
        resp.put("message", "💉 成功补签 " + targetDate + "！连续自律记录已恢复！");
        return resp;
    }

    @Override
    @Transactional
    public StreakCheckinResultDTO autoCheckinOnDietRecord(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;

        LocalDate today = LocalDate.now();
        LocalDate lastDate = user.getLastCheckinDate();

        // 如果今日已经打卡达标并发放过奖励，直接跳过
        if (lastDate != null && lastDate.equals(today)) {
            return null;
        }

        log.info("[STREAK] Auto checkin triggered for user {} upon logging meal", userId);
        return performCheckin(userId);
    }

    private void grantItemReward(Long userId, String itemType) {
        if ("GOLDEN_CHEST".equals(itemType)) {
            // 金色宝箱额外赠送一张护盾和一张补签卡
            addItemQuantity(userId, "CHEAT_SHIELD", 1);
            addItemQuantity(userId, "SERUM_REVIVAL", 1);
        } else {
            addItemQuantity(userId, itemType, 1);
        }
    }

    private void addItemQuantity(Long userId, String itemType, int qty) {
        UserItem item = userItemRepository.findByUserIdAndItemType(userId, itemType)
                .orElse(UserItem.builder()
                        .userId(userId)
                        .itemType(itemType)
                        .quantity(0)
                        .build());
        item.setQuantity(item.getQuantity() + qty);
        userItemRepository.save(item);
    }

    /**
     * 根据打卡流水记录重新精确计算用户的连击数、历史最高连击数与最后打卡日期
     */
    private void recalculateAndSaveUserStreak(User user) {
        List<StreakRecord> records = streakRecordRepository.findByUserIdOrderByCheckinDateDesc(user.getId());
        if (records == null || records.isEmpty()) {
            return;
        }

        Set<LocalDate> checkinDates = records.stream()
                .map(StreakRecord::getCheckinDate)
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        LocalDate startDate = null;
        if (checkinDates.contains(today)) {
            startDate = today;
        } else if (checkinDates.contains(yesterday)) {
            startDate = yesterday;
        }

        int streak = 0;
        if (startDate != null) {
            LocalDate cursor = startDate;
            while (checkinDates.contains(cursor)) {
                streak++;
                cursor = cursor.minusDays(1);
            }
        } else {
            // 今天和昨天都没打卡，说明当前连击中断，当前连击归零
            streak = 0;
        }

        LocalDate latestDate = records.get(0).getCheckinDate();

        user.setCurrentStreak(streak);
        int maxStreak = user.getMaxStreak() != null ? user.getMaxStreak() : 0;
        if (streak > maxStreak) {
            user.setMaxStreak(streak);
        }
        user.setLastCheckinDate(latestDate);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public StreakCheckinResultDTO restartStreak(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "User not found"));

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // 记录已放弃昨日拯救，避免重复触发 isBroken
        String abandonKey = userId + ":" + yesterday;
        abandonedRescueUserIds.add(abandonKey);
        try {
            if (stringRedisTemplate != null) {
                stringRedisTemplate.opsForValue().set("shike:streak:abandon:" + abandonKey, "1", java.time.Duration.ofDays(3));
            }
        } catch (Exception ignored) {}

        // 删除今日可能已存在的打卡流水（若有）
        streakRecordRepository.findByUserIdAndCheckinDate(userId, today).ifPresent(streakRecordRepository::delete);

        // 重置当前连击归零，保持今日待打卡状态（不自动打卡）
        user.setCurrentStreak(0);
        // lastCheckinDate 恢复为除今天之外最近一次实际打卡日期（如前天）
        List<StreakRecord> records = streakRecordRepository.findByUserIdOrderByCheckinDateDesc(userId);
        if (records != null && !records.isEmpty()) {
            user.setLastCheckinDate(records.get(0).getCheckinDate());
        } else {
            user.setLastCheckinDate(null);
        }
        userRepository.save(user);

        log.info("[STREAK] User {} reset streak to 0, pending today's checkin", userId);

        return StreakCheckinResultDTO.builder()
                .currentStreak(0)
                .cycleDay(1)
                .rewardPoints(0)
                .itemReward(null)
                .itemRewardName(null)
                .itemRewardIcon(null)
                .totalUserPoints(user.getPoints())
                .nextDayPoints(LADDER_POINTS[0])
                .message("连击已重置为 0 天，请完成今日自律打卡开启 Day 1！🔥")
                .build();
    }

    /**
     * 补签时，若用户在进行中的契约小队内，自动同步当天的小队打卡状态为达标
     */
    private void syncTeamCheckinOnRecover(Long userId, LocalDate targetDate) {
        if (teamMemberRepository == null || teamRepository == null || teamCheckinRepository == null) {
            return;
        }
        try {
            List<TeamMember> teamMembers = teamMemberRepository.findByUserId(userId);
            for (TeamMember tm : teamMembers) {
                Team t = teamRepository.findById(tm.getTeamId()).orElse(null);
                if (t != null && "ACTIVE".equals(t.getStatus())) {
                    List<TeamCheckin> checkins = teamCheckinRepository.findByTeamIdAndUserId(t.getId(), userId);
                    TeamCheckin checkin = checkins.stream()
                            .filter(c -> c.getCheckinDate().equals(targetDate))
                            .findFirst()
                            .orElse(TeamCheckin.builder()
                                    .teamId(t.getId())
                                    .userId(userId)
                                    .checkinDate(targetDate)
                                    .build());
                    checkin.setIsSuccess(true);
                    teamCheckinRepository.save(checkin);
                    log.info("[STREAK] Synced team checkin success for user {} in team {} on {}", userId, t.getId(), targetDate);
                }
            }
        } catch (Exception e) {
            log.error("[STREAK] Error syncing team checkin on recover: {}", e.getMessage());
        }
    }
}
