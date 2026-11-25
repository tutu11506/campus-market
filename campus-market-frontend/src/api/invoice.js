import request from './request'

// 申请开票
export const applyInvoice = (data) => {
  return request.post('/invoice/apply', data)
}

// 我的发票
export const getMyInvoices = (params = {}) => {
  return request.get('/invoice/my', { params })
}

// 按订单查询发票（未开票返回 null）
export const getInvoiceByOrder = (orderId) => {
  return request.get('/invoice/order/' + orderId)
}

// 发票详情
export const getInvoiceDetail = (id) => {
  return request.get('/invoice/' + id)
}
