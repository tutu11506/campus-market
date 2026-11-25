import request from './request'

/* ==================== 关键词订阅 ==================== */

export const getKeywords = () => request.get('/subscribe/keywords')

export const addKeyword = (keyword) => request.post('/subscribe/keywords', { keyword })

export const deleteKeyword = (id) => request.delete(`/subscribe/keywords/${id}`)

/* ==================== 心愿商品（收藏降价提醒） ==================== */

export const getGoodsWatches = () => request.get('/subscribe/watch/goods')

/* ==================== 教材心愿（上新提醒） ==================== */

export const getTextbookWishes = () => request.get('/subscribe/wish/textbooks')

export const addTextbookWish = (textbookId) => request.post('/subscribe/wish/textbooks', { textbookId })

export const deleteTextbookWish = (id) => request.delete(`/subscribe/wish/textbooks/${id}`)

export const checkTextbookWish = (textbookId) =>
  request.get('/subscribe/wish/check', { params: { textbookId } })

/* ==================== 提醒记录 ==================== */

export const getNotifyList = (params = {}) => request.get('/subscribe/notifications', { params })

export const getNotifyUnread = () => request.get('/subscribe/notifications/unread')

export const markNotifyRead = (id) => request.post(`/subscribe/notifications/${id}/read`)

export const markAllNotifyRead = () => request.post('/subscribe/notifications/read-all')

export const deleteNotify = (id) => request.delete(`/subscribe/notifications/${id}`)

/* ==================== 概览与学期提醒 ==================== */

export const getSubscribeSummary = () => request.get('/subscribe/summary')

export const checkSemesterReminder = () => request.post('/subscribe/semester/check')

export const scanSemesterReminder = () => request.post('/subscribe/semester/scan')
