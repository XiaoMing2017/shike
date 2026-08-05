const app = getApp();

const COMMON_FOOD_DICTIONARY = [
  { name: '油条', unit: '根', standardWeight: 50, caloriesPer100g: 386.0, proteinPer100g: 6.9, fatPer100g: 17.6, carbsPer100g: 51.0 },
  { name: '馒头', unit: '个', standardWeight: 100, caloriesPer100g: 223.0, proteinPer100g: 7.0, fatPer100g: 1.1, carbsPer100g: 47.0 },
  { name: '肉包', unit: '个', standardWeight: 80, caloriesPer100g: 227.0, proteinPer100g: 8.5, fatPer100g: 7.2, carbsPer100g: 31.8 },
  { name: '菜包', unit: '个', standardWeight: 80, caloriesPer100g: 180.0, proteinPer100g: 6.0, fatPer100g: 4.5, carbsPer100g: 29.0 },
  { name: '米饭', unit: '碗', standardWeight: 150, caloriesPer100g: 116.0, proteinPer100g: 2.6, fatPer100g: 0.3, carbsPer100g: 25.9 },
  { name: '面条', unit: '碗', standardWeight: 150, caloriesPer100g: 110.0, proteinPer100g: 4.0, fatPer100g: 0.5, carbsPer100g: 22.0 },
  { name: '水饺', unit: '个', standardWeight: 20, caloriesPer100g: 210.0, proteinPer100g: 8.0, fatPer100g: 6.5, carbsPer100g: 30.0 },
  { name: '鸡蛋', unit: '个', standardWeight: 50, caloriesPer100g: 147.0, proteinPer100g: 13.0, fatPer100g: 10.0, carbsPer100g: 1.0 },
  { name: '牛奶', unit: '盒', standardWeight: 250, caloriesPer100g: 54.0, proteinPer100g: 3.0, fatPer100g: 3.2, carbsPer100g: 4.8 },
  { name: '苹果', unit: '个', standardWeight: 200, caloriesPer100g: 52.0, proteinPer100g: 0.2, fatPer100g: 0.2, carbsPer100g: 13.7 },
  { name: '香蕉', unit: '根', standardWeight: 120, caloriesPer100g: 93.0, proteinPer100g: 1.1, fatPer100g: 0.2, carbsPer100g: 22.0 },
  { name: '鸡胸肉', unit: '块', standardWeight: 150, caloriesPer100g: 133.0, proteinPer100g: 24.6, fatPer100g: 1.9, carbsPer100g: 2.5 }
];

