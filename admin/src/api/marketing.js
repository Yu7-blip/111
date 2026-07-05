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

export function getActivityList(params) {
  return request.get('/admin/activities', { params: cleanParams(params) })
}

export function createActivity(data) {
  return request.post('/admin/activities', data)
}

export function updateActivity(id, data) {
  return request.put(`/admin/activities/${id}`, data)
}

export function deleteActivity(id) {
  return request.delete(`/admin/activities/${id}`)
}

export function updateActivityStatus(id, status) {
  return request.patch(`/admin/activities/${id}/status`, null, { params: { status } })
}
