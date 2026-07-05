import request from './request'

export function getCouponList(params) {
  return request.get('/merchant/coupons', { params })
}

export function createCoupon(data) {
  return request.post('/merchant/coupons', data)
}

export function updateCoupon(id, data) {
  return request.put(`/merchant/coupons/${id}`, data)
}

export function deleteCoupon(id) {
  return request.delete(`/merchant/coupons/${id}`)
}
