package com.shike.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shike.model.entity.User;
import com.shike.model.entity.WaterRecord;
import com.shike.repository.UserRepository;
import com.shike.repository.WaterRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class WaterReminderScheduler {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserRepository userRepository;
    private final WaterRecordRepository waterRecordRepository;

    @Value("${wx.appid:}")
    private String wxAppid;

    @Value("${wx.secret:}")
    private String wxSecret;

    /**
     * 自动定时巡检微信订阅提醒
     * 调整为每天固定 3 次舒适提醒时刻：10:30（上午）、15:30（下午）、20:00（晚上）
     */
    @Scheduled(cron = "0 30 10,15,20 * * ?")
    public void scheduleWaterReminders() {
        LocalTime now = LocalTime.now();
        log.info("Starting scheduled water reminder check at {}", now);

        LocalDate todayDate = LocalDate.now();

        Set<String> subscribeKeys = stringRedisTemplate.keys("shike:wx:subscribe:*:WATER");
        if (subscribeKeys == null || subscribeKeys.isEmpty()) {
            log.info("No users have subscribed to water reminders yet.");
            return;
        }

        ObjectMapper mapper = new ObjectMapper();

        for (String key : subscribeKeys) {
            try {
                String[] parts = key.split(":");
                if (parts.length < 4) continue;
                Long userId = Long.parseLong(parts[3]);

                String val = stringRedisTemplate.opsForValue().get(key);
                if (val == null || val.isEmpty()) continue;

                String templateId = "6rHAfQw2A3WSw00LCaV9MUSop3OFVsRTAx4I-xgW5lw";
                int currentQuota = 1;

                if (val.startsWith("{")) {
                    com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(val);
                    if (node.has("templateId")) templateId = node.get("templateId").asText();
                    if (node.has("quota")) currentQuota = node.get("quota").asInt();
                }

                if (currentQuota <= 0) {
                    stringRedisTemplate.delete(key);
                    continue;
                }

                int timeSlot = getTimeSlot(now);
                String sentKey = "shike:wx:sent:" + userId + ":" + todayDate + ":" + timeSlot;
                if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(sentKey))) {
                    log.info("User {} already notified for time slot {}", userId, timeSlot);
                    continue;
                }

                User user = userRepository.findById(userId).orElse(null);
                if (user == null || user.getOpenid() == null || user.getOpenid().isEmpty()) {
                    continue;
                }

                Optional<WaterRecord> waterRecordOpt = waterRecordRepository.findByUserIdAndRecordDate(userId, todayDate);
                int totalWater = waterRecordOpt.map(WaterRecord::getAmount).orElse(0);

                int expectedWater = getExpectedWaterForSlot(timeSlot);
                if (totalWater < expectedWater) {
                    boolean success = sendWeChatSubscribeMessage(
                            user.getOpenid(),
                            templateId,
                            "🥤 补充水分时刻到啦！",
                            "今日已饮水 " + totalWater + "ml，距离目标还差 " + (2000 - totalWater) + "ml"
                    );

                    if (success) {
                        stringRedisTemplate.opsForValue().set(sentKey, "1", Duration.ofHours(12));
                        int remainingQuota = currentQuota - 1;
                        log.info("Successfully pushed WeChat water reminder to user {}. Remaining quota: {}", userId, remainingQuota);

                        if (remainingQuota > 0) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("templateId", templateId);
                            data.put("quota", remainingQuota);
                            stringRedisTemplate.opsForValue().set(key, mapper.writeValueAsString(data), 30, TimeUnit.DAYS);
                        } else {
                            stringRedisTemplate.delete(key);
                            log.info("Quota exhausted (0) for user {}, deleted subscribe key", userId);
                        }
                    } else {
                        stringRedisTemplate.delete(key);
                        log.info("Cleaned up failed subscription key for user {}", userId);
                    }
                }
            } catch (Exception e) {
                log.error("Error processing water reminder for key {}", key, e);
            }
        }
    }

    private int getTimeSlot(LocalTime now) {
        int hour = now.getHour();
        if (hour < 12) return 1;       // 上午 10:30
        if (hour < 18) return 2;       // 下午 15:30
        return 3;                      // 晚上 20:00
    }

    private int getExpectedWaterForSlot(int slot) {
        switch (slot) {
            case 1: return 500;  // 上午目标 500ml
            case 2: return 1200; // 下午目标 1200ml
            case 3: return 1800; // 晚上目标 1800ml
            default: return 500;
        }
    }

    private boolean sendWeChatSubscribeMessage(String openid, String templateId, String title, String content) {
        try {
            if (wxAppid == null || wxAppid.isEmpty() || wxSecret == null || wxSecret.isEmpty()) {
                log.warn("wxAppid or wxSecret not configured, skip real HTTP call");
                return false;
            }

            String tokenUrl = String.format("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
                    wxAppid, wxSecret);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest tokenRequest = HttpRequest.newBuilder().uri(URI.create(tokenUrl)).GET().build();
            HttpResponse<String> tokenResponse = client.send(tokenRequest, HttpResponse.BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();
            String accessToken = mapper.readTree(tokenResponse.body()).path("access_token").asText();
            if (accessToken == null || accessToken.isEmpty()) {
                return false;
            }

            String sendUrl = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + accessToken;

            Map<String, Object> msgMap = new HashMap<>();
            msgMap.put("touser", openid);
            msgMap.put("template_id", templateId);
            msgMap.put("page", "pages/index/index");

            Map<String, Object> dataMap = new HashMap<>();

            Map<String, String> time6 = new HashMap<>();
            time6.put("value", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            dataMap.put("time6", time6);

            Map<String, String> thing1 = new HashMap<>();
            thing1.put("value", title.length() > 20 ? title.substring(0, 17) + "..." : title);
            dataMap.put("thing1", thing1);

            Map<String, String> thing2 = new HashMap<>();
            thing2.put("value", content.length() > 20 ? content.substring(0, 17) + "..." : content);
            dataMap.put("thing2", thing2);

            msgMap.put("data", dataMap);

            String jsonPayload = mapper.writeValueAsString(msgMap);
            HttpRequest sendReq = HttpRequest.newBuilder()
                    .uri(URI.create(sendUrl))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> sendResp = client.send(sendReq, HttpResponse.BodyHandlers.ofString());
            log.info("Cron WeChat Subscribe Message Response: {}", sendResp.body());
            return sendResp.statusCode() == 200 && sendResp.body().contains("\"errcode\":0");
        } catch (Exception e) {
            log.error("Failed to send WeChat subscribe message in scheduler", e);
            return false;
        }
    }
}
