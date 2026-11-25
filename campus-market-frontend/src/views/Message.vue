<template>
  <div class="message-page">
    <AppSidebar />
    <div class="msg-card">
      <!-- 卡片头部：标题 + 未读角标 -->
      <div class="msg-card-header">
        <div class="msg-card-title-wrap">
          <span class="msg-card-title">消息</span>
          <span class="msg-card-badge" v-if="totalUnread > 0">{{ totalUnread > 99 ? '99+' : totalUnread }}</span>
        </div>
        <div class="msg-card-hint">与买家 / 卖家即时沟通</div>
      </div>

      <div class="msg-card-body">
        <!-- ========== 左侧：会话列表 ========== -->
        <aside class="conv-pane" :class="{ 'is-hidden': currentView === 'chat' }">

      <!-- 搜索用户 -->
      <div class="conv-search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户昵称/学号"
          clearable
          :prefix-icon="Search"
          @input="onSearchInput"
          @clear="clearSearch"
        />
      </div>

      <div class="conversation-list" v-if="!isSearching && conversations.length > 0">
        <div
          class="conversation-item"
          :class="{ active: chatTarget && conv.otherId === chatTarget.id }"
          v-for="conv in conversations"
          :key="conv.otherId"
          @click="openChat(conv)"
        >
          <div class="conv-avatar-wrap">
            <img
              :src="getAvatarUrl(conv.otherAvatar)"
              class="conv-avatar"
              @error="handleAvatarError($event)"
              alt=""
            />
            <span class="unread-badge" v-if="conv.unreadCount > 0">
              {{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}
            </span>
          </div>
          <div class="conv-content">
            <div class="conv-top">
              <span class="conv-name">{{ conv.otherName || '匿名用户' }}</span>
              <span class="conv-time">{{ formatTime(conv.lastTime) }}</span>
            </div>
            <div class="conv-bottom">
              <span class="conv-last-msg">{{ convPreview(conv) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="empty-state" v-if="!isSearching && !loading && conversations.length === 0">
        <el-icon :size="56" color="#ddd"><ChatDotRound /></el-icon>
        <p class="empty-text">暂无会话</p>
        <p class="empty-sub">去逛逛商品，联系卖家吧~</p>
      </div>

      <div class="loading-state" v-if="!isSearching && loading">
        <el-icon :size="28" class="loading-spin"><Loading /></el-icon>
        <p>加载中...</p>
      </div>

      <!-- 搜索结果列表 -->
      <div class="search-result-list" v-if="isSearching">
        <div class="search-loading" v-if="searchLoading">
          <el-icon :size="24" class="loading-spin"><Loading /></el-icon>
          <span>搜索中...</span>
        </div>
        <template v-else>
          <div class="search-result-item" v-for="u in searchResults" :key="u.id">
            <img
              :src="getAvatarUrl(u.avatar)"
              class="conv-avatar"
              @error="handleAvatarError($event)"
              alt=""
            />
            <div class="conv-content">
              <div class="conv-top">
                <span class="conv-name">{{ u.username || '匿名用户' }}</span>
              </div>
              <div class="conv-bottom">
                <span class="conv-last-msg" v-if="u.studentNo">学号：{{ u.studentNo }}</span>
                <span class="conv-last-msg" v-else>点击发消息或进店</span>
              </div>
            </div>
            <div class="search-item-actions">
              <button class="search-chat-btn" @click="openChatWithUser(u)">发消息</button>
              <button class="search-shop-btn" @click="visitUserShop(u)">进店</button>
            </div>
          </div>
          <div class="search-empty" v-if="searchResults.length === 0">
            <el-icon :size="40" color="#ddd"><Search /></el-icon>
            <p>未找到相关用户</p>
          </div>
        </template>
      </div>
    </aside>

    <!-- ========== 右侧：聊天区 ========== -->
    <section class="chat-pane" :class="{ 'is-hidden': currentView === 'list' }">
      <div class="chat-header">
        <div class="chat-back" @click="backToList">
          <el-icon :size="20"><ArrowLeft /></el-icon>
        </div>
        <div class="chat-header-info" v-if="chatTarget">
          <span class="chat-target-name">{{ chatTarget.name || '匿名用户' }}</span>
        </div>
        <div class="chat-header-placeholder"></div>
      </div>

      <!-- 未选择会话（桌面端空态） -->
      <div class="chat-empty-pane" v-if="!chatTarget">
        <el-icon :size="64" color="#e0e0e0"><ChatDotRound /></el-icon>
        <p>选择一个会话开始聊天</p>
      </div>

      <template v-else>
        <!-- 消息列表 -->
        <div class="chat-messages" ref="chatMessagesRef">
          <div class="load-more-row" v-if="chatHasMore" @click="loadMoreHistory">
            <span class="load-more-text">{{ chatLoading ? '加载中...' : '加载更多历史消息' }}</span>
          </div>
          <div
            v-for="msg in chatMessages"
            :key="msg.id"
            class="msg-row"
            :class="{ 'msg-self': msg.senderId === currentUserId, 'msg-other': msg.senderId !== currentUserId }"
          >
            <!-- 对方消息：头像在左 -->
            <template v-if="msg.senderId !== currentUserId">
              <img
                :src="getAvatarUrl(msg.senderAvatar || chatTarget?.avatar)"
                class="msg-avatar"
                @error="handleAvatarError($event)"
                alt=""
              />
              <div class="msg-body">
                <div class="msg-auto" v-if="msg.autoReply">
                  <span class="msg-auto-tag">🤖 自动回复</span>
                </div>
                <div class="msg-bubble bubble-other">
                  <template v-if="msgTypeOf(msg) === 'image'">
                    <img
                      :src="getImageUrl(msg.content)"
                      class="msg-img"
                      @click="previewImage(msg.content)"
                      @error="handleThumbError($event)"
                      alt="图片"
                    />
                  </template>
                  <template v-else-if="msgTypeOf(msg) === 'file'">
                    <a
                      class="msg-file"
                      :href="fileUrl(msg.content)"
                      target="_blank"
                      rel="noopener"
                      :download="msg.fileName || ''"
                    >
                      <el-icon :size="22" class="file-icon"><Document /></el-icon>
                      <div class="file-meta">
                        <div class="file-name">{{ msg.fileName || '文件' }}</div>
                        <div class="file-size">{{ formatFileSize(msg.fileSize) }}</div>
                      </div>
                    </a>
                  </template>
                  <template v-else-if="msgTypeOf(msg) === 'goods'">
                    <div class="msg-goods-card" @click="goGoodsDetail(msg.goodsId)">
                      <SmartImage :src="msg.goodsCover" class="msg-goods-cover" alt="" />
                      <div class="msg-goods-info">
                        <div class="msg-goods-title">{{ msg.goodsTitle || msg.content }}</div>
                        <div class="msg-goods-price" v-if="msg.goodsPrice != null">&yen;{{ msg.goodsPrice }}</div>
                      </div>
                    </div>
                  </template>
                  <template v-else>{{ msg.content }}</template>
                </div>
                <div class="msg-time">{{ formatMessageTime(msg.createTime) }}</div>
              </div>
            </template>
            <!-- 自己的消息：头像在右 -->
            <template v-else>
              <div class="msg-body">
                <div class="msg-auto right" v-if="msg.autoReply">
                  <span class="msg-auto-tag">🤖 自动回复</span>
                </div>
                <div class="msg-bubble bubble-self">
                  <template v-if="msgTypeOf(msg) === 'image'">
                    <img
                      :src="getImageUrl(msg.content)"
                      class="msg-img"
                      @click="previewImage(msg.content)"
                      @error="handleThumbError($event)"
                      alt="图片"
                    />
                  </template>
                  <template v-else-if="msgTypeOf(msg) === 'file'">
                    <a
                      class="msg-file"
                      :href="fileUrl(msg.content)"
                      target="_blank"
                      rel="noopener"
                      :download="msg.fileName || ''"
                    >
                      <el-icon :size="22" class="file-icon"><Document /></el-icon>
                      <div class="file-meta">
                        <div class="file-name">{{ msg.fileName || '文件' }}</div>
                        <div class="file-size">{{ formatFileSize(msg.fileSize) }}</div>
                      </div>
                    </a>
                  </template>
                  <template v-else-if="msgTypeOf(msg) === 'goods'">
                    <div class="msg-goods-card" @click="goGoodsDetail(msg.goodsId)">
                      <SmartImage :src="msg.goodsCover" class="msg-goods-cover" alt="" />
                      <div class="msg-goods-info">
                        <div class="msg-goods-title">{{ msg.goodsTitle || msg.content }}</div>
                        <div class="msg-goods-price" v-if="msg.goodsPrice != null">&yen;{{ msg.goodsPrice }}</div>
                      </div>
                    </div>
                  </template>
                  <template v-else>{{ msg.content }}</template>
                </div>
                <div class="msg-time msg-time-right">{{ formatMessageTime(msg.createTime) }}</div>
              </div>
              <img
                :src="getAvatarUrl(myAvatar)"
                class="msg-avatar"
                @error="handleAvatarError($event)"
                alt=""
              />
            </template>
          </div>

          <div class="chat-empty" v-if="!chatLoading && chatMessages.length === 0">
            <p>暂无消息，发送第一条消息吧~</p>
          </div>
        </div>

        <!-- 底部输入工具栏 -->
        <div class="chat-input-bar">
          <!-- 表情面板 -->
          <div class="emoji-panel" v-if="showEmoji">
            <span
              v-for="(e, i) in emojiList"
              :key="i"
              class="emoji-item"
              @click="insertEmoji(e)"
            >{{ e }}</span>
          </div>

          <!-- 入口商品快捷发送条（不锁定会话，仅一次性提示，可发送/可关闭） -->
          <div class="quick-send-bar" v-if="quickSendGoods">
            <SmartImage :src="quickSendGoods.cover" class="quick-send-cover" alt="" />
            <div class="quick-send-info">
              <div class="quick-send-label">发送商品给卖家</div>
              <div class="quick-send-title" :title="quickSendGoods.title">{{ quickSendGoods.title }}</div>
            </div>
            <div class="quick-send-price" v-if="quickSendGoods.price">&yen;{{ quickSendGoods.price }}</div>
            <button type="button" class="quick-send-btn" @click="sendQuickGoods">发送</button>
            <button type="button" class="quick-send-close" @click="quickSendGoods = null" title="关闭">&times;</button>
          </div>

          <div class="input-wrapper">
            <div class="tool-btns">
              <button
                type="button"
                class="tool-btn"
                :class="{ active: showEmoji }"
                title="表情"
                @click="toggleEmoji"
              >
                <span class="emoji-trigger-icon">😊</span>
              </button>
              <button type="button" class="tool-btn" title="发送图片" @click="triggerImage">
                <el-icon :size="22"><Picture /></el-icon>
              </button>
              <button type="button" class="tool-btn" title="发送文件" @click="triggerFile">
                <el-icon :size="22"><FolderOpened /></el-icon>
              </button>
              <button type="button" class="tool-btn" title="发送商品" @click="openGoodsPicker">
                <el-icon :size="22"><Goods /></el-icon>
              </button>
            </div>
            <textarea
              ref="textareaRef"
              v-model="messageInput"
              class="chat-input"
              rows="1"
              placeholder="输入消息，Enter 发送 / Shift+Enter 换行"
              @keydown="onInputKeydown"
              @input="autoResize"
              :disabled="sending"
            ></textarea>
            <button
              class="send-btn"
              :class="{ active: canSend }"
              :disabled="!canSend || sending"
              @click="handleSend"
            >
              {{ sending ? '发送中...' : '发送' }}
            </button>
          </div>

          <input
            ref="imageInputRef"
            type="file"
            accept="image/*"
            style="display: none"
            @change="onPickImage"
          />
          <input
            ref="fileInputRef"
            type="file"
            accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.csv,.zip,.rar,.7z,.md"
            style="display: none"
            @change="onPickFile"
          />
        </div>
      </template>
    <!-- 发送商品：选择本店（当前聊天对象）的在售商品 -->
    <Teleport to="body">
      <el-dialog v-model="showGoodsPicker" title="选择要发送的商品" width="460px" append-to-body class="goods-picker-dialog">
        <div class="goods-picker" v-loading="goodsPickerLoading">
          <div class="picker-mode-switch">
            <span :class="{ active: pickerMode === 'browsed' }" @click="switchPickerMode('browsed')">我浏览过的</span>
            <span :class="{ active: pickerMode === 'all' }" @click="switchPickerMode('all')">全部商品</span>
          </div>
          <el-input
            v-show="pickerMode === 'all'"
            v-model="goodsPickerKeyword"
            placeholder="搜索本店商品"
            clearable
            @keyup.enter="loadGoodsPicker(1)"
            @clear="loadGoodsPicker(1)"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <div class="goods-picker-list">
            <template v-if="goodsPickerList.length > 0">
              <div
                v-for="g in goodsPickerList"
                :key="g.id"
                class="goods-picker-item"
                @click="pickGoods(g)"
              >
                <SmartImage :src="g.coverImage" class="picker-goods-cover" alt="" />
                <div class="picker-goods-info">
                  <div class="picker-goods-title" :title="g.title">{{ g.title }}</div>
                  <div class="picker-goods-price">&yen;{{ g.price }}</div>
                </div>
              </div>
            </template>
            <div class="goods-picker-empty" v-else-if="!goodsPickerLoading">
              {{ pickerMode === 'browsed' ? '还没浏览过该店商品，可切换到“全部商品”' : '该卖家暂无在售商品' }}
            </div>
          </div>
        </div>
      </el-dialog>
    </Teleport>
    </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import AppSidebar from '../components/AppSidebar.vue'
import { decreaseUnread, refreshUnread } from '../store/unread'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Loading, ArrowLeft, Picture, FolderOpened, Document, Search } from '@element-plus/icons-vue'
import { getConversationList, getChatRecord, sendMessage } from '../api/message'
import { getGoodsList } from '../api/goods'
import { getShopBrowsedGoods } from '../api/browse'
import SmartImage from '../components/SmartImage.vue'
import { uploadImage, uploadFile } from '../api/upload'
import { searchUsers } from '../api/user'
import { baseURL } from '../config'
import { DEFAULT_AVATAR, resolveAvatar } from '../utils/avatar'

const route = useRoute()
const router = useRouter()

// 当前登录用户
const currentUserId = ref(null)
const myAvatar = ref('')

// 视图切换: 'list' | 'chat'（桌面端两栏始终可见，该值仅控制移动端显隐）
const currentView = ref('list')

// 会话列表
const conversations = ref([])
const loading = ref(false)
// 顶部未读总数
const totalUnread = computed(() =>
  conversations.value.reduce((sum, conv) => sum + (Number(conv.unreadCount) || 0), 0)
)

// 聊天视图数据
const chatTarget = ref(null)
const chatMessages = ref([])
const chatLoading = ref(false)
const messageInput = ref('')
const sending = ref(false)
const chatMessagesRef = ref(null)

// 搜索用户（左侧面板内切换：会话列表 ↔ 搜索结果）
const searchKeyword = ref('')
const searchResults = ref([])
const isSearching = ref(false)   // true 时左侧显示搜索结果而非会话列表
const searchLoading = ref(false)
let searchTimer = null

// 输入相关
const textareaRef = ref(null)
const imageInputRef = ref(null)
const fileInputRef = ref(null)
const showEmoji = ref(false)
const emojiList = [
  '😀', '😂', '🥰', '😎', '🤔', '😴', '😭', '😡',
  '👍', '👎', '🙏', '👏', '💪', '🎉', '❤️', '🌹',
  '🔥', '✨', '🎁', '📚', '💻', '🏀', '☕', '🍔',
  '😄', '😍', '😜', '🙂', '😢', '😤', '🤝', '👌'
]
const canSend = computed(() => !!messageInput.value.trim() && !sending.value)

// 入口商品快捷发送条（不锁定会话，仅一次性提示，发完/关闭即消失）
const quickSendGoods = ref(null)
// 发送商品选择器（本店 = 当前聊天对象的在售商品）
const showGoodsPicker = ref(false)
const goodsPickerLoading = ref(false)
const goodsPickerKeyword = ref('')
const goodsPickerList = ref([])
const pickerMode = ref('browsed')   // 'browsed' 我浏览过的 | 'all' 全部商品

// 聊天记录分页
const chatPageSize = 20
const chatPageNum = ref(1)
const chatTotal = ref(0)
const chatHasMore = computed(() => chatMessages.value.length < chatTotal.value)

// 轮询
let pollTimer = null
let lastConvSignature = ''   // 轮询时对比会话列表签名，数据未变则跳过赋值，避免每 5s 全量重渲染
const POLL_INTERVAL = 5000

// 工具函数：消息类型兜底（旧数据无 msgType 按文本处理）
const msgTypeOf = (msg) => (msg && msg.msgType) ? msg.msgType : 'text'

// 会话列表最后一条消息预览文案
const convPreview = (conv) => {
  const t = (conv && conv.msgType) ? conv.msgType : 'text'
  if (t === 'image') return '[图片]'
  if (t === 'file') return '[文件]'
  if (t === 'goods') return '[商品]'
  return conv.lastContent || ''
}

const fileUrl = (path) => {
  if (!path) return ''
  return path.startsWith('http') ? path : baseURL + path
}

const formatFileSize = (b) => {
  if (b === null || b === undefined || isNaN(b)) return ''
  if (b < 1024) return b + ' B'
  if (b < 1024 * 1024) return (b / 1024).toFixed(1) + ' KB'
  return (b / 1024 / 1024).toFixed(1) + ' MB'
}

const previewImage = (path) => {
  const url = getImageUrl(path)
  if (url) window.open(url, '_blank')
}

// 图片URL处理
const getImageUrl = (path) => {
  if (!path) return 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0iI2ZmZjVmNSI+PHBhdGggZD0iTTEyIDEyYzIuMjEgMCA0LTEuNzkgNC00czItNCA0LTQtMS43OS00LTQtNC00LTEuNzkgNC00IDRzMS43OSA0IDQgNHptMCAxNmMzLjMxIDAgNi0yLjY5IDYtNnYtMWMwLTMuMzEtMi42OS02LTYtNnMtNiAyLjY5LTYgNnYxYzAgMy4zMSAyLjY5IDYgNiA2eiIvPjwvc3ZnPg=='
  if (path.startsWith('http')) return path
  return baseURL + path
}

const getAvatarUrl = (path) => resolveAvatar(path)

const handleAvatarError = (e) => {
  e.target.src = DEFAULT_AVATAR
}

const handleThumbError = (e) => {
  e.target.style.display = 'none'
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterday = new Date(today.getTime() - 86400000)
  const msgDate = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  const time = `${h}:${min}`
  if (msgDate.getTime() === today.getTime()) return time
  if (msgDate.getTime() === yesterday.getTime()) return '昨天'
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${m}-${d}`
}

const formatMessageTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const msgDate = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  if (msgDate.getTime() === today.getTime()) return h + ':' + min
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${h}:${min}`
}

const loadConversations = async (silent = false) => {
  if (!silent) loading.value = true
  try {
    const res = await getConversationList(currentUserId.value)
    const list = (res && Array.isArray(res)) ? res : ((res && res.list) ? res.list : [])
    // 轮询(silent)场景：只有会话列表真的变化时才赋值，避免每 5s 触发整列表重渲染
    const sig = list.map(c => `${c.otherId}|${c.lastTime}|${c.unreadCount}|${c.lastContent}`).join(';;')
    if (silent && sig === lastConvSignature) return
    lastConvSignature = sig
    conversations.value = list
  } catch (err) {
    console.error('获取会话列表失败', err)
    // 轮询时网络抖动不应清空已有列表；仅首次加载失败才置空
    if (!silent) {
      conversations.value = []
      lastConvSignature = ''
    }
  } finally {
    if (!silent) loading.value = false
  }
}

const openChat = (conv) => {
  // 立即本地扣减未读数：底部红点、首页铃铛同步消失，随后再和服务端校准
  decreaseUnread(Number(conv.unreadCount) || 0)
  conv.unreadCount = 0
  // 打开的是普通会话：chatTarget 只装用户信息，不携带/锁定任何商品
  chatTarget.value = {
    id: conv.otherId,
    name: conv.otherName || '匿名用户',
    avatar: conv.otherAvatar
  }
  quickSendGoods.value = null   // 切换会话时清除入口快捷条，避免残留误导
  currentView.value = 'chat'
  showEmoji.value = false
  loadChatHistory().then(() => refreshUnread())
}

// ========== 搜索用户 ==========
const onSearchInput = () => {
  if (searchTimer) clearTimeout(searchTimer)
  const kw = searchKeyword.value.trim()
  if (!kw) { clearSearch(); return }
  searchTimer = setTimeout(() => doSearch(), 350)   // 防抖：停止输入 350ms 后搜索
}

const clearSearch = () => {
  if (searchTimer) { clearTimeout(searchTimer); searchTimer = null }
  searchKeyword.value = ''
  searchResults.value = []
  isSearching.value = false
  searchLoading.value = false
}

const doSearch = async () => {
  const kw = searchKeyword.value.trim()
  if (!kw) { clearSearch(); return }
  isSearching.value = true
  searchLoading.value = true
  try {
    const res = await searchUsers(kw, { pageNum: 1, pageSize: 20 })
    searchResults.value = (res && res.list) ? res.list : (Array.isArray(res) ? res : [])
  } catch (err) {
    console.error('搜索用户失败', err)
    searchResults.value = []
  } finally {
    searchLoading.value = false
  }
}

// 搜索结果：发消息（直接在当前消息页打开与该用户的会话）
const openChatWithUser = (u) => {
  if (!u || !u.id) return
  if (u.id === currentUserId.value) {
    ElMessage.info('不能给自己发消息')
    return
  }
  chatTarget.value = { id: u.id, name: u.username || '匿名用户', avatar: u.avatar || '' }
  quickSendGoods.value = null
  currentView.value = 'chat'
  clearSearch()
  loadChatHistory()
}

// 搜索结果：进店（跳转到该用户的公开主页）
const visitUserShop = (u) => {
  if (!u || !u.id) return
  router.push(`/profile?userId=${u.id}`)
}

const loadChatHistory = async (reset = true) => {
  if (!chatTarget.value || !currentUserId.value) return
  const page = reset ? 1 : chatPageNum.value + 1
  chatLoading.value = true
  try {
    const res = await getChatRecord(currentUserId.value, chatTarget.value.id, {
      pageNum: page,
      pageSize: chatPageSize
    })
    const list = (res && res.list) ? res.list.slice().reverse() : (Array.isArray(res) ? res : [])
    chatTotal.value = (res && typeof res.total === 'number') ? res.total : list.length
    if (reset) {
      chatMessages.value = list
      chatPageNum.value = 1
      scrollToBottom()
    } else {
      chatMessages.value = [...list, ...chatMessages.value]
      chatPageNum.value = page
    }
  } catch (err) {
    console.error('获取聊天记录失败', err)
  } finally {
    chatLoading.value = false
  }
}

const loadMoreHistory = () => {
  if (chatLoading.value || !chatHasMore.value) return
  loadChatHistory(false)
}

const isNearBottom = () => {
  const el = chatMessagesRef.value
  if (!el) return true
  return el.scrollTop + el.clientHeight >= el.scrollHeight - 60
}

const refreshChat = async () => {
  if (!chatTarget.value || !currentUserId.value || chatLoading.value) return
  try {
    const res = await getChatRecord(currentUserId.value, chatTarget.value.id, {
      pageNum: 1,
      pageSize: chatPageSize
    })
    const latest = (res && res.list) ? res.list.slice().reverse() : []
    chatTotal.value = (res && typeof res.total === 'number') ? res.total : chatTotal.value
    const existingIds = new Set(chatMessages.value.map(m => m.id))
    const fresh = latest.filter(m => !existingIds.has(m.id))
    if (fresh.length > 0) {
      chatMessages.value = [...chatMessages.value, ...fresh]
      if (isNearBottom()) scrollToBottom()
    }
  } catch (err) {
    console.error('刷新聊天失败', err)
  }
}

const startPolling = () => {
  stopPolling()
  pollTimer = setInterval(() => {
    loadConversations(true)
    if (currentView.value === 'chat') {
      refreshChat()
    }
  }, POLL_INTERVAL)
}

// ==================== 实时推送（SSE） ====================
// 收到新消息：刷新会话列表；若正与该用户聊天，增量拉取新消息
const onRealtimeMessage = (e) => {
  const msg = e.detail || {}
  loadConversations(true)
  refreshUnread()
  if (currentView.value === 'chat' && chatTarget.value &&
      (msg.senderId === chatTarget.value.id || msg.receiverId === chatTarget.value.id)) {
    refreshChat()
  }
}

// 对方已读：刷新当前会话的已读状态
const onRealtimeRead = () => {
  if (currentView.value === 'chat') {
    refreshChat()
  }
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

// 页面不可见时暂停轮询，切回可见时立即补刷一次，减少不必要的请求与重渲染
const onVisibilityChange = () => {
  if (document.hidden) {
    stopPolling()
  } else {
    loadConversations(true)
    startPolling()
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatMessagesRef.value) {
      chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
    }
  })
}

// 文本自适应高度
const autoResize = () => {
  const ta = textareaRef.value
  if (!ta) return
  ta.style.height = 'auto'
  ta.style.height = Math.min(ta.scrollHeight, 120) + 'px'
}

// 回车发送（中文输入法组合中不触发），Shift+Enter 换行
const onInputKeydown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
    e.preventDefault()
    handleSend()
  }
}

