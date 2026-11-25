import request from './request'

// 用户登录
export const login = (data) => {
  return request.post('/user/login', data)
}

// 用户注册
export const register = (data) => {
  return request.post('/user/register', data)
}

// 获取用户信息
export const getUserInfo = (id) => {
  return request.get(`/user/${id}`)
}

// 更新用户信息
export const updateUser = (data) => {
  return request.put('/user/update', data)
}

// 搜索用户（按昵称或学号模糊匹配，不含封禁账号，包含自己），先按昵称再按学号排序，分页
export const searchUsers = (keyword, params = {}) => {
  return request.get('/user/search', { params: { keyword, ...params } })
}

// 个人中心数据概览（发布 / 在售 / 已售 / 收藏 / 收到评价），失败时静默，不打断主页展示
export const getUserStats = () => {
  return request.get('/user/stats', { silent: true })
}
