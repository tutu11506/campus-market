import { DEFAULT_AVATAR, resolveAvatar } from './avatar'
import { baseURL } from '../config'

/** 通用时间格式化 YYYY-MM-DD HH:mm */
export const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}`
}

/** 拼接图片 URL，支持逗号分隔取第一张 */
export const getImageUrl = (path) => {
  if (!path) return 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0iI2YwZjBmMCI+PHJlY3QgeD0iMyIgeT0iMyIgd2lkdGg9IjE4IiBoZWlnaHQ9IjE4IiByeD0iMiIgcnk9IjIiPjwvcmVjdD48Y2lyY2xlIGN4PSI4LjUiIGN5PSI4LjUiIHI9IjEuNSI+PC9jaXJjbGU+PC9zdmc+'
  if (path.startsWith('http')) return path
  const firstImg = path.split(',')[0].trim()
  if (firstImg.startsWith('http')) return firstImg
  return baseURL + firstImg
}

/** 商品状态 → Element Plus Tag type */
export const getStatusType = (status) => ({
  on_sale: 'success',
  sold: 'info',
  off_shelf: 'warning'
}[status] || 'info')

/** 商品状态 → 中文标签 */
export const getStatusLabel = (status) => ({
  on_sale: '在售',
  sold: '已售',
  off_shelf: '下架'
}[status] || status)

/** 公益捐赠：物品类型 → 中文 / 单位 */
export const getDonationItemTypeLabel = type => type === 'books' ? '书籍' : '衣物'
export const getDonationUnit = type => type === 'books' ? '本' : '件'
export const getDonationConditionLabel = value => ({ new: '全新', good: '良好', fair: '一般' }[value] || value)
export const getHandoverLabel = value => value === 'pickup' ? '预约宿舍取件' : '自行送到接收点'
export const getDonationStatusType = status => ({ pending: 'warning', received: 'success', cancelled: 'info' }[status] || 'info')
export const getDonationStatusLabel = status => ({ pending: '待交接', received: '已接收', cancelled: '已取消' }[status] || status)

/** 租赁：类型 / 状态 → 中文 / Tag type */
export const getRentalTypeLabel = type => ({
  daily: '按天租赁',
  weekly: '按周租赁',
  storage: '假期寄存',
  textbook: '教材学期租'
}[type] || type)
export const getRentalStatusType = status => ({
  pending: 'warning',
  renting: 'primary',
  returning: 'warning',
  completed: 'success',
  cancelled: 'info'
}[status] || 'info')
export const getRentalStatusLabel = status => ({
  pending: '待确认',
  renting: '租用中',
  returning: '待收回',
  completed: '已完成',
  cancelled: '已取消'
}[status] || status)

/** 图片 / 头像加载失败兜底 */
export const handleAvatarError = (e) => { e.target.src = DEFAULT_AVATAR }
export const handleThumbError = (e) => {
  e.target.src = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0iI2YwZjBmMCI+PHJlY3QgeD0iMyIgeT0iMyIgd2lkdGg9IjE4IiBoZWlnaHQ9IjE4IiByeD0iMiIgcnk9IjIiPjwvcmVjdD48Y2lyY2xlIGN4PSI4LjUiIGN5PSI4LjUiIHI9IjEuNSI+PC9jaXJjbGU+PC9zdmc+'
}

export { resolveAvatar }
