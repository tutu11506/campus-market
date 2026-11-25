<template>
  <ServiceShell title="发布求购" subtitle="说清楚想要什么，卖家来报价" badge="求">
    <div class="pub-wrap">
      <div class="pub-card">
        <div class="pub-head">
          <span class="ph-title">求购信息</span>
          <span class="ph-sub">带 * 为必填</span>
        </div>

        <div class="form-row">
          <label>求购分类</label>
          <el-select v-model="form.category" style="width: 220px">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </div>

        <div class="form-row">
          <label>标题 <i>*</i></label>
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="例如：收一本高等数学第七版教材" />
        </div>

        <div class="form-row">
          <label>匹配关键词 <i>*</i></label>
          <el-input v-model="form.keyword" maxlength="50" show-word-limit placeholder="用于自动匹配在售商品，如：高数教材、电动车、书桌" />
          <div class="row-tip">系统会按这个词在在售商品里自动匹配并推送给你</div>
        </div>

        <div class="form-row">
          <label>预算区间</label>
          <div class="budget-row">
            <el-input-number v-model="form.budgetMin" :min="0" :precision="0" :controls="false" placeholder="最低" class="budget-input" />
            <span class="budget-sep">—</span>
            <el-input-number v-model="form.budgetMax" :min="0" :precision="0" :controls="false" placeholder="最高" class="budget-input" />
            <span class="budget-unit">元</span>
          </div>
        </div>

        <div class="form-row">
          <label>详细描述</label>
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
            placeholder="成色要求、可接受价格、面交时间地点等"
          />
        </div>

        <div class="pub-head sub">
          <span class="ph-title">定向求购</span>
          <span class="ph-sub">选填，让同年级 / 同专业的同学更快看到</span>
        </div>

        <div class="form-row">
          <label>定向年级</label>
          <el-select v-model="form.targetGrade" clearable placeholder="不限年级" style="width: 220px">
            <el-option v-for="g in grades" :key="g" :label="g" :value="g" />
          </el-select>
        </div>

        <div class="form-row">
          <label>定向专业</label>
          <el-input v-model="form.targetMajor" maxlength="50" placeholder="如：计算机科学与技术、机械设计制造" style="max-width: 340px" />
        </div>

        <div class="pub-tip">
          发布后，系统会按「匹配关键词」自动推送在售商品；卖家也能在求购专区直接报价，你选定报价即成交。
        </div>

        <div class="pub-actions">
          <button class="btn ghost" @click="goBack">取消</button>
          <button class="btn primary" :disabled="submitting" @click="submit">
            {{ submitting ? '发布中…' : '发布求购' }}
          </button>
        </div>
      </div>
    </div>
  </ServiceShell>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ServiceShell from '../components/ServiceShell.vue'
import { createDemand } from '../api/demand'

const router = useRouter()
const submitting = ref(false)

const categories = [
  { value: 'textbook', label: '教材书籍' },
  { value: 'vehicle', label: '电动车/代步' },
  { value: 'furniture', label: '桌椅家具' },
  { value: 'digital', label: '数码电子' },
  { value: 'dorm', label: '宿舍用品' },
  { value: 'other', label: '其他' }
]

const grades = ['大一', '大二', '大三', '大四', '研究生']

const form = reactive({
  category: 'textbook',
  title: '',
  keyword: '',
  budgetMin: null,
  budgetMax: null,
  description: '',
  targetGrade: '',
  targetMajor: ''
})

const goBack = () => router.push('/wanted')

const submit = async () => {
  if (!form.title.trim() || form.title.trim().length < 2) {
    ElMessage.warning('请填写求购标题（至少 2 个字）')
    return
  }
  if (!form.keyword.trim() || form.keyword.trim().length < 2) {
    ElMessage.warning('请填写匹配关键词（至少 2 个字）')
    return
  }
  if (form.budgetMin != null && form.budgetMax != null && form.budgetMin > form.budgetMax) {
    ElMessage.warning('最低预算不能高于最高预算')
    return
  }
  submitting.value = true
  try {
    const created = await createDemand({
      category: form.category,
      title: form.title.trim(),
      keyword: form.keyword.trim(),
      budgetMin: form.budgetMin,
      budgetMax: form.budgetMax,
      description: form.description.trim(),
      targetGrade: form.targetGrade,
      targetMajor: form.targetMajor.trim()
    })
    ElMessage.success(`发布成功，系统已匹配到 ${(created && created.matchCount) || 0} 件在售商品`)
    router.push('/wanted?open=' + created.id)
  } catch (e) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  if (!localStorage.getItem('token')) {
    router.replace('/login?redirect=/wanted/publish')
  }
})
</script>

<style scoped>
.pub-wrap {
  display: flex;
  justify-content: center;
  padding: 6px 0 20px;
}

.pub-card {
  width: 100%;
  max-width: 720px;
  padding: 22px 26px 24px;
  border-radius: 12px;
  background: #fff;
}

.pub-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding-bottom: 12px;
  border-bottom: 1px solid #F0F1F3;
}

.pub-head.sub {
  margin-top: 24px;
}

.ph-title {
  font-size: 16px;
  font-weight: 800;
  color: #1F2937;
}

.ph-sub {
  font-size: 11.5px;
  color: #9CA3AF;
}

.form-row {
  margin-top: 16px;
}

.form-row label {
  display: block;
  margin-bottom: 7px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.form-row label i {
  color: #E1251B;
  font-style: normal;
}

.row-tip {
  margin-top: 5px;
  font-size: 11px;
  color: #B0B4BA;
}

.budget-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.budget-input {
  width: 150px;
}

.budget-sep,
.budget-unit {
  color: #9CA3AF;
  font-size: 13px;
}

.pub-tip {
  margin-top: 20px;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.7;
  color: #B45309;
  background: #FFFBEB;
  border: 1px solid #FDE68A;
}

.pub-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 22px;
}

.btn {
  height: 38px;
  padding: 0 22px;
  border-radius: 19px;
  font-size: 13.5px;
  font-weight: 700;
  cursor: pointer;
}

.btn.ghost {
  border: 1px solid #E5E7EB;
  background: #fff;
  color: #6B7280;
}

.btn.primary {
  border: none;
  color: #fff;
  background: linear-gradient(135deg, #FF7A45, #E1251B);
  box-shadow: 0 6px 16px rgba(225, 37, 27, 0.24);
}

.btn.primary:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}
</style>
