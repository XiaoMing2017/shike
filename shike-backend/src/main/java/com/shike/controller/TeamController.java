package com.shike.controller;

import com.shike.common.ResultDTO;
import com.shike.model.dto.TeamCreateDTO;
import com.shike.model.dto.TeamJoinDTO;
import com.shike.model.entity.Team;
import com.shike.model.entity.TeamMember;
import com.shike.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

@RestController
@RequestMapping("/team")
@RequiredArgsConstructor
@Slf4j
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

    @GetMapping("/qrcode")
    public void getTeamQrCode(@RequestParam("inviteCode") String inviteCode, HttpServletResponse response) {
        try {
            byte[] qrBytes = teamService.getTeamQrCode(inviteCode);
            response.setContentType("image/png");
            response.getOutputStream().write(qrBytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("Failed to generate team qrcode, redirecting to fallback public QR code generator", e);
            try {
                // Fallback: Redirect to a public QR code generator
                String redirectUrl = "https://api.qrserver.com/v1/create-qr-code/?size=280x280&data=" + inviteCode;
                response.sendRedirect(redirectUrl);
            } catch (Exception ex) {
                log.error("Failed to redirect to fallback QR generator", ex);
            }
        }
    }

    @PostMapping("/nudge")
    public ResultDTO<String> nudgeTeammate(@RequestParam Long senderId, @RequestParam Long targetUserId, @RequestParam Long teamId) {
        String res = teamService.nudgeTeammate(senderId, targetUserId, teamId);
        return ResultDTO.success(res);
    }

    @GetMapping("/nudge/alert")
    public ResultDTO<String> getPendingNudgeAlert(@RequestParam Long userId) {
        String alertMsg = teamService.getPendingNudgeAlert(userId);
        return ResultDTO.success(alertMsg);
    }
}
