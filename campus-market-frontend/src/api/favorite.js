import request from './request'

// 切换收藏状态（收藏/取消收藏）
export const toggleFavorite = (data) => {
  return request.post('/favorite/toggle', data)
}

// 获取我的收藏列表（分页）
export const getMyFavorites = (userId, params = {}) => {
  return request.get(`/favorite/my/${userId}`, { params })
}

// 检查是否已收藏
export const checkFavorite = (userId, goodsId) => {
  return request.get('/favorite/check', { params: { userId, goodsId } })
}
