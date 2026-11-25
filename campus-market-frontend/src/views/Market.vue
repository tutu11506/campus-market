<template>
  <ServiceShell title="校园行情" subtitle="本校真实成交价，比全网数据更准" badge="行">
    <div class="stat-row">
      <div class="stat-card">
        <span class="stat-num">{{ totalOnSale }}</span>
        <span class="stat-label">件闲置在售</span>
      </div>
      <div class="stat-card">
        <span class="stat-num">{{ categories.length }}</span>
        <span class="stat-label">个分类</span>
      </div>
      <div class="stat-card">
        <span class="stat-num">{{ totalDeals }}</span>
        <span class="stat-label">笔近 12 周成交</span>
      </div>
      <div class="stat-card">
        <button class="valuation-btn" @click="valuationVisible = true">🧮 估价器</button>
        <span class="stat-label">输入原价算售价</span>
      </div>
    </div>

    <!-- 行情总览 -->
    <section class="panel">
      <div class="panel-head">
        <span class="panel-title">📈 分类行情总览</span>
        <span class="panel-sub">点分类查看在售商品</span>
      </div>
      <div class="cat-grid" v-if="categories.length">
        <div class="cat-card" v-for="c in categories" :key="'mc-' + c.categoryId" @click="openCategory(c)">
          <div class="cat-name">
            {{ c.categoryName }}
            <span class="cat-share">{{ sharePct(c) }}%</span>
          </div>
          <div class="cat-price">均价 ¥{{ c.avgPrice }}</div>
          <div class="cat-bar"><i :style="{ width: Math.max(6, sharePct(c)) + '%' }"></i></div>
          <div class="cat-meta">{{ c.count }} 件在售 · 最低 ¥{{ c.minPrice }}</div>
        </div>
      </div>
      <div class="empty" v-else>暂无行情数据</div>
    </section>

    <!-- 成交风向标 -->
    <section class="panel">
      <div class="panel-head">
        <span class="panel-title">🌬️ 成交风向标</span>
        <span class="panel-sub">真实成交价 vs 在售挂单价，看议价空间</span>
      </div>
      <div class="trend-list" v-if="trend.length">
        <div class="trend-row" v-for="d in trend" :key="'mt-' + d.categoryName">
          <div class="trend-cat">
            <span class="trend-name">{{ d.categoryName }}</span>
            <span class="trend-deals">{{ d.dealCount }} 笔成交</span>
          </div>
          <div class="trend-prices">
            <span class="trend-deal">成交 ¥{{ d.dealAvgPrice }}</span>
            <span class="trend-vs">vs</span>
            <span class="trend-sale">挂单 ¥{{ d.onSaleAvgPrice }}</span>
          </div>
          <span class="trend-verdict">{{ d.verdict }}</span>
          <span class="trend-bargain" v-if="bargainPct(d) !== null">可砍 {{ bargainPct(d) }}%</span>
        </div>
      </div>
      <div class="empty" v-else>暂无成交数据，等第一笔成交后自动生成</div>
    </section>

    <!-- 捡漏榜 -->
    <section class="panel">
      <div class="panel-head">
        <span class="panel-title">💎 校园捡漏榜</span>
        <span class="panel-sub">比同类便宜 30%+ 的在售好货</span>
      </div>
      <div class="radar-grid" v-if="radar.length">
        <div class="radar-card" v-for="item in radar" :key="'mr-' + item.id" @click="goDetail(item.id)">
          <div class="radar-img-wrap">
            <SmartImage :src="item.coverImage || item.images" class="radar-img" alt="" />
            <span class="radar-save">省 {{ item.savePct }}%</span>
          </div>
          <div class="radar-info">
            <div class="radar-title">{{ item.title }}</div>
            <div class="radar-price-row">
              <span class="radar-price">¥{{ item.price }}</span>
              <span class="radar-avg">同类均价 ¥{{ item.categoryAvg }}</span>
              <span class="radar-views" v-if="item.viewCount">👁 {{ item.viewCount }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="empty" v-else>暂时没有明显低于市场价的商品</div>
    </section>

    <!-- 看懂行情 -->
    <section class="panel">
      <div class="panel-head">
        <span class="panel-title">🧭 三句话看懂这张行情</span>
      </div>
      <div class="guide-grid">
        <div class="guide-card">
          <div class="guide-title">成交价永远低于挂单价</div>
          <div class="guide-desc">
            挂单价是卖家心理价，成交价是真实成交。两者的差额就是校园里正常的议价空间，
            看到「可砍 X%」就照着这个幅度谈。
          </div>
        </div>
        <div class="guide-card">
          <div class="guide-title">均价只做参考，别当标尺</div>
          <div class="guide-desc">
            同一分类里既有 5 块的计算器也有 5000 的笔记本，看均价的同时更要看最低价和同类均价。
          </div>
        </div>
        <div class="guide-card">
          <div class="guide-title">捡漏榜看的是"偏离度"</div>
          <div class="guide-desc">
            省 30% 以上的商品通常有两种可能：真便宜，或者有瑕疵。点进去先看描述再联系。
          </div>
        </div>
      </div>
    </section>

    <!-- 估价器 -->
    <el-dialog v-model="valuationVisible" title="🧮 校园估价器" width="420px" append-to-body>
      <p class="dlg-tip">按本校同分类的真实在售价，算算你的东西大概能卖多少钱</p>
      <el-select v-model="valuationCategory" placeholder="选择分类" style="width: 100%; margin-bottom: 10px">
        <el-option v-for="c in categories" :key="'vc-' + c.categoryId" :label="c.categoryName" :value="c.categoryId" />
      </el-select>
      <el-input v-model="valuationOriginal" type="number" placeholder="入手时的原价，如 3999" @keyup.enter="doValuation">
        <template #prefix>¥</template>
      </el-input>
      <button class="dlg-btn" :disabled="valuationLoading" @click="doValuation">
        {{ valuationLoading ? '测算中...' : '开始估价' }}
      </button>
      <div class="dlg-result" v-if="valuationResult && valuationResult.hasData">
        <span class="dlg-result-label">建议校园售价</span>
        <div class="dlg-range">¥{{ valuationResult.lowPrice }} - ¥{{ valuationResult.highPrice }}</div>
        <div class="dlg-discount">{{ valuationResult.discountText }}</div>
        <div class="dlg-ref" v-if="valuationCategoryAvg">该分类当前在售均价 ¥{{ valuationCategoryAvg }}</div>
        <p class="dlg-note">{{ valuationResult.tip }}</p>
      </div>
      <div class="dlg-empty" v-else-if="valuationResult && valuationResult.message">{{ valuationResult.message }}</div>
    </el-dialog>

    <!-- 分类在售商品 -->
    <el-dialog v-model="categoryVisible" :title="(currentCategory && currentCategory.categoryName) + ' · 在售商品'" width="520px" append-to-body>
      <div class="goods-grid" v-if="categoryGoods.length">
        <div class="goods-card" v-for="g in categoryGoods" :key="'mg-' + g.id" @click="goDetail(g.id)">
          <SmartImage :src="g.coverImage" class="goods-img" alt="" />
          <div class="goods-title">{{ g.title }}</div>
          <div class="goods-price">¥{{ g.price }}</div>
        </div>
      </div>
      <div class="empty" v-else>该分类暂无在售商品</div>
    </el-dialog>
  </ServiceShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import ServiceShell from '../components/ServiceShell.vue'
import SmartImage from '../components/SmartImage.vue'
import { getMarketOverview, getDealTrend, getDealRadar, getValuation, getGoodsList } from '../api/goods'

const router = useRouter()

const categories = ref([])
const trend = ref([])
const radar = ref([])

const totalOnSale = computed(() => categories.value.reduce((s, c) => s + (c.count || 0), 0))
const totalDeals = computed(() => trend.value.reduce((s, d) => s + (d.dealCount || 0), 0))

// 该分类占全部在售商品的百分比
const sharePct = (c) => {
  const total = totalOnSale.value || 1
  return Math.round((c.count || 0) / total * 100)
}

// 议价空间：挂单价与成交价之间的可砍幅度
const bargainPct = (d) => {
  const sale = Number(d.onSaleAvgPrice) || 0
  const deal = Number(d.dealAvgPrice) || 0
  if (sale <= 0 || deal <= 0 || deal >= sale) return null
  return Math.round((sale - deal) / sale * 100)
}

// 估价时选中的分类均价参考

const load = async () => {
  try {
    const res = await getMarketOverview()
    categories.value = (res && res.items) || []
  } catch (e) { categories.value = [] }
  try {
    const res = await getDealTrend()
    trend.value = (res && res.items) || []
  } catch (e) { trend.value = [] }
  try {
    const res = await getDealRadar()
    radar.value = (res && res.items) || []
  } catch (e) { radar.value = [] }
}

// 估价器
const valuationVisible = ref(false)
const valuationCategory = ref(null)
const valuationOriginal = ref('')
const valuationResult = ref(null)
const valuationLoading = ref(false)

// 估价时选中的分类均价参考
const valuationCategoryAvg = computed(() => {
  const c = categories.value.find(x => x.categoryId === valuationCategory.value)
  return c ? c.avgPrice : null
})

const doValuation = async () => {
  if (!valuationCategory.value) { valuationResult.value = { message: '请先选择分类' }; return }
  if (!valuationOriginal.value || Number(valuationOriginal.value) <= 0) {
    valuationResult.value = { message: '请输入有效的入手原价' }
    return
  }
  valuationLoading.value = true
  try {
    valuationResult.value = await getValuation(valuationCategory.value, valuationOriginal.value)
  } catch (e) {
    valuationResult.value = { message: '估价失败，请稍后再试' }
  } finally {
    valuationLoading.value = false
  }
}

// 分类商品
const categoryVisible = ref(false)
const currentCategory = ref(null)
const categoryGoods = ref([])

const openCategory = async (c) => {
  currentCategory.value = c
  categoryGoods.value = []
  categoryVisible.value = true
  try {
    const res = await getGoodsList({ categoryId: c.categoryId, pageNum: 1, pageSize: 12, sort: 'new' })
    categoryGoods.value = (res && res.list) || []
  } catch (e) {
    categoryGoods.value = []
  }
}

const goDetail = (id) => router.push('/goods/' + id)

onMounted(load)
</script>

<style scoped>
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
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

.stat-num {
  font-size: 24px;
  font-weight: 800;
  color: #E1251B;
}

.stat-label {
  font-size: 12px;
  color: #9CA3AF;
}

.valuation-btn {
  align-self: flex-start;
  height: 32px;
  padding: 0 14px;
  border: none;
  border-radius: 16px;
  background: linear-gradient(135deg, #FF7A45, #E1251B);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

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

.panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #1F2937;
}

.panel-sub {
  font-size: 11px;
  color: #9CA3AF;
}

.cat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 10px;
}

.cat-card {
  border: 1px solid #EFF0F2;
  border-radius: 10px;
  padding: 12px 14px;
  cursor: pointer;
  transition: all 0.18s ease;
  background: #FAFBFC;
}

.cat-card:hover {
  border-color: #FFC2B8;
  background: #FFF8F6;
  transform: translateY(-2px);
}

.cat-name {
  font-size: 14px;
  font-weight: 700;
  color: #1F2937;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cat-share {
  font-size: 10px;
  font-weight: 600;
  color: #9CA3AF;
}

.cat-bar {
  height: 5px;
  margin-top: 6px;
  background: #F1F2F4;
  border-radius: 3px;
  overflow: hidden;
}

.cat-bar i {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #FFB199, #E1251B);
  border-radius: 3px;
}

.cat-price {
  margin-top: 6px;
  font-size: 16px;
  font-weight: 800;
  color: #E1251B;
}

.cat-meta {
  margin-top: 4px;
  font-size: 11px;
  color: #9CA3AF;
}

.trend-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.trend-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  background: #FAFBFC;
  border: 1px solid #EFF0F2;
  border-radius: 10px;
}

