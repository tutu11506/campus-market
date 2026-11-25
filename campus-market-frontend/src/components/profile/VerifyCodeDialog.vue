<template>
  <el-dialog v-model="visible" title="线下自提核销码" width="400px">
    <div class="vcode-box" v-if="verifyOrderInfo">
      <div class="vcode-tip">当面交易时，把这个核销码出示给卖家核销</div>
      <div class="vcode">{{ verifyOrderInfo.verifyCode || '——' }}</div>
      <div class="vcode-order">{{ verifyOrderInfo.goodsTitle }}</div>
      <div class="vcode-amount">担保金额 ¥{{ verifyOrderInfo.price }}</div>
      <div class="vcode-note">卖家核销成功后交易才算完成，托管资金才会结算给卖家；还没拿到商品前不要核销。</div>
    </div>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { getOrderDetail } from '../../api/order'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  order: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const verifyOrderInfo = ref(null)

watch(() => props.modelValue, async (v) => {
  if (!v || !props.order) return
  verifyOrderInfo.value = props.order
  try {
    const detail = await getOrderDetail(props.order.id)
    if (detail) verifyOrderInfo.value = detail
  } catch (e) { /* 用列表数据兜底 */ }
})
</script>

<style scoped>
.vcode-box {
  padding: 6px 4px 2px;
  text-align: center;
}
.vcode-tip {
  font-size: 12px;
  color: #6B7280;
}
.vcode {
  margin: 14px 0 6px;
  font-family: 'Courier New', monospace;
  font-size: 38px;
  font-weight: 800;
  letter-spacing: 10px;
  color: #E1251B;
}
.vcode-order {
  font-size: 13px;
  color: #374151;
}
.vcode-amount {
  margin-top: 4px;
  font-size: 12px;
  color: #9CA3AF;
}
.vcode-note {
  margin-top: 14px;
  padding: 10px 12px;
  font-size: 11.5px;
  line-height: 1.7;
  text-align: left;
  color: #B45309;
  background: #FFFBEB;
  border: 1px solid #FDE68A;
  border-radius: 8px;
}
</style>
