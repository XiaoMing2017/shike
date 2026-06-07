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
    private Integer points;
    private Integer currentDay;
    private String status;
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
