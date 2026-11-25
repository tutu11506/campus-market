<template>
  <div class="flash-page">
    <AppSidebar />

    <!-- 顶部条 -->
    <div class="fp-header">
      <div class="fp-header-inner">
        <div class="fp-back" @click="goHome">
          <el-icon :size="18"><ArrowLeft /></el-icon>
        </div>
        <div class="fp-brand">
          <span class="fp-logo">闪</span>
          <span class="fp-name">校园闪送</span>
        </div>
        <div class="fp-slogan" v-if="overview.fastestEta">
          校内同学自送 · 最快 <b>{{ overview.fastestEta }}</b> 分钟到
        </div>
        <div class="fp-header-right">
          <div class="fp-msg" @click="go('/message')">
            <el-icon :size="20"><Bell /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <div class="fp-body">
      <!-- 地址 + 履约数据 -->
      <section class="fp-hero">
        <div class="fp-address-row">
          <span class="fp-address-label">送至</span>
          <span class="fp-address-value">{{ selectedLocation || '校园各闪送点' }}</span>
          <span class="fp-address-eta" v-if="currentEta">约 {{ currentEta }} 分钟送达</span>
        </div>

        <div class="fp-stats">
          <div class="fp-stat">
            <span class="fp-stat-num">{{ overview.totalOnSale || 0 }}</span>
            <span class="fp-stat-label">件可闪送</span>
          </div>
          <div class="fp-stat">
            <span class="fp-stat-num">{{ overview.pointsCount || 0 }}</span>
            <span class="fp-stat-label">个闪送点</span>
          </div>
          <div class="fp-stat">
            <span class="fp-stat-num">{{ overview.avgEta || 0 }}<i>分</i></span>
            <span class="fp-stat-label">平均送达</span>
          </div>
          <div class="fp-stat">
            <span class="fp-stat-num">{{ overview.deliveredToday || 0 }}</span>
            <span class="fp-stat-label">今日已送达</span>
          </div>
        </div>

        <div class="fp-promise">
          <span>⚡ 极速达</span>
          <span>🤝 同学面交</span>
          <span>👀 当面验货</span>
          <span>🔒 平台担保</span>
        </div>
      </section>

      <!-- 闪送点选择 -->
      <section class="fp-section" v-if="points.length">
        <div class="fp-section-head">
          <span class="fp-section-title">选择闪送点</span>
          <span class="fp-section-sub">离你越近，送得越快</span>
        </div>
        <div class="fp-point-list">
          <div
            class="fp-point"
            v-for="p in points"
            :key="p.location"
            :class="{ active: selectedLocation === p.location }"
            @click="pickPoint(p)"
          >
            <span class="fp-point-tag" v-if="p.tag">{{ p.tag }}</span>
            <div class="fp-point-name">{{ p.location }}</div>
            <div class="fp-point-eta">{{ p.etaMinutes }} 分钟</div>
            <div class="fp-point-meta">{{ p.count }} 件 · ¥{{ p.minPrice }} 起</div>
          </div>
        </div>
      </section>

      <!-- 分类 -->
      <section class="fp-section" v-if="categories.length">
        <div class="fp-chips">
          <span class="fp-chip" :class="{ active: selectedCategory === null }" @click="pickCategory(null)">
            全部
          </span>
          <span
            class="fp-chip"
            v-for="c in categories"
            :key="'fc-' + c.categoryId"
            :class="{ active: selectedCategory === c.categoryId }"
            @click="pickCategory(c.categoryId)"
          >
            {{ c.categoryName }}<i>{{ c.count }}</i>
          </span>
        </div>
      </section>

      <!-- 此刻最抢手 -->
      <section class="fp-section" v-if="hotNow.length && selectedCategory === null">
        <div class="fp-section-head">
          <span class="fp-section-title">🔥 此刻最抢手</span>
          <span class="fp-section-sub">近 3 天被看最多，手慢无</span>
        </div>
        <div class="fp-hot-row">
          <div class="fp-hot-card" v-for="h in hotNow" :key="'fh-' + h.id" @click="goGoods(h.id)">
            <SmartImage :src="h.coverImage" class="fp-hot-img" alt="" />
            <div class="fp-hot-info">
              <div class="fp-hot-title">{{ h.title }}</div>
              <div class="fp-hot-meta">
                <span class="fp-hot-price">¥{{ h.price }}</span>
                <span class="fp-hot-count">{{ h.hotCount }} 人看过</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 商品列表 -->
      <section class="fp-section">
        <div class="fp-section-head">
          <span class="fp-section-title">
            {{ selectedLocation || '全部闪送点' }}
            <template v-if="selectedCategoryName">· {{ selectedCategoryName }}</template>
          </span>
          <span class="fp-section-sub">{{ goods.length }} 件可送</span>
        </div>

        <div class="fp-toolbar">
          <span class="fp-sort-chip" :class="{ active: sortMode === 'default' }" @click="setSort('default')">综合</span>
          <span class="fp-sort-chip" :class="{ active: sortMode === 'price' }" @click="setSort('price')">最便宜</span>
          <span class="fp-sort-chip" :class="{ active: sortMode === 'fast' }" @click="setSort('fast')">送得最快</span>
          <span class="fp-sort-chip" :class="{ active: sortMode === 'hot' }" @click="setSort('hot')">看的人最多</span>
        </div>

        <div class="fp-goods-grid" v-if="sortedGoods.length">
          <div class="fg-card" v-for="g in sortedGoods" :key="'fg-' + g.id" @click="goGoods(g.id)">
            <div class="fg-img-wrap">
              <SmartImage :src="g.coverImage" class="fg-img" alt="" />
              <span class="fg-eta">{{ g.etaMinutes }} 分钟达</span>
            </div>
            <div class="fg-info">
              <div class="fg-title">{{ g.title }}</div>
              <div class="fg-meta">
                <span class="fg-price">¥{{ g.price }}</span>
                <span class="fg-cat">{{ g.categoryName }}</span>
              </div>
              <div class="fg-loc">
                📍 {{ g.location }}
                <span class="fg-views" v-if="g.viewCount">· 👁 {{ g.viewCount }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="fp-empty" v-else-if="!loading">
          <el-icon :size="48" color="#DCDFE6"><Van /></el-icon>
          <p>这个闪送点暂时没有可送的商品</p>
          <span>换个配送点或分类试试</span>
        </div>
        <div class="fp-loading" v-if="loading">加载中...</div>
      </section>

      <!-- 闪送流程 -->
      <section class="fp-flow">
        <div class="fp-flow-title">校园闪送怎么用？</div>
        <div class="fp-flow-steps">
          <div class="fp-step">
            <span class="fp-step-num">1</span>
            <span class="fp-step-text">选好商品下单，和同学约定送达</span>
          </div>
          <div class="fp-step">
            <span class="fp-step-num">2</span>
            <span class="fp-step-text">同学从最近闪送点取货，校内送到你手上</span>
          </div>
          <div class="fp-step">
            <span class="fp-step-num">3</span>
            <span class="fp-step-text">当面验货，确认无误后确认收货</span>
          </div>
        </div>
        <div class="fp-flow-note">
          闪送仅限校园内，跨校区或大件请走快递；急用的充电器、教材、球拍这类小件，十几分钟就能到手。
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Bell, Van } from '@element-plus/icons-vue'
import AppSidebar from '../components/AppSidebar.vue'
import SmartImage from '../components/SmartImage.vue'
import { getFlashOverview, getFlashGoods } from '../api/flash'

