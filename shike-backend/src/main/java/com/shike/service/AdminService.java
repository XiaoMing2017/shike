package com.shike.service;

import com.shike.model.dto.AdminLoginDTO;
import com.shike.model.dto.AdminStatsDTO;
import com.shike.model.dto.AdminTeamDTO;
import com.shike.model.dto.AdminUserDTO;
import com.shike.model.dto.UserDetailRecordsDTO;

import java.util.List;

public interface AdminService {
    AdminStatsDTO getDashboardStats();
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
}
