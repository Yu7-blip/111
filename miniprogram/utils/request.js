import { Storage } from './storage.js';
import { STORAGE_KEYS } from './constant.js';

const BASE_URL = 'http://localhost:8080/api';

export const request = (url, method = 'GET', data = {}) => {
  return new Promise((resolve, reject) => {
    let token = Storage.get(STORAGE_KEYS.TOKEN);
    if (!token) {
      const app = getApp();
      token = app && app.globalData && app.globalData.token;
    }
    console.log('[request] token=', token ? token.substring(0, 20) + '...' : 'NULL', 'url=', url);

    wx.request({
      url: BASE_URL + url,
      method: method,
      data: data,
      timeout: 10000,
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? 'Bearer ' + token : ''
      },
      success: (res) => {
        const { statusCode, data } = res;
        if (statusCode >= 200 && statusCode < 300) {
          if (data.code === 200) {
            resolve(data.data);
          } else {
            wx.showToast({ title: data.message || data.msg || '请求失败', icon: 'none' });
            reject(data);
          }
        } else if (statusCode === 401) {
          wx.showToast({ title: '登录已过期，请重新登录', icon: 'none' });
          const app = getApp();
          if (app && app.logout) {
            setTimeout(() => app.logout(), 1500);
          }
          reject(res);
        } else {
          wx.showToast({ title: '服务器异常', icon: 'error' });
          reject(res);
        }
      },
      fail: (err) => {
        wx.showToast({ title: '网络连接失败', icon: 'none' });
        reject(err);
      }
    });
  });
};

export const http = {
  get: (url, data) => request(url, 'GET', data),
  post: (url, data) => request(url, 'POST', data),
  put: (url, data) => request(url, 'PUT', data),
  delete: (url, data) => request(url, 'DELETE', data)
};
