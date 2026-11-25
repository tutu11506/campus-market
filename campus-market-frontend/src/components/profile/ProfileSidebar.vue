<template>
  <aside class="me-sidebar">
    <div class="me-nav-group">
      <div class="me-nav-group-title">我的交易</div>
      <div class="me-nav-item" :class="{ active: activeTab === 'published' }" @click="emit('switch-tab', 'published')">我发布的</div>
      <div class="me-nav-item" :class="{ active: activeTab === 'sold' }" @click="emit('switch-tab', 'sold')">我卖出的</div>
      <div class="me-nav-item" :class="{ active: activeTab === 'bought' }" @click="emit('switch-tab', 'bought')">我买到的</div>
    </div>

    <div class="me-nav-item" :class="{ active: activeTab === 'favorites' }" @click="emit('switch-tab', 'favorites')">
      <el-icon><Star /></el-icon>
      <span>我的收藏</span>
    </div>
    <div class="me-nav-item" :class="{ active: activeTab === 'browseHistory' }" @click="emit('switch-tab', 'browseHistory')">
      <el-icon><Clock /></el-icon>
      <span>浏览历史</span>
    </div>
    <div class="me-nav-item" :class="{ active: activeTab === 'receivedComments' }" @click="emit('switch-tab', 'receivedComments')">
      <el-icon><ChatDotRound /></el-icon>
      <span>收到的评价</span>
    </div>
    <div class="me-nav-item" :class="{ active: activeTab === 'invoices' }" @click="emit('switch-tab', 'invoices')">
      <el-icon><Document /></el-icon>
      <span>我的发票</span>
    </div>
    <div class="me-nav-item" :class="{ active: activeTab === 'exchanges' }" @click="emit('switch-tab', 'exchanges')">
      <el-icon><Refresh /></el-icon>
      <span>我的换物</span>
    </div>

    <div class="me-nav-group">
      <div class="me-nav-group-title">我的社区</div>
      <div class="me-nav-item" :class="{ active: activeTab === 'myPosts' }" @click="emit('switch-tab', 'myPosts')">
        <el-icon><ChatLineSquare /></el-icon>
        <span>我的帖子</span>
      </div>
      <div class="me-nav-item" :class="{ active: activeTab === 'forumNotices' }" @click="emit('switch-tab', 'forumNotices')">
        <el-icon><Bell /></el-icon>
        <span>论坛通知</span>
        <span class="me-nav-badge" v-if="forumUnread > 0">{{ forumUnread > 99 ? '99+' : forumUnread }}</span>
      </div>
    </div>

    <div class="me-nav-group">
      <div class="me-nav-group-title">账户设置</div>
      <div class="me-nav-item" @click="emit('edit-profile')">个人资料</div>
      <div class="me-nav-item danger" @click="emit('logout')">退出登录</div>
    </div>
  </aside>
</template>

<script setup>
defineProps({
  activeTab: { type: String, default: 'published' },
  forumUnread: { type: Number, default: 0 }
})
const emit = defineEmits(['switch-tab', 'edit-profile', 'logout'])
</script>

<style scoped>
/* === 左侧菜单 === */
.me-sidebar {
  position: sticky;
  top: 16px;
  width: 200px;
  flex-shrink: 0;
  padding: 10px 8px 14px;
  background: #fff;
  border: 1px solid #DCDCDC;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
}
.me-nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
  white-space: nowrap;
}
.me-nav-item .el-icon {
  font-size: 16px;
  color: #999;
}
.me-nav-item:hover {
  background: #f7f7f8;
}

/* 社区入口未读角标 */
.me-nav-badge {
  margin-left: auto;
  min-width: 17px;
  height: 17px;
  padding: 0 5px;
  line-height: 17px;
  text-align: center;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  background: #E1251B;
  border-radius: 9px;
}

.me-nav-item.active {
  background: #f2f3f5;
  color: #1a1a1a;
  font-weight: 600;
}
.me-nav-item.active .el-icon {
  color: #E1251B;
}
.me-nav-item.danger:hover {
  background: #FFF1F0;
  color: #E1251B;
}
.me-nav-group {
  margin-top: 6px;
}
.me-nav-group-title {
  padding: 10px 12px 6px;
  font-size: 12px;
  color: #aaa;
}
.me-nav-group .me-nav-item {
  padding-left: 32px;
  font-size: 13px;
}

/* === 移动端：左侧菜单横排 === */
@media (max-width: 560px) {
  .me-sidebar {
    position: static;
    width: 100%;
    display: flex;
    align-items: center;
    gap: 6px;
    overflow-x: auto;
    padding: 8px;
    margin-bottom: 10px;
    scrollbar-width: none;
  }
  .me-sidebar::-webkit-scrollbar {
    display: none;
  }
  .me-nav-group {
    display: contents;
  }
  .me-nav-group-title {
    display: none;
  }
  .me-nav-item,
  .me-nav-group .me-nav-item {
    flex-shrink: 0;
    padding: 7px 12px;
    border-radius: 15px;
    background: #f5f6f8;
    font-size: 13px;
    font-weight: 500;
  }
  .me-nav-item .el-icon {
    display: none;
  }
  .me-nav-item.active {
    background: #E1251B;
    color: #fff;
    font-weight: 600;
  }
}
</style>
