<template>
  <!-- 我发布的 -->
  <div v-show="activeTab === 'published'" class="tab-panel">
    <div v-if="myGoodsList.length === 0" class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ddd" stroke-width="1.5">
        <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
        <circle cx="8.5" cy="8.5" r="1.5"></circle>
        <polyline points="21 15 16 10 5 21"></polyline>
      </svg>
      <p>{{ isSelf ? '还没有发布过商品' : (isOwnShop ? '你的店铺还没有在售商品' : 'TA还没有在售商品') }}</p>
      <button v-if="isSelf" class="go-publish" @click="router.push('/publish')">去发布</button>
    </div>
    <div v-else class="goods-list">
      <div
        v-for="item in myGoodsList"
        :key="item.id"
        class="goods-card"
        :class="{ clickable: !isSelf, 'is-sold': item.status === 'sold' }"
        @click="!isSelf && router.push(`/goods/${item.id}`)"
      >
        <div class="goods-cover-wrap" :class="{ 'cover-sold': item.status === 'sold' }">
          <SmartImage :src="item.coverImage" class="goods-cover" alt="" />
          <div v-if="item.status === 'sold'" class="sold-badge">
            <span class="sold-badge-main">卖掉了</span>
            <span class="sold-badge-sub">SOLD OUT</span>
          </div>
        </div>
        <div class="goods-info">
          <div class="goods-title">{{ item.title }}</div>
          <div class="goods-price">&yen;{{ item.price }}</div>
        </div>
        <div v-if="isSelf" class="goods-actions">
          <span class="status-tag" :class="item.status">{{ statusMap[item.status] || item.status }}</span>
          <div class="action-btns">
            <button
              v-if="item.status === 'on_sale'"
              class="action-btn off"
              @click="emit('off-shelf', item.id)"
            >下架</button>
            <button
              v-if="item.status === 'off_shelf'"
              class="action-btn on"
              @click="emit('on-shelf', item.id)"
            >上架</button>
            <button
              v-if="item.status !== 'locked' && item.status !== 'sold'"
              class="action-btn copy"
              @click="emit('copy', item)"
            >复制</button>
            <button
              v-if="item.status !== 'locked' && item.status !== 'sold'"
              class="action-btn edit"
              @click="emit('edit', item)"
            >编辑</button>
            <button
              v-if="item.status !== 'locked'"
              class="action-btn del"
              @click="emit('delete', item.id)"
            >删除</button>
          </div>
        </div>
      </div>
    </div>
    <div class="pagination-wrap" v-if="pageState.published.total > 0">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="pageState.published.total"
        :current-page="pageState.published.pageNum"
        :page-size="pageState.published.pageSize"
        @current-change="(p) => emit('page-change', 'published', p)"
      />
    </div>
  </div>

  <!-- 我的收藏 -->
  <div v-show="activeTab === 'favorites'" class="tab-panel">
    <div v-if="favoriteList.length === 0" class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ddd" stroke-width="1.5">
        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
      </svg>
      <p>还没有收藏商品</p>
    </div>
    <div v-else class="fav-list">
      <div
        v-for="item in favoriteList"
        :key="item.id"
        class="fav-card"
        @click="router.push(`/goods/${item.goodsId}`)"
      >
        <SmartImage :src="item.goodsCover" class="fav-cover" alt="" />
        <div class="fav-info">
          <div class="fav-title">{{ item.goodsTitle }}</div>
          <div class="fav-row">
            <span class="fav-price">&yen;{{ item.goodsPrice }}</span>
            <span class="fav-seller" v-if="item.sellerName">{{ item.sellerName }}</span>
          </div>
        </div>
      </div>
    </div>
    <div class="pagination-wrap" v-if="pageState.favorites.total > 0">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="pageState.favorites.total"
        :current-page="pageState.favorites.pageNum"
        :page-size="pageState.favorites.pageSize"
        @current-change="(p) => emit('page-change', 'favorites', p)"
      />
    </div>
  </div>

  <!-- 浏览历史 -->
  <div v-show="activeTab === 'browseHistory'" class="tab-panel">
    <div v-if="browseHistoryList.length === 0" class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ddd" stroke-width="1.5">
        <circle cx="12" cy="12" r="10"></circle>
        <path d="M2 12h20M12 2a15 15 0 0 1 0 20M12 2a15 15 0 0 0 0 20"></path>
      </svg>
      <p>还没有浏览记录</p>
    </div>
    <div v-else class="fav-list">
      <div
        v-for="item in browseHistoryList"
        :key="item.id"
        class="fav-card"
        @click="router.push(`/goods/${item.id}`)"
      >
        <SmartImage :src="item.coverImage" class="fav-cover" alt="" />
        <div class="fav-info">
          <div class="fav-title">{{ item.title }}</div>
          <div class="fav-row">
            <span class="fav-price">&yen;{{ item.price }}</span>
            <span class="fav-seller" v-if="item.sellerName">{{ item.sellerName }}</span>
          </div>
          <div class="fav-meta">
            <span class="browse-time">浏览于 {{ formatTime(item.browseTime) }}</span>
            <span class="status-tag" :class="item.status">{{ statusMap[item.status] || item.status }}</span>
          </div>
        </div>
      </div>
    </div>
    <div class="pagination-wrap" v-if="pageState.browseHistory.total > 0">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="pageState.browseHistory.total"
        :current-page="pageState.browseHistory.pageNum"
        :page-size="pageState.browseHistory.pageSize"
        @current-change="(p) => emit('page-change', 'browseHistory', p)"
      />
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import SmartImage from '../SmartImage.vue'

