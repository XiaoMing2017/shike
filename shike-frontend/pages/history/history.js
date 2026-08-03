const app = getApp();

Page({
  data: {
    currentYear: 2026,
    currentMonth: 6,
    weekdays: ['日', '一', '二', '三', '四', '五', '六'],
    calendarDays: [],
    selectedDate: '',
    selectedDateStr: '',
    userId: null,
    dayBudget: 2000,
    dayCalories: 0,
    caloriesPercent: 0,
    nutrients: {
      carbs: 0,
      protein: 0,
      fat: 0,
      carbsPercent: 0,
      proteinPercent: 0,
      fatPercent: 0
    },
    dayMeals: []
  },

  onLoad() {
    if (wx.showShareMenu) {
      wx.showShareMenu({
        withShareTicket: true,
        menus: ['shareAppMessage', 'shareTimeline']
      });
    }

    const today = new Date();
    const currentYear = today.getFullYear();
    const currentMonth = today.getMonth() + 1; // 1-indexed
    const dayStr = String(today.getDate()).padStart(2, '0');
    const monthStr = String(currentMonth).padStart(2, '0');
    const selectedDate = `${currentYear}-${monthStr}-${dayStr}`;

    this.setData({
      currentYear,
      currentMonth,
      selectedDate,
      selectedDateStr: `${currentMonth}月${today.getDate()}日`
    });

    this.loginAndInit();
  },

  onShow() {
    if (this.data.userId) {
      this.generateCalendar();
    }
  },

  loginAndInit() {
    wx.showLoading({ title: '正在载入日历...' });
    app.login((user) => {
      wx.hideLoading();
      this.setData({
        userId: user.id
      }, () => {
        this.generateCalendar();
      });
    });
  },

  generateCalendar() {
    const year = this.data.currentYear;
    const month = this.data.currentMonth;

    // Calculate first day of the month and its weekday (0-6)
    const firstDay = new Date(year, month - 1, 1);
    const startDayOfWeek = firstDay.getDay();

    // Calculate total days in this month
    const totalDays = new Date(year, month, 0).getDate();

    const calendarDays = [];

    // Empty grid cells for padding before the 1st of the month
    for (let i = 0; i < startDayOfWeek; i++) {
      calendarDays.push({
        dayNumber: '',
        dateString: '',
        status: 'EMPTY',
        totalCalories: 0,
        targetCalories: 2000
      });
    }

    // Actual day cells
    for (let i = 1; i <= totalDays; i++) {
      const dayStr = String(i).padStart(2, '0');
      const monthStr = String(month).padStart(2, '0');
      const dateString = `${year}-${monthStr}-${dayStr}`;
      calendarDays.push({
        dayNumber: i,
        dateString: dateString,
        status: 'EMPTY',
        totalCalories: 0,
        targetCalories: 2000
      });
    }

    this.setData({ calendarDays }, () => {
      this.fetchMonthSummary();
    });
  },

  fetchMonthSummary() {
    const { userId, currentYear, currentMonth } = this.data;
    if (!userId) return;

    wx.showLoading({ title: '拉取月度摘要...' });
    wx.request({
      url: `${app.globalData.baseUrl}/diet/month-summary`,
      method: 'GET',
      data: {
        userId: userId,
        year: currentYear,
        month: currentMonth
      },
      success: (res) => {
        wx.hideLoading();
        if (res.data && res.data.code === 200) {
          const summaries = res.data.data || [];
          const summaryMap = {};
          summaries.forEach(s => {
            summaryMap[s.date] = s;
          });

          const updatedDays = this.data.calendarDays.map(day => {
            if (!day.dayNumber) return day;
            const s = summaryMap[day.dateString];
            if (s) {
              return {
                ...day,
                status: s.status,
                totalCalories: Math.round(s.totalCalories),
                targetCalories: Math.round(s.targetCalories)
              };
            }
            return day;
          });

          this.setData({ calendarDays: updatedDays });

          // Load details for the currently selected date if it belongs to this month
          const monthStr = String(currentMonth).padStart(2, '0');
          if (this.data.selectedDate && this.data.selectedDate.startsWith(`${currentYear}-${monthStr}`)) {
            this.loadDailyDetails(this.data.selectedDate);
          } else {
            // Default select the first day of the displayed month
            const defaultDateString = `${currentYear}-${monthStr}-01`;
            this.selectDate(defaultDateString);
          }
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '加载摘要失败', icon: 'none' });
      }
    });
  },

  loadDailyDetails(dateString) {
    const { userId } = this.data;
    if (!userId) return;

    wx.request({
      url: `${app.globalData.baseUrl}/diet/daily`,
      method: 'GET',
      data: {
        userId: userId,
        date: dateString
      },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const records = res.data.data || [];

          let dayCalories = 0;
          let carbs = 0;
          let protein = 0;
          let fat = 0;

          const mealTypeZhMap = {
            'BREAKFAST': '早餐',
            'LUNCH': '午餐',
            'DINNER': '晚餐',
            'SNACK': '加餐'
          };

          const oilLevelZhMap = {
            'LIGHT': '清淡少油 🍃',
            'MODERATE': '适中普通 🍲',
            'HEAVY': '重油多盐 🌶️'
          };

          const dayMeals = records.map(r => {
            dayCalories += r.totalCalories;
            carbs += r.totalCarbs || 0;
            protein += r.totalProtein || 0;
            fat += r.totalFat || 0;

            let foodList = [];
            try {
              const rawList = JSON.parse(r.foodItems) || [];
              foodList = rawList.filter(food => food.name && food.name.trim() !== '');
            } catch (e) {}

            const rawDate = r.createdAt || '';
            const timeStr = rawDate.indexOf('T') !== -1 ? rawDate.split('T')[1].substring(0, 5) : '已记录';

            return {
              id: r.id,
              mealType: r.mealType,
              mealTypeZh: mealTypeZhMap[r.mealType] || '餐食',
              timeStr: timeStr,
              totalCalories: Math.round(r.totalCalories),
              oilLevel: r.oilLevel || 'MODERATE',
              oilLevelZh: oilLevelZhMap[r.oilLevel] || '普通',
              imageUrl: r.imageUrl,
              foodList: foodList
            };
          });

          // Determine daily budget: try to match calendar data, fallback to global user tdee
          let dayBudget = 2000;
          const matchingDay = this.data.calendarDays.find(d => d.dateString === dateString);
          if (matchingDay && matchingDay.targetCalories) {
            dayBudget = matchingDay.targetCalories;
          } else if (app.globalData.userInfo && app.globalData.userInfo.targetCalories) {
            dayBudget = Math.round(app.globalData.userInfo.targetCalories);
          }

          dayCalories = Math.round(dayCalories);
          carbs = Math.round(carbs);
          protein = Math.round(protein);
          fat = Math.round(fat);

          const totalGram = carbs + protein + fat;
          let carbsPercent = 0;
          let proteinPercent = 0;
          let fatPercent = 0;

          if (totalGram > 0) {
            carbsPercent = Math.round((carbs / totalGram) * 100);
            proteinPercent = Math.round((protein / totalGram) * 100);
            fatPercent = 100 - carbsPercent - proteinPercent;
          }

          const caloriesPercent = Math.min(100, Math.round((dayCalories / dayBudget) * 100));

          this.setData({
            dayMeals: dayMeals,
            dayCalories: dayCalories,
            dayBudget: dayBudget,
            caloriesPercent: caloriesPercent,
            nutrients: {
              carbs,
              protein,
              fat,
              carbsPercent,
              proteinPercent,
              fatPercent,
              targetCarbs: Math.round(dayBudget * 0.5 / 4),
              targetProtein: Math.round(dayBudget * 0.2 / 4),
              targetFat: Math.round(dayBudget * 0.3 / 9)
            }
          });
        }
      }
    });
  },

  onSelectDate(e) {
    const date = e.currentTarget.dataset.date;
    if (date) {
      this.selectDate(date);
    }
  },

  selectDate(dateString) {
    const parts = dateString.split('-');
    const month = parseInt(parts[1]);
    const day = parseInt(parts[2]);
    const selectedDateStr = `${month}月${day}日`;
    
    this.setData({
      selectedDate: dateString,
      selectedDateStr: selectedDateStr
    });

    this.loadDailyDetails(dateString);
  },

  onPrevMonth() {
    let year = this.data.currentYear;
    let month = this.data.currentMonth - 1;
    if (month < 1) {
      month = 12;
      year--;
    }
    this.setData({
      currentYear: year,
      currentMonth: month
    }, () => {
      this.generateCalendar();
    });
  },

  onNextMonth() {
    let year = this.data.currentYear;
    let month = this.data.currentMonth + 1;
    if (month > 12) {
      month = 1;
      year++;
    }
    this.setData({
      currentYear: year,
      currentMonth: month
    }, () => {
      this.generateCalendar();
    });
  },

  onPreviewImage(e) {
    const src = e.currentTarget.dataset.src;
    if (!src) return;

    let fullUrl = src;
    if (src.startsWith('/uploads')) {
      fullUrl = `${app.globalData.baseUrl.replace('/api/v1', '')}${src}`;
    }

    wx.previewImage({
      urls: [fullUrl],
      current: fullUrl
    });
  },

  onGeneratePoster(e) {
    const meal = e.currentTarget.dataset.meal;
    wx.showModal({
      title: '海报生成提示',
      content: '您可以在【减脂对赌】页面，为今日的饮食和打卡状态一键生成并保存高质感打卡海报。确定前往对赌页面吗？',
      success: (res) => {
        if (res.confirm) {
          wx.switchTab({
            url: '/pages/team/team'
          });
        }
      }
    });
  },

  onShareAppMessage() {
    return {
      title: '📅 我的咔嚓算卡健康膳食历程，一起来打卡减脂吧！',
      path: '/pages/index/index'
    };
  },

  onShareTimeline() {
    return {
      title: '📅 咔嚓算卡 - 记录每一餐的精致与健康，自律从今天开始！',
      query: ''
    };
  }
});
