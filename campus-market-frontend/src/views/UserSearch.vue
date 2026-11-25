<template>
  <div class="user-search-page">
    <!-- 顶部导航 -->
    <div class="us-header">
      <div class="us-back" @click="goBack">
        <el-icon :size="20"><ArrowLeft /></el-icon>
      </div>
      <span class="us-title">找用户</span>
      <div class="us-header-placeholder"></div>
    </div>

    <!-- 搜索框 -->
    <div class="us-search-bar">
      <el-input
        v-model="keyword"
        placeholder="输入昵称或学号搜索"
        clearable
        :prefix-icon="Search"
        @keyup.enter="doSearch(1)"
        @clear="onClear"
      />
      <button class="us-search-btn" :disabled="searching" @click="doSearch(1)">
        {{ searching ? '搜索中' : '搜索' }}
      </button>
    </div>

    <!-- 结果区 -->
    <div class="us-result" v-loading="searching">
      <!-- 未搜索 -->
      <div class="us-empty" v-if="!searched">
        <el-icon :size="56" color="#ddd"><User /></el-icon>
        <p class="us-empty-text">输入昵称或学号，找到校园里的 TA</p>
      </div>

      <!-- 搜索无果 -->
      <div class="us-empty" v-else-if="userList.length === 0">
        <el-icon :size="56" color="#ddd"><Search /></el-icon>
        <p class="us-empty-text">未找到相关用户</p>
        <p class="us-empty-sub">换个关键词试试吧</p>
      </div>

      <!-- 结果列表 -->
      <template v-else>
        <div class="us-tip">共找到 {{ total }} 位用户</div>
        <div class="user-card" v-for="u in userList" :key="u.id">
          <img :src="getImageUrl(u.avatar)" class="us-avatar" @error="handleAvatarError($event)" alt="" />
          <div class="us-info">
            <div class="us-name-row">
              <span class="us-name">{{ u.username || '匿名用户' }}</span>
              <span class="us-self" v-if="isSelf(u)">我</span>
              <span class="us-credit">信用 {{ u.credit != null ? u.credit : '—' }}</span>
            </div>
            <div class="us-meta">
              <span class="us-stuno" v-if="u.studentNo">学号：{{ u.studentNo }}</span>
              <span class="us-reg" v-if="u.createTime">注册于 {{ formatDate(u.createTime) }}</span>
            </div>
          </div>
          <div class="us-actions">
            <button class="us-btn us-btn-chat" v-if="!isSelf(u)" @click="chatWith(u)">
              <el-icon><ChatDotRound /></el-icon><span>发消息</span>
            </button>
            <button class="us-btn us-btn-shop" @click="visitShop(u)">
              <el-icon><Shop /></el-icon><span>进店</span>
            </button>
          </div>
        </div>

        <!-- 分页 -->
        <div class="us-pagination" v-if="total > pageSize">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="total"
            :current-page="pageNum"
            :page-size="pageSize"
            @current-change="doSearch"
          />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, ArrowLeft, User, ChatDotRound, Shop } from '@element-plus/icons-vue'
import { searchUsers } from '../api/user'
import { baseURL } from '../config'
import { DEFAULT_AVATAR, resolveAvatar } from '../utils/avatar'

const route = useRoute()
const router = useRouter()

const currentUserId = ref(null)
const keyword = ref('')
const userList = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const searching = ref(false)
const searched = ref(false)   // 是否已执行过搜索（区分“未搜索”与“搜索无结果”）

const getImageUrl = (path) => resolveAvatar(path)

const handleAvatarError = (e) => {
  e.target.src = DEFAULT_AVATAR
}

const formatDate = (timeStr) => {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

// 判断搜索结果是否为自己
const isSelf = (u) => {
  if (!u || u.id == null) return false
  if (currentUserId.value == null) return false
  return String(u.id) === String(currentUserId.value)
}

const doSearch = async (page = 1) => {
  const kw = keyword.value.trim()
  if (!kw) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  searched.value = true
  pageNum.value = page
  searching.value = true
  try {
    const res = await searchUsers(kw, { pageNum: page, pageSize: pageSize.value })
    userList.value = (res && res.list) ? res.list : (Array.isArray(res) ? res : [])
    total.value = (res && typeof res.total === 'number') ? res.total : userList.value.length
  } catch (err) {
    console.error('搜索用户失败', err)
    userList.value = []
    total.value = 0
  } finally {
    searching.value = false
  }
}

const onClear = () => {
  keyword.value = ''
  userList.value = []
  total.value = 0
  searched.value = false
}

// 发消息：跳转到消息页并带上对方信息，复用消息页“联系卖家”入口逻辑
const chatWith = (u) => {
  if (!u || !u.id) return
  if (isSelf(u)) {
    ElMessage.info('不能给自己发消息')
    return
  }
  router.push({
    path: '/message',
    query: {
      sellerId: u.id,
      sellerName: u.username || '匿名用户',
      sellerAvatar: u.avatar || ''
    }
  })
}

// 进店：跳转到该用户的公开主页
const visitShop = (u) => {
  if (!u || !u.id) return
  router.push(`/profile?userId=${u.id}`)
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  const userStr = localStorage.getItem('user')
  if (!userStr) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    const user = JSON.parse(userStr)
    currentUserId.value = user.id
  } catch (e) {
    console.error('解析用户信息失败', e)
    router.push('/login')
  }
})
</script>

<style scoped>
.user-search-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 80px;
}

.us-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  height: 52px;
  padding: 0 12px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}
.us-back {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 4px;
  color: #333;
  flex-shrink: 0;
}
.us-back:hover { background: #f5f5f5; }
.us-title {
  flex: 1;
  text-align: center;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}
.us-header-placeholder { width: 36px; flex-shrink: 0; }

.us-search-bar {
  display: flex;
  gap: 10px;
  padding: 12px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}
.us-search-bar .el-input { flex: 1; }
.us-search-btn {
  flex-shrink: 0;
  height: 36px;
  padding: 0 18px;
  border: none;
  border-radius: 4px;
  background: #07c160;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.us-search-btn:disabled { background: #a0e0b8; cursor: not-allowed; }

.us-result { padding: 12px; min-height: 300px; }

.us-tip {
  font-size: 12px;
  color: #999;
  margin-bottom: 10px;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  margin-bottom: 10px;
  background: #fff;
  border-radius: 8px;
}
.us-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  background: #f5f5f5;
  flex-shrink: 0;
}
.us-info { flex: 1; min-width: 0; }
.us-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.us-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.us-credit {
  font-size: 11px;
  color: #ff9800;
  background: rgba(255, 152, 0, 0.1);
  padding: 1px 8px;
  border-radius: 10px;
  flex-shrink: 0;
}
.us-self {
  font-size: 11px;
  color: #07c160;
  background: rgba(7, 193, 96, 0.1);
  padding: 1px 8px;
  border-radius: 10px;
  flex-shrink: 0;
}
.us-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
  color: #999;
}
.us-stuno, .us-reg { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.us-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex-shrink: 0;
}
.us-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 28px;
  padding: 0 12px;
  border: none;
  border-radius: 14px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.us-btn-chat { background: #07c160; color: #fff; }
.us-btn-chat:hover { background: #06ad56; }
.us-btn-shop { background: #f0f0f0; color: #555; }
.us-btn-shop:hover { background: #e4e4e4; }

.us-pagination {
  display: flex;
  justify-content: center;
  padding: 16px 0;
}

.us-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: #ccc;
}
.us-empty-text { font-size: 15px; color: #999; margin-top: 14px; }
.us-empty-sub { font-size: 12px; color: #ccc; margin-top: 6px; }
</style>
