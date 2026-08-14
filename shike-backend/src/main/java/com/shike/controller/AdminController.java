package com.shike.controller;

import com.shike.common.ResultDTO;
import com.shike.model.dto.AdminLoginDTO;
import com.shike.model.dto.AdminStatsDTO;
import com.shike.model.dto.AdminTeamDTO;
import com.shike.model.dto.AdminUserDTO;
import com.shike.model.dto.UserDetailRecordsDTO;
import com.shike.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping({"/admin", "/api/v1/admin"})
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping({"/login", "/api/v1/admin/login"})
    public ResultDTO<AdminLoginDTO> login(@RequestBody AdminLoginDTO loginDTO) {
        return ResultDTO.success(adminService.login(loginDTO));
    }

    @GetMapping({"/check-auth", "/api/v1/admin/check-auth"})
    public ResultDTO<String> checkAuth() {
        return ResultDTO.success("Authenticated");
    }

    @PostMapping({"/logout", "/api/v1/admin/logout"})
    public ResultDTO<Void> logout() {
        return ResultDTO.success();
    }

    @GetMapping({"/stats", "/api/v1/admin/stats"})
    public ResultDTO<AdminStatsDTO> getDashboardStats(@RequestParam(value = "days", required = false) Integer days) {
        return ResultDTO.success(adminService.getDashboardStats(days));
    }

    @GetMapping({"/users", "/api/v1/admin/users"})
    public ResultDTO<List<AdminUserDTO>> getAllUsers() {
        return ResultDTO.success(adminService.getAllUsers());
    }

    @GetMapping({"/users/{userId}/records", "/api/v1/admin/users/{userId}/records"})
    public ResultDTO<UserDetailRecordsDTO> getUserDetailRecords(@PathVariable Long userId) {
        return ResultDTO.success(adminService.getUserDetailRecords(userId));
    }

    @GetMapping({"/teams", "/api/v1/admin/teams"})
    public ResultDTO<List<AdminTeamDTO>> getAllTeams() {
        return ResultDTO.success(adminService.getAllTeams());
    }

    @PostMapping({"/teams/{teamId}/status", "/api/v1/admin/teams/{teamId}/status"})
    public ResultDTO<Void> updateTeamStatus(@PathVariable Long teamId, @RequestParam String status) {
        adminService.updateTeamStatus(teamId, status);
        return ResultDTO.success();
    }

    @PostMapping({"/users/{userId}/status", "/api/v1/admin/users/{userId}/status"})
    public ResultDTO<Void> updateUserStatus(@PathVariable Long userId, @RequestParam String status) {
        adminService.updateUserStatus(userId, status, "admin");
        return ResultDTO.success();
    }

    @PostMapping({"/users/{userId}/points", "/api/v1/admin/users/{userId}/points"})
    public ResultDTO<Void> updateUserPoints(@PathVariable Long userId, @RequestParam Integer pointsDelta, @RequestParam(required = false) String remark) {
        adminService.updateUserPoints(userId, pointsDelta, remark, "admin");
        return ResultDTO.success();
    }

    @PostMapping({"/config/ai-limit", "/api/v1/admin/config/ai-limit"})
    public ResultDTO<Void> updateGlobalAiLimit(@RequestParam Integer limit) {
        adminService.updateGlobalAiLimit(limit, "admin");
        return ResultDTO.success();
    }

    @GetMapping({"/users/{userId}/point-logs", "/api/v1/admin/users/{userId}/point-logs"})
    public ResultDTO<List<com.shike.model.entity.PointLog>> getUserPointLogs(@PathVariable Long userId) {
        return ResultDTO.success(adminService.getUserPointLogs(userId));
    }

    @GetMapping({"/audit-logs", "/api/v1/admin/audit-logs"})
    public ResultDTO<List<com.shike.model.entity.AdminAuditLog>> getAuditLogs() {
        return ResultDTO.success(adminService.getAuditLogs());
    }

    @GetMapping({"/export/users", "/api/v1/admin/export/users"})
    public ResponseEntity<byte[]> exportUsersCsv() {
        String csvContent = adminService.exportUsersCsv();
        byte[] bytes = csvContent.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=shike_users.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(bytes);
    }

    @GetMapping({"/export/diets", "/api/v1/admin/export/diets"})
    public ResponseEntity<byte[]> exportDietsCsv() {
        String csvContent = adminService.exportDietsCsv();
        byte[] bytes = csvContent.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=shike_diets.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(bytes);
    }

    @GetMapping({"/features", "/api/v1/admin/features"})
    public ResultDTO<List<com.shike.model.entity.FeatureToggle>> getAllFeatureToggles() {
        return ResultDTO.success(adminService.getAllFeatureToggles());
    }

    @PostMapping({"/features/{featureKey}/toggle", "/api/v1/admin/features/{featureKey}/toggle"})
    public ResultDTO<Void> updateFeatureToggle(
            @PathVariable String featureKey,
            @RequestParam(required = false) String envMode,
            @RequestParam(required = false) Boolean enabled) {
        adminService.updateFeatureToggle(featureKey, envMode, enabled, "admin");
        return ResultDTO.success();
    }

    @GetMapping({"/announcement", "/api/v1/admin/announcement"})
    public ResultDTO<com.shike.model.dto.AnnouncementConfigDTO> getAdminAnnouncementConfig() {
        return ResultDTO.success(adminService.getAnnouncementConfig());
    }

    @PostMapping({"/announcement", "/api/v1/admin/announcement"})
    public ResultDTO<Void> updateAnnouncementConfig(@RequestBody com.shike.model.dto.AnnouncementConfigDTO dto) {
        adminService.updateAnnouncementConfig(dto, "admin");
        return ResultDTO.success();
    }

    @GetMapping({"/ai-models", "/api/v1/admin/ai-models"})
    public ResultDTO<java.util.Map<String, String>> getAiModelConfig() {
        return ResultDTO.success(adminService.getAiModelConfig());
    }

    @GetMapping({"/ai-models/details", "/api/v1/admin/ai-models/details"})
    public ResultDTO<java.util.Map<String, Object>> getAiModelConfigDetails() {
        return ResultDTO.success(adminService.getAiModelConfigDetails());
    }

    @PostMapping({"/ai-models", "/api/v1/admin/ai-models"})
    public ResultDTO<Void> updateAiModelConfig(
            @RequestParam(value = "planModel", required = false) String planModel,
            @RequestParam(value = "dietModel", required = false) String dietModel) {
        adminService.updateAiModelConfig(planModel, dietModel, "admin");
        return ResultDTO.success();
    }

    @PostMapping({"/ai-models/add-option", "/api/v1/admin/ai-models/add-option"})
    public ResultDTO<Void> addCustomAiModelOption(
            @RequestParam("moduleKey") String moduleKey,
            @RequestParam("modelName") String modelName) {
        adminService.addCustomAiModelOption(moduleKey, modelName, "admin");
        return ResultDTO.success();
    }

    @GetMapping({"/server-status", "/api/v1/admin/server-status"})
    public ResultDTO<com.shike.model.dto.ServerStatusDTO> getServerStatus() {
        return ResultDTO.success(adminService.getServerStatus());
    }

    private final com.shike.service.FeedbackService feedbackService;

    @GetMapping({"/feedback/list", "/api/v1/admin/feedback/list"})
    public ResultDTO<com.shike.model.dto.AdminFeedbackPageDTO> getAdminFeedbackList(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResultDTO.success(feedbackService.getAdminFeedbacks(status, type, page, size));
    }

    @PostMapping({"/feedback/{id}/process", "/api/v1/admin/feedback/{id}/process"})
    public ResultDTO<com.shike.model.dto.FeedbackDTO> processFeedback(
            @PathVariable("id") Long id,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "adminReply", required = false) String adminReply,
            @RequestParam(value = "rewardPoints", required = false) Integer rewardPoints) {
        return ResultDTO.success(feedbackService.processFeedback(id, status, adminReply, rewardPoints));
    }

    @GetMapping({"/feedback/stats", "/api/v1/admin/feedback/stats"})
    public ResultDTO<java.util.Map<String, Object>> getFeedbackStatsWidget() {
        return ResultDTO.success(feedbackService.getFeedbackStatsWidget());
    }

    @GetMapping({"/config/contact", "/api/v1/admin/config/contact"})
    public ResultDTO<com.shike.model.dto.ContactConfigDTO> getAdminContactConfig() {
        return ResultDTO.success(adminService.getContactConfig());
    }

    @PostMapping({"/config/contact", "/api/v1/admin/config/contact"})
    public ResultDTO<Void> updateContactConfig(@RequestBody com.shike.model.dto.ContactConfigDTO dto) {
        adminService.updateContactConfig(dto, "admin");
        return ResultDTO.success();
    }

    @GetMapping({"/feature-usage/overview", "/api/v1/admin/feature-usage/overview"})
    public ResultDTO<com.shike.model.dto.AdminFeatureUsageDTO> getFeatureUsageOverview() {
        return ResultDTO.success(adminService.getFeatureUsageOverview());
    }
}
