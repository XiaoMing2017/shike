package com.shike.service;

import com.shike.model.dto.AdminFeedbackPageDTO;
import com.shike.model.dto.FeedbackDTO;

import java.util.List;
import java.util.Map;

public interface FeedbackService {

    FeedbackDTO submitFeedback(Long userId, FeedbackDTO dto);

    List<FeedbackDTO> getUserFeedbacks(Long userId);

    AdminFeedbackPageDTO getAdminFeedbacks(String status, String type, int page, int size);

    FeedbackDTO processFeedback(Long feedbackId, String status, String adminReply, Integer rewardPoints);

    Map<String, Object> getFeedbackStatsWidget();
}
