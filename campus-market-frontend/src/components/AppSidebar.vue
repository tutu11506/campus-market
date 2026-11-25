<template>
  <nav class="app-sidebar" aria-label="校园服务导航">
    <!-- 品牌区 -->
    <div class="sb-brand">
      <span class="sb-logo">校</span>
      <div class="sb-brand-text">
        <span class="sb-brand-name">校园服务</span>
        <span class="sb-brand-sub">{{ serviceGroups.length }} 大特色功能</span>
      </div>
    </div>

    <div class="sb-scroll">
      <div class="sb-section-label"><span>特色功能</span></div>

      <div
        v-for="item in navItems"
        :key="item.key"
        class="sb-item"
        :class="{ active: isActive(item) }"
        :style="itemStyle(item)"
        :title="item.title || item.label"
        @click="go(item)"
      >
        <span class="sb-icon-wrap">
          <el-icon :size="18"><component :is="item.icon" /></el-icon>
        </span>
        <span class="sb-text">
          <span class="sb-label">{{ item.label }}</span>
          <span class="sb-hint" v-if="item.hint">{{ item.hint }}</span>
          <span class="sb-desc" v-if="item.desc">{{ item.desc }}</span>
        </span>
        <span class="sb-new" v-if="item.isNew">新</span>
        <span class="sb-dot" v-if="item.key === 'me' && (hasUnread || notifyUnread > 0)"></span>
      </div>
    </div>

    <!-- 底部发布按钮 -->
    <div class="sb-footer">
      <button class="sb-publish" @click="go({ path: '/publish' })">
        <el-icon :size="15"><Plus /></el-icon>
        <span>发布闲置</span>
      </button>
    </div>
  </nav>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { House, Plus, AlarmClock } from '@element-plus/icons-vue'
import { serviceGroups } from '../config/serviceGroups'
import { unreadState } from '../store/unread'
import { getNotifyUnread } from '../api/subscribe'

const route = useRoute()
const router = useRouter()

const hasUnread = computed(() => unreadState.count > 0)

// 订阅提醒未读数（侧栏红点；收到实时推送立即刷新）
const notifyUnread = ref(0)
const refreshNotifyUnread = async () => {
  const userStr = localStorage.getItem('user')
  const token = localStorage.getItem('token')
  if (!userStr || !token) {
    notifyUnread.value = 0
    return
  }
  // token 已过期就不再请求，避免公开页面上被 401 拦截器跳去登录
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    if (payload.exp && payload.exp * 1000 < Date.now()) {
      notifyUnread.value = 0
      return
    }
  } catch (e) {
    notifyUnread.value = 0
    return
  }
  try {
    notifyUnread.value = Number(await getNotifyUnread()) || 0
  } catch (e) {
    notifyUnread.value = 0
  }
}

onMounted(() => {
  refreshNotifyUnread()
  window.addEventListener('rt:notify', refreshNotifyUnread)
})

onUnmounted(() => {
  window.removeEventListener('rt:notify', refreshNotifyUnread)
})

// 仅管理员可见的入口（自动化中心属于平台经营数据）
const isAdmin = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || '{}').role === 'admin'
  } catch (e) {
    return false
  }
})

const allNavItems = [
  {
    key: 'home',
    label: '首页',
    path: '/',
    icon: House,
    hint: '今日好物推荐',
    desc: '今日好物、校园动态与推荐商品一屏掌握',
    from: '#FFB067',
    to: '#FF4D2E'
  },
  // 功能分组：具体入口收进 /hub/:key 页面，侧栏只保留特色分组
  ...serviceGroups.map(g => ({
    key: g.key,
    label: g.label,
    title: `${g.label}：${g.subtitle}`,
    path: `/hub/${g.key}`,
    icon: g.icon,
    isNew: g.isNew,
    hint: g.navHint,
    desc: g.navDesc,
    from: g.theme?.from,
    to: g.theme?.to,
    children: g.children
  })),
  {
    key: 'automation',
    label: '自动化中心',
    path: '/automation',
    icon: AlarmClock,
    isNew: true,
    hint: '预警 · 自动化',
    desc: '预警规则、自动化任务与平台经营数据统一管理',
    from: '#A78BFA',
    to: '#7C3AED',
    adminOnly: true
  }
]

