<template>
  <div class="home-page">
    <!-- 左侧功能导航（宽屏显示，点击切换页面） -->
    <AppSidebar />

    <HomeTop
      v-model:keyword="keyword"
      :categories="categoryList"
      :active-category="activeCategory"
      :featured-goods="featuredGoods"
      :quick-covers="quickCovers"
      @search="onHeaderSearch"
      @open-goods="goDetail"
      @select-category="selectCategory"
      @select-sub="selectSubCategory"
    />

    <HomeModules @open-goods="goDetail" @filter-location="selectDormArea" />

    <!-- 为你推荐 -->
    <section class="recommend" ref="recommendRef">
      <div class="recommend-toolbar">
        <div class="rec-head">
          <div class="rec-title">
            <span class="rec-title-icon"></span>
            <span>{{ keyword.trim() ? `“${keyword.trim()}” 的搜索结果` : '为你推荐' }}</span>
            <span class="rec-count" v-if="goodsList.length">{{ total }} 件</span>
          </div>
          <div class="sort-tabs">
            <span
              v-for="opt in sortOptions"
              :key="opt.value"
              class="sort-tab"
              :class="{ active: activeSort === opt.value }"
              @click="selectSort(opt.value)"
            >{{ opt.label }}</span>
          </div>
        </div>

        <div class="price-bar">
          <span class="price-label">价格</span>
          <span
            v-for="opt in priceOptions"
            :key="String(opt.value)"
            class="price-chip"
            :class="{ active: activePrice === opt.value }"
            @click="selectPrice(opt.value)"
          >{{ opt.label }}</span>
          <span class="filter-divider"></span>
          <span class="price-label">交易方式</span>
          <span
            v-for="opt in tradeFilterOptions"
            :key="opt.key"
            class="price-chip trade-filter-chip"
            :class="{ active: tradeFilters[opt.key] }"
            @click="toggleTradeFilter(opt.key)"
          >{{ opt.label }}</span>
          <span class="filter-clear" v-if="hasTradeFilter" @click="clearTradeFilters">清空</span>
        </div>

        <!-- 面交地点筛选标签 -->
        <div class="location-filter" v-if="activeLocation">
          <span class="lf-label">面交地点</span>
          <span class="lf-chip">
            <el-icon size="12"><Location /></el-icon>{{ activeLocation }}
            <span class="lf-clear" @click="clearPickupLocation">×</span>
          </span>
        </div>
      </div>

      <HomeFeed
        :goods="goodsList"
        :loading="loading"
        :searched="searched"
        :active-category="activeCategory"
        :has-trade-filter="hasTradeFilter"
        :recommend-list="recommendList"
        :user-results="userResults"
        :user-search-total="userSearchTotal"
        :user-search-loading="userSearchLoading"
        @open-goods="goDetail"
        @open-user="goUserShop"
        @chat-user="chatWithUser"
      />

      <!-- 滚动到底自动加载（无需手动点击） -->
      <div class="scroll-sentinel" ref="sentinelRef">
        <div class="load-status" v-if="loading && goodsList.length > 0">
          <span class="loading-spinner"></span>
          <span class="load-text">加载中...</span>
        </div>
        <div class="no-more" v-else-if="!hasMore && goodsList.length > 0">
          <span class="no-more-line"></span>
          <span class="no-more-text">没有更多了</span>
          <span class="no-more-line"></span>
        </div>
      </div>
    </section>

    <!-- 底部留白（tabbar 高度） -->
    <div class="bottom-placeholder"></div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Location } from '@element-plus/icons-vue'
import { getGoodsList, getHomeInit } from '../api/goods'
import { searchUsers } from '../api/user'
import { baseURL } from '../config'
import AppSidebar from '../components/AppSidebar.vue'
import HomeTop from '../components/home/HomeTop.vue'
import HomeModules from '../components/home/HomeModules.vue'
import HomeFeed from '../components/home/HomeFeed.vue'

const router = useRouter()

