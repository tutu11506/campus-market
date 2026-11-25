<template>
  <ServiceShell title="任务大厅" subtitle="发个悬赏同学接，顺手赚点零花钱" badge="务">
    <!-- 顶部数据 + 发布入口 -->
    <section class="task-hero">
      <div class="hero-left">
        <div class="hero-title">校园里的忙，互相搭把手</div>
        <div class="hero-desc">取快递、搬宿舍、辅导作业都能发任务；有时间就接单，面交完成当场结算。</div>
        <div class="hero-tags">
          <span>⚡ 发布 1 分钟</span>
          <span>🤝 同校面交</span>
          <span>💰 悬赏自定</span>
        </div>
      </div>
      <div class="hero-right">
        <div class="hero-stat">
          <span class="hero-num">{{ hallTotal }}</span>
          <span class="hero-label">个任务</span>
        </div>
        <button class="publish-btn" @click="openPublish">＋ 发布任务</button>
      </div>
    </section>

    <!-- 标签页 -->
    <div class="tab-row">
      <span class="tab" :class="{ active: activeTab === 'hall' }" @click="switchTab('hall')">任务大厅</span>
      <span class="tab" :class="{ active: activeTab === 'published' }" @click="switchTab('published')">我发布的</span>
      <span class="tab" :class="{ active: activeTab === 'accepted' }" @click="switchTab('accepted')">我接的单</span>
      <div class="tab-tools" v-if="activeTab === 'hall'">
        <el-select v-model="statusFilter" size="small" style="width: 110px" @change="reloadHall">
          <el-option label="全部状态" value="all" />
          <el-option label="待接单" value="open" />
          <el-option label="进行中" value="accepted" />
          <el-option label="已完成" value="completed" />
        </el-select>
        <el-input
          v-model="keyword"
          size="small"
          placeholder="搜索任务关键词"
          clearable
          style="width: 180px"
          @keyup.enter="reloadHall"
          @clear="reloadHall"
        />
      </div>
    </div>

    <!-- 任务大厅 -->
    <div v-show="activeTab === 'hall'">
      <div class="task-grid" v-if="hallList.length">
        <div class="task-card" v-for="t in hallList" :key="'ht-' + t.id">
          <div class="task-reward">
            <span class="rw-symbol">¥</span>
            <span class="rw-num">{{ formatReward(t.reward) }}</span>
            <span class="rw-label">悬赏</span>
          </div>

          <div class="task-main">
            <div class="task-top">
              <span class="task-cat">{{ t.category || '校园跑腿' }}</span>
              <span class="task-status" :class="statusOf(t.status).cls">{{ statusOf(t.status).text }}</span>
            </div>
            <div class="task-title">{{ t.title }}</div>
            <div class="task-desc" v-if="t.description">{{ t.description }}</div>
            <div class="task-meta">
              <span v-if="t.location">📍 {{ t.location }}</span>
              <span>⏰ {{ deadlineText(t) }}</span>
              <span>👤 {{ t.publisherName || '同学' }}</span>
            </div>
          </div>

          <div class="task-actions">
            <button v-if="canAccept(t)" class="btn-primary" @click="handleAccept(t)">接单</button>
            <button v-else-if="isMine(t) && t.status === 'open'" class="btn-ghost" @click="handleCancel(t)">取消任务</button>
            <button v-else-if="isMine(t) && t.status === 'accepted'" class="btn-primary" @click="handleFinish(t)">确认完成</button>
            <button v-else-if="t.status === 'accepted' && t.accepterId === currentUserId" class="btn-ghost" disabled>进行中</button>
            <button v-else class="btn-ghost" disabled>{{ statusOf(t.status).text }}</button>
            <button class="btn-text" @click="showDetail(t)">详情</button>
          </div>
        </div>
      </div>

      <div class="empty" v-else-if="!loading">
        <div class="empty-title">还没有符合条件的任务</div>
        <div class="empty-desc">换个筛选条件，或者发布第一个任务试试</div>
      </div>
      <div class="loading" v-if="loading">加载中...</div>

      <div class="more-row" v-if="hasMore && hallList.length">
        <button class="btn-more" @click="loadMore">加载更多</button>
      </div>
    </div>

    <!-- 我发布的 -->
    <div v-show="activeTab === 'published'">
      <div class="task-grid" v-if="myPublished.length">
        <div class="task-card" v-for="t in myPublished" :key="'mp-' + t.id">
          <div class="task-reward">
            <span class="rw-symbol">¥</span>
            <span class="rw-num">{{ formatReward(t.reward) }}</span>
            <span class="rw-label">悬赏</span>
          </div>
          <div class="task-main">
            <div class="task-top">
              <span class="task-cat">{{ t.category || '校园跑腿' }}</span>
              <span class="task-status" :class="statusOf(t.status).cls">{{ statusOf(t.status).text }}</span>
            </div>
            <div class="task-title">{{ t.title }}</div>
            <div class="task-meta">
              <span v-if="t.location">📍 {{ t.location }}</span>
              <span>⏰ {{ deadlineText(t) }}</span>
              <span v-if="t.accepterName">🙋 接单人：{{ t.accepterName }}</span>
            </div>
          </div>
          <div class="task-actions">
            <button v-if="t.status === 'accepted'" class="btn-primary" @click="handleFinish(t)">确认完成</button>
            <button v-if="t.status === 'open'" class="btn-ghost" @click="handleCancel(t)">取消任务</button>
            <button class="btn-text" @click="showDetail(t)">详情</button>
          </div>
        </div>
      </div>
      <div class="empty" v-else-if="!mineLoading">
        <div class="empty-title">你还没有发布过任务</div>
        <div class="empty-desc">点击右上角「发布任务」，把跑腿需求丢给同学</div>
      </div>
    </div>

    <!-- 我接的单 -->
    <div v-show="activeTab === 'accepted'">
      <div class="task-grid" v-if="myAccepted.length">
        <div class="task-card" v-for="t in myAccepted" :key="'ma-' + t.id">
          <div class="task-reward">
            <span class="rw-symbol">¥</span>
            <span class="rw-num">{{ formatReward(t.reward) }}</span>
            <span class="rw-label">悬赏</span>
          </div>
          <div class="task-main">
            <div class="task-top">
              <span class="task-cat">{{ t.category || '校园跑腿' }}</span>
              <span class="task-status" :class="statusOf(t.status).cls">{{ statusOf(t.status).text }}</span>
            </div>
            <div class="task-title">{{ t.title }}</div>
            <div class="task-meta">
              <span v-if="t.location">📍 {{ t.location }}</span>
              <span>⏰ {{ deadlineText(t) }}</span>
              <span>👤 发布人：{{ t.publisherName || '同学' }}</span>
            </div>
          </div>
          <div class="task-actions">
            <button class="btn-ghost" disabled>{{ t.status === 'accepted' ? '待发布人确认' : statusOf(t.status).text }}</button>
            <button class="btn-text" @click="showDetail(t)">详情</button>
          </div>
        </div>
      </div>
      <div class="empty" v-else-if="!mineLoading">
        <div class="empty-title">还没有接过任务</div>
        <div class="empty-desc">在大厅里挑一个合适的接单，赚点零花钱</div>
      </div>
    </div>

    <!-- 发布任务弹窗 -->
    <el-dialog v-model="publishVisible" title="发布任务" width="480px" append-to-body>
      <div class="form-item">
        <label>任务标题 <i>*</i></label>
        <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="例如：帮取快递到南区宿舍" />
      </div>
      <div class="form-row">
        <div class="form-item">
          <label>任务类型</label>
          <el-select v-model="form.category" style="width: 100%">
            <el-option label="跑腿代取" value="跑腿代取" />
            <el-option label="学习辅导" value="学习辅导" />
            <el-option label="体力帮忙" value="体力帮忙" />
            <el-option label="其他任务" value="其他任务" />
          </el-select>
        </div>
        <div class="form-item">
          <label>悬赏金额（元）</label>
          <el-input v-model="form.reward" type="number" placeholder="0 表示免费帮忙" />
        </div>
      </div>
      <div class="form-row">
        <div class="form-item">
          <label>任务地点</label>
          <el-input v-model="form.location" maxlength="100" placeholder="例如：北区食堂门口" />
        </div>
        <div class="form-item">
          <label>期望完成时间</label>
          <el-date-picker
            v-model="form.deadline"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选填"
            style="width: 100%"
          />
        </div>
      </div>
      <div class="form-item">
        <label>任务描述</label>
        <el-input v-model="form.description" type="textarea" :rows="3" maxlength="800" show-word-limit placeholder="补充说明：物品大小、楼层、注意事项等" />
      </div>
      <div class="form-actions">
        <button class="btn-ghost" @click="publishVisible = false">取消</button>
        <button class="btn-primary" :disabled="publishing" @click="submitPublish">
          {{ publishing ? '发布中...' : '发布任务' }}
        </button>
      </div>
    </el-dialog>

    <!-- 任务详情弹窗 -->
    <el-dialog v-model="detailVisible" title="任务详情" width="440px" append-to-body>
      <div v-if="detail">
        <div class="detail-top">
          <span class="detail-reward">¥{{ formatReward(detail.reward) }}</span>
          <span class="task-status" :class="statusOf(detail.status).cls">{{ statusOf(detail.status).text }}</span>
        </div>
        <div class="detail-title">{{ detail.title }}</div>
        <div class="detail-desc">{{ detail.description || '发布人没有填写更多说明' }}</div>
        <div class="detail-rows">
          <div><span>任务类型</span><b>{{ detail.category || '校园跑腿' }}</b></div>
          <div><span>任务地点</span><b>{{ detail.location || '待商定' }}</b></div>
          <div><span>完成时间</span><b>{{ deadlineText(detail) }}</b></div>
          <div><span>发布人</span><b>{{ detail.publisherName || '同学' }}</b></div>
          <div v-if="detail.accepterName"><span>接单人</span><b>{{ detail.accepterName }}</b></div>
          <div><span>发布时间</span><b>{{ fmtTime(detail.createTime) }}</b></div>
        </div>
      </div>
    </el-dialog>
  </ServiceShell>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ServiceShell from '../components/ServiceShell.vue'
