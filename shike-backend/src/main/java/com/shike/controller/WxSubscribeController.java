package com.shike.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shike.common.ResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.shike.model.entity.User;
import com.shike.repository.UserRepository;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class WxSubscribeController {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserRepository userRepository;

    @Value("${wx.appid:}")
    private String wxAppid;

    @Value("${wx.secret:}")
    private String wxSecret;

    @PostMapping("/subscribe")
    public ResultDTO<Map<String, Object>> recordSubscription(
            @RequestParam Long userId,
            @RequestParam String templateId,
            @RequestParam(defaultValue = "1") Integer count,
            @RequestParam(defaultValue = "WATER") String type) {
        String key = "shike:wx:subscribe:" + userId + ":" + type;
        
        int currentQuota = 0;
        String val = stringRedisTemplate.opsForValue().get(key);
        if (val != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(val);
                if (node.has("quota")) {
                    currentQuota = node.get("quota").asInt();
                }
            } catch (Exception ignored) {}
        }
        int newQuota = currentQuota + count;

        Map<String, Object> data = new HashMap<>();
        data.put("templateId", templateId);
        data.put("quota", newQuota);

        try {
            ObjectMapper mapper = new ObjectMapper();
            stringRedisTemplate.opsForValue().set(key, mapper.writeValueAsString(data), 30, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("Save subscribe error", e);
        }

        log.info("Recorded subscription for user {}, added {}, total quota {}", userId, count, newQuota);
        return ResultDTO.success(Map.of("quota", newQuota));
    }

    @GetMapping("/subscribe-info")
    public ResultDTO<Map<String, Object>> getSubscribeInfo(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "WATER") String type) {
        String key = "shike:wx:subscribe:" + userId + ":" + type;
        String val = stringRedisTemplate.opsForValue().get(key);
        int quota = 0;
        if (val != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(val);
                if (node.has("quota")) {
                    quota = node.get("quota").asInt();
                }
            } catch (Exception ignored) {}
        }
        return ResultDTO.success(Map.of("quota", quota, "isSubscribed", quota > 0));
    }

    @PostMapping("/unsubscribe")
    public ResultDTO<String> cancelSubscription(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "WATER") String type) {
        String key = "shike:wx:subscribe:" + userId + ":" + type;
        stringRedisTemplate.delete(key);
        log.info("Cancelled subscription authorization for user {} type {}", userId, type);
        return ResultDTO.success("已取消微信服务通知订阅");
    }

    @RequestMapping(value = "/trigger-test-water-reminder", method = {RequestMethod.GET, RequestMethod.POST})
    public ResultDTO<String> triggerTestWaterReminder(@RequestParam Long userId) {
        log.info("Manually triggering water reminder test for user {}", userId);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getOpenid() == null || user.getOpenid().isEmpty()) {
            return ResultDTO.error("找不到用户或用户未授权openid");
        }
        String templateId = "6rHAfQw2A3WSw00LCaV9MUSop3OFVsRTAx4I-xgW5lw";
        return pushSubscribeMessage(user.getOpenid(), templateId, "🥤 补充水分时刻到啦！", "这是饮水提醒测试，记得适时补充水分哦！");
    }

    @PostMapping("/push-subscribe-message")
    public ResultDTO<String> pushSubscribeMessage(
            @RequestParam String openid,
            @RequestParam String templateId,
            @RequestParam(defaultValue = "饮水与打卡提醒") String title,
            @RequestParam(defaultValue = "保持身体水分平衡，适时补充水分！") String content) {
        try {
            if (wxAppid == null || wxAppid.isEmpty() || wxSecret == null || wxSecret.isEmpty()) {
                return ResultDTO.error("微信 AppID 或 Secret 未配置");
            }

            String tokenUrl = String.format("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
                    wxAppid, wxSecret);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest tokenRequest = HttpRequest.newBuilder().uri(URI.create(tokenUrl)).GET().build();
            HttpResponse<String> tokenResponse = client.send(tokenRequest, HttpResponse.BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();
            String accessToken = mapper.readTree(tokenResponse.body()).path("access_token").asText();

            if (accessToken == null || accessToken.isEmpty()) {
                return ResultDTO.error("获取微信 AccessToken 失败，请检查 appid/secret 配置");
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
            log.info("WeChat Subscribe Message Send Result: {}", sendResp.body());

            return ResultDTO.success("微信服务通知发送指令已提交: " + sendResp.body());
        } catch (Exception e) {
            log.error("Failed to send WeChat subscribe message", e);
            return ResultDTO.error("发送失败: " + e.getMessage());
        }
    }
}
