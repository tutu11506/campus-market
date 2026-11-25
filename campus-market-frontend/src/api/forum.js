import request from './request'

// 帖子列表（公开，board/keyword 可选）
export const getPosts = (params = {}) => {
  return request.get('/forum/posts', { params })
}

// 帖子详情（浏览量 +1）
export const getPostDetail = (id) => {
  return request.get('/forum/posts/' + id)
}

// 发帖
export const createPost = (data) => {
  return request.post('/forum/posts', data)
}

// 删除帖子（本人或管理员）
export const deletePost = (id) => {
  return request.delete('/forum/posts/' + id)
}

// 点赞 / 取消点赞
export const togglePostLike = (id) => {
  return request.post('/forum/posts/' + id + '/like')
}

// 回复列表
export const getComments = (id) => {
  return request.get('/forum/posts/' + id + '/comments')
}

// 发表回复（parentId 非空表示回复某条回复）
export const addComment = (id, content, parentId) => {
  const data = { content }
  if (parentId) data.parentId = parentId
  return request.post('/forum/posts/' + id + '/comments', data)
}

// 删除回复（本人或管理员）
export const deleteComment = (id) => {
  return request.delete('/forum/comments/' + id)
}

// 我发布的帖子
export const getMyPosts = (params = {}) => {
  return request.get('/forum/my/posts', { params })
}

// 我的论坛通知
export const getMyNotifications = (params = {}) => {
  return request.get('/forum/notifications', { params })
}

// 未读通知数量
export const getUnreadNotificationCount = () => {
  return request.get('/forum/notifications/unread')
}

// 通知全部标记已读
export const readNotifications = () => {
  return request.post('/forum/notifications/read')
}
