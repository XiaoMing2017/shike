App({
  onLaunch() {
    console.log('ShiKe Application launched.');
    // Init local storage or settings
  },
  globalData: {
    userInfo: null,
    baseUrl: 'http://localhost:8081/api/v1'
  }
})
