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
public class FeedbackDTO {
    private Long id;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private String type; // BUG, SUGGESTION, QUESTION, OTHER
    private String content;
    private List<String> images;
    private String contactInfo;
    private String status; // PENDING, PROCESSING, RESOLVED, REJECTED
    private String adminReply;
    private Integer pointsRewarded;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
