import { http } from '../../../utils/request';

Page({
  data: {
    balance: '0.00',
    todayData: { income: '0.00', count: 0 },
    incomeList: []
  },

  onLoad() {
    this.fetchIncomeDetails();
  },

  fetchIncomeDetails() {
    wx.showLoading({ title: '加载中' });

    http.get('/wx/delivery/income').then(res => {
      wx.hideLoading();
      this.setData({
        balance: res.balance || '0.00',
        todayData: {
          income: res.todayIncome || '0.00',
          count: res.todayOrders || 0
        },
        incomeList: (res.records || []).map(item => ({
          id: item.id,
          type: item.type,
          time: item.time,
          amount: item.amount,
          afterBalance: item.afterBalance
        }))
      });
    }).catch(() => {
      wx.hideLoading();
    });
  },

  handleWithdraw() {
    if (parseFloat(this.data.balance) <= 0) {
      wx.showToast({ title: '余额不足', icon: 'none' });
      return;
    }

    wx.showModal({
      title: '确认提现',
      content: `确定要将 ¥${this.data.balance} 提现至微信零钱吗？`,
      confirmColor: '#ff6b6b',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '提交中' });
          http.post('/wx/delivery/withdraw', {}).then(result => {
            wx.hideLoading();
            wx.showToast({ title: '申请已提交', icon: 'success' });
            this.fetchIncomeDetails();
          }).catch(() => {
            wx.hideLoading();
          });
        }
      }
    });
  }
});
