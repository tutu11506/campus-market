import { baseURL } from '../config'

// 默认头像（放在 public 下，构建后可直接访问）
export const DEFAULT_AVATAR = '/default-avatar.svg'

// 历史上用户表里用过的占位头像，都按“未设置”处理
const PLACEHOLDER_AVATARS = [
  '/default-avatar.png',
  'default-avatar.png',
  '/default-avatar.svg',
  'default-avatar.svg'
]

// 统一解析头像地址：空值/占位图用默认头像，其余相对路径补全后端地址
export const resolveAvatar = (path) => {
  if (!path || PLACEHOLDER_AVATARS.includes(path)) return DEFAULT_AVATAR
  if (path.startsWith('http') || path.startsWith('data:')) return path
  return baseURL + path
}
