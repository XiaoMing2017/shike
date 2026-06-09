package com.shike.controller;

import com.shike.common.ResultDTO;
import com.shike.model.dto.TeamCreateDTO;
import com.shike.model.dto.TeamJoinDTO;
import com.shike.model.entity.Team;
import com.shike.model.entity.TeamMember;
import com.shike.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping("/create")
    public ResultDTO<Team> createTeam(@RequestBody @Valid TeamCreateDTO createDTO) {
        Team team = teamService.createTeam(
                createDTO.getCreatorId(),
                createDTO.getTeamName(),
                createDTO.getTargetDays(),
                createDTO.getDepositPoints()
        );
        return ResultDTO.success(team);
    }

    @PostMapping("/join")
    public ResultDTO<Team> joinTeam(@RequestBody @Valid TeamJoinDTO joinDTO) {
        Team team = teamService.joinTeam(joinDTO.getUserId(), joinDTO.getInviteCode());
        return ResultDTO.success(team);
    }

    @PostMapping("/{teamId}/leave")
    public ResultDTO<Void> leaveTeam(@PathVariable Long teamId, @RequestParam Long userId) {
        teamService.leaveTeam(userId, teamId);
        return ResultDTO.success();
    }

    @GetMapping("/{id}/members")
    public ResultDTO<List<TeamMember>> getTeamMembers(@PathVariable Long id) {
        List<TeamMember> members = teamService.getTeamMembers(id);
        return ResultDTO.success(members);
    }

    @GetMapping("/user/{userId}/active")
    public ResultDTO<com.shike.model.dto.TeamDetailDTO> getActiveTeam(@PathVariable Long userId) {
        com.shike.model.dto.TeamDetailDTO detail = teamService.getActiveTeamDetails(userId);
        return ResultDTO.success(detail);
    }
}
