import request from './request'

/** 盲盒许愿列表：scope = open 待认领 / mine 我的许愿 / claimed 我接的单 */
export const getBlindBoxList = (scope) => request.get('/blindbox/list', { params: { scope } })

/** 发布许愿：{ category, budget, wishNote } */
export const publishWish = (data) => request.post('/blindbox/wish', data)

/** 认领并打包：{ boxTitle, boxDesc, price } */
export const claimWish = (id, data) => request.post(`/blindbox/${id}/claim`, data)

/** 买家确认收货 */
export const confirmWish = (id) => request.post(`/blindbox/${id}/confirm`)

/** 买家取消许愿 */
export const cancelWish = (id) => request.post(`/blindbox/${id}/cancel`)
