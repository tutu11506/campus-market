<template>
  <ServiceShell title="校园论坛" subtitle="失物招领 · 学习交流 · 拼单组队 · 校园闲聊" badge="坛">
    <!-- 顶部横幅 -->
    <section class="forum-hero">
      <div class="hero-left">
        <div class="hero-title">有话说，来论坛聊聊</div>
        <div class="hero-desc">捡到东西发失物招领，找队友拼单组队，学习上的疑问也能一起讨论。</div>
        <div class="hero-tags">
          <span>📚 学习交流</span>
          <span>🔍 失物招领</span>
          <span>🤝 拼单组队</span>
        </div>
      </div>
      <div class="hero-right">
        <div class="hero-stat">
          <span class="hero-num">{{ total }}</span>
          <span class="hero-label">篇帖子</span>
        </div>
        <button class="publish-btn" @click="openPublish">＋ 发帖子</button>
      </div>
    </section>

    <!-- 板块 + 搜索 -->
    <div class="forum-toolbar">
      <div class="board-tabs">
        <span
          v-for="b in boardTabs"
          :key="b.value"
          class="board-tab"
          :class="{ active: activeBoard === b.value }"
          @click="switchBoard(b.value)"
        >{{ b.label }}</span>
      </div>
      <el-input
        v-model="keyword"
        class="search-input"
        size="small"
        placeholder="搜索帖子标题或内容"
        clearable
        @keyup.enter="reload"
        @clear="reload"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <!-- 帖子列表 -->
    <div class="post-list" v-if="posts.length">
      <article class="post-card" v-for="p in posts" :key="'fp-' + p.id">
        <img class="pc-avatar" :src="resolveAvatar(p.authorAvatar)" @error="onAvatarError" alt="" />
        <div class="pc-main">
          <div class="pc-top">
            <span class="pc-board" :class="'bd-' + p.board">{{ boardLabel(p.board) }}</span>
            <span class="pc-time">{{ fromNow(p.createTime) }}</span>
          </div>
          <h3 class="pc-title" @click="openDetail(p)">{{ p.title }}</h3>
          <p class="pc-excerpt">{{ excerpt(p.content) }}</p>
          <div class="pc-foot">
            <span class="pc-author">{{ p.authorName || '匿名同学' }}</span>
            <span class="pc-stat">👁 {{ p.viewCount || 0 }}</span>
            <span class="pc-stat">💬 {{ p.commentCount || 0 }}</span>
            <div class="pc-actions">
              <button class="like-btn" :class="{ liked: p.liked }" @click="handleLike(p)">
                <span class="like-icon">👍</span>{{ p.likeCount || 0 }}
              </button>
              <button v-if="canDelete(p)" class="del-btn" @click="removePost(p)">删除</button>
              <button class="open-btn" @click="openDetail(p)">查看详情</button>
            </div>
          </div>
        </div>
      </article>
    </div>

    <div class="empty" v-else-if="!loading">
      <div class="empty-icon">🗒️</div>
      <div class="empty-title">这个板块还没有帖子</div>
      <div class="empty-desc">来发第一篇，让同学们看到你的声音</div>
      <button class="empty-btn" @click="openPublish">发帖子</button>
    </div>

    <div class="loading" v-if="loading">加载中...</div>

    <div class="more-row" v-if="hasMore && posts.length">
      <button class="btn-more" :disabled="loading" @click="loadMore">加载更多</button>
    </div>

    <!-- 发帖弹窗 -->
    <el-dialog v-model="publishVisible" title="发布帖子" width="520px" append-to-body>
      <div class="form-item">
        <label>选择板块</label>
        <div class="board-picker">
          <span
            v-for="b in boardTabs.slice(1)"
            :key="'pick-' + b.value"
            class="board-chip"
            :class="{ active: form.board === b.value }"
            @click="form.board = b.value"
          >{{ b.label }}</span>
        </div>
      </div>
      <div class="form-item">
        <label>标题 <i>*</i></label>
        <el-input v-model="form.title" maxlength="120" show-word-limit placeholder="一句话说清楚你要聊什么" />
      </div>
      <div class="form-item">
        <label>正文 <i>*</i></label>
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="6"
          maxlength="5000"
          show-word-limit
          placeholder="补充详细信息，比如时间地点、物品特征、问题背景等"
        />
      </div>
      <div class="form-actions">
        <button class="btn-ghost" @click="publishVisible = false">取消</button>
        <button class="btn-primary" :disabled="publishing" @click="submitPublish">
          {{ publishing ? '发布中...' : '发布' }}
        </button>
      </div>
    </el-dialog>

    <!-- 帖子详情 -->
    <el-dialog v-model="detailVisible" width="640px" append-to-body class="detail-dialog">
      <template #header>
        <div class="dlg-head">
          <span class="pc-board" :class="'bd-' + (detail && detail.board)">{{ boardLabel(detail && detail.board) }}</span>
          <span class="dlg-title">帖子详情</span>
        </div>
      </template>

      <div v-if="detail" class="detail-body">
        <h2 class="detail-title">{{ detail.title }}</h2>
        <div class="detail-meta">
          <img class="detail-avatar" :src="resolveAvatar(detail.authorAvatar)" @error="onAvatarError" alt="" />
          <span class="detail-author">{{ detail.authorName || '匿名同学' }}</span>
          <span class="pc-time">{{ fmtTime(detail.createTime) }}</span>
          <span class="pc-stat">👁 {{ detail.viewCount || 0 }}</span>
        </div>
        <div class="detail-content">{{ detail.content }}</div>
        <div class="detail-actions">
          <button class="like-btn big" :class="{ liked: detail.liked }" @click="handleLike(detail)">
            <span class="like-icon">👍</span>{{ detail.liked ? '已赞' : '点赞' }} {{ detail.likeCount || 0 }}
          </button>
          <button v-if="canDelete(detail)" class="del-btn" @click="removePost(detail)">删除帖子</button>
        </div>

        <div class="comment-head">
          <span>全部回复</span>
          <span class="comment-count">{{ comments.length }}</span>
        </div>

        <div class="comment-list" v-if="comments.length">
          <div class="comment-item" v-for="c in comments" :key="'fc-' + c.id">
            <img class="comment-avatar" :src="resolveAvatar(c.authorAvatar)" @error="onAvatarError" alt="" />
            <div class="comment-main">
              <div class="comment-top">
                <span class="comment-author">{{ c.authorName || '匿名同学' }}</span>
                <span class="pc-time">{{ fromNow(c.createTime) }}</span>
              </div>
              <div class="comment-text">
                <span class="reply-to" v-if="c.replyToName">回复 @{{ c.replyToName }}：</span>{{ c.content }}
              </div>
            </div>
            <div class="comment-ops">
              <button class="comment-reply" @click="startReply(c)">回复</button>
              <button v-if="canDelete(c)" class="comment-del" @click="removeComment(c)">删除</button>
            </div>
          </div>
        </div>
        <div class="comment-empty" v-else>还没有回复，来说两句吧</div>

        <div class="comment-editor">
          <div class="reply-target" v-if="replyTarget">
            <span>回复 @{{ replyTarget.authorName || '同学' }}</span>
            <button class="reply-cancel" title="取消回复" @click="cancelReply">✕</button>
          </div>
          <el-input
            v-model="commentText"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            :placeholder="isLoggedIn ? (replyTarget ? '回复 @' + (replyTarget.authorName || '同学') + '…' : '写下你的回复…') : '登录后即可回复'"
            :disabled="!isLoggedIn"
          />
          <div class="editor-foot">
            <span class="editor-hint" v-if="!isLoggedIn">登录后可发帖、点赞和回复</span>
            <button class="btn-primary" :disabled="submitting || !isLoggedIn" @click="submitComment">
              {{ submitting ? '发送中...' : '发表回复' }}
            </button>
          </div>
        </div>
      </div>
    </el-dialog>
  </ServiceShell>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import ServiceShell from '../components/ServiceShell.vue'