// 表情面板
const toggleEmoji = () => {
  showEmoji.value = !showEmoji.value
}
const insertEmoji = (emoji) => {
  const ta = textareaRef.value
  if (!ta) {
    messageInput.value += emoji
    return
  }
  const start = ta.selectionStart ?? messageInput.value.length
  const end = ta.selectionEnd ?? messageInput.value.length
  messageInput.value = messageInput.value.slice(0, start) + emoji + messageInput.value.slice(end)
  nextTick(() => {
    ta.focus()
    const pos = start + emoji.length
    ta.setSelectionRange(pos, pos)
    autoResize()
  })
}

// 点击外部关闭表情面板
const onDocClick = (e) => {
  if (!showEmoji.value) return
  const t = e.target
  if (!(t.closest && (t.closest('.emoji-panel') || t.closest('.tool-btn')))) {
    showEmoji.value = false
  }
}

// 图片 / 文件 选择与发送
const triggerImage = () => {
  showEmoji.value = false
  imageInputRef.value && imageInputRef.value.click()
}
const triggerFile = () => {
  showEmoji.value = false
  fileInputRef.value && fileInputRef.value.click()
}
const onPickImage = (e) => {
  const f = e.target.files && e.target.files[0]
  e.target.value = ''
  if (f) sendImage(f)
}
const onPickFile = (e) => {
  const f = e.target.files && e.target.files[0]
  e.target.value = ''
  if (f) sendFile(f)
}

