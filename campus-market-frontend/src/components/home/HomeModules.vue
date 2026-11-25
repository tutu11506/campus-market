<template>
    <!-- 教材循环引导横幅（新学期显眼入口） -->
    <section class="textbook-hero" @click="goTextbookImport">
      <div class="tb-hero-left">
        <div class="tb-hero-badge">新学期</div>
        <div class="tb-hero-title">📚 教材循环 · 导入课表一键匹配</div>
        <div class="tb-hero-desc">
          粘贴你的学期课表，自动列出这学期要用的书，平台二手 / 可租版本一目了然，还能看同学避雷留言。
        </div>
        <div class="tb-hero-actions">
          <button class="tb-import-btn" @click.stop="goTextbookImport">
            <el-icon><Calendar /></el-icon>一键导入课表
          </button>
          <button class="tb-ghost-btn" @click.stop="goTextbookBrowse">
            <el-icon><Reading /></el-icon>逛逛教材专区
          </button>
        </div>
      </div>
      <div class="tb-hero-right">
        <div class="tb-stat" v-if="tbStats.goodsTotal > 0">
          <div class="tb-stat-num">{{ tbStats.goodsTotal }}</div>
          <div class="tb-stat-label">本平台在售二手教材</div>
        </div>
        <div class="tb-stat" v-if="tbStats.rentalTotal > 0">
          <div class="tb-stat-num">{{ tbStats.rentalTotal }}</div>
          <div class="tb-stat-label">可租版本</div>
        </div>
        <div class="tb-stat tb-stat-sub" v-if="tbStats.textbookTotal > 0">
          <div class="tb-stat-num">{{ tbStats.textbookTotal }}</div>
          <div class="tb-stat-label">聚合教材主表</div>
        </div>
      </div>
    </section>

    <!-- 个性化推荐横条（有数据才展示；4-6 张横排卡片） -->
    <section class="personalized-bar" v-if="personalizedList.length > 0">
      <div class="pb-head">
        <div class="pb-head-left">
          <div class="pb-title-row">
            <el-icon class="pb-title-icon"><MagicStick /></el-icon>
            <span class="pb-title">{{ personalizedTitle }}</span>
            <span class="pb-source-tag" v-if="personalizedSource === 'complementary'">买过推配件</span>
            <span class="pb-source-tag tag-timely" v-else-if="personalizedSource === 'timely'">时序新款</span>
            <span class="pb-source-tag tag-browse" v-else-if="personalizedSource === 'browse'">猜你喜欢</span>
          </div>
          <div class="pb-subtitle">{{ personalizedSubtitle }}</div>
        </div>
        <span class="pb-divider"></span>
      </div>
      <div class="pb-scroll">
        <div
          class="pb-card"
          v-for="item in personalizedList"
          :key="'pb-' + item.id"
          @click="goDetail(item.id)"
        >
          <div class="pb-img-wrap">
            <SmartImage :src="item.coverImage || item.images" class="pb-img" alt="" />
          </div>
          <div class="pb-info">
            <div class="pb-card-title">{{ item.title }}</div>
            <div class="pb-price-row">
              <span class="pb-price-symbol">¥</span>
              <span class="pb-price">{{ item.price }}</span>
            </div>
            <div class="pb-meta">
              <span class="pb-category" v-if="item.categoryName">{{ item.categoryName }}</span>
              <span class="pb-views">
                <el-icon class="pb-mini-icon"><View /></el-icon>{{ item.viewCount || 0 }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 复购提醒横条（有消耗品历史 + 到期提醒时才展示） -->
    <section class="repurchase-bar" v-if="repurchaseItems.length > 0">
      <div class="rp-icon-wrap">
        <el-icon class="rp-icon"><Lightning /></el-icon>
      </div>
      <div class="rp-content">
        <div class="rp-title-row">
          <span class="rp-title">🔁 该补了</span>
          <span class="rp-sub">{{ repurchaseItems[0].reason }}</span>
        </div>
        <div class="rp-items">
          <span
            class="rp-item"
            v-for="it in repurchaseItems"
            :key="'rp-' + it.goodsId"
            @click="goDetail(it.goodsId)"
          >
            {{ it.title }}
          </span>
        </div>
      </div>
      <div class="rp-action">
        <router-link to="/" class="rp-action-btn" @click.prevent="() => {}">
          <el-icon><ArrowRight /></el-icon>
        </router-link>
      </div>
    </section>

    <!-- 宿舍楼下好货：邻居间的闲置，下楼就能看 -->
    <section class="dorm-goods" v-if="dormGoods.length > 0">
      <div class="dg-head">
        <div class="dg-title-row">
          <span class="dg-title">🏠 宿舍楼下好货</span>
          <span class="dg-tag">邻居闲置 · 下楼就能看</span>
        </div>
        <span class="dg-sub">你附近的宿舍区在出什么？点一个就知道</span>
      </div>
      <div class="dg-grid">
        <div
          class="dg-card"
          v-for="d in dormGoods"
          :key="'dg-' + d.area"
          @click="openDormDetail(d.area)"
        >
          <div class="dg-area">{{ d.area }}</div>
          <div class="dg-price-row">
            <span class="dg-count">{{ d.count }} 件闲置</span>
            <span class="dg-avg">均价 ¥{{ d.avgPrice }}</span>
          </div>
          <div class="dg-summary">{{ d.summary }}</div>
        </div>
      </div>
    </section>

    <!-- 毕业清仓专区：应届毕业生急出榜，离校倒计时 -->
    <section class="grad-sale" v-if="graduationSale.length > 0">
      <div class="gs-head">
        <div class="gs-title-row">
          <span class="gs-title">🎓 毕业清仓专区</span>
          <span class="gs-tag" :class="gradSeason ? 'gs-live' : 'gs-off'">
            {{ gradSeason ? '毕业季进行中' : '毕业季外 · 往届急出' }}
          </span>
        </div>
        <span class="gs-sub">学长学姐快走了，砍价成功率高，也最诚实</span>
      </div>
      <div class="gs-grid">
        <div
          class="gs-card"
          v-for="g in graduationSale"
          :key="'gs-' + g.id"
          @click="goDetail(g.id)"
        >
          <SmartImage :src="g.coverImage" class="gs-img" alt="" />
          <div class="gs-badge" v-if="g.daysToGrad != null">
            {{ g.daysToGrad <= 0 ? '今天离校' : '剩 ' + g.daysToGrad + ' 天' }}
          </div>
          <div class="gs-info">
            <div class="gs-title-text">{{ g.title }}</div>
            <div class="gs-meta">
              <span class="gs-price">¥{{ g.price }}</span>
              <span class="gs-seller">{{ g.sellerName }}</span>
            </div>
            <div class="gs-label" :class="'gl-' + g.gradLabel">{{ g.gradLabel }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 宿舍区详情弹窗 -->
    <el-dialog v-model="dormDetailVisible" :title="'🏠 ' + dormDetailArea + ' 附近的闲置'" width="520px" append-to-body class="location-dialog">
      <div v-if="dormLoading" class="ld-loading">加载中...</div>
      <div v-else-if="dormDetailGoods.length > 0" class="dd-content">
        <p class="dd-summary">{{ dormDetailArea }} 附近有 {{ dormDetailGoods.length }} 件邻居闲置，点商品直接看详情</p>
        <div class="dd-grid">
          <div
            class="dd-card"
            v-for="g in dormDetailGoods"
            :key="'dd-' + g.id"
            @click="goDormGoods(g.id)"
          >
            <SmartImage :src="g.coverImage" class="dd-img" alt="" />
            <div class="dd-info">
              <div class="dd-title">{{ g.title }}</div>
              <div class="dd-meta">
                <span class="dd-price">¥{{ g.price }}</span>
                <span class="dd-cat">{{ g.categoryName }}</span>
              </div>
              <div class="dd-loc" v-if="g.location">📍 {{ g.location }}</div>
            </div>
          </div>
        </div>
        <button class="ld-filter-btn" @click="selectDormArea(dormDetailArea); dormDetailVisible = false">筛选这个宿舍区的全部商品</button>
      </div>
      <div v-else class="ld-empty">这个宿舍区暂时没有在售闲置</div>
    </el-dialog>

</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import SmartImage from '../SmartImage.vue'
import { getGoodsList, getDormGoods, getDormDetail, getGraduationSale } from '../../api/goods'
import { getPersonalizedRecommend, getRepurchaseReminders } from '../../api/recommend'

const emit = defineEmits(['open-goods', 'filter-location'])
const router = useRouter()

const goDetail = (id) => emit('open-goods', id)

// ==================== 教材循环引导横幅 ====================
const tbStats = ref({ goodsTotal: 0, rentalTotal: 0, textbookTotal: 0 })
const loadTbStats = async () => {
  try {
    // categoryId=1 是教材书籍分类
    const [goodsRes, rentalRes, tbRes] = await Promise.allSettled([
      getGoodsList({ categoryId: 1, pageNum: 1, pageSize: 1 }),
      fetchRentalTotal(),
      fetchTextbookMainTotal()
    ])
    const goodsTotal = (goodsRes.status === 'fulfilled' && goodsRes.value) ? (goodsRes.value.total || 0) : 0
    const rentalTotal = (rentalRes.status === 'fulfilled') ? (rentalRes.value || 0) : 0
    const tbTotal = (tbRes.status === 'fulfilled') ? (tbRes.value || 0) : 0
    tbStats.value = { goodsTotal, rentalTotal, textbookTotal: tbTotal }
  } catch (e) {
    tbStats.value = { goodsTotal: 0, rentalTotal: 0, textbookTotal: 0 }
  }
}
// 可租教材数（rentalType=textbook + status=available）
const fetchRentalTotal = async () => {
  try {
    const { default: request } = await import('../../api/request')
    const res = await request.get('/rental/items', { params: { type: 'textbook', pageNum: 1, pageSize: 1 } })
    return (res && res.total) || 0
  } catch (e) { return 0 }
}
// 教材主表总数（公开 GET /textbooks 分页 total）
const fetchTextbookMainTotal = async () => {
  try {
    const { default: request } = await import('../../api/request')
    const res = await request.get('/textbooks', { params: { pageNum: 1, pageSize: 1 } })
    return (res && res.total) || 0
  } catch (e) { return 0 }
}
const goTextbookImport = () => {
  router.push({ path: '/textbook', query: { action: 'import' } })
}
const goTextbookBrowse = () => {
  router.push('/textbook')
}

// ==================== 个性化推荐 + 复购提醒 ====================
const personalizedList = ref([])
const personalizedSource = ref('')
const repurchaseItems = ref([])

const fetchPersonalized = async () => {
  try {
    const res = await getPersonalizedRecommend()
    personalizedList.value = (res && res.list) ? res.list.slice(0, 6) : []
    personalizedSource.value = (res && res.source) || 'hot'
  } catch (e) {
    personalizedList.value = []
    personalizedSource.value = ''
  }
}
const fetchRepurchaseReminders = async () => {
  try {
    const res = await getRepurchaseReminders()
    if (res && res.hasReminders && res.items) {
      repurchaseItems.value = res.items.slice(0, 3)
    } else {
      repurchaseItems.value = []
    }
  } catch (e) {
    repurchaseItems.value = []
  }
}

// 根据推荐来源返回面向用户的标题
const personalizedTitle = computed(() => {
  switch (personalizedSource.value) {
    case 'collaborative':  return '👥 买过它的人也买了'
    case 'complementary':  return '🎒 买过这些？再看看配件'
    case 'timely':         return '✨ 为你找新款'
    case 'browse':         return '🔍 你可能感兴趣'
    default:               return '📢 为你精选'
  }
})
const personalizedSubtitle = computed(() => {
  switch (personalizedSource.value) {
    case 'collaborative':  return '基于买过同一件商品的同学偏好，为你选了这些'
    case 'complementary':  return '根据你买过的商品，推荐同分类好货'
    case 'timely':         return '看看你关注过的品类最近有什么新上架'
    case 'browse':         return '结合你的浏览偏好，猜你喜欢这些'
    default:               return '校园热门好货，大家都在看'
  }
})

// ==================== 宿舍楼下好货 ====================
const dormGoods = ref([])
const dormDetailVisible = ref(false)
const dormDetailGoods = ref([])
const dormDetailArea = ref('')
const dormLoading = ref(false)

const loadDormGoods = async () => {
  try {
    const res = await getDormGoods()
    dormGoods.value = (res && Array.isArray(res.items)) ? res.items : []
  } catch (e) {
    dormGoods.value = []
  }
}
const openDormDetail = async (area) => {
  dormDetailArea.value = area
  dormDetailGoods.value = []
  dormLoading.value = true
  dormDetailVisible.value = true
  try {
    const res = await getDormDetail(area)
    dormDetailGoods.value = (res && Array.isArray(res.goods)) ? res.goods : []
  } catch (e) {
    dormDetailGoods.value = []
  } finally {
    dormLoading.value = false
  }
}
const goDormGoods = (id) => {
  dormDetailVisible.value = false
  goDetail(id)
}
const selectDormArea = (area) => {
  dormDetailVisible.value = false
  emit('filter-location', area)
}

// ==================== 毕业清仓专区 ====================
const graduationSale = ref([])
const gradSeason = ref(false)
const loadGraduationSale = async () => {
  try {
    const res = await getGraduationSale()
    graduationSale.value = (res && Array.isArray(res.items)) ? res.items : []
    gradSeason.value = !!(res && res.isGradSeason)
  } catch (e) {
    graduationSale.value = []
  }
}

onMounted(() => {
  loadTbStats()
  fetchPersonalized()
  fetchRepurchaseReminders()
  loadDormGoods()
  loadGraduationSale()
})
</script>

<style scoped>
/* ========== 个性化推荐横条 ========== */
.personalized-bar {
  width: calc(100% - 40px);
  max-width: 1160px;
  margin: 12px auto 0;
  padding: 14px 18px 16px;
  background: linear-gradient(135deg, #FFF5F4 0%, #FFFFFF 70%);
  border: 1px solid #F3C4C1;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(225, 37, 27, 0.06);
}

.pb-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 12px;
}

.pb-head-left {
  flex: 1;
  min-width: 0;
}

.pb-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 3px;
}

.pb-title-icon {
  color: #E1251B;
  font-size: 18px;
}

.pb-title {
  font-size: 16px;
  font-weight: 700;
  color: #333;
}

.pb-source-tag {
  padding: 1px 8px;
  background: #E1251B;
  border-radius: 10px;
  font-size: 11px;
  color: #fff;
  font-weight: 500;
}

.pb-source-tag.tag-timely {
  background: linear-gradient(135deg, #3B82F6, #2563EB);
}

.pb-source-tag.tag-browse {
  background: linear-gradient(135deg, #10B981, #059669);
}

.pb-subtitle {
  font-size: 12px;
  color: #888;
}

.pb-divider {
  width: 1px;
  height: 28px;
  background: #E8E8E8;
}

.pb-scroll {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 4px;
  scroll-behavior: smooth;
}

.pb-scroll::-webkit-scrollbar {
  height: 4px;
}

.pb-scroll::-webkit-scrollbar-thumb {
  background: #E0E0E0;
  border-radius: 2px;
}

.pb-card {
  flex: 0 0 160px;
  background: #fff;
  border: 1px solid #EEE;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.25s ease;
}

.pb-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  border-color: #F3C4C1;
}

.pb-img-wrap {
  width: 100%;
  padding-top: 100%;
  position: relative;
  background: #f5f5f5;
  overflow: hidden;
}

.pb-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s ease;
}

.pb-card:hover .pb-img {
  transform: scale(1.06);
}

.pb-info {
  padding: 8px 10px 10px;
}

.pb-card-title {
  font-size: 12px;
  color: #333;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 6px;
  min-height: 34px;
}

.pb-price-row {
  display: flex;
  align-items: baseline;
  gap: 2px;
  margin-bottom: 6px;
}

.pb-price-symbol {
  font-size: 11px;
  font-weight: 600;
  color: #E1251B;
}

.pb-price {
  font-size: 15px;
  font-weight: 700;
  color: #E1251B;
  line-height: 1;
}

.pb-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
  color: #999;
}