import { DEFAULT_AVATAR, resolveAvatar } from '../utils/avatar'
import {
  getPosts, getPostDetail, createPost, deletePost,
  togglePostLike, getComments, addComment, deleteComment
} from '../api/forum'

const router = useRouter()
const route = useRoute()

const boardTabs = [
  { value: '', label: '全部' },
  { value: 'chat', label: '校园闲聊' },
  { value: 'lost', label: '失物招领' },
  { value: 'study', label: '学习交流' },
  { value: 'team', label: '拼单组队' },
  { value: 'help', label: '求助问答' }
]
const BOARD_LABEL = {
  chat: '校园闲聊',
  lost: '失物招领',
  study: '学习交流',
  team: '拼单组队',
  help: '求助问答'
}

const activeBoard = ref('')
const keyword = ref('')
const posts = ref([])
const total = ref(0)
const pageNum = ref(1)
const hasMore = ref(false)
const loading = ref(false)

const currentUser = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || '{}')
  } catch (e) {
    return {}
  }
})
const currentUserId = computed(() => currentUser.value.id || null)
const isAdmin = computed(() => currentUser.value.role === 'admin')
const isLoggedIn = computed(() => !!localStorage.getItem('token'))

const boardLabel = (b) => BOARD_LABEL[b] || '校园闲聊'

