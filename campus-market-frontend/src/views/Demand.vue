<template>
  <ServiceShell title="求购热力榜" subtitle="有人想买却没人卖的品类，谁上架谁赚" badge="热">
    <!-- 整体缺口概览 -->
    <div class="stat-row" v-if="items.length">
      <div class="stat-card">
        <span class="stat-num">{{ totalWatchers }}</span>
        <span class="stat-label">人在关注这些品类</span>
      </div>
      <div class="stat-card">
        <span class="stat-num">{{ totalOnSaleItems }}</span>
        <span class="stat-label">件在售商品（越少越缺）</span>
      </div>
      <div class="stat-card">
        <span class="stat-num">{{ topCategory }}</span>
        <span class="stat-label">当前最缺货的品类</span>
      </div>
    </div>

    <!-- 玩法说明 -->
    <section class="intro">
      <div class="intro-left">
        <div class="intro-title">这批品类正在"缺货"</div>
        <div class="intro-desc">
          系统把「浏览人数」「浏览次数」和「在售件数」「成交量」放在一起算热度：
          看的人越多、卖的人越少，说明求购缺口越大。
        </div>
      </div>
      <div class="intro-formula">
        <span>热度分 =</span>
        <span class="formula-part">log(浏览人数)</span>
        <span class="formula-op">×</span>
        <span class="formula-part">浏览次数</span>
        <span class="formula-op">÷</span>
        <span class="formula-part">(在售件数 + 1)</span>
        <span class="formula-op">÷</span>
        <span class="formula-part">成交量</span>
      </div>
    </section>

    <!-- 榜单 -->
    <section class="panel">
      <div class="panel-head">
        <span class="panel-title">🔥 求购热度 TOP{{ items.length }}</span>
        <span class="panel-sub">点击分类查看该品类在售情况</span>
      </div>
      <div class="legend" v-if="items.length">
        <span class="legend-item"><i class="dot exploding"></i>爆火缺货：热度 ≥ 50</span>
        <span class="legend-item"><i class="dot gap"></i>供不应求：热度 15 ~ 50</span>
        <span class="legend-item"><i class="dot watch"></i>持续关注：热度 &lt; 15</span>
      </div>

      <div class="heat-list" v-if="items.length">
        <div class="heat-row" v-for="(d, i) in items" :key="'dh-' + d.categoryId" @click="openCategory(d)">
          <span class="rank" :class="'rank-' + (i + 1)">{{ i + 1 }}</span>

          <div class="heat-cat">
            <div class="heat-cat-name">
              {{ d.categoryName }}
              <span class="level-tag" :class="d.heatLevel">{{ levelText(d.heatLevel) }}</span>
            </div>
            <div class="heat-tip">{{ d.tip }}</div>
          </div>

          <div class="heat-stats">
            <span class="stat">👁 {{ d.browseUsers }} 人</span>
            <span class="stat" :class="{ warn: d.onSaleCount <= 5 }">📦 {{ d.onSaleCount }} 件在售</span>
            <span class="stat">🤝 30 天成交 {{ d.dealCount30d }}</span>
            <span class="stat strong">👤 每件 {{ perItem(d) }} 人盯</span>
          </div>

          <div class="heat-bar-wrap">
            <div class="heat-bar" :style="{ width: barWidth(d.heatScore) }"></div>
          </div>

          <span class="heat-score">{{ d.heatScore.toFixed(2) }}</span>

          <button class="sell-btn" @click.stop="goPublish(d)">我去卖</button>
        </div>
      </div>
      <div class="empty" v-else>暂时没有足够的浏览数据，多逛逛就会生成榜单</div>
    </section>

    <!-- 给卖家的建议 -->
    <section class="panel">
      <div class="panel-head">
        <span class="panel-title">💡 怎么用这张榜</span>
      </div>
      <div class="advice-grid">
        <div class="advice-card">
          <div class="advice-title">场景一 · 想卖闲置</div>
          <div class="advice-desc">先看榜单里有没有你要出的品类，有就直接上架，曝光和成交率都更高。</div>
        </div>
        <div class="advice-card">
          <div class="advice-title">场景二 · 想急着买</div>
          <div class="advice-desc">榜单前列说明大家都在找，价格通常不好谈，看到合适的别犹豫。</div>
        </div>
        <div class="advice-card">
          <div class="advice-title">场景三 · 想囤货转卖</div>
          <div class="advice-desc">长期缺货的品类，去二手市场收一批再挂上来，赚校园里的信息差。</div>
        </div>
      </div>
    </section>

    <!-- 分类在售 -->
    <el-dialog v-model="categoryVisible" :title="(currentCategory && currentCategory.categoryName) + ' · 当前在售'" width="540px" append-to-body>
      <div class="dlg-tip" v-if="currentCategory">
        近 30 天有 {{ currentCategory.browseUsers }} 位同学浏览过这个分类，当前只有 {{ currentCategory.onSaleCount }} 件在售
      </div>
      <div class="goods-grid" v-if="categoryGoods.length">
        <div class="goods-card" v-for="g in categoryGoods" :key="'dg-' + g.id" @click="goDetail(g.id)">
          <SmartImage :src="g.coverImage" class="goods-img" alt="" />
          <div class="goods-title">{{ g.title }}</div>
          <div class="goods-price">¥{{ g.price }}</div>
        </div>
      </div>
      <div class="empty" v-else>这个品类目前真的没人卖，先上架的人最赚</div>
    </el-dialog>
  </ServiceShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import ServiceShell from '../components/ServiceShell.vue'
