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
}
