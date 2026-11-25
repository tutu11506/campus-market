<template>
  <ServiceShell title="校园面交助手" subtitle="在哪儿碰头、几点最容易成交" badge="面">
    <!-- 今日面交建议 -->
    <div class="advice-banner" v-if="hours.peakLabel">
      <span class="advice-icon">💡</span>
      <span class="advice-text">
        今天 <b>{{ hours.peakLabel }}</b> 校园里人流最多，约面交成功率和安全性都更高；
        首选 <b>{{ bestSpotName }}</b>，既有监控又顺路。
      </span>
    </div>

    <div class="top-grid">
      <!-- 面交热点 -->
      <section class="panel">
        <div class="panel-head">
          <span class="panel-title">📍 校园面交热点</span>
          <span class="panel-sub">同学最常碰头的地方</span>
        </div>
        <div class="spot-list" v-if="spots.length">
          <div
            class="spot-row"
            :class="'heat-' + s.heat"
            v-for="(s, i) in spots"
            :key="'sp-' + s.name"
            @click="openSpot(s)"
          >
            <span class="spot-rank">{{ i + 1 }}</span>
            <span class="spot-name">{{ s.name }}</span>
            <span class="spot-heat">{{ heatText(s.heat) }}</span>
            <span class="spot-count">{{ s.count }} 件在售</span>
            <span class="spot-avg">均价 ¥{{ s.avgPrice }}</span>
          </div>
        </div>
        <div class="empty" v-else>暂无面交数据</div>
      </section>

      <!-- 交易高峰 -->
      <section class="panel">
        <div class="panel-head">
          <span class="panel-title">⏰ 校园交易高峰</span>
          <span class="peak-tag" v-if="hours.peakLabel">{{ hours.peakLabel }}</span>
        </div>
        <div class="peak-tip" v-if="hours.peakTip">{{ hours.peakTip }}</div>
        <div class="hour-chart" v-if="displayHours.length">
          <div class="hour-col" v-for="h in displayHours" :key="'hh-' + h.hour">
            <div
              class="hour-bar"
              :class="{ peak: isPeak(h.hour) }"
              :style="{ height: hourHeight(h.count) }"
              :title="h.hour + ':00 · ' + h.count + ' 单'"
            ></div>
            <span class="hour-label">{{ h.hour % 3 === 0 ? h.hour : '' }}</span>
          </div>
        </div>
        <div class="hour-foot">共 {{ hours.total || 0 }} 单校园交易 · 深色为高峰时段</div>
      </section>
    </div>

    <!-- 安全提示 -->
    <section class="panel panel-safety">
      <div class="panel-head">
        <span class="panel-title">🛡️ 面交安全提示</span>
      </div>
      <div class="tip-grid">
        <div class="tip-card">
          <div class="tip-title">选人多的地方</div>
          <div class="tip-desc">优先在食堂门口、图书馆、宿舍楼下等有监控和人流的位置交易。</div>
        </div>
        <div class="tip-card">
          <div class="tip-title">当面验货再付款</div>
          <div class="tip-desc">数码产品当面开机测试，教材翻看版本和笔记，确认无误再确认收货。</div>
        </div>
        <div class="tip-card">
          <div class="tip-title">别提前转账</div>
          <div class="tip-desc">平台内的订单流程有担保，任何要求私下先付定金的行为都要警惕。</div>
        </div>
        <div class="tip-card">
          <div class="tip-title">约在白天</div>
          <div class="tip-desc">参考右侧高峰时段，晚饭前后人流最多，也更安全。</div>
        </div>
      </div>
    </section>

    <!-- 地点详情 -->
    <el-dialog v-model="spotVisible" :title="(currentSpot && currentSpot.name) || '地点详情'" width="520px" append-to-body>
      <div v-if="spotDetail && spotDetail.hasData">
        <p class="detail-summary">{{ spotDetail.summary }}</p>
        <div class="detail-stats">
          <div class="detail-stat">
            <span class="ds-num">{{ spotDetail.onSaleCount }}</span>
            <span class="ds-label">件在售</span>
          </div>
          <div class="detail-stat">
            <span class="ds-num">¥{{ spotDetail.onSaleAvgPrice }}</span>
            <span class="ds-label">均价</span>
          </div>
          <div class="detail-stat">
            <span class="ds-num">¥{{ spotDetail.onSaleMinPrice }}</span>
            <span class="ds-label">最低价</span>
          </div>
          <div class="detail-stat">
            <span class="ds-num">{{ spotDetail.dealCount90d }}</span>
            <span class="ds-label">近 90 天成交</span>
          </div>
        </div>
        <div class="detail-samples" v-if="spotDetail.topCategories && spotDetail.topCategories.length">
          <div class="ds-title">这个地点主要在卖</div>
          <div class="top-cat-row">
            <span class="top-cat" v-for="tc in spotDetail.topCategories" :key="'tc-' + tc.categoryName">
              {{ tc.categoryName }} · {{ tc.count }} 件 · 均价 ¥{{ tc.avgPrice }}
            </span>
          </div>
        </div>
        <div class="detail-samples" v-if="spotDetail.sampleGoods && spotDetail.sampleGoods.length">
          <div class="ds-title">最近上架</div>
          <div class="sample-grid">
            <div class="sample-card" v-for="g in spotDetail.sampleGoods" :key="'ss-' + g.id" @click="goDetail(g.id)">
              <SmartImage :src="g.coverImage" class="sample-img" alt="" />
              <div class="sample-title">{{ g.title }}</div>
              <div class="sample-price">¥{{ g.price }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="empty" v-else>这个地点暂时没有交易数据</div>
    </el-dialog>
  </ServiceShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import ServiceShell from '../components/ServiceShell.vue'
import SmartImage from '../components/SmartImage.vue'
import { getPickupSpots, getActiveHours, getLocationDetail } from '../api/goods'

const router = useRouter()

const spots = ref([])
const hours = ref({})

const displayHours = computed(() => (hours.value.hours || []).filter(h => h.hour >= 6 && h.hour <= 23))

const hourMax = computed(() => Math.max(1, ...(hours.value.hours || []).map(h => h.count || 0)))

const hourHeight = (count) => Math.max(4, Math.round((count || 0) / hourMax.value * 100)) + '%'

const isPeak = (h) => {
  const start = hours.value.peakStart
  const end = hours.value.peakEnd
  if (start == null || end == null) return false
  return start <= end ? (h >= start && h <= end) : (h >= start || h <= end)
}

// 热点等级文案
const heatText = (heat) => {
  if (heat === 'hot') return '很热'
  if (heat === 'warm') return '较热'
  return '一般'
}

// 推荐首选面交点（榜单第一）
const bestSpotName = computed(() => (spots.value.length ? spots.value[0].name : '食堂门口'))

const load = async () => {
  try {
    const res = await getPickupSpots()
    // 过滤历史脏数据：地点名必须包含中文
    spots.value = ((res && res.spots) || []).filter(s => /[\u4e00-\u9fa5]/.test(s.name || ''))
  } catch (e) { spots.value = [] }
  try {
    hours.value = (await getActiveHours()) || {}
  } catch (e) { hours.value = {} }
}

const spotVisible = ref(false)
const currentSpot = ref(null)
const spotDetail = ref(null)

const openSpot = async (s) => {
  currentSpot.value = s
  spotDetail.value = null
  spotVisible.value = true
  try {
    spotDetail.value = await getLocationDetail(s.name)
  } catch (e) {
    spotDetail.value = null
  }
}

const goDetail = (id) => router.push('/goods/' + id)

onMounted(load)
</script>

<style scoped>
.advice-banner {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  background: linear-gradient(90deg, #FFFBEB, #fff);
  border: 1px solid #FDE68A;
  border-radius: 12px;
  padding: 12px 16px;
  margin-bottom: 14px;
}

.advice-icon { font-size: 16px; line-height: 1.4; }

.advice-text {
  font-size: 13px;
  color: #92400E;
  line-height: 1.7;
}

.advice-text b { color: #B45309; }

.spot-heat {
  font-size: 10px;
  padding: 1px 7px;
  border-radius: 8px;
  background: #F3F4F6;
  color: #6B7280;
  flex-shrink: 0;
}

.spot-row.heat-hot .spot-heat { background: #FEE2E2; color: #DC2626; }
.spot-row.heat-warm .spot-heat { background: #FEF3C7; color: #B45309; }

.top-cat-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.top-cat {
  font-size: 11px;
  color: #4B5563;
  background: #F3F4F6;
  padding: 4px 10px;
  border-radius: 6px;
}

.top-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.panel {
  background: #fff;
  border: 1px solid #EFF0F2;
  border-radius: 14px;
  padding: 14px 16px;
}

.panel-safety {
  margin-top: 14px;
}

.panel-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.panel-title { font-size: 15px; font-weight: 700; color: #1F2937; }
.panel-sub { font-size: 11px; color: #9CA3AF; }

.peak-tag {
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(90deg, #F59E0B, #EF4444);
  padding: 2px 9px;
  border-radius: 10px;
}

.peak-tip { font-size: 12px; color: #6B7280; margin-bottom: 10px; }

.spot-list { display: flex; flex-direction: column; gap: 6px; }

.spot-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  background: #FAFBFC;
  border: 1px solid #EFF0F2;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.16s ease;
}

.spot-row:hover { background: #FFF8F6; border-color: #FFC2B8; }

.spot-row.heat-hot { background: linear-gradient(90deg, #FEF2F2, #FAFBFC); }
.spot-row.heat-warm { background: linear-gradient(90deg, #FFFBEB, #FAFBFC); }

.spot-rank {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #D1D5DB;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.spot-row.heat-hot .spot-rank { background: #EF4444; }
.spot-row.heat-warm .spot-rank { background: #F59E0B; }

.spot-name {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
  color: #1F2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spot-count { font-size: 12px; font-weight: 700; color: #E1251B; }
.spot-avg { font-size: 11px; color: #9CA3AF; }

.hour-chart {
  display: flex;
  align-items: flex-end;
  gap: 3px;
  height: 130px;
}

.hour-col {
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: center;
}

.hour-bar {
  width: 100%;
  background: #E5E7EB;
  border-radius: 3px 3px 0 0;
  transition: background 0.18s ease;
}

.hour-bar.peak { background: linear-gradient(180deg, #F97316, #E1251B); }

.hour-label {
  height: 14px;
  font-size: 9px;
  color: #9CA3AF;
  line-height: 14px;
}

.hour-foot { margin-top: 6px; font-size: 11px; color: #9CA3AF; }

.tip-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.tip-card { background: #FAFBFC; border-radius: 10px; padding: 12px 14px; }
.tip-title { font-size: 13px; font-weight: 700; color: #1F2937; }
.tip-desc { margin-top: 5px; font-size: 12px; color: #6B7280; line-height: 1.6; }

.empty { padding: 24px 0; text-align: center; color: #9CA3AF; font-size: 13px; }

.detail-summary {
  font-size: 13px;
  color: #B45309;
  background: #FEF3C7;
  padding: 10px 12px;
  border-radius: 8px;
  line-height: 1.6;
}

.detail-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-top: 12px;
}

.detail-stat {
  background: #FAFBFC;
  border-radius: 8px;
  padding: 10px;
  text-align: center;
}

.ds-num { display: block; font-size: 17px; font-weight: 800; color: #E1251B; }
.ds-label { font-size: 10px; color: #9CA3AF; }

.detail-samples { margin-top: 14px; }
.ds-title { font-size: 12px; font-weight: 700; color: #1F2937; margin-bottom: 8px; }

.sample-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.sample-card { cursor: pointer; }
.sample-img { width: 100%; height: 90px; object-fit: cover; border-radius: 8px; background: #f5f5f5; }
.sample-title { margin-top: 4px; font-size: 11px; color: #374151; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sample-price { font-size: 12px; font-weight: 800; color: #E1251B; }

@media (max-width: 900px) {
  .top-grid { grid-template-columns: 1fr; }
  .tip-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
