<template>
  <ServiceShell title="评审团" subtitle="互评打分 · 大众投票 · 真实才展示" badge="评">
    <!-- 规则与概览 -->
    <section class="hero-panel">
      <div class="hero-main">
        <div class="hero-num">{{ summary.pending || 0 }}</div>
        <div class="hero-label">待评审评价</div>
        <div class="hero-sub">
          已展示 {{ summary.displayed || 0 }} 条 · 我的评价 {{ summary.mine || 0 }} 条 ·
          可评价订单 {{ summary.canReview || 0 }} 单
        </div>
        <div class="hero-rule">
          交易完成后买卖双方互相打分并提交理由、证据；其他用户投票判断真实性，满
          {{ summary.minVotes || 3 }} 票且支持率超过 {{ summary.threshold || 60 }}% 才公开展示。
        </div>
      </div>
      <div class="hero-actions">
        <el-button type="primary" @click="openCreate">发起评价</el-button>
        <el-button :loading="loading" @click="loadAll">刷新</el-button>
      </div>
    </section>

    <!-- 筛选 -->
    <div class="filter-bar">
      <button
        v-for="f in filters"
        :key="f.value"
        class="filter-btn"
        :class="{ active: filter === f.value }"
        @click="changeFilter(f.value)"
      >
        {{ f.label }}
      </button>
      <span class="filter-tip">{{ filterTip }}</span>
    </div>

    <!-- 评价列表 -->
    <div v-loading="loading" class="review-list">
      <article v-for="r in reviews" :key="r.id" class="review-card">
        <div class="rc-head">
          <SmartImage :src="r.reviewerAvatar" class="avatar" />
          <div class="rc-who">
            <div class="rc-names">
              <b>{{ r.reviewerName || '用户' }}</b>
              <span class="role-tag" :class="r.reviewerRole">{{ r.reviewerRole === 'buyer' ? '买家' : '卖家' }}</span>
              <span class="rc-verb">评价</span>
              <b>{{ r.revieweeName || '用户' }}</b>
            </div>
            <div class="rc-meta">
              <span class="stars">
                <i v-for="n in 5" :key="n" :class="{ on: n <= r.rating }">★</i>
              </span>
              <span>{{ formatTime(r.createTime) }}</span>
              <span v-if="r.orderNo" class="order-no">订单 {{ r.orderNo }}</span>
            </div>
          </div>
          <span class="status-tag" :class="r.status">{{ statusText(r.status) }}</span>
        </div>

        <div class="rc-goods" v-if="r.goodsTitle" @click="goGoods(r)">
          <span class="rc-goods-label">交易商品</span>
          <span class="rc-goods-title">{{ r.goodsTitle }}</span>
          <span class="rc-goods-arrow">›</span>
        </div>

        <p class="rc-reason">{{ r.reason }}</p>

        <div class="rc-evidence" v-if="evidenceList(r).length">
          <SmartImage
            v-for="(img, i) in evidenceList(r)"
            :key="i"
            :src="img"
            ratio="1 / 1"
            class="ev-thumb"
            @click="openViewer(r, i)"
          />
        </div>

        <div class="rc-foot">
          <div class="vote-stat">
            <div class="vote-bar">
              <span :style="{ width: Math.min(100, Number(r.approveRate) || 0) + '%' }"></span>
            </div>
            <span class="vote-text">
              支持率 {{ rateText(r) }} · 支持 {{ r.displayVotes || 0 }} / 共 {{ r.totalVotes || 0 }} 票
            </span>
          </div>

          <div class="vote-actions" v-if="r.status === 'pending' && canVote(r)">
            <button class="vote-btn support" :class="{ active: r.myVote === 1 }" @click="handleVote(r, 1)">
              {{ r.myVote === 1 ? '已支持展示' : '支持展示' }}
            </button>
            <button class="vote-btn oppose" :class="{ active: r.myVote === 0 }" @click="handleVote(r, 0)">
              {{ r.myVote === 0 ? '已反对展示' : '不展示' }}
            </button>
          </div>
          <span v-else-if="r.status === 'pending'" class="vote-note">
            {{ isInvolved(r) ? '交易当事人不可参与投票' : '等待其他用户投票评审' }}
          </span>
          <span v-else class="vote-note" :class="r.status">
            {{ r.status === 'displayed' ? '已通过大众评审' : '未通过大众评审' }}
          </span>
        </div>
      </article>

      <el-empty v-if="!loading && !reviews.length" :description="emptyText" :image-size="80" />
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

    <!-- 发起评价 -->
    <el-dialog v-model="createVisible" title="发起评价" width="560px" :close-on-click-modal="false">
      <div v-if="!orders.length" class="no-orders">
        暂无可评价的订单。订单完成后，买卖双方才能互相评价。
      </div>
      <template v-else>
        <div class="form-row">
          <label>选择订单</label>
          <el-select v-model="form.orderId" placeholder="选择已完成的订单" style="width: 100%">
            <el-option
              v-for="o in orders"
              :key="o.orderId"
              :label="`${o.goodsTitle || '订单'} · ${o.reviewerRole === 'buyer' ? '评价卖家' : '评价买家'} ${o.revieweeName || ''}`"
              :value="o.orderId"
            />
          </el-select>
        </div>

        <div class="form-row">
          <label>评分</label>
          <div class="star-pick">
            <i v-for="n in 5" :key="n" :class="{ on: n <= form.rating }" @click="form.rating = n">★</i>
            <span class="star-text">{{ ratingLabel }}</span>
          </div>
        </div>

        <div class="form-row">
          <label>评价理由</label>
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="如实说明交易过程、商品情况与对方表现"
          />
        </div>

        <div class="form-row">
          <label>证据图片 <em>最多 3 张</em></label>
          <div class="upload-row">
            <SmartImage
              v-for="(img, i) in form.evidence"
              :key="img"
              :src="img"
              ratio="1 / 1"
              class="up-thumb"
              @click="removeEvidence(i)"
            />
            <label v-if="form.evidence.length < 3" class="up-add">
              <input type="file" accept="image/*" :disabled="uploading" @change="handleUpload" />
              <span>{{ uploading ? '上传中…' : '+ 上传' }}</span>
            </label>
          </div>
          <div class="form-hint">点击已上传图片可移除</div>
        </div>
      </template>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="!orders.length" @click="handleSubmit">
          提交评价
        </el-button>
      </template>
    </el-dialog>

    <el-image-viewer
      v-if="showViewer"
      :url-list="viewerUrls"
      :initial-index="viewerIndex"
      teleported
      @close="showViewer = false"
    />
  </ServiceShell>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ServiceShell from '../components/ServiceShell.vue'
