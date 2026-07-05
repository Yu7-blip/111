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

export function getMerchantList(params) {
  return request.get('/admin/merchants', { params: cleanParams(params) })
}

export function getMerchantDetail(id) {
  return request.get(`/admin/merchants/${id}`)
}

export function auditMerchant(id, status, remark) {
  return request.post(`/admin/merchants/${id}/audit`, { status, remark })
}

export function updateMerchant(id, data) {
  return request.put(`/admin/merchants/${id}`, data)
}

export function addMerchant(data) {
  return request.post('/admin/merchants', data)
}

export function deleteMerchant(id) {
  return request.delete(`/admin/merchants/${id}`)
}
