package com.shike.controller;

import com.shike.common.ResultDTO;
import com.shike.model.entity.Team;
import com.shike.repository.TeamRepository;
import com.shike.scheduler.TeamSettleScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/test/settle")
@RequiredArgsConstructor
@Slf4j
public class TestSettleController {

    private final TeamSettleScheduler teamSettleScheduler;
    private final TeamRepository teamRepository;
    private final com.shike.repository.DietRecordRepository dietRecordRepository;

    /**
     * 手动触发指定小队在指定日期的每日打卡结算。
     */
    @PostMapping("/day")
    public ResultDTO<String> settleDay(
            @RequestParam Long teamId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Manual trigger: Settle team {} daily for date {}", teamId, date);
        teamSettleScheduler.settleTeamDaily(teamId, date);
        return ResultDTO.success("Successfully settled daily checkins for team " + teamId + " on date " + date);
    }

    /**
     * 强制触发整个团队契约周期的终期积分结算与分发。
     * 该接口会无视时间周期天数限制，直接强制进行终期对赌计算和积分分发。
     */
    @PostMapping("/challenge")
    public ResultDTO<String> settleChallenge(@RequestParam Long teamId) {
        log.info("Manual trigger: Force final challenge settlement for team {}", teamId);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new com.shike.common.BizException(404, "Team not found"));
        teamSettleScheduler.performFinalSettlement(team);
        return ResultDTO.success("Successfully executed force final challenge settlement for team " + team.getTeamName());
    }

    /**
     * 为指定用户在指定日期插入一条 Mock 的饮食记录（用于模拟打卡数据）
     */
    @PostMapping("/mock-diet")
    public ResultDTO<String> mockDiet(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam java.math.BigDecimal calories) {
        log.info("Mock diet: insert record for user {} on date {} with {} kcal", userId, date, calories);
        
        com.shike.model.entity.DietRecord record = com.shike.model.entity.DietRecord.builder()
                .userId(userId)
                .recordDate(date)
                .mealType("LUNCH")
                .foodItems("[{\"name\":\"Mock Food\",\"weight\":100.0}]")
                .oilLevel("MODERATE")
                .totalCalories(calories)
                .totalProtein(java.math.BigDecimal.valueOf(10.0))
                .totalFat(java.math.BigDecimal.valueOf(5.0))
                .totalCarbs(java.math.BigDecimal.valueOf(20.0))
                .build();
        
        dietRecordRepository.save(record);
        return ResultDTO.success("Successfully inserted mock diet record");
    }
}
