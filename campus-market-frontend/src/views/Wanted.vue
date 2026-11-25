<template>
  <ServiceShell title="求购专区" subtitle="发个求购，卖家上门报价" badge="求">
    <!-- 顶部说明 -->
    <div class="wanted-hero">
      <div class="wh-text">
        <div class="wh-title">想要什么，发个求购试试</div>
        <div class="wh-desc">收教材、收电动车、收桌椅都行。卖家接单报价，系统还会按关键词自动匹配在售商品推给你。</div>
      </div>
      <div class="wh-actions">
        <button class="wh-btn primary" @click="goPublish">＋ 发布求购</button>
        <button class="wh-btn" :class="{ active: mineOnly }" @click="toggleMine">我的求购</button>
      </div>
    </div>

    <!-- 分类与搜索 -->
    <div class="wanted-toolbar">
      <div class="cat-tabs">
        <span
          class="cat-tab"
          v-for="c in categories"
          :key="c.value"
          :class="{ active: category === c.value }"
          @click="changeCategory(c.value)"
        >{{ c.label }}</span>
      </div>
      <div class="wanted-search">
        <el-input v-model="keyword" placeholder="搜求购标题 / 描述" clearable @keyup.enter="search" @clear="search" />
        <button class="search-btn" @click="search">搜索</button>
      </div>
    </div>

    <!-- 求购列表 -->
    <div class="wanted-grid" v-if="list.length">
      <div class="wanted-card" v-for="d in list" :key="d.id" @click="openDetail(d.id)">
        <div class="wc-top">
          <span class="wc-cat" :class="'cat-' + d.category">{{ catLabel(d.category) }}</span>
          <span class="wc-status" :class="d.status">{{ statusText(d.status) }}</span>
        </div>
        <div class="wc-title">{{ d.title }}</div>
        <div class="wc-budget" v-if="d.budgetMin != null || d.budgetMax != null">
          <span class="wc-yen">预算</span>
          ¥{{ d.budgetMin != null ? d.budgetMin : 0 }} - ¥{{ d.budgetMax != null ? d.budgetMax : '不限' }}
        </div>
        <div class="wc-desc">{{ d.description || '发布者没有填写详细描述' }}</div>
        <div class="wc-targets" v-if="d.targetGrade || d.targetMajor">
          <span v-if="d.targetGrade">🎓 {{ d.targetGrade }}</span>
          <span v-if="d.targetMajor">🎯 {{ d.targetMajor }}</span>
        </div>
        <div class="wc-foot">
          <span class="wc-author">👤 {{ d.authorName || '同学' }}</span>
          <span class="wc-quotes">💬 {{ d.quoteCount || 0 }} 人报价</span>
          <span class="wc-match">📦 匹配 {{ d.matchCount || 0 }} 件</span>
        </div>
      </div>
    </div>
    <div class="empty" v-else>
      <p>{{ mineOnly ? '你还没有发布过求购' : '还没有求购，发一个试试？' }}</p>
      <button class="wh-btn primary" @click="goPublish">发布求购</button>
    </div>

    <div class="pager" v-if="total > pageState.pageSize">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="total"
        :current-page="pageState.pageNum"
        :page-size="pageState.pageSize"
        @current-change="changePage"
      />
    </div>

    <!-- 求购详情 -->
    <el-dialog v-model="detailVisible" title="求购详情" width="660px" append-to-body>
      <div v-if="detail" class="dd">
        <div class="dd-head">
          <span class="wc-cat" :class="'cat-' + detail.demand.category">{{ catLabel(detail.demand.category) }}</span>
          <span class="wc-status" :class="detail.demand.status">{{ statusText(detail.demand.status) }}</span>
          <span class="dd-time">{{ fmtTime(detail.demand.createTime) }}</span>
        </div>
        <div class="dd-title">{{ detail.demand.title }}</div>
        <div class="dd-budget" v-if="detail.demand.budgetMin != null || detail.demand.budgetMax != null">
          预算 ¥{{ detail.demand.budgetMin != null ? detail.demand.budgetMin : 0 }} - ¥{{ detail.demand.budgetMax != null ? detail.demand.budgetMax : '不限' }}
        </div>
        <div class="dd-desc">{{ detail.demand.description || '发布者没有填写详细描述' }}</div>
        <div class="dd-tags" v-if="detail.demand.targetGrade || detail.demand.targetMajor">
          <span v-if="detail.demand.targetGrade">🎓 定向年级：{{ detail.demand.targetGrade }}</span>
          <span v-if="detail.demand.targetMajor">🎯 定向专业：{{ detail.demand.targetMajor }}</span>
        </div>
        <div class="dd-author">发布者：{{ detail.demand.authorName || '同学' }} · 关键词「{{ detail.demand.keyword }}」</div>

        <!-- 系统匹配 -->
        <div class="dd-section-title">
          <span>📦 系统匹配的在售商品</span>
          <span class="dd-match-count">{{ detail.matches.length }} 件</span>
        </div>
        <div class="match-row" v-if="detail.matches.length">
          <div class="match-card" v-for="g in detail.matches" :key="'m-' + g.id" @click="goGoods(g.id)">
            <SmartImage :src="g.coverImage" class="match-img" alt="" />
            <div class="match-title">{{ g.title }}</div>
            <div class="match-price">¥{{ g.price }}</div>
          </div>
        </div>
        <div class="dd-empty" v-else>暂时没有匹配的在售商品，发布后卖家可以主动报价</div>

        <!-- 报价列表 -->
        <div class="dd-section-title">
          <span>💬 卖家报价</span>
          <span class="dd-match-count">{{ detail.quotes.length }} 条</span>
        </div>
        <div class="quote-list" v-if="detail.quotes.length">
          <div class="quote-item" v-for="q in detail.quotes" :key="'q-' + q.id">
            <img class="quote-avatar" :src="resolveAvatar(q.sellerAvatar)" alt="" />
            <div class="quote-main">
              <div class="quote-top">
                <b>{{ q.sellerName || '同学' }}</b>
                <span class="quote-status" :class="q.status">{{ quoteStatusText(q.status) }}</span>
              </div>
              <div class="quote-msg" v-if="q.message">{{ q.message }}</div>
              <div class="quote-time">{{ fmtTime(q.createTime) }}</div>
            </div>
            <div class="quote-right">
              <span class="quote-price">¥{{ q.price }}</span>
              <button
                v-if="isOwner && detail.demand.status === 'open' && q.status === 'pending'"
                class="accept-btn"
                @click="acceptQuote(q)"
              >选他</button>
            </div>
          </div>
        </div>
        <div class="dd-empty" v-else>还没有卖家报价，等等看～</div>

        <!-- 报价表单 / 我的报价 -->
        <div class="dd-section-title" v-if="!isOwner">
          <span>✍️ 我要报价</span>
        </div>
        <div class="quote-form" v-if="!isOwner">
          <template v-if="detail.demand.status !== 'open'">
            <div class="dd-empty">该求购已结束</div>
          </template>
          <template v-else-if="detail.myQuote">
            <div class="my-quote">
              你已报价 <b>¥{{ detail.myQuote.price }}</b>
              <span class="quote-status" :class="detail.myQuote.status">{{ quoteStatusText(detail.myQuote.status) }}</span>
            </div>
          </template>
          <template v-else>
            <div class="qf-row">
              <el-input v-model="quotePrice" placeholder="你的报价（元）" type="number" style="width: 170px" />
              <el-input v-model="quoteMsg" placeholder="留言：成色、可面交时间等" maxlength="500" />
              <button class="accept-btn" :disabled="quoting" @click="submitQuote">{{ quoting ? '提交中…' : '提交报价' }}</button>
            </div>
          </template>
        </div>

        <div class="dd-footer" v-if="isOwner && detail.demand.status === 'open'">
          <button class="close-btn" @click="closeMyDemand">关闭求购</button>
        </div>
      </div>
    </el-dialog>
  </ServiceShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ServiceShell from '../components/ServiceShell.vue'
