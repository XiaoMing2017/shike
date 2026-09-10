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
import com.shike.model.entity.TeamDailySettlement;
import com.shike.model.entity.TeamLootRecord;
import com.shike.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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
    private final com.shike.repository.TeamDailySettlementRepository teamDailySettlementRepository;
    private final com.shike.repository.TeamLootRecordRepository teamLootRecordRepository;
    private final com.shike.repository.TeamSpyVoteRepository teamSpyVoteRepository;
    private final com.shike.repository.UserItemRepository userItemRepository;
    private final com.shike.repository.TeamAuditTaskRepository teamAuditTaskRepository;
    private final com.shike.repository.TeamAiRoastRepository teamAiRoastRepository;
    private final com.shike.service.WxSubscribeService wxSubscribeService;
    private final java.util.Map<String, String> localTauntCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Value("${ai.api-key:sk-ws-H.EDLLDHH.jhKp.MEQCID5AkHa0TNfVEvwRSbT52_dMmC7R4eMyo0Q3O4cMiEiYAiAnHc4q3LtwuyxzeJFIzNXopiSxd66zHH3IiT_F98SABQ}")
    private String aiApiKey;

    @Value("${ai.model:qwen3.5-plus}")
    private String aiModel;

    @Value("${ai.roast-model:${ai.model:qwen-plus}}")
    private String aiRoastModel;

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

    // 本地无 Redis 时的提醒弹窗内存缓存 (userId -> alertMsg)
    private static final java.util.concurrent.ConcurrentHashMap<Long, String> localNudgeAlerts = new java.util.concurrent.ConcurrentHashMap<>();

    // 本地催促频率记录缓存 (senderId:targetUserId:yyyy-MM-dd -> NudgeRecord)
    private static class NudgeRecord {
        final int count;
        final long lastTimestamp;
        NudgeRecord(int count, long lastTimestamp) {
            this.count = count;
            this.lastTimestamp = lastTimestamp;
        }
    }
    private static final java.util.concurrent.ConcurrentHashMap<String, NudgeRecord> localNudgeRecords = new java.util.concurrent.ConcurrentHashMap<>();

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

        // 自动解散该用户之前创建且无其他成员的旧 ACTIVE 战队
        List<Team> oldActiveTeams = teamRepository.findByStatus("ACTIVE");
        for (Team oldTeam : oldActiveTeams) {
            if (oldTeam.getCreatorId().equals(creatorId)) {
                List<TeamMember> members = teamMemberRepository.findByTeamId(oldTeam.getId());
                if (members == null || members.size() <= 1) {
                    teamMemberRepository.deleteByTeamIdAndUserId(oldTeam.getId(), creatorId);
                    oldTeam.setStatus("DISBANDED");
                    teamRepository.save(oldTeam);
                    log.info("Auto disbanded creator's old active team: {}", oldTeam.getId());
                }
            }
        }

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
        
        Team activeTeam = getActiveTeamForUser(userId);
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
            String avatar = (user != null && user.getAvatarUrl() != null && !user.getAvatarUrl().contains("unsplash"))
                    ? user.getAvatarUrl()
                    : "https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0";

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
                    // 今天：优先检查是否有道具卡生效(如血清补签卡/欺骗餐护盾)或已有成功打卡记录
                    boolean hasCheckinSuccess = checkins.stream()
                            .anyMatch(c -> c.getCheckinDate().equals(LocalDate.now()) && Boolean.TRUE.equals(c.getIsSuccess()));
                    if (hasCheckinSuccess) {
                        checked = true;
                    } else {
                        // 实时查询饮食记录
                        List<DietRecord> todayDiets = dietRecordRepository.findByUserIdAndRecordDate(member.getUserId(), LocalDate.now());
                        if (!todayDiets.isEmpty()) {
                            BigDecimal totalTodayCalories = todayDiets.stream()
                                    .map(DietRecord::getTotalCalories)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                            BigDecimal budget = (user != null && user.getTargetCalories() != null) 
                                    ? user.getTargetCalories() 
                                    : BigDecimal.valueOf(2000.0);
                            BigDecimal maxAllowed = budget.multiply(BigDecimal.valueOf(1.25));
                            checked = totalTodayCalories.compareTo(maxAllowed) <= 0;
                        }
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
                            BigDecimal maxAllowed = budget.multiply(BigDecimal.valueOf(1.25));
                            checked = totalPastCalories.compareTo(maxAllowed) <= 0;
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

        int depositPerMember = activeTeam.getDepositPoints() != null ? activeTeam.getDepositPoints() : 100;
        int totalPoolPoints = teamMembers.size() * depositPerMember;

        // 双轨奖池计算：50% 为每日流动瓜分总池基数，50% 为终极通关大奖池
        int targetDays = activeTeam.getTargetDays() != null && activeTeam.getTargetDays() > 0 ? activeTeam.getTargetDays() : 7;
        int dailyPerPersonBase = depositPerMember / targetDays;
        if (dailyPerPersonBase < 10) dailyPerPersonBase = 10;
        int dailyPotBase = (dailyPerPersonBase / 2) * teamMembers.size();
        int finalPot = totalPoolPoints - (dailyPotBase * targetDays);
        if (finalPot < 0) finalPot = totalPoolPoints / 2;

        // 查询当前用户在当前有效小队中是否有未开的每日盲盒（严格限定当前小队，杜绝历史过期小队盲盒跨队显示）
        com.shike.model.entity.TeamLootRecord pendingLoot = teamLootRecordRepository
                .findByUserIdAndTeamIdAndStatusOrderBySettlementDateDesc(userId, activeTeam.getId(), "UNCLAIMED")
                .stream().findFirst().orElse(null);

        // 查询当前登录用户的个人真实积分余额
        User currentUser = userRepository.findById(userId).orElse(null);
        int currentUserPoints = (currentUser != null && currentUser.getPoints() != null) ? currentUser.getPoints() : 0;

        return TeamDetailDTO.builder()
                .teamId(activeTeam.getId())
                .teamName(activeTeam.getTeamName())
                .inviteCode(activeTeam.getInviteCode())
                .targetDays(activeTeam.getTargetDays())
                .points(totalPoolPoints)
                .userPoints(currentUserPoints)
                .dailyPot(dailyPotBase)
                .finalPot(finalPot)
                .currentDay(currentDay)
                .status(activeTeam.getStatus())
                .pendingLoot(pendingLoot)
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

        // 如果团队内成员已经全部退出，自动将队伍状态更新为 DISBANDED
        List<TeamMember> remaining = teamMemberRepository.findByTeamId(teamId);
        if (remaining == null || remaining.isEmpty()) {
            teamRepository.findById(teamId).ifPresent(team -> {
                team.setStatus("DISBANDED");
                teamRepository.save(team);
                log.info("Team {} has 0 members left, auto-updated status to DISBANDED", teamId);
            });
        }
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
        String nudgeKey = senderId + ":" + targetUserId + ":" + todayStr;
        long now = System.currentTimeMillis();

        // 1. 本地内存频次与30分钟冷却校验
        NudgeRecord localRec = localNudgeRecords.get(nudgeKey);
        if (localRec != null) {
            long elapsed = now - localRec.lastTimestamp;
            if (elapsed < 30 * 60 * 1000) {
                long remainMinutes = Math.max(1, (30 * 60 * 1000 - elapsed) / (60 * 1000));
                throw new BizException(400, "刚刚已经提醒过TA啦，请" + remainMinutes + "分钟后再来催促~");
            }
            if (localRec.count >= 2) {
                throw new BizException(400, "今天已提醒该队友2次啦，给TA一点自律时间吧~");
            }
        }

        // 2. Redis 校验（若有）
        String redisKey = "shike:team:nudge:" + nudgeKey;
        try {
            if (stringRedisTemplate != null) {
                String countVal = stringRedisTemplate.opsForValue().get(redisKey);
                int count = countVal != null ? Integer.parseInt(countVal) : 0;
                if (count >= 2) {
                    throw new BizException(400, "今天已提醒该队友2次啦，给TA一点自律时间吧~");
                }
            }
        } catch (BizException be) {
            throw be;
        } catch (Exception ignored) {}

        // 3. 更新本地与Redis催促记录
        int newCount = localRec != null ? localRec.count + 1 : 1;
        localNudgeRecords.put(nudgeKey, new NudgeRecord(newCount, now));
        try {
            if (stringRedisTemplate != null) {
                stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(newCount), 24, TimeUnit.HOURS);
            }
        } catch (Exception ignored) {}

        String senderName = sender.getNickname() != null ? sender.getNickname() : "队友";
        String alertMsg = "🔔 队友【" + senderName + "】喊你快去打卡：今天就差你啦，快去拍照算卡吧！";

        // 优先存入内存缓存（保证本地即使无 Redis 也能正常弹出提醒）
        localNudgeAlerts.put(targetUserId, alertMsg);

        try {
            if (stringRedisTemplate != null) {
                String alertKey = "shike:team:nudge:alert:" + targetUserId;
                stringRedisTemplate.opsForValue().set(alertKey, alertMsg, 12, TimeUnit.HOURS);
            }
        } catch (Exception ignored) {}

        // 联动发送微信官方服务通知（队友催促打卡提醒）
        try {
            String shortName = senderName.length() > 6 ? senderName.substring(0, 6) : senderName;
            wxSubscribeService.sendUserNotice(
                    targetUserId,
                    "TEAM_AUDIT",
                    "🔔 队友喊你打卡啦！",
                    "队友@" + shortName + " 喊你记餐，全队就差你啦快来打卡！",
                    "pages/index/index"
            );
        } catch (Exception e) {
            log.warn("Failed to send WeChat notice for nudge to user {}: {}", targetUserId, e.getMessage());
        }

        return "已成功提醒 " + (target.getNickname() != null ? target.getNickname() : "队友") + " 打卡！";
    }

    @Override
    public String getPendingNudgeAlert(Long userId) {
        String localMsg = localNudgeAlerts.get(userId);
        if (localMsg != null) {
            return localMsg;
        }
        try {
            if (stringRedisTemplate != null) {
                String alertKey = "shike:team:nudge:alert:" + userId;
                String msg = stringRedisTemplate.opsForValue().get(alertKey);
                if (msg != null) {
                    return msg;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public void dismissNudgeAlert(Long userId) {
        localNudgeAlerts.remove(userId);
        try {
            if (stringRedisTemplate != null) {
                String alertKey = "shike:team:nudge:alert:" + userId;
                stringRedisTemplate.delete(alertKey);
            }
        } catch (Exception ignored) {}
    }

    // =========================================================================
    // 玩法 1：每日盲盒瓜分池（Daily Jackpot Loot）业务实现
    // =========================================================================

    @Override
    @Transactional
    public void settleDailyTeamChallenges(LocalDate targetDate) {
        log.info("Starting daily challenge settlement for target date: {}", targetDate);
        List<Team> activeTeams = teamRepository.findAll().stream()
                .filter(t -> "ACTIVE".equals(t.getStatus()))
                .toList();

        for (Team team : activeTeams) {
            try {
                settleSingleTeamForDate(team, targetDate);
            } catch (Exception e) {
                log.error("Failed to settle team: {} for date: {}", team.getId(), targetDate, e);
            }
        }
        log.info("Finished daily challenge settlement for target date: {}", targetDate);
    }

    private void settleSingleTeamForDate(Team team, LocalDate targetDate) {
        LocalDate teamStartDate = team.getCreatedAt().toLocalDate();
        long dayDiff = java.time.temporal.ChronoUnit.DAYS.between(teamStartDate, targetDate);
        if (dayDiff < 0 || dayDiff >= team.getTargetDays()) {
            return; // 不在挑战周期内
        }

        // 幂等防重：检查是否已经结算过
        if (teamDailySettlementRepository.findByTeamIdAndSettlementDate(team.getId(), targetDate).isPresent()) {
            log.info("Team {} already settled for date {}", team.getId(), targetDate);
            return;
        }

        List<TeamMember> members = teamMemberRepository.findByTeamId(team.getId());
        if (members.isEmpty()) return;

        int depositPerMember = team.getDepositPoints() != null ? team.getDepositPoints() : 100;
        int targetDays = team.getTargetDays() != null && team.getTargetDays() > 0 ? team.getTargetDays() : 7;
        int dailyPerPersonBase = depositPerMember / targetDays;
        if (dailyPerPersonBase < 10) dailyPerPersonBase = 10;
        int dailyLiquidBase = dailyPerPersonBase / 2; // 每日流动瓜分底金（50%）

        List<Long> successUserIds = new java.util.ArrayList<>();
        List<Long> failedUserIds = new java.util.ArrayList<>();

        for (TeamMember m : members) {
            User user = userRepository.findById(m.getUserId()).orElse(null);
            List<DietRecord> diets = dietRecordRepository.findByUserIdAndRecordDate(m.getUserId(), targetDate);
            boolean isSuccess = false;
            // 优先检查是否已有生效的道具卡(如补签卡/欺骗餐护盾)将其标记为达标
            java.util.Optional<TeamCheckin> existingCheckin = teamCheckinRepository.findByTeamIdAndUserIdAndCheckinDate(team.getId(), m.getUserId(), targetDate);
            if (existingCheckin.isPresent() && Boolean.TRUE.equals(existingCheckin.get().getIsSuccess())) {
                isSuccess = true;
            } else if (!diets.isEmpty()) {
                BigDecimal totalCalories = diets.stream()
                        .map(DietRecord::getTotalCalories)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal budget = (user != null && user.getTargetCalories() != null)
                        ? user.getTargetCalories()
                        : BigDecimal.valueOf(2000.0);
                BigDecimal maxAllowed = budget.multiply(BigDecimal.valueOf(1.25));
                isSuccess = totalCalories.compareTo(maxAllowed) <= 0;
            }

            // 记录或更新 TeamCheckin 表
            TeamCheckin checkin = existingCheckin.orElse(TeamCheckin.builder()
                    .teamId(team.getId())
                    .userId(m.getUserId())
                    .checkinDate(targetDate)
                    .isSuccess(isSuccess)
                    .build());
            checkin.setIsSuccess(isSuccess);
            teamCheckinRepository.save(checkin);

            if (isSuccess) {
                successUserIds.add(m.getUserId());
            } else {
                failedUserIds.add(m.getUserId());
            }
        }

        int totalPenalty = failedUserIds.size() * dailyLiquidBase;
        int perPersonReward = 0;
        if (!successUserIds.isEmpty() && totalPenalty > 0) {
            perPersonReward = totalPenalty / successUserIds.size();
        }

        // 保存每日结算总表
        String summary = String.format("全队 %d 人中 %d 人自律达标，%d 人违约。违约积分池 %d 分已均分！",
                members.size(), successUserIds.size(), failedUserIds.size(), totalPenalty);

        TeamDailySettlement settlement = TeamDailySettlement.builder()
                .teamId(team.getId())
                .settlementDate(targetDate)
                .totalMembers(members.size())
                .successMembers(successUserIds.size())
                .failedMembers(failedUserIds.size())
                .penaltyPool(totalPenalty)
                .perPersonReward(perPersonReward)
                .summaryText(summary)
                .build();
        teamDailySettlementRepository.save(settlement);

        // 为达标用户生成待开盲盒（保底 10 积分自律奖励 + 瓜分违约金）
        int baseReward = perPersonReward + 10;
        for (Long sUserId : successUserIds) {
            TeamLootRecord loot = TeamLootRecord.builder()
                    .userId(sUserId)
                    .teamId(team.getId())
                    .settlementDate(targetDate)
                    .baseReward(baseReward)
                    .multiplier(1.0)
                    .finalReward(baseReward)
                    .itemReward("NONE")
                    .status("UNCLAIMED")
                    .build();
            teamLootRecordRepository.save(loot);
        }
    }

    @Override
    public com.shike.model.entity.TeamLootRecord getPendingDailyLoot(Long userId) {
        Team activeTeam = getActiveTeamForUser(userId);
        if (activeTeam == null) return null;
        return teamLootRecordRepository.findByUserIdAndTeamIdAndStatusOrderBySettlementDateDesc(userId, activeTeam.getId(), "UNCLAIMED")
                .stream().findFirst().orElse(null);
    }

    @Override
    @Transactional
    public java.util.Map<String, Object> claimDailyLoot(Long userId, Long lootRecordId) {
        TeamLootRecord loot = teamLootRecordRepository.findById(lootRecordId)
                .orElseThrow(() -> new BizException(404, "盲盒记录不存在"));

        if (!loot.getUserId().equals(userId)) {
            throw new BizException(403, "无权开启该盲盒");
        }
        if ("CLAIMED".equals(loot.getStatus())) {
            throw new BizException(400, "该盲盒已开启领取过了");
        }

        // 校验盲盒归属小队是否仍然有效且用户仍在队中
        Team team = teamRepository.findById(loot.getTeamId()).orElse(null);
        if (team == null || !"ACTIVE".equals(team.getStatus())) {
            throw new BizException(400, "该盲盒所属队伍已失效或解散，无法开启");
        }
        if (teamMemberRepository.findByTeamIdAndUserId(loot.getTeamId(), userId).isEmpty()) {
            throw new BizException(403, "你已不在该队伍中，无法开启此盲盒");
        }

        // 盲盒暴击轮盘概率算法 (权重随机 100%)
        // 15% -> 0.0x 谢谢惠顾 (NONE)
        // 45% -> 1.0x 达标分红 (NONE)
        // 24% -> 1.5x 暴击分红 (NONE)
        // 10% -> 2.0x 护盾暴击 (SHIELD_FRAGMENT)
        // 5%  -> 1.0x 补签特权 (SERUM_CARD)
        // 1%  -> 5.0x 传奇锦鲤 (AVATAR_FRAME)
        double rand = Math.random() * 100.0;
        double multiplier = 1.0;
        String itemReward = "NONE";
        String hitEffect = "BASE";

        if (rand < 15.0) {
            multiplier = 0.0;
            itemReward = "NONE";
            hitEffect = "THANKS";
        } else if (rand < 60.0) {
            multiplier = 1.0;
            itemReward = "NONE";
            hitEffect = "BASE";
        } else if (rand < 84.0) {
            multiplier = 1.5;
            itemReward = "NONE";
            hitEffect = "CRIT_1_5X";
        } else if (rand < 94.0) {
            multiplier = 2.0;
            itemReward = "SHIELD_FRAGMENT";
            hitEffect = "CRIT_2X_SHIELD";
        } else if (rand < 99.0) {
            multiplier = 1.0;
            itemReward = "SERUM_CARD";
            hitEffect = "ITEM_SERUM_CARD";
        } else {
            multiplier = 5.0;
            itemReward = "AVATAR_FRAME";
            hitEffect = "LEGENDARY_5X";
        }

        int finalPoints = (int) Math.round(loot.getBaseReward() * multiplier);
        loot.setMultiplier(multiplier);
        loot.setItemReward(itemReward);
        loot.setFinalReward(finalPoints);
        loot.setStatus("CLAIMED");
        loot.setClaimedAt(java.time.LocalDateTime.now());
        teamLootRecordRepository.save(loot);

        // 增加用户积分 (仅当收益大于 0 时入账)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        if (finalPoints > 0) {
            int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
            user.setPoints(currentPoints + finalPoints);
            userRepository.save(user);

            // 写入积分明细
            PointsRecord pRecord = PointsRecord.builder()
                    .userId(userId)
                    .amount(finalPoints)
                    .type("DAILY_LOOT")
                    .remark(String.format("小队每日自律分红盲盒 (%s倍暴击 + 道具:%s)", multiplier, itemReward))
                    .build();
            pointsRecordRepository.save(pRecord);
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("lootId", loot.getId());
        result.put("baseReward", loot.getBaseReward());
        result.put("multiplier", multiplier);
        result.put("finalPoints", finalPoints);
        result.put("itemReward", itemReward);
        result.put("hitEffect", hitEffect);
        result.put("totalPoints", user.getPoints());
        return result;
    }

    @Override
    public List<TeamDailySettlement> getDailySettlementHistory(Long teamId) {
        return teamDailySettlementRepository.findByTeamIdOrderBySettlementDateDesc(teamId);
    }

    @Override
    @Transactional
    public java.util.Map<String, Object> manualTriggerSettlement(LocalDate targetDate) {
        LocalDate settleDate = targetDate != null ? targetDate : LocalDate.now().minusDays(1);
        settleDailyTeamChallenges(settleDate);
        return java.util.Map.of("message", "Settlement triggered successfully for date " + settleDate);
    }

    // =========================================================================
    // 玩法 2：减脂卧底 / 狼人杀间谍局（Saboteur Mode）
    // =========================================================================

    @Override
    public java.util.Map<String, Object> getSpyGameStatus(Long userId, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "小队不存在"));

        List<TeamMember> members = teamMemberRepository.findByTeamId(teamId);
        
        // 如果队伍人数 >= 3 且未指派卧底，自动指派 1 名
        if (members.size() >= 3 && team.getSpyUserId() == null) {
            int randomIndex = (int) (Math.random() * members.size());
            team.setSpyUserId(members.get(randomIndex).getUserId());
            teamRepository.save(team);
        }

        boolean isSpy = team.getSpyUserId() != null && team.getSpyUserId().equals(userId);
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        String currentPeriod = now.isBefore(LocalTime.of(16, 0)) ? "LUNCH" : "DINNER";
        String currentPeriodName = "LUNCH".equals(currentPeriod) ? "午餐时段" : "晚餐时段";

        // 查当周投票情况
        List<com.shike.model.entity.TeamSpyVote> votes = teamSpyVoteRepository.findByTeamIdAndVoteDate(teamId, today);
        boolean hasVoted = votes.stream().anyMatch(v -> v.getVoterUserId().equals(userId));

        // 获取午餐与晚餐的广播（时段隔离与最新汇总）
        String lunchKey = "shike:team:spy:taunt:" + teamId + ":" + today + ":LUNCH";
        String dinnerKey = "shike:team:spy:taunt:" + teamId + ":" + today + ":DINNER";
        String latestKey = "shike:team:spy:taunt:" + teamId + ":" + today + ":LATEST";
        String legacyKey = "shike:team:spy:taunt:" + teamId + ":" + today;

        String lunchTaunt = getCachedOrRedisTaunt(lunchKey);
        String dinnerTaunt = getCachedOrRedisTaunt(dinnerKey);
        String latestTaunt = getCachedOrRedisTaunt(latestKey);
        if (latestTaunt == null) {
            latestTaunt = "DINNER".equals(currentPeriod) && dinnerTaunt != null 
                    ? dinnerTaunt 
                    : (lunchTaunt != null ? lunchTaunt : getCachedOrRedisTaunt(legacyKey));
        }

        boolean currentPeriodPosted = "LUNCH".equals(currentPeriod) ? (lunchTaunt != null) : (dinnerTaunt != null);
        String tauntPeriod = null;
        String tauntPeriodName = null;
        if (latestTaunt != null) {
            if (latestTaunt.equals(dinnerTaunt)) {
                tauntPeriod = "DINNER";
                tauntPeriodName = "夜宵诱惑";
            } else {
                tauntPeriod = "LUNCH";
                tauntPeriodName = "午餐诱惑";
            }
        }

        java.util.Map<String, Object> res = new java.util.HashMap<>();
        res.put("enabled", members.size() >= 3);
        res.put("isSpy", isSpy);
        res.put("roleName", isSpy ? "🕵️‍♂️ 减脂卧底（捣蛋鬼）" : "🛡️ 自律平民");
        res.put("roleTip", isSpy ? "你的任务是引诱其他队友破防超标！周末公投若未被投出，独吞全部终极奖池！" : "守住自律底线！周日揪出潜伏的捣蛋鬼，平分双倍终极大奖池！");
        res.put("todayTaunt", latestTaunt);
        res.put("tauntPeriod", tauntPeriod);
        res.put("tauntPeriodName", tauntPeriodName);
        res.put("tauntId", latestTaunt != null ? (teamId + "_" + today + "_" + (tauntPeriod != null ? tauntPeriod : "ALL")) : null);
        res.put("canPostTaunt", !currentPeriodPosted);
        res.put("currentPeriod", currentPeriod);
        res.put("currentPeriodName", currentPeriodName);
        res.put("hasVoted", hasVoted);
        res.put("voteCount", votes.size());
        res.put("totalMembers", members.size());
        return res;
    }

    private String getCachedOrRedisTaunt(String key) {
        String val = localTauntCache.get(key);
        if (val != null && !val.isEmpty()) return val;
        try {
            if (stringRedisTemplate != null) {
                String redisVal = stringRedisTemplate.opsForValue().get(key);
                if (redisVal != null && !redisVal.isEmpty()) {
                    localTauntCache.put(key, redisVal);
                    return redisVal;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public String postSpyTaunt(Long userId, Long teamId, String text, String imageUrl) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "小队不存在"));
        if (team.getSpyUserId() == null || !team.getSpyUserId().equals(userId)) {
            throw new BizException(403, "只有被选中的卧底才能发布匿名诱惑挑衅！");
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        String currentPeriod = now.isBefore(LocalTime.of(16, 0)) ? "LUNCH" : "DINNER";
        String periodKey = "shike:team:spy:taunt:" + teamId + ":" + today + ":" + currentPeriod;

        String existing = getCachedOrRedisTaunt(periodKey);
        if (existing != null && !existing.isEmpty()) {
            if ("LUNCH".equals(currentPeriod)) {
                throw new BizException(400, "今日午餐时段已发布过挑衅广播，晚餐时段（16:00后）再来投毒吧！");
            } else {
                throw new BizException(400, "今日晚餐时段已发布过挑衅广播，每日午餐/晚餐各限1次，明天再来吧！");
            }
        }

        String fullTaunt = (text != null && !text.trim().isEmpty()) 
                ? text.trim() 
                : ("LUNCH".equals(currentPeriod) 
                    ? "中午不来份多汁炸鸡配冰阔落吗？吃饱了才有力气减脂！😋" 
                    : "今晚的蒜香小龙虾和全糖奶茶太香了，打什么卡呀，吃起来！🦞🥤");

        // 存入当前时段
        localTauntCache.put(periodKey, fullTaunt);
        // 存入最新
        String latestKey = "shike:team:spy:taunt:" + teamId + ":" + today + ":LATEST";
        localTauntCache.put(latestKey, fullTaunt);
        // 兼容旧键
        String legacyKey = "shike:team:spy:taunt:" + teamId + ":" + today;
        localTauntCache.put(legacyKey, fullTaunt);

        try {
            if (stringRedisTemplate != null) {
                stringRedisTemplate.opsForValue().set(periodKey, fullTaunt, 24, TimeUnit.HOURS);
                stringRedisTemplate.opsForValue().set(latestKey, fullTaunt, 24, TimeUnit.HOURS);
                stringRedisTemplate.opsForValue().set(legacyKey, fullTaunt, 24, TimeUnit.HOURS);
            }
        } catch (Exception ignored) {}

        String periodDesc = "LUNCH".equals(currentPeriod) ? "午餐" : "晚餐/夜宵";
        return "今日【" + periodDesc + "】匿名诱惑广播已成功发布至小队主页！";
    }

    @Override
    @Transactional
    public String castSpyVote(Long voterId, Long targetUserId, Long teamId) {
        LocalDate today = LocalDate.now();
        if (teamSpyVoteRepository.findByTeamIdAndVoterUserIdAndVoteDate(teamId, voterId, today).isPresent()) {
            throw new BizException(400, "你今天已经投过票啦！");
        }

        com.shike.model.entity.TeamSpyVote vote = com.shike.model.entity.TeamSpyVote.builder()
                .teamId(teamId)
                .voterUserId(voterId)
                .targetUserId(targetUserId)
                .voteDate(today)
                .build();
        teamSpyVoteRepository.save(vote);
        return "指认投票成功！周日 21:00 将揭晓真相！";
    }

    // =========================================================================
    // 玩法 3：战术道具卡牌商店（Item Card Shop）
    // =========================================================================

    @Override
    public List<java.util.Map<String, Object>> getShopItems(Long userId) {
        List<com.shike.model.entity.UserItem> inventory = userItemRepository.findByUserId(userId);
        java.util.Map<String, Integer> qtyMap = new java.util.HashMap<>();
        for (com.shike.model.entity.UserItem it : inventory) {
            qtyMap.put(it.getItemType(), it.getQuantity());
        }

        List<java.util.Map<String, Object>> items = new java.util.ArrayList<>();
        items.add(java.util.Map.of(
                "itemType", "CHEAT_SHIELD",
                "name", "欺骗餐豁免盾",
                "icon", "🛡️",
                "price", 150,
                "desc", "大餐聚会当天超标免扣保证金 (每月限1次)",
                "owned", qtyMap.getOrDefault("CHEAT_SHIELD", 0)
        ));
        items.add(java.util.Map.of(
                "itemType", "SERUM_REVIVAL",
                "name", "血清补签卡",
                "icon", "💉",
                "price", 200,
                "desc", "可补录昨日漏卡并退还扣除底金 (连打3天可赠)",
                "owned", qtyMap.getOrDefault("SERUM_REVIVAL", 0)
        ));
        items.add(java.util.Map.of(
                "itemType", "SNIPER_AUDIT",
                "name", "查岗狙击卡",
                "icon", "🎯",
                "price", 50,
                "desc", "突击查岗队友！限时30分钟拍照上传，超时扣20分",
                "owned", qtyMap.getOrDefault("SNIPER_AUDIT", 0)
        ));
        items.add(java.util.Map.of(
                "itemType", "MIRROR_DEFLECT",
                "name", "反弹镜像卡",
                "icon", "🪞",
                "price", 30,
                "desc", "被查岗时反弹发起人，强制对方限时30分钟打卡",
                "owned", qtyMap.getOrDefault("MIRROR_DEFLECT", 0)
        ));
        return items;
    }

    @Override
    @Transactional
    public java.util.Map<String, Object> buyShopItem(Long userId, String itemType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "用户不存在"));

        int price = switch (itemType) {
            case "CHEAT_SHIELD" -> 150;
            case "SERUM_REVIVAL" -> 200;
            case "SNIPER_AUDIT" -> 50;
            case "MIRROR_DEFLECT" -> 30;
            default -> throw new BizException(400, "未知道具类型");
        };

        int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
        if (currentPoints < price) {
            throw new BizException(400, "积分不足以购买此道具(需 " + price + " 积分，当前仅有 " + currentPoints + " 积分)");
        }

        user.setPoints(currentPoints - price);
        userRepository.save(user);

        // 写入积分记录
        PointsRecord pr = PointsRecord.builder()
                .userId(userId)
                .amount(-price)
                .type("BUY_ITEM")
                .remark("购买对赌战术道具: " + itemType)
                .build();
        pointsRecordRepository.save(pr);

        // 增加背包道具
        com.shike.model.entity.UserItem userItem = userItemRepository.findByUserIdAndItemType(userId, itemType)
                .orElse(com.shike.model.entity.UserItem.builder()
                        .userId(userId)
                        .itemType(itemType)
                        .quantity(0)
                        .build());
        userItem.setQuantity(userItem.getQuantity() + 1);
        userItemRepository.save(userItem);

        return java.util.Map.of(
                "itemType", itemType,
                "owned", userItem.getQuantity(),
                "remainingPoints", user.getPoints(),
                "message", "购买成功！"
        );
    }

    @Override
    public List<com.shike.model.entity.UserItem> getUserInventory(Long userId) {
        return userItemRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public java.util.Map<String, Object> useCheatShield(Long userId, Long teamId, LocalDate date) {
        LocalDate useDate = date != null ? date : LocalDate.now();
        com.shike.model.entity.UserItem shield = userItemRepository.findByUserIdAndItemType(userId, "CHEAT_SHIELD")
                .orElseThrow(() -> new BizException(400, "背包中没有【欺骗餐护盾】道具，请前往商店兑换"));

        if (shield.getQuantity() <= 0) {
            throw new BizException(400, "【欺骗餐护盾】数量不足");
        }

        shield.setQuantity(shield.getQuantity() - 1);
        userItemRepository.save(shield);

        // 标记当日打卡为达标
        TeamCheckin checkin = teamCheckinRepository.findByTeamIdAndUserIdAndCheckinDate(teamId, userId, useDate)
                .orElse(TeamCheckin.builder()
                        .teamId(teamId)
                        .userId(userId)
                        .checkinDate(useDate)
                        .isSuccess(true)
                        .build());
        checkin.setIsSuccess(true);
        teamCheckinRepository.save(checkin);

        return java.util.Map.of("message", "🛡️ 【欺骗餐护盾】已生效！今日热量超标已豁免！", "remaining", shield.getQuantity());
    }

    @Override
    @Transactional
    public java.util.Map<String, Object> useSerumRevival(Long userId, Long teamId, LocalDate targetDate) {
        LocalDate remedyDate = targetDate;
        if (remedyDate == null) {
            Team team = teamRepository.findById(teamId).orElse(null);
            LocalDate teamStart = team != null && team.getCreatedAt() != null ? team.getCreatedAt().toLocalDate() : LocalDate.now().minusDays(7);
            List<TeamCheckin> checkins = teamCheckinRepository.findByTeamIdAndUserId(teamId, userId);

            // 1. 优先从昨天往回查找小队挑战期内最近漏签的一天
            LocalDate missedPastDate = null;
            for (LocalDate d = LocalDate.now().minusDays(1); !d.isBefore(teamStart); d = d.minusDays(1)) {
                LocalDate curDate = d;
                boolean hasSuccess = checkins.stream()
                        .anyMatch(c -> c.getCheckinDate().equals(curDate) && Boolean.TRUE.equals(c.getIsSuccess()));
                if (!hasSuccess) {
                    missedPastDate = curDate;
                    break;
                }
            }

            if (missedPastDate != null) {
                remedyDate = missedPastDate;
            } else {
                // 2. 过去日期均已达标，若今日尚未达标，则补签今日
                boolean todaySuccess = checkins.stream()
                        .anyMatch(c -> c.getCheckinDate().equals(LocalDate.now()) && Boolean.TRUE.equals(c.getIsSuccess()));
                if (!todaySuccess) {
                    remedyDate = LocalDate.now();
                } else {
                    remedyDate = LocalDate.now().minusDays(1);
                }
            }
        }

        com.shike.model.entity.UserItem serum = userItemRepository.findByUserIdAndItemType(userId, "SERUM_REVIVAL")
                .orElseThrow(() -> new BizException(400, "背包中没有【血清补签卡】道具"));

        if (serum.getQuantity() <= 0) {
            throw new BizException(400, "【血清补签卡】数量不足");
        }

        serum.setQuantity(serum.getQuantity() - 1);
        userItemRepository.save(serum);

        // 恢复对应日期打卡状态
        TeamCheckin checkin = teamCheckinRepository.findByTeamIdAndUserIdAndCheckinDate(teamId, userId, remedyDate)
                .orElse(TeamCheckin.builder()
                        .teamId(teamId)
                        .userId(userId)
                        .checkinDate(remedyDate)
                        .isSuccess(true)
                        .build());
        checkin.setIsSuccess(true);
        teamCheckinRepository.save(checkin);

        String dateDesc = remedyDate.equals(LocalDate.now()) ? "今日 (" + remedyDate + ")" : remedyDate.toString();
        return java.util.Map.of("message", "💉 【血清补签卡】使用成功！已成功补签 " + dateDesc + "！", "remaining", serum.getQuantity());
    }

    @Override
    @Transactional
    public com.shike.model.entity.TeamAuditTask triggerSniperAudit(Long senderId, Long targetUserId, Long teamId) {
        if (senderId.equals(targetUserId)) {
            throw new BizException(400, "不能向自己发起查岗！");
        }

        com.shike.model.entity.UserItem sniper = userItemRepository.findByUserIdAndItemType(senderId, "SNIPER_AUDIT")
                .orElseThrow(() -> new BizException(400, "背包中没有【查岗狙击卡】"));

        if (sniper.getQuantity() <= 0) {
            throw new BizException(400, "【查岗狙击卡】数量不足，请前往商店购买");
        }

        sniper.setQuantity(sniper.getQuantity() - 1);
        userItemRepository.save(sniper);

        // 生成 30 分钟查岗任务
        com.shike.model.entity.TeamAuditTask task = com.shike.model.entity.TeamAuditTask.builder()
                .teamId(teamId)
                .senderId(senderId)
                .targetId(targetUserId)
                .status("PENDING")
                .expireAt(java.time.LocalDateTime.now().plusMinutes(30))
                .rewardPoints(20)
                .build();
        teamAuditTaskRepository.save(task);

        // 发送给被查岗者的弹窗通知
        User sender = userRepository.findById(senderId).orElse(null);
        String sName = (sender != null && sender.getNickname() != null) ? sender.getNickname() : "队友";
        try {
            if (stringRedisTemplate != null) {
                stringRedisTemplate.opsForValue().set("shike:team:audit:alert:" + targetUserId,
                        "🎯 队友【" + sName + "】对你发起了【突击查岗】！请在 30 分钟内拍照上传餐食，超时将扣除 20 积分保证金！",
                        30, TimeUnit.MINUTES);
            }
        } catch (Exception ignored) {}

        // 联动发送微信官方服务通知（小队突击查岗提醒）
        try {
            String shortName = sName.length() > 6 ? sName.substring(0, 6) : sName;
            wxSubscribeService.sendUserNotice(
                    targetUserId,
                    "TEAM_AUDIT",
                    "🚨 突击查岗警报！",
                    "队友@" + shortName + " 正在抽查你的餐食，限30分内上传打卡！",
                    "pages/team/team"
            );
        } catch (Exception e) {
            log.warn("Failed to send WeChat notice for sniper audit to user {}: {}", targetUserId, e.getMessage());
        }

        return task;
    }

    @Override
    @Transactional
    public java.util.Map<String, Object> deflectAudit(Long userId, Long auditTaskId) {
        com.shike.model.entity.TeamAuditTask task = teamAuditTaskRepository.findById(auditTaskId)
                .orElseThrow(() -> new BizException(404, "查岗任务不存在"));

        if (!task.getTargetId().equals(userId)) {
            throw new BizException(403, "无权反弹该查岗任务");
        }
        if (!"PENDING".equals(task.getStatus())) {
            throw new BizException(400, "该查岗任务已结束或已反弹");
        }

        com.shike.model.entity.UserItem mirror = userItemRepository.findByUserIdAndItemType(userId, "MIRROR_DEFLECT")
                .orElseThrow(() -> new BizException(400, "背包中没有【反弹镜像卡】"));

        if (mirror.getQuantity() <= 0) {
            throw new BizException(400, "【反弹镜像卡】数量不足");
        }

        mirror.setQuantity(mirror.getQuantity() - 1);
        userItemRepository.save(mirror);

        // 反弹：目标变成原发起人，重置 30 分钟
        Long originalSender = task.getSenderId();
        task.setSenderId(userId);
        task.setTargetId(originalSender);
        task.setExpireAt(java.time.LocalDateTime.now().plusMinutes(30));
        task.setStatus("PENDING");
        teamAuditTaskRepository.save(task);

        return java.util.Map.of("message", "🪞 【反弹镜像卡】生效！已成功反弹给发起人！对方必须在 30 分钟内打卡！");
    }

    @Override
    @Transactional
    public java.util.Map<String, Object> respondToAudit(Long userId, Long auditTaskId, Long dietRecordId) {
        com.shike.model.entity.TeamAuditTask task = teamAuditTaskRepository.findById(auditTaskId)
                .orElseThrow(() -> new BizException(404, "查岗任务不存在"));

        if (!task.getTargetId().equals(userId)) {
            throw new BizException(403, "你不是被查岗的目标用户");
        }
        if (!"PENDING".equals(task.getStatus())) {
            throw new BizException(400, "该查岗任务已结束");
        }

        task.setStatus("COMPLETED");
        task.setDietRecordId(dietRecordId);
        teamAuditTaskRepository.save(task);

        // 奖励被查岗者自律防守成功 10 积分
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setPoints((user.getPoints() != null ? user.getPoints() : 0) + 10);
            userRepository.save(user);
        }

        return java.util.Map.of("message", "🎉 查岗响应成功！成功防守并获赠 10 积分！");
    }

    @Override
    public com.shike.model.entity.TeamAuditTask getPendingAuditForUser(Long userId) {
        return teamAuditTaskRepository.findTopByTargetIdAndStatusOrderByCreatedAtDesc(userId, "PENDING")
                .orElse(null);
    }

    // =========================================================================
    // 玩法 4：AI 营养师法官与每日毒舌战报（AI Roast & Arbitration）
    // =========================================================================

    @Override
    @Transactional
    public com.shike.model.entity.TeamAiRoast generateDailyAiRoast(Long teamId, LocalDate date) {
        LocalDate roastDate = date != null ? date : LocalDate.now();
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "小队不存在"));

        List<TeamMember> members = teamMemberRepository.findByTeamId(teamId);
        if (members.isEmpty()) {
            throw new BizException(400, "小队暂无成员");
        }

        // 收集全队当日饮食数据
        StringBuilder statsBuilder = new StringBuilder();
        Long mvpId = null;
        String mvpName = "自律标兵";
        Long slackerId = null;
        String slackerName = "偷吃大王";
        BigDecimal minDiff = BigDecimal.valueOf(99999);
        BigDecimal maxDiff = BigDecimal.ZERO;

        for (TeamMember m : members) {
            User u = userRepository.findById(m.getUserId()).orElse(null);
            String name = (u != null && u.getNickname() != null) ? u.getNickname() : ("队员" + m.getUserId());
            List<DietRecord> diets = dietRecordRepository.findByUserIdAndRecordDate(m.getUserId(), roastDate);

            BigDecimal totalCal = diets.stream().map(DietRecord::getTotalCalories).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal budget = (u != null && u.getTargetCalories() != null) ? u.getTargetCalories() : BigDecimal.valueOf(2000.0);
            BigDecimal diff = totalCal.subtract(budget).abs();

            statsBuilder.append(String.format("- %s: 目标 %s kcal, 实际摄入 %s kcal, 打卡 %d 餐\n",
                    name, budget, totalCal, diets.size()));

            if (diets.size() > 0 && diff.compareTo(minDiff) < 0) {
                minDiff = diff;
                mvpId = m.getUserId();
                mvpName = name;
            }
            if (diets.isEmpty() || totalCal.compareTo(budget.multiply(BigDecimal.valueOf(1.2))) > 0) {
                slackerId = m.getUserId();
                slackerName = name;
            }
        }

        String prompt = String.format("""
                你是一位以犀利毒舌、脱口秀级爆笑吐槽、损人不带脏字闻名的顶级 AI 减脂对赌总裁判（人称“减脂界毒舌暴君”）。
                请根据【%s】小队今天的饮食打卡战况，写一份炸裂微信群的《今日AI毒舌审判密报》：
                
                小队全员战况：
                %s
                
                本期被审判人员：
                - 控卡封神 MVP：%s
                - 偷吃翻车 破防猪队友：%s
                
                审判风格要求：
                1. 标题（title）：极具网感、爆笑讽刺（15字内，带 🌶️/🔥 等emoji，例如：有人在自律燃脂，有人在给脂肪充年费）；
                2. 正文（content）：字数120字左右，拒绝平淡流水账！必须点名疯狂膜拜 MVP（夸成神仙/无情的减脂机器），同时疯狂毒舌开涮猪队友（无情揭穿偷吃借口、调侃其为全队分红的散财童子、一人拉高全队体脂率等），句句扎心却让人爆笑破防，激发强烈胜负欲；
                3. 毒舌扎心一击（quote）：一句话神级吐槽金句（20字内，例如：嘴上喊着想瘦，筷子比谁都诚实，小队奖池谢谢你的巨额赞助！）；
                4. 严格只返回 JSON 格式：{"title": "...", "content": "...", "quote": "..."}
                """, team.getTeamName(), statsBuilder.toString(), mvpName, slackerName);

        String title = "🌶️ 今日毒舌审判：有人在燃脂，有人在给脂肪充年费！";
        String content = String.format("全队都在拼命控卡瓜分大奖，@%s 堪称人形热量精算机，自律得像个莫得感情的减脂AI！而 @%s 同学嘴上喊着要马甲线，筷子却悄悄伸向了高热量深渊！系统警戒红灯已焊死在你头顶，一人超标全队分红，今晚24点小队瓜分池坐等您爆金币！", mvpName, slackerName);
        String quote = "嘴上说想瘦，筷子比谁都诚实，小队奖池感谢你的无私赞助！";

        log.info("Generating AI daily roast for team {} on date {} using model {}...", teamId, roastDate, aiRoastModel);
        long startTime = System.currentTimeMillis();
        try {
            // 调用轻量高敏捷模型生成毒舌战报（1.5~2.5秒极速返回）
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            var payload = java.util.Map.of(
                    "model", (aiRoastModel != null && !aiRoastModel.isBlank()) ? aiRoastModel : "qwen-plus",
                    "messages", List.of(java.util.Map.of("role", "user", "content", prompt)),
                    "temperature", 0.85
            );
            String reqBody = new ObjectMapper().writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"))
                    .header("Authorization", "Bearer " + aiApiKey)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(reqBody, java.nio.charset.StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            if (resp.statusCode() == 200) {
                JsonNode root = new ObjectMapper().readTree(resp.body());
                String aiText = root.path("choices").get(0).path("message").path("content").asText();
                String cleanJson = aiText.replaceAll("```json", "").replaceAll("```", "").trim();
                JsonNode parsed = new ObjectMapper().readTree(cleanJson);
                if (parsed.has("title")) title = parsed.get("title").asText();
                if (parsed.has("content")) content = parsed.get("content").asText();
                if (parsed.has("quote")) quote = parsed.get("quote").asText();
                log.info("AI daily roast successfully generated in {} ms: title={}", (System.currentTimeMillis() - startTime), title);
            } else {
                log.warn("AI roast call returned non-200 status {}: {}", resp.statusCode(), resp.body());
            }
        } catch (Exception e) {
            log.warn("Failed to call AI for roast in {} ms, using spicy template: {}", (System.currentTimeMillis() - startTime), e.getMessage());
        }

        com.shike.model.entity.TeamAiRoast roast = teamAiRoastRepository.findByTeamIdAndRoastDate(teamId, roastDate)
                .orElse(com.shike.model.entity.TeamAiRoast.builder()
                        .teamId(teamId)
                        .roastDate(roastDate)
                        .build());
        roast.setMvpUserId(mvpId);
        roast.setMvpName(mvpName);
        roast.setSlackerUserId(slackerId);
        roast.setSlackerName(slackerName);
        roast.setTitle(title);
        roast.setContent(content);
        roast.setQuote(quote);
        return teamAiRoastRepository.save(roast);
    }

    @Override
    public com.shike.model.entity.TeamAiRoast getTodayAiRoast(Long teamId) {
        LocalDate today = LocalDate.now();
        return teamAiRoastRepository.findByTeamIdAndRoastDate(teamId, today)
                .orElseGet(() -> {
                    try {
                        return generateDailyAiRoast(teamId, today);
                    } catch (Exception e) {
                        return null;
                    }
                });
    }

    private Team getActiveTeamForUser(Long userId) {
        List<TeamMember> userMemberships = teamMemberRepository.findByUserId(userId);
        for (TeamMember membership : userMemberships) {
            Team team = teamRepository.findById(membership.getTeamId()).orElse(null);
            if (team != null && "ACTIVE".equals(team.getStatus())) {
                return team;
            }
        }
        return null;
    }
}
