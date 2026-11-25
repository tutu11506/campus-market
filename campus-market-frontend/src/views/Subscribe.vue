<template>
  <ServiceShell title="消息提醒" subtitle="心愿降价 · 关键词订阅 · 教材上新 · 学期提醒" badge="铃">
    <!-- 概览 -->
    <section class="hero-panel">
      <div class="hero-main">
        <div class="hero-num">{{ summary.unread || 0 }}</div>
        <div class="hero-label">未读提醒</div>
        <div class="hero-sub">
          {{ summary.phaseDesc || '普通时期' }} · 收藏关注 {{ summary.watches || 0 }} 件 ·
          关键词 {{ summary.keywords || 0 }} 个 · 教材心愿 {{ summary.wishes || 0 }} 本
        </div>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading" @click="loadAll">刷新</el-button>
        <el-button type="primary" plain @click="handleCheckSemester">检查学期提醒</el-button>
        <el-button v-if="isAdmin" type="warning" plain :loading="scanning" @click="handleAdminScan">
          管理员演练推送
        </el-button>
      </div>
    </section>

    <div class="sub-grid">
      <!-- 左列：订阅管理 -->
      <div class="sub-col">
        <!-- 关键词订阅 -->
        <section class="card">
          <div class="card-head">
            <h3>关键词订阅</h3>
            <span class="hint">平台上新命中即提醒</span>
          </div>
          <div class="kw-add">
            <el-input
              v-model="keywordInput"
              placeholder="如：电路第十版、电动车"
              maxlength="20"
              clearable
              @keyup.enter="handleAddKeyword"
            />
            <el-button type="primary" :loading="addingKeyword" @click="handleAddKeyword">订阅</el-button>
          </div>
          <div class="chips" v-if="keywords.length">
            <span class="chip" v-for="k in keywords" :key="k.id">
              {{ k.keyword }}
              <em v-if="k.matchCount">{{ k.matchCount }} 次命中</em>
              <i title="取消订阅" @click="handleDeleteKeyword(k)">×</i>
            </span>
          </div>
          <el-empty v-else description="还没有订阅关键词，试试「电路第十版」" :image-size="60" />
        </section>

        <!-- 教材心愿 -->
        <section class="card">
          <div class="card-head">
            <h3>教材心愿</h3>
            <span class="hint">想要的教材上新立刻通知</span>
          </div>
          <div v-if="wishes.length" class="sub-list">
            <div class="sub-row" v-for="w in wishes" :key="w.id">
              <SmartImage :src="w.coverImage" class="row-cover" />
              <div class="row-main">
                <div class="row-title">{{ w.textbookTitle || w.title }}</div>
                <div class="row-sub">
                  {{ w.subject || '未分类' }}<template v-if="w.edition"> · {{ w.edition }}</template>
                </div>
              </div>
              <el-button link type="danger" @click="handleDeleteWish(w)">取消</el-button>
            </div>
          </div>
          <el-empty v-else description="去教材专区点「想要这本书」即可关注" :image-size="60" />
        </section>

        <!-- 收藏降价关注 -->
        <section class="card">
          <div class="card-head">
            <h3>收藏降价关注</h3>
            <span class="hint">收藏即关注，降价自动提醒</span>
          </div>
          <div v-if="watches.length" class="sub-list">
            <div class="sub-row clickable" v-for="w in watches" :key="w.id" @click="goGoods(w.goodsId)">
              <SmartImage :src="w.coverImage" class="row-cover" />
              <div class="row-main">
                <div class="row-title">{{ w.title }}</div>
                <div class="row-sub">
                  当前 ¥{{ w.currentPrice }}
                  <template v-if="w.lastPrice != null"> · 记录价 ¥{{ w.lastPrice }}</template>
                </div>
              </div>
              <span class="row-arrow">›</span>
            </div>
          </div>
          <el-empty v-else description="收藏商品后自动开启降价提醒" :image-size="60" />
        </section>
      </div>

      <!-- 右列：提醒记录 -->
      <div class="sub-col">
        <section class="card records">
          <div class="card-head">
            <h3>提醒记录</h3>
            <el-button link type="primary" :disabled="!summary.unread" @click="handleReadAll">全部已读</el-button>
          </div>
          <div v-loading="loading" class="records-list">
            <div
              v-for="n in notifications"
              :key="n.id"
              class="notify-item"
              :class="{ unread: n.isRead === 0 }"
              @click="openNotify(n)"
            >
              <span class="notify-type" :class="n.notifyType">{{ typeLabel(n.notifyType) }}</span>
              <div class="notify-body">
                <div class="notify-title">{{ n.title }}</div>
                <div class="notify-content">{{ n.content }}</div>
                <div class="notify-time">{{ formatTime(n.createTime) }}</div>
              </div>
              <i class="notify-del" title="删除" @click.stop="handleDeleteNotify(n)">×</i>
            </div>
            <el-empty
              v-if="!loading && notifications.length === 0"
              description="还没有提醒，去订阅试试"
              :image-size="70"
            />
          </div>
          <div class="pager" v-if="total > pageSize">
            <el-pagination
              background
              layout="prev, pager, next"
              :total="total"
              :page-size="pageSize"
              :current-page="pageNum"
              @current-change="onPageChange"
            />
          </div>
        </section>
      </div>
    </div>
  </ServiceShell>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ServiceShell from '../components/ServiceShell.vue'
