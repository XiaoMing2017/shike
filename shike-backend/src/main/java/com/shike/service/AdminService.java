package com.shike.service;

import com.shike.model.dto.AdminLoginDTO;
import com.shike.model.dto.AdminStatsDTO;
import com.shike.model.dto.AdminTeamDTO;
import com.shike.model.dto.AdminUserDTO;
import com.shike.model.dto.UserDetailRecordsDTO;

import java.util.List;

public interface AdminService {
    AdminStatsDTO getDashboardStats();
    AdminStatsDTO getDashboardStats(Integer days);
    List<AdminUserDTO> getAllUsers();
    UserDetailRecordsDTO getUserDetailRecords(Long userId);
    List<AdminTeamDTO> getAllTeams();
    void updateTeamStatus(Long teamId, String status);

    AdminLoginDTO login(AdminLoginDTO loginDTO);
    String exportUsersCsv();
    String exportDietsCsv();

    void updateUserStatus(Long userId, String status, String adminUsername);
    void updateUserPoints(Long userId, Integer pointsDelta, String remark, String adminUsername);
    void updateGlobalAiLimit(Integer limit, String adminUsername);
    List<com.shike.model.entity.PointLog> getUserPointLogs(Long userId);
    List<com.shike.model.entity.AdminAuditLog> getAuditLogs();

    List<com.shike.model.entity.FeatureToggle> getAllFeatureToggles();
    void updateFeatureToggle(String featureKey, String envMode, Boolean enabled, String adminUsername);
    java.util.Map<String, Boolean> getPublicFeatureToggles(String env);

    com.shike.model.dto.AnnouncementConfigDTO getAnnouncementConfig();
    void updateAnnouncementConfig(com.shike.model.dto.AnnouncementConfigDTO dto, String adminUsername);

    com.shike.model.dto.ServerStatusDTO getServerStatus();

    java.util.Map<String, String> getAiModelConfig();
    java.util.Map<String, Object> getAiModelConfigDetails();
    void updateAiModelConfig(String planModel, String dietModel, String adminUsername);
    void addCustomAiModelOption(String moduleKey, String modelName, String adminUsername);

    com.shike.model.dto.AdminFeatureUsageDTO getFeatureUsageOverview();

    com.shike.model.dto.ContactConfigDTO getContactConfig();
    void updateContactConfig(com.shike.model.dto.ContactConfigDTO dto, String adminUsername);
}
