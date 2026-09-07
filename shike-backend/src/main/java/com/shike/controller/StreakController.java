package com.shike.controller;

import com.shike.common.ResultDTO;
import com.shike.model.dto.StreakCheckinResultDTO;
import com.shike.model.dto.StreakStatusDTO;
import com.shike.service.StreakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/streak")
@RequiredArgsConstructor
@Slf4j
public class StreakController {

    private final StreakService streakService;

    /**
     * 获取首页连续自律连击状态与7天轨道
     */
    @GetMapping("/status")
    public ResultDTO<StreakStatusDTO> getStreakStatus(@RequestParam Long userId) {
        StreakStatusDTO status = streakService.getStreakStatus(userId);
        return ResultDTO.success(status);
    }

    /**
     * 用户点击打卡 / 完成今日自律打卡
     */
    @PostMapping("/checkin")
    public ResultDTO<StreakCheckinResultDTO> performCheckin(@RequestParam Long userId) {
        StreakCheckinResultDTO result = streakService.performCheckin(userId);
        return ResultDTO.success(result);
    }

    /**
     * 断签拯救 / 单人主动补签（使用血清补签卡、消耗50积分、或好友分享）
     */
    @PostMapping("/recover")
    public ResultDTO<Map<String, Object>> recoverStreak(@RequestParam Long userId,
                                                        @RequestParam(defaultValue = "SERUM_CARD") String method,
                                                        @RequestParam(required = false) String date) {
        Map<String, Object> result = streakService.recoverStreak(userId, method, date);
        return ResultDTO.success(result);
    }
}
