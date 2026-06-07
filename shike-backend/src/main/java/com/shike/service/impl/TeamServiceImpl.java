package com.shike.service.impl;

import com.shike.common.BizException;
import com.shike.model.dto.TeamDetailDTO;
import com.shike.model.entity.DietRecord;
import com.shike.model.entity.Team;
import com.shike.model.entity.TeamCheckin;
import com.shike.model.entity.TeamMember;
import com.shike.model.entity.User;
import com.shike.repository.DietRecordRepository;
import com.shike.repository.TeamCheckinRepository;
import com.shike.repository.TeamMemberRepository;
import com.shike.repository.TeamRepository;
import com.shike.repository.UserRepository;
import com.shike.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamCheckinRepository teamCheckinRepository;
    private final UserRepository userRepository;
    private final DietRecordRepository dietRecordRepository;

    @Override
    @Transactional
    public Team createTeam(Long creatorId, String teamName, Integer targetDays) {
        log.info("Creating team: {} by creator: {}", teamName, creatorId);
        
        // Generate a simple unique invite code (first 6 chars of a UUID)
        String inviteCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Team team = Team.builder()
                .teamName(teamName)
                .creatorId(creatorId)
                .inviteCode(inviteCode)
                .targetDays(targetDays != null ? targetDays : 7)
                .status("ACTIVE")
                .build();
        
        Team savedTeam = teamRepository.save(team);

        // Creator automatically joins the team
        TeamMember member = TeamMember.builder()
                .teamId(savedTeam.getId())
                .userId(creatorId)
                .build();
        teamMemberRepository.save(member);

        return savedTeam;
    }

    @Override
    @Transactional
    public Team joinTeam(Long userId, String inviteCode) {
        log.info("User: {} attempting to join team with inviteCode: {}", userId, inviteCode);
        
        Team team = teamRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new BizException(404, "Invalid invite code"));

        if (!"ACTIVE".equals(team.getStatus())) {
            throw new BizException(400, "Team challenge is already completed or failed");
        }

        // Check if already a member
        List<TeamMember> currentMembers = teamMemberRepository.findByTeamId(team.getId());
        boolean alreadyJoined = currentMembers.stream()
                .anyMatch(m -> m.getUserId().equals(userId));
        
        if (alreadyJoined) {
            log.info("User: {} already in team: {}", userId, team.getId());
            return team;
        }

        if (currentMembers.size() >= 5) {
            throw new BizException(400, "Team is full (maximum 5 members)");
        }

        TeamMember member = TeamMember.builder()
                .teamId(team.getId())
                .userId(userId)
                .build();
        teamMemberRepository.save(member);

        log.info("User: {} successfully joined team: {}", userId, team.getId());
        return team;
    }

    @Override
    @Transactional
    public void checkin(Long userId, Long teamId, LocalDate date, boolean isSuccess) {
        log.info("Recording check-in for user: {} in team: {} on date: {}, success: {}", 
                userId, teamId, date, isSuccess);
        
        // Check if user is a member
        List<TeamMember> members = teamMemberRepository.findByTeamId(teamId);
        boolean isMember = members.stream().anyMatch(m -> m.getUserId().equals(userId));
        if (!isMember) {
            throw new BizException(403, "User is not a member of this team");
        }

        TeamCheckin checkin = TeamCheckin.builder()
                .teamId(teamId)
                .userId(userId)
                .checkinDate(date)
                .isSuccess(isSuccess)
                .build();
        
        teamCheckinRepository.save(checkin);
    }

    @Override
    public List<TeamMember> getTeamMembers(Long teamId) {
        return teamMemberRepository.findByTeamId(teamId);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamDetailDTO getActiveTeamDetails(Long userId) {
        log.info("Fetching active team details for user: {}", userId);
        
        List<TeamMember> userMemberships = teamMemberRepository.findByUserId(userId);
        if (userMemberships.isEmpty()) {
            log.info("User {} has no team memberships", userId);
            return null;
        }

        Team activeTeam = null;
        for (TeamMember membership : userMemberships) {
            Team team = teamRepository.findById(membership.getTeamId()).orElse(null);
            if (team != null && "ACTIVE".equals(team.getStatus())) {
                activeTeam = team;
                break;
            }
        }

        if (activeTeam == null) {
            log.info("User {} is not currently in any ACTIVE team", userId);
            return null;
        }

        LocalDate teamStartDate = activeTeam.getCreatedAt().toLocalDate();
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(teamStartDate, LocalDate.now());
        int currentDay = (int) daysBetween + 1;
        if (currentDay < 1) currentDay = 1;
        if (currentDay > activeTeam.getTargetDays()) currentDay = activeTeam.getTargetDays();

        List<TeamMember> teamMembers = teamMemberRepository.findByTeamId(activeTeam.getId());
        List<TeamDetailDTO.MemberDetail> memberDetails = new java.util.ArrayList<>();

        for (TeamMember member : teamMembers) {
            User user = userRepository.findById(member.getUserId()).orElse(null);
            String name = (user != null && user.getNickname() != null) ? user.getNickname() : "微信用户";
            String avatar = (user != null && user.getAvatarUrl() != null) ? user.getAvatarUrl() : "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100";

            List<TeamDetailDTO.TickDetail> ticks = new java.util.ArrayList<>();
            boolean todayChecked = false;
            int successCount = 0;

            List<TeamCheckin> checkins = teamCheckinRepository.findByTeamIdAndUserId(activeTeam.getId(), member.getUserId());

            for (int day = 1; day <= activeTeam.getTargetDays(); day++) {
                LocalDate targetDate = teamStartDate.plusDays(day - 1);
                boolean checked = false;

                if (targetDate.isBefore(LocalDate.now())) {
                    checked = checkins.stream()
                            .filter(c -> c.getCheckinDate().equals(targetDate))
                            .map(TeamCheckin::getIsSuccess)
                            .findFirst()
                            .orElse(false);
                } else if (targetDate.isEqual(LocalDate.now())) {
                    List<DietRecord> todayDiets = dietRecordRepository.findByUserIdAndRecordDate(member.getUserId(), LocalDate.now());
                    if (!todayDiets.isEmpty()) {
                        BigDecimal totalTodayCalories = todayDiets.stream()
                                .map(DietRecord::getTotalCalories)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal budget = (user != null && user.getTargetCalories() != null) 
                                ? user.getTargetCalories() 
                                : BigDecimal.valueOf(2000.0);
                        
                        checked = totalTodayCalories.compareTo(budget) <= 0;
                    }
                    todayChecked = checked;
                }
                
                if (checked) {
                    successCount++;
                }

                ticks.add(TeamDetailDTO.TickDetail.builder()
                        .day(day)
                        .checked(checked)
                        .build());
            }

            memberDetails.add(TeamDetailDTO.MemberDetail.builder()
                    .id(member.getUserId())
                    .name(name)
                    .avatar(avatar)
                    .todayChecked(todayChecked)
                    .successCount(successCount)
                    .ticks(ticks)
                    .build());
        }

        return TeamDetailDTO.builder()
                .teamId(activeTeam.getId())
                .teamName(activeTeam.getTeamName())
                .inviteCode(activeTeam.getInviteCode())
                .targetDays(activeTeam.getTargetDays())
                .points(500)
                .currentDay(currentDay)
                .status(activeTeam.getStatus())
                .members(memberDetails)
                .build();
    }
}
