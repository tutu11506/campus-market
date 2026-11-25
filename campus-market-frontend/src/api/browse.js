import request from './request'

// 记录当前用户浏览了某商品（需登录）
export const recordBrowse = (goodsId) => {
  return request.post('/browse/record', null, { params: { goodsId } })
}

// 当前用户在指定卖家店铺浏览过的在售商品（聊天“发送商品”选择器用）
export const getShopBrowsedGoods = (sellerId) => {
  return request.get(`/browse/shop/${sellerId}`)
}

// 我的浏览历史（分页）
export const getMyBrowseHistory = (params) => {
  return request.get('/browse/my', { params })
}