const navItems = computed(() => allNavItems.filter(i => !i.adminOnly || isAdmin.value))

// 每个入口自己的主题色，用于图标渐变、选中竖条与光晕
const itemStyle = (item) => ({
  '--item-from': item.from || '#FF7A45',
  '--item-to': item.to || '#E1251B'
})

const isActive = (item) => {
  if (item.path === '/') return route.path === '/'
  // 分组入口：在所辖任一功能页时保持高亮
  if (item.children) {
    if (route.path === item.path) return true
    return item.children.some(c => route.path === c.path || route.path.startsWith(c.path + '/'))
  }
  return route.path.startsWith(item.path)
}

const go = (item) => {
  if (route.path !== item.path) router.push(item.path)
}
</script>

<style scoped>
/* ========== 深色渐变侧栏，与白色内容区形成强对比 ========== */
.app-sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: 360px;
  z-index: 1001;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: #fff;
  /* 高饱和渐变：靛蓝 → 紫 → 玫红 → 橙，叠加三团亮色光晕 */
  background:
    radial-gradient(circle at 86% 4%, rgba(255, 176, 92, 0.5) 0%, rgba(255, 176, 92, 0) 40%),
    radial-gradient(circle at 2% 46%, rgba(56, 189, 248, 0.34) 0%, rgba(56, 189, 248, 0) 46%),
    radial-gradient(circle at 96% 96%, rgba(244, 114, 182, 0.42) 0%, rgba(244, 114, 182, 0) 46%),
    linear-gradient(160deg, #4338CA 0%, #7E22CE 24%, #BE185D 50%, #E11D48 72%, #C2410C 100%);
  background-size: 150% 150%;
  animation: sbGradientFlow 18s ease-in-out infinite alternate;
  box-shadow: 4px 0 26px rgba(25, 6, 8, 0.22);
}

/* 渐变缓慢流动，让侧栏更有生命力 */
@keyframes sbGradientFlow {
  from {
    background-position: 0% 0%;
  }
  to {
    background-position: 100% 100%;
  }
}

/* 顶部 / 底部氛围光 */
.app-sidebar::before {
  content: '';
  position: absolute;
  top: -90px;
  right: -80px;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 122, 69, 0.38) 0%, rgba(255, 122, 69, 0) 70%);
  pointer-events: none;
}

.app-sidebar::after {
  content: '';
  position: absolute;
  bottom: -100px;
  left: -80px;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.24) 0%, rgba(99, 102, 241, 0) 70%);
  pointer-events: none;
}

@media (prefers-reduced-motion: reduce) {
  .app-sidebar {
    animation: none;
  }
}
.sb-brand {
  position: relative;
  z-index: 1;
  height: 56px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.sb-logo {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: linear-gradient(135deg, #FFC078, #FF4D2E);
  color: #fff;
  font-size: 16px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 3px 12px rgba(255, 77, 46, 0.45);
}

.sb-brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
  min-width: 0;
}

.sb-brand-name {
  font-size: 17px;
  font-weight: 800;
  color: #fff;
  letter-spacing: 0.5px;
}

.sb-brand-sub {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.62);
  white-space: nowrap;
}

/* 可滚动区域 */
.sb-scroll {
  position: relative;
  z-index: 1;
  flex: 1;
  overflow-y: auto;
  padding: 6px 10px 12px;
  scrollbar-width: thin;
}

.sb-scroll::-webkit-scrollbar {
  width: 4px;
}

.sb-scroll::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 2px;
}

.sb-section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 18px 12px 9px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 2px;
  color: rgba(255, 255, 255, 0.55);
}

.sb-section-label::after {
  content: '';
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.16), transparent);
}

/* 每个分组是一张深色玻璃卡 */
.sb-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 88px;
  padding: 11px 13px;
  margin-bottom: 8px;
  border-radius: 13px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.95);
  cursor: pointer;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
  transition: background 0.18s ease, border-color 0.18s ease, transform 0.18s ease, box-shadow 0.18s ease;
}

.sb-item:hover {
  background: rgba(255, 255, 255, 0.18);
  border-color: rgba(255, 255, 255, 0.3);
  transform: translateX(2px);
}