import {
  getTaskList, getTaskDetail, publishTask, acceptTask,
  finishTask, cancelTask, getMyPublished, getMyAccepted
} from '../api/task'

const router = useRouter()

const activeTab = ref('hall')
const loading = ref(false)
const mineLoading = ref(false)
const publishing = ref(false)

const hallList = ref([])
const hallTotal = ref(0)
const pageNum = ref(1)
const hasMore = ref(false)
const statusFilter = ref('all')
const keyword = ref('')

const myPublished = ref([])
const myAccepted = ref([])

const currentUser = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || '{}')
  } catch (e) {
    return {}
  }
})
const currentUserId = computed(() => currentUser.value.id || null)
const isLoggedIn = computed(() => !!localStorage.getItem('token'))

const STATUS_MAP = {
  open: { text: '待接单', cls: 'st-open' },
  accepted: { text: '进行中', cls: 'st-accepted' },
  completed: { text: '已完成', cls: 'st-completed' },
  cancelled: { text: '已取消', cls: 'st-cancelled' }
}
const statusOf = (s) => STATUS_MAP[s] || { text: '未知', cls: 'st-cancelled' }

const isMine = (t) => !!currentUserId.value && t.publisherId === currentUserId.value
const canAccept = (t) => isLoggedIn.value && t.status === 'open' && !isMine(t)

