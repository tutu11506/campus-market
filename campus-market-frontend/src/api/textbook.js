import request from './request'

/* ==================== 教材主表（公开浏览） ==================== */

// 教材列表（公开，支持 subject/teacherFlag/keyword 筛选）
export const getTextbooks = (params = {}) => request.get('/textbooks', { params })

// 学科分类聚合（公开）
export const getTextbookSubjects = () => request.get('/textbooks/subjects')

// 教材详情（公开）
export const getTextbook = (id) => request.get(`/textbooks/${id}`)

// 登录发布新教材
export const createTextbook = (data) => request.post('/textbooks', data)

// 更新教材信息
export const updateTextbook = (id, data) => request.post(`/textbooks/${id}/update`, data)

// 标记老师指定版本
export const markTeacher = (id, flag, note) =>
  request.post(`/textbooks/${id}/teacher-mark`, { flag, note })

/* ==================== 版本避雷 ==================== */

// 公开：教材避雷留言列表
export const getTextbookWarnings = (id) => request.get(`/textbooks/${id}/warnings`)

// 登录：发布避雷留言
export const addTextbookWarning = (id, reason, detail) =>
  request.post(`/textbooks/${id}/warnings`, { reason, detail })

/* ==================== 课表（需登录） ==================== */

// 我的学期列表
export const getSemesters = () => request.get('/textbooks/courses/semesters')

// 指定学期的课表
export const getMyCourses = (semester) => request.get('/textbooks/courses', { params: { semester } })

// 批量导入课表（replaceExisting=true 会跳过已存在的课程）
export const importCourses = (semester, items, replaceExisting = true) =>
  request.post('/textbooks/courses/import', { semester, items, replaceExisting })

// 一键匹配：列出学期课表 → 匹配教材 → 标记在售/可租/还差
export const matchTextbooks = (semester) =>
  request.get('/textbooks/courses/match', { params: { semester } })

/* ==================== 笔记附件 ==================== */

// 公开：按 owner 查附件（ownerType=textbook/goods/rental）
export const getMaterialsByOwner = (ownerType, ownerId) =>
  request.get('/textbooks/materials', { params: { ownerType, ownerId } })

// 公开：看附件详情
export const getMaterial = (id) => request.get(`/textbooks/materials/${id}`)

// 登录：附件下载计数+1（返回最新数据）
export const downloadMaterial = (id) => request.post(`/textbooks/materials/${id}/download`)

// 登录：上传资料
export const addMaterial = (data) => request.post('/textbooks/materials', data)

// 登录：我上传的资料（分页）
export const getMyMaterials = (params = {}) => request.get('/textbooks/materials/my', { params })

// 登录：删除自己上传的资料
export const deleteMaterial = (id) => request.post(`/textbooks/materials/${id}/delete`)
