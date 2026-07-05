import request from './request'

export function getShopInfo() {
  return request.get('/merchant/shop')
}

export function updateShopInfo(data) {
  return request.put('/merchant/shop', {
    name: data.name,
    logo: data.logo,
    phone: data.phone,
    email: data.email,
    username: data.username,
    password: data.password || undefined,
    address: data.address,
    description: data.description,
    notice: data.notice,
    minPrice: data.minPrice,
    deliveryFee: data.deliveryFee,
    latitude: data.latitude,
    longitude: data.longitude,
    openTime: data.openTime,
    closeTime: data.closeTime
  })
}

export function toggleBusinessStatus(businessStatus) {
  return request.put('/merchant/shop/business-status', { businessStatus })
}
