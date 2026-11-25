import request from './request'

// 求购列表（mine=true 只看自己发布的）
export const getDemandList = (params = {}) => {
  return request.get('/demand/posts', { params })
}

// 求购详情：含报价、系统匹配商品、我的报价
export const getDemandDetail = (id) => {
  return request.get('/demand/posts/' + id)
}

// 发布求购
export const createDemand = (data) => {
  return request.post('/demand/posts', data)
}

// 卖家报价
export const quoteDemand = (id, data) => {
  return request.post('/demand/posts/' + id + '/quotes', data)
}

// 发布者选定报价
export const acceptDemandQuote = (quoteId) => {
  return request.post('/demand/quotes/' + quoteId + '/accept')
}

// 关闭求购
export const closeDemand = (id) => {
  return request.post('/demand/posts/' + id + '/close')
}
