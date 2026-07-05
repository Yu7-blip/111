import { http } from '../../../utils/request';
import { Storage } from '../../../utils/storage';
import { STORAGE_KEYS } from '../../../utils/constant';
import { shopLogo } from '../../../utils/images';

const app = getApp();

Page({
  data: {
    role: '',
    currentTab: 'all',
    orderList: [],
    tasks: { pickupTasks: [], deliveringTasks: [] },
    isLoading: false
  },

  onShow() {
    const role = Storage.get(STORAGE_KEYS.ROLE) || app.globalData.role || 'user';
    this.setData({ role });
    if (role === 'delivery') {
      this.fetchDeliveryTasks();
    } else {
      this.fetchOrders();
    }
  },

  onPullDownRefresh() {
    this.fetchOrders(() => wx.stopPullDownRefresh());
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    if (this.data.currentTab === tab) return;
    this.setData({ currentTab: tab, orderList: [] });
    this.fetchOrders();
  },

  fetchOrders(callback) {
    this.setData({ isLoading: true });

    let tab = 0;
    if (this.data.currentTab === 'ongoing') tab = 1;
    else if (this.data.currentTab === 'history') tab = 2;

    http.get('/wx/orders', { tab: tab, page: 1, pageSize: 20 }).then(res => {
      const records = (res.records || []).map(order => ({
        id: order.id,
        shopId: order.shopId,
        shopName: order.shopName || '',
        shopLogo: shopLogo(order.shopName || ''),
        status: order.status,
        statusText: order.statusText || '',
        goodsDesc: order.goodsDesc || '',
        goodsCount: order.goodsCount || 0,
        totalPrice: order.totalPrice || '0',
        isRated: order.isRated || false
      }));
      this.setData({ orderList: records, isLoading: false });
    }).catch(() => {
      this.setData({ isLoading: false });
    });

    if (callback) callback();
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/user/orderDetail/orderDetail?id=${id}` });
  },

  preventBubble() {},

  payOrder(e) {
    const { id, price } = e.currentTarget.dataset;
    wx.navigateTo({ url: `/pages/user/pay/pay?amount=${price}&orderId=${id}` });
  },

  reorder(e) {
    const shopId = e.currentTarget.dataset.shop;
    wx.navigateTo({ url: `/pages/user/menu/menu?id=${shopId}` });
  },

  rateOrder(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/user/orderDetail/orderDetail?id=${id}` });
  },

  refundOrder(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '申请退款',
      content: '确定要申请退款吗？',
      confirmColor: '#ff6b6b',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '申请中' });
          http.post('/wx/orders/' + id + '/refund?reason=用户申请退款').then(() => {
            wx.hideLoading();
            wx.showToast({ title: '退款申请已提交', icon: 'success' });
            this.fetchOrders();
          }).catch(() => {
            wx.hideLoading();
            wx.showToast({ title: '申请失败', icon: 'none' });
          });
        }
      }
    });
  },

  // ================== 骑手端 ==================
  fetchDeliveryTasks() {
    this.setData({ isLoading: true });
    http.get('/wx/delivery/tasks').then(res => {
      this.setData({
        tasks: {
          pickupTasks: res.pickupTasks || [],
          deliveringTasks: res.deliveringTasks || []
        },
        isLoading: false
      });
    }).catch(() => {
      this.setData({ isLoading: false });
    });
  },

  updateTaskStatus(e) {
    const { id, status } = e.currentTarget.dataset;
    const label = status === 'delivering' ? '确认取餐' : '确认送达';
    wx.showModal({
      title: label,
      content: status === 'delivering' ? '确认已从商家取到餐品？' : '确认已送达顾客手中？',
      confirmColor: '#ff6b6b',
      success: (res) => {
        if (res.confirm) {
          http.put(`/wx/delivery/tasks/${id}/status?status=${status}`).then(() => {
            wx.showToast({ title: '操作成功', icon: 'success' });
            this.fetchDeliveryTasks();
          }).catch(() => {});
        }
      }
    });
  },

  openDeliveryMap(e) {
    const { id } = e.currentTarget.dataset;
    wx.navigateTo({ url: `/pages/delivery/delivering/delivering?id=${id}` });
  }
});