const canDelete = (item) => {
  if (!isLoggedIn.value || !item) return false
  if (isAdmin.value) return true
  return !!currentUserId.value && item.userId === currentUserId.value
}

// 时间解析：兼容时间戳 / ISO / "yyyy-MM-dd HH:mm:ss"
const parseDate = (v) => {
  if (v == null) return null
  if (typeof v === 'number') return new Date(v)
  const s = String(v)
  const d = new Date(s.includes('T') ? s : s.replace(/-/g, '/'))
  return isNaN(d.getTime()) ? null : d
}

const fmtTime = (v) => {
  const d = parseDate(v)
  if (!d) return '—'
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

const fromNow = (v) => {
  const d = parseDate(v)
  if (!d) return ''
  const diff = Date.now() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + ' 分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + ' 小时前'
  if (diff < 7 * 86400000) return Math.floor(diff / 86400000) + ' 天前'
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

const excerpt = (text) => {
  const s = String(text || '').replace(/\s+/g, ' ').trim()
  return s.length > 110 ? s.slice(0, 110) + '…' : s
}

const onAvatarError = (e) => {
  if (e && e.target) e.target.src = DEFAULT_AVATAR
}

// ==================== 列表 ====================
const loadPosts = async (append = false) => {
  pageNum.value = append ? pageNum.value + 1 : 1
  loading.value = true
  try {
    const res = await getPosts({
      board: activeBoard.value || undefined,
      keyword: keyword.value.trim() || undefined,
      pageNum: pageNum.value,
      pageSize: 10
    })
    const list = (res && res.list) || []
    posts.value = append ? [...posts.value, ...list] : list
    total.value = (res && res.total) || 0
    hasMore.value = !!(res && res.pageNum < res.pages)
  } catch (e) {
    if (!append) {
      posts.value = []
      total.value = 0
    }
  } finally {
    loading.value = false
  }
}

const reload = () => loadPosts(false)
const loadMore = () => loadPosts(true)

const switchBoard = (b) => {
  if (activeBoard.value === b) return
  activeBoard.value = b
  reload()
}

// ==================== 发帖 ====================
const publishVisible = ref(false)
const publishing = ref(false)
const form = reactive({ board: 'chat', title: '', content: '' })

const openPublish = () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录再发帖')
    router.push('/login?redirect=/forum')
    return
  }
  publishVisible.value = true
}

const submitPublish = async () => {
  if (form.title.trim().length < 2) {
    ElMessage.warning('标题至少 2 个字')
    return
  }
  if (!form.content.trim()) {
    ElMessage.warning('正文不能为空')
    return
  }
  publishing.value = true
  try {
    await createPost({
      board: form.board,
      title: form.title.trim(),
      content: form.content.trim()
    })
    ElMessage.success('发布成功')
    publishVisible.value = false
    form.title = ''
    form.content = ''
    activeBoard.value = ''
    reload()
  } catch (e) {
    // 拦截器已提示
  } finally {
    publishing.value = false
  }
}

// ==================== 详情与回复 ====================
const detailVisible = ref(false)
const detail = ref(null)
const comments = ref([])
const commentText = ref('')
const submitting = ref(false)
const replyTarget = ref(null)

const startReply = (c) => {
  if (!isLoggedIn.value) {
    ElMessage.warning('登录后才能回复')
    router.push('/login?redirect=/forum')
    return
  }
  replyTarget.value = c
}

const cancelReply = () => {
  replyTarget.value = null
}

const openDetail = async (p) => {
  detailVisible.value = true
  detail.value = p
  comments.value = []
  commentText.value = ''
  replyTarget.value = null
  try {
    const [d, cs] = await Promise.all([getPostDetail(p.id), getComments(p.id)])
    detail.value = d || p
    comments.value = cs || []
    const target = posts.value.find(x => x.id === p.id)
    if (target && d) {
      target.viewCount = d.viewCount
      target.likeCount = d.likeCount
      target.commentCount = d.commentCount
      target.liked = d.liked
    }
  } catch (e) {
    // 拦截器已提示
  }
}

