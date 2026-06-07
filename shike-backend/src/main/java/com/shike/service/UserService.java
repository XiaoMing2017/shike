package com.shike.service;

import com.shike.model.entity.User;

import java.math.BigDecimal;

public interface UserService {
    User loginOrRegister(String openid);
    User updateProfile(Long userId, Integer age, Integer gender, BigDecimal height, BigDecimal weight, String activityLevel, String goal);
    User getUserInfo(Long userId);
}
