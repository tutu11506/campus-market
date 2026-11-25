import request from './request'

// 买家申请退货（data: { orderId, reason, description, evidenceUrls }）
export const applyRefund = (data) => {
  return request.post('/refund/apply', data)
}

// 查询某订单的退货申请记录（买家或卖家可查）
export const getRefundsByOrder = (orderId) => {
  return request.get(`/refund/order/${orderId}`)
}

// 卖家的退货审核列表（分页）
export const getSellerRefunds = (params = {}) => {
  return request.get('/refund/seller', { params })
}

// 卖家同意退货
export const approveRefund = (id) => {
  return request.put(`/refund/approve/${id}`)
}

// 卖家拒绝退货（rejectReason 必填）
export const rejectRefund = (id, rejectReason) => {
  return request.put(`/refund/reject/${id}`, { rejectReason })
}