const formatReward = (v) => {
  const n = Number(v || 0)
  return n % 1 === 0 ? String(n) : n.toFixed(2)
}

// 兼容后端返回的时间：时间戳 / ISO("2026-11-21T15:07:41") / "yyyy-MM-dd HH:mm:ss"
const parseDate = (v) => {
  if (v == null) return null
  if (typeof v === 'number') return new Date(v)
  const s = String(v)
  const d = new Date(s.includes('T') ? s : s.replace(/-/g, '/'))
  return isNaN(d.getTime()) ? null : d
}

const fmtTime = (v) => {
  const d = parseDate(v)
  if (!d) return '—'
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

const deadlineText = (t) => {
  if (!t.deadline) return '不限时间'
  const d = parseDate(t.deadline)
  if (!d) return '不限时间'
  const diff = d.getTime() - Date.now()
  if (diff <= 0) return '已过期望时间'
  const mins = Math.floor(diff / 60000)
  if (mins < 60) return Math.max(1, mins) + ' 分钟后截止'
  const hours = Math.floor(mins / 60)
  if (hours < 24) return hours + ' 小时后截止'
  return Math.floor(hours / 24) + ' 天后截止'
}

const loadHall = async (append = false) => {
  if (append) {
    pageNum.value += 1
  } else {
    pageNum.value = 1
  }
  loading.value = true
  try {
    const res = await getTaskList({
      status: statusFilter.value,
      keyword: keyword.value || undefined,
      pageNum: pageNum.value,
      pageSize: 10
    })
    const list = (res && res.list) || []
    hallList.value = append ? [...hallList.value, ...list] : list
    hallTotal.value = (res && res.total) || 0
    hasMore.value = !!(res && res.hasMore)
  } catch (e) {
    if (!append) hallList.value = []
  } finally {
    loading.value = false
  }
}

const reloadHall = () => loadHall(false)
const loadMore = () => loadHall(true)

const loadMine = async () => {
  if (!isLoggedIn.value) return
  mineLoading.value = true
  try {
    const [pub, acc] = await Promise.all([getMyPublished(), getMyAccepted()])
    myPublished.value = pub || []
    myAccepted.value = acc || []
  } catch (e) {
    myPublished.value = []
    myAccepted.value = []
  } finally {
    mineLoading.value = false
  }
}

const switchTab = (tab) => {
  if (tab !== 'hall' && !isLoggedIn.value) {
    ElMessage.warning('登录后才能查看自己的任务')
    router.push('/login?redirect=/task')
    return
  }
  activeTab.value = tab
  if (tab === 'hall') reloadHall()
  else loadMine()
}

// 发布
const publishVisible = ref(false)
const form = reactive({
  title: '',
  category: '跑腿代取',
  reward: '',
  location: '',
  deadline: '',
  description: ''
})

const openPublish = () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录再发布任务')
    router.push('/login?redirect=/task')
    return
  }
  publishVisible.value = true
}

