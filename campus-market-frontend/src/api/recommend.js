import request from './request'

// 获取个性化推荐（登录用户精准推荐，未登录返回全站热门）
export const getPersonalizedRecommend = () => {
  return request.get('/recommend/personalized')
}

// 复购提醒：检测近期该补的消耗品类商品
export const getRepurchaseReminders = () => {
  return request.get('/recommend/repurchase-reminders')
}