Page({
  data: {
    isWaterSubscribed: false,
    waterSubQuota: 0,
    currentDateStr: '',
    avatarUrl: '/images/profile.png',
    remainingCal: 2000,
    targetCal: 2000,
    consumedCal: 0,
    isOverLimit: false,
    ringLabel: '剩余',
    ringColor: '#2DD4BF, #10B981', // Emerald Gradient
    progressPercent: 100,
    nutrients: {
      carbs: 0,
      targetCarbs: 250,
      carbsPercent: 50,
      protein: 0,
      targetProtein: 100,
      proteinPercent: 20,
      fat: 0,
      targetFat: 65,
      fatPercent: 30
    },
    meals: [
      {
        type: 'BREAKFAST',
        name: '早餐',
        recorded: false,
        desc: '',
        calories: 0,
        time: '8:30 AM',
        image: 'https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=200&auto=format&fit=crop'
      },
      {
        type: 'LUNCH',
        name: '午餐',
        recorded: false,
        desc: '',
        calories: 0,
        time: '12:45 PM',
        image: 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=200&auto=format&fit=crop'
      },
      {
        type: 'DINNER',
        name: '晚餐',
        recorded: false,
        desc: '',
        calories: 0,
        time: '7:15 PM',
        image: 'https://images.unsplash.com/photo-1544025162-d76694265947?w=200&auto=format&fit=crop'
      },
      {
        type: 'SNACK',
        name: '加餐',
        recorded: false,
        desc: '',
        calories: 0,
        time: '3:30 PM',
        image: 'https://images.unsplash.com/photo-1619546813926-a78fa6372cd2?w=200&auto=format&fit=crop'
      }
    ],
    viewModeIndex: 0,
    viewModeOptions: ['日', '周'],
    showViewModeSheet: false,
    showMealOptionSheet: false,
    showOilOptionSheet: false,
    currentMealType: '',
    currentMealName: '',
    tempParsedFoodItems: null,
    // AI 识别结果弹窗
    showAiResultModal: false,
    aiResultSuccess: true,
    aiResultFoodText: '',
    aiResultCalories: 0,
    aiResultProtein: 0,
    aiResultFat: 0,
    aiResultCarbs: 0,
    aiResultMessage: '',
    mealHint: '',
    quickTags: ['肉包', '馒头', '菜包', '水饺', '面条', '米饭'],
    aiFoodItems: [],
    helperFoods: [
      { name: '米饭', unit: '碗' },
      { name: '油条', unit: '根' },
      { name: '鸡蛋', unit: '个' },
      { name: '馒头', unit: '个' },
      { name: '牛奶', unit: '盒' },
      { name: '苹果', unit: '个' }
    ],
    // 饮水数据
    showWaterSheet: false,
    waterAmount: 0,
    waterTarget: 2000,
    waterPercent: 0,
    waterFillHeight: 0,
    fabX: 300,
    fabY: 500,
    // 智能营养平衡诊断卡片 & 弹窗
    nutritionInsights: [],
    showNutritionModal: false,
    // 📸 晒餐海报弹窗
    showPosterModal: false,
    activeTemplate: 'morandi',
    tempPosterPath: '',
    posterFoodImg: '', // 用户临时选择的食物照片 (仅本地 tempFilePath，不上传服务器)
    // 🎛️ 线上动态功能开关配置
    features: { ai_plan: false, diet_diagnosis: true, poster_share: true, team_challenge: true, water_log: true },
    // 🎯 专属 AI 运动与饮食计划
    showPlanModal: false,
    planData: null,
    activeDietPlan: null,
    planActiveTab: 'workout',
    planDayIndex: 0,
    planLoading: false,
    planStatus: { hasPlan: false, isFirstTime: true, userPoints: 0 },
    // 个人信息完善引导横幅
    showProfileGuide: false,
    // 运动消耗数据
    exercises: [],
    exerciseCal: 0,
    showExerciseModal: false,
    showExerciseTypeSheet: false,
    exerciseDuration: '',
    exerciseCalories: '',
    exerciseTypeIndex: 0,
    exerciseTypeOptions: [
      { name: '请选择运动类型', met: 4.0 },
      { name: '跑步 🏃', met: 8.0 },
      { name: '慢跑 🏃‍♂️', met: 7.0 },
      { name: '快走 🚶‍♂️', met: 4.5 },
      { name: '散步 🚶', met: 3.0 },
      { name: '动感单车 🚲', met: 6.0 },
      { name: '游泳 🏊', met: 7.0 },
      { name: '力量训练 💪', met: 5.0 },
      { name: '瑜伽/普拉提 🧘', met: 2.5 },
      { name: 'HIIT/有氧操 ⚡', met: 8.0 },
      { name: '篮球/足球/球类 🏀', met: 6.0 }
    ],
    showNewFeatureModal: false,
    isDiagnosing: false,
    aiExpertComment: ''
  },

  onLoad(options) {
    // 检查是否显示新功能上线重磅引导弹窗 (每个版本仅对用户显示1次)
    try {
      const hasSeenModal = wx.getStorageSync('has_seen_v2_new_feature_modal');
      if (!hasSeenModal) {
        setTimeout(() => {
          this.setData({ showNewFeatureModal: true });
        }, 800);
      }
    } catch (e) {
      console.error('Error reading modal storage', e);
    }
    // 动态计算悬浮饮水气泡的初始位置 (右边 34rpx，距离底部 186rpx)
    try {
      const sys = wx.getSystemInfoSync();
      const screenWidth = sys.windowWidth;
      const screenHeight = sys.windowHeight;
      const fabSize = 46; // 92rpx 对应 46px
      this.setData({
        fabX: screenWidth - fabSize - 17, // 右边 17px
        fabY: screenHeight - fabSize - 100 // 底部 100px (安全避开 TabBar)
      });
    } catch (e) {
      console.error('Failed to calculate FAB position', e);
    }

    if (wx.showShareMenu) {
      wx.showShareMenu({
        withShareTicket: true,
        menus: ['shareAppMessage', 'shareTimeline']
      });
    }

    this.fetchFeatureToggles();

    if (options && (options.inviteCode || options.scene)) {
      let inviteCode = options.inviteCode;
      if (options.scene) {
        const scene = decodeURIComponent(options.scene);
        inviteCode = scene;
        if (scene.indexOf('code=') > -1) {
          inviteCode = scene.split('code=')[1];
        }
      }
      app.globalData.pendingInviteCode = inviteCode;
      wx.showToast({
        title: '已收到小队邀请，请切换到“减脂对赌”查看',
        icon: 'none',
        duration: 3500
      });
    }
  },

  closeNewFeatureModal() {
    this.setData({ showNewFeatureModal: false });
    try {
      const user = (app.globalData && app.globalData.userInfo) || wx.getStorageSync('userInfo');
      const isUser2 = user && (user.id == 2 || user.id == '2');
      if (!isUser2) {
        wx.setStorageSync('has_seen_v2_new_feature_modal', true);
      }
    } catch (e) {}
  },

  onLaunchAiPlanFromModal() {
    this.closeNewFeatureModal();
    this.openPlanModal();
  },

  showViewModeModal() {
    this.setData({ showViewModeSheet: true });
  },

  hideViewModeModal() {
    this.setData({ showViewModeSheet: false });
  },

  onTapAvatar() {
    wx.switchTab({
      url: '/pages/profile/profile'
    });
  },

  goToProfile() {
    wx.switchTab({
      url: '/pages/profile/profile'
    });
  },

  onTapHistory() {
    wx.navigateTo({
      url: '/pages/history/history'
    });
  },


  selectViewMode(e) {
    const index = parseInt(e.currentTarget.dataset.index);
    if (index === 1) {
      wx.showToast({
        title: '周看板即将上线，敬请期待！',
        icon: 'none',
        duration: 2000
      });
      this.setData({ showViewModeSheet: false });
      return;
    }
    this.setData({
      viewModeIndex: index,
      showViewModeSheet: false
    });
  },

  preventBubble() {
    // Prevent scrolling behind modal
  },

  onShow() {
    this.updateDateDisplay();
    this.checkUserAndLoadData();
    const subscribed = !!wx.getStorageSync('water_wx_subscribed');
    this.setData({ isWaterSubscribed: subscribed });

    // 静默自动续费订阅额度：用户曾勾选"总是允许"后，此调用无感知通过
    if (subscribed) {
      this._silentRenewWaterQuota();
    }

    // 从后端拉取真实剩余额度
    this._loadWaterSubQuota();
  },

  updateDateDisplay() {
    const now = new Date();
    const month = now.getMonth() + 1;
    const date = now.getDate();
    const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
    const weekday = weekdays[now.getDay()];
    this.setData({
      currentDateStr: `${month}月${date}日 ${weekday}`
    });
  },

  checkUserAndLoadData() {
    app.login((user) => {
      // Always fetch fresh user profile from backend to ensure remaining calories and BMR/TDEE are up to date
      wx.request({
        url: `${app.globalData.baseUrl}/user/${user.id}`,
        method: 'GET',
        success: (res) => {
          if (res.data && res.data.code === 200) {
            const freshUser = res.data.data;
            app.globalData.userInfo = freshUser;
            this.loadUserData(freshUser);
          } else {
            this.loadUserData(user);
          }
        },
        fail: () => {
          this.loadUserData(user);
        }
      });
    });
  },

  loadUserData(user) {
    if (user && user.id) {
      this.checkLateCheckinStatus(user.id);
      this.checkPendingNudgeAlert(user.id);
      this.checkWaterReminderStatus(user.id);

      // 用户 ID 为 2 (测试开发者账号) 每次进入均自动展现弹窗；其他用户仅展现 1 次
      const isUser2 = (user.id == 2 || user.id == '2');
      const hasSeenModal = wx.getStorageSync('has_seen_v2_new_feature_modal');
      if (isUser2 || !hasSeenModal) {
        this.setData({ showNewFeatureModal: true });
      }
    }
    // 1. Set calorie targets and dynamic nutrient distribution
    const targetCal = user.targetCalories || 2000;
    const userWeight = user.weight || 65;
    const userGoal = user.goal || 'MAINTAIN';

    // 动态蛋白质推荐系数 (g/kg 体重)
    let proteinMultiplier = 1.4;
    if (userGoal === 'GAIN_MUSCLE' || userGoal === 'ABS') {
      proteinMultiplier = 2.0; // 增肌/腹肌塑形 2.0g/kg
    } else if (userGoal === 'LOSE_WEIGHT' || userGoal === 'PERIOD' || userGoal === 'CUSTOM') {
      proteinMultiplier = 1.8; // 减脂赤字保护 1.8g/kg
    }

    let proteinGrams = userWeight * proteinMultiplier;
    let proteinCal = proteinGrams * 4;

    const minProteinCal = targetCal * 0.15;
    const maxProteinCal = targetCal * 0.35;
    if (proteinCal < minProteinCal) proteinCal = minProteinCal;
    if (proteinCal > maxProteinCal) proteinCal = maxProteinCal;
    const targetProtein = Math.round(proteinCal / 4);

    let fatCal = targetCal * 0.25;
    const minFatCal = userWeight * 0.6 * 9;
    if (fatCal < minFatCal) fatCal = minFatCal;
    const targetFat = Math.round(fatCal / 9);

    let carbsCal = targetCal - (targetProtein * 4) - (targetFat * 9);
    if (carbsCal < targetCal * 0.15) carbsCal = targetCal * 0.15;
    const targetCarbs = Math.round(carbsCal / 4);

    const carbsPercent = Math.round((targetCarbs * 4 / targetCal) * 100);
    const proteinPercent = Math.round((targetProtein * 4 / targetCal) * 100);
    const fatPercent = 100 - carbsPercent - proteinPercent;

    const showProfileGuide = !user.age || user.age === 0;
    this.setData({
      targetCal,
      showProfileGuide,
      avatarUrl: user.avatarUrl || '/images/profile.png',
      'nutrients.targetCarbs': targetCarbs,
      'nutrients.targetProtein': targetProtein,
      'nutrients.targetFat': targetFat,
      'nutrients.carbsPercent': carbsPercent,
      'nutrients.proteinPercent': proteinPercent,
      'nutrients.fatPercent': fatPercent
    });

    const todayStr = this.getTodayDateString();

    // 2. Fetch daily exercise records
    wx.request({
      url: `${app.globalData.baseUrl}/exercise/daily`,
      method: 'GET',
      data: {
        userId: user.id,
        date: todayStr
      },
      success: (exRes) => {
        if (exRes.data && exRes.data.code === 200) {
          const emojiMap = {
            '跑步': '🏃',
            '慢跑': '🏃‍♂️',
            '快走': '🚶‍♂️',
            '散步': '🚶',
            '动感单车': '🚲',
            '游泳': '🏊',
            '力量训练': '💪',
            '瑜伽/普拉提': '🧘',
            'HIIT/有氧操': '⚡',
            '篮球/足球/球类': '🏀'
          };
          const exercises = (exRes.data.data.records || []).map(item => ({
            ...item,
            emoji: emojiMap[item.activityName] || '🏃'
          }));
          const exerciseCal = Math.round(exRes.data.data.totalCalories || 0);
          this.setData({
            exercises,
            exerciseCal
          }, () => {
            this.fetchDietRecords(user.id, todayStr, targetCal);
          });
        } else {
          this.fetchDietRecords(user.id, todayStr, targetCal);
        }
      },
      fail: () => {
        this.fetchDietRecords(user.id, todayStr, targetCal);
      }
    });

    // 3. Fetch water data
    this.loadWaterData(user.id, todayStr);
  },

  fetchDietRecords(userId, dateStr, targetCal) {
    wx.request({
      url: `${app.globalData.baseUrl}/diet/daily`,
      method: 'GET',
      data: {
        userId: userId,
        date: dateStr
      },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const records = res.data.data;
          this.processDietRecords(records, targetCal);
        }
      }
    });
  },

  getTodayDateString() {
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const dd = String(today.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  },

  processDietRecords(records, targetCal) {
    let consumedCal = 0;
    let carbs = 0;
    let protein = 0;
    let fat = 0;

    // Reset meal recorded status
    const updatedMeals = this.data.meals.map(meal => {
      meal.recorded = false;
      meal.desc = '';
      meal.calories = 0;
      return meal;
    });

    for (const record of records) {
      consumedCal += record.totalCalories;
      carbs += record.totalCarbs || 0;
      protein += record.totalProtein || 0;
      fat += record.totalFat || 0;

      // Find the corresponding meal card
      const mealIdx = updatedMeals.findIndex(m => m.type === record.mealType);
      if (mealIdx !== -1) {
        updatedMeals[mealIdx].recorded = true;
        updatedMeals[mealIdx].calories += record.totalCalories;
        
        // Parse food names and append to description
        try {
          const foodItems = JSON.parse(record.foodItems);
          const names = foodItems.map(item => item.name).join('、');
          updatedMeals[mealIdx].desc = updatedMeals[mealIdx].desc
            ? updatedMeals[mealIdx].desc + '、' + names
            : names;
        } catch (e) {
          if (!updatedMeals[mealIdx].desc) {
            updatedMeals[mealIdx].desc = '点击查看详情';
          }
        }
      }
    }

    const consumedCalVal = Math.round(consumedCal);
    const targetCalVal = Math.round(targetCal);
    const exerciseCalVal = Math.round(this.data.exerciseCal || 0);
    
    let isOverLimit = false;
    let ringLabel = '剩余';
    let displayCal = 0;
    let progressPercent = 0;

    // Formula: netCalories = consumed (exercise is not subtracted per user request)
    const netCalories = consumedCalVal;

    if (netCalories > targetCalVal) {
      isOverLimit = true;
      ringLabel = '已超预算';
      displayCal = netCalories - targetCalVal;
      progressPercent = 100; // 100% warning track
    } else {
      isOverLimit = false;
      ringLabel = '剩余';
      displayCal = targetCalVal - netCalories;
      progressPercent = Math.round((displayCal / targetCalVal) * 100); // Standard 0-100 scale
    }

    // Calculate macronutrient ratios
    const totalGram = carbs + protein + fat;
    let carbsPercent = 50;
    let proteinPercent = 20;
    let fatPercent = 30;

    if (totalGram > 0) {
      carbsPercent = Math.round((carbs / totalGram) * 100);
      proteinPercent = Math.round((protein / totalGram) * 100);
      fatPercent = Math.max(0, 100 - carbsPercent - proteinPercent);
    }

    // 智能营养诊断核心算法计算
    const diagnosisData = {
      consumedCal: consumedCalVal,
      targetCal: targetCalVal,
      carbs: Math.round(carbs),
      targetCarbs: this.data.nutrients.targetCarbs,
      protein: Math.round(protein),
      targetProtein: this.data.nutrients.targetProtein,
      fat: Math.round(fat),
      targetFat: this.data.nutrients.targetFat,
      waterAmount: this.data.waterAmount || 0,
      waterTarget: this.data.waterTarget || 2000
    };
    const nutritionInsights = this.calculateNutritionDiagnosis(diagnosisData);

    this.setData({
      consumedCal: consumedCalVal,
      remainingCal: displayCal,
      isOverLimit,
      ringLabel,
      progressPercent,
      meals: updatedMeals,
      nutritionInsights,
      'nutrients.carbs': Math.round(carbs),
      'nutrients.carbsPercent': carbsPercent,
      'nutrients.protein': Math.round(protein),
      'nutrients.proteinPercent': proteinPercent,
      'nutrients.fat': Math.round(fat),
      'nutrients.fatPercent': fatPercent
    }, () => {
      // 渲染结束后，动态重绘 Canvas 2D 圆环
      this.drawCalorieRing(progressPercent, isOverLimit);
    });
  },

  calculateNutritionDiagnosis(data) {
    const { consumedCal, targetCal, carbs, targetCarbs, protein, targetProtein, fat, targetFat, waterAmount, waterTarget } = data;
    const insights = [];

    // 1. 无记录处理
    if (!consumedCal || consumedCal === 0) {
      insights.push({
        id: 'empty',
        type: 'info',
        badge: '💡 打卡引导',
        badgeClass: 'badge-blue',
        title: '今日尚未记录饮食',
        suggestion: '点击“早餐/午餐/晚餐”或拍摄拍照，AI 将自动分析您的三大营养素与健康建议！'
      });
      return insights;
    }

    // 2. 热量维度判断
    if (consumedCal > targetCal) {
      const overCal = Math.round(consumedCal - targetCal);
      insights.push({
        id: 'cal_over',
        type: 'warning',
        badge: '🚨 热量已超预算',
        badgeClass: 'badge-red',
        title: `今日热量超出预算 ${overCal} kcal`,
        suggestion: `目前已摄入 ${consumedCal} kcal (目标 ${targetCal} kcal)。建议增加 30 分钟有氧运动，或下半天餐饮热量减半。`
      });
    }

    // 3. 脂肪维度判断
    if (fat > targetFat) {
      const overFat = Math.round(fat - targetFat);
      insights.push({
        id: 'fat_over',
        type: 'warning',
        badge: '🥑 脂肪摄入偏高',
        badgeClass: 'badge-red',
        title: `脂肪超出目标 ${overFat}g (${Math.round((fat / targetFat) * 100)}%)`,
        suggestion: `脂肪摄入超标容易造成脂肪堆积。建议后半天餐食选择水煮/蒸煮，避免煎炸、重油酱料及坚果过量。`
      });
    }

    // 4. 蛋白质维度判断
    if (protein < targetProtein * 0.75) {
      const needProtein = Math.round(targetProtein - protein);
      const foodTips = needProtein >= 30 
        ? '推荐补充：200g 鸡胸肉 / 150g 清蒸牛肉 / 2 块煎豆腐' 
        : (needProtein >= 15 ? '推荐补充：2 颗水煮蛋 / 250ml 低脂纯牛奶 / 100g 虾仁' : '推荐补充：1 颗鸡蛋 / 200ml 无糖豆浆');
      insights.push({
        id: 'protein_lack',
        type: 'deficit',
        badge: '🥩 蛋白摄入不足',
        badgeClass: 'badge-yellow',
        title: `蛋白质尚缺 ${needProtein}g (已达成 ${Math.round((protein / targetProtein) * 100)}%)`,
        suggestion: `蛋白质对于保持基础代谢与防止肌肉流失至关重要。建议下一餐优先补充：${foodTips}。`
      });
    }

    // 5. 碳水维度判断
    if (carbs > targetCarbs * 1.15) {
      const overCarbs = Math.round(carbs - targetCarbs);
      insights.push({
        id: 'carbs_over',
        type: 'warning',
        badge: '🍚 碳水摄入偏多',
        badgeClass: 'badge-yellow',
        title: `碳水化合物超出 ${overCarbs}g`,
        suggestion: `精制碳水偏高易引发血糖波动。下半天建议减少米饭、面食与含糖饮料，用紫薯或蔬菜替代。`
      });
    } else if (carbs < targetCarbs * 0.4 && consumedCal > targetCal * 0.5) {
      const needCarbs = Math.round(targetCarbs - carbs);
      insights.push({
        id: 'carbs_lack',
        type: 'deficit',
        badge: '🥔 优质碳水偏低',
        badgeClass: 'badge-blue',
        title: `碳水不足，尚缺 ${needCarbs}g`,
        suggestion: `碳水过低容易引起低血糖与头晕乏力。建议适量补充燕麦、紫薯、全麦面包等优质慢消化碳水。`
      });
    }

    // 6. 饮水维度判断
    if (waterAmount < waterTarget * 0.6) {
      const needWater = Math.round(waterTarget - waterAmount);
      insights.push({
        id: 'water_lack',
        type: 'deficit',
        badge: '💧 饮水充盈度低',
        badgeClass: 'badge-blue',
        title: `饮水量还差 ${needWater} ml`,
        suggestion: `充足的水分有助于加速脂肪代谢与毒素排出。建议分次补充 250~500ml 饮水。`
      });
    }

    // 7. 完美结构
    if (insights.length === 0) {
      insights.push({
        id: 'perfect',
        type: 'healthy',
        badge: '🥗 营养结构优秀',
        badgeClass: 'badge-green',
        title: '三大营养素与热量收支匹配完美！',
        suggestion: '热量控制得当，营养占比契合您的健康目标，请继续保持这良好的饮食习惯！'
      });
    }

    return insights;
  },

  openNutritionDetailModal() {
    this.setData({ showNutritionModal: true });
  },

  closeNutritionDetailModal() {
    this.setData({ showNutritionModal: false });
  },

  onGenerateAiDiagnosis() {
    const user = (app.globalData && app.globalData.userInfo) || wx.getStorageSync('userInfo');
    if (!user || !user.id) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }
    this.setData({ isDiagnosing: true });

    wx.request({
      url: `${app.globalData.baseUrl}/diet/diagnose?userId=${user.id}`,
      method: 'POST',
      success: (res) => {
        this.setData({ isDiagnosing: false });
        if (res.data && res.data.code === 200) {
          const data = res.data.data;
          const updates = {};
          if (data.expertComment) {
            updates.aiExpertComment = data.expertComment;
          }
          if (data.aiInsights && data.aiInsights.length > 0) {
            updates.nutritionInsights = data.aiInsights;
          }
          this.setData(updates);
          wx.showToast({ title: 'AI 诊断计算完成！', icon: 'success' });
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '生成诊断失败', icon: 'none' });
        }
      },
      fail: () => {
        this.setData({ isDiagnosing: false });
        wx.showToast({ title: '网络连接失败，请稍后重试', icon: 'none' });
      }
    });
  },

  drawCalorieRing(progressPercent, isOverLimit) {
    wx.nextTick(() => {
      this.executeDrawCalorieRing(progressPercent, isOverLimit, 0);
    });
  },

  executeDrawCalorieRing(progressPercent, isOverLimit, retryCount) {
    const query = wx.createSelectorQuery();
    query.select('#calorieCanvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!res[0] || !res[0].node) {
          if (retryCount < 6) {
            setTimeout(() => {
              this.executeDrawCalorieRing(progressPercent, isOverLimit, retryCount + 1);
            }, 50);
          } else {
            console.error('Failed to get canvas node after retries');
          }
          return;
        }

        const canvas = res[0].node;
        const ctx = canvas.getContext('2d');
        const systemInfo = wx.getSystemInfoSync ? wx.getSystemInfoSync() : null;
        const dpr = (systemInfo && systemInfo.pixelRatio) || 1;

        const size = res[0].width || 196;
        canvas.width = size * dpr;
        canvas.height = size * dpr;
        ctx.scale(dpr, dpr);

        ctx.clearRect(0, 0, size, size);

        const center = size / 2;
        const radius = size / 2 - 30;
        const lineWidth = 15;
        const startAngle = -0.54 * Math.PI;
        const totalAngle = 1.62 * Math.PI;
        const endAngle = startAngle + totalAngle;

        ctx.shadowColor = 'rgba(255, 255, 255, 0.9)';
        ctx.shadowBlur = 18;
        ctx.shadowOffsetX = 0;
        ctx.shadowOffsetY = 0;
        ctx.strokeStyle = 'rgba(255, 255, 255, 0.72)';
        ctx.lineWidth = lineWidth + 14;
        ctx.lineCap = 'round';
        ctx.beginPath();
        ctx.arc(center, center, radius, startAngle, endAngle);
        ctx.stroke();

        ctx.shadowColor = 'rgba(148, 163, 184, 0.16)';
        ctx.shadowBlur = 12;
        ctx.shadowOffsetY = 8;
        ctx.strokeStyle = 'rgba(231, 238, 243, 0.55)';
        ctx.lineWidth = lineWidth + 4;
        ctx.beginPath();
        ctx.arc(center, center, radius, startAngle, endAngle);
        ctx.stroke();

        ctx.shadowBlur = 0;
        ctx.shadowOffsetY = 0;

        const trackGradient = ctx.createLinearGradient(0, 0, size, size);
        trackGradient.addColorStop(0, 'rgba(255, 255, 255, 0.86)');
        trackGradient.addColorStop(0.52, 'rgba(226, 238, 244, 0.82)');
        trackGradient.addColorStop(1, 'rgba(255, 255, 255, 0.7)');
        ctx.strokeStyle = trackGradient;
        ctx.lineWidth = lineWidth;
        ctx.lineCap = 'round';
        ctx.beginPath();
        ctx.arc(center, center, radius, startAngle, endAngle);
        ctx.stroke();

        if (progressPercent > 0) {
          const safeProgress = Math.max(0, Math.min(progressPercent, 100));
          const progressEndAngle = startAngle + (safeProgress / 100) * totalAngle;
          const gradient = ctx.createLinearGradient(0, size, size, 0);

          if (isOverLimit) {
            gradient.addColorStop(0, '#EF4444');
            gradient.addColorStop(0.58, '#FB7185');
            gradient.addColorStop(1, '#DC2626');
            ctx.shadowColor = 'rgba(239, 68, 68, 0.35)';
          } else if (progressPercent <= 15) {
            gradient.addColorStop(0, '#F97316');
            gradient.addColorStop(0.58, '#F87171');
            gradient.addColorStop(1, '#EF4444');
            ctx.shadowColor = 'rgba(239, 68, 68, 0.30)';
          } else if (progressPercent <= 40) {
            gradient.addColorStop(0, '#F59E0B');
            gradient.addColorStop(0.58, '#FBBF24');
            gradient.addColorStop(1, '#F97316');
            ctx.shadowColor = 'rgba(245, 158, 11, 0.32)';
          } else {
            gradient.addColorStop(0, '#2BB7C6');
            gradient.addColorStop(0.58, '#54D8A6');
            gradient.addColorStop(1, '#A8E86A');
            ctx.shadowColor = 'rgba(45, 212, 191, 0.34)';
          }
          ctx.shadowBlur = 14;
          ctx.shadowOffsetX = 0;
          ctx.shadowOffsetY = 5;
          ctx.strokeStyle = gradient;
          ctx.lineWidth = lineWidth;
          ctx.lineCap = 'round';
          ctx.beginPath();
          ctx.arc(center, center, radius, startAngle, progressEndAngle);
          ctx.stroke();

          ctx.shadowBlur = 0;
          ctx.shadowOffsetY = 0;
          ctx.globalAlpha = 0.42;
          ctx.strokeStyle = 'rgba(255, 255, 255, 0.85)';
          ctx.lineWidth = 3;
          ctx.beginPath();
          ctx.arc(center, center, radius - lineWidth / 2 + 2, startAngle + 0.02, progressEndAngle - 0.02);
          ctx.stroke();
          ctx.globalAlpha = 1;
        }

      });
  },

  onScanMeal(e) {
    const mealType = e.currentTarget.dataset.type;
    const user = app.globalData.userInfo;
    if (!user) {
      wx.showToast({ title: '请先登录/设置档案', icon: 'none' });
      return;
    }
    const meal = this.data.meals.find(m => m.type === mealType);
    const mealName = meal ? meal.name : '饮食';

    this.setData({
      currentMealType: mealType,
      currentMealName: mealName,
      showMealOptionSheet: true,
      mealHint: '' // Clear previous input
    });
  },

  onMealHintInput(e) {
    this.setData({
      mealHint: e.detail.value
    });
  },

  tapQuickTag(e) {
    const tag = e.currentTarget.dataset.tag;
    const newHint = this.data.mealHint === tag ? '' : tag;
    this.setData({
      mealHint: newHint
    });
  },

  hideMealOptionModal() {
    this.setData({ showMealOptionSheet: false });
  },

  selectMealOption(e) {
    const option = e.currentTarget.dataset.option;
    const mealType = this.data.currentMealType;
    this.setData({ showMealOptionSheet: false });

    const user = app.globalData.userInfo;
    if (!user) return;

    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: option === 'camera' ? ['camera'] : ['album'],
      sizeType: ['original'],
      success: (mediaRes) => {
        const tempFilePath = mediaRes.tempFiles[0].tempFilePath;
        wx.showLoading({ title: 'AI 正在识别中...', mask: true });
        
        // 直接使用选择的文件上传，让大模型看到原图以追求极致精度
        this.doUploadAndRecognize(tempFilePath);
      }
    });
  },

  doUploadAndRecognize(filePath) {
    const user = app.globalData.userInfo;
    wx.uploadFile({
      url: `${app.globalData.baseUrl}/diet/recognize`,
      filePath: filePath,
      name: 'file',
      formData: {
        hint: encodeURIComponent(this.data.mealHint || ''),
        userId: user ? user.id : ''
      },
      success: (uploadRes) => {
        wx.hideLoading();
        this.setData({ mealHint: '' }); // Clear hint state after upload processing starts
        try {
          const result = JSON.parse(uploadRes.data);
          if (result.code === 200) {
            const parsedRecord = result.data;
            let foodItems = [];
            try {
              foodItems = JSON.parse(parsedRecord.foodItems);
              if (Array.isArray(foodItems)) {
                foodItems = foodItems.map(item => {
                  return {
                    ...item,
                    unitCalories: item.weight > 0 ? (item.calories / item.weight) : 0,
                    unitProtein: item.weight > 0 ? (item.protein / item.weight) : 0,
                    unitFat: item.weight > 0 ? (item.fat / item.weight) : 0,
                    unitCarbs: item.weight > 0 ? (item.carbs / item.weight) : 0
                  };
                });
              }
            } catch (e) {
              console.error('Failed to parse foodItems JSON:', e);
            }
            this.setData({
              tempParsedFoodItems: parsedRecord.foodItems,
              aiFoodItems: foodItems,
              showAiResultModal: true,
              aiResultSuccess: true,
              aiResultFoodText: this.formatFoodItems(parsedRecord.foodItems),
              aiResultCalories: parsedRecord.totalCalories,
              aiResultProtein: parsedRecord.totalProtein || 0,
              aiResultFat: parsedRecord.totalFat || 0,
              aiResultCarbs: parsedRecord.totalCarbs || 0,
              aiResultMessage: ''
            });
          } else {
            if (result.message && result.message.indexOf('积分余额不足') !== -1) {
              wx.showModal({
                title: '积分不足',
                content: result.message,
                confirmText: '去签到',
                cancelText: '取消',
                success: (modalRes) => {
                  if (modalRes.confirm) {
                    wx.switchTab({
                      url: '/pages/profile/profile'
                    });
                  }
                }
              });
            } else {
              this.setData({
                showAiResultModal: true,
                aiResultSuccess: false,
                aiResultMessage: result.message || 'AI 识别失败',
                aiResultFoodText: '',
                aiResultCalories: 0,
                aiResultProtein: 0,
                aiResultFat: 0,
                aiResultCarbs: 0,
                aiFoodItems: []
              });
            }
          }
        } catch (e) {
          wx.showToast({ title: '解析数据错误', icon: 'error' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '上传失败', icon: 'error' });
      }
    });
  },
  hideAiResultModal() {
    this.setData({ showAiResultModal: false });
  },

  onAiResultConfirm() {
    this.setData({ showAiResultModal: false });
    if (this.data.aiResultSuccess) {
      // Serialize updated items back to JSON string
      const updatedJson = JSON.stringify(this.data.aiFoodItems);
      this.setData({
        tempParsedFoodItems: updatedJson
      });
      // 识别成功，进入油量选择
      this.setData({ showOilOptionSheet: true });
    }
  },

  onFoodItemNameInput(e) {
    const index = e.currentTarget.dataset.index;
    const value = e.detail.value.trim();
    const items = this.data.aiFoodItems;
    items[index].name = value;

    // Fuzzy matching against the dictionary
    if (value.length > 0) {
      const match = COMMON_FOOD_DICTIONARY.find(f => f.name.includes(value) || value.includes(f.name));
      if (match) {
        items[index].matched = match;
      } else {
        items[index].matched = null;
      }
    } else {
      items[index].matched = null;
    }

    this.setData({
      aiFoodItems: items
    });
  },

  onFoodItemWeightInput(e) {
    const index = e.currentTarget.dataset.index;
    const newWeight = parseFloat(e.detail.value) || 0;
    const items = this.data.aiFoodItems;
    const item = items[index];
    
    // Look up dictionary if matching food exists, to get exact nutrition ratios
    let dictMatch = COMMON_FOOD_DICTIONARY.find(f => f.name === item.name);
    
    if (dictMatch && newWeight > 0) {
      item.calories = parseFloat(((dictMatch.caloriesPer100g * newWeight) / 100).toFixed(1));
      item.protein = parseFloat(((dictMatch.proteinPer100g * newWeight) / 100).toFixed(1));
      item.fat = parseFloat(((dictMatch.fatPer100g * newWeight) / 100).toFixed(1));
      item.carbs = parseFloat(((dictMatch.carbsPer100g * newWeight) / 100).toFixed(1));
    } else if (item.weight > 0 && newWeight > 0) {
      const factor = newWeight / item.weight;
      item.calories = parseFloat((item.calories * factor).toFixed(1));
      if (item.protein !== undefined) item.protein = parseFloat((item.protein * factor).toFixed(1));
      if (item.fat !== undefined) item.fat = parseFloat((item.fat * factor).toFixed(1));
      if (item.carbs !== undefined) item.carbs = parseFloat((item.carbs * factor).toFixed(1));
    }
    item.weight = newWeight;
    
    this.setData({
      aiFoodItems: items
    });
    this.recalculateTotalCalories();
  },

  onFoodItemCaloriesInput(e) {
    const index = e.currentTarget.dataset.index;
    const newCal = parseFloat(e.detail.value) || 0;
    const items = this.data.aiFoodItems;
    const item = items[index];
    item.calories = newCal;
    item.unitCalories = item.weight > 0 ? (newCal / item.weight) : 0;
    this.setData({
      aiFoodItems: items
    });
    this.recalculateTotalCalories();
  },

  applyMatchSuggestion(e) {
    const index = e.currentTarget.dataset.index;
    const items = this.data.aiFoodItems;
    const item = items[index];
    const match = item.matched;
    if (match) {
      item.name = match.name;
      item.weight = match.standardWeight;
      item.calories = parseFloat(((match.caloriesPer100g * match.standardWeight) / 100).toFixed(1));
      item.protein = parseFloat(((match.proteinPer100g * match.standardWeight) / 100).toFixed(1));
      item.fat = parseFloat(((match.fatPer100g * match.standardWeight) / 100).toFixed(1));
      item.carbs = parseFloat(((match.carbsPer100g * match.standardWeight) / 100).toFixed(1));
      item.matched = null; // Hide matched tooltip
      
      this.setData({
        aiFoodItems: items
      });
      this.recalculateTotalCalories();
    }
  },

  addHelperFood(e) {
    const name = e.currentTarget.dataset.name;
    const match = COMMON_FOOD_DICTIONARY.find(f => f.name === name);
    if (match) {
      const items = this.data.aiFoodItems || [];
      items.push({
        name: match.name,
        weight: match.standardWeight,
        calories: parseFloat(((match.caloriesPer100g * match.standardWeight) / 100).toFixed(1)),
        protein: parseFloat(((match.proteinPer100g * match.standardWeight) / 100).toFixed(1)),
        fat: parseFloat(((match.fatPer100g * match.standardWeight) / 100).toFixed(1)),
        carbs: parseFloat(((match.carbsPer100g * match.standardWeight) / 100).toFixed(1))
      });
      
      this.setData({
        aiFoodItems: items
      });
      this.recalculateTotalCalories();
    }
  },

  deleteFoodItem(e) {
    const index = e.currentTarget.dataset.index;
    const items = this.data.aiFoodItems;
    items.splice(index, 1);
    this.setData({
      aiFoodItems: items
    });
    this.recalculateTotalCalories();
  },

  addNewFoodItem() {
    const items = this.data.aiFoodItems || [];
    items.push({
      name: '',
      weight: 0,
      calories: 0,
      protein: 0,
      fat: 0,
      carbs: 0
    });
    this.setData({
      aiFoodItems: items
    });
    this.recalculateTotalCalories();
  },

  recalculateTotalCalories() {
    const items = this.data.aiFoodItems || [];
    let totalCal = 0;
    let totalProtein = 0;
    let totalFat = 0;
    let totalCarbs = 0;
    items.forEach(item => {
      totalCal += parseFloat(item.calories) || 0;
      totalProtein += parseFloat(item.protein) || 0;
      totalFat += parseFloat(item.fat) || 0;
      totalCarbs += parseFloat(item.carbs) || 0;
    });
    this.setData({
      aiResultCalories: parseFloat(totalCal.toFixed(1)),
      aiResultProtein: parseFloat(totalProtein.toFixed(1)),
      aiResultFat: parseFloat(totalFat.toFixed(1)),
      aiResultCarbs: parseFloat(totalCarbs.toFixed(1))
    });
  },

  onFoodItemProteinInput(e) {
    const index = e.currentTarget.dataset.index;
    const newProtein = parseFloat(e.detail.value) || 0;
    const items = this.data.aiFoodItems;
    items[index].protein = newProtein;
    this.setData({
      aiFoodItems: items
    });
    this.recalculateTotalCalories();
  },

  onFoodItemFatInput(e) {
    const index = e.currentTarget.dataset.index;
    const newFat = parseFloat(e.detail.value) || 0;
    const items = this.data.aiFoodItems;
    items[index].fat = newFat;
    this.setData({
      aiFoodItems: items
    });
    this.recalculateTotalCalories();
  },

  onFoodItemCarbsInput(e) {
    const index = e.currentTarget.dataset.index;
    const newCarbs = parseFloat(e.detail.value) || 0;
    const items = this.data.aiFoodItems;
    items[index].carbs = newCarbs;
    this.setData({
      aiFoodItems: items
    });
    this.recalculateTotalCalories();
  },

  hideOilOptionModal() {
    this.setData({ showOilOptionSheet: false });
  },

  selectOilOption(e) {
    const oilLevel = e.currentTarget.dataset.level;
    this.setData({ showOilOptionSheet: false });

    const user = app.globalData.userInfo;
    if (!user) return;

    this.saveMealRecord(user.id, this.data.currentMealType, this.data.tempParsedFoodItems, oilLevel);
  },

  formatFoodItems(jsonStr) {
    try {
      const items = JSON.parse(jsonStr);
      return items.map(i => `${i.name} (${i.weight}克)`).join('\n');
    } catch(e) {
      return '混合膳食';
    }
  },

  saveMealRecord(userId, mealType, foodItemsJson, oilLevel) {
    wx.showLoading({ title: '正在保存记录...' });
    wx.request({
      url: `${app.globalData.baseUrl}/diet/record`,
      method: 'POST',
      data: {
        userId,
        mealType,
        foodItems: foodItemsJson,
        oilLevel,
        imageUrl: 'https://images.example.com/meals/lunch.jpg'
      },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          wx.showToast({ title: '记录成功', icon: 'success' });
          this.checkUserAndLoadData(); // reload dashboard
        } else {
          wx.showToast({ title: '保存记录失败', icon: 'error' });
        }
      },
      fail: () => {
        wx.showToast({ title: '连接服务器失败', icon: 'error' });
      },
      complete: () => {
        wx.hideLoading();
      }
    });
  },

  // ==================== 饮水相关 ====================
  openWaterSheet() {
    this.setData({ showWaterSheet: true });
  },

  closeWaterSheet() {
    this.setData({ showWaterSheet: false });
  },

  loadWaterData(userId, dateStr) {
    wx.request({
      url: `${app.globalData.baseUrl}/water/daily`,
      method: 'GET',
      data: { userId, date: dateStr },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const record = res.data.data;
          const amount = (record && record.amount) ? record.amount : 0;
          const target = (record && record.target) ? record.target : 2000;
          this.updateWaterUI(amount, target);
        }
      }
    });
  },

  updateWaterUI(amount, target) {
    const pct = target > 0 ? Math.min(Math.round(amount / target * 100), 100) : 0;
    const fillH = target > 0 ? Math.min(amount / target * 100, 100) : 0;
    this.setData({
      waterAmount: amount,
      waterTarget: target,
      waterPercent: pct,
      waterFillHeight: fillH
    }, () => {
      if (this.data.nutrients) {
        const diagnosisData = {
          consumedCal: this.data.consumedCal || 0,
          targetCal: this.data.targetCal || 2000,
          carbs: this.data.nutrients.carbs || 0,
          targetCarbs: this.data.nutrients.targetCarbs || 250,
          protein: this.data.nutrients.protein || 0,
          targetProtein: this.data.nutrients.targetProtein || 70,
          fat: this.data.nutrients.fat || 0,
          targetFat: this.data.nutrients.targetFat || 50,
          waterAmount: amount,
          waterTarget: target
        };
        const nutritionInsights = this.calculateNutritionDiagnosis(diagnosisData);
        this.setData({ nutritionInsights });
      }
    });
  },

  onWaterAdd(e) {
    const addAmount = parseInt(e.currentTarget.dataset.amount) || 250;
    const user = app.globalData.userInfo;
    if (!user) return;
    const todayStr = this.getTodayDateString();
    wx.request({
      url: `${app.globalData.baseUrl}/water/add`,
      method: 'POST',
      data: { userId: user.id, date: todayStr, amount: addAmount },
      header: { 'content-type': 'application/x-www-form-urlencoded' },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const record = res.data.data;
          this.updateWaterUI(record.amount, record.target || this.data.waterTarget);
          wx.showToast({ title: `+${addAmount}ml 💧`, icon: 'none' });
        }
      }
    });
  },

  onWaterReduce(e) {
    const reduceAmount = parseInt(e.currentTarget.dataset.amount) || 250;
    if (this.data.waterAmount <= 0) {
      wx.showToast({ title: '已经是 0 了', icon: 'none' });
      return;
    }
    const user = app.globalData.userInfo;
    if (!user) return;
    const todayStr = this.getTodayDateString();
    wx.request({
      url: `${app.globalData.baseUrl}/water/reduce`,
      method: 'POST',
      data: { userId: user.id, date: todayStr, amount: reduceAmount },
      header: { 'content-type': 'application/x-www-form-urlencoded' },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const record = res.data.data;
          this.updateWaterUI(record.amount, record.target || this.data.waterTarget);
          wx.showToast({ title: `-${reduceAmount}ml`, icon: 'none' });
        }
      }
    });
  },

  // ==================== 运动消耗与热量抵扣 ====================
  openAddExerciseModal() {
    this.setData({
      showExerciseModal: true,
      exerciseDuration: '',
      exerciseCalories: '',
      exerciseTypeIndex: 0
    });
  },

  closeAddExerciseModal() {
    this.setData({
      showExerciseModal: false,
      showExerciseTypeSheet: false
    });
  },

  openExerciseTypeSheet() {
    this.setData({
      showExerciseTypeSheet: true
    });
  },

  closeExerciseTypeSheet() {
    this.setData({
      showExerciseTypeSheet: false
    });
  },

  selectExerciseType(e) {
    const idx = parseInt(e.currentTarget.dataset.index) || 0;
    this.setData({
      exerciseTypeIndex: idx,
      showExerciseTypeSheet: false
    }, () => {
      this.recalculateExerciseCalories();
    });
  },

  onExerciseDurationInput(e) {
    const duration = e.detail.value;
    this.setData({
      exerciseDuration: duration
    }, () => {
      this.recalculateExerciseCalories();
    });
  },

  onExerciseCaloriesInput(e) {
    this.setData({
      exerciseCalories: e.detail.value
    });
  },

  recalculateExerciseCalories() {
    const idx = this.data.exerciseTypeIndex;
    const duration = parseInt(this.data.exerciseDuration) || 0;
    if (idx === 0 || duration <= 0) {
      this.setData({ exerciseCalories: '' });
      return;
    }
    const met = this.data.exerciseTypeOptions[idx].met;
    // 获取用户体重，如无则默认 70kg
    const user = app.globalData.userInfo || {};
    const weight = user.weight || 70.0;
    // 消耗卡路里 = MET * 体重(kg) * (时长/60) * 1.05
    const calories = Math.round(met * weight * (duration / 60.0) * 1.05 * 10) / 10;
    this.setData({
      exerciseCalories: calories
    });
  },

  submitExercise() {
    const idx = this.data.exerciseTypeIndex;
    const duration = parseInt(this.data.exerciseDuration) || 0;
    const calories = parseFloat(this.data.exerciseCalories) || 0.0;

    if (idx === 0) {
      wx.showToast({ title: '请选择运动类型', icon: 'none' });
      return;
    }
    if (duration <= 0) {
      wx.showToast({ title: '请输入运动时长', icon: 'none' });
      return;
    }

    const activityName = this.data.exerciseTypeOptions[idx].name.replace(/[\uD83C-\uDBFF\uDC00-\uDFFF\s]/g, ''); // 过滤表情
    const todayStr = this.getTodayDateString();
    
    wx.showLoading({ title: '正在保存...' });
    wx.request({
      url: `${app.globalData.baseUrl}/exercise/add`,
      method: 'POST',
      header: {
        'content-type': 'application/x-www-form-urlencoded'
      },
      data: {
        userId: app.globalData.userInfo.id,
        date: todayStr,
        activityName: activityName,
        durationMinutes: duration,
        caloriesBurned: calories
      },
      success: (res) => {
        wx.hideLoading();
        if (res.data && res.data.code === 200) {
          wx.showToast({ title: '记录成功', icon: 'success' });
          this.setData({
            showExerciseModal: false,
            showExerciseTypeSheet: false
          });
          this.checkUserAndLoadData(); // 重新加载数据刷新进度
        } else {
          wx.showToast({ title: res.data.msg || '保存失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络请求失败', icon: 'none' });
      }
    });
  },

  onDeleteExercise(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) return;

    wx.showModal({
      title: '提示',
      content: '确定要删除这条运动记录吗？',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '正在删除...' });
          wx.request({
            url: `${app.globalData.baseUrl}/exercise/delete/${id}`,
            method: 'DELETE',
            success: (delRes) => {
              wx.hideLoading();
              if (delRes.data && delRes.data.code === 200) {
                wx.showToast({ title: '已删除', icon: 'success' });
                this.checkUserAndLoadData(); // 重新刷新看板
              } else {
                wx.showToast({ title: '删除失败', icon: 'none' });
              }
            },
            fail: () => {
              wx.hideLoading();
              wx.showToast({ title: '网络请求失败', icon: 'none' });
            }
          });
        }
      }
    });
  },

  checkLateCheckinStatus(userId) {
    const now = new Date();
    const hour = now.getHours();
    if (hour >= 20) {
      wx.request({
        url: `${app.globalData.baseUrl}/team/user/${userId}/active`,
        method: 'GET',
        success: (res) => {
          if (res.data && res.data.code === 200 && res.data.data) {
            const team = res.data.data;
            const currentUserMember = team.members ? team.members.find(m => m.userId === userId) : null;
            if (currentUserMember && !currentUserMember.todayChecked) {
              this.setData({ showLateCheckinWarning: true });
            } else {
              this.setData({ showLateCheckinWarning: false });
            }
          }
        }
      });
    } else {
      this.setData({ showLateCheckinWarning: false });
    }
  },

  checkPendingNudgeAlert(userId) {
    if (!userId) return;
    wx.request({
      url: `${app.globalData.baseUrl}/team/nudge/alert?userId=${userId}`,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200 && res.data.data) {
          wx.showModal({
            title: '🔔 小队打卡提醒',
            content: res.data.data,
            confirmText: '去拍照打卡',
            confirmColor: '#10B981',
            cancelText: '知道啦',
            success: (mRes) => {
              if (mRes.confirm) {
                this.onTapAddMeal();
              }
            }
          });
        }
      }
    });
  },

  onQuickPhotoRecord() {
    this.onTapAddMeal();
  },

  checkWaterReminderStatus(userId) {
    if (!userId) return;
    const today = new Date().toISOString().split('T')[0];

    wx.request({
      url: `${app.globalData.baseUrl}/water/reminder-check?userId=${userId}&targetAmount=${this.data.waterTarget}`,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200 && res.data.data) {
          const info = res.data.data;
          const dismissedDate = wx.getStorageSync(`water_dismissed_${info.timeSlot}`);

          if (info.needReminder && dismissedDate !== today) {
            this.setData({
              showWaterReminderBar: true,
              waterReminderMsg: info.reminderMsg || '🥤 记得补水哦！保持水分代谢平衡',
              currentWaterTimeSlot: info.timeSlot
            });
          } else {
            this.setData({ showWaterReminderBar: false });
          }
        }
      }
    });
  },

  onQuickAddWater250() {
    app.login((user) => {
      const today = new Date().toISOString().split('T')[0];
      const slot = this.data.currentWaterTimeSlot || 0;
      wx.setStorageSync(`water_dismissed_${slot}`, today);

      wx.request({
        url: `${app.globalData.baseUrl}/water/add?userId=${user.id}&date=${today}&amount=250`,
        method: 'POST',
        success: (res) => {
          if (res.data && res.data.code === 200) {
            wx.showToast({ title: '已补水 250ml 💧', icon: 'success' });
            this.setData({ showWaterReminderBar: false });
            this.loadWaterRecord(user.id);
          }
        }
      });
    });
  },

  onDismissWaterReminder() {
    const today = new Date().toISOString().split('T')[0];
    const slot = this.data.currentWaterTimeSlot || 0;
    wx.setStorageSync(`water_dismissed_${slot}`, today);
    this.setData({ showWaterReminderBar: false });
  },

  _loadWaterSubQuota() {
    app.login((user) => {
      if (!user || !user.id) return;
      wx.request({
        url: `${app.globalData.baseUrl}/user/subscribe-info?userId=${user.id}&type=WATER`,
        method: 'GET',
        success: (res) => {
          if (res.data && res.data.code === 200) {
            const quota = res.data.data.quota || 0;
            this.setData({
              waterSubQuota: quota,
              isWaterSubscribed: quota > 0
            });
            if (quota > 0) {
              wx.setStorageSync('water_wx_subscribed', true);
            }
          }
        }
      });
    });
  },

  _silentRenewWaterQuota() {
    const templateId = '6rHAfQw2A3WSw00LCaV9MUSop3OFVsRTAx4I-xgW5lw';
    wx.requestSubscribeMessage({
      tmplIds: [templateId],
      success: (res) => {
        if (res[templateId] === 'accept') {
          // 静默续费成功，通知后端 +1 额度
          app.login((user) => {
            if (user && user.id) {
              wx.request({
                url: `${app.globalData.baseUrl}/user/subscribe?userId=${user.id}&templateId=${templateId}&type=WATER&count=1`,
                method: 'POST',
                success: () => {
                  this._loadWaterSubQuota();
                }
              });
            }
          });
        }
      },
      fail: () => {
        // 静默续费失败 (用户未勾选"总是允许")，不提示
      }
    });
  },

  onSubscribeWaterPush() {
    if (this.data.isWaterSubscribed) {
      wx.showModal({
        title: '关闭微信提醒',
        content: `当前剩余 ${this.data.waterSubQuota} 次提醒额度。\n确定要关闭微信聊天框定时喝水提醒吗？`,
        confirmText: '确定关闭',
        cancelText: '保持开启',
        success: (res) => {
          if (res.confirm) {
            this.setData({ isWaterSubscribed: false, waterSubQuota: 0 });
            wx.removeStorageSync('water_wx_subscribed');
            wx.showToast({ title: '已关闭微信提醒', icon: 'none' });
            app.login((user) => {
              if (user && user.id) {
                wx.request({
                  url: `${app.globalData.baseUrl}/user/unsubscribe?userId=${user.id}&type=WATER`,
                  method: 'POST'
                });
              }
            });
          }
        }
      });
      return;
    }

    const templateId = '6rHAfQw2A3WSw00LCaV9MUSop3OFVsRTAx4I-xgW5lw';

    wx.requestSubscribeMessage({
      tmplIds: [templateId],
      success: (res) => {
        if (res[templateId] === 'accept') {
          this.setData({ isWaterSubscribed: true });
          wx.setStorageSync('water_wx_subscribed', true);

          app.login((user) => {
            if (user && user.id) {
              wx.request({
                url: `${app.globalData.baseUrl}/user/subscribe?userId=${user.id}&templateId=${templateId}&type=WATER&count=1`,
                method: 'POST',
                success: () => {
                  this._loadWaterSubQuota();
                  wx.showModal({
                    title: '✅ 已开启饮水提醒',
                    content: '💡 小贴士：授权弹窗中勾选「总是保持以上选择」后，每次打开小程序会自动续费额度，提醒永不断供！',
                    showCancel: false,
                    confirmText: '知道了'
                  });
                }
              });
            }
          });
        } else {
          this.setData({ isWaterSubscribed: false });
          wx.removeStorageSync('water_wx_subscribed');
          wx.showToast({ title: '未授权，无法发送提醒', icon: 'none' });
        }
      },
      fail: (err) => {
        console.log('Subscribe message fail:', err);
        wx.showToast({ title: '授权失败', icon: 'none' });
      }
    });
  },

  openPlanModal() {
    if (this.data.features && this.data.features.ai_plan === false) {
      wx.showToast({ title: '该功能升级维护中，敬请期待！', icon: 'none' });
      return;
    }
    this.setData({ showPlanModal: true });
    this.fetchPlanStatus(() => {
      // 如果已有计划缓存且本页面 planData 尚空，只读取不主动付费生成
      if (this.data.planStatus && this.data.planStatus.hasPlan && !this.data.planData) {
        this.fetchAiPlan(false, false);
      }
    });
  },

  fetchFeatureToggles() {
    let env = 'release';
    try {
      const accountInfo = wx.getAccountInfoSync();
      env = (accountInfo && accountInfo.miniProgram && accountInfo.miniProgram.envVersion) || 'release';
    } catch (e) {}

    wx.request({
      url: `${app.globalData.baseUrl}/config/features?env=${env}`,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200 && res.data.data) {
          this.setData({ features: res.data.data });
        }
      }
    });
  },

  fetchPlanStatus(callback) {
    app.login((user) => {
      if (!user || !user.id) return;
      wx.request({
        url: `${app.globalData.baseUrl}/plan/status?userId=${user.id}`,
        method: 'GET',
        success: (res) => {
          if (res.data && res.data.code === 200) {
            this.setData({ planStatus: res.data.data });
            if (callback) callback();
          }
        }
      });
    });
  },

  closePlanModal() {
    this.setData({ showPlanModal: false });
  },

  updateActiveDietPlan(planData, dayIndex) {
    if (!planData || !planData.dietPlan) return null;
    if (Array.isArray(planData.dietPlan)) {
      return planData.dietPlan[dayIndex] || planData.dietPlan[0];
    }
    return planData.dietPlan;
  },

  switchPlanTab(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({ planActiveTab: tab });
  },

  switchPlanDay(e) {
    const index = parseInt(e.currentTarget.dataset.index) || 0;
    const activeDietPlan = this.updateActiveDietPlan(this.data.planData, index);
    this.setData({
      planDayIndex: index,
      activeDietPlan: activeDietPlan
    });
  },

  onGeneratePlanClick() {
    const status = this.data.planStatus || { isFirstTime: true, userPoints: 0 };
    const isFirstTime = status.isFirstTime;
    const userPoints = status.userPoints || 0;

    if (!isFirstTime && userPoints < 100) {
      wx.showModal({
        title: '契约积分不足',
        content: `重新定制计划需消耗 100 积分，您当前共有 ${userPoints} 积分。\n\n可以通过每日签到（+20积分）或打卡赚取积分哦！`,
        confirmText: '去签到',
        cancelText: '取消',
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({ url: '/pages/profile/profile' });
          }
        }
      });
      return;
    }

    const title = isFirstTime ? '✨ 首次生成免费' : '🤖 重新定制计划';
    const content = isFirstTime
      ? '首次生成专属 7 天运动与膳食食谱【免费】！是否立即让 AI 为您推算？'
      : `本次重新定制将消耗 100 积分（当前可用 ${userPoints} 积分），是否确定生成？`;

    wx.showModal({
      title,
      content,
      confirmText: '确定生成',
      confirmColor: '#10B981',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          this.fetchAiPlan(true, true);
        }
      }
    });
  },

  onRefreshPlan() {
    this.onGeneratePlanClick();
  },

  fetchAiPlan(forceRefresh, createIfAbsent = true) {
    this.setData({ planLoading: true });
    app.login((user) => {
      if (!user || !user.id) {
        this.setData({ planLoading: false });
        return;
      }
      wx.request({
        url: `${app.globalData.baseUrl}/plan/generate?userId=${user.id}&forceRefresh=${forceRefresh ? 'true' : 'false'}&createIfAbsent=${createIfAbsent ? 'true' : 'false'}`,
        method: 'GET',
        success: (res) => {
          this.setData({ planLoading: false });
          if (res.data && res.data.code === 200) {
            const planData = res.data.data;
            if (planData) {
              const activeDietPlan = this.updateActiveDietPlan(planData, 0);
              const userPoints = planData.userPoints !== undefined ? planData.userPoints : (this.data.planStatus ? this.data.planStatus.userPoints : 0);
              this.setData({
                planData: planData,
                planDayIndex: 0,
                activeDietPlan: activeDietPlan,
                'planStatus.hasPlan': true,
                'planStatus.isFirstTime': false,
                'planStatus.userPoints': userPoints
              });
              if (forceRefresh) {
                wx.showToast({ title: 'AI 定制计划已更新！', icon: 'success' });
              }
            }
          } else {
            wx.showToast({ title: res.data.message || '获取计划失败', icon: 'none' });
          }
        },
        fail: () => {
          this.setData({ planLoading: false });
          wx.showToast({ title: '网络失败', icon: 'none' });
        }
      });
    });
  },

  onRefreshPlan() {
    wx.showModal({
      title: '🤖 重新定制计划',
      content: '是否基于您最新的身体档案和目标，让 AI 重新推算生成专属计划？',
      confirmText: '重新生成',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          this.fetchAiPlan(true);
        }
      }
    });
  },

  onAddPlanExercise(e) {
    const { name, duration, calories } = e.currentTarget.dataset;
    app.login((user) => {
      if (!user || !user.id) return;
      wx.request({
        url: `${app.globalData.baseUrl}/exercise/add`,
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        data: {
          userId: user.id,
          exerciseType: name,
          durationMinutes: parseInt(duration) || 30,
          caloriesBurned: parseInt(calories) || 150
        },
        success: (res) => {
          if (res.data && res.data.code === 200) {
            wx.showToast({ title: `已成功打卡 ${name}！`, icon: 'success' });
            this.loadUserData(user); // 刷新首页进度条
          } else {
            wx.showToast({ title: '打卡失败', icon: 'none' });
          }
        }
      });
    });
  },

  onChoosePosterPhoto() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      sizeType: ['compressed'],
      success: (res) => {
        const tempFilePath = res.tempFiles[0].tempFilePath;
        this.setData({ posterFoodImg: tempFilePath, tempPosterPath: '' });
        // 如果海报弹窗已打开，自动重新绘制
        if (this.data.showPosterModal) {
          this._doGeneratePoster();
        }
      }
    });
  },

  onRemovePosterPhoto() {
    this.setData({ posterFoodImg: '', tempPosterPath: '' });
    if (this.data.showPosterModal) {
      this._doGeneratePoster();
    }
  },

  onGeneratePoster() {
    this.setData({
      showPosterModal: true,
      tempPosterPath: ''
    });

    // 如果用户还没有选照片，先弹出选择器；如果已选过直接生成
    if (!this.data.posterFoodImg) {
      wx.showModal({
        title: '🥗 选择一张今日美食照片',
        content: '上传你的餐食美照，让海报更加精美有个性！\n（照片不会上传服务器，仅在本地临时使用）',
        confirmText: '选择照片',
        cancelText: '跳过',
        success: (res) => {
          if (res.confirm) {
            wx.chooseMedia({
              count: 1,
              mediaType: ['image'],
              sourceType: ['album', 'camera'],
              sizeType: ['compressed'],
              success: (mediaRes) => {
                this.setData({ posterFoodImg: mediaRes.tempFiles[0].tempFilePath });
                this._doGeneratePoster();
              },
              fail: () => {
                this._doGeneratePoster();
              }
            });
          } else {
            this._doGeneratePoster();
          }
        }
      });
    } else {
      this._doGeneratePoster();
    }
  },

  _doGeneratePoster() {
    wx.showLoading({ title: '正在生成海报...' });

    const user = app.globalData.userInfo;
    const myId = user ? user.id : null;
    const todayStr = this.getTodayDateString();

    wx.request({
      url: `${app.globalData.baseUrl}/diet/daily?userId=${myId}&date=${todayStr}`,
      method: 'GET',
      success: (dietRes) => {
        const dietRecords = dietRes.data && dietRes.data.code === 200 ? dietRes.data.data : [];
        this._dietRecords = dietRecords;

        // 使用用户本地临时选择的照片 (不上传服务器，零内存负担)
        let bgUrl = this.data.posterFoodImg || '';

        const inviteCode = user && user.inviteCode ? user.inviteCode : '';
        const qrUrl = inviteCode
          ? `${app.globalData.baseUrl}/team/qrcode?inviteCode=${inviteCode}`
          : `${app.globalData.baseUrl}/team/qrcode`;
        
        let avatarUrl = '/images/profile.png';
        if (user && user.avatarUrl) {
          if (user.avatarUrl.startsWith('/uploads')) {
            avatarUrl = `${app.globalData.baseUrl}${user.avatarUrl}`;
          } else {
            avatarUrl = user.avatarUrl;
          }
        }

        const downloadBgPromise = new Promise((resolve) => {
          if (!bgUrl) {
            resolve('');
          } else if (bgUrl.startsWith('http')) {
            wx.downloadFile({
              url: bgUrl,
              success: (res) => resolve(res.statusCode === 200 ? res.tempFilePath : ''),
              fail: () => resolve('')
            });
          } else {
            // 本地临时文件路径 (wx.chooseMedia 返回的 tempFilePath)
            wx.getImageInfo({
              src: bgUrl,
              success: (res) => resolve(res.path),
              fail: () => resolve('')
            });
          }
        });

        const downloadQrPromise = new Promise((resolve) => {
          wx.downloadFile({
            url: qrUrl,
            success: (res) => resolve(res.statusCode === 200 ? res.tempFilePath : ''),
            fail: () => resolve('')
          });
        });

        const downloadAvatarPromise = new Promise((resolve) => {
          if (avatarUrl.startsWith('/')) {
            wx.getImageInfo({
              src: avatarUrl,
              success: (res) => resolve(res.path),
              fail: () => resolve('')
            });
          } else {
            wx.downloadFile({
              url: avatarUrl,
              success: (res) => resolve(res.statusCode === 200 ? res.tempFilePath : ''),
              fail: () => resolve('')
            });
          }
        });

        Promise.all([downloadBgPromise, downloadQrPromise, downloadAvatarPromise]).then(([tempBgPath, tempQrPath, tempAvatarPath]) => {
          const avatarFallbackPromise = tempAvatarPath 
            ? Promise.resolve(tempAvatarPath) 
            : new Promise((res) => {
                wx.getImageInfo({
                  src: '/images/profile.png',
                  success: (info) => res(info.path),
                  fail: () => res('')
                });
              });

          avatarFallbackPromise.then((finalAvatarPath) => {
            const query = wx.createSelectorQuery();
            query.select('#posterCanvas')
              .fields({ node: true, size: true })
              .exec((res) => {
                if (!res[0] || !res[0].node) {
                  wx.hideLoading();
                  wx.showToast({ title: '未找到绘制画布', icon: 'none' });
                  return;
                }

                const canvas = res[0].node;
                const ctx = canvas.getContext('2d');
                const systemInfo = wx.getSystemInfoSync ? wx.getSystemInfoSync() : null;
                const dpr = (systemInfo && systemInfo.pixelRatio) || 2;
                
                canvas.width = 750 * dpr;
                canvas.height = 1000 * dpr;
                ctx.scale(dpr, dpr);

                Promise.all([
                  this.loadImage(canvas, tempBgPath),
                  this.loadImage(canvas, tempQrPath),
                  this.loadImage(canvas, finalAvatarPath)
                ]).then(([bgImg, qrImg, avatarImg]) => {
                  const template = this.data.activeTemplate;
                  if (template === 'morandi') {
                    this.drawMorandiPoster(canvas, ctx, bgImg, qrImg, avatarImg);
                  } else if (template === 'vogue') {
                    this.drawVoguePoster(canvas, ctx, bgImg, qrImg, avatarImg);
                  } else {
                    this.drawReceiptPoster(canvas, ctx, bgImg, qrImg, avatarImg);
                  }

                  wx.canvasToTempFilePath({
                    canvas,
                    destWidth: 750,
                    destHeight: 1000,
                    success: (resTemp) => {
                      wx.hideLoading();
                      this.setData({ tempPosterPath: resTemp.tempFilePath });
                    },
                    fail: (err) => {
                      console.error('Canvas export error:', err);
                      wx.hideLoading();
                      wx.showToast({ title: '导出图片失败', icon: 'none' });
                    }
                  });
                });
              });
          });
        }).catch((err) => {
          console.error(err);
          wx.hideLoading();
          wx.showToast({ title: '下载素材失败', icon: 'none' });
        });
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '拉取记录失败', icon: 'none' });
      }
    });
  },

  switchTemplate(e) {
    const template = e.currentTarget.dataset.template;
    if (template === this.data.activeTemplate) return;
    this.setData({ activeTemplate: template, tempPosterPath: '' });
    this._doGeneratePoster();
  },

  closePosterModal() {
    this.setData({ showPosterModal: false });
  },

  savePosterToAlbum() {
    if (!this.data.tempPosterPath) {
      wx.showToast({ title: '海报生成中...', icon: 'none' });
      return;
    }
    wx.saveImageToPhotosAlbum({
      filePath: this.data.tempPosterPath,
      success: () => {
        wx.showToast({ title: '已保存至相册！', icon: 'success' });
      },
      fail: (err) => {
        if (err.errMsg && err.errMsg.indexOf('auth deny') >= 0) {
          wx.showModal({
            title: '授权提示',
            content: '请在设置中允许保存照片到相册',
            success: (modalRes) => {
              if (modalRes.confirm) wx.openSetting();
            }
          });
        }
      }
    });
  },

  loadImage(canvas, src) {
    return new Promise((resolve) => {
      if (!src) { resolve(null); return; }
      const img = canvas.createImage();
      img.onload = () => resolve(img);
      img.onerror = () => resolve(null);
      img.src = src;
    });
  },

  drawRoundedRect(ctx, x, y, width, height, radius) {
    ctx.beginPath();
    ctx.moveTo(x + radius, y);
    ctx.arcTo(x + width, y, x + width, y + height, radius);
    ctx.arcTo(x + width, y + height, x, y + height, radius);
    ctx.arcTo(x, y + height, x, y, radius);
    ctx.arcTo(x, y, x + width, y, radius);
    ctx.closePath();
  },

  drawMorandiPoster(canvas, ctx, bgImg, qrImg, avatarImg) {
    const user = app.globalData.userInfo;
    const nickname = user && user.nickname ? user.nickname : '自律达人';
    const consumedCal = this.data.consumedCal || 0;
    const targetCal = this.data.targetCal || 2000;
    const nutrients = this.data.nutrients || {};

    // 1. Sage Green & Soft Cream Gradient Background
    ctx.clearRect(0, 0, 750, 1000);
    const bgGrad = ctx.createLinearGradient(0, 0, 750, 1000);
    bgGrad.addColorStop(0, '#E8EFE5'); // Soft Morandi Sage Green
    bgGrad.addColorStop(1, '#D8E2D3');
    ctx.fillStyle = bgGrad;
    ctx.fillRect(0, 0, 750, 1000);

    // 2. High-End Frosted Glass Container Card
    ctx.fillStyle = 'rgba(255, 255, 255, 0.88)';
    ctx.shadowColor = 'rgba(47, 65, 52, 0.12)';
    ctx.shadowBlur = 32;
    ctx.shadowOffsetY = 16;
    ctx.beginPath();
    this.drawRoundedRect(ctx, 40, 50, 670, 900, 36);
    ctx.fill();

    ctx.shadowColor = 'transparent';
    ctx.shadowBlur = 0;
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.9)';
    ctx.lineWidth = 2;
    ctx.stroke();

    // 3. Header Text & Minimalist Lifestyle Tag
    ctx.font = 'bold 36px sans-serif';
    ctx.fillStyle = '#2D3A31';
    ctx.fillText('咔嚓算卡 · 极简健康日记', 80, 125);

    ctx.font = '22px sans-serif';
    ctx.fillStyle = '#5A6E60';
    ctx.fillText('HEALTHY LIFESTYLE JOURNAL · ' + (this.data.currentDateStr || ''), 80, 165);

    // 4. Polaroid Photo Frame with food image
    ctx.fillStyle = '#FFFFFF';
    ctx.shadowColor = 'rgba(0, 0, 0, 0.08)';
    ctx.shadowBlur = 16;
    ctx.beginPath();
    this.drawRoundedRect(ctx, 80, 195, 590, 440, 24);
    ctx.fill();
    ctx.shadowColor = 'transparent';

    if (bgImg) {
      ctx.save();
      ctx.beginPath();
      this.drawRoundedRect(ctx, 95, 210, 560, 360, 16);
      ctx.clip();
      ctx.drawImage(bgImg, 95, 210, 560, 360);
      ctx.restore();
    } else {
      // 无照片时绘制精美莫兰迪风格装饰区域
      const artGrad = ctx.createLinearGradient(95, 210, 95, 570);
      artGrad.addColorStop(0, '#D4DDD0');
      artGrad.addColorStop(0.5, '#C8D5C3');
      artGrad.addColorStop(1, '#BCC9B6');
      ctx.fillStyle = artGrad;
      ctx.beginPath();
      this.drawRoundedRect(ctx, 95, 210, 560, 360, 16);
      ctx.fill();

      // 装饰性细线框
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.5)';
      ctx.lineWidth = 2;
      ctx.beginPath();
      this.drawRoundedRect(ctx, 115, 230, 520, 320, 12);
      ctx.stroke();

      // 居中食物 emoji 装饰
      ctx.font = '60px sans-serif';
      ctx.fillText('🥗', 310, 350);

      // 优雅提示文案
      ctx.font = 'bold 26px sans-serif';
      ctx.fillStyle = '#4A5E4F';
      ctx.fillText('记录每一餐的精致美好', 220, 420);

      ctx.font = '20px sans-serif';
      ctx.fillStyle = '#6B7F70';
      ctx.fillText('点击海报下方「📷 换照片」上传美食图', 175, 460);

      // 四角装饰点
      ctx.fillStyle = 'rgba(255, 255, 255, 0.6)';
      ctx.beginPath(); ctx.arc(135, 250, 4, 0, 2 * Math.PI); ctx.fill();
      ctx.beginPath(); ctx.arc(615, 250, 4, 0, 2 * Math.PI); ctx.fill();
      ctx.beginPath(); ctx.arc(135, 530, 4, 0, 2 * Math.PI); ctx.fill();
      ctx.beginPath(); ctx.arc(615, 530, 4, 0, 2 * Math.PI); ctx.fill();
    }

    // Photo Caption Inside Polaroid
    ctx.font = 'bold 24px sans-serif';
    ctx.fillStyle = '#334155';
    ctx.fillText('今日热量摄入 ' + consumedCal + ' / ' + targetCal + ' kcal', 105, 605);

    // 5. Macro Metrics Pills
    const proteinG = nutrients.protein || 0;
    const carbG = nutrients.carbs || 0;
    const fatG = nutrients.fat || 0;
    const macroStr = `蛋白 ${proteinG}g · 碳水 ${carbG}g · 脂肪 ${fatG}g`;

    ctx.fillStyle = '#F1F5F0';
    ctx.beginPath();
    this.drawRoundedRect(ctx, 80, 660, 590, 54, 16);
    ctx.fill();

    ctx.font = 'bold 22px sans-serif';
    ctx.fillStyle = '#3D5243';
    ctx.fillText('营养结构: ' + macroStr, 105, 695);

    // 6. User Avatar & Profile Bar (Bottom Left)
    if (avatarImg) {
      ctx.save();
      ctx.beginPath();
      ctx.arc(120, 785, 36, 0, 2 * Math.PI);
      ctx.clip();
      ctx.drawImage(avatarImg, 84, 749, 72, 72);
      ctx.restore();
      
      ctx.strokeStyle = '#FFFFFF';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(120, 785, 36, 0, 2 * Math.PI);
      ctx.stroke();
    }

    ctx.font = 'bold 26px sans-serif';
    ctx.fillStyle = '#1E293B';
    ctx.fillText(nickname, 170, 775);

    ctx.font = '20px sans-serif';
    ctx.fillStyle = '#64748B';
    ctx.fillText('与 10,000+ 伙伴一起自律打卡', 170, 805);

    // 7. QR Code (Bottom Right)
    if (qrImg) {
      ctx.drawImage(qrImg, 530, 735, 140, 140);
      ctx.font = '18px sans-serif';
      ctx.fillStyle = '#64748B';
      ctx.fillText('微信扫码记餐', 550, 895);
    }
  },

  drawVoguePoster(canvas, ctx, bgImg, qrImg, avatarImg) {
    const user = app.globalData.userInfo;
    const nickname = user && user.nickname ? user.nickname : '自律达人';
    const consumedCal = this.data.consumedCal || 0;
    const targetCal = this.data.targetCal || 2000;
    const nutrients = this.data.nutrients || {};

    ctx.clearRect(0, 0, 750, 1000);

    // 1. Full-bleed background food photo or luxury dark gradient
    if (bgImg) {
      ctx.drawImage(bgImg, 0, 0, 750, 1000);
      // Dark vignette overlay
      const grad = ctx.createLinearGradient(0, 0, 0, 1000);
      grad.addColorStop(0, 'rgba(15, 23, 42, 0.7)');
      grad.addColorStop(0.4, 'rgba(15, 23, 42, 0.3)');
      grad.addColorStop(1, 'rgba(15, 23, 42, 0.85)');
      ctx.fillStyle = grad;
      ctx.fillRect(0, 0, 750, 1000);
    } else {
      const grad = ctx.createLinearGradient(0, 0, 750, 1000);
      grad.addColorStop(0, '#0F172A');
      grad.addColorStop(1, '#1E293B');
      ctx.fillStyle = grad;
      ctx.fillRect(0, 0, 750, 1000);
    }

    // 2. High Fashion Vogue Serif Magazine Header
    ctx.font = 'bold 44px sans-serif';
    ctx.fillStyle = '#FFFFFF';
    ctx.fillText('SHIKE HEALTH EDITORIAL', 65, 110);

    ctx.font = 'bold 20px sans-serif';
    ctx.fillStyle = 'rgba(255, 255, 255, 0.7)';
    ctx.fillText('ISSUE N°' + (this.data.currentDateStr || '2026.08') + ' · DAILY CALORIE COVER', 65, 150);

    // Separator Thin Line
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.25)';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(65, 175);
    ctx.lineTo(685, 175);
    ctx.stroke();

    // 3. Huge Bold Numeric Badge
    ctx.font = 'bold 100px sans-serif';
    ctx.fillStyle = '#FFFFFF';
    ctx.fillText(String(consumedCal), 65, 300);

    ctx.font = 'bold 26px sans-serif';
    ctx.fillStyle = '#38BDF8';
    ctx.fillText('KCAL CONSUMED / TARGET ' + targetCal + ' KCAL', 65, 345);

    // 4. Translucent Frosted Glass Card for Nutrition Details
    ctx.fillStyle = 'rgba(255, 255, 255, 0.15)';
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.3)';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    this.drawRoundedRect(ctx, 65, 400, 620, 280, 24);
    ctx.fill();
    ctx.stroke();

    ctx.font = 'bold 26px sans-serif';
    ctx.fillStyle = '#FFFFFF';
    ctx.fillText('NUTRITIONAL BALANCE ANALYTICS', 95, 455);

    const proteinG = nutrients.protein || 0;
    const carbG = nutrients.carbs || 0;
    const fatG = nutrients.fat || 0;

    ctx.font = '24px sans-serif';
    ctx.fillStyle = 'rgba(255, 255, 255, 0.9)';
    ctx.fillText(`• PROTEIN (蛋白质): ${proteinG} g`, 95, 510);
    ctx.fillText(`• CARBOHYDRATE (碳水): ${carbG} g`, 95, 560);
    ctx.fillText(`• FAT (优质脂肪): ${fatG} g`, 95, 610);

    // 5. Bottom Author & QR Bar
    ctx.fillStyle = 'rgba(15, 23, 42, 0.8)';
    ctx.beginPath();
    this.drawRoundedRect(ctx, 65, 730, 620, 200, 24);
    ctx.fill();

    if (avatarImg) {
      ctx.save();
      ctx.beginPath();
      ctx.arc(125, 830, 40, 0, 2 * Math.PI);
      ctx.clip();
      ctx.drawImage(avatarImg, 85, 790, 80, 80);
      ctx.restore();
      ctx.strokeStyle = '#38BDF8';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(125, 830, 40, 0, 2 * Math.PI);
      ctx.stroke();
    }

    ctx.font = 'bold 28px sans-serif';
    ctx.fillStyle = '#FFFFFF';
    ctx.fillText(nickname, 185, 815);

    ctx.font = '20px sans-serif';
    ctx.fillStyle = '#94A3B8';
    ctx.fillText('咔嚓算卡 · AI 智能营养师专属诊断', 185, 855);

    if (qrImg) {
      ctx.drawImage(qrImg, 525, 760, 140, 140);
    }
  },

  drawReceiptPoster(canvas, ctx, bgImg, qrImg, avatarImg) {
    const user = app.globalData.userInfo;
    const nickname = user && user.nickname ? user.nickname : '自律达人';
    const consumedCal = this.data.consumedCal || 0;
    const targetCal = this.data.targetCal || 2000;
    const nutrients = this.data.nutrients || {};

    ctx.clearRect(0, 0, 750, 1000);
    ctx.fillStyle = '#E2E8F0';
    ctx.fillRect(0, 0, 750, 1000);

    ctx.fillStyle = '#FFFFFF';
    ctx.shadowColor = 'rgba(15, 23, 42, 0.12)';
    ctx.shadowBlur = 24;
    ctx.beginPath();
    this.drawRoundedRect(ctx, 55, 45, 640, 910, 16);
    ctx.fill();

    ctx.shadowColor = 'transparent';
    ctx.font = 'bold 36px monospace';
    ctx.fillStyle = '#0F172A';
    ctx.fillText('=== CALORIE BILL RECORD ===', 100, 120);

    ctx.font = '22px monospace';
    ctx.fillStyle = '#475569';
    ctx.fillText('STORE: 咔嚓算卡 HEALTH LAB', 100, 170);
    ctx.fillText('CUSTOMER: ' + nickname, 100, 205);
    ctx.fillText('DATE: ' + (this.data.currentDateStr || ''), 100, 240);
    ctx.fillText('-----------------------------------', 100, 275);

    ctx.font = 'bold 26px monospace';
    ctx.fillStyle = '#1E293B';
    ctx.fillText('ITEM               QTY    CALORIES', 100, 320);
    ctx.font = '22px monospace';
    ctx.fillStyle = '#475569';
    ctx.fillText('蛋白质 (PROTEIN)    ' + (nutrients.protein || 0) + 'g    ' + ((nutrients.protein||0)*4) + ' kcal', 100, 365);
    ctx.fillText('碳水化合物 (CARB)  ' + (nutrients.carbs || 0) + 'g    ' + ((nutrients.carbs||0)*4) + ' kcal', 100, 410);
    ctx.fillText('优质脂肪 (FAT)      ' + (nutrients.fat || 0) + 'g    ' + ((nutrients.fat||0)*9) + ' kcal', 100, 455);
    ctx.fillText('-----------------------------------', 100, 505);

    ctx.font = 'bold 30px monospace';
    ctx.fillStyle = '#0F172A';
    ctx.fillText('TOTAL CONSUMED: ' + consumedCal + ' KCAL', 100, 555);
    ctx.fillText('BUDGET TARGET:  ' + targetCal + ' KCAL', 100, 600);

    ctx.font = '22px monospace';
    ctx.fillStyle = (consumedCal <= targetCal) ? '#059669' : '#DC2626';
    ctx.fillText('STATUS: ' + ((consumedCal <= targetCal) ? '✓ 赤字达标 HEALTHY' : '⚠ 热量超标 DEFICIT OVER'), 100, 645);
    ctx.fillText('-----------------------------------', 100, 685);

    if (qrImg) {
      ctx.drawImage(qrImg, 490, 720, 160, 160);
    }

    ctx.font = '20px monospace';
    ctx.fillStyle = '#64748B';
    ctx.fillText('THANK YOU FOR BEING DISCIPLINED!', 100, 760);
    ctx.fillText('SCAN QR CODE TO JOIN US', 100, 800);
  },

  onShareAppMessage() {
    const user = app.globalData.userInfo;
    const nickname = user && user.nickname ? user.nickname : '自律达人';
    return {
      title: `🥗 ${nickname}的今日卡路里膳食记录，拍照算卡，健康减脂！`,
      path: '/pages/index/index',
      imageUrl: this.data.tempPosterPath || ''
    };
  },

  onShareTimeline() {
    const user = app.globalData.userInfo;
    const nickname = user && user.nickname ? user.nickname : '自律达人';
    return {
      title: `🥗 ${nickname}的今日卡路里膳食打卡，AI 智能算卡，快来和我一起自律！`,
      query: '',
      imageUrl: this.data.tempPosterPath || ''
    };
  },

  onSharePosterClick() {
    const filePath = this.data.tempPosterPath;
    if (!filePath) {
      wx.showToast({ title: '海报生成中，请稍候', icon: 'none' });
      return;
    }
    if (wx.showShareImageMenu) {
      wx.showShareImageMenu({
        path: filePath,
        fail: () => {
          wx.showToast({ title: '已自动保存相册，可直接在微信中发送朋友圈', icon: 'none' });
        }
      });
    } else {
      wx.showToast({ title: '已自动保存相册，可直接在微信中发送朋友圈', icon: 'none' });
    }
  }
})
