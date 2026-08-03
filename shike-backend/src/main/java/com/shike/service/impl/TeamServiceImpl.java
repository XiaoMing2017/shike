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
import com.shike.repository.PointsRecordRepository;
import com.shike.model.entity.PointsRecord;
import com.shike.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamCheckinRepository teamCheckinRepository;
    private final UserRepository userRepository;
    private final DietRecordRepository dietRecordRepository;
    private final PointsRecordRepository pointsRecordRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${wx.mock}")
    private boolean wxMock;

    @Value("${wx.appid}")
    private String wxAppid;

    @Value("${wx.secret}")
    private String wxSecret;

    @Value("${wx.env-version:develop}")
    private String wxEnvVersion;

    // 缓存微信 Access Token
    private static String cachedAccessToken = null;
    private static long tokenExpiryTime = 0L;

    // 缓存已经生成的小程序码二进制数据
    private static final java.util.concurrent.ConcurrentHashMap<String, byte[]> qrCodeCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    @Transactional
    public Team createTeam(Long creatorId, String teamName, Integer targetDays, Integer depositPoints) {
        log.info("Creating team: {} by creator: {} with points: {}", teamName, creatorId, depositPoints);

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new BizException(404, "Creator not found"));

        int depPoints = depositPoints != null ? depositPoints : 100;
        int currentPoints = creator.getPoints() != null ? creator.getPoints() : 0;
        if (currentPoints < depPoints) {
            throw new BizException(400, "积分余额不足以支付创建团队契约金(需 " + depPoints + " 积分，当前仅有 " + currentPoints + " 积分)");
        }

        creator.setPoints(currentPoints - depPoints);
        userRepository.save(creator);

        PointsRecord pRecord = PointsRecord.builder()
                .userId(creatorId)
                .amount(-depPoints)
                .type("TEAM_DEPOSIT")
                .remark("创建契约小队 [" + teamName + "] 冻结保证金")
                .build();
        pointsRecordRepository.save(pRecord);

        // Generate a simple unique invite code (first 6 chars of a UUID)
        String inviteCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Team team = Team.builder()
                .teamName(teamName)
                .creatorId(creatorId)
                .inviteCode(inviteCode)
                .targetDays(targetDays != null ? targetDays : 7)
                .depositPoints(depPoints)
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

        // 扣除积分逻辑
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "User not found"));
        int depPoints = team.getDepositPoints() != null ? team.getDepositPoints() : 100;
        int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
        if (currentPoints < depPoints) {
            throw new BizException(400, "积分余额不足以支付加入团队契约金(需 " + depPoints + " 积分，当前仅有 " + currentPoints + " 积分)");
        }

        user.setPoints(currentPoints - depPoints);
        userRepository.save(user);

        PointsRecord pRecord = PointsRecord.builder()
                .userId(userId)
                .amount(-depPoints)
                .type("TEAM_DEPOSIT")
                .remark("加入契约小队 [" + team.getTeamName() + "] 冻结保证金")
                .build();
        pointsRecordRepository.save(pRecord);

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

            List<TeamCheckin> checkins = teamCheckinRepository.findByTeamIdAndUserId(activeTeam.getId(), member.getUserId());

            for (int day = 1; day <= activeTeam.getTargetDays(); day++) {
                LocalDate targetDate = teamStartDate.plusDays(day - 1);
                boolean checked = false;

                if (targetDate.isAfter(LocalDate.now())) {
                    // 未来日期，不做判断
                    checked = false;
                } else if (targetDate.isEqual(LocalDate.now())) {
                    // 今天：实时查询饮食记录
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
                } else {
                    // 过去日期：优先从 TeamCheckin 表查记录
                    java.util.Optional<TeamCheckin> checkinOpt = checkins.stream()
                            .filter(c -> c.getCheckinDate().equals(targetDate))
                            .findFirst();
                    
                    if (checkinOpt.isPresent()) {
                        checked = checkinOpt.get().getIsSuccess();
                    } else {
                        // 无结算记录（定时任务可能还未执行），回退到实时饮食记录判断
                        List<DietRecord> pastDiets = dietRecordRepository.findByUserIdAndRecordDate(member.getUserId(), targetDate);
                        if (!pastDiets.isEmpty()) {
                            BigDecimal totalPastCalories = pastDiets.stream()
                                    .map(DietRecord::getTotalCalories)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                            BigDecimal budget = (user != null && user.getTargetCalories() != null) 
                                    ? user.getTargetCalories() 
                                    : BigDecimal.valueOf(2000.0);
                            checked = totalPastCalories.compareTo(budget) <= 0;
                        }
                    }
                }

                ticks.add(TeamDetailDTO.TickDetail.builder()
                        .day(day)
                        .checked(checked)
                        .build());
            }

            // 计算真正的「连续达标天数」：从今天/最近已过日期往回数连续成功天数
            int successCount = 0;
            for (int i = ticks.size() - 1; i >= 0; i--) {
                LocalDate tickDate = teamStartDate.plusDays(i);
                if (tickDate.isAfter(LocalDate.now())) continue; // 跳过未来日期
                
                boolean isToday = tickDate.isEqual(LocalDate.now());
                if (ticks.get(i).getChecked() != null && ticks.get(i).getChecked()) {
                    successCount++;
                } else {
                    if (isToday) {
                        // 如果是今天且未打卡，不算中断，继续往前累计昨天及之前的状态
                        continue;
                    }
                    break; // 过去任何一天未达标，中断连续计数
                }
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

        int totalPoolPoints = teamMembers.size() * (activeTeam.getDepositPoints() != null ? activeTeam.getDepositPoints() : 100);

        return TeamDetailDTO.builder()
                .teamId(activeTeam.getId())
                .teamName(activeTeam.getTeamName())
                .inviteCode(activeTeam.getInviteCode())
                .targetDays(activeTeam.getTargetDays())
                .points(totalPoolPoints)
                .currentDay(currentDay)
                .status(activeTeam.getStatus())
                .members(memberDetails)
                .build();
    }

    @Override
    @Transactional
    public void leaveTeam(Long userId, Long teamId) {
        teamMemberRepository.deleteByTeamIdAndUserId(teamId, userId);
        userRepository.findById(userId).ifPresent(user -> {
            int originalPoints = user.getPoints() != null ? user.getPoints() : 1000;
            user.setPoints(Math.max(0, originalPoints - 100));
            userRepository.save(user);
            
            PointsRecord pRecord = PointsRecord.builder()
                    .userId(userId)
                    .amount(-100)
                    .type("TEAM_DEPOSIT")
                    .remark("中途退出小队扣除惩罚积分")
                    .build();
            pointsRecordRepository.save(pRecord);
        });
        log.info("User {} left team {} and got 100 points penalty.", userId, teamId);
    }

    @Override
    public byte[] getTeamQrCode(String inviteCode) {
        log.info("Generating WeChat Mini Program QR Code for inviteCode: {}", inviteCode);

        // 1. 优先从内存缓存中获取已生成的小程序码
        if (qrCodeCache.containsKey(inviteCode)) {
            log.info("Found cached QR code bytes for inviteCode: {}", inviteCode);
            return qrCodeCache.get(inviteCode);
        }

        if (wxMock) {
            log.info("WeChat Mini Program is mocked, generating local QR code via ZXing for inviteCode: {}", inviteCode);
            try {
                String encodeUrl = "https://mp.weixin.qq.com/a/~~?inviteCode=" + inviteCode;
                com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
                com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(encodeUrl, com.google.zxing.BarcodeFormat.QR_CODE, 430, 430);
                java.io.ByteArrayOutputStream pngOutputStream = new java.io.ByteArrayOutputStream();
                com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
                byte[] qrBytes = pngOutputStream.toByteArray();
                qrCodeCache.put(inviteCode, qrBytes);
                return qrBytes;
            } catch (Exception e) {
                log.error("Failed to generate local QR code using ZXing", e);
                throw new BizException(500, "Failed to generate local QR code: " + e.getMessage());
            }
        }

        try {
            // 2. 获取 Access Token (优先使用缓存的 Token)
            String accessToken;
            long now = System.currentTimeMillis();
            if (cachedAccessToken != null && now < tokenExpiryTime) {
                accessToken = cachedAccessToken;
                log.info("Using cached WeChat access token.");
            } else {
                log.info("WeChat access token expired or null, fetching from WeChat API...");
                String tokenUrl = String.format("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
                        wxAppid, wxSecret);
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest tokenRequest = HttpRequest.newBuilder()
                        .uri(URI.create(tokenUrl))
                        .GET()
                        .timeout(Duration.ofMillis(5000))
                        .build();
                HttpResponse<String> tokenResponse = client.send(tokenRequest, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
                String tokenBody = tokenResponse.body();
                
                ObjectMapper mapper = new ObjectMapper();
                JsonNode tokenNode = mapper.readTree(tokenBody);
                if (!tokenNode.has("access_token")) {
                    throw new BizException(500, "Failed to fetch WeChat access token: " + tokenBody);
                }
                accessToken = tokenNode.get("access_token").asText();
                long expiresIn = tokenNode.has("expires_in") ? tokenNode.get("expires_in").asLong() : 7200L;
                
                // 将 Token 缓存起来，为了安全起见，我们提前 5 分钟失效
                cachedAccessToken = accessToken;
                tokenExpiryTime = now + (expiresIn - 300) * 1000;
                log.info("Fetched new WeChat access token. Expires in {} seconds.", expiresIn);
            }

            // 3. 请求无限制小程序码
            String wxaUrl = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + accessToken;
            String sceneVal = (inviteCode != null && !inviteCode.trim().isEmpty()) ? inviteCode : "SHIKE";
            String requestBody = String.format("{\"scene\":\"%s\",\"page\":\"pages/index/index\",\"width\":430,\"check_path\":false,\"env_version\":\"%s\"}",
                    sceneVal, (wxEnvVersion != null ? wxEnvVersion : "develop"));
            
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest wxaRequest = HttpRequest.newBuilder()
                    .uri(URI.create(wxaUrl))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(10000))
                    .build();
            
            HttpResponse<byte[]> wxaResponse = client.send(wxaRequest, HttpResponse.BodyHandlers.ofByteArray());
            byte[] responseBytes = wxaResponse.body();
            
            // 检查返回结果是否是 JSON (说明是报错信息) 而不是图片字节流
            if (responseBytes.length < 1000) {
                String responseStr = new String(responseBytes, java.nio.charset.StandardCharsets.UTF_8);
                if (responseStr.contains("errcode")) {
                    throw new BizException(500, "WeChat API error: " + responseStr);
                }
            }

            // 4. 将生成的小程序码字节存入缓存
            qrCodeCache.put(inviteCode, responseBytes);
            return responseBytes;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate WeChat QR Code", e);
            throw new BizException(500, "Failed to generate WeChat QR Code: " + e.getMessage());
        }
    }

    @Override
    public String nudgeTeammate(Long senderId, Long targetUserId, Long teamId) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new BizException(404, "发送者不存在"));
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BizException(404, "目标队友不存在"));

        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String redisKey = "shike:team:nudge:" + senderId + ":" + targetUserId + ":" + todayStr;

        String countVal = stringRedisTemplate.opsForValue().get(redisKey);
        int count = countVal != null ? Integer.parseInt(countVal) : 0;
        if (count >= 3) {
            throw new BizException(400, "今天已经提醒过该队友3次啦，给TA一点时间吧~");
        }

        stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(count + 1), 24, TimeUnit.HOURS);

        String alertKey = "shike:team:nudge:alert:" + targetUserId;
        String senderName = sender.getNickname() != null ? sender.getNickname() : "队友";
        String alertMsg = "🔔 队友【" + senderName + "】喊你快去打卡：今天就差你啦，快去拍照算卡吧！";
        stringRedisTemplate.opsForValue().set(alertKey, alertMsg, 12, TimeUnit.HOURS);

        return "已成功提醒 " + (target.getNickname() != null ? target.getNickname() : "队友") + " 打卡！";
    }

    @Override
    public String getPendingNudgeAlert(Long userId) {
        String alertKey = "shike:team:nudge:alert:" + userId;
        String msg = stringRedisTemplate.opsForValue().get(alertKey);
        if (msg != null) {
            stringRedisTemplate.delete(alertKey);
            return msg;
        }
        return null;
    }
}
