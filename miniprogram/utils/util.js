// 格式化时间 (例如：2026-05-12 14:30:00)
export const formatTime = (date) => {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours()
  const minute = date.getMinutes()
  const second = date.getSeconds()

  return (
    [year, month, day].map(formatNumber).join('-') +
    ' ' +
    [hour, minute, second].map(formatNumber).join(':')
  )
}

const formatNumber = (n) => {
  n = n.toString()
  return n[1] ? n : '0' + n
}

// 计算两点之间的距离 (Haversine 公式，外卖骑手端计算距离必备)
export const getDistance = (lat1, lng1, lat2, lng2) => {
  const radLat1 = lat1 * Math.PI / 180.0;
  const radLat2 = lat2 * Math.PI / 180.0;
  const a = radLat1 - radLat2;
  const b = lng1 * Math.PI / 180.0 - lng2 * Math.PI / 180.0;
  let s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
    Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
  s = s * 6378.137; // 地球半径
  s = Math.round(s * 10000) / 10000;
  return s.toFixed(1); // 返回公里数，保留1位小数
}

// ==================== 导航工具 ====================

/** 腾讯地图小程序 AppId */
const QQMAP_APPID = 'wx7643d5f831302ab0';

/**
 * 打开路线导航（优先跳腾讯地图小程序做真实导航，回退到微信内置地图）
 *
 * 微信的 wx.openLocation() 只能看静态点位，用户点"导航"按钮时，
 * 微信内部通过 qqmap:// URL Scheme 唤起腾讯地图 App。
 * 在开发者工具上 qqmap:// 无法识别导致报错。
 *
 * 此函数优先使用 wx.navigateToMiniProgram 直接跳到腾讯地图小程序的路线规划页，
 * 失败时回退到 wx.openLocation()（手机上仍然有效）。
 *
 * @param {number} fromLat 起点纬度（骑手/用户当前位置）
 * @param {number} fromLng 起点经度
 * @param {number} toLat   终点纬度
 * @param {number} toLng   终点经度
 * @param {string} toName  终点名称（显示用）
 * @param {string} mode    出行方式: 'bicycling'(默认)/'driving'/'walking'/'transit'
 */
export function openNavigation(fromLat, fromLng, toLat, toLng, toName, mode) {
  mode = mode || 'bicycling';

  // 腾讯地图小程序路径：将 routeplan 参数拼在 query 里
  const path = [
    'modules/routeplan/pages/index',
    '?type=', mode,
    '&from=我的位置',
    '&fromcoord=', fromLat, ',', fromLng,
    '&to=', encodeURIComponent(toName || '目的地'),
    '&tocoord=', toLat, ',', toLng,
    '&referer=wxdelivery'
  ].join('');

  wx.navigateToMiniProgram({
    appId: QQMAP_APPID,
    path: path,
    envVersion: 'release',
    fail: () => {
      // 回退：微信内置地图（手机上用户可手动点"导航"）
      wx.openLocation({
        latitude: toLat,
        longitude: toLng,
        name: toName || '目的地',
        scale: 16
      });
    }
  });
}

/**
 * 仅用微信内置地图展示一个位置（不规划路线）。
 * 用于不需要导航、只需看位置在哪里的场景。
 */
export function openMapLocation(lat, lng, name, address) {
  wx.openLocation({
    latitude: lat,
    longitude: lng,
    name: name || '',
    address: address || '',
    scale: 16
  });
}