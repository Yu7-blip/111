// 角色枚举
export const ROLES = {
  USER: 'user',
  DELIVERY: 'delivery'
};

// 订单状态字典
export const ORDER_STATUS = {
  UNPAID: '待支付',
  PAID: '已支付',
  ACCEPTED: '商家接单',
  PICKING: '待取餐',
  DELIVERING: '配送中',
  FINISHED: '已完成',
  CANCELED: '已取消'
};

// 本地缓存 Key
export const STORAGE_KEYS = {
  TOKEN: 'auth_token',
  USER_INFO: 'user_info',
  ROLE: 'current_role',
  CART: 'shopping_cart'
};

// 骑手位置上报间隔（毫秒）
export const LOCATION_REPORT_INTERVAL = 5000;

// 支付方式
export const PAY_METHODS = {
  WECHAT: '微信支付',
  ALIPAY: '支付宝'
};