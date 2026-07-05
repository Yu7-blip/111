import request from './request'

export function login(data) {
  return request.post('/admin/login', data)
}

export function logout() {
  return request.post('/admin/logout')
}
