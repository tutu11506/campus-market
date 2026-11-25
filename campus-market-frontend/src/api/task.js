import request from './request'

// 任务大厅列表（公开）
export const getTaskList = (params = {}) => {
  return request.get('/task/list', { params })
}

// 任务详情
export const getTaskDetail = (id) => {
  return request.get('/task/detail/' + id)
}

// 发布任务
export const publishTask = (data) => {
  return request.post('/task/publish', data)
}

// 接单
export const acceptTask = (id) => {
  return request.post('/task/' + id + '/accept')
}

// 发布人确认完成
export const finishTask = (id) => {
  return request.post('/task/' + id + '/finish')
}

// 发布人取消任务
export const cancelTask = (id) => {
  return request.post('/task/' + id + '/cancel')
}

// 我发布的任务
export const getMyPublished = () => {
  return request.get('/task/my/published')
}

// 我接的任务
export const getMyAccepted = () => {
  return request.get('/task/my/accepted')
}
