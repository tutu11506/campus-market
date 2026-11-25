import request from './request'

// 待领取的优惠券礼包（没有任何可用券时后端会自动补发一整套）
export const getPendingCoupons = () => request.get('/coupon/pending')

// 一键领取全部
export const claimAllCoupons = () => request.post('/coupon/claim-all')

// 我的可用券
export const getMyCoupons = () => request.get('/coupon/mine')

// 当前金额可用的最优券
export const getApplicableCoupon = (amount) => request.get('/coupon/applicable', { params: { amount } })