import SmartImage from '../components/SmartImage.vue'
import { uploadImage } from '../api/upload'
import { resolveImage } from '../utils/image'
import {
  createJuryReview,
  getJuryReviewableOrders,
  getJuryReviews,
  getJurySummary,
  voteJuryReview
} from '../api/jury'

const router = useRouter()

const filters = [
  { value: 'pending', label: '评审中' },
  { value: 'displayed', label: '已展示' },
  { value: 'mine', label: '我的' }
]

const summary = ref({})
const reviews = ref([])
const orders = ref([])

const loading = ref(false)
const filter = ref('pending')
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)

const createVisible = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const form = reactive({ orderId: null, rating: 5, reason: '', evidence: [] })

const showViewer = ref(false)
const viewerUrls = ref([])
const viewerIndex = ref(0)

const myId = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || '{}').id
  } catch (e) {
    return null
  }
})

const filterTip = computed(() => ({
  pending: '投票判断评价是否真实可信',
  displayed: '已通过大众评审、真实可信的评价',
  mine: '我发出的评价与我收到的评价'
}[filter.value] || ''))

const emptyText = computed(() => ({
  pending: '暂无待评审的评价',
  displayed: '暂无通过评审的评价',
  mine: '你还没有发出或收到评价'
}[filter.value] || '暂无评价'))

const ratingLabel = computed(() => {
  return {
    1: '很差',
    2: '较差',
    3: '一般',
    4: '满意',
    5: '非常满意'
  }[form.rating] || ''
})

const loadSummary = async () => {
  try {
    summary.value = (await getJurySummary()) || {}
  } catch (e) {
    summary.value = {}
  }
}

const loadOrders = async () => {
  try {
    const res = await getJuryReviewableOrders()
    orders.value = Array.isArray(res) ? res : []
  } catch (e) {
    orders.value = []
  }
}

const loadReviews = async () => {
  try {
    const res = await getJuryReviews({ filter: filter.value, pageNum: pageNum.value, pageSize })
    reviews.value = (res && res.list) ? res.list : []
    total.value = (res && typeof res.total === 'number') ? res.total : reviews.value.length
  } catch (e) {
    reviews.value = []
    total.value = 0
  }
}

const loadAll = async () => {
  loading.value = true
  try {
    await Promise.all([loadSummary(), loadReviews(), loadOrders()])
  } finally {
    loading.value = false
  }
}

const changeFilter = (value) => {
  if (filter.value === value) return
  filter.value = value
  pageNum.value = 1
  loadReviews()
}

const onPageChange = (page) => {
  pageNum.value = page
  loadReviews()
}

const statusText = (s) => ({ pending: '评审中', displayed: '已展示', hidden: '不展示' }[s] || '评审中')

