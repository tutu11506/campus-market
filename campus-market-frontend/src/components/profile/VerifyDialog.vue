<template>
  <el-dialog v-model="visible" title="线下核销收款" width="400px">
    <div class="verify-form" v-if="order">
      <div class="vf-tip">请让买家出示 6 位核销码，核销成功后订单完成，平台托管资金结算给你。</div>
      <div class="vf-order">{{ order.goodsTitle }} · ¥{{ order.price }}</div>
      <el-input
        v-model="verifyCodeInput"
        maxlength="6"
        placeholder="请输入 6 位核销码"
        class="vf-input"
        @keyup.enter="submitVerify"
      />
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="verifying" @click="submitVerify">确认核销</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { verifyOrder } from '../../api/order'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  order: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'done'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const verifyCodeInput = ref('')
const verifying = ref(false)

watch(() => props.modelValue, (v) => {
  if (v) verifyCodeInput.value = ''
})

const submitVerify = async () => {
  if (!/^\d{6}$/.test(verifyCodeInput.value.trim())) {
    ElMessage.warning('请输入 6 位核销码')
    return
  }
  verifying.value = true
  try {
    const msg = await verifyOrder(props.order.id, verifyCodeInput.value.trim())
    if (typeof msg === 'string' && !msg.includes('核销成功')) {
      ElMessage.warning(msg)
      return
    }
    visible.value = false
    ElMessage.success('核销成功，交易完成，资金已结算')
    emit('done')
  } catch (e) {
    // 拦截器已提示
  } finally {
    verifying.value = false
  }
}
</script>

<style scoped>
.verify-form {
  padding: 2px;
}
.vf-tip {
  font-size: 12px;
  line-height: 1.7;
  color: #6B7280;
}
.vf-order {
  margin: 10px 0 12px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}
.vf-input :deep(.el-input__inner) {
  height: 46px;
  font-size: 20px;
  letter-spacing: 6px;
  text-align: center;
}
</style>
