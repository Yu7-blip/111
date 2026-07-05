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

export function getAdminUserList(params) {
  return request.get('/admin/admins', { params: cleanParams(params) })
}

export function getAdminUserDetail(id) {
  return request.get(`/admin/admins/${id}`)
}

export function createAdminUser(data) {
  return request.post('/admin/admins', data)
}

export function updateAdminUser(id, data) {
  return request.put(`/admin/admins/${id}`, data)
}

export function deleteAdminUser(id) {
  return request.delete(`/admin/admins/${id}`)
}

export function updateAdminUserStatus(id, status) {
  return request.patch(`/admin/admins/${id}/status`, null, { params: { status } })
}
