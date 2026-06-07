package com.shike.controller;

import com.shike.common.ResultDTO;
import com.shike.model.dto.UserLoginDTO;
import com.shike.model.dto.UserProfileDTO;
import com.shike.model.entity.User;
import com.shike.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
                profileDTO.getGoal()
        );
        return ResultDTO.success(user);
    }

    @GetMapping("/{id}")
    public ResultDTO<User> getUserInfo(@PathVariable Long id) {
        User user = userService.getUserInfo(id);
        return ResultDTO.success(user);
    }
}
