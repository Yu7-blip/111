import { http } from '../../../utils/request';
import { shopLogo } from '../../../utils/images';

Page({
  data: {
    shopList: [],
    activeTab: 'default',
    page: 1,
    isLoading: false,
    hasMore: true,
    searchKey: ''
  },

  onLoad() {
    this.fetchShops();
  },

  fetchShops(isRefresh = false) {
    if (this.data.isLoading || (!this.data.hasMore && !isRefresh)) return;
    this.setData({ isLoading: true });

    const page = isRefresh ? 1 : this.data.page;

    http.get('/wx/shops', { page: page, pageSize: 10 }).then(res => {
      const records = (res.records || []).map(s => ({
        ...s,
        logo: s.logo || shopLogo(s.name)
      }));
      const newList = isRefresh ? records : [...this.data.shopList, ...records];
      this.setData({
        shopList: newList,
        isLoading: false,
        hasMore: records.length >= 10,
        page: isRefresh ? 1 : this.data.page + 1
      });
      if (isRefresh) wx.stopPullDownRefresh();
    }).catch(() => {
      this.setData({ isLoading: false });
      if (isRefresh) wx.stopPullDownRefresh();
    });
  },

  changeTab(e) {
    const tab = e.currentTarget.dataset.tab;
    if (this.data.activeTab === tab) return;
    this.setData({ activeTab: tab, shopList: [], hasMore: true, page: 1 });
    this.fetchShops(true);
  },

  onSearchInput(e) {
    this.setData({ searchKey: e.detail.value });
  },

  executeSearch() {
    this.setData({ shopList: [], hasMore: true, page: 1 });
    this.fetchShops(true);
  },

  loadMoreShops() {
    this.fetchShops();
  },

  onPullDownRefresh() {
    this.fetchShops(true);
  },

  goToMenu(e) {
    const id = e.detail.id || e.currentTarget.dataset.id;
    if (!id) return;
    wx.navigateTo({ url: `/pages/user/menu/menu?id=${id}` });
  }
});