const afterSendOk = () => {
  loadChatHistory()
  loadConversations()
}

const sendImage = async (file) => {
  if (!chatTarget.value || sending.value) return
  sending.value = true
  try {
    const path = await uploadImage(file)
    await sendMessage({ receiverId: chatTarget.value.id, content: path, msgType: 'image' })
    afterSendOk()
  } catch (err) {
    console.error('发送图片失败', err)
  } finally {
    sending.value = false
  }
}

const sendFile = async (file) => {
  if (!chatTarget.value || sending.value) return
  sending.value = true
  try {
    const path = await uploadFile(file)
    await sendMessage({
      receiverId: chatTarget.value.id,
      content: path,
      msgType: 'file',
      fileName: file.name,
      fileSize: file.size
    })
    afterSendOk()
  } catch (err) {
    console.error('发送文件失败', err)
  } finally {
    sending.value = false
  }
}

// 跳转商品详情
const goGoodsDetail = (id) => {
  if (id) router.push(`/goods/${id}`)
}

// 发送商品卡片消息（content 存商品标题作兜底文案，goodsId 关联商品供后端 JOIN 出卡片信息）
const sendGoodsCard = async (goods) => {
  if (!chatTarget.value || sending.value || !goods || !goods.id) return
  sending.value = true
  try {
    await sendMessage({
      receiverId: chatTarget.value.id,
      content: goods.title || '商品',
      msgType: 'goods',
      goodsId: goods.id
    })
    afterSendOk()
  } catch (err) {
    console.error('发送商品失败', err)
  } finally {
    sending.value = false
  }
}