import SmartImage from '../components/SmartImage.vue'
import { resolveAvatar } from '../utils/avatar'
import {
  getDemandList, getDemandDetail, quoteDemand, acceptDemandQuote, closeDemand
} from '../api/demand'

const route = useRoute()
const router = useRouter()

const categories = [
  { value: '', label: '全部' },
  { value: 'textbook', label: '教材书籍' },
  { value: 'vehicle', label: '电动车/代步' },
  { value: 'furniture', label: '桌椅家具' },
  { value: 'digital', label: '数码电子' },
  { value: 'dorm', label: '宿舍用品' },
  { value: 'other', label: '其他' }
]

const CAT_LABEL = {
  textbook: '教材书籍',
  vehicle: '电动车/代步',
  furniture: '桌椅家具',
  digital: '数码电子',
  dorm: '宿舍用品',
  other: '其他'
}
const catLabel = (c) => CAT_LABEL[c] || '其他'
const statusText = (s) => (s === 'matched' ? '已成交' : s === 'closed' ? '已关闭' : '求购中')
const quoteStatusText = (s) => (s === 'accepted' ? '已接单' : s === 'rejected' ? '未选中' : '待选择')

const list = ref([])
const total = ref(0)
const category = ref('')
const keyword = ref('')
const mineOnly = ref(false)
const pageState = { pageNum: 1, pageSize: 8 }

