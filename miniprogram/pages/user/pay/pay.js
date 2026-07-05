import { http } from '../../../utils/request';

Page({
  data: {
    amount: '0.00',
    orderId: '',
    orderInfo: null,
    discount: 0,
    payMethod: '微信支付',
    paying: false
  },

  onLoad(options) {
    this.setData({
      amount: options.amount || '0.00',
      orderId: options.orderId || '',
      discount: parseFloat(options.discount || '0')
    });
    if (options.orderId) {
      this.fetchOrderInfo(options.orderId);
    }
  },

  fetchOrderInfo(orderId) {
    http.get('/wx/orders/' + orderId).then(res => {
      const goodsTotal = res.totalPrice || 0;
      const deliveryFee = res.deliveryFee || 0;
      const actualAmount = res.actualAmount || goodsTotal;
      const discount = parseFloat(goodsTotal) + parseFloat(deliveryFee) - parseFloat(actualAmount);
      this.setData({
        orderInfo: res,
        discount: discount.toFixed(2)
      });
    }).catch(() => {});
  },

  selectPayMethod(e) {
    this.setData({ payMethod: e.currentTarget.dataset.method });
  },

  mockPay() {
    const { orderId, payMethod, paying } = this.data;
    if (paying) return; // 防止重复点击

    this.setData({ paying: true });
    wx.showLoading({ title: '支付处理中...', mask: true });

    const payPromise = orderId
      ? http.post('/wx/orders/' + orderId + '/pay', { payMethod })
      : Promise.resolve();

    payPromise.then(() => {
      wx.hideLoading();
      wx.showToast({ title: '支付成功', icon: 'success', duration: 1500 });
      wx.removeStorageSync('currentCart');
      setTimeout(() => {
        wx.switchTab({ url: '/pages/user/orders/orders' });
      }, 1500);
    }).catch((err) => {
      wx.hideLoading();
      const msg = (err && err.message) || '支付失败，请重试';
      wx.showModal({
        title: '支付失败',
        content: msg,
        confirmText: '重试',
        cancelText: '取消',
        success: (res) => {
          if (res.confirm) {
            this.mockPay(); // 自动重试
          }
        }
      });
    }).finally(() => {
      this.setData({ paying: false });
    });
  }
});
