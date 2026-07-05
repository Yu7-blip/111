import request from './request'

export function getDashboardStats() {
  return request.get('/admin/dashboard/stats')
}

export function getOrderTrend() {
  return request.get('/admin/dashboard/order-trend')
}
