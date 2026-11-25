<template>
  <ServiceShell :title="group?.label || '服务分组'" :subtitle="group?.subtitle || ''" :badge="group?.badge || '服'">
    <div v-if="group" class="hub-page" :style="themeVars">
      <section class="hub-hero" :style="heroStyle">
        <span class="hero-blob blob-a"></span>
        <span class="hero-blob blob-b"></span>
        <span class="hero-watermark">
          <el-icon :size="140"><component :is="group.icon" /></el-icon>
        </span>
        <span class="hub-icon">
          <el-icon :size="28"><component :is="group.icon" /></el-icon>
        </span>
        <div class="hub-main">
          <div class="hub-kicker">校园服务 · {{ group.label }}</div>
          <div class="hub-name">{{ group.label }}</div>
          <div class="hub-sub">{{ group.intro || group.subtitle }}</div>
          <div class="hub-tags" v-if="group.highlights && group.highlights.length">
            <span v-for="tag in group.highlights" :key="tag" class="hub-tag">{{ tag }}</span>
          </div>
        </div>
        <div class="hub-side">
          <div class="hub-count">{{ group.children.length }}<small>项服务</small></div>
          <div class="hub-side-note">已按场景整理</div>
        </div>
      </section>

      <div class="grid-head">
        <span class="grid-title">全部服务</span>
        <span class="grid-line"></span>
        <span class="grid-note">挑一个马上用起来</span>
      </div>

      <div class="hub-grid">
        <button
          v-for="(item, idx) in group.children"
          :key="item.path"
          class="hub-card"
          :class="{ featured: item.featured }"
          :style="{ animationDelay: `${idx * 45}ms` }"
          @click="go(item.path)"
        >
          <span class="card-icon">
            <el-icon :size="item.featured ? 26 : 24"><component :is="item.icon" /></el-icon>
          </span>
          <span class="card-main">
            <span class="card-title">
              <span class="card-recommend" v-if="item.featured">推荐</span>
              {{ item.label }}
              <span class="card-new" v-if="item.isNew">新</span>
            </span>
            <span class="card-desc">{{ item.featured ? (item.featuredDesc || item.desc) : item.desc }}</span>
            <span class="card-meta" v-if="item.tag">{{ item.tag }}</span>
          </span>
          <span class="card-cta">
            <span>{{ item.featured ? '立即体验' : '进入' }}</span>
            <el-icon :size="12"><ArrowRight /></el-icon>
          </span>
        </button>
      </div>

      <div class="hub-tip">
        <span class="tip-label">小贴士</span>
        <span>{{ group.tip || '原有功能入口全部保留，从这里进入即可。' }}</span>
      </div>
    </div>

    <el-empty v-else description="该服务分组不存在" :image-size="90" />
  </ServiceShell>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import ServiceShell from '../components/ServiceShell.vue'
import { getServiceGroup } from '../config/serviceGroups'

const defaultTheme = { from: '#FF7A45', to: '#E1251B', soft: '#FFF3EF', accent: '#E1251B' }

const route = useRoute()
const router = useRouter()

const group = computed(() => getServiceGroup(route.params.group))
const theme = computed(() => group.value?.theme || defaultTheme)

// 分组主题色通过 CSS 变量下发，卡片、标签、小贴士共用一套配色
const themeVars = computed(() => ({
  '--hub-from': theme.value.from,
  '--hub-to': theme.value.to,
  '--hub-soft': theme.value.soft,
  '--hub-accent': theme.value.accent
}))

const heroStyle = computed(() => ({
  background: `linear-gradient(135deg, ${theme.value.from} 0%, ${theme.value.to} 100%)`
}))

const go = (path) => router.push(path)
</script>

<style scoped>
/* ========== 分组头图 ========== */
.hub-hero {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 24px 26px;
  margin-bottom: 18px;
  border-radius: 16px;
  color: #fff;
  box-shadow: 0 14px 30px rgba(31, 41, 55, 0.16);
}

.hero-blob {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.13);
  pointer-events: none;
}

.blob-a {
  width: 200px;
  height: 200px;
  right: -50px;
  top: -80px;
}

.blob-b {
  width: 140px;
  height: 140px;
  left: 34%;
  bottom: -90px;
  background: rgba(255, 255, 255, 0.09);
}

.hero-watermark {
  position: absolute;
  right: 14px;
  bottom: -26px;
  color: #fff;
  opacity: 0.16;
  pointer-events: none;
}

.hub-icon {
  position: relative;
  width: 58px;
  height: 58px;
  flex-shrink: 0;
  border-radius: 17px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.35);
}

.hub-main {
  position: relative;
  flex: 1;
  min-width: 0;
}

.hub-kicker {
  font-size: 11px;
  letter-spacing: 1.5px;
  color: rgba(255, 255, 255, 0.7);
}

.hub-name {
  margin-top: 5px;
  font-size: 23px;
  font-weight: 800;
  letter-spacing: 0.5px;
}

.hub-sub {
  margin-top: 8px;
  max-width: 700px;
  font-size: 13px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.9);
}

.hub-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 11px;
}

.hub-tag {
  padding: 2px 10px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.32);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
}

.hub-side {
  position: relative;
  flex-shrink: 0;
  text-align: right;
}

.hub-count {
  font-size: 26px;
  font-weight: 800;
  line-height: 1.1;
}

