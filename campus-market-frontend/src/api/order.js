import request from './request'

// 创建订单
export const createOrder = (data) => {
  return request.post('/order/create', data)
}

// 获取订单详情
export const getOrderDetail = (id) => {
  return request.get(`/order/${id}`)
}

// 获取买家订单列表（分页）
export const getBuyerOrders = (userId, params = {}) => {
  return request.get(`/order/buyer/${userId}`, { params })
}

// 获取卖家订单列表（分页）
export const getSellerOrders = (userId, params = {}) => {
  return request.get(`/order/seller/${userId}`, { params })
}

// 确认订单（卖家确认）
export const confirmOrder = (id) => {
  return request.put(`/order/confirm/${id}`)
}

// 完成订单
export const completeOrder = (id) => {
  return request.put(`/order/complete/${id}`)
}

// 模拟支付（买家）
export const payOrder = (id) => {
  return request.put(`/order/pay/${id}`)
}

// 取消订单
export const cancelOrder = (id) => {
  return request.put(`/order/cancel/${id}`)
}

// 卖家线下核销（输入买家出示的核销码）
export const verifyOrder = (id, code) => {
  return request.post(`/order/verify/${id}`, { code })
}
