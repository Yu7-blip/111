import { http } from '../../../utils/request';

Page({
  data: {
    addressList: [],
    mode: 'manage' // 'select' or 'manage'
  },

  onLoad(options) {
    if (options.mode === 'select') {
      this.setData({ mode: 'select' });
    }
  },

  onShow() {
    this.fetchAddresses();
  },

  onPullDownRefresh() {
    this.fetchAddresses().then(() => wx.stopPullDownRefresh());
  },

  fetchAddresses() {
    return http.get('/wx/address').then(list => {
      this.setData({ addressList: list || [] });
    }).catch(() => {});
  },

  selectAddress(e) {
    if (this.data.mode !== 'select') return;
    const index = e.currentTarget.dataset.index;
    const addr = this.data.addressList[index];
    wx.setStorageSync('selectedAddress', addr);
    wx.navigateBack();
  },

  addAddress() {
    wx.navigateTo({ url: '/pages/user/address/edit/edit' });
  },

  editAddress(e) {
    const index = e.currentTarget.dataset.index;
    const addr = this.data.addressList[index];
    wx.navigateTo({
      url: `/pages/user/address/edit/edit?id=${addr.id}`
    });
  },

  deleteAddress(e) {
    const index = e.currentTarget.dataset.index;
    const addr = this.data.addressList[index];
    wx.showModal({
      title: '确认删除',
      content: '确定要删除该地址吗？',
      confirmColor: '#ff6b6b',
      success: (res) => {
        if (res.confirm) {
          http.delete(`/wx/address/${addr.id}`).then(() => {
            wx.showToast({ title: '已删除', icon: 'success' });
            this.fetchAddresses();
          }).catch(() => {});
        }
      }
    });
  },

  setDefault(e) {
    const index = e.currentTarget.dataset.index;
    const addr = this.data.addressList[index];
    if (addr.isDefault === 1) return;
    http.put(`/wx/address/${addr.id}`, { isDefault: 1 }).then(() => {
      wx.showToast({ title: '已设为默认', icon: 'success' });
      this.fetchAddresses();
    }).catch(() => {});
  }
});