// 点击入口快捷条上的“发送”
const sendQuickGoods = () => {
  if (!quickSendGoods.value) return
  const g = quickSendGoods.value
  quickSendGoods.value = null   // 发完即消失，绝不锁定会话
  sendGoodsCard(g)
}

// 打开“发送商品”选择器：本店 = 当前聊天对象的在售商品
const openGoodsPicker = async () => {
  showEmoji.value = false
  if (!chatTarget.value) return
  pickerMode.value = 'browsed'   // 默认展示“我浏览过的”
  goodsPickerKeyword.value = ''
  showGoodsPicker.value = true
  await loadGoodsPicker(1)
}

// 切换选择器数据源：我浏览过的 / 全部商品
const switchPickerMode = async (mode) => {
  if (pickerMode.value === mode) return
  pickerMode.value = mode
  goodsPickerKeyword.value = ''
  await loadGoodsPicker(1)
}

const loadGoodsPicker = async (pageNum = 1) => {
  if (!chatTarget.value) return
  goodsPickerLoading.value = true
  try {
    if (pickerMode.value === 'browsed') {
      // 我浏览过的该店在售商品（后端按浏览时间倒序，不支持关键词）
      const res = await getShopBrowsedGoods(chatTarget.value.id)
      goodsPickerList.value = Array.isArray(res) ? res : []
    } else {
      const res = await getGoodsList({
        sellerId: chatTarget.value.id,
        keyword: goodsPickerKeyword.value.trim() || undefined,
        pageNum,
        pageSize: 20,
        sort: 'new'
      })
      goodsPickerList.value = (res && res.list) ? res.list : (Array.isArray(res) ? res : [])
    }
  } catch (err) {
    console.error('加载商品失败', err)
    goodsPickerList.value = []
  } finally {
    goodsPickerLoading.value = false
  }
}