const submitPublish = async () => {
  if (!form.title.trim()) {
    ElMessage.warning('请填写任务标题')
    return
  }
  publishing.value = true
  try {
    await publishTask({
      title: form.title.trim(),
      category: form.category,
      reward: form.reward === '' ? 0 : Number(form.reward),
      location: form.location.trim(),
      deadline: form.deadline || '',
      description: form.description.trim()
    })
    ElMessage.success('任务发布成功')
    publishVisible.value = false
    form.title = ''
    form.reward = ''
    form.location = ''
    form.deadline = ''
    form.description = ''
    await reloadHall()
    await loadMine()
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    publishing.value = false
  }
}

// 接单 / 完成 / 取消
const handleAccept = async (t) => {
  try {
    await ElMessageBox.confirm(`确认接下「${t.title}」？悬赏 ¥${formatReward(t.reward)}`, '接单确认', {
      confirmButtonText: '确认接单',
      cancelButtonText: '再看看',
      type: 'info'
    })
  } catch (e) { return }
  try {
    await acceptTask(t.id)
    ElMessage.success('接单成功，记得联系发布人')
    await reloadHall()
    await loadMine()
  } catch (e) { /* 拦截器已提示 */ }
}

const handleFinish = async (t) => {
  try {
    await ElMessageBox.confirm(`确认「${t.title}」已经完成？`, '确认完成', {
      confirmButtonText: '确认完成',
      cancelButtonText: '还没好',
      type: 'warning'
    })
  } catch (e) { return }
  try {
    await finishTask(t.id)
    ElMessage.success('任务已完成')
    await reloadHall()
    await loadMine()
  } catch (e) { /* 拦截器已提示 */ }
}

