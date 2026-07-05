import request from './request'

export function getCouponList() {
  return request.get('/admin/coupons')
}

export function createCoupon(data) {
  return request.post('/admin/coupons', data)
}

export function updateCoupon(id, data) {
  return request.put(`/admin/coupons/${id}`, data)
}

export function deleteCoupon(id) {
  return request.delete(`/admin/coupons/${id}`)
}