// 选中商品后发送并关闭弹窗
const pickGoods = (goods) => {
  showGoodsPicker.value = false
  sendGoodsCard(goods)
}

// 发送文本消息
const handleSend = async () => {
  const content = messageInput.value.trim()
  if (!content || sending.value || !chatTarget.value) return
  sending.value = true
  try {
    const params = { receiverId: chatTarget.value.id, content, msgType: 'text' }
    // 不再自动锁定商品：普通文本消息不附带 goodsId，仅“发送商品”卡片才带
    await sendMessage(params)
    messageInput.value = ''
    nextTick(autoResize)
    afterSendOk()
  } catch (err) {
    console.error('发送消息失败', err)
  } finally {
    sending.value = false
  }
}

const backToList = () => {
  currentView.value = 'list'
  chatMessages.value = []
  chatTarget.value = null
  quickSendGoods.value = null
  messageInput.value = ''
  showEmoji.value = false
  loadConversations()
}

onMounted(async () => {
  // 仅消息页：锁定根容器到视口，消除下滑滚轮时底部露出的灰色带
  document.documentElement.classList.add('view-message')
  const userStr = localStorage.getItem('user')
  if (!userStr) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    const user = JSON.parse(userStr)
    currentUserId.value = user.id
    myAvatar.value = user.avatar || ''

    // 始终先加载左侧会话列表（避免带参进入时左侧要等轮询才出现），
    // 并 await 其完成，以便下面从列表里取该卖家的真实昵称/头像
    await loadConversations()
    const sellerId = route.query.sellerId
    if (sellerId) {
      // 联系卖家：只打开与该卖家的普通会话，不锁定任何商品
      const conv = conversations.value.find(c => c.otherId === Number(sellerId))
      if (conv) {
        decreaseUnread(Number(conv.unreadCount) || 0)
        conv.unreadCount = 0
      }
      chatTarget.value = {
        id: Number(sellerId),
        name: conv ? (conv.otherName || '卖家') : (route.query.sellerName || '卖家'),
        avatar: conv ? conv.otherAvatar : (route.query.sellerAvatar || '')
      }
      currentView.value = 'chat'
      loadChatHistory()
    }
    // 从商品详情进来时，给一个“快捷发送该商品”的提示条（不锁定，发完/关闭即消失）
    if (route.query.goodsId && route.query.goodsTitle) {
      quickSendGoods.value = {
        id: Number(route.query.goodsId),
        title: route.query.goodsTitle,
        cover: route.query.goodsCover || '',
        price: route.query.goodsPrice || ''
      }
    }
    startPolling()
    // 实时推送：新消息即时刷新，轮询保留作为断线兜底
    window.addEventListener('rt:message', onRealtimeMessage)
    window.addEventListener('rt:read', onRealtimeRead)
    document.addEventListener('click', onDocClick)
    document.addEventListener('visibilitychange', onVisibilityChange)
  } catch (e) {
    console.error('解析用户信息失败', e)
    ElMessage.error('用户信息异常')
    router.push('/login')
  }
})

