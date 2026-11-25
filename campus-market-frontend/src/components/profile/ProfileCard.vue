<template>
  <section class="me-card">
    <div class="me-banner"></div>
    <div class="me-card-body">
      <div class="me-avatar-wrap">
        <img :src="avatarUrl" class="me-avatar" />
      </div>
      <div class="me-info">
        <div class="me-name">{{ userInfo.username || '未设置昵称' }}</div>
        <div class="me-meta">
          <span>学号：{{ userInfo.studentNo || '未知' }}</span>
          <span class="me-meta-dot">·</span>
          <span>信用分 {{ userInfo.credit || 0 }}</span>
          <span class="me-meta-dot">·</span>
          <span>在售 {{ publishedTotal || 0 }} 件</span>
        </div>
      </div>
      <div class="me-actions">
        <button v-if="isAdmin" class="me-btn ghost" @click="emit('go-admin')">管理后台</button>
        <button class="me-btn primary" @click="emit('edit-profile')">编辑资料</button>
      </div>
    </div>

    <!-- 数据概览 -->
    <div class="me-stats">
      <div
        v-for="item in statItems"
        :key="item.key"
        class="stat-item"
        @click="emit('go-stat-tab', item.tab)"
      >
        <div class="stat-num">{{ stats[item.key] || 0 }}</div>
        <div class="stat-label">{{ item.label }}</div>
      </div>
    </div>

    <!-- 卖家数据看板（校园特色） -->
    <SellerDashboard v-if="sellerStats" :seller-stats="sellerStats" :recent-max="recentMax" />
  </section>
</template>

<script setup>
import SellerDashboard from './SellerDashboard.vue'

defineProps({
  userInfo: { type: Object, default: () => ({}) },
  avatarUrl: { type: String, default: '' },
  isAdmin: { type: Boolean, default: false },
  publishedTotal: { type: Number, default: 0 },
  stats: { type: Object, default: () => ({}) },
  statItems: { type: Array, default: () => [] },
  sellerStats: { type: Object, default: null },
  recentMax: { type: Number, default: 1 }
})
const emit = defineEmits(['edit-profile', 'go-admin', 'go-stat-tab'])
</script>

<style scoped>
/* === 个人信息卡片（图1风格） === */
.me-card {
  background: #fff;
  border: 1px solid #DCDCDC;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.07);
}
.me-banner {
  position: relative;
  height: 132px;
  overflow: hidden;
  background: linear-gradient(
    180deg,
    #FF0F23 0%,
    #FF5C5C 38%,
    #FFB8B8 72%,
    #FFFFFF 100%
  );
}
.me-card-body {
  position: relative;
  display: flex;
  align-items: flex-end;
  gap: 16px;
  margin-top: -40px;
  padding: 0 24px 14px;
}
.me-avatar-wrap {
  width: 84px;
  height: 84px;
  flex-shrink: 0;
  overflow: hidden;
  border: 4px solid #fff;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.1);
}
.me-avatar {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.me-info {
  flex: 1;
  min-width: 0;
  padding-bottom: 6px;
}
.me-name {
  font-size: 20px;
  font-weight: 700;
  color: #1f1f1f;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.me-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
  font-size: 12px;
  color: #999;
}
.me-meta-dot {
  color: #ddd;
}
.me-actions {
  display: flex;
  gap: 8px;
  padding-bottom: 6px;
  flex-shrink: 0;
}
.me-btn {
  padding: 8px 18px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.2s ease, transform 0.1s ease;
}
.me-btn:active {
  transform: scale(0.97);
}
.me-btn.primary {
  background: #2b2b2b;
  color: #fff;
}
.me-btn.primary:hover {
  background: #111;
}
.me-btn.ghost {
  background: #fff;
  color: #444;
  border: 1px solid #e4e4e4;
}
.me-btn.ghost:hover {
  border-color: #ccc;
}

/* === 数据概览（卡片内） === */
.me-stats {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  padding: 14px 6px;
  border-top: 1px solid #f3f3f3;
}
.stat-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}
.stat-item + .stat-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 1px;
  height: 26px;
  background: #f0f0f0;
}
.stat-num {
  font-size: 20px;
  font-weight: 700;
  line-height: 1.2;
  color: #222;
  font-variant-numeric: tabular-nums;
  transition: color 0.2s ease;
}
.stat-item:hover .stat-num {
  color: #E1251B;
}
.stat-label {
  font-size: 11px;
  color: #999;
}

/* === 移动端 === */
@media (max-width: 560px) {
  .me-banner {
    height: 96px;
  }
  .me-card-body {
    margin-top: -32px;
    padding: 0 14px 12px;
    gap: 12px;
  }
  .me-avatar-wrap {
    width: 68px;
    height: 68px;
    border-width: 3px;
  }
  .me-name {
    font-size: 17px;
  }
  .me-meta {
    font-size: 11px;
    gap: 4px;
  }
  .me-btn {
    padding: 6px 12px;
    font-size: 12px;
  }
  .me-meta-dot {
    display: none;
  }
}
</style>
