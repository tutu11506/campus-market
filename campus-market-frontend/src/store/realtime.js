import { baseURL } from '../config'

// 实时通道：一条 SSE 长连接承载新消息、未读数、已读回执
let es = null
let retry = 0
let retryTimer = null
let manualClose = false

const emit = (name, detail) => {
  window.dispatchEvent(new CustomEvent(name, { detail }))
}

const parse = (raw) => {
  try {
    return JSON.parse(raw)
  } catch (e) {
    return null
  }
}

const scheduleReconnect = () => {
  if (manualClose) return
  const delay = Math.min(1000 * Math.pow(2, retry), 15000)
  retry += 1
  clearTimeout(retryTimer)
  retryTimer = setTimeout(connectRealtime, delay)
}

export const connectRealtime = () => {
  const token = localStorage.getItem('token')
  if (!token) return
  if (es && es.readyState !== EventSource.CLOSED) return
  manualClose = false
  // EventSource 不能带自定义请求头，token 通过查询参数传给后端过滤器
  es = new EventSource(`${baseURL}/message/stream?token=${encodeURIComponent(token)}`)

  es.addEventListener('connected', () => {
    retry = 0
    emit('rt:connected')
  })
  es.addEventListener('message', (e) => emit('rt:message', parse(e.data)))
  es.addEventListener('read', (e) => emit('rt:read', parse(e.data)))
  es.addEventListener('unread', (e) => emit('rt:unread', Number(e.data) || 0))
  // 订阅提醒（降价 / 关键词上新 / 教材上新 / 学期提醒）
  es.addEventListener('notify', (e) => emit('rt:notify', parse(e.data)))

  es.onerror = () => {
    // 断线后指数退避重连（1s → 2s → 4s … 最多 15s），重连后由页面自行补拉数据
    if (es) {
      es.close()
      es = null
    }
    scheduleReconnect()
  }
}

export const disconnectRealtime = () => {
  manualClose = true
  clearTimeout(retryTimer)
  retry = 0
  if (es) {
    es.close()
    es = null
  }
}