onUnmounted(() => {
  stopPolling()
  window.removeEventListener('rt:message', onRealtimeMessage)
  window.removeEventListener('rt:read', onRealtimeRead)
  document.removeEventListener('click', onDocClick)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  document.documentElement.classList.remove('view-message')
})
</script>

<style>
/* 消息页专属：把 html/body/#app 锁定到视口高度并隐藏外层滚动。
   原因：全局 #app 有 padding-bottom:60px（为普通滚动页预留 tabbar 空间），
   而 .message-page 自身又用 calc(100vh - 60px) 减了一次 60px，
   双重预留导致 #app 撑高到 100vh + 60px，下滑滚轮就露出底部 60px 灰带。
   仅在 <html class="view-message"> 时生效，离开消息页即移除，不影响其他页面滚动。 */
html.view-message,
html.view-message body {
  height: 100%;
  overflow: hidden;
}

html.view-message #app {
  height: 100vh;
  min-height: 0;
  padding-bottom: 0;
  overflow: hidden;
  box-sizing: border-box;
}
</style>

<style scoped>
.message-page {
  display: flex;
  justify-content: center;
  height: calc(100vh - 60px);
  padding: 18px 20px;
  overflow: hidden;
  background-color: #fff;
  background-image: linear-gradient(
    180deg,
    #FF0F23 0px,
    #FF4444 72px,
    #FF9A9A 150px,
    #FFDADA 226px,
    #FFFFFF 300px
  );
  background-repeat: no-repeat;
}

/* ========== 居中白色大卡片 ========== */
/* 宽屏 / 中屏：为左侧导航让出空间 */
@media (min-width: 1100px) {
  .message-page {
    padding-left: 360px;
  }
}

@media (min-width: 768px) and (max-width: 1099px) {
  .message-page {
    padding-left: 72px;
  }
}

.msg-card {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 1120px;
  height: 100%;
  background: #fff;
  border: 1px solid #DCDCDC;
  border-radius: 14px;
  box-shadow: 0 10px 34px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.msg-card-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 54px;
  padding: 0 18px;
  border-bottom: 1px solid #f2f2f2;
}

.msg-card-title-wrap {
  display: flex;
  align-items: center;
}

