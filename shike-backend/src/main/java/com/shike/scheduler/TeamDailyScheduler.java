package com.shike.scheduler;

import com.shike.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class TeamDailyScheduler {

    private final TeamService teamService;

    /**
     * 每日午夜 00:00:05 执行昨日小队契约结算
     */
    @Scheduled(cron = "5 0 0 * * ?")
    public void scheduleDailyTeamSettlement() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("[SCHEDULE-TASK] Triggering midnight daily team settlement for date: {}", yesterday);
        try {
            teamService.settleDailyTeamChallenges(yesterday);
            log.info("[SCHEDULE-TASK] Midnight daily team settlement completed successfully for: {}", yesterday);
        } catch (Exception e) {
            log.error("[SCHEDULE-TASK] Midnight daily team settlement failed for: {}", yesterday, e);
        }
    }
}