const handleCancel = async (t) => {
  try {
    await ElMessageBox.confirm(`确定取消「${t.title}」吗？`, '取消任务', {
      confirmButtonText: '确认取消',
      cancelButtonText: '不取消',
      type: 'warning'
    })
  } catch (e) { return }
  try {
    await cancelTask(t.id)
    ElMessage.success('任务已取消')
    await reloadHall()
    await loadMine()
  } catch (e) { /* 拦截器已提示 */ }
}

// 详情
const detailVisible = ref(false)
const detail = ref(null)

const showDetail = async (t) => {
  detail.value = t
  detailVisible.value = true
  try {
    const res = await getTaskDetail(t.id)
    if (res) detail.value = res
  } catch (e) { /* 保留列表数据兜底 */ }
}

onMounted(() => {
  loadHall()
  if (isLoggedIn.value) loadMine()
})
</script>

<style scoped>
.task-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: linear-gradient(120deg, #FFF3F0 0%, #FFFFFF 65%);
  border: 1px solid #FFE0D9;
  border-radius: 14px;
  padding: 18px 20px;
  flex-wrap: wrap;
}

.hero-title { font-size: 18px; font-weight: 800; color: #1F2937; }
.hero-desc { margin-top: 6px; font-size: 13px; color: #6B7280; max-width: 520px; line-height: 1.6; }

.hero-tags {
  display: flex;
  gap: 14px;
  margin-top: 10px;
  font-size: 12px;
  color: #6B7280;
}

.hero-right { display: flex; align-items: center; gap: 16px; }

.hero-stat { display: flex; flex-direction: column; align-items: center; }
.hero-num { font-size: 26px; font-weight: 800; color: #E1251B; line-height: 1.1; }
.hero-label { font-size: 11px; color: #9CA3AF; }

.publish-btn {
  height: 38px;
  padding: 0 20px;
  border: none;
  border-radius: 19px;
  background: linear-gradient(135deg, #FF7A45, #E1251B);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 6px 16px rgba(225, 37, 27, 0.28);
  transition: transform 0.16s ease;
}

.publish-btn:active { transform: scale(0.96); }

.tab-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 16px 0 12px;
  flex-wrap: wrap;
}

.tab {
  padding: 6px 16px;
  font-size: 13px;
  color: #4B5563;
  background: #fff;
  border: 1px solid #EFF0F2;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.16s ease;
}

.tab.active {
  background: #E1251B;
  border-color: #E1251B;
  color: #fff;
  font-weight: 700;
}

.tab-tools { margin-left: auto; display: flex; gap: 8px; }

.task-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.task-card {
  display: flex;
  gap: 14px;
  background: #fff;
  border: 1px solid #EFF0F2;
  border-radius: 14px;
  padding: 14px 16px;
  transition: all 0.18s ease;
}

.task-card:hover {
  border-color: #FFC2B8;
  box-shadow: 0 6px 18px rgba(225, 37, 27, 0.1);
}

.task-reward {
  width: 72px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #FFF3F0;
  border-radius: 10px;
  padding: 8px 4px;
}

.rw-symbol { font-size: 11px; color: #E1251B; font-weight: 700; }
.rw-num { font-size: 22px; font-weight: 800; color: #E1251B; line-height: 1.15; }
.rw-label { font-size: 10px; color: #9CA3AF; }

.task-main { flex: 1; min-width: 0; }

.task-top { display: flex; align-items: center; gap: 8px; }

.task-cat {
  font-size: 10px;
  color: #B45309;
  background: #FEF3C7;
  padding: 2px 8px;
  border-radius: 8px;
}

.task-status {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 8px;
}

.st-open { background: #ECFDF5; color: #059669; }
.st-accepted { background: #DBEAFE; color: #1D4ED8; }
.st-completed { background: #F3F4F6; color: #6B7280; }
.st-cancelled { background: #FEE2E2; color: #B91C1C; }

.task-title {
  margin-top: 6px;
  font-size: 14px;
  font-weight: 700;
  color: #1F2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-desc {
  margin-top: 4px;
  font-size: 12px;
  color: #9CA3AF;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
}

.task-meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 6px;
  font-size: 11px;
  color: #9CA3AF;
}

.task-actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  flex-shrink: 0;
}

.btn-primary,
.btn-ghost,
.btn-text,
.btn-more {
  border: none;
  cursor: pointer;
  font-size: 12px;
  border-radius: 14px;
  padding: 6px 14px;
  transition: all 0.16s ease;
  white-space: nowrap;
}

.btn-primary {
  background: linear-gradient(135deg, #FF7A45, #E1251B);
  color: #fff;
  font-weight: 700;
}

.btn-ghost {
  background: #fff;
  border: 1px solid #E5E7EB;
  color: #6B7280;
}

.btn-ghost:disabled { color: #C0C4CC; cursor: not-allowed; }

.btn-text {
  background: transparent;
  color: #9CA3AF;
  padding: 2px 6px;
}

.btn-text:hover { color: #E1251B; }

.btn-more {
  background: #fff;
  border: 1px solid #E5E7EB;
  color: #4B5563;
  padding: 8px 26px;
}

.more-row { text-align: center; margin-top: 14px; }

.empty {
  background: #fff;
  border: 1px solid #EFF0F2;
  border-radius: 14px;
  padding: 46px 0;
  text-align: center;
}

.empty-title { font-size: 14px; color: #4B5563; }
.empty-desc { margin-top: 6px; font-size: 12px; color: #9CA3AF; }

.loading { text-align: center; padding: 24px 0; color: #9CA3AF; font-size: 13px; }

/* 弹窗表单 */
.form-item { margin-bottom: 12px; }
.form-item label { display: block; font-size: 12px; color: #6B7280; margin-bottom: 6px; }
.form-item label i { color: #E1251B; font-style: normal; }
.form-row { display: flex; gap: 12px; }
.form-row .form-item { flex: 1; }

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 6px;
}

.form-actions .btn-primary,
.form-actions .btn-ghost { padding: 8px 20px; }

/* 详情 */
.detail-top { display: flex; align-items: center; gap: 10px; }
.detail-reward { font-size: 22px; font-weight: 800; color: #E1251B; }
.detail-title { margin-top: 8px; font-size: 15px; font-weight: 700; color: #1F2937; }
.detail-desc {
  margin-top: 6px;
  font-size: 13px;
  color: #6B7280;
  line-height: 1.7;
  background: #FAFBFC;
  border-radius: 8px;
  padding: 10px 12px;
}

.detail-rows { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; }
.detail-rows > div { display: flex; justify-content: space-between; font-size: 12px; }
.detail-rows span { color: #9CA3AF; }
.detail-rows b { color: #374151; font-weight: 600; }

@media (max-width: 900px) {
  .task-grid { grid-template-columns: 1fr; }
  .tab-tools { margin-left: 0; width: 100%; }
  .form-row { flex-direction: column; gap: 0; }
}
</style>
