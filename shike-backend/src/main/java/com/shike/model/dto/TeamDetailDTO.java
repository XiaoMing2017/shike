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
public class TeamDetailDTO {

    private Long teamId;
    private String teamName;
    private String inviteCode;
    private Integer targetDays;
    private Integer points; // 小队总押金奖池
    private Integer userPoints; // 当前查询用户的个人可用积分余额
    private Integer dailyPot; // 今日待瓜分流动池
    private Integer finalPot; // 终极通关大奖池
    private Integer currentDay;
    private String status;
    private Object pendingLoot; // 未领取的盲盒详情
    private List<MemberDetail> members;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberDetail {
        private Long id;
        private String name;
        private String avatar;
        private Boolean todayChecked;
        private Integer successCount;
        private List<TickDetail> ticks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TickDetail {
        private Integer day;
        private Boolean checked;
    }
}
