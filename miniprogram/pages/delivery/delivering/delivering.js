import { http } from '../../../utils/request';
import { startLocationReport, stopLocationReport } from '../../../utils/location';
import { openNavigation } from '../../../utils/util';

Page({
  data: {
    latitude: null,       // 骑手当前纬度（GPS获取，不再用硬编码）
    longitude: null,      // 骑手当前经度
    remainingDistance: '--',
    remainingTime: '--',
    recordId: '',
    taskStatus: 'pickup',  // 'pickup' → 去取餐, 'delivering' → 去配送
    orderInfo: { orderNo: '', shopName: '', shopAddress: '', shopPhone: '', userName: '', userAddress: '', userPhone: '' },
    markers: [],
    polyline: [],
    deliveryLat: null,     // 客户地址纬度
    deliveryLng: null,     // 客户地址经度
    shopLat: null,         // 商家纬度
    shopLng: null,         // 商家经度
    noDeliveryCoords: false, // 客户地址无坐标时的标记
    // 导航增强
    nextInstruction: '',
    steps: [],
    currentStepIndex: 0,
    totalSteps: 0,
    lastStepLat: null,
    lastStepLng: null
  },

  onLoad(options) {
    const id = options.id;
    if (id) {
      this.setData({ recordId: id });
      // 先获取任务数据，完成后再获取GPS和加载路线
      this.fetchTaskDetail(id, () => {
        this.getRiderLocation();
      });
    }
    startLocationReport();
    this.startLiveTracking();
  },

  onHide() {
    stopLocationReport();
    this.stopLiveTracking();
  },

  onUnload() {
    stopLocationReport();
    this.stopLiveTracking();
  },

  onShow() {
    if (this.data.recordId) {
      startLocationReport();
      this.startLiveTracking();
      // 重新获取任务数据（状态可能已变更，如取餐→配送）
      this.fetchTaskDetail(this.data.recordId, () => {
        this.getRiderLocation();
      });
    }
  },

  // ==================== 实时位置追踪 ====================

  startLiveTracking() {
    this.stopLiveTracking();
    this._trackTimerId = setInterval(() => {
      this.refreshPosition();
    }, 15000);
  },

  stopLiveTracking() {
    if (this._trackTimerId) {
      clearInterval(this._trackTimerId);
      this._trackTimerId = null;
    }
  },

  refreshPosition() {
    wx.getLocation({
      type: 'gcj02',
      success: (res) => {
        const newLat = res.latitude;
        const newLng = res.longitude;

        const markers = this.data.markers.map(m => {
          if (m.id === 0) return { ...m, latitude: newLat, longitude: newLng };
          return m;
        });

        this.setData({ latitude: newLat, longitude: newLng, markers });
        this.recalcETA(newLat, newLng);
        this.checkStepProgress(newLat, newLng);
      },
      fail: () => {}
    });
  },

  // ==================== 任务详情 + 路线 ====================

  getRiderLocation() {
    wx.getLocation({
      type: 'gcj02',
      success: (res) => {
        this.setData({ latitude: res.latitude, longitude: res.longitude });
        this.loadRoutes(res.latitude, res.longitude);
      },
      fail: () => {
        // GPS 获取失败，尝试用已缓存的上一轮位置
        if (this.data.latitude && this.data.longitude) {
          this.loadRoutes(this.data.latitude, this.data.longitude);
        } else {
          wx.showToast({ title: '请开启GPS定位', icon: 'none' });
        }
      }
    });
  },

  fetchTaskDetail(id, callback) {
    http.get('/wx/delivery/tasks').then(res => {
      const allTasks = [...(res.pickupTasks || []), ...(res.deliveringTasks || [])];
      const task = allTasks.find(t => t.id == id);
      if (task) {
        // 商家坐标：必须来自数据库，无兜底
        const shopLat = task.shopLat || null;
        const shopLng = task.shopLng || null;
        // 客户地址坐标：可能为 null（地址未定位的情况）
        const deliveryLat = task.deliveryLat || null;
        const deliveryLng = task.deliveryLng || null;
        const noDeliveryCoords = !deliveryLat || !deliveryLng;

        let distStr = task.deliveryDistance || '--';
        if (typeof distStr === 'string') distStr = distStr.replace('km', '');

        this.setData({
          taskStatus: task.status || 'pickup',
          orderInfo: {
            orderNo: task.orderNo || '',
            shopName: task.shopName || '',
            shopAddress: task.shopAddress || '',
            shopPhone: task.shopPhone || '',
            userName: task.userName || '',
            userAddress: task.deliveryAddress || '',
            userPhone: task.userPhone || ''
          },
          remainingDistance: distStr,
          remainingTime: task.estimateTime || '--',
          shopLat: shopLat,
          shopLng: shopLng,
          deliveryLat: deliveryLat,
          deliveryLng: deliveryLng,
          noDeliveryCoords: noDeliveryCoords
        });

        // 检查坐标情况
        if (!shopLat || !shopLng) {
          console.error('[delivering] 商家缺少坐标! shopId=', task.shopName);
          wx.showToast({ title: '商家位置缺失，请联系平台', icon: 'none' });
        }
        if (noDeliveryCoords) {
          console.warn('[delivering] 客户地址缺少坐标，地址文本:', task.deliveryAddress);
        }
      } else {
        console.error('[delivering] 未找到任务 id=', id);
      }
      // 数据加载完毕，执行回调（触发路线加载）
      if (callback) callback();
    }).catch(() => {
      // 即使失败也触发回调，让GPS定位继续
      if (callback) callback();
    });
  },

  /**
   * 根据当前 taskStatus 决定路线：
   *   'pickup'    → 骑手 → 商家（必需有商家坐标）
   *   'delivering' → 骑手 → 客户（必需有客户坐标，否则显示提示）
   */
  loadRoutes(riderLat, riderLng) {
    if (!riderLat || !riderLng) return;

    const shopLat = this.data.shopLat;
    const shopLng = this.data.shopLng;
    const delLat = this.data.deliveryLat;
    const delLng = this.data.deliveryLng;
    const isPickup = this.data.taskStatus === 'pickup';

    // 确定导航目标
    let targetLat, targetLng;
    if (isPickup) {
      // 取餐：必须去商家，无商家坐标则报错
      if (!shopLat || !shopLng) {
        wx.showToast({ title: '商家位置未知，无法导航', icon: 'none' });
        return;
      }
      targetLat = shopLat;
      targetLng = shopLng;
    } else {
      // 配送：必须去客户，无客户坐标则提示
      if (!delLat || !delLng) {
        // 不 fallback 到商家！显示提示
        this.setData({
          noDeliveryCoords: true,
          nextInstruction: '⚠️ 客户地址未定位，请电话联系确认地址',
          remainingDistance: '--',
          remainingTime: '--'
        });
        // 仍然展示商家的 marker
        const markers = [{
          id: 0, latitude: riderLat, longitude: riderLng,
          width: 1, height: 1,
          callout: { content: '🏍️', padding: 6, borderRadius: 20, display: 'ALWAYS', bgColor: '#ff6b6b', color: '#fff' },
          label: { content: '🏍️', fontSize: 24, anchorX: -12, anchorY: -12 }
        }];
        this.setData({ markers });
        return;
      }
      targetLat = delLat;
      targetLng = delLng;
    }

    // 构建 markers
    const markers = [{
      id: 0, latitude: riderLat, longitude: riderLng,
      width: 1, height: 1,
      callout: { content: '🏍️', padding: 6, borderRadius: 20, display: 'ALWAYS', bgColor: '#ff6b6b', color: '#fff' },
      label: { content: '🏍️', fontSize: 24, anchorX: -12, anchorY: -12 }
    }, {
      id: 1, latitude: shopLat, longitude: shopLng,
      width: 1, height: 1,
      callout: { content: '🏪 取餐', padding: 6, borderRadius: 8, display: 'ALWAYS' },
      label: { content: '🏪', fontSize: 24, anchorX: -12, anchorY: -12 }
    }];

    if (delLat && delLng) {
      markers.push({
        id: 2, latitude: delLat, longitude: delLng,
        width: 1, height: 1,
        callout: { content: '🏠 送达', padding: 6, borderRadius: 8, display: 'ALWAYS' },
        label: { content: '🏠', fontSize: 24, anchorX: -12, anchorY: -12 }
      });
    }

    this.setData({ markers, noDeliveryCoords: false });

    // 加载路线
    const routes = [];
    let pending = 1;

    http.get('/wx/delivery/route/enhanced', {
      fromLng: riderLng, fromLat: riderLat,
      toLng: targetLng, toLat: targetLat
    }).then(res => {
      if (res.polyline) {
        routes[0] = { points: res.polyline, color: isPickup ? '#409EFF' : '#ff6b6b', width: 6 };
      }
      if (res.steps && res.steps.length > 0) {
        this.setData({
          steps: res.steps,
          totalSteps: res.steps.length,
          currentStepIndex: 0,
          nextInstruction: res.steps[0].instruction || ''
        });
      } else if (res.distance != null) {
        this.setData({
          nextInstruction: isPickup ? '前往商家取餐' : '前往客户地址配送'
        });
      }
    }).catch(() => {}).finally(() => {
      pending--;
      if (pending === 0) this.finalizePolylines(routes);
    });

    // 取餐阶段额外加载第二段（商家→客户）虚线预览
    if (isPickup && delLat && delLng) {
      pending++;
      http.get('/wx/delivery/route', {
        fromLng: shopLng, fromLat: shopLat,
        toLng: delLng, toLat: delLat
      }).then(res => {
        if (res.polyline) {
          routes[1] = { points: res.polyline, color: '#999999', width: 4, dottedLine: true };
        }
      }).catch(() => {}).finally(() => {
        pending--;
        if (pending === 0) this.finalizePolylines(routes);
      });
    }
  },

  finalizePolylines(routes) {
    const polylines = routes.filter(Boolean);
    if (polylines.length === 0) {
      const points = [];
      const m = this.data.markers;
      for (let i = 0; i < m.length; i++) {
        points.push({ latitude: m[i].latitude, longitude: m[i].longitude });
      }
      if (points.length >= 2) {
        polylines.push({ points, color: '#ff6b6b', width: 6, dottedLine: true });
      }
    }
    this.setData({ polyline: polylines });
  },

  // ==================== 实时ETA ====================

  recalcETA(riderLat, riderLng) {
    const isPickup = this.data.taskStatus === 'pickup';
    let destLat, destLng;

    if (isPickup) {
      destLat = this.data.shopLat;
      destLng = this.data.shopLng;
    } else {
      destLat = this.data.deliveryLat;
      destLng = this.data.deliveryLng;
    }

    if (!destLat || !destLng) return;

    // Use local Haversine for real-time ETA (avoids API call every interval)
    const distKm = this.haversine(riderLat, riderLng, destLat, destLng);
    // Estimate: avg 20km/h biking speed
    const etaMin = Math.ceil(distKm / 20 * 60);
    this.setData({
      remainingDistance: distKm.toFixed(1),
      remainingTime: etaMin
    });
  },

  // ==================== 转弯步骤检测 ====================

  checkStepProgress(currentLat, currentLng) {
    const steps = this.data.steps;
    const idx = this.data.currentStepIndex;
    if (!steps || steps.length === 0 || idx >= steps.length) return;

    const lastLat = this.data.lastStepLat;
    const lastLng = this.data.lastStepLng;
    const step = steps[idx];

    let shouldAdvance = false;

    if (step.startLat != null && step.startLng != null) {
      const dist = this.haversine(currentLat, currentLng, step.startLat, step.startLng);
      if (dist < 0.03) shouldAdvance = true;
    } else if (lastLat != null && lastLng != null) {
      const moved = this.haversine(currentLat, currentLng, lastLat, lastLng);
      if (moved > 0.2) shouldAdvance = true;
    }

    if (shouldAdvance) {
      const nextIdx = idx + 1;
      this.setData({ lastStepLat: currentLat, lastStepLng: currentLng });
      if (nextIdx < steps.length) {
        this.setData({
          currentStepIndex: nextIdx,
          nextInstruction: steps[nextIdx].instruction || '继续直行'
        });
      } else {
        this.setData({
          nextInstruction: '📍 即将到达，准备确认送达'
        });
      }
    }

    if (lastLat == null) {
      this.setData({ lastStepLat: currentLat, lastStepLng: currentLng });
    }
  },

  haversine(lat1, lng1, lat2, lng2) {
    const R = 6371;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLng = (lng2 - lng1) * Math.PI / 180;
    const a = Math.sin(dLat / 2) ** 2
      + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180)
      * Math.sin(dLng / 2) ** 2;
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  },

  // ==================== 操作 ====================

  callPhone(e) {
    const phone = e.currentTarget.dataset.phone;
    if (phone) wx.makePhoneCall({ phoneNumber: phone });
  },

  openNavigation() {
    const isPickup = this.data.taskStatus === 'pickup';

    // 确定目的地
    let destLat, destLng, destName;
    if (isPickup) {
      destLat = this.data.shopLat;
      destLng = this.data.shopLng;
      destName = this.data.orderInfo.shopName || '商家取餐点';
    } else {
      destLat = this.data.deliveryLat;
      destLng = this.data.deliveryLng;
      destName = this.data.orderInfo.userAddress || '客户收餐点';
    }

    // 检查骑手当前位置
    if (!this.data.latitude || !this.data.longitude) {
      wx.showToast({ title: '无法获取当前位置，请开启GPS', icon: 'none' });
      return;
    }

    // 检查目的地坐标
    if (!destLat || !destLng) {
      wx.showModal({
        title: '目的地无精确坐标',
        content: '未获取到目的地GPS坐标，是否使用地址文字搜索？',
        confirmColor: '#ff6b6b',
        success: (res) => {
          if (res.confirm) {
            // 无坐标时仅展示地图位置（微信会尝试搜索地址）
            wx.openLocation({
              latitude: this.data.latitude,
              longitude: this.data.longitude,
              name: destName,
              scale: 16
            });
          }
        }
      });
      return;
    }

    // 优先走腾讯地图小程序路线规划，回退到微信内置地图
    openNavigation(
      this.data.latitude, this.data.longitude,
      destLat, destLng, destName,
      'bicycling'
    );
  },

  confirmDelivery() {
    if (this.data.taskStatus === 'pickup') {
      wx.showModal({
        title: '确认取餐',
        content: '请确认已从商家取到餐品',
        confirmColor: '#1890ff',
        success: (res) => {
          if (res.confirm) {
            wx.showLoading({ title: '提交中' });
            http.put('/wx/delivery/tasks/' + this.data.recordId + '/status?status=delivering', {}).then(() => {
              wx.hideLoading();
              wx.showToast({ title: '已取餐，开始配送', icon: 'success' });
              this.setData({ taskStatus: 'delivering', steps: [], nextInstruction: '', noDeliveryCoords: false });
              // 重新加载任务数据，获取最新的配送坐标
              this.fetchTaskDetail(this.data.recordId, () => {
                this.getRiderLocation();
              });
            }).catch(() => {
              wx.hideLoading();
              wx.showToast({ title: '提交失败', icon: 'none' });
            });
          }
        }
      });
      return;
    }

    // 配送阶段 → 确认送达
    wx.showModal({
      title: '确认送达',
      content: '请确认您已将餐品交给客户',
      confirmColor: '#ff6b6b',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '提交中' });
          http.put('/wx/delivery/tasks/' + this.data.recordId + '/status?status=completed', {}).then(() => {
            wx.hideLoading();
            stopLocationReport();
            this.stopLiveTracking();
            wx.showToast({ title: '配送完成！', icon: 'success' });
            setTimeout(() => { wx.navigateBack(); }, 1500);
          }).catch(() => {
            wx.hideLoading();
            wx.showToast({ title: '提交失败', icon: 'none' });
          });
        }
      }
    });
  }
});
