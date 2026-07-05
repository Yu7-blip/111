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

export function getConfigList(params) {
  return request.get('/admin/configs', { params: cleanParams(params) })
}

export function getConfigByKey(key) {
  return request.get(`/admin/configs/${key}`)
}

export function createConfig(data) {
  return request.post('/admin/configs', data)
}

export function updateConfig(id, data) {
  return request.put(`/admin/configs/${id}`, data)
}

export function deleteConfig(id) {
  return request.delete(`/admin/configs/${id}`)
}
