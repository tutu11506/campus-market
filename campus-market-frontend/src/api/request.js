import axios from 'axios'
import { ElMessage } from 'element-plus'
import { baseURL } from '../config'

const request = axios.create({
  baseURL,
  timeout: 10000
})

// 401/403 登录失效 并发去重：多个请求同时失效时只弹一次提示、只跳一次登录
let isRedirecting = false

// 当前是否已经在登录页或 404 页——这些页面不需要再跳登录
const isOnPublicPage = () => {
  const p = window.location.pathname
  return p.startsWith('/login') || p === '/' || p === '/404' || !p.startsWith('/')
}

// 请求拦截器 - 自动附带 JWT（Authorization: Bearer <token>）
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器 - 统一处理响应
request.interceptors.response.use(
  response => {
    // 滑动续期：后端在 token 临期时通过响应头下发新 token，这里静默替换
    const refreshed = response.headers['x-refresh-token']
    if (refreshed) {
      localStorage.setItem('token', refreshed)
    }
    const res = response.data
    if (res.code === 200) {
      return res.data
    } else {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
  },
  error => {
    if (error.response) {
      const status = error.response.status
      const data = error.response.data || {}
      const silent = error.config && error.config.silent
      if (status === 401) {
        if (!isRedirecting) {
          isRedirecting = true
          localStorage.removeItem('token')
          localStorage.removeItem('user')
          if (isOnPublicPage()) {
            // 已经在登录页，只清 token 不跳（App.vue 也不再触发 API 调用）
          } else {
            ElMessage.error('登录已过期，请重新登录')
            const redirect = encodeURIComponent(window.location.pathname + window.location.search)
            window.location.href = '/login?redirect=' + redirect
          }
          setTimeout(() => { isRedirecting = false }, 1500)
        }
      } else if (status === 403) {
        const msg = data.msg || '没有权限执行此操作'
        const isAuthExpired = msg === '没有权限执行此操作'
          || /未登录|已过期|登录已过期/.test(msg)
        if (isAuthExpired && !isRedirecting) {
          isRedirecting = true
          localStorage.removeItem('token')
          localStorage.removeItem('user')
          if (isOnPublicPage()) {
            // 已在登录页，只清 token 不跳
          } else {
            ElMessage.error('登录状态已失效，请重新登录')
            const redirect = encodeURIComponent(window.location.pathname + window.location.search)
            window.location.href = '/login?redirect=' + redirect
          }
          setTimeout(() => { isRedirecting = false }, 1500)
        } else if (!silent) {
          ElMessage.error(msg)
        }
      } else if (!silent) {
        ElMessage.error(data.msg || '服务器错误')
      }
    } else if (!(error.config && error.config.silent)) {
      ElMessage.error('网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default request
