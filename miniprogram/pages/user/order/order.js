import { http } from '../../../utils/request';

Page({
  data: {
    address: null,
    cartList: [],
    shop: {},
    totalPrice: 0,
    finalPrice: '0.00',
    goodsTotal: 0,
    discountAmount: 0,
    activityDiscount: 0,
    activityName: '',
    selectedCoupon: null,
    coupons: []
  },

  onLoad() {
    this.initOrderData();
    this.fetchAddress();
    this.fetchActivity();
    this.fetchCoupons();
  },

  onShow() {
    const selected = wx.getStorageSync('selectedAddress');
    if (selected) {
      wx.removeStorageSync('selectedAddress');
      this.setData({
        address: {
          id: selected.id,
          name: selected.name,
          phone: selected.phone,
          detail: (selected.province || '') + (selected.city || '') + (selected.district || '') + (selected.detail || '')
        }
      });
    }
  },

  initOrderData() {
    const cartList = wx.getStorageSync('currentCart') || [];
    const shop = wx.getStorageSync('currentShop') || {};
    let goodsTotal = 0;
    cartList.forEach(item => {
      goodsTotal += item.price * item.count;
    });
    const deliveryFee = shop.deliveryFee || 0;
    const totalPrice = (goodsTotal + deliveryFee).toFixed(2);

    this.setData({
      cartList, shop, totalPrice,
      goodsTotal, finalPrice: totalPrice
    });
  },

  fetchAddress() {
    http.get('/wx/address').then(res => {
      const list = res || [];
      const defaultAddr = list.find(a => a.isDefault === 1) || list[0];
      if (defaultAddr) {
        this.setData({
          address: {
            id: defaultAddr.id,
            name: defaultAddr.name,
            phone: defaultAddr.phone,
            detail: (defaultAddr.province || '') + (defaultAddr.city || '') + (defaultAddr.district || '') + (defaultAddr.detail || '')
          }
        });
      }
    }).catch(() => {});
  },

  fetchActivity() {
    const shopId = this.data.shop.id;
    const amount = this.data.goodsTotal;
    if (!shopId || !amount) return;
    http.get('/wx/orders/active-activity', { shopId, amount }).then(res => {
      if (res) {
        const activityDiscount = res.reduceAmount || 0;
        this.setData({ activityDiscount, activityName: res.name });
        this.updateFinalPrice(this.data.discountAmount);
      }
    }).catch(() => {});
  },

  fetchCoupons() {
    const goodsTotal = this.data.goodsTotal;
    const amount = goodsTotal > 0 ? goodsTotal : parseFloat(this.data.totalPrice);
    const shopId = this.data.shop.id;
    http.get('/wx/coupons', { orderAmount: amount, shopId: shopId }).then(res => {
      const coupons = (res || []).map(c => ({
        ...c,
        desc: c.tip || ('满' + c.conditionAmount + '减' + c.reduceAmount),
        selected: false
      }));
      this.setData({ coupons });
    }).catch(() => {});
  },

  selectCoupon(e) {
    const index = e.currentTarget.dataset.index;
    const coupon = this.data.coupons[index];
    if (!coupon || !coupon.usable) return;

    const coupons = this.data.coupons.map((c, i) => ({
      ...c,
      selected: i === index && (c.id !== (this.data.selectedCoupon && this.data.selectedCoupon.id))
    }));

    const isDeselect = this.data.selectedCoupon && this.data.selectedCoupon.id === coupon.id;
    this.setData({
      coupons,
      selectedCoupon: isDeselect ? null : coupon,
      discountAmount: isDeselect ? 0 : coupon.reduceAmount
    });
    this.updateFinalPrice(isDeselect ? 0 : coupon.reduceAmount);
  },

  updateFinalPrice(couponDiscount) {
    const goodsTotal = this.data.goodsTotal;
    const deliveryFee = this.data.shop.deliveryFee || 0;
    const activityDiscount = this.data.activityDiscount || 0;
    const totalDiscount = (couponDiscount || 0) + activityDiscount;
    const final = Math.max(0, goodsTotal + deliveryFee - totalDiscount);
    this.setData({ finalPrice: final.toFixed(2) });
  },

  calcFinalPrice() {
    const goodsTotal = this.data.goodsTotal;
    const deliveryFee = this.data.shop.deliveryFee || 0;
    const couponDiscount = this.data.discountAmount || 0;
    const activityDiscount = this.data.activityDiscount || 0;
    const totalDiscount = couponDiscount + activityDiscount;
    const final = Math.max(0, goodsTotal + deliveryFee - totalDiscount);
    return final.toFixed(2);
  },

  goGetCoupons() {
    wx.navigateTo({ url: '/pages/user/coupons/coupons' });
  },

  chooseAddress() {
    wx.navigateTo({ url: '/pages/user/address/address?mode=select' });
  },

  submitOrder() {
    if (!this.data.address) {
      wx.showToast({ title: '请选择收货地址', icon: 'none' });
      return;
    }
    if (!this.data.shop.id) {
      wx.showToast({ title: '店铺信息异常', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '订单创建中' });

    const items = this.data.cartList.map(item => ({
      goodsId: item.id,
      count: item.count
    }));

    http.post('/wx/orders', {
      addressId: this.data.address.id,
      shopId: this.data.shop.id,
      items: items,
      remark: '',
      couponId: this.data.selectedCoupon ? this.data.selectedCoupon.id : null
    }).then(res => {
      wx.hideLoading();
      const order = res;
      const amount = order.actualAmount || this.calcFinalPrice();
      const totalDiscount = (this.data.discountAmount || 0) + (this.data.activityDiscount || 0);
      wx.navigateTo({
        url: `/pages/user/pay/pay?amount=${amount}&orderId=${order.id}&discount=${totalDiscount}`
      });
    }).catch(() => {
      wx.hideLoading();
      wx.showToast({ title: '创建订单失败', icon: 'none' });
    });
  }
});
