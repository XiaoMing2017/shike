package com.shike.scheduler;

import com.shike.model.entity.DietRecord;
import com.shike.model.entity.Team;
import com.shike.model.entity.TeamMember;
import com.shike.model.entity.User;
import com.shike.repository.DietRecordRepository;
import com.shike.repository.TeamMemberRepository;
import com.shike.repository.TeamRepository;
import com.shike.repository.UserRepository;
import com.shike.service.WxSubscribeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DietReminderScheduler {

    private final DietRecordRepository dietRecordRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final WxSubscribeService wxSubscribeService;
    private final StringRedisTemplate stringRedisTemplate;

    public enum MealSlot {
        BREAKFAST("BREAKFAST", "今日早餐未打卡提醒", "吃完早餐记得拍照记餐，开启健康控卡一天！"),
        LUNCH("LUNCH", "今日午餐未打卡提醒", "午餐时间到啦，随手拍照记餐防热量超标！"),
        DINNER("DINNER", "今日晚餐未打卡提醒", "今日打卡即将截止，随手记录晚餐达标控卡！");

        public final String mealType;
        public final String title;
        public final String content;

        MealSlot(String mealType, String title, String content) {
            this.mealType = mealType;
            this.title = title;
            this.content = content;
        }
    }

    /**
     * 1. 早上 08:00 早餐未打卡提醒
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void scheduleBreakfastReminder() {
        log.info("[DIET-REMINDER] Executing Breakfast reminder check at 08:00");
        checkAndSendMealReminder(MealSlot.BREAKFAST);
    }

    /**
     * 2. 中午 12:45 午餐未打卡提醒
     */
    @Scheduled(cron = "0 45 12 * * ?")
    public void scheduleLunchReminder() {
        log.info("[DIET-REMINDER] Executing Lunch reminder check at 12:45");
        checkAndSendMealReminder(MealSlot.LUNCH);
    }

    /**
     * 3. 晚上 20:30 晚餐/未完成打卡提醒
     */
    @Scheduled(cron = "0 30 20 * * ?")
    public void scheduleDinnerReminder() {
        log.info("[DIET-REMINDER] Executing Dinner reminder check at 20:30");
        checkAndSendMealReminder(MealSlot.DINNER);
    }

    /**
     * 核心逻辑：扫描未打卡用户并推送对应餐次微信服务通知
     */
    public int checkAndSendMealReminder(MealSlot slot) {
        LocalDate today = LocalDate.now();
        log.info("[DIET-REMINDER] Checking meal slot {} for date: {}", slot.name(), today);

        Set<Long> candidateUserIds = findCandidateUserIds();
        log.info("[DIET-REMINDER] Total candidate users to inspect: {}", candidateUserIds.size());

        int remindedCount = 0;
        for (Long userId : candidateUserIds) {
            try {
                // 防重检查：避免同一餐次重复推送
                String sentKey = "shike:diet:reminder:sent:" + userId + ":" + today + ":" + slot.name();
                if (stringRedisTemplate != null && Boolean.TRUE.equals(stringRedisTemplate.hasKey(sentKey))) {
                    continue;
                }

                // 检查用户今天是否已经记录了该餐次
                List<DietRecord> records = dietRecordRepository.findByUserIdAndRecordDate(userId, today);
                boolean hasCheckedMeal = records.stream()
                        .anyMatch(r -> slot.mealType.equalsIgnoreCase(r.getMealType()));

                // 如果该餐次已经打过卡，则不打扰用户
                if (hasCheckedMeal) {
                    continue;
                }

                User user = userRepository.findById(userId).orElse(null);
                if (user == null || user.getOpenid() == null || user.getOpenid().trim().isEmpty()) {
                    continue;
                }

                // 下发微信官方服务通知
                boolean sent = wxSubscribeService.sendUserNotice(
                        userId,
                        "DIET_REMINDER",
                        slot.title,
                        slot.content,
                        "pages/index/index"
                );

                if (sent) {
                    remindedCount++;
                    if (stringRedisTemplate != null) {
                        stringRedisTemplate.opsForValue().set(sentKey, "1", Duration.ofHours(12));
                    }
                    log.info("[DIET-REMINDER] Successfully pushed {} reminder to user {}", slot.name(), userId);
                }
            } catch (Exception e) {
                log.warn("[DIET-REMINDER] Error processing {} reminder for user {}: {}", slot.name(), userId, e.getMessage());
            }
        }

        log.info("[DIET-REMINDER] Completed {} reminder job. Sent notices to {} users", slot.name(), remindedCount);
        return remindedCount;
    }

    /**
     * 获取所有需要接收提醒的用户集合（活跃小队成员 + 订阅了提醒的用户）
     */
    private Set<Long> findCandidateUserIds() {
        Set<Long> candidateUserIds = new HashSet<>();

        // 1. 扫描所有进行中的对赌小队成员
        try {
            List<Team> activeTeams = teamRepository.findAll().stream()
                    .filter(t -> "ACTIVE".equalsIgnoreCase(t.getStatus()))
                    .toList();
            for (Team team : activeTeams) {
                List<TeamMember> members = teamMemberRepository.findByTeamId(team.getId());
                for (TeamMember m : members) {
                    if (m.getUserId() != null) {
                        candidateUserIds.add(m.getUserId());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DIET-REMINDER] Error fetching active team members: {}", e.getMessage());
        }

        // 2. 扫描已订阅 DIET_REMINDER 微信服务通知的用户
        try {
            if (stringRedisTemplate != null) {
                Set<String> subKeys = stringRedisTemplate.keys("shike:wx:subscribe:*:DIET_REMINDER");
                if (subKeys != null) {
                    for (String key : subKeys) {
                        String[] parts = key.split(":");
                        if (parts.length >= 4) {
                            candidateUserIds.add(Long.parseLong(parts[3]));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DIET-REMINDER] Error fetching DIET_REMINDER subscribe keys: {}", e.getMessage());
        }

        return candidateUserIds;
    }
}
