package com.shike.service;

import com.shike.model.dto.UserLoginDTO;
import com.shike.model.entity.User;

import java.math.BigDecimal;

public interface UserService {
    User loginOrRegister(UserLoginDTO loginDTO);
    User updateProfile(Long userId, Integer age, Integer gender, java.math.BigDecimal height, java.math.BigDecimal weight, String activityLevel, String goal, String nickname, String avatarUrl);
    User getUserInfo(Long userId);
    User signIn(Long userId);
    java.util.List<com.shike.model.entity.PointsRecord> getPointsRecords(Long userId);
}
