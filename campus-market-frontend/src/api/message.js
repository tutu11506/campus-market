import request from './request'

// 发送消息
export const sendMessage = (data) => {
  return request.post('/message/send', data)
}

// 获取与某人的聊天记录（分页，默认每页 20 条；后端按时间倒序返回，前端自行反转为正序）
export const getChatRecord = (userId, otherId, params = {}) => {
  return request.get('/message/chat', { params: { userId, otherId, ...params } })
}

// 获取会话列表
export const getConversationList = (userId) => {
  return request.get(`/message/list/${userId}`)
}

// 获取未读消息数
export const getUnreadCount = (userId) => {
  return request.get(`/message/unread/${userId}`)
}
