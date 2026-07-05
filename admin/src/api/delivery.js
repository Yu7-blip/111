import request from './request'

function cleanParams(params) {
  const cleaned = {}
  Object.keys(params).forEach(k => {
    if (params[k] !== null && params[k] !== undefined && params[k] !== '') {
      cleaned[k] = params[k]
    }
  })
  return cleaned
}

export function getDeliveryList(params) {
  return request.get('/admin/delivery', { params: cleanParams(params) })
}

export function getDeliveryDetail(id) {
  return request.get(`/admin/delivery/${id}`)
}

export function updateDeliveryStatus(id, status) {
  return request.patch(`/admin/delivery/${id}/status`, null, { params: { status } })
}

export function reviewDeliveryVerification(id, verifyStatus, remark) {
  return request.post(`/admin/delivery/${id}/verify-review`, { verifyStatus, remark })
}
