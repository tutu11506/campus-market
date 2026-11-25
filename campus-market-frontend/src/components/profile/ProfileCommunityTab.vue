<template>
  <!-- 收到的评价 -->
  <div v-show="activeTab === 'receivedComments'" class="tab-panel">
    <div v-if="receivedCommentList.length === 0" class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ddd" stroke-width="1.5">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
      </svg>
      <p>还没有收到评价</p>
    </div>
    <div v-else class="fav-list">
      <div
        v-for="item in receivedCommentList"
        :key="item.id"
        class="comment-card"
        @click="router.push(`/goods/${item.goodsId}`)"
      >
        <SmartImage :src="item.goodsCover" class="fav-cover" alt="" />
        <div class="fav-info">
          <div class="fav-title">{{ item.goodsTitle }}</div>
          <div class="comment-rating">
            <span class="stars">
              <span v-for="s in 5" :key="s" :style="{ color: s <= item.rating ? '#ff9800' : '#ddd' }">★</span>
            </span>
            <span class="comment-user" v-if="item.fromUserName">{{ item.fromUserName }}</span>
            <span class="comment-time">{{ formatTime(item.createTime) }}</span>
          </div>
          <div class="comment-content">{{ item.content }}</div>
        </div>
      </div>
    </div>
    <div class="pagination-wrap" v-if="pageState.receivedComments.total > 0">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="pageState.receivedComments.total"
        :current-page="pageState.receivedComments.pageNum"
        :page-size="pageState.receivedComments.pageSize"
        @current-change="(p) => emit('page-change', 'receivedComments', p)"
      />
    </div>
  </div>

  <!-- 我的帖子 -->
  <div v-show="activeTab === 'myPosts'" class="tab-panel">
    <div v-if="myForumPosts.length === 0" class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ddd" stroke-width="1.5">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
      </svg>
      <p>还没有在论坛发过帖子</p>
      <button class="go-publish" @click="router.push('/forum')">去论坛看看</button>
    </div>
    <div v-else class="forum-post-list">
      <div class="forum-post-item" v-for="p in myForumPosts" :key="'mp-' + p.id" @click="emit('open-post', p.id)">
        <div class="fpi-top">
          <span class="fpi-board" :class="'bd-' + p.board">{{ forumBoardLabel(p.board) }}</span>
          <span class="fpi-time">{{ formatTime(p.createTime) }}</span>
        </div>
        <div class="fpi-title">{{ p.title }}</div>
        <div class="fpi-content">{{ p.content }}</div>
        <div class="fpi-meta">
          <span>👁 {{ p.viewCount || 0 }}</span>
          <span>💬 {{ p.commentCount || 0 }}</span>
          <span>👍 {{ p.likeCount || 0 }}</span>
        </div>
      </div>
    </div>
    <div class="pagination-wrap" v-if="pageState.myPosts.total > 0">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="pageState.myPosts.total"
        :current-page="pageState.myPosts.pageNum"
        :page-size="pageState.myPosts.pageSize"
        @current-change="(p) => emit('page-change', 'myPosts', p)"
      />
    </div>
  </div>

  <!-- 论坛通知 -->
  <div v-show="activeTab === 'forumNotices'" class="tab-panel">
    <div v-if="forumNotices.length === 0" class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ddd" stroke-width="1.5">
        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
        <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
      </svg>
      <p>还没有论坛通知</p>
    </div>
    <div v-else class="forum-notice-list">
      <div
        class="forum-notice-item"
        :class="{ unread: !n.isRead }"
        v-for="n in forumNotices"
        :key="'fn-' + n.id"
        @click="emit('open-post', n.postId)"
      >
        <img class="fn-avatar" :src="resolveAvatar(n.fromUserAvatar)" alt="" />
        <div class="fn-main">
          <div class="fn-title">
            <b>{{ n.fromUserName || '同学' }}</b>
            {{ n.type === 'comment_reply' ? '回复了你的评论' : '回复了你的帖子' }}
          </div>
          <div class="fn-post">《{{ n.postTitle }}》</div>
          <div class="fn-content">{{ n.content }}</div>
        </div>
        <div class="fn-side">
          <span class="fn-time">{{ formatTime(n.createTime) }}</span>
          <span class="fn-dot" v-if="!n.isRead"></span>
        </div>
      </div>
    </div>
    <div class="pagination-wrap" v-if="pageState.forumNotices.total > 0">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="pageState.forumNotices.total"
        :current-page="pageState.forumNotices.pageNum"
        :page-size="pageState.forumNotices.pageSize"
        @current-change="(p) => emit('page-change', 'forumNotices', p)"
      />
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import SmartImage from '../SmartImage.vue'
import { resolveAvatar } from '../../utils/avatar'

