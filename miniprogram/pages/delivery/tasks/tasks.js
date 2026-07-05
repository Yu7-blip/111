import { http } from '../../../utils/request';

Page({
  data: {
    currentTab: 'pickup',
    taskList: [],
    pickupCount: 0,
    deliveringCount: 0
  },

  onShow() {
    this.fetchTasks();
  },

  onPullDownRefresh() {
    this.fetchTasks(() => wx.stopPullDownRefresh());
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    if (this.data.currentTab === tab) return;
    this.setData({ currentTab: tab, taskList: [] });
    this.fetchTasks();
  },

  fetchTasks(callback) {
    wx.showLoading({ title: '加载中' });

    const doFetch = (lat, lng) => {
      let url = '/wx/delivery/tasks';
      if (lat != null && lng != null) {
        url += '?lat=' + lat + '&lng=' + lng;
      }
      http.get(url).then(res => {
        wx.hideLoading();
        const pickupTasks = res.pickupTasks || [];
        const deliveringTasks = res.deliveringTasks || [];
        const allTasks = [...pickupTasks, ...deliveringTasks];

        this.setData({
          taskList: allTasks.filter(t => t.status === this.data.currentTab),
          pickupCount: pickupTasks.length,
          deliveringCount: deliveringTasks.length
        });
        if (callback) callback();
      }).catch(() => {
        wx.hideLoading();
        if (callback) callback();
      });
    };

    wx.getLocation({
      type: 'gcj02',
      success: (res) => doFetch(res.latitude, res.longitude),
      fail: () => doFetch()
    });
  },

  preventBubble() {},

  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/delivery/delivering/delivering?id=${id}` });
  },

  callPhone(e) {
    const phone = e.currentTarget.dataset.phone;
    wx.makePhoneCall({ phoneNumber: phone });
  },

  openMap(e) {
    const { lat, lng, name } = e.currentTarget.dataset;
    const destLat = parseFloat(lat) || 0;
    const destLng = parseFloat(lng) || 0;
    const destName = name || '目标地点';

    if (!destLat || !destLng) {
      wx.showToast({ title: '该地点暂无坐标信息', icon: 'none' });
      return;
    }

    // 调腾讯地图小程序做路线导航
    const QQMAP_APPID = 'wx7643d5f831302ab0';
    wx.navigateToMiniProgram({
      appId: QQMAP_APPID,
      path: [
        'modules/routeplan/pages/index',
        '?type=bicycling',
        '&from=我的位置',
        '&fromcoord=CurrentLocation',
        '&to=', encodeURIComponent(destName),
        '&tocoord=', destLat, ',', destLng,
        '&referer=wxdelivery'
      ].join(''),
      envVersion: 'release',
      fail: () => {
        wx.openLocation({
          latitude: destLat,
          longitude: destLng,
          name: destName,
          scale: 16
        });
      }
    });
  },

  confirmPickup(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认取餐',
      content: '请核对餐品是否完整，确认后将进入配送中状态',
      success: (res) => {
        if (res.confirm) {
          http.put('/wx/delivery/tasks/' + id + '/status?status=delivering', {}).then(() => {
            wx.showToast({ title: '取餐成功', icon: 'success' });
            this.fetchTasks();
          }).catch(() => {
            wx.showToast({ title: '操作失败', icon: 'none' });
          });
        }
      }
    });
  },

  confirmDelivery(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认送达',
      content: '确认已将餐品安全交至客户手中？',
      success: (res) => {
        if (res.confirm) {
          http.put('/wx/delivery/tasks/' + id + '/status?status=completed', {}).then(() => {
            wx.showToast({ title: '任务完成！', icon: 'success' });
            this.fetchTasks();
          }).catch(() => {
            wx.showToast({ title: '操作失败', icon: 'none' });
          });
        }
      }
    });
  }
});