const rateText = (r) => {
  if (!r.totalVotes) return '暂无投票'
  const v = Number(r.approveRate) || 0
  return `${v.toFixed(1).replace(/\.0$/, '')}%`
}

const isInvolved = (r) => myId.value != null && (r.reviewerId === myId.value || r.revieweeId === myId.value)

const canVote = (r) => r.status === 'pending' && !isInvolved(r)

const evidenceList = (r) => {
  if (!r.evidence) return []
  return String(r.evidence).split(',').map(s => s.trim()).filter(Boolean)
}

const openViewer = (r, index) => {
  viewerUrls.value = evidenceList(r).map(u => resolveImage(u))
  viewerIndex.value = index
  showViewer.value = true
}

const handleVote = async (r, vote) => {
  try {
    const res = await voteJuryReview(r.id, vote)
    if (res) {
      r.displayVotes = res.displayVotes
      r.hideVotes = res.hideVotes
      r.totalVotes = res.totalVotes
      r.approveRate = res.approveRate
      r.status = res.status
      r.myVote = vote
    }
    ElMessage.success((res && res.message) || '投票成功')
    await loadSummary()
    // 结果已出（展示/不展示）时从「评审中」列表移除
    if (res && res.status !== 'pending' && filter.value === 'pending') {
      reviews.value = reviews.value.filter(item => item.id !== r.id)
      total.value = Math.max(0, total.value - 1)
      if (!reviews.value.length && pageNum.value > 1) {
        pageNum.value -= 1
        loadReviews()
      }
    }
  } catch (e) {}
}

const openCreate = async () => {
  if (!orders.value.length) await loadOrders()
  form.orderId = orders.value.length ? orders.value[0].orderId : null
  form.rating = 5
  form.reason = ''
  form.evidence = []
  createVisible.value = true
}

const handleUpload = async (e) => {
  const file = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!file) return
  if (!file.type || !file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 5MB')
    return
  }
  uploading.value = true
  try {
    const res = await uploadImage(file)
    const path = typeof res === 'string' ? res : (res && res.url ? res.url : '')
    if (path) form.evidence.push(path)
    else ElMessage.error('上传失败，请重试')
  } catch (err) {
  } finally {
    uploading.value = false
  }
}

const removeEvidence = (index) => {
  form.evidence.splice(index, 1)
}

const handleSubmit = async () => {
  if (!form.orderId) {
    ElMessage.warning('请选择要评价的订单')
    return
  }
  if (!form.rating) {
    ElMessage.warning('请给出评分')
    return
  }
  if (!form.reason.trim()) {
    ElMessage.warning('请填写评价理由')
    return
  }
  submitting.value = true
  try {
    const res = await createJuryReview({
      orderId: form.orderId,
      rating: form.rating,
      reason: form.reason.trim(),
      evidence: form.evidence
    })
    ElMessage.success((res && res.message) || '评价已提交，等待评审')
    createVisible.value = false
    filter.value = 'mine'
    pageNum.value = 1
    await loadAll()
  } catch (e) {
  } finally {
    submitting.value = false
  }
}

const goGoods = (r) => {
  if (r.goodsId) router.push(`/goods/${r.goodsId}`)
}

