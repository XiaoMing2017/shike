package com.shike.service;

import java.util.Map;

public interface WxSubscribeService {

    /**
     * 记录用户新增订阅配额
     * @param userId 用户ID
     * @param templateId 微信模板ID
     * @param count 增加的次数
     * @param type 场景类型 (WATER, TEAM_AUDIT, TEAM_LOOT, DIET_REMINDER)
     * @return 最新的总配额
     */
    int recordSubscription(Long userId, String templateId, int count, String type);

    /**
     * 批量记录用户多场景订阅配额
     */
    Map<String, Integer> recordBatchSubscription(Long userId, String templateId, int count, String[] types);

    /**
     * 获取指定场景的用户剩余推送配额
     */
    int getSubscriptionQuota(Long userId, String type);

    /**
     * 取消指定场景的订阅
     */
    void cancelSubscription(Long userId, String type);

    /**
     * 尝试向指定用户发送微信服务通知 (自动扣减配额池 1 次，并向微信下发)
     * @param userId 目标用户ID
     * @param type 场景类型 (如 TEAM_AUDIT)
     * @param title 提醒标题 (thing1, 限20字)
     * @param content 详细说明 (thing2, 限20字)
     * @param pagePath 点击跳转的小程序页面路径 (如 pages/team/team)
     * @return 是否成功扣减并下发通知
     */
    boolean sendUserNotice(Long userId, String type, String title, String content, String pagePath);

    /**
     * 直接向指定 openid 发送微信订阅消息 (底层方法)
     */
    boolean pushSubscribeMessageDirect(String openid, String templateId, String title, String content, String pagePath);
}
