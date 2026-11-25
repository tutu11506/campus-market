<template>
  <div id="app">
    <!-- 路由切换顶部进度条 -->
    <div class="route-progress" :class="{ active: routeLoading }">
      <div class="route-progress-bar" :style="{ width: routeProgress + '%' }"></div>
    </div>

    <router-view v-slot="{ Component }">
      <transition name="page-fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>

    <!-- 优惠券礼包：首次进入首页时居中弹窗发放 5 张不同档位的券 -->
    <CouponPopup />

    <!-- 回到顶部（右侧醒目悬浮按钮） -->
    <transition name="backtop-fade">
      <div v-if="showBackTop" class="back-top" title="回到顶部" @click="scrollToTop">
        <el-icon :size="18"><ArrowUpBold /></el-icon>
        <span class="back-top-text">顶部</span>
      </div>
    </transition>

    <!-- 底部Tab导航（登录页和管理后台不显示） -->
    <div class="tabbar" v-if="showTabbar">
      <div class="tab-item" :class="{ active: currentTab === 'home' }" @click="switchTab('/')">
        <el-icon :size="24"><HomeFilled /></el-icon>
        <span>首页</span>
      </div>
      <div class="tab-item" :class="{ active: currentTab === 'message' }" @click="switchTab('/message')">
        <div class="tab-icon-wrap">
          <el-icon :size="24"><ChatDotRound /></el-icon>
          <span class="tab-dot" v-if="hasUnread"></span>
        </div>
        <span>消息</span>
      </div>
      <div class="tab-item publish-btn" @click="switchTab('/publish')">
        <div class="publish-pill">发闲置</div>
      </div>
      <div class="tab-item" :class="{ active: currentTab === 'profile' }" @click="switchTab('/profile')">
        <el-icon :size="24"><User /></el-icon>
        <span>我的</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { HomeFilled, ChatDotRound, User, ArrowUpBold } from '@element-plus/icons-vue'
import { unreadState, refreshUnread, setUnread } from './store/unread'
import { connectRealtime, disconnectRealtime } from './store/realtime'
import CouponPopup from './components/CouponPopup.vue'

const route = useRoute()
const router = useRouter()

// 隐藏tabbar的页面
const hiddenPaths = ['/login', '/admin']
function shouldHide(path) {
  return hiddenPaths.some(p => path.startsWith(p)) || /^\/goods\/\d+/.test(path) || path === '/publish'
}

const showTabbar = computed(() => !shouldHide(route.path))

// 当前激活的tab
const currentTab = computed(() => {
  const path = route.path
  if (path === '/' || path.startsWith('/home')) return 'home'
  if (path.startsWith('/message')) return 'message'
  // 查看他人主页（带 userId）时不高亮「我的」
  if (path.startsWith('/profile') && !route.query.userId) return 'profile'
  return ''
})

// 切换tab
const switchTab = (path) => {
  if (route.path !== path) {
    router.push(path)
  }
}

// ==================== 底部消息红点 ====================
const hasUnread = computed(() => unreadState.count > 0)
let unreadTimer = null

// ==================== 路由进度条 ====================
const routeLoading = ref(false)
const routeProgress = ref(0)
let progressTimer = null
let progressDoneTimer = null

const startProgress = () => {
  clearInterval(progressTimer)
  clearTimeout(progressDoneTimer)
  routeLoading.value = true
  routeProgress.value = 8
  requestAnimationFrame(() => { routeProgress.value = 65 })
  progressTimer = setInterval(() => {
    if (routeProgress.value < 90) routeProgress.value += 4
  }, 220)
}

const finishProgress = () => {
  clearInterval(progressTimer)
  if (!routeLoading.value) return
  routeProgress.value = 100
  progressDoneTimer = setTimeout(() => {
    routeLoading.value = false
    routeProgress.value = 0
  }, 260)
}

router.beforeEach((to, from, next) => {
  if (to.fullPath !== from.fullPath) startProgress()
  next()
})
router.afterEach(() => {
  finishProgress()
  // 公开页面（登录/404 等）不触发未读数刷新——这些接口需要登录态
  const skipPaths = ['/login']
  const isLoginPage = skipPaths.some(p => route.path.startsWith(p))
  if (!isLoginPage && localStorage.getItem('token')) {
    refreshUnread()
  }
})

// ==================== 回到顶部 ====================
const showBackTop = ref(false)
const onScroll = () => {
  showBackTop.value = window.scrollY > 400
}
const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 实时未读数：收到推送立即更新红点；轮询保留作为兜底
const onRealtimeUnread = (e) => setUnread(e.detail)

