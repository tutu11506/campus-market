import request from './request'

/* ==================== 商品草稿箱 ==================== */

export const getDrafts = () => request.get('/goods-draft')

export const getDraft = (id) => request.get(`/goods-draft/${id}`)

export const saveDraft = (data) => request.post('/goods-draft', data)

export const deleteDraft = (id) => request.delete(`/goods-draft/${id}`)
