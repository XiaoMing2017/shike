App({
  onLaunch() {
    console.log('ShiKe Application launched.');
    // Init local storage or settings
  },
  globalData: {
    userInfo: null,
    baseUrl: 'http://localhost:8081/api/v1'
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
        }
      }
    });
  }
})
