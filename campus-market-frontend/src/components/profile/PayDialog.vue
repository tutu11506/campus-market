<template>
  <el-dialog v-model="visible" title="收银台（模拟支付）" width="380px" :close-on-click-modal="false">
    <div v-if="order" class="pay-panel">
      <div class="pay-goods">{{ order.goodsTitle }}</div>
      <div class="pay-amount">&yen;{{ order.price }}</div>
      <div class="pay-method">支付方式：模拟支付</div>
      <div class="pay-tip">演示环境，点击“确认支付”即模拟支付成功，不会产生真实扣款。资金先由平台担保托管，确认收货或卖家核销后才结算给卖家。</div>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="paySubmitting" @click="submitPay">确认支付</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { payOrder } from '../../api/order'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  order: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'done'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const paySubmitting = ref(false)

const submitPay = async () => {
  if (!props.order) return
  paySubmitting.value = true
  try {
    await payOrder(props.order.id)
    ElMessage.success('支付成功，资金已由平台担保托管')
    visible.value = false
    emit('done')
  } catch (e) { /* 错误由拦截器提示 */ } finally {
    paySubmitting.value = false
  }
}
</script>

<style scoped>
.pay-panel {
  text-align: center;
  padding: 4px 0 8px;
}
.pay-goods {
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
}
.pay-amount {
  font-size: 32px;
  font-weight: 700;
  color: #ff5a3c;
  margin-bottom: 14px;
}
.pay-method {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  background: #f2f7ff;
  color: #3b7cff;
  font-size: 13px;
  margin-bottom: 12px;
}
.pay-tip {
  font-size: 12px;
  color: #999;
  line-height: 1.7;
}
</style>
