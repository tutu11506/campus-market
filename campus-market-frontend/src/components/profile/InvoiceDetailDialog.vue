<template>
  <el-dialog v-model="visible" title="电子普通发票" width="480px">
    <div class="e-invoice" v-if="viewingInvoice">
      <div class="ei-head">
        <span class="ei-title">校园二手市场 · 电子普通发票</span>
        <span class="ei-status">已开票</span>
      </div>
      <div class="ei-row"><span>发票号码</span><b>{{ viewingInvoice.invoiceNo }}</b></div>
      <div class="ei-row"><span>开票日期</span><b>{{ formatTime(viewingInvoice.issueTime || viewingInvoice.createTime) }}</b></div>
      <div class="ei-divider"></div>
      <div class="ei-row"><span>购买方</span><b>{{ viewingInvoice.title }}</b></div>
      <div class="ei-row" v-if="viewingInvoice.taxNo"><span>纳税人识别号</span><b>{{ viewingInvoice.taxNo }}</b></div>
      <div class="ei-row"><span>订单号</span><b>{{ viewingInvoice.orderNo }}</b></div>
      <div class="ei-row"><span>商品</span><b>{{ viewingInvoice.goodsTitle || '校园二手商品' }}</b></div>
      <div class="ei-divider"></div>
      <div class="ei-total"><span>价税合计</span><b>¥{{ viewingInvoice.amount }}</b></div>
      <div class="ei-foot">销售方：校园二手市场（演示平台） · 电子发票仅作演示</div>
    </div>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" @click="copyInvoiceNo">复制发票号码</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getInvoiceDetail } from '../../api/invoice'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  invoice: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const viewingInvoice = ref(null)

watch(() => props.modelValue, async (v) => {
  if (!v || !props.invoice) return
  viewingInvoice.value = props.invoice
  try {
    const detail = await getInvoiceDetail(props.invoice.id)
    if (detail) viewingInvoice.value = detail
  } catch (e) { /* 用列表数据兜底 */ }
})

const copyInvoiceNo = async () => {
  const no = viewingInvoice.value && viewingInvoice.value.invoiceNo
  if (!no) return
  try {
    await navigator.clipboard.writeText(no)
    ElMessage.success('发票号码已复制')
  } catch (e) {
    ElMessage.warning('复制失败，请手动选择复制')
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hour = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hour}:${min}`
}
</script>

<style scoped>
.e-invoice {
  padding: 18px;
  border: 1px solid #E5E7EB;
  border-radius: 10px;
  background: linear-gradient(180deg, #FFFDFC, #fff);
}
.ei-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 2px solid #E1251B;
}
.ei-title {
  font-size: 14px;
  font-weight: 800;
  color: #1F2937;
}
.ei-status {
  padding: 1px 8px;
  font-size: 10px;
  color: #16A34A;
  background: #ECFDF3;
  border-radius: 8px;
}
.ei-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 7px 0;
  font-size: 12.5px;
}
.ei-row span {
  flex-shrink: 0;
  color: #9CA3AF;
}
.ei-row b {
  text-align: right;
  color: #374151;
  word-break: break-all;
}
.ei-divider {
  height: 1px;
  margin: 6px 0;
  background: #F0F1F3;
}
.ei-total {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 10px 0 4px;
  font-size: 13px;
  color: #374151;
}
.ei-total b {
  font-size: 22px;
  font-weight: 800;
  color: #E1251B;
}
.ei-foot {
  margin-top: 10px;
  font-size: 10.5px;
  text-align: center;
  color: #B0B4BA;
}
</style>
