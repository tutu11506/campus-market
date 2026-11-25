<template>
      <!-- 搜索命中的用户（与商品结果共用一个搜索框） -->
      <div class="user-result-panel" v-if="searched && (userSearchLoading || userResults.length > 0)">
        <div class="user-result-head">
          <span class="user-result-title">相关用户</span>
          <span class="user-result-count" v-if="userSearchTotal">{{ userSearchTotal }} 位</span>
        </div>
        <div class="user-result-list" v-if="userResults.length">
          <div
            v-for="u in userResults"
            :key="'ur-' + u.id"
            class="user-chip"
            @click="goUserShop(u)"
          >
            <img :src="resolveAvatar(u.avatar)" class="user-chip-avatar" @error="handleAvatarError($event)" alt="" />
            <div class="user-chip-info">
              <div class="user-chip-name">{{ u.username || '匿名用户' }}</div>
              <div class="user-chip-meta">
                <span v-if="u.studentNo">学号 {{ u.studentNo }}</span>
                <span v-else>信用分 {{ u.credit || 0 }}</span>
              </div>
            </div>
            <button class="user-chip-btn" @click.stop="chatWithUser(u)">发消息</button>
          </div>
        </div>
        <div class="user-result-loading" v-else>
          <span class="loading-spinner"></span>
          <span class="load-text">正在查找用户...</span>
        </div>
      </div>

      <!-- 加载中提示（放在列表顶部，进入页面即可看到） -->
      <div class="initial-loading" v-if="loading && goods.length === 0">
        <span class="loading-spinner"></span>
        <span class="load-text">加载中...</span>
      </div>

      <!-- 骨架屏 loading -->
      <div class="goods-grid" v-if="loading && goods.length === 0">
        <div class="goods-card skeleton-card" v-for="n in 12" :key="'skeleton-' + n">
          <div class="card-img-wrap">
            <el-skeleton :rows="1" animated style="width:100%;height:100%;" />
          </div>
          <div class="card-info">
            <el-skeleton :rows="2" animated />
            <el-skeleton :rows="1" animated style="width:60%;margin-top:8px;" />
          </div>
        </div>
      </div>

      <!-- 商品列表：一行六个 -->
      <div class="goods-grid" v-if="goods.length > 0">
        <div class="goods-card" v-for="item in goods" :key="item.id" @click="goDetail(item.id)" @mouseenter="onCardEnter(item, $event)" @mouseleave="onCardLeave">
          <div class="card-img-wrap">
            <SmartImage :src="item.coverImage || item.images" class="card-img" alt="" />
          </div>
          <div class="card-info">
            <div class="card-title">{{ item.title }}</div>
            <div class="card-price-area">
              <span class="card-price-symbol">¥</span>
              <span class="card-price">{{ item.price }}</span>
              <span class="card-original-price" v-if="item.originalPrice && item.originalPrice > item.price">
                ¥{{ item.originalPrice }}
              </span>
            </div>
            <!-- 风控：高于近期同类均价提示 -->
            <div class="card-guard-badge" v-if="item.priceAlert === 1">
              高于近期均价
            </div>
            <!-- 校园专属标签：🎓 毕业清仓 / 学长学姐特惠 等 -->
            <div class="card-tags" v-if="item.priceTag || item.tradeMode === 'exchange' || item.canMail || item.canDeliver || item.exchangeSupported">
              <span class="card-tag" :class="priceTagClass(item.priceTag)" v-if="item.priceTag">{{ item.priceTag }}</span>
              <span class="card-tag tag-mode" v-if="item.tradeMode === 'exchange'">换物</span>
              <span class="card-tag tag-mail" v-if="item.canMail">可邮寄</span>
              <span class="card-tag tag-deliver" v-if="item.canDeliver">送宿舍</span>
              <span class="card-tag tag-exchange" v-if="item.exchangeSupported && item.tradeMode !== 'exchange'">可换物</span>
            </div>
            <div class="card-footer">
              <span class="card-views">
                <el-icon class="views-icon"><View /></el-icon>
                {{ item.viewCount || 0 }} 人浏览
              </span>
              <span class="card-comments">
                <el-icon class="comments-icon"><ChatDotRound /></el-icon>
                {{ item.commentCount || 0 }} 评价
              </span>
              <span class="card-seller" v-if="item.sellerName" :title="item.sellerName">
                <el-icon class="seller-icon"><User /></el-icon>
                {{ item.sellerName }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 商品悬停预览卡片 -->
      <teleport to="body">
        <transition name="gp-fade">
          <div
            v-if="previewItem"
            class="gp-card"
            :style="previewStyle"
            @mouseenter="keepPreview"
            @mouseleave="leavePreview"
            @click="goDetail(previewItem.id)"
          >
            <div class="gp-cover-wrap">
              <SmartImage :src="previewItem.coverImage || previewItem.images" class="gp-cover" alt="" />
              <span class="gp-badge" v-if="previewItem.priceTag">{{ previewItem.priceTag }}</span>
            </div>
            <div class="gp-body">
              <div class="gp-title">{{ previewItem.title }}</div>
              <div class="gp-price-row">
                <template v-if="previewItem.tradeMode === 'exchange'">
                  <span class="gp-exchange">换物</span>
                  <span class="gp-original">参考价 ¥{{ previewItem.price }}</span>
                </template>
                <template v-else>
                  <span class="gp-symbol">¥</span>
                  <span class="gp-price">{{ previewItem.price }}</span>
                  <span class="gp-original" v-if="previewItem.originalPrice && previewItem.originalPrice > previewItem.price">¥{{ previewItem.originalPrice }}</span>
                </template>
              </div>
              <div class="gp-desc">{{ previewItem.description || '卖家还没有填写商品描述，点击查看详情' }}</div>
              <div class="gp-meta">
                <span v-if="previewItem.location"><el-icon><Location /></el-icon>{{ previewItem.location }}</span>
                <span v-if="previewItem.categoryName">{{ previewItem.categoryName }}</span>
                <span><el-icon><Clock /></el-icon>{{ previewTime(previewItem.createTime) }}</span>
              </div>
              <div class="gp-footer">
                <span class="gp-seller" v-if="previewItem.sellerName"><el-icon><User /></el-icon>{{ previewItem.sellerName }}</span>
                <span class="gp-views">{{ previewItem.viewCount || 0 }} 人浏览</span>
                <span class="gp-go">查看详情 ›</span>
              </div>
            </div>
          </div>
        </transition>
      </teleport>

      <!-- 空状态 -->
      <div class="empty-state" v-if="!loading && goods.length === 0">
        <div class="empty-icon">
          <el-icon :size="72" color="#DCDFE6"><Search /></el-icon>
        </div>
        <p class="empty-title">{{ searched || activeCategory !== null || hasTradeFilter ? '暂无相关商品' : '还没有商品哦' }}</p>
        <p class="empty-desc">{{ searched || activeCategory !== null || hasTradeFilter ? '换个关键词、分类或筛选条件试试吧' : '快去发布你的第一件闲置商品吧' }}</p>

        <!-- 空结果时推荐同分类热门（用 sort=views 拉取浏览量最高的 4 件） -->
        <div class="recommend-block" v-if="recommendList.length > 0">
          <div class="recommend-head">
            <span class="recommend-title">
              <el-icon><Lightning /></el-icon>
              <span>{{ activeCategory !== null ? '该分类热门好货' : '站内热门好货' }}</span>
            </span>
            <span class="recommend-tag">猜你喜欢</span>
          </div>
          <div class="recommend-grid">
            <div
              class="recommend-card"
              v-for="g in recommendList"
              :key="'rc-' + g.id"
              @click="goDetail(g.id)"
            >
              <SmartImage :src="g.coverImage || g.images" class="recommend-img" alt="" />
              <div class="recommend-info">
                <div class="recommend-title-text">{{ g.title }}</div>
                <div class="recommend-meta">
                  <span class="recommend-price">¥{{ g.price }}</span>
                  <span class="recommend-views">
                    <el-icon class="mini-icon"><View /></el-icon>{{ g.viewCount || 0 }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { Search, View, ChatDotRound, User, Location, Clock, Lightning } from '@element-plus/icons-vue'
import SmartImage from '../SmartImage.vue'
import { DEFAULT_AVATAR, resolveAvatar } from '../../utils/avatar'

defineProps({
  goods: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  searched: { type: Boolean, default: false },
  activeCategory: { type: [Number, String], default: null },
  hasTradeFilter: { type: Boolean, default: false },
  recommendList: { type: Array, default: () => [] },
  userResults: { type: Array, default: () => [] },
  userSearchTotal: { type: Number, default: 0 },
  userSearchLoading: { type: Boolean, default: false }
})
const emit = defineEmits(['open-goods', 'open-user', 'chat-user'])

const goDetail = (id) => emit('open-goods', id)
const goUserShop = (u) => emit('open-user', u)
const chatWithUser = (u) => emit('chat-user', u)

const handleAvatarError = (e) => {
  e.target.src = DEFAULT_AVATAR
}

// priceTag 样式映射：不同标签显示不同颜色
const priceTagClass = (tag) => {
  if (!tag) return ''
  if (tag.includes('学长学姐特惠')) return 'tag-senior'      // 红底最醒目
  if (tag.includes('毕业清仓')) return 'tag-graduation'      // 橙黄
  if (tag.includes('同校好货')) return 'tag-campus'          // 蓝绿
  return ''
}

// ==================== 商品悬停预览 ====================
const previewItem = ref(null)
const previewStyle = ref({})
let previewShowTimer = null
let previewHideTimer = null

const previewTime = (v) => {
  if (!v) return ''
  const s = String(v)
  const d = new Date(s.includes('T') ? s : s.replace(/-/g, '/'))
  if (isNaN(d.getTime())) return ''
  const diff = Date.now() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + ' 分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + ' 小时前'
  if (diff < 7 * 86400000) return Math.floor(diff / 86400000) + ' 天前'
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const calcPreviewStyle = (e) => {
  const rect = e.currentTarget.getBoundingClientRect()
  const width = 286
  const gap = 12
  const viewportW = window.innerWidth
  const viewportH = window.innerHeight
  let left = rect.right + gap
  if (left + width > viewportW - 12) left = rect.left - width - gap
  if (left < 12) left = 12
  let top = rect.top
  const estHeight = 340
  if (top + estHeight > viewportH - 12) top = Math.max(12, viewportH - estHeight - 12)
  return { left: left + 'px', top: top + 'px', width: width + 'px' }
}

const onCardEnter = (item, e) => {
  clearTimeout(previewShowTimer)
  clearTimeout(previewHideTimer)
  const style = calcPreviewStyle(e)
  previewShowTimer = setTimeout(() => {
    previewStyle.value = style
    previewItem.value = item
  }, 180)
}
const onCardLeave = () => {
  clearTimeout(previewShowTimer)
  clearTimeout(previewHideTimer)
  previewHideTimer = setTimeout(() => { previewItem.value = null }, 160)
}
const keepPreview = () => {
  clearTimeout(previewHideTimer)
}
const leavePreview = () => {
  clearTimeout(previewHideTimer)
  previewHideTimer = setTimeout(() => { previewItem.value = null }, 120)
}
const hidePreviewOnScroll = () => {
  clearTimeout(previewShowTimer)
  previewItem.value = null
}

onMounted(() => {
  window.addEventListener('scroll', hidePreviewOnScroll, { passive: true })
})
onUnmounted(() => {
  window.removeEventListener('scroll', hidePreviewOnScroll)
  clearTimeout(previewShowTimer)
  clearTimeout(previewHideTimer)
})
</script>

<style scoped>
/* ========== 搜索命中的用户 ========== */
.user-result-panel {
  margin-bottom: 12px;
  padding: 12px 14px;
  background: #fff;
  border: 1px solid #DDDDDD;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.user-result-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.user-result-title {
  font-size: 14px;
  font-weight: 700;
  color: #333;
}

.user-result-count {
  font-size: 12px;
  color: #999;
}

.user-result-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid #E7E7E7;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.user-chip:hover {
  background: #FFF7F6;
  border-color: #F3C4C1;
}

.user-chip-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  background: #eee;
  flex-shrink: 0;
}

.user-chip-info {
  flex: 1;
  min-width: 0;
}

.user-chip-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-chip-meta {
  margin-top: 2px;
  font-size: 11px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-chip-btn {
  flex-shrink: 0;
  padding: 5px 12px;
  border: none;
  border-radius: 14px;
  background: #E1251B;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}

.user-chip-btn:hover {
  background: #C81E18;
}

.user-result-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  font-size: 12px;
  color: #999;
}

/* ========== 商品网格：一行六个 ========== */
.goods-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.goods-card {
  background: #fff;
  border: 1px solid #DDDDDD;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.07);
}

.goods-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

/* ========== 商品悬停预览卡片 ========== */
.gp-card {
  position: fixed;
  z-index: 3000;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #F0F1F3;
  box-shadow: 0 16px 44px rgba(17, 24, 39, 0.18);
  overflow: hidden;
  cursor: pointer;
}

.gp-cover-wrap {
  position: relative;
  width: 100%;
  height: 150px;
  background: #F5F5F5;
}

.gp-cover {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.gp-badge {
  position: absolute;
  left: 10px;
  top: 10px;
  padding: 2px 8px;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  background: rgba(225, 37, 27, 0.92);
  border-radius: 8px;
}

.gp-body {
  padding: 10px 12px 12px;
}

.gp-title {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
  color: #1F2937;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.gp-price-row {
  display: flex;
  align-items: baseline;
  gap: 3px;
  margin-top: 7px;
}

.gp-symbol {
  font-size: 12px;
  font-weight: 700;
  color: #E1251B;
}

.gp-price {
  font-size: 20px;
  font-weight: 800;
  line-height: 1;
  color: #E1251B;
}

.gp-original {
  margin-left: 4px;
  font-size: 11px;
  color: #B0B4BA;
  text-decoration: line-through;
}

.gp-exchange {
  font-size: 18px;
  font-weight: 800;
  color: #7C3AED;
}

.gp-desc {
  margin-top: 7px;
  font-size: 12px;
  line-height: 1.6;
  color: #6B7280;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.gp-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-top: 8px;
  font-size: 11px;
  color: #9CA3AF;
}

.gp-meta span {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.gp-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 9px;
  padding-top: 9px;
  border-top: 1px dashed #F0F1F3;
  font-size: 11px;
  color: #9CA3AF;
}

.gp-seller {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  max-width: 110px;
  color: #4B5563;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gp-views {
  margin-left: auto;
}

.gp-go {
  color: #E1251B;
  font-weight: 700;
}

.gp-fade-enter-active,
.gp-fade-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.gp-fade-enter-from,
.gp-fade-leave-to {
  opacity: 0;
  transform: translateY(4px);
}

.card-img-wrap {
  width: 100%;
  padding-top: 100%;
  position: relative;
  background: #f5f5f5;
  overflow: hidden;
}

.card-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.card-img.img-loaded {
  animation: cardImgFade 0.28s ease;
}

@keyframes cardImgFade {
  from { opacity: 0.25; }
  to { opacity: 1; }
}

.goods-card:hover .card-img {
  transform: scale(1.05);
}

.card-info {
  padding: 8px 10px 10px;
}

.card-title {
  font-size: 13px;
  color: #333;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 8px;
  min-height: 39px;
}

.card-price-area {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 6px;
}

.card-price-symbol {
  font-size: 12px;
  font-weight: 700;
  color: #E1251B;
}

.card-price {
  font-size: 17px;
  font-weight: 700;
  color: #E1251B;
  line-height: 1;
}

.card-original-price {
  font-size: 11px;
  color: #bbb;
  text-decoration: line-through;
  margin-left: 2px;
}

/* 校园专属标签 */
.card-tags {
  margin-bottom: 6px;
}

/* 风控价格提示徽标 */
.card-guard-badge {
  display: inline-block;
  padding: 1px 7px;
  margin-bottom: 6px;
  border: 1px solid #f3c1bd;
  border-radius: 8px;
  background: #fff5f4;
  color: #E1251B;
  font-size: 11px;
  line-height: 16px;
}

.card-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
  line-height: 1.4;
}

.card-tag.tag-senior {
  background: linear-gradient(135deg, #E1251B, #C81E18);
  color: #fff;
}

.card-tag.tag-graduation {
  background: linear-gradient(135deg, #F59E0B, #D97706);
  color: #fff;
}

.card-tag.tag-campus {
  background: linear-gradient(135deg, #10B981, #059669);
  color: #fff;
}

.card-tag.tag-mode {
  background: #F5F3FF;
  color: #7C3AED;
}

.card-tag.tag-mail {
  background: #EFF6FF;
  color: #2563EB;
}

.card-tag.tag-deliver {
  background: #ECFDF5;
  color: #059669;
}

.card-tag.tag-exchange {
  background: #FDF2F8;
  color: #DB2777;
}

.card-footer {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px 8px;
}

.card-seller {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  color: #999;
  margin-left: auto;
  max-width: 50%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.seller-icon,
.views-icon,
.comments-icon {
  font-size: 11px;
  flex-shrink: 0;
}

.card-views,
.card-comments {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: #999;
}

/* ========== 空状态 ========== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
}

.empty-icon {
  margin-bottom: 20px;
}

.empty-title {
  font-size: 16px;
  color: #666;
  margin-bottom: 6px;
  font-weight: 500;
}

.empty-desc {
  font-size: 13px;
  color: #999;
}

/* ========== 空结果推荐 ========== */
.recommend-block {
  width: 100%;
  max-width: 780px;
  margin-top: 40px;
  padding-top: 28px;
  border-top: 1px solid #f0f0f0;
}

.recommend-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.recommend-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.recommend-title .el-icon {
  color: #FF8C00;
}

.recommend-tag {
  font-size: 12px;
  color: #E1251B;
  background: #FFF1EF;
  padding: 3px 10px;
  border-radius: 10px;
  font-weight: 500;
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.recommend-card {
  background: #fff;
  border-radius: 10px;
  border: 1px solid #f0f0f0;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.recommend-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.1);
  border-color: #E1251B;
}

.recommend-img {
  width: 100%;
  height: 120px;
  object-fit: cover;
  display: block;
  background: #f5f5f5;
}

.recommend-info {
  padding: 10px 12px 12px;
}

.recommend-title-text {
  font-size: 13px;
  color: #333;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 8px;
  min-height: 36px;
}

.recommend-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.recommend-price {
  color: #E1251B;
  font-size: 16px;
  font-weight: 700;
}

.recommend-views {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: #999;
}

.recommend-views .mini-icon {
  font-size: 12px;
}

/* ========== 加载动画 ========== */
.loading-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid #eee;
  border-top-color: #E1251B;
  border-radius: 50%;
  animation: loading-spin 0.7s linear infinite;
}

@keyframes loading-spin {
  to { transform: rotate(360deg); }
}

.load-text {
  margin-left: 0;
}

/* ========== 响应式适配 ========== */
@media (max-width: 1024px) {
  .goods-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .goods-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
  }

  .user-result-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .goods-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .user-result-list {
    grid-template-columns: minmax(0, 1fr);
  }

  .user-chip-btn {
    display: none;
  }

  .card-seller {
    display: none;
  }
}
</style>