defineProps({
  activeTab: { type: String, default: 'published' },
  isSelf: { type: Boolean, default: true },
  isOwnShop: { type: Boolean, default: false },
  myGoodsList: { type: Array, default: () => [] },
  favoriteList: { type: Array, default: () => [] },
  browseHistoryList: { type: Array, default: () => [] },
  pageState: { type: Object, required: true }
})
const emit = defineEmits(['off-shelf', 'on-shelf', 'copy', 'edit', 'delete', 'page-change'])
const router = useRouter()

// 状态映射
const statusMap = {
  on_sale: '在售',
  locked: '交易中',
  sold: '已售',
  off_shelf: '下架'
}

// 时间格式化
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

/* === 商品卡片（我发布的）- 白色卡片 8px圆角 === */
.goods-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.goods-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
.goods-card.clickable {
  cursor: pointer;
  transition: background 0.2s;
}
.goods-card.clickable:active {
  background: #fafafa;
}
.goods-cover-wrap {
  position: relative;
  width: 80px;
  height: 80px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
}
.goods-cover {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  object-fit: cover;
  display: block;
  background: #f5f5f5;
}
/* 已卖出：图片置灰 + 卖掉了印章 */
.goods-cover-wrap.cover-sold .goods-cover {
  filter: grayscale(1) brightness(1.05);
  opacity: 0.6;
}
.goods-cover-wrap.cover-sold::after {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.16);
  pointer-events: none;
}
.sold-badge {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%) rotate(-9deg);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3px 7px 2px;
  background: rgba(17, 17, 17, 0.9);
  border: 1.5px solid #fff;
  border-radius: 4px;
  box-shadow: 0 0 0 1.5px rgba(17, 17, 17, 0.9);
  white-space: nowrap;
  pointer-events: none;
}
.sold-badge-main {
  font-size: 11px;
  font-weight: 800;
  color: #fff;
  letter-spacing: 1px;
  line-height: 1.25;
}
.sold-badge-sub {
  font-size: 5px;
  color: rgba(255, 255, 255, 0.85);
  letter-spacing: 0.5px;
  line-height: 1.2;
}
.goods-card.is-sold .goods-title {
  color: #aaa;
}
.goods-card.is-sold .goods-price {
  color: #aaa;
}
.goods-info {
  flex: 1;
  min-width: 0;
}
.goods-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}
.goods-price {
  font-size: 16px;
  font-weight: 700;
  color: #E1251B;
}
.goods-actions {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}
.status-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}
.status-tag.on_sale {
  background: #ffeaea;
  color: #E1251B;
}
.status-tag.sold {
  background: #f5f5f5;
  color: #999;
}
.status-tag.locked {
  background: #FFF4E5;
  color: #C77700;
}
.status-tag.off_shelf {
  background: #f5f5f5;
  color: #999;
}
.action-btns {
  display: flex;
  gap: 6px;
}
.action-btn {
  padding: 4px 10px;
  border: none;
  border-radius: 4px;
  font-size: 11px;
  cursor: pointer;
  font-family: inherit;
}
.action-btn.off {
  background: #f5f5f5;
  color: #666;
}
.action-btn.on {
  background: #e8f5e9;
  color: #4caf50;
}
.action-btn.del {
  background: #ffeaea;
  color: #E1251B;
}
.action-btn.edit {
  background: #e8f4ff;
  color: #1976d2;
}
.action-btn.edit:hover {
  background: #d1ebff;
}
.action-btn.copy {
  background: #fff4e5;
  color: #b26a00;
}
.action-btn.copy:hover {
  background: #ffe8cc;
}

/* === 收藏列表 - 白色卡片 8px圆角 === */
.fav-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.fav-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: background 0.2s;
}
.fav-card:active {
  background: #fafafa;
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
.fav-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.fav-price {
  font-size: 16px;
  font-weight: 700;
  color: #E1251B;
}
.fav-seller {
  font-size: 12px;
  color: #999;
}
.fav-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
}
.browse-time {
  font-size: 11px;
  color: #bbb;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 16px 0 8px;
}
</style>
