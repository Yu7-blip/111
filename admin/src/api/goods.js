import request from './request'

export function adminGetGoodsList(params) {
  return request.get('/admin/goods', { params })
}
