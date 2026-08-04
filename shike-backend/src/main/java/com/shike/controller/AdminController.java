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
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/login")
    public ResultDTO<AdminLoginDTO> login(@RequestBody AdminLoginDTO loginDTO) {
        return ResultDTO.success(adminService.login(loginDTO));
    }

    @GetMapping("/check-auth")
    public ResultDTO<String> checkAuth() {
        return ResultDTO.success("Authenticated");
    }

    @PostMapping("/logout")
    public ResultDTO<Void> logout() {
        return ResultDTO.success();
    }

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

    @GetMapping("/teams")
    public ResultDTO<List<AdminTeamDTO>> getAllTeams() {
        return ResultDTO.success(adminService.getAllTeams());
    }

    @PostMapping("/teams/{teamId}/status")
    public ResultDTO<Void> updateTeamStatus(@PathVariable Long teamId, @RequestParam String status) {
        adminService.updateTeamStatus(teamId, status);
        return ResultDTO.success();
    }

    @GetMapping("/export/users")
    public ResponseEntity<byte[]> exportUsersCsv() {
        String csvContent = adminService.exportUsersCsv();
        byte[] bytes = csvContent.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=shike_users.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(bytes);
    }

    @GetMapping("/export/diets")
    public ResponseEntity<byte[]> exportDietsCsv() {
        String csvContent = adminService.exportDietsCsv();
        byte[] bytes = csvContent.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=shike_diets.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(bytes);
    }
}
