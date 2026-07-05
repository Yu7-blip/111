import { http } from './request';
import { LOCATION_REPORT_INTERVAL } from './constant';

// ==================== 基础定位工具 ====================
export const Location = {
  // 获取当前经纬度
  getCurrentPosition() {
    return new Promise((resolve, reject) => {
      wx.getLocation({
        type: 'gcj02',
        success: resolve,
        fail: reject
      });
    });
  },

  // 调起地图选择位置
  chooseLocation() {
    return new Promise((resolve, reject) => {
      wx.chooseLocation({
        success: resolve,
        fail: reject
      });
    });
  }
};

// ==================== 骑手位置周期上报 ====================
let reportTimerId = null;
let reportRunning = false;

/**
 * 开始周期上报骑手位置到后端（每5秒一次）
 */
export function startLocationReport() {
  if (reportRunning) return;
  reportRunning = true;
  doReport(); // 首次立即上报
  reportTimerId = setInterval(doReport, LOCATION_REPORT_INTERVAL);
}

/**
 * 停止周期上报
 */
export function stopLocationReport() {
  reportRunning = false;
  if (reportTimerId) {
    clearInterval(reportTimerId);
    reportTimerId = null;
  }
}

/**
 * 是否正在上报
 */
export function isLocationReporting() {
  return reportRunning;
}

// GPS 平滑：保存最近3个位置，过滤异常跳跃
let lastLat = null, lastLng = null;
const MIN_MOVE_THRESHOLD = 0.00001; // ~1米，小于此值视为静止抖动
const MAX_JUMP_THRESHOLD = 0.01;    // ~1km，大于此值视为GPS跳变

function doReport() {
  wx.getLocation({
    type: 'gcj02',
    success: (res) => {
      const lat = res.latitude;
      const lng = res.longitude;

      // 首次上报直接发送
      if (lastLat == null) {
        lastLat = lat;
        lastLng = lng;
        sendLocationReport(lat, lng, res.speed || 0);
        return;
      }

      // 计算与上次位置的距离（简化：用度数差）
      const dLat = Math.abs(lat - lastLat);
      const dLng = Math.abs(lng - lastLng);

      // 静止抖动：忽略极小位移
      if (dLat < MIN_MOVE_THRESHOLD && dLng < MIN_MOVE_THRESHOLD) {
        return;
      }

      // GPS跳变：超过阈值时使用上次位置做加权平滑
      if (dLat > MAX_JUMP_THRESHOLD || dLng > MAX_JUMP_THRESHOLD) {
        // 指数平滑：新位置 = 0.3*新 + 0.7*旧
        const smoothLat = lastLat * 0.7 + lat * 0.3;
        const smoothLng = lastLng * 0.7 + lng * 0.3;
        lastLat = smoothLat;
        lastLng = smoothLng;
        sendLocationReport(smoothLat, smoothLng, res.speed || 0);
        return;
      }

      lastLat = lat;
      lastLng = lng;
      sendLocationReport(lat, lng, res.speed || 0);
    },
    fail: () => {
      // GPS获取失败，继续尝试下一轮
    }
  });
}

function sendLocationReport(lat, lng, speed) {
  http.post('/wx/delivery/location', {
    lat: lat,
    lng: lng,
    speed: speed || 0
  }).catch(() => {
    // 静默失败
  });
}
