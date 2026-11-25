<template>
  <el-dialog v-model="visible" title="申请电子发票" width="440px" :close-on-click-modal="false">
    <div class="inv-form">
      <div class="inv-form-row">
        <label>抬头类型</label>
        <el-radio-group v-model="invoiceForm.titleType" size="default">
          <el-radio label="personal">个人</el-radio>
          <el-radio label="company">企业</el-radio>
        </el-radio-group>
      </div>
      <div class="inv-form-row">
        <label>{{ invoiceForm.titleType === 'company' ? '企业名称' : '发票抬头' }} <span class="req">*</span></label>
        <el-input v-model="invoiceForm.title" maxlength="100" show-word-limit placeholder="请输入发票抬头" />
      </div>
      <div class="inv-form-row" v-if="invoiceForm.titleType === 'company'">
        <label>税号 <span class="req">*</span></label>
        <el-input v-model="invoiceForm.taxNo" maxlength="32" placeholder="请输入纳税人识别号" />
      </div>
      <div class="inv-form-row">
        <label>接收邮箱</label>
        <el-input v-model="invoiceForm.email" maxlength="100" placeholder="选填，用于接收电子发票" />
      </div>
      <div class="inv-form-tip" v-if="order">
        订单：{{ order.goodsTitle }} · 可开票金额 ¥{{ invoiceAmount }}
      </div>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submittingInvoice" @click="submitInvoice">提交开票</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { applyInvoice } from '../../api/invoice'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  order: { type: Object, default: null },
  defaultTitle: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue', 'done'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const submittingInvoice = ref(false)
const invoiceForm = reactive({ titleType: 'personal', title: '', taxNo: '', email: '' })

const invoiceAmount = computed(() => {
  const o = props.order
  if (!o) return '0.00'
  const v = Number(o.price || 0) - Number(o.discountAmount || 0)
  return Math.max(v, 0).toFixed(2)
})

watch(() => props.modelValue, (v) => {
  if (v) {
    invoiceForm.titleType = 'personal'
    invoiceForm.title = props.defaultTitle || ''
    invoiceForm.taxNo = ''
    invoiceForm.email = ''
  }
})

const submitInvoice = async () => {
  if (!props.order) return
  if (!invoiceForm.title.trim()) {
    ElMessage.warning(invoiceForm.titleType === 'company' ? '请填写企业名称' : '请填写发票抬头')
    return
  }
  if (invoiceForm.titleType === 'company' && !invoiceForm.taxNo.trim()) {
    ElMessage.warning('企业抬头需要填写税号')
    return
  }
  submittingInvoice.value = true
  try {
    const inv = await applyInvoice({
      orderId: props.order.id,
      titleType: invoiceForm.titleType,
      title: invoiceForm.title.trim(),
      taxNo: invoiceForm.taxNo.trim(),
      email: invoiceForm.email.trim()
    })
    visible.value = false
    emit('done', inv)
    ElMessage.success('开票成功，可在「我的发票」查看')
  } catch (e) {
    // 拦截器已提示
  } finally {
    submittingInvoice.value = false
  }
}
</script>

<style scoped>
.inv-form-row {
  margin-bottom: 15px;
}
.inv-form-row label {
  display: block;
  margin-bottom: 7px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}
.inv-form-row .req {
  color: #E1251B;
}
.inv-form-tip {
  padding: 9px 12px;
  font-size: 12px;
  line-height: 1.6;
  color: #B45309;
  background: #FFFBEB;
  border: 1px solid #FDE68A;
  border-radius: 8px;
}
</style>
