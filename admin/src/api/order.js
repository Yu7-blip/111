import request from './request'

const statusMap = {
  0: '待支付',
  1: '已支付',
  2: '配送中',
  3: '已完成',
  4: '已取消',
  5: '退款中',
  6: '已退款',
  7: '商家已拒绝'
}

export function getOrderList(params) {
  const cleaned = {}
  Object.keys(params).forEach(k => {
    if (params[k] !== null && params[k] !== undefined && params[k] !== '') {
      cleaned[k] = params[k]
    }
  })
  return request.get('/admin/orders', { params: cleaned })
}

export function getOrderDetail(id) {
  return request.get(`/admin/orders/${id}`)
}

export function cancelOrder(id) {
  return request.post(`/admin/orders/${id}/cancel`)
}

export function getRefundList(params) {
  return request.get('/admin/orders/refunds', { params })
}

export function approveRefund(id) {
  return request.post(`/admin/orders/${id}/refund/approve`)
}

export function rejectRefund(id) {
  return request.post(`/admin/orders/${id}/refund/reject`)
}

export function getOrderChildren(id) {
  return request.get(`/admin/orders/${id}/children`)
}

export { statusMap }
