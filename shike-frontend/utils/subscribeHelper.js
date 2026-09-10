/**
 * 微信小程序订阅消息与服务通知助手 (瑞幸式静默滚雪球与配额同步)
 */

const DEFAULT_TEMPLATE_ID = 'NkhvxMufBmdrVDAiiK-ySgwrDQpwUixTBwaXhSxOsLo';

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
        if (typeStr.includes('TEAM')) {
          wx.setStorageSync('team_wx_subscribed', true);
        }
        if (typeStr.includes('WATER')) {
          wx.setStorageSync('water_wx_subscribed', true);
        }
        if (typeStr.includes('DIET')) {
          wx.setStorageSync('diet_wx_subscribed', true);
        }

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
 * 用于“创建小队”与“加入小队”时调用
 * @param {Function} callback 完成回调
 * @param {Boolean} isSilent 是否为静默调用（默认 false：首次主动拉起微信授权让用户有机会勾选“总是保持”，已授权则静默累加）
 */
function requestTeamSubscriptions(callback, isSilent = false) {
  if (isSilent && !wx.getStorageSync('team_wx_subscribed') && !wx.getStorageSync('global_reminder_subscribed')) {
    if (callback) callback(false);
    return;
  }
  requestSubscriptions(['TEAM_AUDIT', 'TEAM_LOOT'], callback);
}

/**
 * 催TA打卡手势触发的服务通知授权（静默累加 TEAM_AUDIT 配额）
 * @param {Function} callback 完成回调
 * @param {Boolean} isSilent 默认 true：防扰保护，已开启过提醒才静默累加
 */
function requestNudgeSubscription(callback, isSilent = true) {
  const hasAuth = wx.getStorageSync('team_wx_subscribed') ||
                  wx.getStorageSync('global_reminder_subscribed') ||
                  wx.getStorageSync('sub_auth_TEAM_AUDIT');
  if (isSilent && !hasAuth) {
    if (callback) callback(false);
    return;
  }
  requestSubscriptions(['TEAM_AUDIT'], callback);
}

/**
 * 饮水打卡手势触发的服务通知授权（静默累加 WATER 配额）
 * 每次用户点击 +250ml / +500ml 时调用
 * @param {Function} callback 完成回调
 * @param {Boolean} isSilent 默认 true：防扰保护，已开启过饮水/全局提醒才静默累加
 */
function requestWaterSubscription(callback, isSilent = true) {
  const hasAuth = wx.getStorageSync('water_wx_subscribed') ||
                  wx.getStorageSync('global_reminder_subscribed') ||
                  wx.getStorageSync('sub_auth_WATER');
  if (isSilent && !hasAuth) {
    if (callback) callback(false);
    return;
  }
  requestSubscriptions(['WATER'], callback);
}

/**
 * 请求每日控卡与餐食打卡提醒授权 (瑞幸式静默滚雪球)
 * @param {Function} callback 完成回调
 * @param {Boolean} isSilent 是否为静默调用（默认 true：若未主动授权过则不强弹扰民）
 */
function requestDietReminderSubscription(callback, isSilent = true) {
  const hasAuth = wx.getStorageSync('diet_wx_subscribed') ||
                  wx.getStorageSync('global_reminder_subscribed') ||
                  wx.getStorageSync('sub_auth_DIET_REMINDER');
  if (isSilent && !hasAuth) {
    if (callback) callback(false);
    return;
  }
  requestSubscriptions(['DIET_REMINDER'], callback);
}

module.exports = {
  DEFAULT_TEMPLATE_ID,
  requestSubscriptions,
  requestTeamSubscriptions,
  requestNudgeSubscription,
  requestWaterSubscription,
  requestDietReminderSubscription
};