.pb-category {
  color: #B8B8B8;
}

.pb-mini-icon {
  font-size: 11px;
  margin-right: 2px;
  vertical-align: middle;
}

/* ========== 复购提醒 ========== */
.repurchase-bar {
  width: calc(100% - 40px);
  max-width: 1160px;
  margin: 10px auto 0;
  padding: 10px 16px;
  background: linear-gradient(135deg, #FEF3C7 0%, #FEF9C3 50%, #FFFFFF 100%);
  border: 1px solid #FDE68A;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.1);
  display: flex;
  align-items: center;
  gap: 14px;
}

.rp-icon-wrap {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  background: linear-gradient(135deg, #F59E0B, #D97706);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.rp-icon {
  font-size: 20px;
  color: #fff;
}

.rp-content {
  flex: 1;
  min-width: 0;
}

.rp-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 3px;
}

.rp-title {
  font-size: 14px;
  font-weight: 700;
  color: #B45309;
}

.rp-sub {
  font-size: 12px;
  color: #92400E;
}

.rp-items {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.rp-item {
  font-size: 12px;
  color: #78350F;
  background: rgba(245, 158, 11, 0.12);
  padding: 2px 8px;
  border-radius: 10px;
  max-width: 200px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
  transition: background 0.2s;
}

.rp-item:hover {
  background: rgba(245, 158, 11, 0.25);
}

.rp-action {
  flex-shrink: 0;
}

.rp-action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(245, 158, 11, 0.15);
  color: #B45309;
  text-decoration: none;
  transition: all 0.2s;
}

.rp-action-btn:hover {
  background: rgba(245, 158, 11, 0.3);
  transform: translateX(2px);
}

/* ========== 宿舍楼下好货 ========== */
.dorm-goods {
  width: calc(100% - 40px);
  max-width: 1160px;
  margin: 12px auto 0;
  background: #fff;
  border-radius: 8px;
  padding: 14px 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
}

.dg-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.dg-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dg-title {
  font-size: 16px;
  font-weight: 700;
  color: #222;
}

.dg-tag {
  font-size: 11px;
  color: #059669;
  background: #ECFDF5;
  border-radius: 10px;
  padding: 2px 8px;
}

.dg-sub {
  font-size: 12px;
  color: #999;
}

.dg-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
}

