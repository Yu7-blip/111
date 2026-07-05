import { http } from '../../../utils/request';

Page({
  data: {
    myCoupons: [],
    availableCoupons: [],
    tabIndex: 0,
    loading: false
  },

  onShow() {
    this.fetchMyCoupons();
    this.fetchAvailableCoupons();
  },

  fetchMyCoupons() {
    http.get('/wx/coupons').then(res => {
      console.log('[coupons] my coupons count=', (res || []).length);
      const list = (res || []).map(c => ({
        ...c,
        desc: c.tip || ('满' + c.conditionAmount + '减' + c.reduceAmount)
      }));
      this.setData({ myCoupons: list });
    }).catch(err => {
      console.error('[coupons] fetchMyCoupons failed:', err);
    });
  },

  fetchAvailableCoupons() {
    http.get('/wx/coupons/available').then(res => {
      console.log('[coupons] available coupons count=', (res || []).length, 'sample=', res && res.length > 0 ? JSON.stringify(res[0]) : 'EMPTY');
      const list = (res || []).map(c => ({
        ...c,
        desc: '满' + c.conditionAmount + '减' + c.reduceAmount
      }));
      this.setData({ availableCoupons: list });
    }).catch(err => {
      console.error('[coupons] fetchAvailableCoupons failed:', err);
    });
  },

  switchTab(e) {
    const idx = parseInt(e.currentTarget.dataset.index, 10);
    this.setData({ tabIndex: idx });
  },

  claimCoupon(e) {
    const id = e.currentTarget.dataset.id;
    wx.showLoading({ title: '领取中' });
    http.post('/wx/coupons/' + id + '/receive').then(() => {
      wx.hideLoading();
      wx.showToast({ title: '领取成功', icon: 'success' });
      this.fetchMyCoupons();
      this.fetchAvailableCoupons();
    }).catch(() => {
      wx.hideLoading();
      wx.showToast({ title: '领取失败', icon: 'none' });
    });
  }
});
