const app = getApp();

Page({
  data: {
    remainingCal: 2000,
    targetCal: 2000,
    consumedCal: 0,
    nutrients: {
      carbs: 0,
      targetCarbs: 250,
      protein: 0,
      targetProtein: 100,
      fat: 0,
      targetFat: 65
    },
    meals: [
      {
        type: 'BREAKFAST',
        name: '早餐',
        recorded: false,
        desc: '',
        calories: 0,
        icon: '🍳'
      },
      {
        type: 'LUNCH',
        name: '午餐',
        recorded: false,
        desc: '',
        calories: 0,
        icon: '🍱'
      },
      {
        type: 'DINNER',
        name: '晚餐',
        recorded: false,
        desc: '',
        calories: 0,
        icon: '🥗'
      },
      {
        type: 'SNACK',
        name: '加餐',
        recorded: false,
        desc: '',
        calories: 0,
        icon: '🍎'
      }
    ]
  },

  onShow() {
    this.checkUserAndLoadData();
  },

  checkUserAndLoadData() {
    app.login((user) => {
      this.loadUserData(user);
    });
  },

  loadUserData(user) {
    // 1. Set calorie targets
    const targetCal = user.targetCalories || 2000;
    this.setData({
      targetCal,
      'nutrients.targetCarbs': Math.round(targetCal * 0.5 / 4),      // 50% carbs
      'nutrients.targetProtein': Math.round(targetCal * 0.2 / 4),    // 20% protein
      'nutrients.targetFat': Math.round(targetCal * 0.3 / 9)         // 30% fat
    });

    // 2. Fetch daily diet records
    const todayStr = this.getTodayDateString();
    wx.request({
      url: `${app.globalData.baseUrl}/diet/daily`,
      method: 'GET',
      data: {
        userId: user.id,
        date: todayStr
      },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const records = res.data.data;
          this.processDietRecords(records);
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

  processDietRecords(records) {
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
        updatedMeals[mealIdx].calories = record.totalCalories;
        
        // Parse food names for description
        try {
          const foodItems = JSON.parse(record.foodItems);
          updatedMeals[mealIdx].desc = foodItems.map(item => item.name).join('、');
        } catch (e) {
          updatedMeals[mealIdx].desc = '点击查看详情';
        }
      }
    }

    const remainingCal = Math.max(0, Math.round(this.data.targetCal - consumedCal));

    this.setData({
      consumedCal: Math.round(consumedCal),
      remainingCal,
      meals: updatedMeals,
      'nutrients.carbs': Math.round(carbs),
      'nutrients.protein': Math.round(protein),
      'nutrients.fat': Math.round(fat)
    });
  },

  onScanMeal(e) {
    const mealType = e.currentTarget.dataset.type;
    const user = app.globalData.userInfo;
    if (!user) {
      wx.showToast({ title: '请先登录/设置档案', icon: 'none' });
      return;
    }

    wx.showActionSheet({
      itemList: ['拍照识别 (AI)', '从相册选择 (AI)'],
      success: (res) => {
        wx.chooseMedia({
          count: 1,
          mediaType: ['image'],
          sourceType: res.tapIndex === 0 ? ['camera'] : ['album'],
          success: (mediaRes) => {
            const tempFilePath = mediaRes.tempFiles[0].tempFilePath;
            wx.showLoading({ title: 'AI 正在拍照识别...' });
            
            // 1. Upload to recognize endpoint
            wx.uploadFile({
              url: `${app.globalData.baseUrl}/diet/recognize`,
              filePath: tempFilePath,
              name: 'file',
              success: (uploadRes) => {
                wx.hideLoading();
                try {
                  const result = JSON.parse(uploadRes.data);
                  if (result.code === 200) {
                    const parsedRecord = result.data;
                    
                    // Show a simulated verification & calibration dialog
                    wx.showModal({
                      title: 'AI 识别成功',
                      content: `识别菜品:\n${this.formatFoodItems(parsedRecord.foodItems)}\n估算总热量: ${parsedRecord.totalCalories} kcal\n\n是否选择油量并保存记录？`,
                      success: (modalRes) => {
                        if (modalRes.confirm) {
                          // Prompt user to select oil level
                          wx.showActionSheet({
                            itemList: ['清淡少油 (脂肪-15%, 热量-10%)', '适中普通 (无修正)', '重油多盐 (脂肪+20%, 热量+15%)'],
                            success: (actionRes) => {
                              let oilLevel = 'MODERATE';
                              if (actionRes.tapIndex === 0) oilLevel = 'LIGHT';
                              if (actionRes.tapIndex === 2) oilLevel = 'HEAVY';
                              
                              this.saveMealRecord(user.id, mealType, parsedRecord.foodItems, oilLevel);
                            }
                          });
                        }
                      }
                    });
                  } else {
                    wx.showToast({ title: 'AI 解析失败', icon: 'error' });
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
          }
        });
      }
    });
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
  }
})
