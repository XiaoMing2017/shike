/**
 * 微信小程序订阅消息与服务通知助手 (瑞幸式静默滚雪球与配额同步)
 */

const DEFAULT_TEMPLATE_ID = '6rHAfQw2A3WSw00LCaV9MUSop3OFVsRTAx4I-xgW5lw';

/**
 * 唤起订阅消息授权并向后端同步增加配额
 * @param {Array<string>|string} types 场景列表，如 ['TEAM_AUDIT', 'TEAM_LOOT'] 或 'DIET_REMINDER'
 * @param {Function} callback 完成回调 (success: boolean)
 */
function requestSubscriptions(types, callback) {
  if (!wx.requestSubscribeMessage) {
    if (callback) callback(false);
    return;
  }

  const app = getApp();
  const user = (app && app.globalData && app.globalData.userInfo) || wx.getStorageSync('userInfo');
  const typeStr = Array.isArray(types) ? types.join(',') : (types || 'GENERAL');

  wx.requestSubscribeMessage({
    tmplIds: [DEFAULT_TEMPLATE_ID],
    success: (res) => {
      const status = res[DEFAULT_TEMPLATE_ID];
      if (status === 'accept') {
        console.log('[SubscribeHelper] 用户同意订阅服务通知:', typeStr);
        wx.setStorageSync(`sub_auth_${typeStr}`, true);

        // 向后端配额池同步增加 1 次推送额度
        if (user && user.id && app && app.globalData && app.globalData.baseUrl) {
          wx.request({
            url: `${app.globalData.baseUrl}/user/subscribe/batch?userId=${user.id}&templateId=${DEFAULT_TEMPLATE_ID}&types=${typeStr}&count=1`,
            method: 'POST',
            success: (sRes) => {
              console.log('[SubscribeHelper] 订阅额度池已成功同步 +1:', sRes.data);
            },
            fail: (err) => {
              console.warn('[SubscribeHelper] 同步订阅额度至后端异常:', err);
            }
          });
        }

        if (callback) callback(true);
      } else {
        console.log('[SubscribeHelper] 用户未同意或已忽略订阅消息:', status);
        if (callback) callback(false);
      }
    },
    fail: (err) => {
      console.warn('[SubscribeHelper] 唤起订阅消息弹窗失败或环境不支持:', err);
      if (callback) callback(false);
    }
  });
}

/**
 * 请求小队相关服务通知授权 (突击查岗提醒 + 战报金币结算)
 */
function requestTeamSubscriptions(callback) {
  requestSubscriptions(['TEAM_AUDIT', 'TEAM_LOOT'], callback);
}

/**
 * 请求每日控卡与餐食打卡提醒授权
 */
function requestDietReminderSubscription(callback) {
  requestSubscriptions(['DIET_REMINDER'], callback);
}

module.exports = {
  DEFAULT_TEMPLATE_ID,
  requestSubscriptions,
  requestTeamSubscriptions,
  requestDietReminderSubscription
};
