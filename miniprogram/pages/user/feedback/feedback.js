import { http } from '../../../utils/request';

Page({
  data: {
    type: 'complaint',
    content: '',
    submitting: false
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
    http.post('/wx/feedback', { type: this.data.type, content }).then(() => {
      wx.showToast({ title: '已提交，平台会尽快处理', icon: 'success' });
      this.setData({ content: '', submitting: false });
      setTimeout(() => wx.navigateBack(), 1000);
    }).catch(() => {
      this.setData({ submitting: false });
      wx.showToast({ title: '提交失败', icon: 'none' });
    });
  }
});