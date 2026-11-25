<template>
  <el-dialog
    v-model="visible"
    title="编辑资料"
    width="360px"
    :close-on-click-modal="false"
    custom-class="edit-dialog"
  >
    <div class="edit-form">
      <div class="edit-avatar-row">
        <img :src="editForm.avatar ? resolveAvatar(editForm.avatar) : avatarUrl" class="edit-avatar-preview" />
        <button class="upload-avatar-btn" @click="triggerAvatarUpload">
          更换头像
        </button>
        <input
          ref="avatarInput"
          type="file"
          accept="image/*"
          style="display: none"
          @change="handleAvatarChange"
        />
      </div>
      <div class="edit-field">
        <label>昵称</label>
        <input v-model="editForm.username" placeholder="请输入昵称" class="edit-input" />
      </div>
      <div class="edit-field">
        <label>手机号</label>
        <input v-model="editForm.phone" placeholder="请输入手机号" class="edit-input" />
      </div>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="saveEdit" :loading="savingEdit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadImage } from '../../api/upload'
import { updateUser } from '../../api/user'
import { baseURL as BASE_URL } from '../../config'
import { resolveAvatar } from '../../utils/avatar'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  userInfo: { type: Object, default: () => ({}) },
  avatarUrl: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue', 'saved'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const savingEdit = ref(false)
const avatarInput = ref(null)
const editForm = ref({ username: '', phone: '', avatar: '' })

watch(() => props.modelValue, (v) => {
  if (v) {
    editForm.value.username = props.userInfo.username || ''
    editForm.value.phone = props.userInfo.phone || ''
    editForm.value.avatar = props.userInfo.avatar || ''
  }
})

const triggerAvatarUpload = () => {
  avatarInput.value && avatarInput.value.click()
}

const handleAvatarChange = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  try {
    const res = await uploadImage(file)
    const url = typeof res === 'string'
      ? (res.startsWith('http') ? res : BASE_URL + res)
      : (res.url ? (res.url.startsWith('http') ? res.url : BASE_URL + res.url) : '')
    if (url) {
      editForm.value.avatar = url
    }
  } catch (err) {
    ElMessage.error('头像上传失败')
  }
  e.target.value = ''
}

const saveEdit = async () => {
  const username = (editForm.value.username || '').trim()
  const phone = (editForm.value.phone || '').trim()
  if (!username) {
    ElMessage.warning('请输入昵称')
    return
  }

  // 只提交发生变化的字段，避免历史遗留的非法手机号阻塞保存
  const data = {}
  if (username !== (props.userInfo.username || '')) {
    data.username = username
  }
  if (phone !== (props.userInfo.phone || '')) {
    if (phone && !/^1[3-9]\d{9}$/.test(phone)) {
      ElMessage.warning('请输入正确的手机号')
      return
    }
    data.phone = phone
  }
  if (editForm.value.avatar && editForm.value.avatar !== (props.userInfo.avatar || '')) {
    // 存相对路径，换域名后头像依然有效
    data.avatar = editForm.value.avatar.startsWith(BASE_URL)
      ? editForm.value.avatar.slice(BASE_URL.length)
      : editForm.value.avatar
  }

  if (Object.keys(data).length === 0) {
    visible.value = false
    return
  }

  savingEdit.value = true
  try {
    const cachedUser = JSON.parse(localStorage.getItem('user') || '{}')
    const uid = props.userInfo.id || cachedUser.id
    await updateUser({ id: uid, ...data })
    const cached = JSON.parse(localStorage.getItem('user') || '{}')
    localStorage.setItem('user', JSON.stringify({ ...cached, ...data }))
    emit('saved', data)
    visible.value = false
    ElMessage.success('资料已更新')
  } catch (e) {
    // 错误提示已由请求拦截器统一处理，这里保留弹窗方便修改
  } finally {
    savingEdit.value = false
  }
}
</script>

<style scoped>
.edit-form {
  padding: 0 8px;
}
.edit-avatar-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.edit-avatar-preview {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  background: #f5f5f5;
}
.upload-avatar-btn {
  padding: 6px 16px;
  border: 1px solid #E1251B;
  border-radius: 16px;
  background: transparent;
  color: #E1251B;
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
}
.edit-field {
  margin-bottom: 16px;
}
.edit-field label {
  display: block;
  font-size: 13px;
  color: #999;
  margin-bottom: 6px;
}
.edit-input {
  width: 100%;
  height: 40px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 14px;
  color: #333;
  outline: none;
  font-family: inherit;
  transition: border-color 0.2s;
}
.edit-input:focus {
  border-color: #E1251B;
}
</style>