// 登录/404 等公开页面不需要启动登录态相关的轮询和实时通道
const requiresLoginBootstrap = () => {
  if (!localStorage.getItem('token')) return false
  return !route.path.startsWith('/login') && !route.path.startsWith('/admin')
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
  if (requiresLoginBootstrap()) {
    refreshUnread()
    unreadTimer = setInterval(refreshUnread, 10000)
    window.addEventListener('focus', refreshUnread)
  }
  window.addEventListener('rt:unread', onRealtimeUnread)
  window.addEventListener('rt:message', refreshUnread)
  if (requiresLoginBootstrap()) {
    connectRealtime()
  }
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('focus', refreshUnread)
  window.removeEventListener('rt:unread', onRealtimeUnread)
  window.removeEventListener('rt:message', refreshUnread)
  disconnectRealtime()
  if (unreadTimer) {
    clearInterval(unreadTimer)
    unreadTimer = null
  }
  clearInterval(progressTimer)
  clearTimeout(progressDoneTimer)
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
body {
  font-family: 'Microsoft YaHei', 'PingFang SC', 'Helvetica Neue', sans-serif;
  background-color: #fff;
}
#app {
  min-height: 100vh;
  padding-bottom: 60px;
}

/* 页面切换过渡（仅透明度，避免 transform 影响页面内固定定位元素） */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.18s ease;
}
.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
}

/* 按钮按下的统一反馈 */
.el-button:active:not(.is-disabled),
button:active:not(:disabled) {
  transform: scale(0.98);
}

@media (prefers-reduced-motion: reduce) {
  .page-fade-enter-active,
  .page-fade-leave-active {
    transition: none;
  }
}
</style>

<style scoped>
/* 路由顶部进度条 */
.route-progress {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  z-index: 3000;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease;
}
.route-progress.active {
  opacity: 1;
}
.route-progress-bar {
  height: 100%;
  width: 0;
  background: #E1251B;
  border-radius: 0 2px 2px 0;
  box-shadow: 0 0 6px rgba(225, 37, 27, 0.55);
  transition: width 0.25s ease;
}

/* 回到顶部 */
.back-top {
  position: fixed;
  right: 26px;
  bottom: 96px;
  z-index: 1500;
  width: 54px;
  height: 58px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  background: linear-gradient(135deg, #E1251B, #C81E18);
  color: #fff;
  border-radius: 14px;
  box-shadow: 0 6px 18px rgba(225, 37, 27, 0.38);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}
.back-top:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 24px rgba(225, 37, 27, 0.45);
  background: linear-gradient(135deg, #F0372C, #C81E18);
}
.back-top-text {
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: 0.5px;
}

@media (max-width: 768px) {
  .back-top {
    right: 14px;
    bottom: 84px;
    width: 46px;
    height: 52px;
    border-radius: 12px;
  }
  .back-top-text {
    font-size: 10px;
  }
}

.backtop-fade-enter-active,
.backtop-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.backtop-fade-enter-from,
.backtop-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

.tabbar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-around;
  border-top: 1px solid #f0f0f0;
  z-index: 999;
  padding-bottom: env(safe-area-inset-bottom);
}

/* 宽屏已使用左侧整列导航，中屏使用图标导轨，隐藏底部 tabbar 避免重复且互相遮挡 */
@media (min-width: 768px) {
  .tabbar {
    display: none;
  }
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
  cursor: pointer;
  transition: color 0.2s ease;
  -webkit-tap-highlight-color: transparent;
  flex: 1;
  height: 100%;
}

.tab-item span {
  font-size: 11px;
  margin-top: 2px;
}

/* 底部消息按钮右上角小红点 */
.tab-icon-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.tab-dot {
  position: absolute;
  top: -2px;
  right: -5px;
  width: 8px;
  height: 8px;
  background: #E1251B;
  border: 1.5px solid #fff;
  border-radius: 50%;
}

.tab-item.active {
  color: #E1251B;
}

/* 发布按钮 - 中间突出的红色“发闲置”胶囊 */
.publish-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: -18px;
}

.publish-pill {
  padding: 0 16px;
  height: 40px;
  border-radius: 20px;
  background: linear-gradient(135deg, #E1251B, #C81E18);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  line-height: 40px;
  white-space: nowrap;
  box-shadow: 0 3px 8px rgba(225, 37, 27, 0.35);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.publish-btn:active .publish-pill {
  transform: scale(0.92);
  box-shadow: 0 2px 6px rgba(225, 37, 27, 0.25);
}
</style>
