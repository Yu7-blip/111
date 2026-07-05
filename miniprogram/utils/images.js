/**
 * 图片资源路径统一管理
 * 所有图片引用必须从这里取，禁止在 WXML/JS 中硬编码路径
 *
 * 使用方式：
 *   const IMG = require('../../utils/images');
 *   page.setData({ IMG });   // 然后在 WXML 中用 {{IMG.defaults.avatar}}
 *
 *   // 或者在 JS 中直接：
 *   wx.previewImage({ urls: [IMG.defaults.avatar] });
 */

// ==================== 基础路径 ====================
const BASE = '/images';

// ==================== 后端服务器地址 ====================
// 开发环境用本地，上线时改为你的HTTPS域名
const SERVER = 'http://localhost:8080';

// 把后端返回的相对图片路径转为完整URL
// 用法: IMG.serverUrl(shop.logo) → 'http://localhost:8080/uploads/shops/shop1_logo.png'
function serverUrl(path) {
  if (!path) return '';
  if (path.startsWith('http')) return path;  // 已经是完整URL
  return SERVER + path;                       // 拼接服务器地址
}

// ==================== 默认占位图 ====================
const defaults = {
  avatar:   `${BASE}/default/avatar.png`,
  shop:     `${BASE}/default/shop.png`,
  food:     `${BASE}/default/food.png`,
  goods:    `${BASE}/default/food.png`,    // 商品图复用菜品图
  banner:   `${BASE}/default/banner.png`,  // 首页广告位占位
};

// ==================== 空状态插图 ====================
const empty = {
  general: `${BASE}/icons/empty.png`,        // 通用空状态
  order:   `${BASE}/icons/empty-order.png`,  // 暂无订单
  coupon:  `${BASE}/icons/empty-coupon.png`, // 暂无优惠券
  address: `${BASE}/icons/empty-address.png`,// 暂无地址
  search:  `${BASE}/icons/empty-search.png`, // 搜索无结果
  task:    `${BASE}/icons/empty-task.png`,   // 暂无任务
};

// ==================== TabBar 图标 ====================
const tabbar = {
  home:       `${BASE}/tabbar/home.png`,
  homeActive: `${BASE}/tabbar/home-active.png`,
  orders:     `${BASE}/tabbar/orders.png`,
  ordersActive:`${BASE}/tabbar/orders-active.png`,
  profile:    `${BASE}/tabbar/profile.png`,
  profileActive:`${BASE}/tabbar/profile-active.png`,
};

// ==================== 功能图标 ====================
const icons = {
  close:      `${BASE}/icons/close.png`,
  search:     `${BASE}/icons/search.png`,
  cart:       `${BASE}/icons/cart.png`,
  location:   `${BASE}/icons/location.png`,
  phone:      `${BASE}/icons/phone.png`,
  star:       `${BASE}/icons/star.png`,
  starFilled: `${BASE}/icons/star-filled.png`,
  coupon:     `${BASE}/icons/coupon.png`,
  rider:      `${BASE}/icons/rider.png`,
  notification:`${BASE}/icons/notification.png`,
  edit:       `${BASE}/icons/edit.png`,
  delete:     `${BASE}/icons/delete.png`,
  add:        `${BASE}/icons/add.png`,
  minus:      `${BASE}/icons/minus.png`,
  check:      `${BASE}/icons/check.png`,
  clock:      `${BASE}/icons/clock.png`,
};

// ==================== 品牌 & Logo ====================
const brand = {
  logo:       `${BASE}/logo.png`,
  wechatPay:  `${BASE}/icons/pay-wechat.png`,
  alipay:     `${BASE}/icons/pay-alipay.png`,
};

// ==================== 根据商品名匹配本地图片 ====================
// key: 商品名, value: 本地图片路径
const FOOD_IMAGE_MAP = {
  // 老北京烤鸭 (shop 1)
  '招牌北京烤鸭半只': '/images/goods/g1_roast_duck.png',
  '烤鸭整只套装':     '/images/goods/g1_roast_duck.png',
  '凉拌黄瓜':         '/images/goods/g3_cucumber.png',
  '皮蛋豆腐':         '/images/goods/g4_tofu.png',
  '双人烤鸭套餐':     '/images/goods/g1_roast_duck.png',
  '酸梅汤':           '/images/goods/g1_roast_duck.png',

  // 老王麻辣烫 (shop 2)
  '经典麻辣烫': '/images/goods/g7_malatang.png',
  '麻辣拌':     '/images/goods/g7_malatang.png',
  '肥牛卷':     '/images/goods/g9_beef.png',
  '土豆片':     '/images/goods/g10_potato.png',
  '米饭':       '/images/goods/g7_malatang.png',

  // 阿强面馆 (shop 3)
  '红烧牛肉面':      '/images/goods/g12_beef_noodle.png',
  '西红柿鸡蛋面':    '/images/goods/g13_tomato_egg_noodle.png',
  '兰州拉面':        '/images/goods/g12_beef_noodle.png',
  '卤蛋':            '/images/goods/g12_beef_noodle.png',
  '酱牛肉':          '/images/goods/g12_beef_noodle.png',
};

// 根据商品名获取本地图片，找不到返回默认图
function foodImage(name) {
  return FOOD_IMAGE_MAP[name] || defaults.food;
}

// 根据店铺名获取本地logo，找不到返回默认图
const SHOP_LOGO_MAP = {
  '老北京烤鸭（高新店）': '/images/shops/shop1_logo.png',
  '老王麻辣烫':           '/images/shops/shop2_logo.png',
  '阿强面馆':             '/images/shops/shop3_logo.png',
  '老凯里酸汤牛肉':       '/images/shops/shop5_logo.png',
};
function shopLogo(name) {
  return SHOP_LOGO_MAP[name] || defaults.shop;
}

// ==================== 导出 ====================
module.exports = {
  BASE,
  SERVER,
  serverUrl,
  defaults,
  empty,
  tabbar,
  icons,
  brand,
  foodImage,
  shopLogo,
};
