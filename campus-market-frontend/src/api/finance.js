import request from './request'

/** 月度营收账单：卖出收入 / 跑腿支出 / 积分变动 */
export const getLedger = (month) => request.get('/finance/ledger', { params: { month } })

/** 商品曝光数据看板：浏览量 / 收藏 / 咨询 / 宿舍区来源 */
export const getExposure = () => request.get('/seller/exposure')
