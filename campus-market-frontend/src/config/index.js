// 全局统一配置
// API 地址固定为相对路径 /api，由前端开发服务器代理到后端 8081
// 这样公网只需要暴露一个地址，浏览器不会直接请求后端
export const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