// ==================== 数据状态 ====================
const goodsList = ref([])
const categoryList = ref([])
const featuredGoods = ref([])   // 轮播/大类卡片复用的已有商品
const quickCovers = ref({})     // 右侧四大类封面（取对应分类下已有商品图）
const activeCategory = ref(null)
const keyword = ref('')
const pageNum = ref(1)
const pageSize = 24
const total = ref(0)
const loading = ref(false)
const exhausted = ref(false)    // 已确认没有更多数据（含请求失败），避免无限重试
const searched = ref(false)

const recommendList = ref([])
let recommendSeq = 0

const sentinelRef = ref(null)
const recommendRef = ref(null)
let observer = null
let requestSeq = 0

// 搜索结果里的用户
const userResults = ref([])
const userSearchTotal = ref(0)
const userSearchLoading = ref(false)
let userSearchSeq = 0

// 价格区间选项
const priceOptions = [
  { label: '全部', value: null },
  { label: '0-100', value: '0-100' },
  { label: '100-500', value: '100-500' },
  { label: '500-1000', value: '500-1000' },
  { label: '1000-5000', value: '1000-5000' },
  { label: '5000+', value: '5000' }
]
const activePrice = ref(null)
// 面交地点筛选（空字符串表示不筛选）
const activeLocation = ref('')
// 交易方式筛选，可单独或组合使用
const tradeFilterOptions = [
  { label: '可邮寄', key: 'canMail' },
  { label: '送宿舍', key: 'canDeliver' },
  { label: '可换物', key: 'exchangeSupported' }
]
const tradeFilters = ref({ canMail: false, canDeliver: false, exchangeSupported: false })
const hasTradeFilter = computed(() => Object.values(tradeFilters.value).some(Boolean))

// 排序选项
const sortOptions = [
  { label: '最新', value: 'new' },
  { label: '价格↑', value: 'price_asc' },
  { label: '价格↓', value: 'price_desc' },
  { label: '浏览量', value: 'views' }
]
const activeSort = ref('new')

// 本地默认占位图（不依赖外网，断网也不会裂图）
const DEFAULT_IMG = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='300' height='300'><rect width='300' height='300' fill='%23f0f0f0'/><text x='50%25' y='50%25' font-size='18' fill='%23bbb' text-anchor='middle' dy='.3em'>暂无图片</text></svg>"

// ==================== 计算属性 ====================
const hasMore = computed(() => !exhausted.value && goodsList.value.length < total.value)

// ==================== 基础工具 ====================
// 列表卡片使用 320px 缩略图，首屏图片体积小很多；原图失败时由 handleImgError 兜底
const getThumbUrl = (path) => {
  if (!path) return DEFAULT_IMG
  const firstImg = path.split(',')[0].trim()
  if (firstImg.startsWith('http')) return firstImg
  const name = firstImg.split('/').pop()
  return baseURL + '/thumb/' + name
}

// 滚动到“为你推荐”区域，保证筛选后能直接看到商品
const scrollToRecommend = () => {
  nextTick(() => {
    const el = recommendRef.value
    if (!el) return
    const top = el.getBoundingClientRect().top + window.scrollY - 64
    window.scrollTo({ top: Math.max(top, 0), behavior: 'smooth' })
  })
}

// ==================== 数据加载 ====================
// 让加载状态至少展示一段时间，避免一闪而过
const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms))
const ensureMinLoading = async (startedAt, minMs) => {
  const remain = minMs - (Date.now() - startedAt)
  if (remain > 0) await sleep(remain)
}