const currentUserId = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || '{}').id
  } catch (e) {
    return null
  }
})

const load = async () => {
  try {
    const res = await getDemandList({
      category: category.value || undefined,
      keyword: keyword.value.trim() || undefined,
      mine: mineOnly.value,
      pageNum: pageState.pageNum,
      pageSize: pageState.pageSize
    })
    list.value = (res && res.list) ? res.list : []
    total.value = (res && res.total) ? res.total : 0
  } catch (e) {
    list.value = []
    total.value = 0
  }
}

const changeCategory = (v) => {
  category.value = v
  pageState.pageNum = 1
  load()
}

const search = () => {
  pageState.pageNum = 1
  load()
}

const toggleMine = () => {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('登录后查看我的求购')
    router.push('/login?redirect=/wanted')
    return
  }
  mineOnly.value = !mineOnly.value
  pageState.pageNum = 1
  load()
}

const changePage = (p) => {
  pageState.pageNum = p
  load()
}

const goPublish = () => {
  router.push('/wanted/publish')
}

const goGoods = (id) => {
  router.push('/goods/' + id)
}

// ==================== 详情 ====================
const detailVisible = ref(false)
const detail = ref(null)
const quotePrice = ref('')
const quoteMsg = ref('')
const quoting = ref(false)

const isOwner = computed(() => {
  return detail.value && detail.value.demand && currentUserId.value === detail.value.demand.userId
})

const openDetail = async (id) => {
  try {
    const res = await getDemandDetail(id)
    detail.value = res
    quotePrice.value = ''
    quoteMsg.value = ''
    detailVisible.value = true
  } catch (e) {
    // 拦截器已提示
  }
}

const submitQuote = async () => {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('登录后才能报价')
    router.push('/login?redirect=/wanted')
    return
  }
  const price = Number(quotePrice.value)
  if (!price || price <= 0) {
    ElMessage.warning('请填写有效报价')
    return
  }
  quoting.value = true
  try {
    await quoteDemand(detail.value.demand.id, { price, message: quoteMsg.value.trim() })
    ElMessage.success('报价已提交，等待对方选择')
    const res = await getDemandDetail(detail.value.demand.id)
    detail.value = res
    load()
  } catch (e) {
    // 拦截器已提示
  } finally {
    quoting.value = false
  }
}

const acceptQuote = async (q) => {
  try {
    await acceptDemandQuote(q.id)
    ElMessage.success('已选定该报价，求购成交')
    const res = await getDemandDetail(detail.value.demand.id)
    detail.value = res
    load()
  } catch (e) {
    // 拦截器已提示
  }
}

const closeMyDemand = async () => {
  try {
    await closeDemand(detail.value.demand.id)
    ElMessage.success('已关闭求购')
    const res = await getDemandDetail(detail.value.demand.id)
    detail.value = res
    load()
  } catch (e) {
    // 拦截器已提示
  }
}

