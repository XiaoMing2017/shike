const app = getApp();

Page({
  data: {
    nickname: '微信用户',
    userId: null,
    avatarUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100',
    age: 25,
    height: 175.5,
    weight: 70.0,
    bmr: 0,
    tdee: 0,
    targetCal: 0,
    activityIndex: 0,
    activityOptions: [
      { key: 'SEDENTARY', label: '久坐不运动 (办公室族)', value: 1.2 },
      { key: 'LIGHT', label: '轻度运动 (每周运动1-3次)', value: 1.375 },
      { key: 'MODERATE', label: '中度运动 (每周运动3-5次)', value: 1.55 },
      { key: 'ACTIVE', label: '重度运动 (每天高强度运动)', value: 1.725 }
    ],
    goalIndex: 1,
    goalOptions: [
      { key: 'LOSE_WEIGHT', label: '科学减脂 (热量赤字)', offset: -500 },
      { key: 'MAINTAIN', label: '保持体重 (热量收支平衡)', offset: 0 },
      { key: 'GAIN_MUSCLE', label: '增肌塑形 (热量盈余)', offset: 300 }
    ]
  },

  onLoad() {
    this.loginAndFetchProfile();
  },

  loginAndFetchProfile() {
    wx.showLoading({ title: '正在同步档案...' });
    
    // For MVP/Local testing, we use a mock OpenID to login.
    // In production, you would call wx.login to get a code and swap it for an openid.
    const mockOpenid = 'mock_user_openid_123';
    
    wx.request({
      url: `${app.globalData.baseUrl}/user/login`,
      method: 'POST',
      data: { openid: mockOpenid },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const user = res.data.data;
          app.globalData.userInfo = user;
          
          this.setData({
            userId: user.id,
            nickname: user.nickname || '微信用户',
            avatarUrl: user.avatarUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100'
          });

          // If user already has profile stats, load them
          if (user.age) {
            const activityIdx = this.data.activityOptions.findIndex(o => o.key === user.activityLevel);
            const goalIdx = this.data.goalOptions.findIndex(o => o.key === user.goal);
            
            this.setData({
              age: user.age,
              height: user.height,
              weight: user.weight,
              activityIndex: activityIdx !== -1 ? activityIdx : 0,
              goalIndex: goalIdx !== -1 ? goalIdx : 1,
              bmr: user.bmr || 0,
              tdee: user.tdee || 0,
              targetCal: user.targetCalories || 0
            });
          } else {
            // First time calculate
            this.recalculateMetabolism();
          }
        } else {
          wx.showToast({ title: '登录同步失败', icon: 'error' });
        }
      },
      fail: () => {
        wx.showToast({ title: '网络连接失败', icon: 'error' });
      },
      complete: () => {
        wx.hideLoading();
      }
    });
  },

  onAgeInput(e) {
    this.setData({ age: parseInt(e.detail.value) || 0 });
    this.recalculateMetabolism();
  },

  onHeightInput(e) {
    this.setData({ height: parseFloat(e.detail.value) || 0 });
    this.recalculateMetabolism();
  },

  onWeightInput(e) {
    this.setData({ weight: parseFloat(e.detail.value) || 0 });
    this.recalculateMetabolism();
  },

  onActivityChange(e) {
    this.setData({ activityIndex: parseInt(e.detail.value) });
    this.recalculateMetabolism();
  },

  onGoalChange(e) {
    this.setData({ goalIndex: parseInt(e.detail.value) });
    this.recalculateMetabolism();
  },

  recalculateMetabolism() {
    const { age, height, weight, activityIndex, activityOptions, goalIndex, goalOptions } = this.data;
    if (!age || !height || !weight) return;

    // Male BMR Mifflin-St Jeor formula
    const bmr = Math.round(10 * weight + 6.25 * height - 5 * age + 5);
    const activityFactor = activityOptions[activityIndex].value;
    const tdee = Math.round(bmr * activityFactor);
    const offset = goalOptions[goalIndex].offset;
    const targetCal = Math.round(tdee + offset);

    this.setData({
      bmr,
      tdee,
      targetCal
    });
  },

  onSaveProfile() {
    if (!this.data.userId) {
      wx.showToast({ title: '用户未登录', icon: 'error' });
      return;
    }

    wx.showLoading({ title: '正在保存档案...' });
    
    const activityLevel = this.data.activityOptions[this.data.activityIndex].key;
    const goal = this.data.goalOptions[this.data.goalIndex].key;

    wx.request({
      url: `${app.globalData.baseUrl}/user/profile`,
      method: 'POST',
      data: {
        userId: this.data.userId,
        age: this.data.age,
        gender: 1, // Male by default
        height: this.data.height,
        weight: this.data.weight,
        activityLevel: activityLevel,
        goal: goal
      },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const user = res.data.data;
          app.globalData.userInfo = user;
          
          this.setData({
            bmr: user.bmr,
            tdee: user.tdee,
            targetCal: user.targetCalories
          });

          wx.showToast({
            title: '档案更新成功',
            icon: 'success'
          });
        } else {
          wx.showToast({ title: '保存失败', icon: 'error' });
        }
      },
      fail: () => {
        wx.showToast({ title: '网络连接失败', icon: 'error' });
      },
      complete: () => {
        wx.hideLoading();
      }
    });
  }
})
