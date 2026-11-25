<template>
  <ServiceShell title="自动化中心" subtitle="定时扫描库存、临期、滞销与缺货，异常自动提醒" badge="自">
    <!-- 顶部统计 + 扫描 -->
    <section class="auto-hero">
      <div class="hero-left">
        <div class="hero-title">5 条自动化规则 24 小时值守</div>
        <div class="hero-desc">
          启动后自动扫描一次，之后每 6 小时扫描一遍；同一条告警每天只提醒一次，不会重复打扰。
        </div>
        <div class="hero-meta">
          <span class="meta-item">
            <i class="pulse"></i>
            {{ overview.lastScanTime ? '上次扫描 ' + overview.lastScanTime : '等待首次扫描' }}
          </span>
          <span class="meta-item" v-if="overview.lastGenerated !== null && overview.lastGenerated !== undefined">
            上次新增 {{ overview.lastGenerated }} 条
          </span>
        </div>
      </div>
      <button class="scan-btn" :disabled="scanning" @click="doScan">
        <el-icon :size="15"><Refresh /></el-icon>
        {{ scanning ? '扫描中...' : '立即扫描' }}
      </button>
    </section>

    <!-- 数据卡 -->
    <div class="stat-row">
      <div class="stat-card">
        <span class="stat-num">{{ stats.todayTotal || 0 }}</span>
        <span class="stat-label">今日新增预警</span>
      </div>
      <div class="stat-card">
        <span class="stat-num warn">{{ stats.openTotal || 0 }}</span>
        <span class="stat-label">待处理</span>
      </div>
      <div class="stat-card">
        <span class="stat-num danger">{{ stats.highOpen || 0 }}</span>
        <span class="stat-label">高优先级待处理</span>
      </div>
      <div class="stat-card">
        <span class="stat-num">{{ rules.length }}</span>
        <span class="stat-label">启用规则</span>
      </div>
    </div>

    <!-- 规则卡 -->
    <section class="panel">
      <div class="panel-head">
        <span class="panel-title">⚙️ 自动化规则</span>
        <span class="panel-sub">点击规则查看对应预警</span>
      </div>
      <div class="rule-grid">
        <div
          class="rule-card"
          v-for="r in rules"
          :key="r.code"
          :class="{ active: ruleCode === r.code }"
          @click="filterByRule(r.code)"
        >
          <div class="rule-top">
            <span class="rule-name">{{ r.name }}</span>
            <span class="level-tag" :class="'lv-' + r.level">{{ levelText(r.level) }}</span>
          </div>
          <div class="rule-desc">{{ r.desc }}</div>
          <div class="rule-threshold">触发条件：{{ r.threshold }}</div>
          <div class="rule-foot">
            <span class="rule-today">今日 {{ r.todayCount || 0 }} 条</span>
            <span class="rule-open">待处理 {{ r.openCount || 0 }} 条</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 预警列表 -->
    <section class="panel" id="alert-list">
      <div class="panel-head">
        <span class="panel-title">🔔 预警记录</span>
        <span class="panel-sub">共 {{ alertTotal }} 条</span>
      </div>

      <div class="filter-row">
        <span
          class="filter-chip"
          v-for="f in statusFilters"
          :key="f.value"
          :class="{ active: statusFilter === f.value }"
          @click="setStatus(f.value)"
        >{{ f.label }}</span>

        <el-select v-model="ruleCode" size="small" style="width: 160px; margin-left: auto" placeholder="全部规则" @change="reloadAlerts">
          <el-option label="全部规则" value="" />
          <el-option v-for="r in rules" :key="'opt-' + r.code" :label="r.name" :value="r.code" />
        </el-select>
      </div>

      <div class="alert-list" v-if="alerts.length">
        <div class="alert-row" v-for="a in alerts" :key="'al-' + a.id" :class="{ resolved: a.status === 'resolved' }">
          <span class="alert-level" :class="'dot-' + a.level" :title="levelText(a.level)"></span>
          <div class="alert-main">
            <div class="alert-title">
              {{ a.title }}
              <span class="alert-rule">{{ ruleName(a.ruleCode) }}</span>
            </div>
            <div class="alert-content">{{ a.content }}</div>
            <div class="alert-meta">
              <span>{{ fmtDate(a.createTime) }}</span>
              <span class="alert-status" :class="a.status">{{ a.status === 'open' ? '待处理' : '已处理' }}</span>
            </div>
          </div>
          <button v-if="a.status === 'open'" class="resolve-btn" @click="handleResolve(a)">标记处理</button>
        </div>
      </div>

      <div class="empty" v-else-if="!loading">
        <div class="empty-title">当前没有预警</div>
        <div class="empty-desc">系统会在发现库存、临期等问题时自动生成</div>
      </div>

      <div class="loading" v-if="loading">加载中...</div>

      <div class="more-row" v-if="alertTotal > alerts.length">
        <button class="btn-more" @click="loadMore">加载更多</button>
      </div>
    </section>
  </ServiceShell>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import ServiceShell from '../components/ServiceShell.vue'