import SmartImage from '../components/SmartImage.vue'
import {
  addKeyword,
  deleteKeyword,
  deleteNotify,
  deleteTextbookWish,
  getGoodsWatches,
  getKeywords,
  getNotifyList,
  getSubscribeSummary,
  getTextbookWishes,
  markAllNotifyRead,
  markNotifyRead,
  scanSemesterReminder,
  checkSemesterReminder
} from '../api/subscribe'

const router = useRouter()

const summary = ref({})
const keywords = ref([])
const wishes = ref([])
const watches = ref([])
const notifications = ref([])

const loading = ref(false)
const addingKeyword = ref(false)
const keywordInput = ref('')
const scanning = ref(false)

const pageNum = ref(1)
const pageSize = 10
const total = ref(0)

const isAdmin = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || '{}').role === 'admin'
  } catch (e) {
    return false
  }
})

const loadSummary = async () => {
  try {
    summary.value = (await getSubscribeSummary()) || {}
  } catch (e) {
    summary.value = {}
  }
}

const loadNotifications = async () => {
  try {
    const res = await getNotifyList({ pageNum: pageNum.value, pageSize })
    notifications.value = (res && res.list) ? res.list : []
    total.value = (res && typeof res.total === 'number') ? res.total : notifications.value.length
  } catch (e) {
    notifications.value = []
    total.value = 0
  }
}

const loadAll = async () => {
  loading.value = true
  try {
    const [kw, wish, watch] = await Promise.all([
      getKeywords().catch(() => []),
      getTextbookWishes().catch(() => []),
      getGoodsWatches().catch(() => [])
    ])
    keywords.value = Array.isArray(kw) ? kw : []
    wishes.value = Array.isArray(wish) ? wish : []
    watches.value = Array.isArray(watch) ? watch : []
    await Promise.all([loadSummary(), loadNotifications()])
  } finally {
    loading.value = false
  }
}

const handleAddKeyword = async () => {
  const kw = keywordInput.value.trim()
  if (!kw) {
    ElMessage.warning('请先输入关键词')
    return
  }
  addingKeyword.value = true
  try {
    const msg = await addKeyword(kw)
    ElMessage.success(msg || '订阅成功')
    keywordInput.value = ''
    keywords.value = (await getKeywords()) || []
    await loadSummary()
  } catch (e) {
    // 请求拦截器已提示
  } finally {
    addingKeyword.value = false
  }
}

const handleDeleteKeyword = async (k) => {
  try {
    await deleteKeyword(k.id)
    keywords.value = keywords.value.filter(item => item.id !== k.id)
    ElMessage.success('已取消订阅')
    await loadSummary()
  } catch (e) {}
}

const handleDeleteWish = async (w) => {
  try {
    await deleteTextbookWish(w.id)
    wishes.value = wishes.value.filter(item => item.id !== w.id)
    ElMessage.success('已取消教材心愿')
    await loadSummary()
  } catch (e) {}
}

const openNotify = async (n) => {
  if (n.isRead === 0) {
    try {
      await markNotifyRead(n.id)
      n.isRead = 1
      summary.value.unread = Math.max(0, (summary.value.unread || 0) - 1)
      window.dispatchEvent(new CustomEvent('rt:notify'))
    } catch (e) {}
  }
  if (n.link) router.push(n.link)
}

const handleReadAll = async () => {
  try {
    const msg = await markAllNotifyRead()
    ElMessage.success(msg || '已全部标记为已读')
    notifications.value.forEach(n => { n.isRead = 1 })
    summary.value.unread = 0
    window.dispatchEvent(new CustomEvent('rt:notify'))
  } catch (e) {}
}

const handleDeleteNotify = async (n) => {
  try {
    await deleteNotify(n.id)
    notifications.value = notifications.value.filter(item => item.id !== n.id)
    total.value = Math.max(0, total.value - 1)
    await loadSummary()
  } catch (e) {}
}

const onPageChange = (page) => {
  pageNum.value = page
  loadNotifications()
}

const handleCheckSemester = async () => {
  try {
    const msg = await checkSemesterReminder()
    ElMessage.info(msg || '已检查')
    await loadAll()
  } catch (e) {}
}

