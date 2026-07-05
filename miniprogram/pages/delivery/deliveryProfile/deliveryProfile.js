const app = getApp();
import { http } from '../../../utils/request';

Page({
  data: {
    userInfo: null,
    profile: null,
    showVehicleDialog: false,
    vehicleInput: '',
    showVerifyDialog: false,
    realName: '',
    idCard: ''
  },

  onShow() {
    this.syncUserInfo();
    this.fetchProfile();
  },

  syncUserInfo() {
    this.setData({
      userInfo: app.globalData.userInfo
    });
  },

  fetchProfile() {
    http.get('/wx/delivery/profile').then(res => {
      this.setData({ profile: res });
      if (res.verifyStatus !== undefined) {
        this.setData({ 'profile.verifyStatus': res.verifyStatus });
      }
    }).catch(() => {});
  },

  handleMenu(e) {
    const type = e.currentTarget.dataset.type;
    if (type === 'vehicle') {
      this.setData({ showVehicleDialog: true });
      return;
    }
    if (type === 'verify') {
      this.setData({ showVerifyDialog: true });
      return;
    }
    if (type === 'history') {
      wx.navigateTo({ url: '/pages/delivery/income/income' });
      return;
    }
    if (type === 'evaluations') {
      wx.navigateTo({ url: '/pages/delivery/evaluations/evaluations' });
      return;
    }
    if (type === 'rule') {
      wx.navigateTo({ url: '/pages/delivery/rule/rule' });
      return;
    }
    if (type === 'service') {
      wx.navigateTo({ url: '/pages/delivery/service/service' });
      return;
    }

    wx.showToast({ title: '模块开发中', icon: 'none' });
  },

  closeVehicleDialog() {
    this.setData({ showVehicleDialog: false, vehicleInput: '' });
  },

  onVehicleInput(e) {
    this.setData({ vehicleInput: e.detail.value });
  },

  updateVehicle() {
    const vehicle = this.data.vehicleInput.trim();
    if (!vehicle) {
      wx.showToast({ title: '请输入车辆信息', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '提交中' });
    http.put('/api/wx/delivery/vehicle', { vehicle }).then(() => {
      wx.hideLoading();
      wx.showToast({ title: '车辆登记成功', icon: 'success' });
      this.setData({ showVehicleDialog: false, vehicleInput: '' });
      this.fetchProfile();
    }).catch(() => {
      wx.hideLoading();
      wx.showToast({ title: '登记失败', icon: 'none' });
    });
  },

  closeVerifyDialog() {
    this.setData({ showVerifyDialog: false, realName: '', idCard: '' });
  },

  onRealNameInput(e) {
    this.setData({ realName: e.detail.value });
  },

  onIdCardInput(e) {
    this.setData({ idCard: e.detail.value });
  },

  submitVerify() {
    const realName = this.data.realName.trim();
    const idCard = this.data.idCard.trim();
    if (!realName) {
      wx.showToast({ title: '请输入真实姓名', icon: 'none' });
      return;
    }
    if (idCard.length < 5) {
      wx.showToast({ title: '请输入证件号后5位以上', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '提交中' });
    http.post('/api/wx/delivery/verify', { realName, idCard }).then(() => {
      wx.hideLoading();
      wx.showToast({ title: '认证提交成功', icon: 'success' });
      this.setData({ showVerifyDialog: false, realName: '', idCard: '' });
      this.fetchProfile();
    }).catch(() => {
      wx.hideLoading();
      wx.showToast({ title: '认证提交失败', icon: 'none' });
    });
  },

  handleLogout() {
    wx.showModal({
      title: '操作确认',
      content: '确定要下线并退出当前骑手账号吗？',
      confirmColor: '#ff6b6b',
      success: (res) => {
        if (res.confirm) {
          // 调用 app.js 中的登出方法
          app.logout();
        }
      }
    });
  }
});