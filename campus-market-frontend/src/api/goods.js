import request from './request'

// 获取商品列表（支持分页和筛选）
export const getGoodsList = (params) => {
  return request.get('/goods/list', { params })
}

// 获取分类列表
export const getCategoryList = () => {
  return request.get('/category/list')
}

// 获取商品详情
export const getGoodsDetail = (id) => {
  return request.get(`/goods/${id}`)
}

// 首页聚合数据：分类 + 轮播 + 四大类封面 + 首屏商品（一次请求）
export const getHomeInit = (pageSize = 24) => {
  return request.get('/home/init', { params: { pageSize } })
}

// 发布商品
export const addGoods = (data) => {
  return request.post('/goods/add', data)
}

// 获取我发布的商品（分页）
export const getMyGoods = (sellerId, params = {}) => {
  return request.get(`/goods/my/${sellerId}`, { params })
}

// 修改商品
export const updateGoods = (data) => {
  return request.put('/goods/update', data)
}

// 修改商品状态（上下架）
export const updateGoodsStatus = (id, status) => {
  return request.put('/goods/status', null, { params: { id, status } })
}

// 删除商品
export const deleteGoods = (id) => {
  return request.delete(`/goods/delete/${id}`)
}

// 一键复制商品：复制自己商品的信息，生成 count 件新的在售商品
export const copyGoods = (id, count = 1) => {
  return request.post('/goods/copy', { id, count })
}

// 校园同款比价（同分类价格区间 -50%~+50% 内的在校商品）
export const getCampusSimilar = (goodsId) => {
  return request.get(`/goods/similar`, { params: { goodsId } })
}

// 校园面交点热力（按交易地点聚合在售商品）
export const getPickupSpots = () => {
  return request.get('/home/pickup-spots')
}

// 校园交易高峰时段（订单按小时分布 + 高峰窗口）
export const getActiveHours = () => {
  return request.get('/home/active-hours')
}

// 校园行情价 & 砍价助手（同分类四分位区间 + 建议出价 + 话术）
export const getMarketPrice = (goodsId) => {
  return request.get('/goods/market-price', { params: { goodsId } })
}

// 多平台比价（闲鱼 / 转转 / 爱回收 / 淘宝参考价）
export const getPlatformCompare = (goodsId) => {
  return request.get('/goods/platform-compare', { params: { goodsId } })
}

// 校园捡漏榜（价格显著低于同分类均价的在售商品）
export const getDealRadar = () => {
  return request.get('/home/deal-radar')
}

// 校园行情总览（各分类在售件数 / 均价 / 最低价）
export const getMarketOverview = () => {
  return request.get('/home/market-overview')
}

// 教材循环站（教材按学科聚合的件数与价格）
export const getTextbookSubjects = () => {
  return request.get('/home/textbook-subjects')
}

// 校园估价器（分类 + 原价 → 建议校园售价区间与折扣）
export const getValuation = (categoryId, originalPrice) => {
  return request.get('/goods/valuation', { params: { categoryId, originalPrice } })
}

// 同地点交易榜（某个面交地点的在售详情 + 成交概况）
export const getLocationDetail = (name) => {
  return request.get('/home/location-detail', { params: { name } })
}

// 成交风向标（各分类真实成交均价 vs 在售均价对比）
export const getDealTrend = () => {
  return request.get('/home/deal-trend')
}

// 卖家数据看板（登录用户自己的发布概览/流量/交易/分类/趋势）
export const getSellerStats = () => {
  return request.get('/seller/stats')
}

// 宿舍楼下好货：按宿舍区聚合的邻居闲置
export const getDormGoods = () => {
  return request.get('/home/dorm-goods')
}

// 某个宿舍区的在售商品样本
export const getDormDetail = (area) => {
  return request.get('/home/dorm-detail', { params: { area } })
}

// 求购热力榜：大家都在看但没人卖的品类 TOP6
export const getDemandHeat = () => {
  return request.get('/home/demand-heat')
}

// 毕业清仓专区：应届毕业生急出榜
export const getGraduationSale = () => {
  return request.get('/home/graduation-sale')
}

// 校园教材价格指数：教材成交均价趋势 + 时机建议
export const getTextbookIndex = () => {
  return request.get('/home/textbook-index')
}
