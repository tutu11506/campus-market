import request from './request'

// 租赁物品列表（公开）
export const getRentalItems = (params = {}) => {
  return request.get('/rental/items', { params })
}

// 租赁物品详情（公开）
export const getRentalItem = (id) => {
  return request.get(`/rental/items/${id}`)
}

// 发布出租
export const publishRentalItem = (data) => {
  return request.post('/rental/items', data)
}

// 下单租用
export const createRentalOrder = (data) => {
  return request.post('/rental/orders', data)
}

// 我的租赁：role=renter 我租的 / role=lender 我出租的
export const getMyRentalOrders = (params = {}) => {
  return request.get('/rental/orders/my', { params })
}

// 出租方确认出租
export const confirmRentalOrder = (id) => {
  return request.post(`/rental/orders/${id}/confirm`)
}

// 租客发起归还
export const applyRentalReturn = (id) => {
  return request.post(`/rental/orders/${id}/return`)
}

// 出租方确认收回
export const completeRentalOrder = (id) => {
  return request.post(`/rental/orders/${id}/complete`)
}

// 取消待确认订单
export const cancelRentalOrder = (id) => {
  return request.post(`/rental/orders/${id}/cancel`)
}