import {
  getAutomationOverview, getAutomationAlerts, scanAutomation, resolveAutomationAlert
} from '../api/automation'

const overview = ref({})
const rules = ref([])
const stats = ref({})

const alerts = ref([])
const alertTotal = ref(0)
const pageNum = ref(1)
const pageSize = 20
const statusFilter = ref('open')
const ruleCode = ref('')
const loading = ref(false)
const scanning = ref(false)

const statusFilters = [
  { label: '待处理', value: 'open' },
  { label: '已处理', value: 'resolved' },
  { label: '全部', value: 'all' }
]

const levelText = (lv) => (lv === 'high' ? '高' : lv === 'medium' ? '中' : '低')

const ruleName = (code) => {
  const r = rules.value.find(x => x.code === code)
  return r ? r.name.replace('预警', '') : '预警'
}

const fmtDate = (v) => {
  if (!v) return ''
  const d = new Date(typeof v === 'number' ? v : String(v).includes('T') ? String(v) : String(v).replace(/-/g, '/'))
  if (isNaN(d.getTime())) return String(v)
  const p = (n) => String(n).padStart(2, '0')
  return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

const loadOverview = async () => {
  try {
    const res = await getAutomationOverview()
    overview.value = res || {}
    rules.value = (res && res.rules) || []
    stats.value = (res && res.stats) || {}
  } catch (e) {
    overview.value = {}
  }
}

const loadAlerts = async (append = false) => {
  if (append) pageNum.value += 1
  else pageNum.value = 1
  loading.value = true
  try {
    const res = await getAutomationAlerts({
      ruleCode: ruleCode.value || 'all',
      status: statusFilter.value,
      pageNum: pageNum.value,
      pageSize
    })
    const list = (res && res.list) || []
    alerts.value = append ? [...alerts.value, ...list] : list
    alertTotal.value = (res && res.total) || 0
  } catch (e) {
    if (!append) alerts.value = []
  } finally {
    loading.value = false
  }
}

const reloadAlerts = () => loadAlerts(false)
const loadMore = () => loadAlerts(true)

const setStatus = (v) => {
  statusFilter.value = v
  reloadAlerts()
}

const filterByRule = (code) => {
  ruleCode.value = ruleCode.value === code ? '' : code
  statusFilter.value = 'all'
  reloadAlerts()
  const el = document.getElementById('alert-list')
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const doScan = async () => {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('登录后才能手动扫描')
    return
  }
  scanning.value = true
  try {
    const res = await scanAutomation()
    ElMessage.success(`扫描完成，新增 ${res && res.generated !== undefined ? res.generated : 0} 条预警`)
    await loadOverview()
    await loadAlerts(false)
  } catch (e) {
    // 拦截器已提示
  } finally {
    scanning.value = false
  }
}

const handleResolve = async (a) => {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('登录后才能处理预警')
    return
  }
  try {
    await resolveAutomationAlert(a.id)
    a.status = 'resolved'
    ElMessage.success('已标记处理')
    await loadOverview()
  } catch (e) {
    // 拦截器已提示
  }
}

onMounted(async () => {
  await loadOverview()
  await loadAlerts(false)
})
</script>

<style scoped>
.auto-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: linear-gradient(120deg, #EFF6FF 0%, #fff 65%);
  border: 1px solid #DBEAFE;
  border-radius: 14px;
  padding: 18px 20px;
  flex-wrap: wrap;
}