.msg-card-title {
  font-size: 17px;
  font-weight: 700;
  color: #1f1f1f;
}

.msg-card-badge {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  margin-left: 7px;
  border-radius: 9px;
  background: #F5453D;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.msg-card-hint {
  font-size: 12px;
  color: #bbb;
}

.msg-card-body {
  flex: 1;
  min-height: 0;
  display: flex;
}

/* ========== 左侧会话列表 ========== */
.conv-pane {
  width: 300px;
  flex-shrink: 0;
  height: 100%;
  background: #fff;
  border-right: 1px solid #E8E8E8;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}

.conversation-item:hover {
  background: #f5f5f5;
}

.conversation-item.active {
  background: #f5f6f8;
}

.conv-avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.conv-avatar {
  width: 44px;
  height: 44px;
  border-radius: 4px;
  object-fit: cover;
  background: #f5f5f5;
}

.unread-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 10px;
  background: #f55;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.conv-content {
  flex: 1;
  min-width: 0;
}

.conv-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.conv-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-time {
  font-size: 11px;
  color: #bbb;
  flex-shrink: 0;
}

.conv-bottom {
  display: flex;
  align-items: center;
}

.conv-last-msg {
  flex: 1;
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.empty-state,
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #ccc;
}

.empty-text {
  font-size: 15px;
  color: #999;
  margin-top: 14px;
  margin-bottom: 6px;
}

.empty-sub {
  font-size: 12px;
  color: #ccc;
}

.loading-state p {
  margin-top: 10px;
  font-size: 13px;
}

.loading-spin {
  animation: spin 1s linear infinite;
  color: #E1251B;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ========== 右侧聊天区 ========== */
.chat-pane {
  flex: 1;
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  overflow: hidden;
}

.chat-header {
  position: relative;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  padding: 0 12px;
  background: #fff;
  border-bottom: 1px solid #E8E8E8;
  flex-shrink: 0;
}

.chat-back {
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

.chat-back:hover {
  background: #f5f5f5;
}

.chat-header-info {
  flex: 1;
  text-align: left;
  padding-left: 4px;
  min-width: 0;
}

.chat-target-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-target-goods {
  font-size: 11px;
  color: #999;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 1px;
}

.chat-header-placeholder {
  width: 36px;
  flex-shrink: 0;
}

/* 未选会话空态 */
.chat-empty-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #ccc;
  gap: 12px;
}

.chat-empty-pane p {
  font-size: 14px;
}

/* 商品引用卡片 */
.goods-refer-card {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 8px 12px;
  padding: 10px 12px;
  background: #fff;
  border-radius: 6px;
  flex-shrink: 0;
}

.refer-goods-img {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  object-fit: cover;
  background: #f5f5f5;
  flex-shrink: 0;
}

.refer-goods-info {
  flex: 1;
  min-width: 0;
}

.refer-goods-title {
  font-size: 13px;
  color: #333;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.refer-goods-price {
  font-size: 14px;
  font-weight: 700;
  color: #E1251B;
  margin-top: 2px;
}

/* 消息列表 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 14px 20px;
  -webkit-overflow-scrolling: touch;
}

.load-more-row {
  display: flex;
  justify-content: center;
  padding: 8px 0 14px;
  cursor: pointer;
}

.load-more-text {
  font-size: 12px;
  color: #E1251B;
  padding: 4px 14px;
  border-radius: 12px;
  background: rgba(225, 37, 27, 0.08);
}

.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 14px;
}

/* 自己的消息：气泡在左、头像在最右 */
.msg-self {
  justify-content: flex-end;
}

.msg-avatar {
  width: 38px;
  height: 38px;
  border-radius: 4px;
  object-fit: cover;
  background: #e0e0e0;
  flex-shrink: 0;
}

.msg-body {
  max-width: 62%;
  min-width: 0;
}

.msg-bubble {
  position: relative;
  padding: 8px 12px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
  border-radius: 8px;
  max-width: 100%;
}

.bubble-other {
  background: #f6f7f9;
  color: #333;
}

.bubble-other::after {
  content: '';
  position: absolute;
  left: -6px;
  top: 12px;
  border: 6px solid transparent;
  border-right-color: #f6f7f9;
}

.bubble-self {
  background: #FFE94A;
  color: #1a1a1a;
}

.bubble-self::after {
  content: '';
  position: absolute;
  right: -6px;
  top: 12px;
  border: 6px solid transparent;
  border-left-color: #FFE94A;
}

.msg-img {
  display: block;
  max-width: 200px;
  max-height: 200px;
  border-radius: 4px;
  cursor: pointer;
}

.msg-file {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px;
  min-width: 180px;
  text-decoration: none;
  color: inherit;
  border-radius: 4px;
}

.msg-file:hover {
  background: rgba(0, 0, 0, 0.04);
}

.file-icon {
  color: #E1251B;
  flex-shrink: 0;
}

.file-meta {
  min-width: 0;
}

.file-name {
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 180px;
}

.file-size {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}

.msg-time {
  font-size: 11px;
  color: #bbb;
  margin-top: 4px;
  padding-left: 2px;
}

/* 机器人自动回复标识 */
.msg-auto {
  margin-bottom: 5px;
}

.msg-auto.right {
  text-align: right;
}

.msg-auto-tag {
  display: inline-block;
  padding: 1px 8px;
  font-size: 10px;
  line-height: 16px;
  color: #B45309;
  background: #FFF7E6;
  border: 1px solid #FDE68A;
  border-radius: 8px;
}

.msg-time-right {
  text-align: right;
  padding-right: 2px;
}

.chat-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 160px;
}

.chat-empty p {
  font-size: 13px;
  color: #ccc;
}

/* ========== 底部输入工具栏 ========== */
.chat-input-bar {
  position: relative;
  z-index: 20;
  background: #fff;
  border-top: 1px solid #e8e8e8;
  padding: 8px 12px;
  padding-bottom: calc(8px + env(safe-area-inset-bottom, 0px));
  flex-shrink: 0;
}

