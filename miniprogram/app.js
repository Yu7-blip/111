App({
  globalData: {
    userInfo: null,
    token: wx.getStorageSync('auth_token') || '',
    role: wx.getStorageSync('current_role') || '',
    baseUrl: 'http://localhost:8080/api',
    isLogin: false
  },

  onLaunch: function () {
    this.checkLoginStatus();
  },

  checkLoginStatus: function () {
    const token = this.globalData.token;
    if (!token) {
      // 已在登录页，无需跳转
      return;
    }

    this.globalData.isLogin = true;
    console.log('当前登录角色:', this.globalData.role);
    // 已登录但在登录页 → 直接进首页
    const pages = getCurrentPages();
    if (pages.length > 0 && pages[pages.length - 1].route === 'pages/login/login') {
      wx.switchTab({ url: '/pages/index/index' });
    }
  },

  setLogin(data) {
    this.globalData.token = data.token;
    this.globalData.userInfo = data.userInfo;
    this.globalData.role = data.role;
    this.globalData.isLogin = true;
    wx.setStorageSync('auth_token', data.token);
    wx.setStorageSync('current_role', data.role);
    wx.setStorageSync('user_info', data.userInfo);
  },

  logout: function () {
    this.globalData.token = '';
    this.globalData.userInfo = null;
    this.globalData.role = '';
    this.globalData.isLogin = false;
    wx.clearStorageSync();
    wx.reLaunch({ url: '/pages/login/login' });
  }
})