.sb-item.active {
  background: linear-gradient(100deg, rgba(255, 255, 255, 0.28), rgba(255, 255, 255, 0.13));
  border-color: rgba(255, 255, 255, 0.38);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.22);
}

/* 选中时左侧出现该分组主题色竖条 */
.sb-item.active::before {
  content: '';
  position: absolute;
  left: -1px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 24px;
  border-radius: 0 3px 3px 0;
  background: linear-gradient(180deg, var(--item-from), var(--item-to));
  box-shadow: 0 0 10px var(--item-from);
}

/* 彩色渐变图标块，是侧栏最抢眼的视觉点 */
.sb-icon-wrap {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, var(--item-from), var(--item-to));
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.32);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.sb-item:hover .sb-icon-wrap {
  transform: scale(1.06);
}

.sb-item.active .sb-icon-wrap {
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.34), 0 0 0 2px rgba(255, 255, 255, 0.16);
}

.sb-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sb-label {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.3px;
  color: rgba(255, 255, 255, 0.92);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sb-hint {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sb-desc {
  font-size: 11.5px;
  line-height: 1.55;
  color: rgba(255, 255, 255, 0.56);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.sb-item.active .sb-label {
  color: #fff;
}

.sb-item.active .sb-hint {
  color: rgba(255, 255, 255, 0.85);
}

.sb-item.active .sb-desc {
  color: rgba(255, 255, 255, 0.7);
}

.sb-new {
  flex-shrink: 0;
  height: 18px;
  line-height: 18px;
  padding: 0 6px;
  font-size: 10px;
  font-weight: 800;
  color: #431006;
  background: linear-gradient(135deg, #FFD166, #FF8A3D);
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(255, 196, 102, 0.35);
}

.sb-dot {
  flex-shrink: 0;
  width: 7px;
  height: 7px;
  background: #FF5A5A;
  border-radius: 50%;
  box-shadow: 0 0 0 3px rgba(255, 90, 90, 0.2);
  animation: sbPulse 1.6s ease-in-out infinite;
}

@keyframes sbPulse {
  0%,
  100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.25);
    opacity: 0.7;
  }
}

/* 底部发布 */
.sb-footer {
  position: relative;
  z-index: 1;
  flex-shrink: 0;
  padding: 12px 14px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.sb-publish {
  width: 100%;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: none;
  border-radius: 21px;
  background: linear-gradient(135deg, #FF8A4C, #E1251B);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 6px 18px rgba(225, 37, 27, 0.4);
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.sb-publish:hover {
  box-shadow: 0 8px 24px rgba(255, 90, 60, 0.5);
  transform: translateY(-1px);
}

.sb-publish:active {
  transform: scale(0.97);
}

/* 中等宽度（768-1099px）：收成彩色图标导轨 */
@media (min-width: 768px) and (max-width: 1099px) {
  .app-sidebar {
    width: 72px;
  }

  .sb-brand {
    justify-content: center;
    padding: 0;
  }

  .sb-brand-text {
    display: none;
  }

  .sb-scroll {
    padding: 8px 8px 10px;
  }

  /* 导轨模式下分组标题改为分隔线，避免文字被裁切 */
  .sb-section-label {
    height: 1px;
    margin: 10px 6px;
    padding: 0;
    background: rgba(255, 255, 255, 0.1);
    font-size: 0;
    color: transparent;
    letter-spacing: 0;
    overflow: hidden;
  }

  .sb-section-label::after {
    display: none;
  }

  .sb-item {
    min-height: 52px;
    height: 52px;
    justify-content: center;
    padding: 0;
    gap: 0;
    margin-bottom: 6px;
  }

  .sb-text,
  .sb-new {
    display: none;
  }

  .sb-icon-wrap {
    width: 38px;
    height: 38px;
  }

  .sb-dot {
    position: absolute;
    top: 10px;
    right: 12px;
  }

  .sb-footer {
    padding: 10px 8px 14px;
  }

  .sb-publish {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    padding: 0;
    margin: 0 auto;
  }

  .sb-publish span {
    display: none;
  }
}

/* 窄屏隐藏，移动端由底部 tabbar 导航 */
@media (max-width: 767px) {
  .app-sidebar {
    display: none;
  }
}
</style>