// 首页初始化：分类 + 轮播 + 四大类封面 + 首屏商品，1 个请求完成
const loadHomeInit = async () => {
  const seq = ++requestSeq
  const startedAt = Date.now()
  loading.value = true
  try {
    const res = await getHomeInit(24)
    if (seq !== requestSeq) return

    categoryList.value = (res && res.categories) ? res.categories : []
    featuredGoods.value = (res && res.banners) ? res.banners : []

    quickCovers.value = {}
    const covers = (res && res.covers) ? res.covers : {}
    Object.keys(covers).forEach(id => {
      quickCovers.value[id] = getThumbUrl(covers[id])
    })

    const page = res && res.goods ? res.goods : null
    const list = page && page.list ? page.list : []
    goodsList.value = list
    total.value = page && typeof page.total === 'number' ? page.total : list.length
    pageNum.value = 1
    exhausted.value = list.length >= total.value
  } catch (err) {
    console.error('首页初始化失败', err)
    goodsList.value = []
    total.value = 0
    exhausted.value = true
  } finally {
    if (seq === requestSeq) {
      await ensureMinLoading(startedAt, 200)
      if (seq === requestSeq) {
        loading.value = false
        nextTick(fillViewport)
      }
    }
  }
}

// 加载商品列表（reset=true 重新加载第一页，false 追加下一页）
const loadGoods = async (reset = true) => {
  // 追加请求在加载中直接忽略；重新筛选则允许打断进行中的请求
  if (!reset && loading.value) return
  const seq = ++requestSeq
  const startedAt = Date.now()
  loading.value = true

  if (reset) {
    pageNum.value = 1
    goodsList.value = []
    total.value = 0
    exhausted.value = false
    recommendList.value = []   // 重新筛选时先清空推荐，等结果出来再决定要不要拉
  }

  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize,
      sort: activeSort.value
    }
    if (activeCategory.value !== null) {
      params.categoryId = activeCategory.value
    }
    if (keyword.value.trim()) {
      params.keyword = keyword.value.trim()
    }
    if (activeLocation.value) {
      params.location = activeLocation.value
    }
    if (tradeFilters.value.canMail) params.canMail = 1
    if (tradeFilters.value.canDeliver) params.canDeliver = 1
    if (tradeFilters.value.exchangeSupported) params.exchangeSupported = 1
    if (activePrice.value) {
      if (activePrice.value === '5000') {
        params.minPrice = 5000
      } else {
        const [min, max] = activePrice.value.split('-')
        params.minPrice = min
        params.maxPrice = max
      }
    }

    const res = await getGoodsList(params)
    // 已有更新的请求（例如用户又切换了分类），丢弃过期结果
    if (seq !== requestSeq) return
    const list = (res && res.list) ? res.list : []
    const totalCount = (res && typeof res.total === 'number') ? res.total : list.length

    if (reset) {
      goodsList.value = list
      // 空结果时拉取热门推荐（同分类优先；全空则拉全站热门）
      if (totalCount === 0) {
        fetchRecommend()
      }
    } else {
      // 追加时按 id 去重，避免分页边界重复数据造成重复渲染
      const exist = new Set(goodsList.value.map(g => g.id))
      goodsList.value = goodsList.value.concat(list.filter(g => !exist.has(g.id)))
    }
    total.value = totalCount
    if (list.length === 0 || goodsList.value.length >= totalCount) {
      exhausted.value = true
    }
  } catch (err) {
    if (seq !== requestSeq) return
    console.error('获取商品失败', err)
    exhausted.value = true
  } finally {
    if (seq === requestSeq) {
      await ensureMinLoading(startedAt, 200)
      if (seq === requestSeq) {
        loading.value = false
        // 首屏或一页数据不足时，继续自动补足，直到铺满可视区域
        nextTick(fillViewport)
      }
    }
  }
}

// 空结果时推荐热门商品：优先同分类 sort=views，没有分类时拉全站热门 4 件
const fetchRecommend = async () => {
  const seq = ++recommendSeq
  try {
    const params = { sort: 'views', pageNum: 1, pageSize: 4 }
    if (activeCategory.value !== null) {
      params.categoryId = activeCategory.value
    }
    const res = await getGoodsList(params)
    if (seq !== recommendSeq) return
    recommendList.value = (res && res.list) ? res.list : []
    // 同分类也没热门（理论上不会，但保险），回退到全站
    if (recommendList.value.length === 0 && activeCategory.value !== null) {
      const fallback = await getGoodsList({ sort: 'views', pageNum: 1, pageSize: 4 })
      if (seq === recommendSeq) recommendList.value = (fallback && fallback.list) ? fallback.list : []
    }
  } catch (e) {
    if (seq === recommendSeq) recommendList.value = []
  }
}

