package com.shike.service;

import com.shike.model.dto.StreakCheckinResultDTO;
import com.shike.model.dto.StreakStatusDTO;

import java.util.Map;

public interface StreakService {

    StreakStatusDTO getStreakStatus(Long userId);

    StreakCheckinResultDTO performCheckin(Long userId);

    StreakCheckinResultDTO autoCheckinOnDietRecord(Long userId);

    Map<String, Object> recoverStreak(Long userId, String method);

    Map<String, Object> recoverStreak(Long userId, String method, String dateStr);
}