const router = useRouter()

const overview = ref({})
const points = ref([])
const categories = ref([])
const hotNow = ref([])
const goods = ref([])
const loading = ref(false)

// 商品排序：综合 / 最便宜 / 送得最快 / 看的人最多
const sortMode = ref('default')

const sortedGoods = computed(() => {
  const list = [...goods.value]
  if (sortMode.value === 'price') {
    list.sort((a, b) => (Number(a.price) || 0) - (Number(b.price) || 0))
  } else if (sortMode.value === 'fast') {
    list.sort((a, b) => (a.etaMinutes || 99) - (b.etaMinutes || 99))
  } else if (sortMode.value === 'hot') {
    list.sort((a, b) => (b.viewCount || 0) - (a.viewCount || 0))
  }
  return list
})

const setSort = (mode) => { sortMode.value = mode }

const selectedLocation = ref('')
const selectedCategory = ref(null)

const currentEta = computed(() => {
  const p = points.value.find(x => x.location === selectedLocation.value)
  return p ? p.etaMinutes : (overview.value.avgEta || 0)
})

const selectedCategoryName = computed(() => {
  const c = categories.value.find(x => x.categoryId === selectedCategory.value)
  return c ? c.categoryName : ''
})

const loadOverview = async () => {
  try {
    const data = await getFlashOverview()
    overview.value = data || {}
    points.value = (data && data.points) || []
    categories.value = (data && data.categories) || []
    hotNow.value = (data && data.hotNow) || []
    if (!selectedLocation.value && points.value.length) {
      selectedLocation.value = points.value[0].location
    }
    await loadGoods()
  } catch (e) {
    overview.value = {}
  }
}

const loadGoods = async () => {
  loading.value = true
  try {
    const params = { limit: 48 }
    if (selectedLocation.value) params.location = selectedLocation.value
    if (selectedCategory.value !== null) params.categoryId = selectedCategory.value
    const data = await getFlashGoods(params)
    goods.value = (data && data.items) || []
  } catch (e) {
    goods.value = []
  } finally {
    loading.value = false
  }
}

