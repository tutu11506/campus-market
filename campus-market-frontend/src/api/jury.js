import request from './request'

/* ==================== 评审团 ==================== */

export const getJurySummary = () => request.get('/jury/summary')

export const getJuryReviews = (params = {}) => request.get('/jury/reviews', { params })

export const getJuryReviewableOrders = () => request.get('/jury/orders')

export const createJuryReview = (data) => request.post('/jury/reviews', data)

export const voteJuryReview = (id, vote) => request.post(`/jury/reviews/${id}/vote`, { vote })