import SmartImage from '../components/SmartImage.vue'
import { getDemandHeat, getGoodsList } from '../api/goods'

const router = useRouter()

const items = ref([])

const maxScore = computed(() => Math.max(...items.value.map(i => i.heatScore || 0), 1))

// 概览：总关注人数 / 在售总件数 / 最缺货品类
const totalWatchers = computed(() => items.value.reduce((s, i) => s + (i.browseUsers || 0), 0))
const totalOnSaleItems = computed(() => items.value.reduce((s, i) => s + (i.onSaleCount || 0), 0))
const topCategory = computed(() => (items.value.length ? items.value[0].categoryName : '—'))

// 单品类供需比：每件在售被多少人盯着
const perItem = (d) => {
  if (!d.onSaleCount) return '∞'
  return (d.browseUsers / d.onSaleCount).toFixed(1)
}

const barWidth = (score) => Math.max(6, Math.round((Number(score) || 0) / maxScore.value * 100)) + '%'

const levelText = (level) => {
  if (level === 'exploding') return '爆火缺货'
  if (level === 'gap') return '供不应求'
  return '持续关注'
}

const load = async () => {
  try {
    const res = await getDemandHeat()
    items.value = (res && res.items) || []
  } catch (e) {
    items.value = []
  }
}

const categoryVisible = ref(false)
const currentCategory = ref(null)
const categoryGoods = ref([])

const openCategory = async (d) => {
  currentCategory.value = d
  categoryGoods.value = []
  categoryVisible.value = true
  try {
    const res = await getGoodsList({ categoryId: d.categoryId, pageNum: 1, pageSize: 12, sort: 'views' })
    categoryGoods.value = (res && res.list) || []
  } catch (e) {
    categoryGoods.value = []
  }
}

const goDetail = (id) => router.push('/goods/' + id)
const goPublish = (d) => router.push({ path: '/publish', query: { categoryId: d.categoryId } })

onMounted(load)
</script>

