import { http } from '../../../utils/request';

Page({
  data: {
    type: 'support',
    content: '',
    submitting: false,
    feedbackList: [],
    loadingHistory: false
  },

  onShow() {
    this.fetchMyFeedback();
  },

  fetchMyFeedback() {
    this.setData({ loadingHistory: true });
    http.get('/wx/delivery/my-feedback').then(res => {
      this.setData({ feedbackList: res || [], loadingHistory: false });
    }).catch(err => {
      console.error('加载反馈历史失败', err);
      this.setData({ loadingHistory: false });
    });
  },

  setType(e) {
    this.setData({ type: e.currentTarget.dataset.type });
  },

  onInput(e) {
    this.setData({ content: e.detail.value });
  },

  submit() {
    const content = this.data.content.trim();
    if (content.length < 5) {
      wx.showToast({ title: '请至少输入5个字符', icon: 'none' });
      return;
    }
    this.setData({ submitting: true });
    wx.showLoading({ title: '提交中' });
    const payload = { type: this.data.type, content: content };
    http.post('/wx/delivery/feedback', payload).then(() => {
      wx.hideLoading();
      wx.showToast({ title: '已提交，平台会尽快处理', icon: 'success' });
      this.setData({ content: '', submitting: false });
      this.fetchMyFeedback();
    }).catch(err => {
      wx.hideLoading();
      this.setData({ submitting: false });
      console.error('[service] 提交失败:', JSON.stringify(err));
      wx.showToast({ title: err?.msg || '提交失败，请重试', icon: 'none', duration: 3000 });
    });
  }
});
