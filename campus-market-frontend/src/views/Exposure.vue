<template>
  <ServiceShell title="商品曝光看板" subtitle="浏览量 · 收藏 · 咨询 · 宿舍区来源，帮你调价改描述" badge="曝">
    <section class="summary-grid">
      <div class="sum-card main">
        <div class="sum-label">我的商品</div>
        <div class="sum-value">{{ summary.goodsCount ?? 0 }}</div>
        <div class="sum-sub">含在售 / 交易中 / 已售出</div>
      </div>
      <div class="sum-card">
        <div class="sum-label">总浏览量</div>
        <div class="sum-value">{{ summary.totalViews ?? 0 }}</div>
        <div class="sum-sub">商品详情页累计被打开</div>
      </div>
      <div class="sum-card">
        <div class="sum-label">总收藏数</div>
        <div class="sum-value">{{ summary.totalFavorites ?? 0 }}</div>
        <div class="sum-sub">收藏 = 有意向的潜在买家</div>
      </div>
      <div class="sum-card">
        <div class="sum-label">总咨询人数</div>
        <div class="sum-value">{{ summary.totalConsults ?? 0 }}</div>
        <div class="sum-sub">按咨询过的买家去重</div>
      </div>
    </section>

    <section class="area-panel" v-if="areas.length">
      <div class="panel-head">
        <div class="panel-title">曝光来源宿舍区</div>
        <div class="panel-note">{{ areaNote }}</div>
      </div>
      <div class="area-list">
        <div v-for="a in areas" :key="a.area" class="area-row">
          <div class="area-name">{{ a.area }}</div>
          <div class="area-bar">
            <div class="area-bar-fill" :style="{ width: barWidth(a.views) }"></div>
          </div>
          <div class="area-nums">
            <span class="views">{{ a.views }} 浏览</span>
            <span>{{ a.favorites }} 收藏</span>
            <span>{{ a.consults }} 咨询</span>
            <span class="muted">{{ a.goodsCount }} 件</span>
          </div>
        </div>
      </div>
    </section>

    <section class="goods-panel">
      <div class="panel-head">
        <div class="panel-title">
          商品明细
          <span class="panel-count">{{ goods.length }} 件</span>
        </div>
        <div class="sorts">
          <button
            v-for="s in sorts"
            :key="s.value"
            class="sort-btn"
            :class="{ active: sortBy === s.value }"
            @click="sortBy = s.value"
          >
            {{ s.label }}
          </button>
        </div>
      </div>

      <div v-loading="loading" class="goods-list">
        <article v-for="g in sortedGoods" :key="g.id" class="goods-card" @click="goDetail(g.id)">
          <SmartImage :src="g.coverImage" class="goods-cover" />
          <div class="goods-main">
            <div class="goods-top">
              <div class="goods-title">{{ g.title }}</div>
              <span class="goods-status" :class="g.status">{{ statusLabel(g.status) }}</span>
            </div>
            <div class="goods-meta">
              <span class="price">¥{{ money(g.price) }}</span>
              <span class="chip">{{ g.area }}</span>
              <span class="loc">{{ g.location || '未填写交易地点' }}</span>
            </div>
            <div class="goods-tip">{{ tipOf(g) }}</div>
          </div>
          <div class="metrics">
            <div class="metric">
              <div class="metric-num">{{ g.views }}</div>
              <div class="metric-label">浏览</div>
            </div>
            <div class="metric">
              <div class="metric-num fav">{{ g.favorites }}</div>
              <div class="metric-label">收藏</div>
            </div>
            <div class="metric">
              <div class="metric-num consult">{{ g.consults }}</div>
              <div class="metric-label">咨询</div>
            </div>
          </div>
        </article>

        <el-empty
          v-if="!loading && !goods.length"
          description="还没有发布过商品，去发布一件看看曝光数据"
          :image-size="80"
        />
      </div>
    </section>
  </ServiceShell>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import ServiceShell from '../components/ServiceShell.vue'
import SmartImage from '../components/SmartImage.vue'
import { getExposure } from '../api/finance'

const router = useRouter()
const loading = ref(false)
const goods = ref([])
const areas = ref([])
const areaNote = ref('')
const summary = ref({ goodsCount: 0, totalViews: 0, totalFavorites: 0, totalConsults: 0 })
const sortBy = ref('views')

const sorts = [
  { label: '按浏览', value: 'views' },
  { label: '按收藏', value: 'favorites' },
  { label: '按咨询', value: 'consults' }
]

const load = async () => {
  loading.value = true
  try {
    const res = await getExposure()
    summary.value = res?.summary || summary.value
    goods.value = Array.isArray(res?.goods) ? res.goods : []
    areas.value = Array.isArray(res?.areas) ? res.areas : []
    areaNote.value = res?.areaNote || ''
  } catch (e) {
    goods.value = []
    areas.value = []
  } finally {
    loading.value = false
  }
}

const sortedGoods = computed(() => {
  const key = sortBy.value
  return [...goods.value].sort((a, b) => (b[key] || 0) - (a[key] || 0))
})

const maxAreaViews = computed(() => {
  return Math.max(1, ...areas.value.map(a => a.views || 0))
})

const barWidth = (views) => {
  const pct = Math.max(4, Math.round(((views || 0) / maxAreaViews.value) * 100))
  return pct + '%'
}

