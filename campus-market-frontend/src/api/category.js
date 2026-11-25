import request from './request'

// 获取分类列表
export const getCategoryList = () => {
  return request.get('/category/list')
}

// 获取分类详情
export const getCategoryDetail = (id) => {
  return request.get(`/category/${id}`)
}

// 添加分类
export const addCategory = (data) => {
  return request.post('/category/add', data)
}

// 更新分类
export const updateCategory = (data) => {
  return request.put('/category/update', data)
}

// 删除分类
export const deleteCategory = (id) => {
  return request.delete(`/category/delete/${id}`)
}