const pickPoint = (p) => {
  selectedLocation.value = p.location
  loadGoods()
}

const pickCategory = (id) => {
  selectedCategory.value = id
  loadGoods()
}

const goHome = () => router.push('/')
const goGoods = (id) => router.push('/goods/' + id)
const go = (path) => router.push(path)

onMounted(loadOverview)
</script>

<style scoped>
.flash-page {
  min-height: 100vh;
  background: #F6F7F9;
  padding-top: 56px;
  padding-bottom: 80px;
}

/* 顶部条 */
.fp-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 56px;
  z-index: 1000;
  background: linear-gradient(135deg, #FF5A3C 0%, #E1251B 100%);
  box-shadow: 0 2px 10px rgba(225, 37, 27, 0.3);
}

/* 宽屏：为左侧功能导航让出一列，顶栏同步右移保持对齐 */
@media (min-width: 1100px) {
  .flash-page {
    padding-left: 360px;
  }
  .fp-header {
    left: 360px;
  }
}

/* 中等宽度：侧栏收起为 72px 图标导轨 */
@media (min-width: 768px) and (max-width: 1099px) {
  .flash-page {
    padding-left: 72px;
  }
  .fp-header {
    left: 72px;
  }
}

.fp-header-inner {
  max-width: 1160px;
  height: 100%;
  margin: 0 auto;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.fp-back {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 50%;
  cursor: pointer;
}

.fp-brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.fp-logo {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: #fff;
  color: #E1251B;
  font-size: 15px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}

.fp-name {
  color: #fff;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.fp-slogan {
  color: rgba(255, 255, 255, 0.92);
  font-size: 12px;
}

.fp-slogan b {
  font-size: 14px;
}

.fp-header-right {
  margin-left: auto;
}

.fp-msg {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 50%;
  cursor: pointer;
}

/* 内容 */
.fp-body {
  max-width: 1160px;
  margin: 0 auto;
  padding: 12px 16px 0;
}

.fp-hero {
  background: linear-gradient(120deg, #FFF3F0 0%, #FFFFFF 60%);
  border: 1px solid #FFE0D9;
  border-radius: 14px;
  padding: 16px 18px;
}

.fp-address-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.fp-address-label {
  font-size: 12px;
  color: #9CA3AF;
}

.fp-address-value {
  font-size: 18px;
  font-weight: 800;
  color: #1F2937;
}

.fp-address-eta {
  font-size: 12px;
  font-weight: 700;
  color: #E1251B;
  background: #FFEBE7;
  padding: 3px 10px;
  border-radius: 12px;
}

.fp-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-top: 14px;
}

.fp-stat {
  background: #fff;
  border: 1px solid #F0F1F3;
  border-radius: 10px;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.fp-stat-num {
  font-size: 22px;
  font-weight: 800;
  color: #E1251B;
  line-height: 1.2;
}

.fp-stat-num i {
  font-size: 11px;
  font-style: normal;
  font-weight: 600;
  margin-left: 1px;
}

.fp-stat-label {
  font-size: 11px;
  color: #9CA3AF;
}

.fp-promise {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 12px;
  font-size: 12px;
  color: #6B7280;
}

/* 通用区块 */
.fp-section {
  margin-top: 14px;
  background: #fff;
  border: 1px solid #EFF0F2;
  border-radius: 14px;
  padding: 14px 16px;
}

.fp-section-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
}

.fp-section-title {
  font-size: 15px;
  font-weight: 700;
  color: #1F2937;
}

.fp-section-sub {
  font-size: 11px;
  color: #9CA3AF;
}

/* 闪送点 */
.fp-point-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 10px;
}

.fp-point {
  position: relative;
  border: 1px solid #EDEEF0;
  border-radius: 10px;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.18s ease;
  background: #FAFBFC;
}

.fp-point:hover {
  border-color: #FFC2B8;
  background: #FFF8F6;
}

.fp-point.active {
  border-color: #E1251B;
  background: #FFF3F0;
  box-shadow: 0 4px 14px rgba(225, 37, 27, 0.12);
}

.fp-point-tag {
  position: absolute;
  top: -8px;
  right: 8px;
  font-size: 9px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #FF7A45, #E1251B);
  padding: 1px 7px;
  border-radius: 8px;
}

.fp-point-name {
  font-size: 13px;
  font-weight: 700;
  color: #1F2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fp-point-eta {
  margin-top: 4px;
  font-size: 15px;
  font-weight: 800;
  color: #E1251B;
}

