<template>
  <div class="seller-dashboard">
    <div class="sd-header">
      <span class="sd-title">📊 卖家数据看板</span>
      <span class="sd-sub">校园小店经营洞察 · 闲鱼/转转不会给你的数据</span>
    </div>

    <!-- 核心指标卡片 -->
    <div class="sd-overview">
      <div class="sd-card">
        <div class="sd-card-num">¥{{ sellerStats.overview?.totalAmount || '0.00' }}</div>
        <div class="sd-card-label">累计成交额</div>
      </div>
      <div class="sd-card">
        <div class="sd-card-num">{{ sellerStats.overview?.completedOrders || 0 }}</div>
        <div class="sd-card-label">成交订单</div>
      </div>
      <div class="sd-card">
        <div class="sd-card-num">{{ sellerStats.overview?.dealRate || 0 }}%</div>
        <div class="sd-card-label">成交率</div>
      </div>
      <div class="sd-card">
        <div class="sd-card-num">{{ sellerStats.overview?.totalViewCount || 0 }}</div>
        <div class="sd-card-label">总浏览量</div>
      </div>
      <div class="sd-card">
        <div class="sd-card-num">{{ sellerStats.overview?.avgViewPerGoods || 0 }}</div>
        <div class="sd-card-label">平均浏览/件</div>
      </div>
    </div>

    <div class="sd-grid-2">
      <!-- 热门商品 Top3 -->
      <div class="sd-block">
        <div class="sd-block-title">🔥 热门商品 Top3</div>
        <div v-if="sellerStats.topGoods?.length" class="sd-top-goods">
          <div v-for="(g, i) in sellerStats.topGoods" :key="g.id" class="sd-top-item">
            <span class="sd-rank" :class="'rank-' + (i + 1)">{{ i + 1 }}</span>
            <div class="sd-top-info">
              <div class="sd-top-title">{{ g.title }}</div>
              <div class="sd-top-meta">浏览 {{ g.viewCount || 0 }} · 成交 {{ g.soldCount || 0 }}</div>
            </div>
            <div class="sd-top-price">¥{{ g.price }}</div>
          </div>
        </div>
        <div v-else class="sd-empty-mini">暂无数据</div>
      </div>

      <!-- 分类分布 -->
      <div class="sd-block">
        <div class="sd-block-title">📂 分类分布</div>
        <div v-if="sellerStats.categoryDist?.length" class="sd-cat-dist">
          <div v-for="c in sellerStats.categoryDist" :key="c.name" class="sd-cat-row">
            <div class="sd-cat-name">{{ c.name }}</div>
            <div class="sd-cat-bar-wrap">
              <div class="sd-cat-bar" :style="{ width: Math.min(100, c.percent) + '%' }"></div>
            </div>
            <div class="sd-cat-count">{{ c.count }}</div>
          </div>
        </div>
        <div v-else class="sd-empty-mini">暂无数据</div>
      </div>
    </div>

    <!-- 近 7 天成交趋势 -->
    <div class="sd-block sd-trend-block" v-if="sellerStats.recentTrend?.length">
      <div class="sd-block-title">📈 近 7 天成交趋势</div>
      <div class="sd-trend">
        <div v-for="d in sellerStats.recentTrend" :key="d.date" class="sd-trend-col">
          <div class="sd-trend-bar" :style="{ height: Math.max(4, (d.count / (recentMax || 1)) * 100) + '%' }">
            <span class="sd-trend-val" v-if="d.count > 0">{{ d.count }}</span>
          </div>
          <div class="sd-trend-label">{{ d.label || d.date }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  sellerStats: { type: Object, required: true },
  recentMax: { type: Number, default: 1 }
})
</script>

<style scoped>
/* ====== 卖家数据看板 ====== */
.seller-dashboard {
  margin-top: 16px;
  background: linear-gradient(135deg, #fff 0%, #FFFAFA 100%);
  border: 1px solid #FFE0E0;
  border-radius: 12px;
  padding: 16px;
}
.sd-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px dashed #FFE0E0;
}
.sd-title {
  font-size: 16px;
  font-weight: 700;
  color: #E1251B;
}
.sd-sub {
  font-size: 12px;
  color: #aaa;
}
.sd-overview {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  margin-bottom: 16px;
}
.sd-card {
  background: #fff;
  border: 1px solid #FFE8E8;
  border-radius: 8px;
  padding: 10px 8px;
  text-align: center;
  transition: all .2s;
}
.sd-card:hover {
  border-color: #E1251B;
  box-shadow: 0 2px 8px rgba(225,37,27,0.08);
}
.sd-card-num {
  font-size: 18px;
  font-weight: 700;
  color: #E1251B;
  line-height: 1.3;
}
.sd-card-label {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}
.sd-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 14px;
}
.sd-block {
  background: #fff;
  border: 1px solid #FFE8E8;
  border-radius: 8px;
  padding: 12px;
}
.sd-block-title {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
}
.sd-top-goods {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.sd-top-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.sd-rank {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #eee;
  color: #999;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.sd-rank.rank-1 { background: #E1251B; color: #fff; }
.sd-rank.rank-2 { background: #FF6B35; color: #fff; }
.sd-rank.rank-3 { background: #FFB347; color: #fff; }
.sd-top-info {
  flex: 1;
  min-width: 0;
}
.sd-top-title {
  font-size: 12px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.sd-top-meta {
  font-size: 10px;
  color: #aaa;
  margin-top: 2px;
}
.sd-top-price {
  font-size: 13px;
  font-weight: 700;
  color: #E1251B;
}
.sd-cat-dist {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.sd-cat-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
}
.sd-cat-name {
  width: 60px;
  color: #666;
  flex-shrink: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.sd-cat-bar-wrap {
  flex: 1;
  height: 8px;
  background: #f5f5f5;
  border-radius: 4px;
  overflow: hidden;
}
.sd-cat-bar {
  height: 100%;
  background: linear-gradient(90deg, #FF6B35, #E1251B);
  border-radius: 4px;
  transition: width .4s ease;
}
.sd-cat-count {
  width: 24px;
  text-align: right;
  color: #E1251B;
  font-weight: 600;
  flex-shrink: 0;
}
.sd-trend-block {
  margin-bottom: 0;
}
.sd-trend {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 90px;
  padding-top: 6px;
}
.sd-trend-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  justify-content: flex-end;
}
.sd-trend-bar {
  width: 100%;
  background: linear-gradient(180deg, #FF6B35, #E1251B);
  border-radius: 3px 3px 0 0;
  min-height: 4px;
  position: relative;
  transition: height .4s ease;
}
.sd-trend-val {
  position: absolute;
  top: -16px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  font-weight: 600;
  color: #E1251B;
  white-space: nowrap;
}
.sd-trend-label {
  font-size: 10px;
  color: #aaa;
  margin-top: 4px;
  white-space: nowrap;
}
.sd-empty-mini {
  text-align: center;
  color: #ccc;
  font-size: 12px;
  padding: 16px 0;
}
</style>
