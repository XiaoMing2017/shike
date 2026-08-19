App({
  onLaunch() {
    console.log('ShiKe Application launched.');
    // Init local storage or settings
  },
  globalData: {
    userInfo: null,
    // =========================================================================
    // 🌐 后端接口地址切换配置 (按需注释/取消注释即可快速切换)
    // =========================================================================
    // 1. 本地开发调试环境 (电脑本地运行后端服务)
    // baseUrl: 'http://127.0.0.1:8081/api/v1',

    // 2. 线上正线服务器环境 (云端真实 Docker 后端 117.72.61.18)
    // baseUrl: 'http://117.72.61.18:8081/api/v1',

    // 3. 线上正式域名环境 (已配置 SSL 证书的微信发版域名)
    baseUrl: 'https://shike.store/api/v1'
  },
  formatImageUrl(url) {
    const defaultAvatar = 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0';
    if (!url || url === '/images/profile.png' || url === 'tmp' || url.includes('.tmp')) {
      return defaultAvatar;
    }
    let formatted = url;
    if (formatted.startsWith('/uploads/')) {
      formatted = this.globalData.baseUrl + formatted;
    } else if (formatted.startsWith('uploads/')) {
      formatted = this.globalData.baseUrl + '/' + formatted;
    }
    if (formatted.startsWith('http://')) {
      formatted = formatted.replace('http://', 'https://');
    }
    return formatted;
  },
  login(callback) {
    if (this.globalData.userInfo) {
      if (callback) callback(this.globalData.userInfo);
      return;
    }

    wx.login({
      success: (loginRes) => {
        if (loginRes.code) {
          wx.request({
            url: `${this.globalData.baseUrl}/user/login`,
            method: 'POST',
            data: { code: loginRes.code },
            success: (res) => {
              if (res.data && res.data.code === 200) {
                const user = res.data.data;
                this.globalData.userInfo = user;
                if (callback) callback(user);
              } else {
                console.error('Server login failed, falling back to mock login', res);
                this.mockLoginFallback(callback);
              }
            },
            fail: (err) => {
              console.warn('Login request failed, falling back to mock login', err);
              this.mockLoginFallback(callback);
            }
          });
        } else {
          console.warn('wx.login failed, falling back to mock login', loginRes.errMsg);
          this.mockLoginFallback(callback);
        }
      },
      fail: (err) => {
        console.warn('wx.login call failed, falling back to mock login', err);
        this.mockLoginFallback(callback);
      }
    });
  },
  mockLoginFallback(callback) {
    const mockOpenid = 'mock_user_openid_123';
    wx.request({
      url: `${this.globalData.baseUrl}/user/login`,
      method: 'POST',
      data: { openid: mockOpenid },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const user = res.data.data;
          this.globalData.userInfo = user;
          if (callback) callback(user);
        } else {
          this.useLocalMockUser(callback);
        }
      },
      fail: () => {
        this.useLocalMockUser(callback);
      }
    });
  },
  useLocalMockUser(callback) {
    console.warn("Using local mock user fallback because backend is offline.");
    const mockUser = {
      id: 1,
      openid: 'mock_user_openid_123',
      nickname: '自律挑战者',
      avatarUrl: '/images/profile.png',
      gender: 1,
      age: 25,
      height: 175.0,
      weight: 70.0,
      activityLevel: 'SEDENTARY',
      goal: 'MAINTAIN',
      bmr: 1625.0,
      tdee: 1950.0,
      targetCalories: 1950.0,
      points: 850
    };
    this.globalData.userInfo = mockUser;
    if (callback) callback(mockUser);
  }
})