const fmtTime = (v) => {
  if (!v) return ''
  const d = new Date(String(v).includes('T') ? v : String(v).replace(/-/g, '/'))
  if (isNaN(d.getTime())) return ''
  return `${d.getMonth() + 1}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

onMounted(async () => {
  await load()
  const openId = Number(route.query.open)
  if (openId) openDetail(openId)
})
</script>

<style scoped>
.wanted-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px;
  margin-bottom: 12px;
  border-radius: 12px;
  background: linear-gradient(135deg, #FFF7F5 0%, #FFFDFC 60%, #FFF9F4 100%);
  border: 1px solid #FFE7E0;
}

.wh-title {
  font-size: 18px;
  font-weight: 800;
  color: #1F2937;
}

.wh-desc {
  margin-top: 6px;
  font-size: 12.5px;
  line-height: 1.6;
  color: #6B7280;
  max-width: 620px;
}

.wh-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.wh-btn {
  height: 36px;
  padding: 0 16px;
  border: 1px solid #E5E7EB;
  border-radius: 18px;
  background: #fff;
  color: #4B5563;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.wh-btn.primary {
  border: none;
  color: #fff;
  background: linear-gradient(135deg, #FF7A45, #E1251B);
  box-shadow: 0 5px 14px rgba(225, 37, 27, 0.22);
}

.wh-btn.active {
  color: #E1251B;
  border-color: #FFD9D3;
  background: #FFF1F0;
}

.wanted-toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  margin-bottom: 12px;
  border-radius: 10px;
  background: #fff;
}

.cat-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  flex: 1;
}

.cat-tab {
  padding: 6px 12px;
  font-size: 12.5px;
  color: #4B5563;
  border-radius: 15px;
  cursor: pointer;
  user-select: none;
}

.cat-tab:hover {
  color: #E1251B;
  background: #FFF7F5;
}

.cat-tab.active {
  color: #fff;
  background: #E1251B;
  font-weight: 700;
}

.wanted-search {
  display: flex;
  gap: 8px;
  width: 300px;
  flex-shrink: 0;
}

.search-btn {
  padding: 0 14px;
  border: none;
  border-radius: 6px;
  background: #E1251B;
  color: #fff;
  font-size: 12.5px;
  cursor: pointer;
}

.wanted-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.wanted-card {
  padding: 16px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #F0F1F3;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.wanted-card:hover {
  transform: translateY(-3px);
  border-color: #FFD9D3;
  box-shadow: 0 10px 24px rgba(225, 37, 27, 0.08);
}

.wc-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.wc-cat {
  padding: 1px 8px;
  font-size: 11px;
  font-weight: 600;
  border-radius: 9px;
  color: #2563EB;
  background: #EFF6FF;
}

.cat-textbook { color: #7C3AED; background: #F5F3FF; }
.cat-vehicle { color: #059669; background: #ECFDF5; }
.cat-furniture { color: #B45309; background: #FFFBEB; }
.cat-digital { color: #2563EB; background: #EFF6FF; }
.cat-dorm { color: #DB2777; background: #FDF2F8; }

.wc-status {
  margin-left: auto;
  font-size: 11px;
  color: #16A34A;
}

.wc-status.matched,
.wc-status.closed {
  color: #9CA3AF;
}

.wc-title {
  margin-top: 9px;
  font-size: 15px;
  font-weight: 700;
  color: #1F2937;
}

.wc-budget {
  margin-top: 6px;
  font-size: 13px;
  color: #E1251B;
  font-weight: 700;
}

.wc-yen {
  margin-right: 4px;
  font-size: 11px;
  font-weight: 500;
  color: #B45309;
}

.wc-desc {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: #6B7280;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.wc-targets {
  display: flex;
  gap: 8px;
  margin-top: 7px;
  font-size: 11px;
  color: #B45309;
}

.wc-foot {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #F0F1F3;
  font-size: 11px;
  color: #9CA3AF;
}

.wc-match {
  margin-left: auto;
  color: #2563EB;
}

.empty {
  padding: 48px 0;
  text-align: center;
  color: #9CA3AF;
  font-size: 13px;
}

.empty p {
  margin-bottom: 14px;
}

.pager {
  display: flex;
  justify-content: center;
  padding: 20px 0 10px;
}

/* 详情 */
.dd-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dd-time {
  margin-left: auto;
  font-size: 11px;
  color: #B0B4BA;
}

.dd-title {
  margin-top: 10px;
  font-size: 19px;
  font-weight: 800;
  color: #1F2937;
}

.dd-budget {
  margin-top: 6px;
  font-size: 16px;
  font-weight: 800;
  color: #E1251B;
}

.dd-desc {
  margin-top: 10px;
  font-size: 13px;
  line-height: 1.75;
  color: #4B5563;
  white-space: pre-wrap;
}

.dd-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
  font-size: 12px;
  color: #B45309;
}

.dd-author {
  margin-top: 8px;
  font-size: 12px;
  color: #9CA3AF;
}

.dd-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 18px 0 10px;
  padding-top: 14px;
  border-top: 1px solid #F0F1F3;
  font-size: 13px;
  font-weight: 700;
  color: #1F2937;
}

.dd-match-count {
  margin-left: auto;
  font-size: 11px;
  font-weight: 500;
  color: #9CA3AF;
}

.dd-empty {
  padding: 14px;
  border-radius: 8px;
  background: #FAFAFB;
  font-size: 12px;
  color: #9CA3AF;
  text-align: center;
}

.match-row {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.match-card {
  flex: 0 0 118px;
  border: 1px solid #F0F1F3;
  border-radius: 9px;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.16s ease;
}

.match-card:hover {
  border-color: #FFD9D3;
}

.match-img {
  width: 100%;
  height: 86px;
  object-fit: cover;
  display: block;
  background: #F5F5F5;
}

.match-title {
  padding: 6px 8px 0;
  font-size: 11px;
  color: #4B5563;
  line-height: 1.5;
  height: 33px;
  overflow: hidden;
}

.match-price {
  padding: 2px 8px 8px;
  font-size: 12px;
  font-weight: 700;
  color: #E1251B;
}

.quote-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 220px;
  overflow-y: auto;
}

.quote-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #F0F1F3;
  border-radius: 9px;
}

.quote-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  object-fit: cover;
  background: #F5F5F5;
}

.quote-main {
  flex: 1;
  min-width: 0;
}

.quote-top {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: #374151;
}

.quote-status {
  padding: 1px 7px;
  font-size: 10px;
  border-radius: 8px;
  color: #B45309;
  background: #FFFBEB;
}

.quote-status.accepted {
  color: #16A34A;
  background: #ECFDF3;
}

.quote-status.rejected {
  color: #9CA3AF;
  background: #F3F4F6;
}

.quote-msg {
  margin-top: 4px;
  font-size: 12px;
  color: #6B7280;
}

.quote-time {
  margin-top: 3px;
  font-size: 10.5px;
  color: #B0B4BA;
}

.quote-right {
  flex-shrink: 0;
  text-align: right;
}

.quote-price {
  display: block;
  font-size: 16px;
  font-weight: 800;
  color: #E1251B;
}

.accept-btn {
  margin-top: 6px;
  padding: 4px 12px;
  border: none;
  border-radius: 12px;
  background: #E1251B;
  color: #fff;
  font-size: 12px;
  cursor: pointer;
}

.accept-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.qf-row {
  display: flex;
  gap: 8px;
}

.my-quote {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 8px;
  background: #FFF9F8;
  font-size: 13px;
  color: #4B5563;
}

.my-quote b {
  color: #E1251B;
}

.dd-footer {
  margin-top: 16px;
  text-align: right;
}

.close-btn {
  padding: 7px 16px;
  border: 1px solid #E5E7EB;
  border-radius: 16px;
  background: #fff;
  color: #6B7280;
  font-size: 12px;
  cursor: pointer;
}

@media (max-width: 900px) {
  .wanted-grid {
    grid-template-columns: 1fr;
  }
  .wanted-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .wanted-search {
    width: 100%;
  }
  .wanted-hero {
    flex-direction: column;
    align-items: flex-start;
  }
  .qf-row {
    flex-direction: column;
  }
}
</style>
