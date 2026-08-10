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
public class AdminFeatureUsageDTO {

    private Long totalDiagnosisCount;
    private Long todayDiagnosisCount;
    private Long totalPosterCount;
    private Long todayPosterCount;

    private List<UsageRecordItem> diagnosisRecords;
    private List<UsageRecordItem> posterRecords;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageRecordItem {
        private Long id;
        private Long userId;
        private String nickname;
        private String avatar;
        private String phone;
        private String type;
        private String typeName;
        private Integer amount;
        private String remark;
        private LocalDateTime createdAt;
    }
}
