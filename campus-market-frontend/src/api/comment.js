import request from './request'

// 添加评价
export const addComment = (data) => {
  return request.post('/comment/add', data)
}

// 获取商品评价列表（分页）
export const getGoodsComments = (goodsId, params = {}) => {
  return request.get(`/comment/goods/${goodsId}`, { params })
}

// 获取我的评价列表（分页）
export const getMyComments = (userId, params = {}) => {
  return request.get(`/comment/my/${userId}`, { params })
}

// 获取某个卖家收到的评价列表（公开，分页）
export const getSellerComments = (sellerId, params = {}) => {
  return request.get(`/comment/seller/${sellerId}`, { params })
}
