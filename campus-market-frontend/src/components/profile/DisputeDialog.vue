<template>
  <el-dialog v-model="visible" title="申请交易仲裁" width="460px" :close-on-click-modal="false">
    <div class="return-form">
      <div class="return-field">
        <label>纠纷类型 <span class="req">*</span></label>
        <el-select v-model="disputeForm.reasonType" placeholder="请选择纠纷类型" style="width: 100%">
          <el-option label="商品质量问题" value="quality" />
          <el-option label="与描述不符" value="not_as_described" />
          <el-option label="未发货 / 未交付" value="no_delivery" />
          <el-option label="对方拒绝退款" value="refund_refused" />
          <el-option label="其他纠纷" value="other" />
        </el-select>
      </div>
      <div class="return-field">
        <label>纠纷说明 <span class="req">*</span></label>
        <el-input v-model="disputeForm.description" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请描述交易经过与争议点" />
      </div>
      <div class="return-field">
        <label>诉求金额（选填）</label>
        <el-input v-model="disputeForm.claimAmount" placeholder="如：要求退款 35 元" />
      </div>
      <div class="return-field">
        <label>聊天记录</label>
        <el-input v-model="disputeForm.evidenceText" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="粘贴聊天记录中的关键内容（选填）" />
      </div>
      <div class="return-field">
        <label>照片证据</label>
        <div class="return-upload-grid">
          <div v-for="(img, index) in disputeImageList" :key="index" class="return-upload-item has-image">
            <img :src="img" class="return-preview-img" @click="previewImage(img)" />
            <div class="return-delete-btn" @click="removeDisputeImage(index)">
              <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </div>
          </div>
          <div v-if="disputeImageList.length < 4" class="return-upload-item add-btn" @click="triggerDisputeUpload">
            <input ref="disputeFileInput" type="file" accept="image/*" multiple style="display: none" @change="handleDisputeFileChange" />
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#ccc" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            <span class="upload-tip">{{ disputeImageList.length }}/4</span>
          </div>
        </div>
      </div>
      <div class="dispute-tip">提交后该订单担保资金将被冻结，由管理员介入仲裁；请如实提交证据。</div>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="danger" :loading="disputeSubmitting" @click="submitDispute">提交仲裁</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadImage } from '../../api/upload'
import { createDispute, addDisputeEvidence } from '../../api/risk'
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

const ALLOWED_IMG_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
const MAX_IMG_SIZE = 5 * 1024 * 1024

const disputeSubmitting = ref(false)
const disputeForm = reactive({ orderId: null, reasonType: '', description: '', claimAmount: '', evidenceText: '' })
const disputeImageList = ref([])    // 预览用完整 URL
const disputeImagePaths = ref([])   // 提交用相对路径
const disputeFileInput = ref(null)

watch(() => props.modelValue, (v) => {
  if (v && props.order) {
    disputeForm.orderId = props.order.id
    disputeForm.reasonType = ''
    disputeForm.description = ''
    disputeForm.claimAmount = ''
    disputeForm.evidenceText = ''
    disputeImageList.value = []
    disputeImagePaths.value = []
  }
})

const previewImage = (url) => { window.open(url, '_blank') }
const triggerDisputeUpload = () => { disputeFileInput.value && disputeFileInput.value.click() }

const handleDisputeFileChange = async (e) => {
  const files = Array.from(e.target.files || [])
  const remain = 4 - disputeImageList.value.length
  for (const file of files.slice(0, remain)) {
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
        disputeImagePaths.value.push(relPath)
        disputeImageList.value.push(relPath.startsWith('http') ? relPath : BASE_URL + relPath)
      }
    } catch (err) {
      ElMessage.error(`${file.name} 上传失败`)
    }
  }
  e.target.value = ''
}

const removeDisputeImage = (index) => {
  disputeImageList.value.splice(index, 1)
  disputeImagePaths.value.splice(index, 1)
}

const submitDispute = async () => {
  if (!disputeForm.reasonType) {
    ElMessage.warning('请选择纠纷类型')
    return
  }
  if (!disputeForm.description.trim()) {
    ElMessage.warning('请填写纠纷说明')
    return
  }
  disputeSubmitting.value = true
  try {
    const res = await createDispute({
      orderId: disputeForm.orderId,
      reasonType: disputeForm.reasonType,
      description: disputeForm.description,
      claimAmount: disputeForm.claimAmount ? disputeForm.claimAmount : null
    })
    const disputeId = res && res.disputeId
    // 聊天记录 + 照片作为证据提交
    if (disputeId) {
      const text = disputeForm.evidenceText.trim()
      if (text || disputeImagePaths.value.length) {
        await addDisputeEvidence({
          disputeId,
          type: text ? 'chat' : 'image',
          content: text || null,
          fileUrl: disputeImagePaths.value[0] || null
        })
        for (let i = 1; i < disputeImagePaths.value.length; i++) {
          await addDisputeEvidence({ disputeId, type: 'image', content: null, fileUrl: disputeImagePaths.value[i] })
        }
      }
    }
    ElMessage.success((res && res.message) || '仲裁申请已提交，请等待管理员处理')
    visible.value = false
    emit('done')
  } catch (e) { /* 错误由拦截器提示 */ } finally {
    disputeSubmitting.value = false
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
.dispute-tip {
  font-size: 12px;
  color: #C2410C;
  background: #FFF7ED;
  border: 1px solid #FED7AA;
  border-radius: 6px;
  padding: 8px 10px;
  line-height: 1.6;
}
</style>