const statusLabel = (status) => {
  const map = { on_sale: '在售', locked: '交易中', sold: '已售出', off_sale: '已下架' }
  return map[status] || status || '未知'
}

const money = (v) => Number(v || 0).toFixed(2)

/** 根据曝光结构给一句可执行的调整建议 */
const tipOf = (g) => {
  const views = g.views || 0
  const favorites = g.favorites || 0
  const consults = g.consults || 0
  if (views >= 20 && consults === 0 && favorites === 0) {
    return '浏览不少但没人问，试试补实拍细节图、把瑕疵写清楚'
  }
  if (favorites > 0 && consults === 0) {
    return '有人收藏在观望，小幅降价或标注可小刀更容易成交'
  }
  if (consults > 0 && favorites > consults * 2) {
    return '咨询转化一般，检查描述与实物是否一致、价格是否有竞争力'
  }
  if (views < 5) {
    return '曝光偏少，换个更清楚的封面图、补全关键词试试'
  }
  if (consults > 0) {
    return '有人主动咨询，说明价格和描述匹配，保持沟通尽快成交'
  }
  return '数据正常，继续保持，可定期刷新描述提升曝光'
}

const goDetail = (id) => router.push(`/goods/${id}`)

onMounted(load)
</script>

<style scoped>
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
  font-size: 26px;
  font-weight: 800;
  color: #1F2937;
  line-height: 1.15;
}

.sum-card.main .sum-value {
  color: #E1251B;
}

.sum-sub {
  margin-top: 6px;
  font-size: 12px;
  color: #A5AAB3;
}

.area-panel,
.goods-panel {
  background: #fff;
  border: 1px solid #EDEEF0;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 14px;
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

.panel-note {
  font-size: 12px;
  color: #A5AAB3;
}

.area-list {
  padding: 8px 18px 14px;
}

.area-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 9px 0;
}

.area-name {
  width: 76px;
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 600;
  color: #4B5563;
}

.area-bar {
  flex: 1;
  height: 8px;
  background: #F5F6F8;
  border-radius: 4px;
  overflow: hidden;
}

.area-bar-fill {
  height: 100%;
  border-radius: 4px;
  background: linear-gradient(90deg, #FF9A7A, #E1251B);
  transition: width 0.4s ease;
}

.area-nums {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
  font-size: 12px;
  color: #6B7280;
}

.area-nums .views {
  color: #E1251B;
  font-weight: 700;
}

.area-nums .muted {
  color: #B0B4BA;
}

.sorts {
  display: flex;
  gap: 8px;
}

.sort-btn {
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

.sort-btn:hover {
  border-color: #FFD4D0;
  color: #E1251B;
}

.sort-btn.active {
  border-color: #E1251B;
  background: #FFF1F0;
  color: #E1251B;
  font-weight: 700;
}

.goods-list {
  min-height: 180px;
}

.goods-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  border-bottom: 1px solid #F7F8FA;
  cursor: pointer;
  transition: background 0.16s ease;
}

.goods-card:last-child {
  border-bottom: none;
}

.goods-card:hover {
  background: #FCFCFD;
}

.goods-cover {
  width: 72px;
  height: 72px;
  flex-shrink: 0;
  border-radius: 10px;
  overflow: hidden;
  background: #F6F7F9;
}

.goods-main {
  flex: 1;
  min-width: 0;
}

.goods-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.goods-title {
  min-width: 0;
  font-size: 15px;
  font-weight: 700;
  color: #1F2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-status {
  flex-shrink: 0;
  padding: 1px 7px;
  border-radius: 6px;
  font-size: 11px;
  background: #F2F3F5;
  color: #6B7280;
}

.goods-status.on_sale {
  background: #EEF7F1;
  color: #2B7A4B;
}

.goods-status.sold {
  background: #FFF1F0;
  color: #E1251B;
}

.goods-status.locked {
  background: #FFF7E6;
  color: #B7791F;
}

.goods-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 6px;
  font-size: 12px;
  color: #8A8F99;
  flex-wrap: wrap;
}

.goods-meta .price {
  font-size: 14px;
  font-weight: 800;
  color: #E1251B;
}

.goods-meta .chip {
  padding: 1px 7px;
  border-radius: 6px;
  background: #F0F4FF;
  color: #3B5BDB;
  font-size: 11px;
  font-weight: 600;
}

.goods-meta .loc {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #A5AAB3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metrics {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.metric {
  width: 58px;
  text-align: center;
  padding: 8px 0;
  border-radius: 8px;
  background: #F9FAFB;
}

.metric-num {
  font-size: 16px;
  font-weight: 800;
  color: #1F2937;
}

.metric-num.fav {
  color: #B7791F;
}

.metric-num.consult {
  color: #3B5BDB;
}

.metric-label {
  margin-top: 2px;
  font-size: 11px;
  color: #A5AAB3;
}

@media (max-width: 900px) {
  .summary-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .area-row {
    flex-wrap: wrap;
    gap: 6px 12px;
  }

  .area-bar {
    flex-basis: 100%;
    order: 3;
  }
}

@media (max-width: 640px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .goods-card {
    flex-wrap: wrap;
  }

  .goods-main {
    flex-basis: calc(100% - 86px);
  }

  .metrics {
    margin-left: 86px;
  }

  .area-nums {
    flex-wrap: wrap;
    gap: 8px;
  }
}
</style>
