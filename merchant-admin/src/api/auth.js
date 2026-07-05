import request from './request'

export function login(data) {
  return request.post('/merchant/login', data)
}

export function getUserInfo() {
  return request.get('/merchant/userinfo')
}
