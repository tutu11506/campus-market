<template>
  <div class="svc-page">
    <AppSidebar />

    <div class="svc-header">
      <div class="svc-header-inner">
        <div class="svc-back" title="返回首页" @click="goHome">
          <el-icon :size="18"><ArrowLeft /></el-icon>
        </div>
        <div class="svc-brand">
          <span class="svc-logo">{{ badge }}</span>
          <span class="svc-name">{{ title }}</span>
        </div>
        <span class="svc-slogan" v-if="subtitle">{{ subtitle }}</span>
        <div class="svc-header-right">
          <div class="svc-msg" @click="goMessage">
            <el-icon :size="20"><Bell /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <div class="svc-body">
      <slot />
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ArrowLeft, Bell } from '@element-plus/icons-vue'
import AppSidebar from './AppSidebar.vue'

defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  badge: { type: String, default: '校' }
})

const router = useRouter()
const goHome = () => router.push('/')
const goMessage = () => router.push('/message')
</script>

<style scoped>
.svc-page {
  min-height: 100vh;
  background: #F6F7F9;
  padding-top: 56px;
  padding-bottom: 80px;
}

.svc-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 56px;
  z-index: 1000;
  background: linear-gradient(135deg, #E1251B 0%, #C81E18 100%);
  box-shadow: 0 2px 10px rgba(225, 37, 27, 0.28);
}

.svc-header-inner {
  max-width: 1160px;
  height: 100%;
  margin: 0 auto;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.svc-back,
.svc-msg {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 50%;
  cursor: pointer;
  transition: background 0.18s ease;
}

.svc-back:hover,
.svc-msg:hover {
  background: rgba(255, 255, 255, 0.3);
}

.svc-brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.svc-logo {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: #fff;
  color: #E1251B;
  font-size: 14px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}

.svc-name {
  color: #fff;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.svc-slogan {
  color: rgba(255, 255, 255, 0.9);
  font-size: 12px;
}

.svc-header-right {
  margin-left: auto;
}

.svc-body {
  max-width: 1160px;
  margin: 0 auto;
  padding: 12px 16px 0;
}

/* 宽屏：为左侧功能导航让出一列，顶栏同步右移保持对齐 */
@media (min-width: 1100px) {
  .svc-page {
    padding-left: 360px;
  }
  .svc-header {
    left: 360px;
  }
}

/* 中等宽度：侧栏收起为 72px 图标导轨 */
@media (min-width: 768px) and (max-width: 1099px) {
  .svc-page {
    padding-left: 72px;
  }
  .svc-header {
    left: 72px;
  }
}

@media (max-width: 768px) {
  .svc-slogan {
    display: none;
  }
}
</style>
