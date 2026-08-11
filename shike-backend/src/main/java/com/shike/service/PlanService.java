package com.shike.service;

import java.util.Map;

public interface PlanService {

    /**
     * 生成或获取用户的专属 AI 运动与膳食计划
     * @param userId 用户ID
     * @param forceRefresh 是否强制重新生成
     * @param createIfAbsent 缓存未命中时是否自动调用 AI 模型生成
     * @param location 训练场景 (HOME: 居家, GYM: 健身房)
     * @return 包含 workoutPlan 和 dietPlan 的结构化 Map
     */
    Map<String, Object> generateOrGetPlan(Long userId, Boolean forceRefresh, Boolean createIfAbsent);
    Map<String, Object> generateOrGetPlan(Long userId, Boolean forceRefresh, Boolean createIfAbsent, String location);
    
    Map<String, Object> getPlanStatus(Long userId);
}