const handleAdminScan = async () => {
  scanning.value = true
  try {
    const res = await scanSemesterReminder()
    ElMessage.success(`演练完成，本次推送 ${res && res.sent != null ? res.sent : 0} 条`)
    await loadAll()
  } catch (e) {
  } finally {
    scanning.value = false
  }
}

const goGoods = (id) => {
  if (id) router.push(`/goods/${id}`)
}

const typeLabel = (t) => ({
  price_drop: '降价',
  new_goods: '关键词上新',
  textbook_new: '教材上新',
  semester_start: '开学提醒',
  graduation: '毕业提醒'
}[t] || '提醒')

const formatTime = (v) => {
  if (!v) return ''
  const d = new Date(v)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

// 实时推送：收到新提醒立即刷新列表与未读数
const onRealtimeNotify = () => {
  loadAll()
}

onMounted(() => {
  loadAll()
  window.addEventListener('rt:notify', onRealtimeNotify)
})

onUnmounted(() => {
  window.removeEventListener('rt:notify', onRealtimeNotify)
})
</script>

<style scoped>
.hero-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 20px 22px;
  margin-bottom: 14px;
  border-radius: 14px;
  background: linear-gradient(135deg, #FFF1F0 0%, #FFFFFF 60%);
  border: 1px solid #FFE0DD;
}

.hero-num {
  font-size: 32px;
  font-weight: 800;
  color: #E1251B;
  line-height: 1.1;
}

.hero-label {
  font-size: 13px;
  color: #8A8F99;
  margin-top: 2px;
}

.hero-sub {
  font-size: 12px;
  color: #A5AAB3;
  margin-top: 8px;
}

.hero-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.sub-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 14px;
  align-items: start;
  padding-bottom: 30px;
}

.sub-col {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
}

.card {
  background: #fff;
  border: 1px solid #EDEEF0;
  border-radius: 12px;
  padding: 16px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.card-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #1F2937;
}

.hint {
  font-size: 12px;
  color: #A5AAB3;
}

.kw-add {
  display: flex;
  gap: 8px;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  font-size: 13px;
  color: #C81E18;
  background: #FFF1F0;
  border: 1px solid #FFD9D5;
  border-radius: 15px;
}

.chip em {
  font-style: normal;
  font-size: 11px;
  color: #E1251B;
  opacity: 0.75;
}

.chip i {
  font-style: normal;
  cursor: pointer;
  font-size: 15px;
  line-height: 1;
  color: #E1251B;
}

.sub-list {
  display: flex;
  flex-direction: column;
}

.sub-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 0;
  border-bottom: 1px dashed #F0F1F3;
}

.sub-row:last-child {
  border-bottom: none;
}

.sub-row.clickable {
  cursor: pointer;
}

.sub-row.clickable:hover .row-title {
  color: #E1251B;
}

.row-cover {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
  background: #F6F7F9;
}

.row-main {
  flex: 1;
  min-width: 0;
}

.row-title {
  font-size: 13px;
  color: #333;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.row-sub {
  font-size: 12px;
  color: #9AA0A6;
  margin-top: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.row-arrow {
  color: #C9CDD4;
  font-size: 18px;
}

.records {
  min-height: 420px;
}

.records-list {
  display: flex;
  flex-direction: column;
  min-height: 300px;
}

.notify-item {
  position: relative;
  display: flex;
  gap: 10px;
  padding: 12px 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
}

.notify-item:hover {
  background: #FAFBFC;
}

.notify-item.unread {
  background: #FFFAF9;
}

.notify-item.unread .notify-title::before {
  content: '';
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 6px;
  border-radius: 50%;
  background: #E1251B;
  vertical-align: middle;
}

.notify-type {
  flex-shrink: 0;
  height: 20px;
  line-height: 20px;
  padding: 0 7px;
  font-size: 11px;
  border-radius: 6px;
  color: #fff;
  background: #E1251B;
  margin-top: 1px;
}

.notify-type.price_drop { background: #E1251B; }
.notify-type.new_goods { background: #F59E0B; }
.notify-type.textbook_new { background: #2563EB; }
.notify-type.semester_start { background: #10B981; }
.notify-type.graduation { background: #8B5CF6; }

.notify-body {
  flex: 1;
  min-width: 0;
}

.notify-title {
  font-size: 14px;
  font-weight: 600;
  color: #1F2937;
}

.notify-content {
  font-size: 12px;
  color: #6B7280;
  line-height: 1.55;
  margin-top: 4px;
}

.notify-time {
  font-size: 11px;
  color: #B0B4BA;
  margin-top: 6px;
}

.notify-del {
  flex-shrink: 0;
  font-style: normal;
  color: #C9CDD4;
  font-size: 16px;
  line-height: 1;
  padding: 2px 4px;
}

.notify-del:hover {
  color: #E1251B;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

@media (max-width: 900px) {
  .sub-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
