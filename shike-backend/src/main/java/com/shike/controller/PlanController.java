package com.shike.controller;

import com.shike.common.ResultDTO;
import com.shike.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/plan", "/api/v1/plan"})
@RequiredArgsConstructor
@Slf4j
public class PlanController {

    private final PlanService planService;

    /**
     * 获取或生成用户专属 AI 运动与饮食计划
     * @param userId 用户ID
     * @param forceRefresh 是否强制重新生成 (默认 false)
     */
    @GetMapping("/generate")
    public ResultDTO<Map<String, Object>> generateOrGetPlan(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "false") Boolean forceRefresh,
            @RequestParam(defaultValue = "true") Boolean createIfAbsent,
            @RequestParam(defaultValue = "GYM") String location) {
        log.info("Request plan for userId: {}, forceRefresh: {}, createIfAbsent: {}, location: {}", userId, forceRefresh, createIfAbsent, location);
        Map<String, Object> plan = planService.generateOrGetPlan(userId, forceRefresh, createIfAbsent, location);
        return ResultDTO.success(plan);
    }

    /**
     * 查询用户专属计划状态、是否首次免费、当前积分余额
     */
    @GetMapping("/status")
    public ResultDTO<Map<String, Object>> getPlanStatus(@RequestParam Long userId) {
        Map<String, Object> status = planService.getPlanStatus(userId);
        return ResultDTO.success(status);
    }
}