.trend-cat {
  width: 150px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.trend-name {
  font-size: 13px;
  font-weight: 700;
  color: #1F2937;
}

.trend-deals {
  font-size: 11px;
  color: #9CA3AF;
}

.trend-prices {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.trend-deal { color: #E1251B; font-weight: 700; }
.trend-vs { color: #C0C4CC; font-size: 11px; }
.trend-sale { color: #6B7280; }

.trend-verdict {
  font-size: 11px;
  color: #B45309;
  background: #FEF3C7;
  padding: 3px 10px;
  border-radius: 10px;
  white-space: nowrap;
}

.trend-bargain {
  font-size: 11px;
  font-weight: 700;
  color: #059669;
  background: #ECFDF5;
  padding: 3px 10px;
  border-radius: 10px;
  white-space: nowrap;
}

.radar-views {
  margin-left: auto;
  font-size: 10px;
  color: #9CA3AF;
}

.guide-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.guide-card {
  background: #FAFBFC;
  border-radius: 10px;
  padding: 14px;
}

.guide-title {
  font-size: 13px;
  font-weight: 700;
  color: #1F2937;
}

.guide-desc {
  margin-top: 5px;
  font-size: 12px;
  color: #6B7280;
  line-height: 1.7;
}

.dlg-ref {
  margin-top: 6px;
  font-size: 12px;
  color: #6B7280;
}

.radar-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.radar-card {
  border: 1px solid #EFF0F2;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
}

.radar-card:hover {
  border-color: #FFC2B8;
  box-shadow: 0 6px 18px rgba(225, 37, 27, 0.12);
  transform: translateY(-2px);
}

.radar-img-wrap { position: relative; }

.radar-img {
  width: 100%;
  height: 130px;
  object-fit: cover;
  display: block;
  background: #f5f5f5;
}

.radar-save {
  position: absolute;
  top: 8px;
  left: 8px;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #10B981, #059669);
  padding: 3px 8px;
  border-radius: 10px;
}

.radar-info { padding: 9px 11px 11px; }

.radar-title {
  font-size: 12px;
  color: #1F2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.radar-price-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.radar-price { font-size: 15px; font-weight: 800; color: #E1251B; }
.radar-avg { font-size: 10px; color: #9CA3AF; }

.empty {
  padding: 26px 0;
  text-align: center;
  color: #9CA3AF;
  font-size: 13px;
}

.dlg-tip { font-size: 13px; color: #6B7280; margin-bottom: 12px; }

.dlg-btn {
  width: 100%;
  height: 38px;
  margin-top: 12px;
  border: none;
  border-radius: 19px;
  background: linear-gradient(135deg, #FF7A45, #E1251B);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.dlg-result {
  margin-top: 14px;
  padding: 14px;
  background: #FFF3F0;
  border-radius: 10px;
  text-align: center;
}

.dlg-result-label { font-size: 12px; color: #9CA3AF; }
.dlg-range { font-size: 22px; font-weight: 800; color: #E1251B; margin: 4px 0; }
.dlg-discount { font-size: 12px; color: #B45309; }
.dlg-note { margin-top: 8px; font-size: 12px; color: #6B7280; line-height: 1.6; }
.dlg-empty { margin-top: 14px; font-size: 13px; color: #9CA3AF; text-align: center; }

.goods-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.goods-card { cursor: pointer; }

.goods-img {
  width: 100%;
  height: 100px;
  object-fit: cover;
  border-radius: 8px;
  background: #f5f5f5;
}

.goods-title {
  margin-top: 5px;
  font-size: 12px;
  color: #374151;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-price { font-size: 13px; font-weight: 800; color: #E1251B; }

@media (max-width: 1000px) {
  .radar-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 760px) {
  .stat-row { grid-template-columns: repeat(2, 1fr); }
  .radar-grid { grid-template-columns: repeat(2, 1fr); }
  .trend-row { flex-wrap: wrap; }
  .guide-grid { grid-template-columns: 1fr; }
}
</style>