.dg-card {
  background: linear-gradient(180deg, #F0FDF4, #fff);
  border: 1px solid #D1FAE5;
  border-radius: 8px;
  padding: 12px 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.dg-card:hover {
  border-color: #10B981;
  box-shadow: 0 4px 14px rgba(16, 185, 129, 0.15);
  transform: translateY(-2px);
}

.dg-area {
  font-size: 15px;
  font-weight: 700;
  color: #065F46;
}

.dg-price-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.dg-count {
  font-size: 13px;
  font-weight: 700;
  color: #10B981;
}

.dg-avg {
  font-size: 11px;
  color: #999;
}

.dg-summary {
  margin-top: 6px;
  font-size: 11px;
  color: #6B7280;
  line-height: 1.5;
}

@media (max-width: 1100px) {
  .dg-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 760px) {
  .dg-grid { grid-template-columns: repeat(2, 1fr); }
}

/* ========== 宿舍区详情弹窗 ========== */
.dd-content {
  font-size: 13px;
}

.dd-summary {
  margin: 0 0 14px;
  font-size: 13px;
  color: #555;
  line-height: 1.6;
  padding: 10px 12px;
  background: #F0FDF4;
  border-radius: 6px;
  border-left: 3px solid #10B981;
}

.dd-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 14px;
}

.dd-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.dd-card:hover {
  transform: translateY(-2px);
}

.dd-img {
  width: 100%;
  height: 110px;
  object-fit: cover;
  border-radius: 6px;
  background: #f5f5f5;
}

.dd-info {
  margin-top: 6px;
}

.dd-title {
  font-size: 12px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dd-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 2px;
}

.dd-price {
  font-size: 14px;
  font-weight: 800;
  color: #E1251B;
}

.dd-cat {
  font-size: 11px;
  color: #999;
  background: #f5f5f5;
  padding: 1px 6px;
  border-radius: 3px;
}

.dd-loc {
  font-size: 11px;
  color: #10B981;
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ========== 毕业清仓专区 ========== */
.grad-sale {
  width: calc(100% - 40px);
  max-width: 1160px;
  margin: 12px auto 0;
  background: linear-gradient(135deg, #FFF7ED 0%, #fff 60%);
  border: 1px solid #FED7AA;
  border-radius: 8px;
  padding: 14px 16px;
}

.gs-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.gs-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.gs-title {
  font-size: 16px;
  font-weight: 700;
  color: #222;
}

.gs-tag {
  font-size: 11px;
  border-radius: 10px;
  padding: 2px 8px;
}

.gs-tag.gs-live {
  color: #fff;
  background: linear-gradient(90deg, #EA580C, #DC2626);
}

.gs-tag.gs-off {
  color: #6B7280;
  background: #F3F4F6;
}

.gs-sub {
  font-size: 12px;
  color: #9A3412;
}

.gs-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.gs-card {
  position: relative;
  background: #fff;
  border: 1px solid #FED7AA;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}

.gs-card:hover {
  border-color: #EA580C;
  box-shadow: 0 4px 16px rgba(234, 88, 12, 0.15);
  transform: translateY(-2px);
}

.gs-img {
  width: 100%;
  height: 130px;
  object-fit: cover;
  background: #f5f5f5;
}

.gs-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  background: linear-gradient(90deg, #EA580C, #DC2626);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(220, 38, 38, 0.3);
}

.gs-info {
  padding: 10px 12px 12px;
}

.gs-title-text {
  font-size: 13px;
  font-weight: 600;
  color: #222;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gs-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
}

.gs-price {
  font-size: 15px;
  font-weight: 800;
  color: #DC2626;
}

.gs-seller {
  font-size: 11px;
  color: #999;
}

.gs-label {
  display: inline-block;
  margin-top: 6px;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
}

.gs-label.gl-马上离校 {
  background: #FEE2E2;
  color: #DC2626;
}

.gs-label.gl-即将离校 {
  background: #FFEDD5;
  color: #EA580C;
}

.gs-label.gl-毕业清仓 {
  background: #FEF3C7;
  color: #D97706;
}

@media (max-width: 1100px) {
  .gs-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 760px) {
  .gs-grid { grid-template-columns: repeat(2, 1fr); }
}

.ld-filter-btn {
  width: 100%;
  padding: 10px;
  border: none;
  border-radius: 6px;
  background: #E1251B;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.ld-filter-btn:hover { opacity: 0.9; }

.ld-empty {
  text-align: center;
  padding: 40px 20px;
  font-size: 13px;
  color: #999;
}

/* ==================== 教材循环引导横幅 ==================== */
.textbook-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin: 16px auto 20px;
  padding: 28px 36px;
  max-width: 1180px;
  border-radius: 16px;
  background: linear-gradient(135deg, #FEF3C7 0%, #FDE68A 40%, #FCA5A5 100%);
  border: 1px solid rgba(252, 211, 77, 0.55);
  box-shadow: 0 4px 16px rgba(252, 165, 165, 0.18), 0 1px 3px rgba(0,0,0,0.04);
  cursor: pointer;
  transition: transform .2s ease, box-shadow .2s ease;
}
.textbook-hero:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(252, 165, 165, 0.25), 0 2px 6px rgba(0,0,0,0.06);
}

.tb-hero-left {
  flex: 1;
  min-width: 0;
}
.tb-hero-badge {
  display: inline-block;
  padding: 3px 12px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  color: #B45309;
  background: rgba(255, 255, 255, 0.7);
  letter-spacing: 0.5px;
  margin-bottom: 10px;
}
.tb-hero-title {
  font-size: 24px;
  font-weight: 800;
  color: #7C2D12;
  margin-bottom: 8px;
  letter-spacing: 0.3px;
}
.tb-hero-desc {
  font-size: 14px;
  color: #92400E;
  line-height: 1.6;
  margin-bottom: 16px;
  max-width: 520px;
}
.tb-hero-actions {
  display: flex;
  gap: 12px;
}
.tb-import-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 22px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #F97316, #DC2626);
  border: none;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(220, 38, 38, 0.3);
  transition: transform .15s ease, box-shadow .15s ease;
}
.tb-import-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(220, 38, 38, 0.4);
}
.tb-ghost-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #92400E;
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(251, 146, 60, 0.5);
  cursor: pointer;
  transition: background .15s ease;
}
.tb-ghost-btn:hover {
  background: rgba(255, 255, 255, 0.95);
}

.tb-hero-right {
  display: flex;
  gap: 20px;
  align-items: center;
}
.tb-stat {
  text-align: center;
  padding: 12px 18px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.8);
  min-width: 90px;
}
.tb-stat-sub {
  background: rgba(255, 255, 255, 0.5);
}
.tb-stat-num {
  font-size: 28px;
  font-weight: 800;
  color: #B91C1C;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}
.tb-stat-label {
  font-size: 12px;
  color: #92400E;
  margin-top: 4px;
  line-height: 1.3;
}

@media (max-width: 768px) {
  .textbook-hero {
    flex-direction: column;
    align-items: flex-start;
    padding: 22px;
    gap: 18px;
    margin: 12px 12px 16px;
  }
  .tb-hero-title { font-size: 20px; }
  .tb-hero-right { width: 100%; justify-content: flex-start; flex-wrap: wrap; }
  .tb-stat { padding: 10px 14px; min-width: 80px; }
  .tb-stat-num { font-size: 22px; }
}
</style>
