package com.shike.controller;

import com.shike.common.ResultDTO;
import com.shike.model.dto.UserLoginDTO;
import com.shike.model.dto.UserProfileDTO;
import com.shike.model.entity.User;
import com.shike.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.UUID;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public ResultDTO<User> login(@RequestBody @Valid UserLoginDTO loginDTO) {
        User user = userService.loginOrRegister(loginDTO);
        return ResultDTO.success(user);
    }

    @PostMapping("/profile")
    public ResultDTO<User> updateProfile(@RequestBody @Valid UserProfileDTO profileDTO) {
        User user = userService.updateProfile(
                profileDTO.getUserId(),
                profileDTO.getAge(),
                profileDTO.getGender(),
                profileDTO.getHeight(),
                profileDTO.getWeight(),
                profileDTO.getActivityLevel(),
                profileDTO.getGoal(),
                profileDTO.getNickname(),
                profileDTO.getAvatarUrl(),
                profileDTO.getCustomGoalType(),
                profileDTO.getCustomGoalDays(),
                profileDTO.getCustomGoalWeight(),
                profileDTO.getCurrentBodyFat()
        );
        return ResultDTO.success(user);
    }

    @PostMapping("/avatar/upload")
    public ResultDTO<String> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (file.isEmpty()) {
            return ResultDTO.error("File is empty");
        }
        try {
            File uploadDir = new File("uploads").getAbsoluteFile();
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String ext = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + ext;
            File dest = new File(uploadDir, filename);
            
            // 使用 Java NIO 流拷贝，解决 Linux/云端环境下相对路径 FileNotFound 的兼容性 Bug
            java.nio.file.Files.copy(file.getInputStream(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            String scheme = request.getHeader("X-Forwarded-Proto");
            if (scheme == null || scheme.isEmpty()) {
                scheme = request.getScheme();
            }
            if ("shike.store".equalsIgnoreCase(serverName) || "117.72.61.18".equals(serverName) || "http".equals(scheme)) {
                scheme = "https";
            }
            
            String baseUrl = scheme + "://" + serverName;
            if (serverPort != 80 && serverPort != 443 && !serverName.contains("shike.store")) {
                baseUrl += ":" + serverPort;
            }
            String contextPath = request.getContextPath();
            if (contextPath != null && !contextPath.isEmpty()) {
                baseUrl += contextPath;
            }
            
            String avatarUrl = baseUrl + "/uploads/" + filename;
            return ResultDTO.success(avatarUrl);
        } catch (Exception e) {
            return ResultDTO.error("Failed to upload avatar: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResultDTO<User> getUserInfo(@PathVariable Long id) {
        User user = userService.getUserInfo(id);
        return ResultDTO.success(user);
    }

    @PostMapping("/sign-in")
    public ResultDTO<User> signIn(@RequestParam("userId") Long userId) {
        User user = userService.signIn(userId);
        return ResultDTO.success(user);
    }

    @GetMapping("/{id}/points-records")
    public ResultDTO<java.util.List<com.shike.model.entity.PointsRecord>> getPointsRecords(@PathVariable("id") Long userId) {
        java.util.List<com.shike.model.entity.PointsRecord> records = userService.getPointsRecords(userId);
        return ResultDTO.success(records);
    }
}
