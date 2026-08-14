package com.shike.controller;

import com.shike.common.ResultDTO;
import com.shike.model.dto.AnnouncementConfigDTO;
import com.shike.model.dto.ContactConfigDTO;
import com.shike.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ResultDTO<Map<String, Boolean>> getPublicFeatureToggles(@RequestParam(required = false, defaultValue = "release") String env) {
        return ResultDTO.success(adminService.getPublicFeatureToggles(env));
    }

    /**
     * 获取客户端版本公告与引导弹窗动态配置 (供小程序前端渲染)
     */
    @GetMapping("/announcement")
    public ResultDTO<AnnouncementConfigDTO> getAnnouncementConfig() {
        return ResultDTO.success(adminService.getAnnouncementConfig());
    }

    /**
     * 获取客服与联系方式配置 (供小程序客服弹窗渲染)
     */
    @GetMapping("/contact")
    public ResultDTO<ContactConfigDTO> getContactConfig() {
        return ResultDTO.success(adminService.getContactConfig());
    }
}
