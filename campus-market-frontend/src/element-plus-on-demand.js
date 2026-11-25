/**
 * Element Plus 按需出口。
 *
 * vite.config.js 里用正则别名把 `element-plus` 精确指向本文件，
 * 各业务文件里的 `import { ElMessage } from 'element-plus'` 无需改动，
 * 但打包时只会引入下面列出的组件，而不是整个组件库（首屏 JS 大幅减小）。
 *
 * 新增 Element Plus 组件时：在这里补一行导出，并在 main.js 里注册。
 */
export { ElAlert } from 'element-plus/es/components/alert/index'
export { ElButton } from 'element-plus/es/components/button/index'
export { ElDatePicker } from 'element-plus/es/components/date-picker/index'
export { ElDialog } from 'element-plus/es/components/dialog/index'
export { ElEmpty } from 'element-plus/es/components/empty/index'
export { ElForm, ElFormItem } from 'element-plus/es/components/form/index'
export { ElIcon } from 'element-plus/es/components/icon/index'
export { ElImageViewer } from 'element-plus/es/components/image-viewer/index'
export { ElInput } from 'element-plus/es/components/input/index'
export { ElInputNumber } from 'element-plus/es/components/input-number/index'
export { ElLoading } from 'element-plus/es/components/loading/index'
export { ElMessage } from 'element-plus/es/components/message/index'
export { ElMessageBox } from 'element-plus/es/components/message-box/index'
export { ElOption, ElSelect } from 'element-plus/es/components/select/index'
export { ElPagination } from 'element-plus/es/components/pagination/index'
export { ElRadio, ElRadioButton, ElRadioGroup } from 'element-plus/es/components/radio/index'
export { ElSkeleton } from 'element-plus/es/components/skeleton/index'
export { ElTable, ElTableColumn } from 'element-plus/es/components/table/index'
export { ElTag } from 'element-plus/es/components/tag/index'