const submitComment = async () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('登录后才能回复')
    router.push('/login?redirect=/forum')
    return
  }
  if (!commentText.value.trim()) {
    ElMessage.warning('回复内容不能为空')
    return
  }
  submitting.value = true
  try {
    await addComment(detail.value.id, commentText.value.trim(), replyTarget.value ? replyTarget.value.id : null)
    commentText.value = ''
    replyTarget.value = null
    comments.value = await getComments(detail.value.id)
    detail.value.commentCount = comments.value.length
    const target = posts.value.find(x => x.id === detail.value.id)
    if (target) target.commentCount = comments.value.length
    ElMessage.success('回复成功')
  } catch (e) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

const removeComment = async (c) => {
  try {
    await ElMessageBox.confirm('确定删除这条回复吗？', '删除回复', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await deleteComment(c.id)
    comments.value = comments.value.filter(x => x.id !== c.id)
    if (detail.value) {
      detail.value.commentCount = comments.value.length
      const target = posts.value.find(x => x.id === detail.value.id)
      if (target) target.commentCount = comments.value.length
    }
    ElMessage.success('已删除')
  } catch (e) {
    // 拦截器已提示
  }
}

// ==================== 点赞 ====================
const handleLike = async (p) => {
  if (!isLoggedIn.value) {
    ElMessage.warning('登录后才能点赞')
    router.push('/login?redirect=/forum')
    return
  }
  try {
    const res = await togglePostLike(p.id)
    if (res && res.needLogin) {
      router.push('/login?redirect=/forum')
      return
    }
    const liked = !!(res && res.liked)
    const count = res && res.likeCount != null ? res.likeCount : p.likeCount
    p.liked = liked
    p.likeCount = count
    const target = posts.value.find(x => x.id === p.id)
    if (target) {
      target.liked = liked
      target.likeCount = count
    }
    if (detail.value && detail.value.id === p.id) {
      detail.value.liked = liked
      detail.value.likeCount = count
    }
  } catch (e) {
    // 拦截器已提示
  }
}

// ==================== 删除帖子 ====================
const removePost = async (p) => {
  try {
    await ElMessageBox.confirm('删除后帖子将不再展示，确定删除吗？', '删除帖子', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await deletePost(p.id)
    posts.value = posts.value.filter(x => x.id !== p.id)
    total.value = Math.max(0, total.value - 1)
    if (detail.value && detail.value.id === p.id) {
      detailVisible.value = false
    }
    ElMessage.success('已删除')
  } catch (e) {
    // 拦截器已提示
  }
}

onMounted(async () => {
  await loadPosts(false)
  // 从“我的帖子/通知”跳转进来时，直接打开对应帖子详情
  const pid = Number(route.query.postId)
  if (pid) openDetail({ id: pid })
})
</script>

<style scoped>
/* ==================== 顶部横幅 ==================== */
.forum-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px;
  margin-bottom: 14px;
  border-radius: 14px;
  color: #fff;
  background: linear-gradient(120deg, #2B3A67 0%, #3F5A9E 52%, #E1251B 130%);
  box-shadow: 0 8px 22px rgba(43, 58, 103, 0.18);
}

.hero-title {
  font-size: 21px;
  font-weight: 800;
  letter-spacing: 0.5px;
}

.hero-desc {
  margin-top: 7px;
  font-size: 13px;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.86);
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.hero-tags span {
  padding: 3px 10px;
  font-size: 11px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.16);
}

.hero-right {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.hero-stat {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.hero-num {
  font-size: 30px;
  font-weight: 800;
  line-height: 1;
}

.hero-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.82);
}

.publish-btn {
  height: 36px;
  padding: 0 18px;
  border: none;
  border-radius: 18px;
  background: #fff;
  color: #E1251B;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.16s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.14);
}

.publish-btn:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

.publish-btn:active {
  transform: scale(0.97);
}

/* ==================== 工具栏 ==================== */
.forum-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  margin-bottom: 12px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(17, 24, 39, 0.05);
}

.board-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.board-tab {
  padding: 6px 13px;
  font-size: 13px;
  color: #4B5563;
  border-radius: 16px;
  cursor: pointer;
  transition: background 0.16s ease, color 0.16s ease;
  white-space: nowrap;
}

.board-tab:hover {
  background: #F7F8FA;
  color: #E1251B;
}

.board-tab.active {
  background: #FFF1F0;
  color: #E1251B;
  font-weight: 700;
}

