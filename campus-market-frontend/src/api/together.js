import request from './request'

/* ==================== 拼单 ==================== */

// 拼单列表（公开）
export const getGroupBuys = (params = {}) => {
  return request.get('/group-buys', { params })
}

// 拼单详情（公开）
export const getGroupBuy = (id) => {
  return request.get(`/group-buys/${id}`)
}

// 发起拼单
export const createGroupBuy = (data) => {
  return request.post('/group-buys', data)
}

// 参与拼单
export const joinGroupBuy = (id, shares = 1) => {
  return request.post(`/group-buys/${id}/join`, { shares })
}

// 退出拼单
export const leaveGroupBuy = (id) => {
  return request.post(`/group-buys/${id}/leave`)
}

// 发起人确认成团
export const confirmGroupBuy = (id) => {
  return request.post(`/group-buys/${id}/confirm`)
}

// 发起人关闭拼单
export const closeGroupBuy = (id) => {
  return request.post(`/group-buys/${id}/close`)
}

// 我的拼单：role=owner 我发起的 / member 我参与的
export const getMyGroupBuys = (params = {}) => {
  return request.get('/group-buys/my', { params })
}

// 发起搭子时可关联的、我发起的进行中拼单
export const getLinkableGroupBuys = () => {
  return request.get('/group-buys/linkable')
}

/* ==================== 搭子 ==================== */

// 搭子列表（公开）
export const getPartners = (params = {}) => {
  return request.get('/partners', { params })
}

// 搭子详情（公开）
export const getPartner = (id) => {
  return request.get(`/partners/${id}`)
}

// 发起搭子
export const createPartner = (data) => {
  return request.post('/partners', data)
}

// 加入搭子
export const joinPartner = (id) => {
  return request.post(`/partners/${id}/join`)
}

// 退出搭子
export const leavePartner = (id) => {
  return request.post(`/partners/${id}/leave`)
}

// 发起人取消搭子
export const cancelPartner = (id) => {
  return request.post(`/partners/${id}/cancel`)
}

// 我的搭子：role=owner 我发起的 / member 我参与的
export const getMyPartners = (params = {}) => {
  return request.get('/partners/my', { params })
}
