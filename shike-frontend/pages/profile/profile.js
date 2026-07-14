const app = getApp();

Page({
  data: {
    nickname: '微信用户',
    userId: null,
    avatarUrl: '/images/profile.png',
    points: 0,
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
    ],
    genderIndex: 0,
    genderOptions: [
      { key: 1, label: '男 ♂️' },
      { key: 2, label: '女 ♀️' }
    ],
    showGenderSheet: false,
    isFormCollapsed: false,
    targetProtein: 0,
    targetCarbs: 0,
    targetFat: 0,
    showActivitySheet: false,
    showGoalSheet: false,
    alreadySignedIn: false,
    showPointsBillModal: false,
    pointsRecords: []
  },

  onLoad() {
    this.loginAndFetchProfile();
  },

  loginAndFetchProfile() {
    wx.showLoading({ title: '正在同步档案...' });
    
    app.login((user) => {
      wx.hideLoading();
      this.setData({
        userId: user.id,
        nickname: user.nickname || '微信用户',
        avatarUrl: user.avatarUrl || '/images/profile.png',
        points: user.points || 0
      });

      this.fetchPointsRecords();

      // If user already has profile stats, load them
      if (user.age) {
        const activityIdx = this.data.activityOptions.findIndex(o => o.key === user.activityLevel);
        const goalIdx = this.data.goalOptions.findIndex(o => o.key === user.goal);
        const genderIdx = user.gender === 2 ? 1 : 0;
        
        this.setData({
          age: user.age,
          height: user.height,
          weight: user.weight,
          activityIndex: activityIdx !== -1 ? activityIdx : 0,
          goalIndex: goalIdx !== -1 ? goalIdx : 1,
          genderIndex: genderIdx,
          bmr: user.bmr || 0,
          tdee: user.tdee || 0,
          targetCal: user.targetCalories || 0
        }, () => {
          this.calculateNutrientsTargets();
        });
      } else {
        // First time calculate
        this.recalculateMetabolism();
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

  showActivityModal() {
    this.setData({ showActivitySheet: true });
  },

  hideActivityModal() {
    this.setData({ showActivitySheet: false });
  },

  selectActivity(e) {
    const index = parseInt(e.currentTarget.dataset.index);
    this.setData({
      activityIndex: index,
      showActivitySheet: false
    });
    this.recalculateMetabolism();
    this.saveProfileSilent(null, null);
  },

  showGoalModal() {
    this.setData({ showGoalSheet: true });
  },

  hideGoalModal() {
    this.setData({ showGoalSheet: false });
  },

  selectGoal(e) {
    const index = parseInt(e.currentTarget.dataset.index);
    this.setData({
      goalIndex: index,
      showGoalSheet: false
    });
    this.recalculateMetabolism();
    this.saveProfileSilent(null, null);
  },

  showGenderModal() {
    this.setData({ showGenderSheet: true });
  },

  hideGenderModal() {
    this.setData({ showGenderSheet: false });
  },

  selectGender(e) {
    const index = parseInt(e.currentTarget.dataset.index);
    this.setData({
      genderIndex: index,
      showGenderSheet: false
    });
    this.recalculateMetabolism();
    this.saveProfileSilent(null, null);
  },

  toggleFormCollapse() {
    this.setData({
      isFormCollapsed: !this.data.isFormCollapsed
    });
  },

  onFieldBlur() {
    this.saveProfileSilent(null, null);
  },

  preventBubble() {
    // Prevent scrolling behind modal
  },

  onChooseAvatar(e) {
    const avatarUrl = e.detail.avatarUrl;
    this.setData({ avatarUrl });
    
    // Upload avatar to backend
    wx.showLoading({ title: '正在上传头像...' });
    wx.uploadFile({
      url: `${app.globalData.baseUrl}/user/avatar/upload`,
      filePath: avatarUrl,
      name: 'file',
      success: (res) => {
        wx.hideLoading();
        try {
          const result = JSON.parse(res.data);
          if (result.code === 200) {
            this.setData({
              avatarUrl: result.data
            });
            // 同步更新全局用户信息，切换到首页时能立即显示新头像
            if (app.globalData.userInfo) {
              app.globalData.userInfo.avatarUrl = result.data;
            }
            wx.showToast({ title: '头像上传成功', icon: 'success' });
            
            // 自动保存至数据库，避免用户未点击保存按钮导致头像丢失
            this.saveProfileSilent(result.data, null);
          } else {
            wx.showToast({ title: result.message || '上传失败', icon: 'none' });
          }
        } catch (err) {
          wx.showToast({ title: '解析头像错误', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络连接失败', icon: 'none' });
      }
    });
  },

  onNicknameInput(e) {
    this.setData({ nickname: e.detail.value });
  },

  onNicknameBlur(e) {
    const nickname = e.detail.value;
    this.setData({ nickname });
    
    // 自动保存至数据库，避免用户未点击保存按钮导致昵称丢失
    this.saveProfileSilent(null, nickname);
  },

  recalculateMetabolism() {
    const { age, height, weight, activityIndex, activityOptions, goalIndex, goalOptions, genderIndex, genderOptions } = this.data;
    if (!age || !height || !weight) return;

    const gender = genderOptions[genderIndex].key;
    // Mifflin-St Jeor formula
    // Male: BMR = 10 * w + 6.25 * h - 5 * age + 5
    // Female: BMR = 10 * w + 6.25 * h - 5 * age - 161
    const bmr = gender === 2
      ? Math.round(10 * weight + 6.25 * height - 5 * age - 161)
      : Math.round(10 * weight + 6.25 * height - 5 * age + 5);

    const activityFactor = activityOptions[activityIndex].value;
    const tdee = Math.round(bmr * activityFactor);
    const offset = goalOptions[goalIndex].offset;
    const targetCal = Math.round(tdee + offset);

    this.setData({
      bmr,
      tdee,
      targetCal
    }, () => {
      this.calculateNutrientsTargets();
    });
  },

  calculateNutrientsTargets() {
    const targetCal = this.data.targetCal;
    if (!targetCal) return;

    this.setData({
      targetProtein: Math.round(targetCal * 0.2 / 4),
      targetCarbs: Math.round(targetCal * 0.5 / 4),
      targetFat: Math.round(targetCal * 0.3 / 9)
    });
  },

  saveProfileSilent(newAvatarUrl, newNickname) {
    if (!this.data.userId) return;

    const activityLevel = this.data.activityOptions[this.data.activityIndex].key;
    const goal = this.data.goalOptions[this.data.goalIndex].key;
    const gender = this.data.genderOptions[this.data.genderIndex].key;

    wx.request({
      url: `${app.globalData.baseUrl}/user/profile`,
      method: 'POST',
      data: {
        userId: this.data.userId,
        age: this.data.age,
        gender: gender,
        height: this.data.height,
        weight: this.data.weight,
        activityLevel: activityLevel,
        goal: goal,
        nickname: newNickname !== null ? newNickname : this.data.nickname,
        avatarUrl: newAvatarUrl !== null ? newAvatarUrl : this.data.avatarUrl
      },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const user = res.data.data;
          app.globalData.userInfo = user;
          console.log('Profile auto-saved silently:', user);
        }
      }
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
    const gender = this.data.genderOptions[this.data.genderIndex].key;

    wx.request({
      url: `${app.globalData.baseUrl}/user/profile`,
      method: 'POST',
      data: {
        userId: this.data.userId,
        age: this.data.age,
        gender: gender,
        height: this.data.height,
        weight: this.data.weight,
        activityLevel: activityLevel,
        goal: goal,
        nickname: this.data.nickname,
        avatarUrl: this.data.avatarUrl
      },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const user = res.data.data;
          app.globalData.userInfo = user;
          
          this.setData({
            bmr: user.bmr,
            tdee: user.tdee,
            targetCal: user.targetCalories,
            nickname: user.nickname,
            avatarUrl: user.avatarUrl
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
  },

  fetchPointsRecords() {
    if (!this.data.userId) return;
    wx.request({
      url: `${app.globalData.baseUrl}/user/${this.data.userId}/points-records`,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const records = res.data.data || [];
          
          const formatted = records.map(r => {
            const rawDate = r.createdAt || '';
            const formattedDate = rawDate.replace('T', ' ').substring(0, 16);
            return {
              ...r,
              formattedDate: formattedDate
            };
          });

          const today = new Date();
          const y = today.getFullYear();
          const m = String(today.getMonth() + 1).padStart(2, '0');
          const d = String(today.getDate()).padStart(2, '0');
          const todayStr = `${y}-${m}-${d}`;
          
          const signedInToday = formatted.some(r => r.type === 'SIGN_IN' && r.formattedDate.startsWith(todayStr));

          this.setData({
            pointsRecords: formatted,
            alreadySignedIn: signedInToday
          });
        }
      }
    });
  },

  onSignIn() {
    if (this.data.alreadySignedIn) return;
    wx.showLoading({ title: '签到中...' });
    wx.request({
      url: `${app.globalData.baseUrl}/user/sign-in`,
      method: 'POST',
      data: {
        userId: this.data.userId
      },
      header: {
        'content-type': 'application/x-www-form-urlencoded'
      },
      success: (res) => {
        wx.hideLoading();
        if (res.data && res.data.code === 200) {
          wx.showToast({ title: '签到成功 +20 积分！', icon: 'success' });
          const updatedUser = res.data.data;
          this.setData({
            points: updatedUser.points,
            alreadySignedIn: true
          });
          if (app.globalData.userInfo) {
            app.globalData.userInfo.points = updatedUser.points;
          }
          this.fetchPointsRecords();
        } else {
          wx.showToast({ title: res.data.message || '签到失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络连接失败', icon: 'none' });
      }
    });
  },

  showPointsBill() {
    this.setData({ showPointsBillModal: true });
    this.fetchPointsRecords();
  },

  hidePointsBill() {
    this.setData({ showPointsBillModal: false });
  },

  onTapHistory() {
    wx.navigateTo({
      url: '/pages/history/history'
    });
  }
})
