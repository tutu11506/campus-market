import request from './request'

// 获取用户列表（管理员，分页）
export const getUserList = (params = {}) => {
  return request.get('/admin/user/list', { params })
}

// 更新用户状态（启用/禁用）
export const updateUserStatus = (id, status) => {
  return request.put('/admin/user/status', null, { params: { id, status } })
}

// 获取商品列表（管理员，分页）
export const getGoodsList = (params = {}) => {
  return request.get('/admin/goods/list', { params })
}

// 下架商品（管理员）
export const offlineGoods = (id) => {
  return request.put(`/admin/goods/offline/${id}`)
}

// 删除商品（管理员）
export const deleteGoods = (id) => {
  return request.delete(`/admin/goods/delete/${id}`)
}

// 公益捐赠记录（管理员，分页）
export const getDonationList = (params = {}) => {
  return request.get('/admin/donations', { params })
}

// 依据社团回执确认实际接收数量
export const receiveDonation = (id, receivedQuantity) => {
  return request.put(`/admin/donations/${id}/receive`, { receivedQuantity })
}

// 租赁记录（管理员，分页）
export const getRentalList = (params = {}) => {
  return request.get('/admin/rentals', { params })
}

// 获取统计数据
export const getStatistics = () => {
  return request.get('/admin/statistics')
}

// 数据看板：近 7 天/8 周趋势 + 分类分布
// period: "day" | "week"
export const getDashboard = (period = 'day') => {
  return request.get('/admin/dashboard', { params: { period } })
}

// CSV 导出（直接返回文件下载）
export const getExportUrl = (period = 'day') => {
  const token = localStorage.getItem('token') || ''
  // 生产是同源，直接用相对路径；开发代理也走 /api 前缀
  return `/api/admin/export?period=${period}&token=${encodeURIComponent(token)}`
}
