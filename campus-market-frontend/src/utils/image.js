import { baseURL } from '../config'

// 本地默认占位图（不依赖外网，断网也不会裂图）
export const DEFAULT_IMG = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='300' height='300'><rect width='300' height='300' fill='%23f0f0f0'/><text x='50%25' y='50%25' font-size='18' fill='%23bbb' text-anchor='middle' dy='.3em'>暂无图片</text></svg>"

// 原图地址：images 字段可能逗号分隔多图，取第一张
export const resolveImage = (path) => {
  if (!path) return DEFAULT_IMG
  const firstImg = path.split(',')[0].trim()
  if (firstImg.startsWith('http')) return firstImg
  return baseURL + firstImg
}

// 列表卡片使用 320px 缩略图，首屏图片体积小很多
export const thumbUrl = (path) => {
  if (!path) return DEFAULT_IMG
  const firstImg = path.split(',')[0].trim()
  if (firstImg.startsWith('http')) return firstImg
  const name = firstImg.split('/').pop()
  return baseURL + '/thumb/' + name
}

// 兼容旧调用名
export const getImageUrl = resolveImage
export const getThumbUrl = thumbUrl

export const handleImgError = (e) => {
  e.target.src = DEFAULT_IMG
  e.target.classList.add('img-loaded')
}
