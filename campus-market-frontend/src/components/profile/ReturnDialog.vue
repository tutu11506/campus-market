<template>
  <el-dialog v-model="visible" title="申请退货" width="420px" :close-on-click-modal="false">
    <div class="return-form">
      <div class="return-field">
        <label>退货原因 <span class="req">*</span></label>
        <el-select v-model="returnForm.reason" placeholder="请选择退货原因" style="width: 100%">
          <el-option v-for="r in returnReasonOptions" :key="r" :label="r" :value="r" />
        </el-select>
      </div>
      <div class="return-field">
        <label>详细说明</label>
        <el-input v-model="returnForm.description" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请描述具体情况（选填）" />
      </div>
      <div class="return-field">
        <label>凭证图片</label>
        <div class="return-upload-grid">
          <div v-for="(img, index) in returnImageList" :key="index" class="return-upload-item has-image">
            <img :src="img" class="return-preview-img" @click="previewImage(img)" />
            <div class="return-delete-btn" @click="removeReturnImage(index)">
              <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </div>
          </div>
          <div v-if="returnImageList.length < 6" class="return-upload-item add-btn" @click="triggerReturnUpload">
            <input ref="returnFileInput" type="file" accept="image/*" multiple style="display: none" @change="handleReturnFileChange" />
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#ccc" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            <span class="upload-tip">{{ returnImageList.length }}/6</span>
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="returnSubmitting" @click="submitReturn">提交申请</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadImage } from '../../api/upload'
import { applyRefund } from '../../api/refund'
import { baseURL as BASE_URL } from '../../config'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  order: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'done'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const returnReasonOptions = ['商品与描述不符', '商品存在质量问题', '不想要了/拍错了', '其他']
const ALLOWED_IMG_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
const MAX_IMG_SIZE = 5 * 1024 * 1024

const returnSubmitting = ref(false)
const returnForm = ref({ orderId: null, reason: '', description: '' })
const returnImageList = ref([])     // 预览用完整 URL
const returnImagePaths = ref([])    // 存库用相对路径
const returnFileInput = ref(null)

watch(() => props.modelValue, (v) => {
  if (v && props.order) {
    returnForm.value = { orderId: props.order.id, reason: '', description: '' }
    returnImageList.value = []
    returnImagePaths.value = []
  }
})

const previewImage = (url) => { window.open(url, '_blank') }
const triggerReturnUpload = () => { returnFileInput.value && returnFileInput.value.click() }

const handleReturnFileChange = async (e) => {
  const files = Array.from(e.target.files)
  const remain = 6 - returnImageList.value.length
  if (remain <= 0) {
    ElMessage.warning('最多上传6张图片')
    e.target.value = ''
    return
  }
  const toUpload = files.slice(0, remain)
  for (const file of toUpload) {
    if (!ALLOWED_IMG_TYPES.includes(file.type)) {
      ElMessage.warning(`${file.name} 格式不支持，仅支持 jpg/png/webp/gif`)
      continue
    }
    if (file.size > MAX_IMG_SIZE) {
      ElMessage.warning(`${file.name} 超过 5MB，请压缩后上传`)
      continue
    }
    try {
      const res = await uploadImage(file)
      const relPath = typeof res === 'string' ? res : (res && res.url ? res.url : '')
      if (relPath) {
        returnImagePaths.value.push(relPath)
        returnImageList.value.push(relPath.startsWith('http') ? relPath : BASE_URL + relPath)
      }
    } catch (err) {
      ElMessage.error(`${file.name} 上传失败`)
    }
  }
  e.target.value = ''
}

const removeReturnImage = (index) => {
  returnImageList.value.splice(index, 1)
  returnImagePaths.value.splice(index, 1)
}

const submitReturn = async () => {
  if (!returnForm.value.reason) {
    ElMessage.warning('请选择退货原因')
    return
  }
  returnSubmitting.value = true
  try {
    await applyRefund({
      orderId: returnForm.value.orderId,
      reason: returnForm.value.reason,
      description: returnForm.value.description || '',
      evidenceUrls: returnImagePaths.value.join(',')
    })
    ElMessage.success('退货申请已提交，请等待卖家审核')
    visible.value = false
    emit('done')
  } catch (e) { /* 错误由拦截器提示 */ } finally {
    returnSubmitting.value = false
  }
}
</script>

<style scoped>
.return-form { display: flex; flex-direction: column; gap: 12px; }
.return-field { display: flex; flex-direction: column; gap: 6px; }
.return-field > label { font-size: 13px; color: #333; font-weight: 500; }
.return-field .req { color: #E1251B; }
.return-upload-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; }
.return-upload-item { position: relative; width: 100%; aspect-ratio: 1 / 1; border-radius: 6px; overflow: hidden; }
.return-upload-item.has-image { background: #f5f5f5; }
.return-upload-item.add-btn { border: 1px dashed #ddd; display: flex; flex-direction: column; align-items: center; justify-content: center; cursor: pointer; background: #fafafa; }
.return-upload-item.add-btn:hover { border-color: #E1251B; }
.return-preview-img { width: 100%; height: 100%; object-fit: cover; cursor: pointer; }
.return-delete-btn { position: absolute; top: 2px; right: 2px; width: 18px; height: 18px; background: rgba(0,0,0,0.5); border-radius: 50%; display: flex; align-items: center; justify-content: center; cursor: pointer; }
</style>