.fp-point-meta {
  margin-top: 3px;
  font-size: 11px;
  color: #9CA3AF;
}

/* 分类 */
.fp-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.fp-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  font-size: 13px;
  color: #4B5563;
  background: #F5F6F8;
  border: 1px solid transparent;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.16s ease;
}

.fp-chip i {
  font-style: normal;
  font-size: 10px;
  color: #9CA3AF;
}

.fp-chip:hover {
  background: #FFF1F0;
  color: #E1251B;
}

.fp-chip.active {
  background: #E1251B;
  color: #fff;
  border-color: #E1251B;
}

.fp-chip.active i {
  color: rgba(255, 255, 255, 0.8);
}

/* 此刻最抢手 */
.fp-hot-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.fp-hot-card {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 8px;
  border: 1px solid #F0F1F3;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.fp-hot-card:hover {
  border-color: #FFC2B8;
  box-shadow: 0 4px 14px rgba(225, 37, 27, 0.1);
}

.fp-hot-img {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  object-fit: cover;
  background: #f5f5f5;
  flex-shrink: 0;
}

.fp-hot-info {
  min-width: 0;
  flex: 1;
}

.fp-hot-title {
  font-size: 12px;
  color: #1F2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fp-hot-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
}

.fp-hot-price {
  font-size: 14px;
  font-weight: 800;
  color: #E1251B;
}

.fp-hot-count {
  font-size: 10px;
  color: #9CA3AF;
}

/* 商品 */
.fp-goods-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.fg-card {
  border: 1px solid #F0F1F3;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #fff;
}

.fg-card:hover {
  border-color: #FFC2B8;
  box-shadow: 0 6px 18px rgba(225, 37, 27, 0.12);
  transform: translateY(-2px);
}

.fg-img-wrap {
  position: relative;
}

.fg-img {
  width: 100%;
  height: 132px;
  object-fit: cover;
  display: block;
  background: #f5f5f5;
}

.fg-eta {
  position: absolute;
  left: 8px;
  top: 8px;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #FF7A45, #E1251B);
  padding: 3px 8px;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(225, 37, 27, 0.28);
}

.fg-info {
  padding: 9px 11px 11px;
}

.fg-title {
  font-size: 13px;
  color: #1F2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fg-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 5px;
}

.fg-price {
  font-size: 16px;
  font-weight: 800;
  color: #E1251B;
}

.fg-cat {
  font-size: 10px;
  color: #9CA3AF;
  background: #F5F6F8;
  padding: 1px 6px;
  border-radius: 4px;
}

.fg-loc {
  margin-top: 5px;
  font-size: 11px;
  color: #10B981;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fg-views {
  color: #9CA3AF;
}

.fp-toolbar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.fp-sort-chip {
  padding: 5px 13px;
  font-size: 12px;
  color: #4B5563;
  background: #F5F6F8;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.16s ease;
}

.fp-sort-chip:hover {
  background: #FFF1F0;
  color: #E1251B;
}

.fp-sort-chip.active {
  background: #E1251B;
  color: #fff;
}

.fp-flow-note {
  margin-top: 12px;
  font-size: 12px;
  color: #6B7280;
  background: #F8F9FB;
  border-radius: 8px;
  padding: 10px 12px;
  line-height: 1.7;
}

.fp-empty {
  padding: 40px 0;
  text-align: center;
  color: #9CA3AF;
}

.fp-empty p {
  margin: 10px 0 4px;
  font-size: 14px;
  color: #4B5563;
}

.fp-empty span {
  font-size: 12px;
}

.fp-loading {
  text-align: center;
  padding: 26px 0;
  color: #9CA3AF;
  font-size: 13px;
}

/* 流程 */
.fp-flow {
  margin: 14px 0 0;
  background: #fff;
  border: 1px solid #EFF0F2;
  border-radius: 14px;
  padding: 16px 18px;
}

.fp-flow-title {
  font-size: 14px;
  font-weight: 700;
  color: #1F2937;
  margin-bottom: 12px;
}

.fp-flow-steps {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.fp-step {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #FAFBFC;
  border-radius: 10px;
  padding: 12px 14px;
}

.fp-step-num {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #E1251B;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.fp-step-text {
  font-size: 12px;
  color: #4B5563;
  line-height: 1.5;
}

@media (max-width: 1100px) {
  .fp-goods-grid { grid-template-columns: repeat(3, 1fr); }
  .fp-hot-row { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 760px) {
  .fp-stats { grid-template-columns: repeat(2, 1fr); }
  .fp-goods-grid { grid-template-columns: repeat(2, 1fr); }
  .fp-hot-row { grid-template-columns: 1fr; }
  .fp-flow-steps { grid-template-columns: 1fr; }
  .fp-slogan { display: none; }
}
</style>
