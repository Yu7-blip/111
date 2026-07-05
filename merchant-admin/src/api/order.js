import request from './request'

export function getOrderList(params) {
  const cleaned = {}
  Object.keys(params).forEach(k => {
    if (params[k] !== null && params[k] !== undefined && params[k] !== '') {
      cleaned[k] = params[k]
    }
  })
  return request.get('/merchant/orders', { params: cleaned })
}

export function updateOrderStatus(id, status) {
  return request.patch(`/merchant/orders/${id}/status`, null, { params: { status } })
}

export function getRefundList(params) {
  const cleaned = {}
  Object.keys(params).forEach(k => {
    if (params[k] !== null && params[k] !== undefined && params[k] !== '') {
      cleaned[k] = params[k]
    }
  })
  return request.get('/merchant/orders/refunds', { params: cleaned })
}

export function approveRefund(id) {
  return request.post(`/merchant/orders/${id}/refund/approve`)
}

export function rejectRefund(id) {
  return request.post(`/merchant/orders/${id}/refund/reject`)
}

export function getOrderDetail(id) {
  return request.get(`/merchant/orders/${id}`)
}

export function getOrderChildren(id) {
  return request.get(`/merchant/orders/${id}/children`)
}

export function splitLargeOrder(id) {
  return request.post(`/merchant/orders/${id}/split`)
}
