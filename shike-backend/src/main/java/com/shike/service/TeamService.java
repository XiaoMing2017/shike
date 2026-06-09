package com.shike.service;

import com.shike.model.entity.Team;
import com.shike.model.entity.TeamMember;

import java.time.LocalDate;
import java.util.List;

import com.shike.model.dto.TeamDetailDTO;

public interface TeamService {
    Team createTeam(Long creatorId, String teamName, Integer targetDays, Integer depositPoints);
    Team joinTeam(Long userId, String inviteCode);
    void checkin(Long userId, Long teamId, LocalDate date, boolean isSuccess);
    List<TeamMember> getTeamMembers(Long teamId);
    TeamDetailDTO getActiveTeamDetails(Long userId);
    void leaveTeam(Long userId, Long teamId);
}
