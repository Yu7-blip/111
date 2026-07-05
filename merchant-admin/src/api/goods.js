import request from './request'

export function getGoodsList(params) {
  return request.get('/merchant/goods', { params })
}

export function getGoodsById(id) {
  return request.get(`/merchant/goods/${id}`)
}

export function createGoods(data) {
  return request.post('/merchant/goods', {
    name: data.name,
    categoryId: data.category,
    price: data.price,
    stock: data.stock,
    description: data.desc,
    richDesc: data.richDesc || data.desc,
    status: data.status
  })
}

export function updateGoods(id, data) {
  return request.put(`/merchant/goods/${id}`, {
    name: data.name,
    categoryId: data.category,
    price: data.price,
    stock: data.stock,
    description: data.desc,
    richDesc: data.richDesc || data.desc,
    status: data.status
  })
}

export function deleteGoods(id) {
  return request.delete(`/merchant/goods/${id}`)
}

export function toggleGoodsStatus(id, status) {
  return request.patch(`/merchant/goods/${id}/status`, null, { params: { status } })
}
