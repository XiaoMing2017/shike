package com.shike.service;

import com.shike.model.entity.Team;
import com.shike.model.entity.TeamMember;
import com.shike.model.dto.TeamDetailDTO;

import java.time.LocalDate;
import java.util.List;

public interface TeamService {
    Team createTeam(Long creatorId, String teamName, Integer targetDays, Integer depositPoints);
    Team joinTeam(Long userId, String inviteCode);
    void checkin(Long userId, Long teamId, LocalDate date, boolean isSuccess);
    List<TeamMember> getTeamMembers(Long teamId);
    TeamDetailDTO getActiveTeamDetails(Long userId);
    void leaveTeam(Long userId, Long teamId);
    byte[] getTeamQrCode(String inviteCode);
    String nudgeTeammate(Long senderId, Long targetUserId, Long teamId);
    String getPendingNudgeAlert(Long userId);
    void dismissNudgeAlert(Long userId);

    // 玩法 1：每日盲盒瓜分池
    void settleDailyTeamChallenges(LocalDate targetDate);
    com.shike.model.entity.TeamLootRecord getPendingDailyLoot(Long userId);
    java.util.Map<String, Object> claimDailyLoot(Long userId, Long lootRecordId);
    List<com.shike.model.entity.TeamDailySettlement> getDailySettlementHistory(Long teamId);
    java.util.Map<String, Object> manualTriggerSettlement(LocalDate targetDate);

    // 玩法 2：减脂卧底 / 狼人杀间谍局
    java.util.Map<String, Object> getSpyGameStatus(Long userId, Long teamId);
    String postSpyTaunt(Long userId, Long teamId, String text, String imageUrl);
    String castSpyVote(Long voterId, Long targetUserId, Long teamId);

    // 玩法 3：战术道具卡牌商店
    List<java.util.Map<String, Object>> getShopItems(Long userId);
    java.util.Map<String, Object> buyShopItem(Long userId, String itemType);
    List<com.shike.model.entity.UserItem> getUserInventory(Long userId);
    java.util.Map<String, Object> useCheatShield(Long userId, Long teamId, LocalDate date);
    java.util.Map<String, Object> useSerumRevival(Long userId, Long teamId, LocalDate targetDate);
    com.shike.model.entity.TeamAuditTask triggerSniperAudit(Long senderId, Long targetUserId, Long teamId);
    java.util.Map<String, Object> deflectAudit(Long userId, Long auditTaskId);
    java.util.Map<String, Object> respondToAudit(Long userId, Long auditTaskId, Long dietRecordId);
    com.shike.model.entity.TeamAuditTask getPendingAuditForUser(Long userId);

    // 玩法 4：AI 营养师法官与每日毒舌战报
    com.shike.model.entity.TeamAiRoast generateDailyAiRoast(Long teamId, LocalDate date);
    com.shike.model.entity.TeamAiRoast getTodayAiRoast(Long teamId);
}
