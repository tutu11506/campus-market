import request from './request'

/* ==================== 风控公开查询 ==================== */

// 买家侧价格洞察：是否高于近期同类均价
export const getPriceInsight = (goodsId) => {
  return request.get('/risk/price/insight', { params: { goodsId } })
}

// 卖家平台失信查询（全平台提醒）
export const checkBlacklist = (userId) => {
  return request.get('/risk/blacklist/check', { params: { userId } })
}

/* ==================== 举报处理 ==================== */

export const getReportList = (params = {}) => {
  return request.get('/risk/admin/report/list', { params })
}

export const handleReport = (reportId, status) => {
  return request.put('/risk/admin/report/handle', null, { params: { reportId, status } })
}

/* ==================== 违禁品识别 ==================== */

export const getModerationList = (params = {}) => {
  return request.get('/risk/admin/moderation/list', { params })
}

export const confirmModeration = (id, action = 'confirm') => {
  return request.put('/risk/admin/moderation/confirm', null, { params: { id, action } })
}

export const getModerationStats = () => {
  return request.get('/risk/admin/moderation/stats')
}

export const getProhibitedItems = () => {
  return request.get('/risk/admin/moderation/items')
}

export const refreshProhibitedWords = () => {
  return request.post('/risk/admin/moderation/refresh')
}

/* ==================== 价格预警 ==================== */

export const getPriceAlertList = (params = {}) => {
  return request.get('/risk/admin/price/list', { params })
}

export const handlePriceAlert = (id, status, note = '') => {
  return request.put('/risk/admin/price/handle', null, { params: { id, status, note } })
}

export const rescanPrice = () => {
  return request.post('/risk/admin/price/rescan')
}

export const getPriceAlertStats = () => {
  return request.get('/risk/admin/price/stats')
}

/* ==================== 交易纠纷仲裁 ==================== */

export const createDispute = (data) => {
  return request.post('/dispute/create', data)
}

export const addDisputeEvidence = (data) => {
  return request.post('/dispute/evidence', data)
}

export const getMyDisputes = (params = {}) => {
  return request.get('/dispute/my', { params })
}

export const getDisputeDetail = (id) => {
  return request.get(`/dispute/${id}`)
}

export const getAdminDisputeList = (params = {}) => {
  return request.get('/dispute/admin/list', { params })
}

export const getAdminDisputeDetail = (id) => {
  return request.get(`/dispute/admin/${id}`)
}

export const startDisputeProcessing = (id) => {
  return request.put('/dispute/admin/processing', null, { params: { id } })
}

export const resolveDispute = (data) => {
  return request.put('/dispute/admin/resolve', data)
}

export const getDisputeStats = () => {
  return request.get('/dispute/admin/stats')
}

/* ==================== 平台黑名单 ==================== */

export const getBlacklistPage = (params = {}) => {
  return request.get('/risk/admin/blacklist/list', { params })
}

export const markBlacklist = (data) => {
  return request.post('/risk/admin/blacklist/mark', data)
}

export const revokeBlacklist = (id) => {
  return request.put('/risk/admin/blacklist/revoke', null, { params: { id } })
}

export const getBlacklistStats = () => {
  return request.get('/risk/admin/blacklist/stats')
}
