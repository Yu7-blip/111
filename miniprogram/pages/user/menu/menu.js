import { http } from '../../../utils/request';
import { openNavigation, openMapLocation } from '../../../utils/util';
import { foodImage } from '../../../utils/images';

Page({
  data: {
    shopInfo: {},
    categories: [],
    activeCategoryId: '',
    scrollToId: '',
    cartList: [],
    cartTotalCount: 0,
    cartTotalPrice: 0,
    shopCoupons: []
  },

  onLoad(options) {
    this.shopId = options.id || 1;
    this.cartIdMap = {};
    this.fetchMenuData(this.shopId);
    this.fetchShopCoupons(this.shopId);
  },

  onShow() {
    if (this.shopId) {
      this.fetchMenuData(this.shopId);
    }
  },

  fetchMenuData(shopId) {
    wx.showLoading({ title: '加载中' });

    Promise.all([
      http.get('/wx/shops/' + shopId + '/goods'),
      http.get('/wx/cart')
    ]).then(([menuRes, cartRes]) => {
      wx.hideLoading();

      const shop = menuRes.shop || {};
      const cartItems = cartRes || [];
      this.cartIdMap = {};
      cartItems.forEach(item => {
        if (item.shopId == shopId) {
          this.cartIdMap[item.goodsId] = item.id;
        }
      });

      const categories = (menuRes.categories || []).map(cat => ({
        id: cat.id,
        name: cat.name,
        foods: (cat.goods || []).map(g => {
          const cartCount = this.shopCartCount(cartItems, shopId, g.id);
          return {
            id: g.id,
            name: g.name,
            desc: g.desc || '',
            image: foodImage(g.name),  // 本地图片
            price: g.price,
            count: cartCount
          };
        })
      }));

      this.setData({
        shopInfo: shop,
        categories: categories,
        activeCategoryId: categories.length > 0 ? categories[0].id : ''
      });
      this.recalcCart();
    }).catch((e) => {
      wx.hideLoading();
      console.error('[menu] load failed:', e);
    });
  },

  shopCartCount(cartItems, shopId, goodsId) {
    for (const item of cartItems) {
      if (item.goodsId === goodsId && item.shopId == shopId) {
        return item.count;
      }
    }
    return 0;
  },

  switchCategory(e) {
    const id = e.currentTarget.dataset.id;
    this.setData({ activeCategoryId: id, scrollToId: id });
  },

  onFoodScroll() {},

  /* ========== 手动输入数量 ========== */

  // 点击数量数字 → 切换为输入框
  onCountTap(e) {
    const { category, food } = e.currentTarget.dataset;
    const categories = this.data.categories;

    categories.forEach(cate => {
      cate.foods.forEach(f => {
        if (f.id === food.id && cate.id === category) {
          f._editing = true;
          f._editCount = f.count;
        } else {
          f._editing = false;
        }
      });
    });

    this.setData({ categories });
  },

  // 确认输入 → 更新数量
  onCountConfirm(e) {
    const { category, food } = e.currentTarget.dataset;
    let newCount = parseInt(e.detail.value);

    if (isNaN(newCount) || newCount < 0) {
      newCount = 0;
    }
    if (newCount > 99) {
      newCount = 99;
      wx.showToast({ title: '单次最多99件', icon: 'none', duration: 1500 });
    }

    const categories = this.data.categories;
    let oldCount = 0;

    categories.forEach(cate => {
      if (cate.id === category) {
        cate.foods.forEach(f => {
          if (f.id === food.id) {
            oldCount = f.count;
            f.count = newCount;
            f._editing = false;
          }
        });
      }
    });

    this.setData({ categories });
    this.recalcCart();

    if (newCount !== oldCount) {
      this.syncCartCount(food, newCount);
    }
  },

  // 同步绝对数量到服务端
  syncCartCount(food, newCount) {
    const cartId = this.cartIdMap[food.id];

    if (newCount <= 0) {
      if (cartId) {
        http.delete('/wx/cart/' + cartId).then(() => {
          delete this.cartIdMap[food.id];
        }).catch(() => {});
      }
    } else if (!cartId) {
      http.post('/wx/cart', { goodsId: food.id, count: newCount }).then(cartIdFromServer => {
        this.cartIdMap[food.id] = cartIdFromServer;
      }).catch(() => {});
    } else {
      http.put('/wx/cart/' + cartId + '?count=' + newCount, {}).catch(() => {});
    }
  },

  updateCart(e) {
    const { category, food, type } = e.currentTarget.dataset;
    let categories = this.data.categories;

    categories.forEach(cate => {
      if (cate.id === category) {
        cate.foods.forEach(f => {
          if (f.id === food.id) {
            if (type === 'plus') f.count++;
            if (type === 'minus' && f.count > 0) f.count--;
          }
        });
      }
    });

    this.setData({ categories });
    this.recalcCart();
    this.syncCartToServer(food, type);
  },

  syncCartToServer(food, type) {
    const cartId = this.cartIdMap[food.id];
    if (type === 'plus') {
      http.post('/wx/cart', { goodsId: food.id, count: 1 }).then(cartIdFromServer => {
        this.cartIdMap[food.id] = cartIdFromServer;
      }).catch(() => {});
    } else if (type === 'minus') {
      if (!cartId) return;
      const newCount = this.getFoodCount(food.id);
      if (newCount <= 0) {
        http.delete('/wx/cart/' + cartId).then(() => {
          delete this.cartIdMap[food.id];
        }).catch(() => {});
      } else {
        http.put('/wx/cart/' + cartId + '?count=' + newCount, {}).catch(() => {});
      }
    }
  },

  getFoodCount(goodsId) {
    for (const cate of this.data.categories) {
      for (const f of cate.foods) {
        if (f.id === goodsId) return f.count;
      }
    }
    return 0;
  },

  recalcCart() {
    let totalCount = 0;
    let totalPrice = 0;
    let cartList = [];

    this.data.categories.forEach(cate => {
      cate.foods.forEach(f => {
        if (f.count > 0) {
          totalCount += f.count;
          totalPrice += f.count * f.price;
          cartList.push(f);
        }
      });
    });

    this.setData({
      cartList,
      cartTotalCount: totalCount,
      cartTotalPrice: totalPrice.toFixed(2)
    });

    if (totalCount > 0) {
      wx.setTabBarBadge({ index: 0, text: String(totalCount) });
    } else {
      wx.removeTabBarBadge({ index: 0 });
    }
  },

  fetchShopCoupons(shopId) {
    http.get('/wx/coupons/available', { shopId: shopId }).then(res => {
      this.setData({ shopCoupons: (res || []).map(c => ({ ...c, claimed: false })) });
    }).catch(() => {});
  },

  claimShopCoupon(e) {
    const { id, index } = e.currentTarget.dataset;
    wx.showLoading({ title: '领取中' });
    http.post('/wx/coupons/' + id + '/receive').then(() => {
      wx.hideLoading();
      wx.showToast({ title: '领取成功', icon: 'success' });
      const coupons = this.data.shopCoupons;
      coupons[index].claimed = true;
      this.setData({ shopCoupons: coupons });
    }).catch(() => {
      wx.hideLoading();
      wx.showToast({ title: '领取失败', icon: 'none' });
    });
  },

  goToOrder() {
    if (this.data.cartTotalPrice < (this.data.shopInfo.minPrice || 0)) return;
    wx.setStorageSync('currentCart', this.data.cartList);
    wx.setStorageSync('currentShop', this.data.shopInfo);
    wx.removeTabBarBadge({ index: 0 });
    wx.navigateTo({ url: '/pages/user/order/order' });
  },

  navigateToShop() {
    const shop = this.data.shopInfo;
    if (!shop.latitude || !shop.longitude) {
      wx.showToast({ title: '店铺位置未知', icon: 'none' });
      return;
    }

    // 获取用户当前位置 → 规划路线到店铺
    wx.getLocation({
      type: 'gcj02',
      success: (res) => {
        openNavigation(
          res.latitude, res.longitude,
          shop.latitude, shop.longitude,
          shop.name || '店铺位置',
          'walking'
        );
      },
      fail: () => {
        // 无 GPS 时仅展示店铺位置
        openMapLocation(shop.latitude, shop.longitude, shop.name || '店铺位置', shop.address || '');
      }
    });
  },

  onHide() {
    wx.removeTabBarBadge({ index: 0 });
  }
});
