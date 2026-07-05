import { http } from '../../../utils/request';
import { foodImage } from '../../../utils/images';

Page({
  data: {
    orderId: '',
    order: {},
    statusDesc: '',
    rating: 0,
    comment: '',
    discountAmount: 0,
    actualAmount: 0
  },

  onLoad(options) {
    const orderId = options.id || '1';
    this.setData({ orderId });
    this.fetchOrderDetail(orderId);
  },

  fetchOrderDetail(id) {
    wx.showLoading({ title: '加载中' });

    http.get('/wx/orders/' + id).then(res => {
      wx.hideLoading();
      const order = {
        ...res,
        statusText: res.statusText || '',
        address: {
          detail: res.addressInfo || '',
          name: res.username || '',
          phone: res.userPhone || ''
        },
        goodsList: (res.goodsList || []).map(item => ({
          id: item.id,
          name: item.goodsName || '',
          price: item.goodsPrice || 0,
          count: item.count,
          image: item.image || foodImage(item.goodsName || '')
        }))
      };
      this.setData({
        order: order,
        statusDesc: this.generateStatusDesc(res.status)
      });
    }).catch(() => {
      wx.hideLoading();
      wx.showToast({ title: '加载失败', icon: 'none' });
    });
  },

  generateStatusDesc(status) {
    const map = {
      0: '请在 15 分钟内完成支付',
      1: '商家正在努力备餐中',
      2: '骑手正快马加鞭赶来，请保持电话畅通',
      3: '订单已送达，期待您的再次光临',
      4: '订单已取消，款项将原路退回',
      5: '退款中，等待商家审核',
      6: '已退款',
      7: '商家已拒绝退款，待平台裁定'
    };
    return map[status] || '';
  },

  callPhone(e) {
    const phone = e.currentTarget.dataset.phone;
    if (phone) {
      wx.makePhoneCall({ phoneNumber: phone });
    }
  },

  goToShop() {
    wx.navigateTo({
      url: `/pages/user/menu/menu?id=${this.data.order.shopId}`
    });
  },

  reorder() {
    this.goToShop();
  },

  chooseStar(e) {
    const val = e.currentTarget.dataset.val;
    this.setData({ rating: val });
  },

  onCommentInput(e) {
    this.setData({ comment: e.detail.value });
  },

  requestRefund() {
    wx.showModal({
      title: '申请退款',
      content: '确定要申请退款吗？',
      confirmColor: '#ff6b6b',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '申请中' });
          http.post('/wx/orders/' + this.data.orderId + '/refund?reason=用户申请退款').then(() => {
            wx.hideLoading();
            wx.showToast({ title: '退款申请已提交', icon: 'success' });
            this.fetchOrderDetail(this.data.orderId);
          }).catch(() => {
            wx.hideLoading();
            wx.showToast({ title: '申请失败', icon: 'none' });
          });
        }
      }
    });
  },

  submitRate() {
    if (this.data.rating === 0) {
      wx.showToast({ title: '请先打分哦', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '提交中' });

    http.post('/wx/evaluation', {
      orderId: Number(this.data.orderId),
      rating: this.data.rating,
      content: this.data.comment
    }).then(() => {
      wx.hideLoading();
      wx.showToast({ title: '评价成功', icon: 'success' });
      this.setData({ 'order.isRated': true, statusDesc: '感谢您的评价！' });
    }).catch(() => {
      wx.hideLoading();
      wx.showToast({ title: '评价失败', icon: 'none' });
    });
  }
});
