import request from './request'

// 上传图片（传入原生 File 对象；Content-Type 由浏览器自动设置带 boundary 的 multipart/form-data）
export const uploadImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/image', formData)
}

// 上传文件（聊天用文档类，最大 20MB；返回服务器相对路径 /upload/xxx.ext）
export const uploadFile = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/file', formData)
}
