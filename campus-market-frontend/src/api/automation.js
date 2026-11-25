import request from './request'

// 预警中心概览：规则 + 统计 + 上次扫描时间
export const getAutomationOverview = () => {
  return request.get('/automation/overview')
}

// 预警列表
export const getAutomationAlerts = (params = {}) => {
  return request.get('/automation/alerts', { params })
}

// 立即扫描
export const scanAutomation = () => {
  return request.post('/automation/scan')
}

// 标记预警已处理
export const resolveAutomationAlert = (id) => {
  return request.post('/automation/alerts/' + id + '/resolve')
}
