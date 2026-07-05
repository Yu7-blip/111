import { http } from '../../../utils/request';

Page({
  data: {
    evaluations: [],
    showDialog: false,
    appealId: null,
    reason: ''
  },

  onShow() {
    this.fetchEvaluations();
  },

  fetchEvaluations() {
    http.get('/wx/delivery/evaluations').then(res => {
      this.setData({ evaluations: res || [] });
    }).catch(err => {
      console.error('获取评价列表失败', err);
      wx.showToast({ title: '加载评价失败', icon: 'none' });
    });
  },

  showAppeal(e) {
    this.setData({ showDialog: true, appealId: e.currentTarget.dataset.id, reason: '' });
  },

  closeDialog() {
    this.setData({ showDialog: false, appealId: null, reason: '' });
  },

  onInput(e) {
    this.setData({ reason: e.detail.value });
  },

  submit() {
    const reason = this.data.reason.trim();
    if (reason.length < 5) {
      wx.showToast({ title: '请至少输入5个字符', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '提交中' });
    http.post('/wx/delivery/appeal', { evaluationId: this.data.appealId, reason }).then(() => {
      wx.hideLoading();
      wx.showToast({ title: '申诉已提交', icon: 'success' });
      this.closeDialog();
    }).catch(err => {
      wx.hideLoading();
      console.error('提交申诉失败', err);
      wx.showToast({ title: err?.msg || '提交失败，请重试', icon: 'none' });
    });
  }
});