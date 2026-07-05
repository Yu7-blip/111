import { Storage } from '../../utils/storage';
import { STORAGE_KEYS } from '../../utils/constant';
import { http } from '../../utils/request';
import { shopLogo } from '../../utils/images';

const app = getApp();

Page({
  data: {
    role: '',
    currentLocation: '正在定位...',
    shopList: [],
    sortBy: 'rating',
    latitude: null,
    longitude: null,
    isWorking: false,
    todayStats: { completed: 0, income: '0.00' },
    taskList: []
  },

  onShow() {
    this.checkRoleAndLoadData(false);
  },

  onPullDownRefresh() {
    this.checkRoleAndLoadData(true, () => wx.stopPullDownRefresh());
  },

  checkRoleAndLoadData(force, callback) {
    const role = Storage.get(STORAGE_KEYS.ROLE) || app.globalData.role || 'user';
    // Skip reload if role unchanged and data already loaded (tab switch)
    if (!force && role === this.data.role && this.data.shopList.length > 0 && role === 'user') {
      if (callback) callback();
      return;
    }
    if (!force && role === this.data.role && this.data.isWorking === this._lastWorkingState && role === 'delivery') {
      if (callback) callback();
      return;
    }
    this.setData({ role });
    if (role === 'user') {
      this.initUserData(callback);
    } else if (role === 'delivery') {
      this.initDeliveryData(callback);
    }
  },

  // ==================== 用户端 ====================
  initUserData(callback) {
    this.setData({ currentLocation: '正在定位...' });
    this.getLocationAndLoadShops(callback);
  },

  getLocationAndLoadShops(callback) {
    const params = { page: 1, pageSize: 10, sort: this.data.sortBy };
    const that = this;

    wx.getLocation({
      type: 'gcj02',
      success: (res) => {
        params.lat = res.latitude;
        params.lng = res.longitude;
        that.setData({ latitude: res.latitude, longitude: res.longitude, currentLocation: '已定位' });
        that.fetchShops(params, callback);
      },
      fail: () => {
        that.setData({ currentLocation: '定位失败' });
        that.fetchShops(params, callback);
      }
    });
  },

  fetchShops(params, callback) {
    http.get('/wx/shops', params).then(res => {
      const shops = (res.records || []).map(s => ({
        ...s,
        logo: s.logo || shopLogo(s.name)
      }));
      this.setData({ shopList: shops });
      if (callback) callback();
    }).catch(() => {
      if (callback) callback();
    });
  },

  switchSort(e) {
    const sort = e.currentTarget.dataset.sort;
    this.setData({ sortBy: sort });
    this.initUserData();
  },

  goToShop(e) {
    const shopId = e.detail.id;
    if (!shopId) return;
    wx.navigateTo({ url: `/pages/user/menu/menu?id=${shopId}` });
  },

  // ==================== 骑手端 ====================
  initDeliveryData(callback) {
    http.get('/wx/delivery/income').then(res => {
      this.setData({
        todayStats: {
          completed: res.todayOrders || 0,
          income: res.todayIncome || '0.00'
        }
      });
    }).catch(() => {});
    this.fetchTaskList(callback);
  },

  fetchTaskList(callback) {
    if (!this.data.isWorking) {
      this.setData({ taskList: [] });
      if (callback) callback();
      return;
    }
    http.get('/wx/delivery/lobby').then(res => {
      this.setData({ taskList: res || [] });
      if (callback) callback();
    }).catch(() => {
      if (callback) callback();
    });
  },

  toggleWorkStatus(e) {
    const isWorking = e.detail.value;
    const status = isWorking ? 1 : 0;
    http.put(`/wx/delivery/status?status=${status}`).then(() => {
      this.setData({ isWorking });
      if (isWorking) {
        wx.showToast({ title: '开始接单', icon: 'success' });
        this.fetchTaskList();
      } else {
        wx.showToast({ title: '已休息', icon: 'none' });
        this.setData({ taskList: [] });
      }
    }).catch(() => {
      this.setData({ isWorking: !isWorking });
    });
  },

  acceptOrder(e) {
    const taskId = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认抢单',
      content: '抢单后请尽快前往商家取餐',
      success: (res) => {
        if (res.confirm) {
          http.post('/wx/delivery/grab/' + taskId, {}).then(() => {
            wx.showToast({ title: '抢单成功', icon: 'success' });
            const newList = this.data.taskList.filter(item => item.id !== taskId);
            this.setData({ taskList: newList });
          }).catch(() => {
            wx.showToast({ title: '抢单失败', icon: 'none' });
          });
        }
      }
    });
  },

  handleTaskAction(e) {
    const { action, task } = e.detail;
    if (action === 'grab') {
      this.acceptOrder({ currentTarget: { dataset: { id: task.id } } });
    }
  }
});
