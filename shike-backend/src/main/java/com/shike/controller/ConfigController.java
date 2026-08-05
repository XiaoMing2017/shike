package com.shike.controller;

import com.shike.common.ResultDTO;
import com.shike.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/config", "/api/v1/config"})
@RequiredArgsConstructor
public class ConfigController {

    private final AdminService adminService;

    /**
     * 获取全平台公开的功能开关状态 (供小程序前端显示隐藏控制)
     */
    @GetMapping("/features")
    public ResultDTO<Map<String, Boolean>> getPublicFeatureToggles(@org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "release") String env) {
        return ResultDTO.success(adminService.getPublicFeatureToggles(env));
    }
}