<style scoped>
.intro {
  background: linear-gradient(135deg, #FFF7ED 0%, #fff 65%);
  border: 1px solid #FED7AA;
  border-radius: 14px;
  padding: 16px 18px;
}

.intro-title { font-size: 16px; font-weight: 800; color: #9A3412; }
.intro-desc { margin-top: 6px; font-size: 12px; color: #78716C; line-height: 1.7; max-width: 640px; }

.intro-formula {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 12px;
  font-size: 12px;
  color: #78716C;
}

.formula-part {
  background: #fff;
  border: 1px solid #FED7AA;
  border-radius: 6px;
  padding: 3px 8px;
  font-weight: 600;
  color: #C2410C;
}

.formula-op { color: #C0C4CC; }

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

.heat-list { display: flex; flex-direction: column; gap: 8px; }

.heat-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: #FAFBFC;
  border: 1px solid #EFF0F2;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.heat-row:hover {
  background: #FFF8F6;
  border-color: #FFC2B8;
}

.rank {
  width: 26px;
  height: 26px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #D1D5DB;
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}

.rank-1 { background: linear-gradient(135deg, #EF4444, #B91C1C); }
.rank-2 { background: linear-gradient(135deg, #F59E0B, #B45309); }
.rank-3 { background: linear-gradient(135deg, #10B981, #047857); }
.rank-4 { background: linear-gradient(135deg, #6366F1, #4338CA); }
.rank-5 { background: linear-gradient(135deg, #8B5CF6, #6D28D9); }
.rank-6 { background: linear-gradient(135deg, #EC4899, #BE185D); }

.heat-cat { width: 220px; flex-shrink: 0; }

.heat-cat-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
  color: #1F2937;
}

.level-tag {
  font-size: 9px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 8px;
}

.level-tag.exploding { background: #FEE2E2; color: #DC2626; }
.level-tag.gap { background: #FEF3C7; color: #B45309; }
.level-tag.watch { background: #F3F4F6; color: #6B7280; }

.heat-tip { margin-top: 3px; font-size: 11px; color: #9CA3AF; }

.heat-stats { display: flex; gap: 10px; flex-wrap: wrap; }

.stat { font-size: 11px; color: #6B7280; }
.stat.warn { color: #DC2626; font-weight: 700; }
.stat.strong { color: #C2410C; font-weight: 700; }

/* 概览卡 */
.stat-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 14px;
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

/* 热度等级图例 */
.legend {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 10px;
  font-size: 11px;
  color: #6B7280;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.dot.exploding { background: #EF4444; }
.dot.gap { background: #F59E0B; }
.dot.watch { background: #9CA3AF; }

.heat-bar-wrap {
  flex: 1;
  min-width: 60px;
  height: 8px;
  background: #F1F2F4;
  border-radius: 4px;
  overflow: hidden;
}

.heat-bar {
  height: 100%;
  background: linear-gradient(90deg, #F59E0B, #EF4444);
  border-radius: 4px;
  transition: width 0.4s ease;
}

.heat-score {
  width: 44px;
  text-align: right;
  font-size: 13px;
  font-weight: 800;
  color: #EF4444;
}

.sell-btn {
  height: 28px;
  padding: 0 12px;
  border: 1px solid #FFC2B8;
  border-radius: 14px;
  background: #fff;
  color: #E1251B;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.16s ease;
}

.sell-btn:hover {
  background: #E1251B;
  color: #fff;
  border-color: #E1251B;
}

.advice-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.advice-card {
  background: #FAFBFC;
  border-radius: 10px;
  padding: 14px;
}

.advice-title { font-size: 13px; font-weight: 700; color: #1F2937; }
.advice-desc { margin-top: 5px; font-size: 12px; color: #6B7280; line-height: 1.6; }

.empty { padding: 26px 0; text-align: center; color: #9CA3AF; font-size: 13px; }

.dlg-tip {
  font-size: 12px;
  color: #B45309;
  background: #FEF3C7;
  padding: 10px 12px;
  border-radius: 8px;
  margin-bottom: 12px;
  line-height: 1.6;
}

.goods-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.goods-card { cursor: pointer; }
.goods-img { width: 100%; height: 100px; object-fit: cover; border-radius: 8px; background: #f5f5f5; }
.goods-title { margin-top: 5px; font-size: 12px; color: #374151; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.goods-price { font-size: 13px; font-weight: 800; color: #E1251B; }

@media (max-width: 1000px) {
  .heat-cat { width: 150px; }
  .heat-stats { display: none; }
  .advice-grid { grid-template-columns: 1fr; }
  .stat-row { grid-template-columns: 1fr; }
}
</style>
