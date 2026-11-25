import request from './request'

/** 全部预设校友标签 + 每个标签的同学数 / 闲置数 + 我的标签 */
export const getAlumniTags = () => request.get('/alumni/tags')

/** 某个标签下的同学与在售闲置 */
export const getAlumniUsers = (tag) => request.get('/alumni/users', { params: { tag } })

/** 保存我的标签（覆盖式，最多 5 个） */
export const saveMyTags = (tags) => request.put('/alumni/my-tags', { tags })
