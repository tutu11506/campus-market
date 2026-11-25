<template>
  <ServiceShell title="营收账单" subtitle="卖出收入 · 跑腿支出 · 积分变动，一键导出对账" badge="账">
    <section class="month-bar">
      <div class="month-left">
        <el-date-picker
          v-model="month"
          type="month"
          value-format="YYYY-MM"
          placeholder="选择月份"
          :clearable="false"
          :editable="false"
          @change="load"
        />
        <span class="month-tip">按结算 / 任务完成 / 积分变动时间归月</span>
      </div>
      <div class="month-actions">
        <el-button :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :disabled="!records.length" @click="exportCsv">导出账单</el-button>
      </div>
    </section>

    <section class="summary-grid">
      <div class="sum-card main">
        <div class="sum-label">现金净收入</div>
        <div class="sum-value">¥{{ money(summary.cashNet) }}</div>
        <div class="sum-sub">卖出收入 − 跑腿支出</div>
      </div>
      <div class="sum-card">
        <div class="sum-label">卖出收入</div>
        <div class="sum-value income">+¥{{ money(summary.income) }}</div>
        <div class="sum-sub">{{ counts.income }} 笔已完成订单</div>
      </div>
      <div class="sum-card">
        <div class="sum-label">跑腿支出</div>
        <div class="sum-value expense">-¥{{ money(summary.errandExpense) }}</div>
        <div class="sum-sub">{{ counts.errand }} 个已完成任务</div>
      </div>
      <div class="sum-card">
        <div class="sum-label">积分净变动</div>
        <div class="sum-value" :class="summary.pointsNet >= 0 ? 'income' : 'expense'">
          {{ summary.pointsNet >= 0 ? '+' : '' }}{{ summary.pointsNet ?? 0 }}
        </div>
        <div class="sum-sub">获得 {{ summary.pointsIncome ?? 0 }} · 消耗 {{ summary.pointsExpense ?? 0 }}</div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-head">
        <div class="panel-title">
          账单明细
          <span class="panel-count">{{ records.length }} 条</span>
        </div>
        <div class="filters">
          <button
            v-for="f in filters"
            :key="f.value"
            class="filter-btn"
            :class="{ active: activeType === f.value }"
            @click="activeType = f.value"
          >
            {{ f.label }}
            <span v-if="typeCounts[f.value]">{{ typeCounts[f.value] }}</span>
          </button>
        </div>
      </div>

      <div v-loading="loading" class="ledger-list">
        <div v-for="(r, idx) in filteredRecords" :key="idx" class="ledger-row">
          <div class="lr-time">{{ r.time || '-' }}</div>
          <span class="lr-type" :class="r.type">{{ r.typeLabel }}</span>
          <div class="lr-main">
            <div class="lr-title">{{ r.title }}</div>
            <div class="lr-remark" v-if="r.remark">{{ r.remark }}</div>
          </div>
          <div class="lr-amount" :class="r.type === 'points' ? 'points' : (r.direction > 0 ? 'income' : 'expense')">
            <template v-if="r.type === 'points'">
              {{ r.direction > 0 ? '+' : '-' }}{{ r.amount }} <span class="unit">分</span>
            </template>
            <template v-else>
              {{ r.direction > 0 ? '+' : '-' }}¥{{ money(r.amount) }}
            </template>
          </div>
        </div>

        <el-empty
          v-if="!loading && !filteredRecords.length"
          :description="records.length ? '当前类型暂无明细' : '这个月还没有账单记录'"
          :image-size="80"
        />
      </div>

      <div class="panel-foot">
        有疑问的订单可在「我的订单」里核对；导出 CSV 含时间、类型、明细、金额与备注。
      </div>
    </section>
  </ServiceShell>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import ServiceShell from '../components/ServiceShell.vue'
import { getLedger } from '../api/finance'

const now = new Date()
const month = ref(`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`)
const loading = ref(false)
const records = ref([])
const summary = ref({
  income: 0,
  errandExpense: 0,
  cashNet: 0,
  pointsIncome: 0,
  pointsExpense: 0,
  pointsNet: 0
})
const activeType = ref('all')

const filters = [
  { label: '全部', value: 'all' },
  { label: '卖出收入', value: 'income' },
  { label: '跑腿支出', value: 'errand' },
  { label: '积分变动', value: 'points' }
]

const load = async () => {
  loading.value = true
  try {
    const res = await getLedger(month.value)
    summary.value = res?.summary || summary.value
    records.value = Array.isArray(res?.list) ? res.list : []
  } catch (e) {
    records.value = []
  } finally {
    loading.value = false
  }
}

const filteredRecords = computed(() => {
  if (activeType.value === 'all') return records.value
  return records.value.filter(r => r.type === activeType.value)
})

