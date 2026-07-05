import { STORAGE_KEYS } from './constant.js';

export const Storage = {
  set(key, value) {
    try {
      wx.setStorageSync(key, value);
    } catch (e) {
      console.error('存储缓存失败', e);
    }
  },

  get(key) {
    try {
      return wx.getStorageSync(key);
    } catch (e) {
      console.error('读取缓存失败', e);
      return null;
    }
  },

  remove(key) {
    try {
      wx.removeStorageSync(key);
    } catch (e) { }
  },

  clear() {
    wx.clearStorageSync();
  },

  // 专门针对购物车的快捷方法
  getCart() {
    return this.get(STORAGE_KEYS.CART) || [];
  },
  
  setCart(cartList) {
    this.set(STORAGE_KEYS.CART, cartList);
  }
};