.search-input {
  width: 220px;
  flex-shrink: 0;
}

/* ==================== 帖子卡片 ==================== */
.post-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.post-card {
  display: flex;
  gap: 13px;
  padding: 16px 18px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #F2F3F5;
  transition: box-shadow 0.18s ease, border-color 0.18s ease;
}

.post-card:hover {
  border-color: #FFE0DA;
  box-shadow: 0 6px 18px rgba(225, 37, 27, 0.07);
}

.pc-avatar {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  border-radius: 50%;
  object-fit: cover;
  background: #F5F5F5;
  border: 1px solid #F0F0F0;
}

.pc-main {
  flex: 1;
  min-width: 0;
}

.pc-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pc-board {
  padding: 1px 8px;
  font-size: 11px;
  font-weight: 600;
  border-radius: 9px;
  white-space: nowrap;
}

.bd-chat { color: #059669; background: #ECFDF5; }
.bd-lost { color: #D97706; background: #FFFBEB; }
.bd-study { color: #2563EB; background: #EFF6FF; }
.bd-team { color: #7C3AED; background: #F5F3FF; }
.bd-help { color: #E1251B; background: #FFF1F0; }

.pc-time {
  font-size: 11px;
  color: #B0B4BA;
}

.pc-title {
  margin-top: 6px;
  font-size: 16px;
  font-weight: 700;
  color: #1F2937;
  line-height: 1.45;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pc-title:hover {
  color: #E1251B;
}

.pc-excerpt {
  margin-top: 5px;
  font-size: 13px;
  line-height: 1.65;
  color: #6B7280;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.pc-foot {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 10px;
}

.pc-author {
  font-size: 12px;
  color: #374151;
  font-weight: 600;
}

.pc-stat {
  font-size: 12px;
  color: #9CA3AF;
}

.pc-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.like-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 28px;
  padding: 0 11px;
  font-size: 12px;
  color: #6B7280;
  background: #F7F8FA;
  border: 1px solid transparent;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.16s ease;
}

.like-btn:hover {
  color: #E1251B;
  background: #FFF1F0;
}

.like-btn.liked {
  color: #E1251B;
  font-weight: 700;
  background: #FFF1F0;
  border-color: #FFD9D3;
}

.like-icon {
  font-size: 12px;
}

.del-btn {
  height: 28px;
  padding: 0 11px;
  font-size: 12px;
  color: #9CA3AF;
  background: transparent;
  border: 1px solid #EDEEF0;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.16s ease;
}

.del-btn:hover {
  color: #E1251B;
  border-color: #FFD9D3;
  background: #FFF7F5;
}

.open-btn {
  height: 28px;
  padding: 0 13px;
  font-size: 12px;
  font-weight: 600;
  color: #E1251B;
  background: #FFF1F0;
  border: none;
  border-radius: 14px;
  cursor: pointer;
  transition: background 0.16s ease;
}

.open-btn:hover {
  background: #FFE4E0;
}

/* ==================== 空态与加载 ==================== */
.empty {
  padding: 56px 0;
  text-align: center;
  background: #fff;
  border-radius: 12px;
}

.empty-icon {
  font-size: 34px;
}

.empty-title {
  margin-top: 10px;
  font-size: 15px;
  font-weight: 700;
  color: #374151;
}

.empty-desc {
  margin-top: 6px;
  font-size: 12px;
  color: #9CA3AF;
}

.empty-btn {
  margin-top: 16px;
  height: 34px;
  padding: 0 20px;
  border: none;
  border-radius: 17px;
  background: linear-gradient(135deg, #FF7A45, #E1251B);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.loading {
  padding: 20px 0;
  text-align: center;
  font-size: 13px;
  color: #9CA3AF;
}

.more-row {
  padding: 16px 0 4px;
  text-align: center;
}

.btn-more {
  height: 34px;
  padding: 0 26px;
  font-size: 13px;
  color: #4B5563;
  background: #fff;
  border: 1px solid #E5E7EB;
  border-radius: 17px;
  cursor: pointer;
}

.btn-more:hover:not(:disabled) {
  color: #E1251B;
  border-color: #FFD9D3;
}

/* ==================== 弹窗表单 ==================== */
.form-item {
  margin-bottom: 16px;
}

.form-item label {
  display: block;
  margin-bottom: 7px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.form-item label i {
  color: #E1251B;
  font-style: normal;
}

.board-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.board-chip {
  padding: 6px 14px;
  font-size: 13px;
  color: #4B5563;
  border: 1px solid #E5E7EB;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.16s ease;
}

.board-chip:hover {
  border-color: #FFD9D3;
  color: #E1251B;
}

.board-chip.active {
  color: #E1251B;
  font-weight: 700;
  background: #FFF1F0;
  border-color: #FFB4A8;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 4px;
}

.btn-primary {
  height: 36px;
  padding: 0 22px;
  border: none;
  border-radius: 18px;
  background: linear-gradient(135deg, #FF7A45, #E1251B);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.16s ease;
}

.btn-primary:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.btn-ghost {
  height: 36px;
  padding: 0 20px;
  border: 1px solid #E5E7EB;
  border-radius: 18px;
  background: #fff;
  color: #4B5563;
  font-size: 13px;
  cursor: pointer;
}

.btn-ghost:hover {
  border-color: #D1D5DB;
  color: #374151;
}

/* ==================== 详情弹窗 ==================== */
.dlg-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dlg-title {
  font-size: 16px;
  font-weight: 700;
  color: #1F2937;
}

.detail-title {
  font-size: 20px;
  font-weight: 800;
  line-height: 1.45;
  color: #1F2937;
}

.detail-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 9px;
  margin-top: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid #F2F3F5;
}

.detail-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  object-fit: cover;
  background: #F5F5F5;
}

.detail-author {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.detail-content {
  margin-top: 14px;
  font-size: 14px;
  line-height: 1.85;
  color: #374151;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
}

.like-btn.big {
  height: 34px;
  padding: 0 16px;
  font-size: 13px;
  border-radius: 17px;
}

/* 回复区 */
.comment-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px solid #F2F3F5;
  font-size: 14px;
  font-weight: 700;
  color: #1F2937;
}

.comment-count {
  padding: 0 8px;
  font-size: 11px;
  font-weight: 600;
  color: #E1251B;
  background: #FFF1F0;
  border-radius: 9px;
}

.comment-list {
  margin-top: 12px;
  max-height: 280px;
  overflow-y: auto;
}

.comment-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 11px 0;
  border-bottom: 1px dashed #F0F1F3;
}

.comment-avatar {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 50%;
  object-fit: cover;
  background: #F5F5F5;
}

.comment-main {
  flex: 1;
  min-width: 0;
}

.comment-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.comment-author {
  font-size: 12px;
  font-weight: 600;
  color: #374151;
}

.comment-text {
  margin-top: 4px;
  font-size: 13px;
  line-height: 1.65;
  color: #4B5563;
  white-space: pre-wrap;
  word-break: break-word;
}

.comment-del {
  flex-shrink: 0;
  padding: 2px 8px;
  font-size: 11px;
  color: #B0B4BA;
  background: transparent;
  border: none;
  border-radius: 9px;
  cursor: pointer;
}

.comment-ops {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 2px;
}

.comment-reply {
  padding: 2px 8px;
  font-size: 11px;
  color: #6B7280;
  background: transparent;
  border: none;
  border-radius: 9px;
  cursor: pointer;
}

.comment-reply:hover {
  color: #E1251B;
  background: #FFF7F5;
}

.reply-to {
  color: #E1251B;
  font-weight: 600;
}

.reply-target {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  padding: 6px 10px;
  font-size: 12px;
  color: #B45309;
  background: #FFF7E6;
  border: 1px solid #FDE68A;
  border-radius: 8px;
}

.reply-cancel {
  border: none;
  background: transparent;
  color: #B45309;
  font-size: 12px;
  cursor: pointer;
  line-height: 1;
}

.comment-del:hover {
  color: #E1251B;
  background: #FFF7F5;
}

.comment-empty {
  padding: 22px 0;
  text-align: center;
  font-size: 12px;
  color: #B0B4BA;
}

.comment-editor {
  margin-top: 14px;
}

.editor-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 10px;
}

.editor-hint {
  font-size: 11px;
  color: #B0B4BA;
}

/* ==================== 响应式 ==================== */
@media (max-width: 767px) {
  .forum-hero {
    flex-direction: column;
    align-items: flex-start;
    padding: 18px;
  }

  .hero-right {
    flex-direction: row;
    align-items: center;
    width: 100%;
    justify-content: space-between;
  }

  .forum-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }

  .pc-excerpt {
    -webkit-line-clamp: 2;
  }

  .pc-actions {
    width: 100%;
    margin-left: 0;
    justify-content: flex-end;
  }

  .pc-title {
    white-space: normal;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }
}
</style>