const typeCounts = computed(() => {
  const c = { all: records.value.length, income: 0, errand: 0, points: 0 }
  records.value.forEach(r => {
    if (c[r.type] != null) c[r.type]++
  })
  return c
})

const counts = computed(() => ({
  income: typeCounts.value.income,
  errand: typeCounts.value.errand
}))

const money = (v) => Number(v || 0).toFixed(2)

const csvCell = (v) => {
  const s = v == null ? '' : String(v)
  return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s
}

const exportCsv = () => {
  const header = ['时间', '类型', '明细', '金额', '备注']
  const lines = [header.map(csvCell).join(',')]
  records.value.forEach(r => {
    const amount = r.type === 'points'
      ? `${r.direction > 0 ? '+' : '-'}${r.amount}分`
      : `${r.direction > 0 ? '+' : '-'}${money(r.amount)}`
    lines.push([r.time, r.typeLabel, r.title, amount, r.remark].map(csvCell).join(','))
  })

  const blob = new Blob(['\ufeff' + lines.join('\r\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `营收账单-${month.value}.csv`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

onMounted(load)
</script>

<style scoped>
.month-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  padding: 14px 18px;
  margin-bottom: 14px;
  background: #fff;
  border: 1px solid #EDEEF0;
  border-radius: 12px;
}

.month-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.month-tip {
  font-size: 12px;
  color: #A5AAB3;
}

.month-actions {
  display: flex;
  gap: 8px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 14px;
}

.sum-card {
  padding: 16px 18px;
  background: #fff;
  border: 1px solid #EDEEF0;
  border-radius: 12px;
}

.sum-card.main {
  background: linear-gradient(135deg, #FFF1F0 0%, #FFFFFF 70%);
  border-color: #FFE0DD;
}

.sum-label {
  font-size: 12px;
  color: #8A8F99;
}

.sum-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 800;
  color: #1F2937;
  line-height: 1.15;
  word-break: break-all;
}

.sum-value.income {
  color: #E1251B;
}

.sum-value.expense {
  color: #2B7A4B;
}

.sum-sub {
  margin-top: 6px;
  font-size: 12px;
  color: #A5AAB3;
}

.panel {
  background: #fff;
  border: 1px solid #EDEEF0;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 20px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 14px 18px;
  border-bottom: 1px solid #F2F3F5;
}

.panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #1F2937;
}

.panel-count {
  margin-left: 6px;
  font-size: 12px;
  font-weight: 400;
  color: #A5AAB3;
}

.filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-btn {
  height: 30px;
  padding: 0 12px;
  border: 1px solid #EDEEF0;
  border-radius: 15px;
  background: #fff;
  color: #6B7280;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.16s ease;
}

.filter-btn span {
  margin-left: 4px;
  color: #B0B4BA;
}

.filter-btn:hover {
  border-color: #FFD4D0;
  color: #E1251B;
}

.filter-btn.active {
  border-color: #E1251B;
  background: #FFF1F0;
  color: #E1251B;
  font-weight: 700;
}

.ledger-list {
  min-height: 180px;
}

.ledger-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 13px 18px;
  border-bottom: 1px solid #F7F8FA;
}

.ledger-row:last-child {
  border-bottom: none;
}

.lr-time {
  width: 132px;
  flex-shrink: 0;
  font-size: 12px;
  color: #A5AAB3;
}

.lr-type {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
}

.lr-type.income {
  background: #FFF1F0;
  color: #E1251B;
}

.lr-type.errand {
  background: #EEF7F1;
  color: #2B7A4B;
}

.lr-type.points {
  background: #F0F4FF;
  color: #3B5BDB;
}

.lr-main {
  flex: 1;
  min-width: 0;
}

.lr-title {
  font-size: 14px;
  font-weight: 600;
  color: #1F2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lr-remark {
  margin-top: 3px;
  font-size: 12px;
  color: #A5AAB3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lr-amount {
  flex-shrink: 0;
  font-size: 15px;
  font-weight: 800;
}

.lr-amount.income {
  color: #E1251B;
}

.lr-amount.expense {
  color: #2B7A4B;
}

.lr-amount.points {
  color: #3B5BDB;
}

.lr-amount .unit {
  font-size: 11px;
  font-weight: 400;
}

.panel-foot {
  padding: 12px 18px;
  border-top: 1px solid #F2F3F5;
  font-size: 12px;
  color: #A5AAB3;
}

@media (max-width: 900px) {
  .summary-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .ledger-row {
    flex-wrap: wrap;
    gap: 6px 10px;
  }

  .lr-time {
    width: auto;
  }

  .lr-main {
    flex-basis: 100%;
    order: 3;
  }

  .lr-amount {
    margin-left: auto;
  }
}
</style>
