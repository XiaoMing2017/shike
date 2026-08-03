package com.shike.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTeamDTO {
    private Long id;
    private String teamName;
    private Long creatorId;
    private String creatorName;
    private String creatorAvatar;
    private String inviteCode;
    private Integer targetDays;
    private Integer currentDay;
    private Integer depositPoints;
    private String status; // ACTIVE, SUCCESS, FAILED
    private LocalDateTime createdAt;
    private Integer memberCount;
    private List<MemberItem> members;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberItem {
        private Long userId;
        private String nickname;
        private String avatarUrl;
        private Boolean isCreator;
        private Integer points;
        private Boolean todayChecked;
        private Integer successCount;
        private LocalDateTime joinedAt;
    }
}
