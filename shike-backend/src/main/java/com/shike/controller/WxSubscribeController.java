package com.shike.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shike.common.ResultDTO;
import com.shike.service.WxSubscribeService;
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

    private final WxSubscribeService wxSubscribeService;
    private final UserRepository userRepository;

    @PostMapping("/subscribe")
    public ResultDTO<Map<String, Object>> recordSubscription(
            @RequestParam Long userId,
            @RequestParam(required = false) String templateId,
            @RequestParam(defaultValue = "1") Integer count,
            @RequestParam(defaultValue = "WATER") String type) {
        int newQuota = wxSubscribeService.recordSubscription(userId, templateId, count, type);
        return ResultDTO.success(Map.of("quota", newQuota));
    }

    @PostMapping("/subscribe/batch")
    public ResultDTO<Map<String, Integer>> recordBatchSubscription(
            @RequestParam Long userId,
            @RequestParam(required = false) String templateId,
            @RequestParam(defaultValue = "1") Integer count,
            @RequestParam String types) { // 逗号分隔如: "TEAM_AUDIT,TEAM_LOOT,DIET_REMINDER"
        String[] typeArr = types.split(",");
        Map<String, Integer> res = wxSubscribeService.recordBatchSubscription(userId, templateId, count, typeArr);
        return ResultDTO.success(res);
    }

    @GetMapping("/subscribe-info")
    public ResultDTO<Map<String, Object>> getSubscribeInfo(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "WATER") String type) {
        int quota = wxSubscribeService.getSubscriptionQuota(userId, type);
        return ResultDTO.success(Map.of("quota", quota, "isSubscribed", quota > 0));
    }

    @PostMapping("/unsubscribe")
    public ResultDTO<String> cancelSubscription(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "WATER") String type) {
        wxSubscribeService.cancelSubscription(userId, type);
        return ResultDTO.success("已取消微信服务通知订阅");
    }

    @RequestMapping(value = "/trigger-test-water-reminder", method = {RequestMethod.GET, RequestMethod.POST})
    public ResultDTO<String> triggerTestWaterReminder(@RequestParam Long userId) {
        log.info("Manually triggering water reminder test for user {}", userId);
        boolean sent = wxSubscribeService.sendUserNotice(userId, "WATER", "🥤 补充水分时刻到啦！", "这是饮水提醒测试，记得适时补充水分哦！", "pages/index/index");
        return ResultDTO.success(sent ? "服务通知已发送成功" : "发送未成功(可能无配额或未授权)");
    }

    @RequestMapping(value = "/trigger-test-diet-reminder", method = {RequestMethod.GET, RequestMethod.POST})
    public ResultDTO<String> triggerTestDietReminder(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "DINNER") String slot) {
        log.info("Manually triggering {} diet reminder test for user {}", slot, userId);
        String title;
        String content;
        if ("BREAKFAST".equalsIgnoreCase(slot)) {
            title = "🍳 今日早餐还未打卡";
            content = "活力早晨记得吃早餐！拍照算卡开启一天减脂目标";
        } else if ("LUNCH".equalsIgnoreCase(slot)) {
            title = "🍱 今日午餐还未打卡";
            content = "午饭时间到啦，随手拍照记餐，轻松控卡不超标！";
        } else {
            title = "🥗 今日控卡还差晚餐打卡";
            content = "距离今日结算仅剩3小时，打卡成功即可保住小队契约金！";
        }
        boolean sent = wxSubscribeService.sendUserNotice(userId, "DIET_REMINDER", title, content, "pages/index/index");
        return ResultDTO.success(sent ? "服务通知已发送成功" : "发送未成功(可能无配额或未授权)");
    }

    @PostMapping("/push-subscribe-message")
    public ResultDTO<String> pushSubscribeMessage(
            @RequestParam String openid,
            @RequestParam(required = false) String templateId,
            @RequestParam(defaultValue = "饮水与打卡提醒") String title,
            @RequestParam(defaultValue = "保持身体水分平衡，适时补充水分！") String content,
            @RequestParam(defaultValue = "pages/index/index") String pagePath) {
        boolean sent = wxSubscribeService.pushSubscribeMessageDirect(openid, templateId, title, content, pagePath);
        return sent ? ResultDTO.success("微信服务通知发送指令已执行成功") : ResultDTO.error("微信服务通知发送失败");
    }
}
