package com.shike.controller;

import com.shike.common.Result;
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
    public Result<FeedbackDTO> submitFeedback(@RequestHeader(value = "X-User-Id", defaultValue = "2") Long userId,
                                             @RequestBody FeedbackDTO dto) {
        FeedbackDTO result = feedbackService.submitFeedback(userId, dto);
        return Result.success("提交成功，感谢您的反馈！", result);
    }

    @GetMapping("/my")
    public Result<List<FeedbackDTO>> getMyFeedbacks(@RequestHeader(value = "X-User-Id", defaultValue = "2") Long userId) {
        List<FeedbackDTO> list = feedbackService.getUserFeedbacks(userId);
        return Result.success(list);
    }
}
