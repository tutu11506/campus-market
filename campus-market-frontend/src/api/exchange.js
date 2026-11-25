import request from './request'

// 发起换物申请（拿自己的在售商品换对方商品）
export const applyExchange = (data) => {
  return request.post('/exchange/apply', data)
}

// 我收到的换物申请
export const getReceivedExchanges = () => {
  return request.get('/exchange/received')
}

// 我发起的换物申请
export const getSentExchanges = () => {
  return request.get('/exchange/sent')
}

export const acceptExchange = (id) => {
  return request.post(`/exchange/${id}/accept`)
}

export const rejectExchange = (id) => {
  return request.post(`/exchange/${id}/reject`)
}

export const cancelExchange = (id) => {
  return request.post(`/exchange/${id}/cancel`)
}

export const finishExchange = (id) => {
  return request.post(`/exchange/${id}/finish`)
}