.hero-title { font-size: 17px; font-weight: 800; color: #1E3A8A; }
.hero-desc { margin-top: 6px; font-size: 12px; color: #6B7280; line-height: 1.7; max-width: 620px; }

.hero-meta {
  display: flex;
  gap: 14px;
  margin-top: 10px;
  font-size: 12px;
  color: #6B7280;
  flex-wrap: wrap;
}

.meta-item { display: inline-flex; align-items: center; gap: 6px; }

.pulse {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #10B981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.18);
}

.scan-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 20px;
  border: none;
  border-radius: 19px;
  background: linear-gradient(135deg, #3B82F6, #1D4ED8);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 6px 16px rgba(29, 78, 216, 0.25);
  transition: transform 0.16s ease;
}

.scan-btn:active { transform: scale(0.96); }
.scan-btn:disabled { opacity: 0.7; cursor: not-allowed; }

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-top: 14px;
}

.stat-card {
  background: #fff;
  border: 1px solid #EFF0F2;
  border-radius: 12px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-num { font-size: 24px; font-weight: 800; color: #1F2937; }
.stat-num.warn { color: #D97706; }
.stat-num.danger { color: #DC2626; }
.stat-label { font-size: 12px; color: #9CA3AF; }

.panel {
  margin-top: 14px;
  background: #fff;
  border: 1px solid #EFF0F2;
  border-radius: 14px;
  padding: 14px 16px;
}

.panel-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
}

.panel-title { font-size: 15px; font-weight: 700; color: #1F2937; }
.panel-sub { font-size: 11px; color: #9CA3AF; }

.rule-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(215px, 1fr));
  gap: 10px;
}

.rule-card {
  border: 1px solid #EFF0F2;
  border-radius: 11px;
  padding: 12px 14px;
  background: #FAFBFC;
  cursor: pointer;
  transition: all 0.18s ease;
}

.rule-card:hover { border-color: #BFDBFE; background: #F8FAFF; transform: translateY(-2px); }
.rule-card.active { border-color: #3B82F6; background: #EFF6FF; box-shadow: 0 6px 16px rgba(59, 130, 246, 0.12); }

.rule-top { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.rule-name { font-size: 13px; font-weight: 700; color: #1F2937; }

.level-tag { font-size: 9px; font-weight: 700; padding: 2px 7px; border-radius: 8px; }
.lv-high { background: #FEE2E2; color: #DC2626; }
.lv-medium { background: #FEF3C7; color: #B45309; }
.lv-low { background: #F3F4F6; color: #6B7280; }

.rule-desc { margin-top: 6px; font-size: 11px; color: #9CA3AF; line-height: 1.6; min-height: 34px; }
.rule-threshold { margin-top: 4px; font-size: 11px; color: #1D4ED8; background: #EFF6FF; display: inline-block; padding: 2px 8px; border-radius: 6px; }

.rule-foot {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #EEF0F3;
  font-size: 11px;
}

.rule-today { color: #6B7280; }
.rule-open { color: #D97706; font-weight: 700; }

.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.filter-chip {
  padding: 5px 14px;
  font-size: 12px;
  color: #4B5563;
  background: #F5F6F8;
  border-radius: 14px;
  cursor: pointer;
}

.filter-chip.active { background: #E1251B; color: #fff; font-weight: 700; }

.alert-list { display: flex; flex-direction: column; gap: 8px; }

.alert-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 14px;
  background: #FAFBFC;
  border: 1px solid #EFF0F2;
  border-radius: 11px;
  transition: all 0.16s ease;
}

.alert-row:hover { border-color: #E5E7EB; background: #fff; }
.alert-row.resolved { opacity: 0.62; }

.alert-level { width: 8px; height: 8px; border-radius: 50%; margin-top: 6px; flex-shrink: 0; }
.dot-high { background: #EF4444; box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.15); }
.dot-medium { background: #F59E0B; box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.15); }
.dot-low { background: #9CA3AF; }

.alert-main { flex: 1; min-width: 0; }

.alert-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  color: #1F2937;
  flex-wrap: wrap;
}

.alert-rule {
  font-size: 9px;
  font-weight: 600;
  color: #6B7280;
  background: #F3F4F6;
  padding: 1px 7px;
  border-radius: 7px;
}

.alert-content { margin-top: 4px; font-size: 12px; color: #6B7280; line-height: 1.6; }

.alert-meta {
  display: flex;
  gap: 10px;
  margin-top: 6px;
  font-size: 10px;
  color: #B0B4BA;
}

.alert-status.open { color: #D97706; }
.alert-status.resolved { color: #10B981; }

.resolve-btn {
  flex-shrink: 0;
  height: 28px;
  padding: 0 12px;
  border: 1px solid #E5E7EB;
  border-radius: 14px;
  background: #fff;
  color: #6B7280;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.16s ease;
}

.resolve-btn:hover { border-color: #10B981; color: #059669; }

.empty { padding: 36px 0; text-align: center; }
.empty-title { font-size: 14px; color: #4B5563; }
.empty-desc { margin-top: 6px; font-size: 12px; color: #9CA3AF; }

.loading { text-align: center; padding: 22px 0; color: #9CA3AF; font-size: 13px; }

.more-row { text-align: center; margin-top: 12px; }

.btn-more {
  padding: 8px 26px;
  border: 1px solid #E5E7EB;
  border-radius: 16px;
  background: #fff;
  color: #4B5563;
  font-size: 12px;
  cursor: pointer;
}

@media (max-width: 900px) {
  .stat-row { grid-template-columns: repeat(2, 1fr); }
}
</style>
