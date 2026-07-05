import { http } from '../../../utils/request';
import { startLocationReport, stopLocationReport } from '../../../utils/location';

Page({
  data: {
    isWorking: false,
    orderList: []
  },

  onShow() {
    if (this.data.isWorking) {
      this.fetchOrders();
    }
  },

  onHide() {
    // 页面隐藏时停止上报
    stopLocationReport();
  },

  onUnload() {
    stopLocationReport();
  },

  onPullDownRefresh() {
    if (this.data.isWorking) {
      this.fetchOrders(() => wx.stopPullDownRefresh());
    } else {
      wx.stopPullDownRefresh();
    }
  },

  toggleWorkStatus(e) {
    const isWorking = e.detail.value;
    const status = isWorking ? 1 : 0;
    http.put(`/wx/delivery/status?status=${status}`).then(() => {
      this.setData({ isWorking });
      if (isWorking) {
        wx.showToast({ title: '开始听单', icon: 'success' });
        startLocationReport(); // 开始上报位置
        this.fetchOrders();
      } else {
        wx.showToast({ title: '已停止听单', icon: 'none' });
        stopLocationReport(); // 停止上报位置
        this.setData({ orderList: [] });
      }
    }).catch(() => {
      this.setData({ isWorking: !isWorking });
    });
  },

  fetchOrders(callback) {
    wx.showLoading({ title: '寻找订单中' });

    const doFetch = (lat, lng) => {
      let url = '/wx/delivery/lobby';
      if (lat != null && lng != null) {
        url += '?lat=' + lat + '&lng=' + lng;
      }
      http.get(url).then(res => {
        wx.hideLoading();
        this.setData({ orderList: res || [] });
        if (callback) callback();
      }).catch(() => {
        wx.hideLoading();
        if (callback) callback();
      });
    };

    wx.getLocation({
      type: 'gcj02',
      success: (res) => doFetch(res.latitude, res.longitude),
      fail: () => {
        wx.showToast({ title: '无法获取位置，按默认排序展示', icon: 'none', duration: 2000 });
        doFetch();
      }
    });
  },

  grabOrder(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认抢单',
      content: '抢单后不可随意取消，确定接单吗？',
      confirmColor: '#ff6b6b',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '抢单中' });
          http.post('/wx/delivery/grab/' + id, {}).then(() => {
            wx.hideLoading();
            wx.showToast({ title: '抢单成功！', icon: 'success' });
            const newList = this.data.orderList.filter(item => item.id !== id);
            this.setData({ orderList: newList });
          }).catch(() => {
            wx.hideLoading();
            wx.showToast({ title: '抢单失败', icon: 'none' });
          });
        }
      }
    });
  }
});
