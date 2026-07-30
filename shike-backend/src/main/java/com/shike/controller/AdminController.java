package com.shike.controller;

import com.shike.common.ResultDTO;
import com.shike.model.dto.AdminStatsDTO;
import com.shike.model.dto.AdminUserDTO;
import com.shike.model.dto.UserDetailRecordsDTO;
import com.shike.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ResultDTO<AdminStatsDTO> getDashboardStats() {
        return ResultDTO.success(adminService.getDashboardStats());
    }

    @GetMapping("/users")
    public ResultDTO<List<AdminUserDTO>> getAllUsers() {
        return ResultDTO.success(adminService.getAllUsers());
    }

    @GetMapping("/users/{userId}/records")
    public ResultDTO<UserDetailRecordsDTO> getUserDetailRecords(@PathVariable Long userId) {
        return ResultDTO.success(adminService.getUserDetailRecords(userId));
    }
}
