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
    public void getTeamQrCode(@RequestParam(value = "inviteCode", required = false) String inviteCode, HttpServletResponse response) {
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            inviteCode = "SHIKE";
        }
        try {
            byte[] qrBytes = teamService.getTeamQrCode(inviteCode);
            response.setContentType("image/png");
            response.getOutputStream().write(qrBytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("Failed to generate team qrcode, redirecting to fallback public QR code generator", e);
            try {
                String targetUrl = "https://mp.weixin.qq.com/a/~~?inviteCode=" + inviteCode;
                String redirectUrl = "https://api.qrserver.com/v1/create-qr-code/?size=430x430&data=" + java.net.URLEncoder.encode(targetUrl, "UTF-8");
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

    @PostMapping("/nudge/alert/ack")
    public ResultDTO<Void> ackNudgeAlert(@RequestParam Long userId) {
        teamService.dismissNudgeAlert(userId);
        return ResultDTO.success();
    }

    // =========================================================================
    // 玩法 1：每日盲盒瓜分池 REST 接口
    // =========================================================================

    @GetMapping("/daily-loot/pending")
    public ResultDTO<com.shike.model.entity.TeamLootRecord> getPendingDailyLoot(@RequestParam Long userId) {
        com.shike.model.entity.TeamLootRecord loot = teamService.getPendingDailyLoot(userId);
        return ResultDTO.success(loot);
    }

    @PostMapping("/daily-loot/claim")
    public ResultDTO<java.util.Map<String, Object>> claimDailyLoot(@RequestParam Long userId, @RequestParam Long lootId) {
        java.util.Map<String, Object> res = teamService.claimDailyLoot(userId, lootId);
        return ResultDTO.success(res);
    }

    @GetMapping("/daily-loot/history")
    public ResultDTO<List<com.shike.model.entity.TeamDailySettlement>> getDailySettlementHistory(@RequestParam Long teamId) {
        List<com.shike.model.entity.TeamDailySettlement> list = teamService.getDailySettlementHistory(teamId);
        return ResultDTO.success(list);
    }

    @PostMapping("/daily-loot/manual-settle")
    public ResultDTO<java.util.Map<String, Object>> manualTriggerSettlement(@RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        java.util.Map<String, Object> res = teamService.manualTriggerSettlement(date);
        return ResultDTO.success(res);
    }

    // =========================================================================
    // 玩法 2：减脂卧底 / 狼人杀间谍局 REST 接口
    // =========================================================================

    @GetMapping("/spy/status")
    public ResultDTO<java.util.Map<String, Object>> getSpyGameStatus(@RequestParam Long userId, @RequestParam Long teamId) {
        java.util.Map<String, Object> res = teamService.getSpyGameStatus(userId, teamId);
        return ResultDTO.success(res);
    }

    @PostMapping("/spy/taunt")
    public ResultDTO<String> postSpyTaunt(@RequestParam Long userId, @RequestParam Long teamId,
                                          @RequestParam(required = false) String text,
                                          @RequestParam(required = false) String imageUrl) {
        String msg = teamService.postSpyTaunt(userId, teamId, text, imageUrl);
        return ResultDTO.success(msg);
    }

    @PostMapping("/spy/vote")
    public ResultDTO<String> castSpyVote(@RequestParam Long voterId, @RequestParam Long targetUserId, @RequestParam Long teamId) {
        String msg = teamService.castSpyVote(voterId, targetUserId, teamId);
        return ResultDTO.success(msg);
    }

    // =========================================================================
    // 玩法 3：对赌战术道具卡牌商店 REST 接口
    // =========================================================================

    @GetMapping("/items/shop")
    public ResultDTO<List<java.util.Map<String, Object>>> getShopItems(@RequestParam Long userId) {
        List<java.util.Map<String, Object>> items = teamService.getShopItems(userId);
        return ResultDTO.success(items);
    }

    @PostMapping("/items/buy")
    public ResultDTO<java.util.Map<String, Object>> buyShopItem(@RequestParam Long userId, @RequestParam String itemType) {
        java.util.Map<String, Object> res = teamService.buyShopItem(userId, itemType);
        return ResultDTO.success(res);
    }

    @GetMapping("/items/inventory")
    public ResultDTO<List<com.shike.model.entity.UserItem>> getUserInventory(@RequestParam Long userId) {
        List<com.shike.model.entity.UserItem> inventory = teamService.getUserInventory(userId);
        return ResultDTO.success(inventory);
    }

    @PostMapping("/items/use/shield")
    public ResultDTO<java.util.Map<String, Object>> useCheatShield(@RequestParam Long userId, @RequestParam Long teamId,
                                                                   @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        java.util.Map<String, Object> res = teamService.useCheatShield(userId, teamId, date);
        return ResultDTO.success(res);
    }

    @PostMapping("/items/use/revival")
    public ResultDTO<java.util.Map<String, Object>> useSerumRevival(@RequestParam Long userId, @RequestParam Long teamId,
                                                                    @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate targetDate) {
        java.util.Map<String, Object> res = teamService.useSerumRevival(userId, teamId, targetDate);
        return ResultDTO.success(res);
    }

    @PostMapping("/items/use/sniper")
    public ResultDTO<com.shike.model.entity.TeamAuditTask> triggerSniperAudit(@RequestParam Long senderId, @RequestParam Long targetUserId, @RequestParam Long teamId) {
        com.shike.model.entity.TeamAuditTask task = teamService.triggerSniperAudit(senderId, targetUserId, teamId);
        return ResultDTO.success(task);
    }

    @PostMapping("/items/use/deflect")
    public ResultDTO<java.util.Map<String, Object>> deflectAudit(@RequestParam Long userId, @RequestParam Long auditTaskId) {
        java.util.Map<String, Object> res = teamService.deflectAudit(userId, auditTaskId);
        return ResultDTO.success(res);
    }

    @PostMapping("/items/audit/respond")
    public ResultDTO<java.util.Map<String, Object>> respondToAudit(@RequestParam Long userId, @RequestParam Long auditTaskId, @RequestParam Long dietRecordId) {
        java.util.Map<String, Object> res = teamService.respondToAudit(userId, auditTaskId, dietRecordId);
        return ResultDTO.success(res);
    }

    @GetMapping("/items/audit/pending")
    public ResultDTO<com.shike.model.entity.TeamAuditTask> getPendingAudit(@RequestParam Long userId) {
        com.shike.model.entity.TeamAuditTask task = teamService.getPendingAuditForUser(userId);
        return ResultDTO.success(task);
    }

    // =========================================================================
    // 玩法 4：AI 营养师法官与每日毒舌战报 REST 接口
    // =========================================================================

    @GetMapping("/ai-roast/today")
    public ResultDTO<com.shike.model.entity.TeamAiRoast> getTodayAiRoast(@RequestParam Long teamId) {
        com.shike.model.entity.TeamAiRoast roast = teamService.getTodayAiRoast(teamId);
        return ResultDTO.success(roast);
    }

    @PostMapping("/ai-roast/generate")
    public ResultDTO<com.shike.model.entity.TeamAiRoast> generateAiRoast(@RequestParam Long teamId,
                                                                         @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        com.shike.model.entity.TeamAiRoast roast = teamService.generateDailyAiRoast(teamId, date);
        return ResultDTO.success(roast);
    }
}
