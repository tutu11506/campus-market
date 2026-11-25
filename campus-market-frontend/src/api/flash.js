import request from './request'

// 校园闪送总览：配送点 / 分类 / 履约数据 / 此刻最抢手
export const getFlashOverview = () => {
  return request.get('/flash/overview')
}

// 闪送商品列表：按配送点 / 分类筛选
export const getFlashGoods = (params = {}) => {
  return request.get('/flash/goods', { params })
}
