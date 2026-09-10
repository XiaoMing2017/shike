package com.shike.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shike.model.entity.User;
import com.shike.repository.UserRepository;
import com.shike.service.WxSubscribeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class WxSubscribeServiceImpl implements WxSubscribeService {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserRepository userRepository;

    public static final String DEFAULT_TEMPLATE_ID = "NkhvxMufBmdrVDAiiK-ySgwrDQpwUixTBwaXhSxOsLo";

    private static final ConcurrentHashMap<String, String> localSubscribeMap = new ConcurrentHashMap<>();
    private static volatile String cachedAccessToken = null;
    private static volatile long accessTokenExpireTime = 0;

    @Value("${wx.appid:wx826fd59633a30041}")
    private String wxAppid;

    @Value("${wx.secret:8194daf0bfc77af46c26710d4a2b5acc}")
    private String wxSecret;

    @Override
    public int recordSubscription(Long userId, String templateId, int count, String type) {
        if (userId == null) return 0;
        if (templateId == null || templateId.trim().isEmpty()) {
            templateId = DEFAULT_TEMPLATE_ID;
        }
        if (type == null || type.trim().isEmpty()) {
            type = "WATER";
        }
        String key = "shike:wx:subscribe:" + userId + ":" + type;

        int currentQuota = getSubscriptionQuota(userId, type);
        int newQuota = currentQuota + Math.max(1, count);

        Map<String, Object> data = new HashMap<>();
        data.put("templateId", templateId);
        data.put("quota", newQuota);

        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(data);
            localSubscribeMap.put(key, json);
            if (stringRedisTemplate != null) {
                stringRedisTemplate.opsForValue().set(key, json, 60, TimeUnit.DAYS);
            }
        } catch (Exception e) {
            log.error("Save subscribe error for user {}", userId, e);
        }

        log.info("Recorded subscription for user {}, type {}, added {}, total quota {}", userId, type, count, newQuota);
        return newQuota;
    }

    @Override
    public Map<String, Integer> recordBatchSubscription(Long userId, String templateId, int count, String[] types) {
        Map<String, Integer> res = new HashMap<>();
        if (userId == null || types == null) return res;
        for (String t : types) {
            if (t != null && !t.trim().isEmpty()) {
                int q = recordSubscription(userId, templateId, count, t.trim());
                res.put(t.trim(), q);
            }
        }
        return res;
    }

    @Override
    public int getSubscriptionQuota(Long userId, String type) {
        if (userId == null) return 0;
        String key = "shike:wx:subscribe:" + userId + ":" + type;
        String val = null;
        try {
            if (stringRedisTemplate != null) {
                val = stringRedisTemplate.opsForValue().get(key);
            }
        } catch (Exception ignored) {}
        if (val == null) {
            val = localSubscribeMap.get(key);
        }
        if (val != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(val);
                if (node.has("quota")) {
                    return node.get("quota").asInt();
                }
            } catch (Exception ignored) {}
        }
        return 0;
    }

    @Override
    public void cancelSubscription(Long userId, String type) {
        if (userId == null) return;
        String key = "shike:wx:subscribe:" + userId + ":" + type;
        localSubscribeMap.remove(key);
        try {
            if (stringRedisTemplate != null) {
                stringRedisTemplate.delete(key);
            }
        } catch (Exception ignored) {}
        log.info("Cancelled subscription for user {} type {}", userId, type);
    }

    @Override
    public boolean sendUserNotice(Long userId, String type, String title, String content, String pagePath) {
        if (userId == null) return false;
        int currentQuota = getSubscriptionQuota(userId, type);
        log.info("Attempting to send notice to user {}, type {}, current quota: {}", userId, type, currentQuota);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getOpenid() == null || user.getOpenid().trim().isEmpty()) {
            log.warn("Cannot send notice: user {} has no valid openid", userId);
            return false;
        }

        // 如果有配额，则扣减 1 次
        if (currentQuota > 0) {
            int newQuota = currentQuota - 1;
            String key = "shike:wx:subscribe:" + userId + ":" + type;
            Map<String, Object> data = new HashMap<>();
            data.put("templateId", DEFAULT_TEMPLATE_ID);
            data.put("quota", newQuota);
            try {
                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(data);
                localSubscribeMap.put(key, json);
                if (stringRedisTemplate != null) {
                    if (newQuota > 0) {
                        stringRedisTemplate.opsForValue().set(key, json, 60, TimeUnit.DAYS);
                    } else {
                        stringRedisTemplate.delete(key);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to update quota after sending notice: {}", e.getMessage());
            }
        } else {
            log.info("User {} has 0 quota for type {}, attempting direct push or skip", userId, type);
        }

        // 发送微信服务通知
        return pushSubscribeMessageDirect(user.getOpenid(), DEFAULT_TEMPLATE_ID, title, content, pagePath);
    }

    @Override
    public boolean pushSubscribeMessageDirect(String openid, String templateId, String title, String content, String pagePath) {
        if (openid == null || openid.trim().isEmpty()) return false;
        if (templateId == null || templateId.trim().isEmpty()) templateId = DEFAULT_TEMPLATE_ID;
        if (pagePath == null || pagePath.trim().isEmpty()) pagePath = "pages/index/index";

        try {
            String accessToken = getValidAccessToken();
            if (accessToken == null || accessToken.isEmpty()) {
                log.error("Failed to get valid access token for WeChat push");
                return false;
            }

            String sendUrl = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + accessToken;

            Map<String, Object> msgMap = new HashMap<>();
            msgMap.put("touser", openid);
            msgMap.put("template_id", templateId);
            msgMap.put("page", pagePath);

            Map<String, Object> dataMap = new HashMap<>();

            if ("NkhvxMufBmdrVDAiiK-ySgwrDQpwUixTBwaXhSxOsLo".equals(templateId)) {
                // 针对打卡提醒模板 NkhvxMufBmdrVDAiiK-ySgwrDQpwUixTBwaXhSxOsLo (打卡名称: thing4, 提醒时间: time13)
                Map<String, String> thing4 = new HashMap<>();
                String tVal = (title != null && !title.trim().isEmpty()) ? title.trim() : "自律打卡提醒";
                thing4.put("value", tVal.length() > 20 ? tVal.substring(0, 17) + "..." : tVal);
                dataMap.put("thing4", thing4);

                Map<String, String> time13 = new HashMap<>();
                time13.put("value", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
                dataMap.put("time13", time13);
            } else {
                // 针对旧版日常饮水提醒模板 6rHAfQw2A3WSw00LCaV9MUSop3OFVsRTAx4I-xgW5lw
                Map<String, String> time6 = new HashMap<>();
                time6.put("value", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                dataMap.put("time6", time6);

                Map<String, String> thing1 = new HashMap<>();
                thing1.put("value", title.length() > 20 ? title.substring(0, 17) + "..." : title);
                dataMap.put("thing1", thing1);

                Map<String, String> thing2 = new HashMap<>();
                thing2.put("value", content.length() > 20 ? content.substring(0, 17) + "..." : content);
                dataMap.put("thing2", thing2);
            }

            msgMap.put("data", dataMap);

            ObjectMapper mapper = new ObjectMapper();
            String jsonPayload = mapper.writeValueAsString(msgMap);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest sendReq = HttpRequest.newBuilder()
                    .uri(URI.create(sendUrl))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(6))
                    .build();

            HttpResponse<String> sendResp = client.send(sendReq, HttpResponse.BodyHandlers.ofString());
            log.info("WeChat Subscribe Message Send Result to {}: {}", openid, sendResp.body());

            JsonNode respNode = mapper.readTree(sendResp.body());
            int errcode = respNode.path("errcode").asInt(-1);
            return errcode == 0;
        } catch (Exception e) {
            log.error("Failed to send WeChat subscribe message to {}: {}", openid, e.getMessage());
            return false;
        }
    }

    private synchronized String getValidAccessToken() {
        long now = System.currentTimeMillis();
        if (cachedAccessToken != null && now < accessTokenExpireTime) {
            return cachedAccessToken;
        }

        try {
            if (wxAppid == null || wxAppid.isEmpty() || wxSecret == null || wxSecret.isEmpty()) {
                return null;
            }
            String tokenUrl = String.format("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
                    wxAppid, wxSecret);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest tokenRequest = HttpRequest.newBuilder().uri(URI.create(tokenUrl)).GET().timeout(Duration.ofSeconds(5)).build();
            HttpResponse<String> tokenResponse = client.send(tokenRequest, HttpResponse.BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(tokenResponse.body());
            String token = node.path("access_token").asText();
            int expiresIn = node.path("expires_in").asInt(7200);

            if (token != null && !token.isEmpty()) {
                cachedAccessToken = token;
                accessTokenExpireTime = now + (expiresIn - 300) * 1000L; // 提前 5 分钟刷新
                return token;
            }
        } catch (Exception e) {
            log.error("Error fetching WeChat access token: {}", e.getMessage());
        }
        return null;
    }
}