.hub-count small {
  margin-left: 3px;
  font-size: 11px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.75);
}

.hub-side-note {
  margin-top: 4px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.65);
}

/* ========== 服务列表标题 ========== */
.grid-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 2px 2px 12px;
}

.grid-title {
  font-size: 14px;
  font-weight: 800;
  color: #1F2937;
}

.grid-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, #E5E7EB, transparent);
}

.grid-note {
  font-size: 11px;
  color: #B0B4BA;
}

/* ========== 服务列表：所有入口像推荐卡一样整行向下排列 ========== */
.hub-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.hub-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  text-align: left;
  background: #fff;
  border: 1px solid #EDEEF0;
  border-radius: 14px;
  cursor: pointer;
  overflow: hidden;
  opacity: 0;
  animation: hubCardIn 0.42s ease forwards;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

/* 顶部主题色条 */
.hub-card::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--hub-from), var(--hub-to));
  opacity: 0.45;
  transition: opacity 0.18s ease;
}

.hub-card:hover {
  transform: translateY(-2px);
  border-color: #FFE0DD;
  box-shadow: 0 10px 24px rgba(31, 41, 55, 0.09);
}

.hub-card:hover::before {
  opacity: 1;
}

.card-icon {
  width: 54px;
  height: 54px;
  flex-shrink: 0;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, var(--hub-from), var(--hub-to));
  box-shadow: 0 6px 16px rgba(31, 41, 55, 0.16);
}

.card-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 700;
  color: #1F2937;
}

.card-desc {
  font-size: 12.5px;
  color: #5B6470;
  line-height: 1.6;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-meta {
  align-self: flex-start;
  padding: 2px 9px;
  border-radius: 8px;
  background: var(--hub-soft);
  color: var(--hub-accent);
  font-size: 11px;
  font-weight: 600;
}

.card-new {
  height: 15px;
  line-height: 15px;
  padding: 0 5px;
  border-radius: 8px;
  background: linear-gradient(135deg, #FF5A3C, #E1251B);
  color: #fff;
  font-size: 9px;
  font-weight: 700;
}

/* 每个入口统一使用实心按钮，和闲置盲盒保持一致 */
.card-cta {
  display: flex;
  align-items: center;
  gap: 3px;
  align-self: center;
  flex-shrink: 0;
  padding: 8px 16px;
  border-radius: 15px;
  background: var(--hub-accent);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  box-shadow: 0 4px 12px rgba(31, 41, 55, 0.16);
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.hub-card:hover .card-cta {
  transform: translateX(2px);
  box-shadow: 0 6px 16px rgba(31, 41, 55, 0.22);
}

/* ========== 推荐卡片：保留推荐标记与浅色底纹 ========== */
.hub-card.featured {
  order: -1;
  background: linear-gradient(135deg, var(--hub-soft) 0%, #fff 68%);
  border-color: transparent;
  box-shadow: 0 8px 22px rgba(31, 41, 55, 0.07);
}

.hub-card.featured::after {
  content: '';
  position: absolute;
  right: -40px;
  bottom: -60px;
  width: 170px;
  height: 170px;
  border-radius: 50%;
  background: var(--hub-soft);
  opacity: 0.85;
  pointer-events: none;
}

.hub-card.featured::before {
  height: 4px;
  opacity: 0.9;
}

.hub-card.featured:hover {
  border-color: transparent;
  box-shadow: 0 12px 28px rgba(31, 41, 55, 0.11);
}

.hub-card.featured .card-icon {
  width: 58px;
  height: 58px;
}

.hub-card.featured .card-title {
  font-size: 17px;
}

.hub-card.featured .card-meta {
  background: #fff;
  border: 1px solid var(--hub-soft);
}

.hub-card.featured .card-cta {
  position: relative;
  z-index: 1;
  padding: 9px 18px;
  border-radius: 16px;
  box-shadow: 0 5px 14px rgba(31, 41, 55, 0.2);
}

.card-recommend {
  padding: 1px 7px;
  border-radius: 8px;
  background: var(--hub-accent);
  color: #fff;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

/* ========== 底部小贴士 ========== */
.hub-tip {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 16px;
  padding: 11px 14px;
  background: var(--hub-soft);
  border-radius: 10px;
  font-size: 12px;
  color: #6B7280;
  line-height: 1.6;
}

.tip-label {
  flex-shrink: 0;
  padding: 0 7px;
  border-radius: 7px;
  background: var(--hub-accent);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 18px;
}

@keyframes hubCardIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hub-card {
    animation: none;
    opacity: 1;
  }
}

@media (max-width: 680px) {
  .hub-card {
    gap: 12px;
    padding: 14px;
  }

  .card-icon {
    width: 46px;
    height: 46px;
    border-radius: 13px;
  }

  .hub-card.featured .card-icon {
    width: 48px;
    height: 48px;
  }

  .card-cta {
    padding: 7px 12px;
    font-size: 11px;
  }

  .hub-card.featured .card-cta {
    padding: 8px 14px;
  }

  .hub-hero {
    gap: 12px;
    padding: 18px;
  }

  .hub-side {
    display: none;
  }

  .hub-name {
    font-size: 20px;
  }

  .hub-sub {
    font-size: 12px;
  }
}
</style>