defineProps({
  activeTab: { type: String, default: 'receivedComments' },
  receivedCommentList: { type: Array, default: () => [] },
  myForumPosts: { type: Array, default: () => [] },
  forumNotices: { type: Array, default: () => [] },
  pageState: { type: Object, required: true }
})
const emit = defineEmits(['open-post', 'page-change'])
const router = useRouter()

const FORUM_BOARD_LABEL = {
  chat: '校园闲聊',
  lost: '失物招领',
  study: '学习交流',
  team: '拼单组队',
  help: '求助问答'
}
const forumBoardLabel = (b) => FORUM_BOARD_LABEL[b] || '校园闲聊'

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hour = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hour}:${min}`
}
</script>

<style scoped>
.tab-panel {
  width: 100%;
}

/* === 空状态 === */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: #ccc;
}
.empty-state p {
  margin-top: 12px;
  font-size: 14px;
}
.go-publish {
  margin-top: 16px;
  padding: 8px 24px;
  border: none;
  border-radius: 16px;
  background: linear-gradient(135deg, #E1251B, #C81E18);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  font-family: inherit;
}

/* === 我的帖子 === */
.forum-post-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.forum-post-item {
  padding: 14px 16px;
  border: 1px solid #F0F1F3;
  border-radius: 10px;
  cursor: pointer;
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}
.forum-post-item:hover {
  border-color: #FFD9D3;
  box-shadow: 0 4px 14px rgba(225, 37, 27, 0.07);
}
.fpi-top {
  display: flex;
  align-items: center;
  gap: 8px;
}
.fpi-board {
  padding: 1px 8px;
  font-size: 11px;
  font-weight: 600;
  border-radius: 9px;
}
.bd-chat { color: #059669; background: #ECFDF5; }
.bd-lost { color: #D97706; background: #FFFBEB; }
.bd-study { color: #2563EB; background: #EFF6FF; }
.bd-team { color: #7C3AED; background: #F5F3FF; }
.bd-help { color: #E1251B; background: #FFF1F0; }
.fpi-time {
  margin-left: auto;
  font-size: 11px;
  color: #B0B4BA;
}
.fpi-title {
  margin-top: 6px;
  font-size: 15px;
  font-weight: 700;
  color: #1F2937;
}
.fpi-content {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: #6B7280;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.fpi-meta {
  display: flex;
  gap: 14px;
  margin-top: 8px;
  font-size: 11px;
  color: #9CA3AF;
}

/* 论坛通知 */
.forum-notice-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.forum-notice-item {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 13px 14px;
  border: 1px solid #F0F1F3;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.16s ease, background 0.16s ease;
}
.forum-notice-item:hover {
  border-color: #FFD9D3;
  background: #FFFDFC;
}
.forum-notice-item.unread {
  border-color: #FFE0DA;
  background: #FFF9F8;
}
.fn-avatar {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 50%;
  object-fit: cover;
  background: #F5F5F5;
}
.fn-main {
  flex: 1;
  min-width: 0;
}
.fn-title {
  font-size: 13px;
  color: #4B5563;
}
.fn-title b {
  color: #1F2937;
}
.fn-post {
  margin-top: 3px;
  font-size: 12px;
  color: #E1251B;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.fn-content {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: #6B7280;
}
.fn-side {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}
.fn-time {
  font-size: 11px;
  color: #B0B4BA;
}
.fn-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #E1251B;
}

/* === 收藏/评价卡片 === */
.fav-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.fav-cover {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
  background: #f5f5f5;
}
.fav-info {
  flex: 1;
  min-width: 0;
}
.fav-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 6px;
}
.comment-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  cursor: pointer;
}
.comment-card:active {
  background: #fafafa;
}
.comment-rating {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.comment-rating .stars {
  letter-spacing: 1px;
  font-size: 13px;
}
.comment-user {
  font-size: 12px;
  color: #666;
}
.comment-time {
  font-size: 11px;
  color: #bbb;
  margin-left: auto;
}
.comment-content {
  font-size: 13px;
  color: #555;
  line-height: 1.6;
  word-break: break-all;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 16px 0 8px;
}
</style>
