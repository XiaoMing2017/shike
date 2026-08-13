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
public class AdminFeedbackPageDTO {
    private List<FeedbackDTO> items;
    private long total;
    private long pendingCount;
    private long bugCount;
    private long suggestionCount;
    private long resolvedCount;
}