.emoji-panel {
  position: absolute;
  bottom: calc(100% + 4px);
  left: 12px;
  width: 280px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  padding: 8px;
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 2px;
  z-index: 200;
}

.emoji-item {
  font-size: 22px;
  text-align: center;
  padding: 4px 0;
  cursor: pointer;
  border-radius: 4px;
  line-height: 1;
}

.emoji-item:hover {
  background: #f0f0f0;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 10px;
}

.tool-btns {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
  padding-bottom: 2px;
}

.tool-btn {
  width: 34px;
  height: 34px;
  border: none;
  background: transparent;
  color: #555;
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s, color 0.15s;
}

.tool-btn:hover {
  background: #f5f5f5;
  color: #E1251B;
}

.tool-btn.active {
  color: #E1251B;
}

.emoji-trigger-icon {
  font-size: 22px;
  line-height: 1;
}

.chat-input {
  flex: 1;
  min-height: 36px;
  max-height: 120px;
  resize: none;
  border: 1px solid #DCDCDC;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 14px;
  line-height: 1.5;
  color: #333;
  background: #fafbfc;
  outline: none;
  font-family: inherit;
  transition: border-color 0.2s, background 0.2s;
}

.chat-input:focus {
  border-color: #FFD400;
  background: #fff;
}

.chat-input::placeholder {
  color: #bbb;
}

.send-btn {
  flex-shrink: 0;
  height: 36px;
  padding: 0 22px;
  border: none;
  border-radius: 8px;
  background: #FFF7CC;
  color: #b8a63e;
  font-size: 14px;
  font-weight: 600;
  cursor: not-allowed;
  font-family: inherit;
  transition: all 0.2s;
}

.send-btn.active {
  background: #FFE60F;
  color: #2b2b2b;
  cursor: pointer;
}

.send-btn.active:hover {
  background: #FFDC00;
}

.send-btn.active:active {
  transform: scale(0.96);
}

/* ========== 移动端：单列切换 ========== */
@media (max-width: 560px) {
  .message-page {
    padding: 0;
  }
  .msg-card {
    border-radius: 0;
    box-shadow: none;
  }
  .msg-card-header {
    height: 50px;
    padding: 0 14px;
  }
  .msg-card-hint {
    display: none;
  }
  .conv-pane {
    width: 100%;
  }
  .conv-pane.is-hidden,
  .chat-pane.is-hidden {
    display: none;
  }
}

/* 桌面端：隐藏返回箭头，让标题靠左展示 */
@media (min-width: 561px) {
  .chat-back,
  .chat-header-placeholder {
    visibility: hidden;
  }
}

/* ========== 入口商品快捷发送条（不锁定，发完/关闭即消失） ========== */
.quick-send-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  margin: 0 12px 8px;
  background: #f7f8fa;
  border: 1px solid #eaecef;
  border-radius: 8px;
}
.quick-send-cover {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
}
.quick-send-info {
  flex: 1;
  min-width: 0;
}
.quick-send-label {
  font-size: 11px;
  color: #999;
}
.quick-send-title {
  font-size: 13px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.quick-send-price {
  color: #e1251b;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}
.quick-send-btn {
  border: none;
  background: #E1251B;
  color: #fff;
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 14px;
  cursor: pointer;
  flex-shrink: 0;
}
.quick-send-close {
  border: none;
  background: transparent;
  color: #bbb;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  padding: 0 2px;
  flex-shrink: 0;
}
.quick-send-close:hover {
  color: #666;
}

/* ========== 聊天内商品卡片消息 ========== */
.msg-goods-card {
  width: 220px;
  display: flex;
  gap: 10px;
  padding: 10px;
  background: #fff;
  border: 1px solid #eaecef;
  border-radius: 8px;
  cursor: pointer;
  transition: box-shadow 0.15s;
}
.msg-goods-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}
.msg-goods-cover {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
}
.msg-goods-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.msg-goods-title {
  font-size: 13px;
  color: #333;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.msg-goods-price {
  color: #e1251b;
  font-size: 15px;
  font-weight: 600;
}

/* ========== 发送商品选择器 ========== */
.goods-picker-list {
  max-height: 360px;
  overflow-y: auto;
  margin-top: 12px;
}
.goods-picker-item {
  display: flex;
  gap: 12px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}
.goods-picker-item:hover {
  background: #f5f7fa;
}
.picker-goods-cover {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
}
.picker-goods-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.picker-goods-title {
  font-size: 14px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.picker-goods-price {
  color: #e1251b;
  font-size: 15px;
  font-weight: 600;
}
.goods-picker-empty {
  text-align: center;
  color: #999;
  font-size: 13px;
  padding: 40px 0;
}

/* 选择器数据源切换：我浏览过的 / 全部商品 */
.picker-mode-switch {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}
.picker-mode-switch span {
  padding: 4px 14px;
  font-size: 13px;
  border-radius: 14px;
  background: #f2f3f5;
  color: #666;
  cursor: pointer;
  transition: all 0.15s;
}
.picker-mode-switch span.active {
  background: #E1251B;
  color: #fff;
}

/* ========== 搜索用户 ========== */
.conv-search {
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
}

.search-result-list {
  flex: 1;
  overflow-y: auto;
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-bottom: 1px solid #f5f5f5;
  cursor: default;
  transition: background 0.15s;
}
.search-result-item:hover {
  background: #f7f8fa;
}
.search-result-item .conv-content {
  flex: 1;
  min-width: 0;
}

.search-item-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex-shrink: 0;
}
.search-chat-btn,
.search-shop-btn {
  height: 26px;
  padding: 0 12px;
  border: none;
  border-radius: 13px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  white-space: nowrap;
}
.search-chat-btn {
  background: #E1251B;
  color: #fff;
}
.search-chat-btn:hover {
  background: #C81E18;
}
.search-shop-btn {
  background: #f0f0f0;
  color: #555;
}
.search-shop-btn:hover {
  background: #e4e4e4;
}

.search-loading,
.search-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px 16px;
  color: #999;
  font-size: 13px;
}
.search-loading {
  flex-direction: row;
  color: #E1251B;
}
</style>