const formatTime = (v) => {
  if (!v) return ''
  const d = new Date(v)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

onMounted(loadAll)
</script>

<style scoped>
.hero-panel {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 20px 22px;
  margin-bottom: 14px;
  border-radius: 14px;
  background: linear-gradient(135deg, #FFF1F0 0%, #FFFFFF 62%);
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

.hero-rule {
  max-width: 640px;
  margin-top: 10px;
  padding: 8px 10px;
  font-size: 12px;
  line-height: 1.6;
  color: #C81E18;
  background: rgba(225, 37, 27, 0.06);
  border-radius: 8px;
}

.hero-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.filter-btn {
  height: 32px;
  padding: 0 16px;
  border: 1px solid #E5E7EB;
  border-radius: 16px;
  background: #fff;
  color: #4B5563;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.16s ease;
}

.filter-btn:hover {
  color: #E1251B;
  border-color: #FFD9D5;
}

.filter-btn.active {
  color: #fff;
  background: #E1251B;
  border-color: #E1251B;
  font-weight: 600;
}

.filter-tip {
  font-size: 12px;
  color: #A5AAB3;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 220px;
  padding-bottom: 10px;
}

.review-card {
  background: #fff;
  border: 1px solid #EDEEF0;
  border-radius: 12px;
  padding: 16px;
}

.rc-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  flex-shrink: 0;
  overflow: hidden;
}

.rc-who {
  flex: 1;
  min-width: 0;
}

.rc-names {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #1F2937;
  flex-wrap: wrap;
}

.role-tag {
  height: 18px;
  line-height: 18px;
  padding: 0 6px;
  font-size: 10px;
  border-radius: 5px;
  color: #C81E18;
  background: #FFF1F0;
  border: 1px solid #FFD9D5;
}

.role-tag.seller {
  color: #1D4ED8;
  background: #EFF6FF;
  border-color: #DBEAFE;
}

.rc-verb {
  font-size: 12px;
  color: #9AA0A6;
}

.rc-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
  font-size: 12px;
  color: #9AA0A6;
  flex-wrap: wrap;
}

.stars {
  display: inline-flex;
  gap: 1px;
}

.stars i {
  font-style: normal;
  font-size: 13px;
  color: #E5E7EB;
}

.stars i.on {
  color: #FF9F1C;
}

.order-no {
  color: #C9CDD4;
}

.status-tag {
  flex-shrink: 0;
  height: 22px;
  line-height: 22px;
  padding: 0 9px;
  font-size: 11px;
  border-radius: 11px;
  background: #F3F4F6;
  color: #6B7280;
}

.status-tag.pending {
  background: #FFF7E6;
  color: #D97706;
}

.status-tag.displayed {
  background: #ECFDF5;
  color: #059669;
}

.status-tag.hidden {
  background: #F3F4F6;
  color: #9CA3AF;
}

.rc-goods {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 7px 10px;
  border-radius: 8px;
  background: #FAFBFC;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
}

.rc-goods:hover .rc-goods-title {
  color: #E1251B;
}

.rc-goods-label {
  flex-shrink: 0;
  font-size: 11px;
  color: #9AA0A6;
}

.rc-goods-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rc-goods-arrow {
  color: #C9CDD4;
}

.rc-reason {
  margin: 12px 0 0;
  font-size: 14px;
  line-height: 1.7;
  color: #374151;
  white-space: pre-wrap;
  word-break: break-word;
}

.rc-evidence {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.ev-thumb {
  width: 86px;
  height: 86px;
  border-radius: 8px;
  overflow: hidden;
  cursor: zoom-in;
  border: 1px solid #F0F1F3;
}

.rc-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed #F0F1F3;
}

.vote-stat {
  flex: 1;
  min-width: 200px;
}

.vote-bar {
  height: 6px;
  border-radius: 3px;
  background: #F0F1F3;
  overflow: hidden;
}

.vote-bar span {
  display: block;
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, #FF7A45, #E1251B);
  transition: width 0.25s ease;
}

.vote-text {
  display: inline-block;
  margin-top: 6px;
  font-size: 12px;
  color: #8A8F99;
}

.vote-actions {
  display: flex;
  gap: 8px;
}

.vote-btn {
  height: 30px;
  padding: 0 14px;
  border-radius: 15px;
  border: 1px solid #E5E7EB;
  background: #fff;
  font-size: 12px;
  color: #4B5563;
  cursor: pointer;
  transition: all 0.16s ease;
}

.vote-btn:hover {
  border-color: #FFD9D5;
  color: #E1251B;
}

.vote-btn.support.active {
  color: #fff;
  background: #E1251B;
  border-color: #E1251B;
}

.vote-btn.oppose.active {
  color: #fff;
  background: #6B7280;
  border-color: #6B7280;
}

.vote-note {
  font-size: 12px;
  color: #9AA0A6;
}

.vote-note.displayed {
  color: #059669;
}

.pager {
  display: flex;
  justify-content: center;
  margin: 6px 0 30px;
}

.no-orders {
  padding: 26px 0;
  text-align: center;
  font-size: 13px;
  color: #9AA0A6;
}

.form-row {
  margin-bottom: 16px;
}

.form-row > label {
  display: block;
  margin-bottom: 7px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.form-row > label em {
  font-style: normal;
  font-weight: 400;
  font-size: 11px;
  color: #A5AAB3;
}

.star-pick {
  display: flex;
  align-items: center;
  gap: 4px;
}

.star-pick i {
  font-style: normal;
  font-size: 26px;
  line-height: 1;
  color: #E5E7EB;
  cursor: pointer;
  transition: color 0.15s ease, transform 0.15s ease;
}

.star-pick i:hover {
  transform: scale(1.12);
}

.star-pick i.on {
  color: #FF9F1C;
}

.star-text {
  margin-left: 8px;
  font-size: 13px;
  color: #8A8F99;
}

.upload-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.up-thumb {
  width: 84px;
  height: 84px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #F0F1F3;
}

.up-add {
  width: 84px;
  height: 84px;
  border: 1px dashed #D9DCE1;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #9AA0A6;
  cursor: pointer;
  background: #FAFBFC;
  transition: all 0.16s ease;
}

.up-add:hover {
  border-color: #E1251B;
  color: #E1251B;
}

.up-add input {
  display: none;
}

.form-hint {
  margin-top: 6px;
  font-size: 11px;
  color: #B0B4BA;
}
</style>
