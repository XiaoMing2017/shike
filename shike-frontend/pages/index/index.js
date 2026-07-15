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
    ]
  },

  onLoad(options) {
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
    // 1. Set calorie targets
    const targetCal = user.targetCalories || 2000;
    const showProfileGuide = !user.age || user.age === 0;
    this.setData({
      targetCal,
      showProfileGuide,
      avatarUrl: user.avatarUrl || '/images/profile.png',
      'nutrients.targetCarbs': Math.round(targetCal * 0.5 / 4),      // 50% carbs
      'nutrients.targetProtein': Math.round(targetCal * 0.2 / 4),    // 20% protein
      'nutrients.targetFat': Math.round(targetCal * 0.3 / 9)         // 30% fat
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

    // Formula: netCalories = consumed - exercise
    const netCalories = consumedCalVal - exerciseCalVal;

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

    this.setData({
      consumedCal: consumedCalVal,
      remainingCal: displayCal,
      isOverLimit,
      ringLabel,
      progressPercent,
      meals: updatedMeals,
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
            gradient.addColorStop(0, '#F87171');
            gradient.addColorStop(0.58, '#FB7185');
            gradient.addColorStop(1, '#EF4444');
          } else {
            gradient.addColorStop(0, '#2BB7C6');
            gradient.addColorStop(0.58, '#54D8A6');
            gradient.addColorStop(1, '#A8E86A');
          }

          ctx.shadowColor = isOverLimit ? 'rgba(239, 68, 68, 0.28)' : 'rgba(45, 212, 191, 0.34)';
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
    let total = 0;
    items.forEach(item => {
      total += parseFloat(item.calories) || 0;
    });
    this.setData({
      aiResultCalories: total
    });
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
  }
})
