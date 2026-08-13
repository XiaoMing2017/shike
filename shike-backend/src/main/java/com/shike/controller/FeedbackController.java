package com.shike.controller;

import com.shike.common.ResultDTO;
import com.shike.model.dto.FeedbackDTO;
import com.shike.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResultDTO<FeedbackDTO> submitFeedback(@RequestHeader(value = "X-User-Id", defaultValue = "2") Long userId,
                                                @RequestBody FeedbackDTO dto) {
        FeedbackDTO result = feedbackService.submitFeedback(userId, dto);
        return ResultDTO.success(result);
    }

    @GetMapping("/my")
    public ResultDTO<List<FeedbackDTO>> getMyFeedbacks(@RequestHeader(value = "X-User-Id", defaultValue = "2") Long userId) {
        List<FeedbackDTO> list = feedbackService.getUserFeedbacks(userId);
        return ResultDTO.success(list);
    }
}
