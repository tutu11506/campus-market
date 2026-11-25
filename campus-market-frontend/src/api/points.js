import request from './request'

export const getPointAccount = () => request.get('/points/account')
export const getPointRecords = (pageNum = 1, pageSize = 20) => request.get('/points/records', { params: { pageNum, pageSize } })
export const getPointRewards = () => request.get('/points/rewards')
export const exchangeReward = (rewardId) => request.post('/points/exchange', { rewardId })
export const getMyExchanges = (pageNum = 1, pageSize = 20) => request.get('/points/exchanges', { params: { pageNum, pageSize } })
export const getMyBadges = () => request.get('/points/badges')
export const getAllBadges = () => request.get('/points/badges/all')
export const checkBadges = () => request.post('/points/badges/check')
export const getLeaderboard = (scope = 'user_balance') => request.get('/points/leaderboard', { params: { scope } })