const loadMore = () => {
  if (loading.value || !hasMore.value) return
  pageNum.value += 1
  loadGoods(false)
}

// 哨兵元素在可视区域附近时自动加载下一页
const fillViewport = () => {
  const el = sentinelRef.value
  if (!el || loading.value || !hasMore.value) return
  if (el.getBoundingClientRect().top < window.innerHeight + 200) {
    loadMore()
  }
}

// ==================== 筛选交互 ====================
const selectCategory = (catId) => {
  activeCategory.value = catId
  keyword.value = ''
  searched.value = false
  userResults.value = []
  userSearchTotal.value = 0
  loadGoods(true)
  scrollToRecommend()
}

const selectSubCategory = (cat, sub) => {
  activeCategory.value = cat.id
  keyword.value = sub
  searched.value = true
  userResults.value = []
  userSearchTotal.value = 0
  loadGoods(true)
  scrollToRecommend()
}

const selectPrice = (value) => {
  activePrice.value = value
  loadGoods(true)
}

const toggleTradeFilter = (key) => {
  tradeFilters.value[key] = !tradeFilters.value[key]
  loadGoods(true)
}

const clearTradeFilters = () => {
  tradeFilters.value = { canMail: false, canDeliver: false, exchangeSupported: false }
  loadGoods(true)
}

const selectSort = (value) => {
  activeSort.value = value
  loadGoods(true)
}

const clearPickupLocation = () => {
  activeLocation.value = ''
  loadGoods(true)
}

// 搜索结果里的用户列表
const fetchUsers = async (kw) => {
  const seq = ++userSearchSeq
  if (!localStorage.getItem('token')) {
    userResults.value = []
    userSearchTotal.value = 0
    userSearchLoading.value = false
    return
  }
  userSearchLoading.value = true
  try {
    const res = await searchUsers(kw, { pageNum: 1, pageSize: 6 })
    if (seq !== userSearchSeq) return
    userResults.value = (res && res.list) ? res.list : (Array.isArray(res) ? res : [])
    userSearchTotal.value = (res && typeof res.total === 'number') ? res.total : userResults.value.length
  } catch (e) {
    if (seq === userSearchSeq) {
      userResults.value = []
      userSearchTotal.value = 0
    }
  } finally {
    if (seq === userSearchSeq) userSearchLoading.value = false
  }
}

// 头部搜索：同步关键词，拉取商品与用户结果
const onHeaderSearch = (kw) => {
  keyword.value = kw
  searched.value = !!kw
  if (kw) {
    fetchUsers(kw)
  } else {
    userResults.value = []
    userSearchTotal.value = 0
    userSearchLoading.value = false
  }
  loadGoods(true)
  scrollToRecommend()
}

// 宿舍区点击：切换到 location 筛选
const selectDormArea = (area) => {
  activeLocation.value = area
  loadGoods(true)
  scrollToRecommend()
}

// ==================== 跳转 ====================
const goDetail = (id) => {
  router.push(`/goods/${id}`)
}

const goUserShop = (u) => {
  if (!u || !u.id) return
  router.push(`/profile?userId=${u.id}`)
}

const chatWithUser = (u) => {
  if (!u || !u.id) return
  const userStr = localStorage.getItem('user')
  const me = userStr ? JSON.parse(userStr) : null
  if (me && String(me.id) === String(u.id)) return
  router.push({
    path: '/message',
    query: {
      sellerId: u.id,
      sellerName: u.username || '匿名用户',
      sellerAvatar: u.avatar || ''
    }
  })
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadHomeInit()

  // 滚动到底部哨兵时自动加载下一页
  if (typeof IntersectionObserver !== 'undefined' && sentinelRef.value) {
    observer = new IntersectionObserver((entries) => {
      if (entries.some(entry => entry.isIntersecting)) {
        loadMore()
      }
    }, { rootMargin: '300px 0px' })
    observer.observe(sentinelRef.value)
  }
})

