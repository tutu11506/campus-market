<template>
  <el-dialog v-model="visible" title="退货审核" width="460px" :close-on-click-modal="false">
    <div v-if="currentRefund" class="audit-content">
      <div class="audit-goods">
        <SmartImage :src="currentRefund.order.goodsCover" class="audit-cover" alt="" />
        <div class="audit-goods-info">
          <div class="audit-title">{{ currentRefund.order.goodsTitle }}</div>
          <div class="audit-amount">退款金额：&yen;{{ currentRefund.refund ? currentRefund.refund.amount : currentRefund.order.price }}</div>
        </div>
      </div>
      <template v-if="currentRefund.refund">
        <div class="audit-row"><span class="audit-label">退货原因：</span>{{ currentRefund.refund.reason }}</div>
        <div class="audit-row" v-if="currentRefund.refund.description"><span class="audit-label">详细说明：</span>{{ currentRefund.refund.description }}</div>
        <div class="audit-row" v-if="evidenceImgList.length">
          <span class="audit-label">凭证图片：</span>
          <div class="audit-imgs">
            <img v-for="(img, i) in evidenceImgList" :key="i" :src="img" class="audit-img" @click="previewImage(img)" />
          </div>
        </div>
      </template>
      <div v-else class="audit-empty">未找到待审核的退货申请</div>
      <el-input v-if="auditRejectMode" v-model="auditRejectReason" type="textarea" :rows="2" maxlength="255" show-word-limit placeholder="请填写拒绝原因（必填）" style="margin-top: 10px" />
    </div>
    <template #footer>
      <template v-if="!auditRejectMode">
        <el-button @click="visible = false">关闭</el-button>
        <el-button @click="auditRejectMode = true">拒绝</el-button>
        <el-button type="danger" :loading="auditSubmitting" @click="submitApprove">同意退货</el-button>
      </template>
      <template v-else>
        <el-button @click="auditRejectMode = false; auditRejectReason = ''">返回</el-button>
        <el-button type="primary" :loading="auditSubmitting" @click="submitReject">确认拒绝</el-button>
      </template>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SmartImage from '../SmartImage.vue'
import { getRefundsByOrder, approveRefund, rejectRefund } from '../../api/refund'
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

const auditSubmitting = ref(false)
const auditRejectMode = ref(false)
const auditRejectReason = ref('')
const currentRefund = ref(null)     // { order, refund }

const evidenceImgList = computed(() => {
  const r = currentRefund.value && currentRefund.value.refund
  if (!r || !r.evidenceUrls) return []
  return r.evidenceUrls.split(',').filter(Boolean).map(p => p.startsWith('http') ? p : BASE_URL + p)
})

const previewImage = (url) => { window.open(url, '_blank') }

watch(() => props.modelValue, async (v) => {
  if (!v || !props.order) return
  const order = props.order
  currentRefund.value = { order, refund: null }
  auditRejectMode.value = false
  auditRejectReason.value = ''
  try {
    const list = await getRefundsByOrder(order.id)
    const arr = Array.isArray(list) ? list : []
    // 优先取待审核的；没有则取最新一条
    const pending = arr.find(r => r.status === 'pending')
    currentRefund.value = { order, refund: pending || arr[0] || null }
  } catch (e) { /* 错误由拦截器提示 */ }
})

const submitApprove = async () => {
  const r = currentRefund.value && currentRefund.value.refund
  if (!r) return
  try {
    await ElMessageBox.confirm('确定同意退货？同意后订单将标记为已退货，商品会重新上架。', '提示', {
      confirmButtonText: '同意退货',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) { return }
  auditSubmitting.value = true
  try {
    await approveRefund(r.id)
    ElMessage.success('已同意退货，订单已标记为已退货，商品已重新上架')
    visible.value = false
    emit('done')
  } catch (e) { /* 错误由拦截器提示 */ } finally {
    auditSubmitting.value = false
  }
}

const submitReject = async () => {
  const r = currentRefund.value && currentRefund.value.refund
  if (!r) return
  if (!auditRejectReason.value.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  auditSubmitting.value = true
  try {
    await rejectRefund(r.id, auditRejectReason.value.trim())
    ElMessage.success('已拒绝退货申请')
    visible.value = false
    emit('done')
  } catch (e) { /* 错误由拦截器提示 */ } finally {
    auditSubmitting.value = false
  }
}
</script>

<style scoped>
.audit-content { display: flex; flex-direction: column; gap: 10px; }
.audit-goods { display: flex; gap: 10px; align-items: center; padding: 10px; background: #fafafa; border-radius: 8px; }
.audit-cover { width: 56px; height: 56px; border-radius: 6px; object-fit: cover; background: #f0f0f0; }
.audit-goods-info { flex: 1; min-width: 0; }
.audit-title { font-size: 14px; font-weight: 500; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.audit-amount { font-size: 13px; color: #E1251B; font-weight: 600; margin-top: 4px; }
.audit-row { font-size: 13px; color: #555; line-height: 1.6; word-break: break-all; }
.audit-label { color: #999; }
.audit-imgs { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 4px; }
.audit-img { width: 72px; height: 72px; object-fit: cover; border-radius: 6px; cursor: pointer; background: #f5f5f5; }
.audit-empty { text-align: center; color: #bbb; font-size: 13px; padding: 16px 0; }
</style>
