package com.shike.service;

import com.shike.model.dto.AdminStatsDTO;
import com.shike.model.dto.AdminUserDTO;
import com.shike.model.dto.UserDetailRecordsDTO;

import java.util.List;

public interface AdminService {
    AdminStatsDTO getDashboardStats();
    List<AdminUserDTO> getAllUsers();
    UserDetailRecordsDTO getUserDetailRecords(Long userId);
}