onUnmounted(() => {
  if (observer) {
    observer.disconnect()
    observer = null
  }
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background-color: #fff;
  background-image: linear-gradient(
    180deg,
    #FF0F23 0px,
    #E1251B 56px,
    #FF6E6E 138px,
    #FFC9C9 206px,
    #FFFFFF 286px
  );
  background-repeat: no-repeat;
  padding-top: 56px;
}

/* ========== 为你推荐 ========== */
.recommend {
  width: calc(100% - 40px);
  max-width: 1160px;
  margin: 12px auto 0;
  padding-bottom: 10px;
}

/* 筛选区吸顶，滚动浏览商品时仍可切换排序/价格 */
.recommend-toolbar {
  position: sticky;
  top: 56px;
  z-index: 30;
  margin-top: -8px;
  padding-top: 8px;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid #EFEFEF;
  backdrop-filter: blur(8px);
}

.rec-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  padding: 0 2px;
}

.rec-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 700;
  color: #333;
}

.rec-title-icon {
  width: 4px;
  height: 18px;
  background: #E1251B;
  border-radius: 2px;
}

.rec-count {
  font-size: 12px;
  font-weight: 400;
  color: #999;
}

.sort-tabs {
  display: flex;
  gap: 6px;
}

.sort-tab {
  padding: 5px 14px;
  background: #fff;
  border: 1px solid #E7E7E7;
  border-radius: 14px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s ease;
}

.sort-tab:hover {
  color: #E1251B;
  background: #FFF5F4;
}

.sort-tab.active {
  background: #E1251B;
  border-color: #E1251B;
  color: #fff;
  font-weight: 600;
}

.price-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px 14px;
  background: #fff;
  border: 1px solid #DDDDDD;
  border-radius: 8px;
}

.price-label {
  flex-shrink: 0;
  font-size: 12px;
  color: #999;
}

.price-chip {
  padding: 3px 12px;
  background: #fff;
  border: 1px solid #E7E7E7;
  border-radius: 12px;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  transition: all 0.15s ease;
}

.price-chip:hover {
  color: #E1251B;
  background: #FFF5F4;
}

.price-chip.active {
  background: #E1251B;
  border-color: #E1251B;
  color: #fff;
}

.filter-divider {
  width: 1px;
  height: 18px;
  margin: 0 4px;
  background: #E5E7EB;
}

.trade-filter-chip.active {
  background: #7C3AED;
  border-color: #7C3AED;
}

.filter-clear {
  padding: 3px 6px;
  color: #999;
  font-size: 12px;
  cursor: pointer;
}

.filter-clear:hover {
  color: #E1251B;
}

/* 面交地点筛选标签 */
.location-filter {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.lf-label {
  font-size: 12px;
  color: #999;
}

.lf-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: #FEF0EF;
  border: 1px solid #F5C2BE;
  color: #E1251B;
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 12px;
}

.lf-clear {
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  margin-left: 2px;
  color: #E1251B;
}

.lf-clear:hover {
  color: #C81E18;
}

/* ========== 自动加载状态 ========== */
.scroll-sentinel {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 56px;
  margin-top: 8px;
}

.load-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
  color: #999;
}

.initial-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 14px 0 10px;
  font-size: 13px;
  color: #999;
}

.loading-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid #eee;
  border-top-color: #E1251B;
  border-radius: 50%;
  animation: loading-spin 0.7s linear infinite;
}

@keyframes loading-spin {
  to { transform: rotate(360deg); }
}

.load-text {
  margin-left: 0;
}

.no-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  width: 100%;
}

.no-more-line {
  flex: 1;
  max-width: 60px;
  height: 1px;
  background: linear-gradient(to right, transparent, #ddd, transparent);
}

.no-more-text {
  font-size: 12px;
  color: #bbb;
}

/* ========== 底部留白 ========== */
.bottom-placeholder {
  height: 60px;
}

@media (max-width: 768px) {
  .rec-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}

</style>
