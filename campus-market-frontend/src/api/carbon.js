import request from './request'

/** 碳足迹统计：年度 / 累计减碳、12 个月分布、分类占比 */
export const getCarbonStats = (year) => request.get('/carbon/stats', { params: { year } })
