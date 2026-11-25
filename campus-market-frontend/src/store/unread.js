import { reactive } from 'vue'
import { getUnreadCount } from '../api/message'

// 全局共享未读数：底部红点、首页铃铛、消息页共用同一份状态，改动立即同步
export const unreadState = reactive({ count: 0 })

// 从服务端刷新未读总数
export const refreshUnread = async () => {
  try {
    const userStr = localStorage.getItem('user')
    if (!userStr) {
      unreadState.count = 0
      return
    }
    const user = JSON.parse(userStr)
    if (!user || !user.id) {
      unreadState.count = 0
      return
    }
    const count = await getUnreadCount(user.id)
    unreadState.count = typeof count === 'number' ? count : (count || 0)
  } catch (e) {
    unreadState.count = 0
  }
}

// 打开会话时先本地扣减，红点立即响应，随后再由服务端校准
export const decreaseUnread = (n) => {
  const num = Number(n) || 0
  unreadState.count = Math.max(0, unreadState.count - num)
}

// 实时通道推送的未读数，直接覆盖本地状态
export const setUnread = (n) => {
  unreadState.count = Math.max(0, Number(n) || 0)
}
