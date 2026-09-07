package com.shike.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreakStatusDTO {

    private Integer currentStreak;
    private Integer maxStreak;
    private Boolean todayChecked;
    private Integer currentCycleDay; // 1-7
    private Integer todayPoints;

    private Boolean isBroken; // 昨日是否漏签，且符合断签拯救条件
    private Integer brokenStreak; // 中断前的连击天数
    private Integer serumCount; // 背包中血清补签卡数量
    private Integer userPoints; // 当前用户的契约积分余额
    private Boolean isLateWarning; // 是否超过20:00且今日尚未打卡

    private StreakCheckinResultDTO todayCheckinResult; // 今日打卡奖励结算详情（用于前端自动弹窗庆祝）

    private List<DayNodeDTO> days;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayNodeDTO {
        private Integer day; // 1-7
        private Integer points;
        private String itemReward; // CHEAT_SHIELD, SERUM_REVIVAL, GOLDEN_CHEST, null
        private String itemRewardName; // 欺骗餐护盾, 血清补签卡, 金色宝箱
        private String itemRewardIcon; // 🛡️, 💉, 🎁
        private String status; // COMPLETED, TODAY_PENDING, TODAY_DONE, UPCOMING
    }
}
