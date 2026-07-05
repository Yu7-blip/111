import { http } from '../../../utils/request';
import { Storage } from '../../../utils/storage';
import { STORAGE_KEYS } from '../../../utils/constant';

const app = getApp();

Page({
  data: {
    role: '',
    userInfo: null,
    isLogin: false,
    couponCount: 0,
    riderStats: {
      balance: '0.00',
      todayOrders: 0,
      todayIncome: '0.00'
    }
  },

  onShow() {
    this.syncUserInfo();
    const role = Storage.get(STORAGE_KEYS.ROLE) || app.globalData.role || 'user';
    this.setData({ role });
    // 延迟加载非关键数据，避免阻塞页面渲染
    if (role === 'delivery') {
      setTimeout(() => this.fetchRiderStats(), 300);
    } else {
      setTimeout(() => this.fetchCouponCount(), 300);
    }
  },

  syncUserInfo() {
    this.setData({
      userInfo: app.globalData.userInfo,
      isLogin: app.globalData.isLogin
    });
  },

  fetchCouponCount() {
    http.get('/wx/coupons').then(res => {
      this.setData({ couponCount: (res || []).length });
    }).catch(() => {});
  },

  fetchRiderStats() {
    http.get('/wx/delivery/income').then(res => {
      this.setData({
        riderStats: {
          balance: res.balance || '0.00',
          todayOrders: res.todayOrders || 0,
          todayIncome: res.todayIncome || '0.00'
        }
      });
    }).catch(() => {});
  },

  goToLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  },

  goCoupons() {
    wx.navigateTo({ url: '/pages/user/coupons/coupons' });
  },

  handleMenu(e) {
    if (!this.data.isLogin) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      setTimeout(() => { this.goToLogin(); }, 800);
      return;
    }

    const type = e.currentTarget.dataset.type;
    const role = this.data.role;

    if (role === 'delivery') {
      switch(type) {
        case 'tasks':
          wx.navigateTo({ url: '/pages/delivery/tasks/tasks' });
          break;
        case 'income':
          wx.navigateTo({ url: '/pages/delivery/income/income' });
          break;
        case 'settings':
          wx.navigateTo({ url: '/pages/delivery/deliveryProfile/deliveryProfile' });
          break;
      }
      return;
    }

    // Customer menu
    switch(type) {
      case 'address':
        wx.navigateTo({ url: '/pages/user/address/address' });
        break;
      case 'favorite':
        wx.showToast({ title: '我的收藏功能开发中', icon: 'none' });
        break;
      case 'service':
        wx.navigateTo({ url: '/pages/user/feedback/feedback' });
        break;
    }
  },

  handleLogout() {
    wx.showModal({
      title: '操作确认',
      content: '确定要退出当前账号吗？',
      confirmColor: '#ff6b6b',
      success: (res) => {
        if (res.confirm) {
          app.logout();
        }
      }
    });
  }
});
