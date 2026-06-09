const app = getApp();

Page({
  data: {
    currentDateStr: '',
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
    ]
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

    const consumedCalVal = Math.round(consumedCal);
    const targetCalVal = Math.round(targetCal);
    let isOverLimit = false;
    let ringLabel = '剩余';
    let displayCal = 0;
    let progressPercent = 0;

    if (consumedCalVal > targetCalVal) {
      isOverLimit = true;
      ringLabel = '已超预算';
      displayCal = consumedCalVal - targetCalVal;
      progressPercent = 100; // 100% warning track
    } else {
      isOverLimit = false;
      ringLabel = '剩余';
      displayCal = targetCalVal - consumedCalVal;
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
