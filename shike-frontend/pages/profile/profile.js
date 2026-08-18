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
      { key: 'GAIN_MUSCLE', label: '增肌塑形 (热量盈余)', offset: 300 },
      { key: 'PERIOD', label: '定制周期定量计划 (限时体重管理)', offset: 0 },
      { key: 'ABS', label: '定制极致腹肌计划 (体脂率管理)', offset: 0 }
    ],
    genderIndex: 0,
    genderOptions: [
      { key: 1, label: '男 ♂️' },
      { key: 2, label: '女 ♀️' }
    ],
    trainingIndex: 0,
    trainingOptions: [
      { key: 'BEGINNER', label: '小白零基础 (未规律健身或 <6个月)' },
      { key: 'NOVICE', label: '新手进阶 (规律健身 6个月~1.5年)' },
      { key: 'INTERMEDIATE', label: '中级玩家 (规律健身 1.5~3年)' },
      { key: 'ADVANCED', label: '资深老炮 (规律健身 3年以上)' }
    ],
    showGenderSheet: false,
    showTrainingSheet: false,
    isFormCollapsed: true,
    targetProtein: 0,
    targetCarbs: 0,
    targetFat: 0,
    showActivitySheet: false,
    showGoalSheet: false,
    alreadySignedIn: false,
    showPointsBillModal: false,
    pointsRecords: [],
    customGoalType: 'PERIOD',
    periodDirection: 'LOSE', // 'LOSE' 减脂, 'GAIN' 增重
    customGoalDays: '',
    customGoalWeight: '',
    customGoalWeightDisplay: '',
    currentBodyFat: '',
    customGoalWarning: '',
    features: { user_feedback: true }
  },

  onLoad() {
    if (wx.showShareMenu) {
      wx.showShareMenu({
        withShareTicket: true,
        menus: ['shareAppMessage', 'shareTimeline']
      });
    }
    this.loginAndFetchProfile();
    this.fetchFeatureToggles();
  },

  onShow() {
    this.loginAndFetchProfile();
    this.fetchFeatureToggles();
  },

  fetchFeatureToggles() {
    wx.request({
      url: `${app.globalData.baseUrl}/config/features?env=release`,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200 && res.data.data) {
          this.setData({ features: res.data.data });
        }
      }
    });
  },

  onShareAppMessage() {
    this._rewardSharePoints('SHARE_FRIEND');
    return {
      title: '🥑 咔嚓算卡 - AI 智能拍照算卡与健康膳食助手',
      path: '/pages/index/index'
    };
  },

  onShareTimeline() {
    this._rewardSharePoints('SHARE_TIMELINE');
    return {
      title: '🥑 咔嚓算卡 - 拍照即可自动识别千种食物与卡路里，开启自律生活！',
      query: ''
    };
  },

  _rewardSharePoints(shareType) {
    const user = app.globalData.userInfo;
    if (!user || !user.id) return;
    wx.request({
      url: `${app.globalData.baseUrl}/user/share-reward?userId=${user.id}&shareType=${shareType}`,
      method: 'POST',
      success: (res) => {
        if (res.data && res.data.code === 200 && res.data.data) {
          const { rewarded, points } = res.data.data;
          if (rewarded) {
            wx.showToast({ title: '分享成功 +10积分', icon: 'none', duration: 2000 });
            if (app.globalData.userInfo) {
              app.globalData.userInfo.points = points;
            }
            if (this.data.userInfo) {
              this.setData({ 'userInfo.points': points });
            }
          }
        }
      }
    });
  },

  loginAndFetchProfile() {
    wx.showLoading({ title: '正在同步档案...' });
    
    app.login((user) => {
      wx.hideLoading();
      const isAdmin = user.id === 2 || user.id === 1 || (user.openid && user.openid.includes('oCWo85VjB67HWAxXvsZBSb-NouMg'));
      this.setData({
        userId: user.id,
        nickname: user.nickname || '微信用户',
        avatarUrl: user.avatarUrl || '/images/profile.png',
        points: user.points || 0,
        isAdmin
      });

      this.fetchPointsRecords();

      // If user already has profile stats, load them
      if (user.age) {
        const activityIdx = this.data.activityOptions.findIndex(o => o.key === user.activityLevel);
        const goalIdx = this.data.goalOptions.findIndex(o => o.key === user.goal);
        const trainingIdx = this.data.trainingOptions.findIndex(o => o.key === user.trainingLevel);
        const genderIdx = user.gender === 2 ? 1 : 0;
        
        let periodDirection = 'LOSE';
        let customGoalWeightDisplay = '';
        if (user.customGoalWeight != null && user.customGoalWeight !== '') {
          const rawW = parseFloat(user.customGoalWeight);
          if (rawW < 0) {
            periodDirection = 'LOSE';
            customGoalWeightDisplay = Math.abs(rawW).toString();
          } else {
            periodDirection = 'GAIN';
            customGoalWeightDisplay = rawW.toString();
          }
        }

        this.setData({
          age: user.age,
          height: user.height,
          weight: user.weight,
          activityIndex: activityIdx !== -1 ? activityIdx : 0,
          goalIndex: goalIdx !== -1 ? goalIdx : 1,
          trainingIndex: trainingIdx !== -1 ? trainingIdx : 0,
          genderIndex: genderIdx,
          bmr: user.bmr || 0,
          tdee: user.tdee || 0,
          targetCal: user.targetCalories || 0,
          customGoalType: user.customGoalType || 'PERIOD',
          periodDirection,
          customGoalDays: user.customGoalDays || '',
          customGoalWeight: user.customGoalWeight || '',
          customGoalWeightDisplay,
          currentBodyFat: user.currentBodyFat || ''
        }, () => {
          this.calculateNutrientsTargets();
          this.recalculateMetabolism();
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

  showTrainingModal() {
    this.setData({ showTrainingSheet: true });
  },

  hideTrainingModal() {
    this.setData({ showTrainingSheet: false });
  },

  selectTraining(e) {
    const index = parseInt(e.currentTarget.dataset.index);
    this.setData({
      trainingIndex: index,
      showTrainingSheet: false
    });
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

  openAdminDashboard() {
    wx.showModal({
      title: '👑 开发者运营后台',
      content: '后台系统已就绪！您可以通过手机/电脑浏览器直接打开：\n\nhttp://117.72.61.18:8081/api/v1/admin/index.html\n\n是否复制访问链接？',
      confirmText: '复制链接',
      confirmColor: '#10B981',
      success: (res) => {
        if (res.confirm) {
          wx.setClipboardData({
            data: 'http://117.72.61.18:8081/api/v1/admin/index.html',
            success: () => {
              wx.showToast({ title: '链接已复制到剪贴板', icon: 'success' });
            }
          });
        }
      }
    });
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

  onSelectPeriodDirection(e) {
    const periodDirection = e.currentTarget.dataset.dir || 'LOSE';
    this.setData({ periodDirection }, () => {
      this.recalculateMetabolism();
      this.saveProfileSilent();
    });
  },

  onCustomGoalDaysInput(e) {
    this.setData({ customGoalDays: parseInt(e.detail.value) || '' });
    this.recalculateMetabolism();
  },

  onCustomGoalWeightInput(e) {
    const val = parseFloat(e.detail.value);
    const customGoalWeightDisplay = isNaN(val) ? '' : val.toString();
    const sign = this.data.periodDirection === 'LOSE' ? -1 : 1;
    const customGoalWeight = isNaN(val) ? '' : (sign * Math.abs(val));
    
    this.setData({
      customGoalWeightDisplay,
      customGoalWeight
    });
    this.recalculateMetabolism();
  },

  onCurrentBodyFatInput(e) {
    this.setData({ currentBodyFat: parseFloat(e.detail.value) || '' });
    this.recalculateMetabolism();
  },

  recalculateMetabolism() {
    const { age, height, weight, activityIndex, activityOptions, goalIndex, goalOptions, genderIndex, genderOptions, periodDirection, customGoalWeightDisplay } = this.data;
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
    
    let targetCal = tdee;
    let warning = '';
    const goalKey = goalOptions[goalIndex].key;

    if (goalKey === 'PERIOD') {
      const days = parseInt(this.data.customGoalDays);
      const rawWeight = parseFloat(customGoalWeightDisplay);
      const weightChange = (periodDirection === 'LOSE' ? -1 : 1) * Math.abs(rawWeight || 0);

      if (days && days > 0 && rawWeight > 0) {
        let calculatedOffset = (weightChange * 3850.0) / days;
        const ratePerWeek = Math.abs(weightChange) / days * 7.0;

        if (weightChange < 0) { // Loss
          if (ratePerWeek > 2.0) {
            calculatedOffset = -1000.0;
            warning = '⚠️ 提示：您填写的计划速度超出了健康减重建议范围（每周最多减 2 斤）。已自动为您调整为健康安全上限（每日赤字 1000 kcal），以防代谢受损。';
          }
        } else { // Gain
          if (ratePerWeek > 1.0) {
            calculatedOffset = 500.0;
            warning = '⚠️ 提示：您填写的计划速度超出了健康增肌建议范围（每周最多增 1 斤）。已自动为您调整为健康安全上限（每日盈余 500 kcal）。';
          }
        }
        targetCal = Math.round(tdee + calculatedOffset);
      }
    } else if (goalKey === 'ABS') {
      const days = parseInt(this.data.customGoalDays);
      if (days && days > 0) {
        const currentFatRate = parseFloat(this.data.currentBodyFat) || (gender === 2 ? 26.0 : 20.0);
        const targetFatRate = gender === 2 ? 18.0 : 12.0;

        const fatMass = weight * (currentFatRate / 100.0);
        const leanMass = weight - fatMass;
        const targetWeight = leanMass / (1.0 - (targetFatRate / 100.0));
        
        const fatLossNeeded = weight - targetWeight;
        if (fatLossNeeded > 0) {
          const fatLossNeededInJin = fatLossNeeded * 2.0;
          let calculatedOffset = (-fatLossNeededInJin * 3850.0) / days;
          
          const ratePerWeek = fatLossNeededInJin / days * 7.0;
          if (ratePerWeek > 2.0) {
            calculatedOffset = -1000.0;
            warning = '⚠️ 提示：为了在 ' + days + ' 天内露出腹肌，需要减掉 ' + fatLossNeededInJin.toFixed(1) + ' 斤脂肪。这超出了健康减重建议范围（每周最多减 2 斤）。已自动调整为健康安全上限（每日赤字 1000 kcal）。';
          } else {
            warning = '💡 分析：为了露出清晰腹肌，您需要减少约 ' + fatLossNeededInJin.toFixed(1) + ' 斤纯脂肪，计划每天保持 -' + Math.round(Math.abs(calculatedOffset)) + ' kcal 的热量赤字。';
          }
          targetCal = Math.round(tdee + calculatedOffset);
        }
      }
    } else {
      const offset = goalOptions[goalIndex].offset;
      targetCal = Math.round(tdee + offset);
    }

    // 科学安全双轨保底 (国际医学绝对底线: 男1500, 女1200; 相对底线: BMR * 85%)
    const absoluteFloor = gender === 2 ? 1200 : 1500;
    const safetyFloor = Math.max(Math.round(bmr * 0.85), absoluteFloor);

    if (targetCal < safetyFloor) {
      targetCal = safetyFloor;
      if (!warning) {
        warning = '🛡️ 科学守护：计算出的摄入热量已触及安全代谢保底线（' + safetyFloor + ' kcal），已自动锁定底线以保护脏器与肌肉健康。';
      } else {
        warning += ' 此外，建议摄入量已触及安全代谢保底线（' + safetyFloor + ' kcal）。';
      }
    }

    this.setData({
      bmr,
      tdee,
      targetCal,
      customGoalWarning: warning
    }, () => {
      this.calculateNutrientsTargets();
    });
  },

  calculateNutrientsTargets() {
    const { targetCal, weight, goalIndex, goalOptions } = this.data;
    if (!targetCal) return;

    const userWeight = weight || 65;
    const goalKey = (goalOptions && goalOptions[goalIndex]) ? goalOptions[goalIndex].key : 'MAINTAIN';

    // 1. 动态蛋白质推荐系数 (g/kg 体重)
    let proteinMultiplier = 1.4; // 默认保持/轻度运动基准 1.4g/kg
    if (goalKey === 'GAIN_MUSCLE' || goalKey === 'ABS') {
      proteinMultiplier = 2.0; // 增肌/腹肌塑形 2.0g/kg
    } else if (goalKey === 'LOSE_WEIGHT' || goalKey === 'PERIOD' || goalKey === 'CUSTOM') {
      proteinMultiplier = 1.8; // 减脂赤字保护 1.8g/kg
    }

    let proteinGrams = userWeight * proteinMultiplier;
    let proteinCal = proteinGrams * 4;

    // 安全区间约束 (蛋白质热量占总目标热量 15% <= proteinCal <= 35%)
    const minProteinCal = targetCal * 0.15;
    const maxProteinCal = targetCal * 0.35;
    if (proteinCal < minProteinCal) proteinCal = minProteinCal;
    if (proteinCal > maxProteinCal) proteinCal = maxProteinCal;
    const targetProtein = Math.round(proteinCal / 4);

    // 2. 动态脂肪推荐量 (基准占比 25%，保底最低 0.6g/kg 保证内分泌健康)
    let fatCal = targetCal * 0.25;
    const minFatCal = userWeight * 0.6 * 9;
    if (fatCal < minFatCal) fatCal = minFatCal;
    const targetFat = Math.round(fatCal / 9);

    // 3. 碳水化合物充填剩余热量缺口
    let carbsCal = targetCal - (targetProtein * 4) - (targetFat * 9);
    if (carbsCal < targetCal * 0.15) carbsCal = targetCal * 0.15;
    const targetCarbs = Math.round(carbsCal / 4);

    this.setData({
      targetProtein,
      targetCarbs,
      targetFat
    });
  },

  saveProfileSilent(newAvatarUrl, newNickname) {
    if (!this.data.userId) return;

    const activityLevel = this.data.activityOptions[this.data.activityIndex].key;
    const goal = this.data.goalOptions[this.data.goalIndex].key;
    const gender = this.data.genderOptions[this.data.genderIndex].key;
    const trainingLevel = this.data.trainingOptions[this.data.trainingIndex].key;

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
        trainingLevel: trainingLevel,
        nickname: newNickname !== null ? newNickname : this.data.nickname,
        avatarUrl: newAvatarUrl !== null ? newAvatarUrl : this.data.avatarUrl,
        customGoalType: goal === 'PERIOD' ? 'PERIOD' : (goal === 'ABS' ? 'ABS' : null),
        customGoalDays: (goal === 'PERIOD' || goal === 'ABS') ? (parseInt(this.data.customGoalDays) || null) : null,
        customGoalWeight: goal === 'PERIOD' ? (parseFloat(this.data.customGoalWeight) || null) : null,
        currentBodyFat: goal === 'ABS' ? (parseFloat(this.data.currentBodyFat) || null) : null
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
    const trainingLevel = this.data.trainingOptions[this.data.trainingIndex].key;

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
        trainingLevel: trainingLevel,
        nickname: this.data.nickname,
        avatarUrl: this.data.avatarUrl,
        customGoalType: goal === 'PERIOD' ? 'PERIOD' : (goal === 'ABS' ? 'ABS' : null),
        customGoalDays: (goal === 'PERIOD' || goal === 'ABS') ? (parseInt(this.data.customGoalDays) || null) : null,
        customGoalWeight: goal === 'PERIOD' ? (parseFloat(this.data.customGoalWeight) || null) : null,
        currentBodyFat: goal === 'ABS' ? (parseFloat(this.data.currentBodyFat) || null) : null
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
  },

  onGoToHomePlan() {
    wx.switchTab({
      url: '/pages/index/index',
      success: () => {
        setTimeout(() => {
          const pages = getCurrentPages();
          const indexPage = pages.find(p => p.route === 'pages/index/index');
          if (indexPage) {
            indexPage.openPlanModal();
          }
        }, 300);
      }
    });
  },

  openContactModal() {
    wx.switchTab({
      url: '/pages/index/index',
      success: () => {
        const page = getCurrentPages().pop();
        if (page && page.openContactModal) {
          page.openContactModal();
        }
      }
    });
  }
});
