<template>
    <!-- 顶部导航栏（固定顶部） -->
    <div class="header">
      <div class="header-content">
        <!-- 左侧 Logo + 标题 -->
        <div class="header-left" @click="scrollToTop">
          <div class="logo-box">
            <span class="logo-text">校</span>
          </div>
          <span class="header-title">校园二手</span>
        </div>

        <!-- 中间大搜索框：商品 + 用户 一次搜索 -->
        <div class="search-area">
          <div class="search-wrapper">
            <el-icon class="search-prefix"><Search /></el-icon>
            <input
              v-model="keyword"
              class="search-input"
              placeholder="搜索商品、用户昵称或学号"
              @focus="onSearchFocus"
              @input="onKeywordInput"
              @keyup.enter="doSearch"
              @keyup.esc="closeSuggest"
              @blur="closeSuggestLater"
            />
            <span class="search-scope">商品 / 用户</span>
            <button class="search-btn" @click="doSearch">
              <el-icon><Search /></el-icon>
              <span>搜索</span>
            </button>
          </div>

          <!-- 搜索联想下拉 -->
          <div class="search-dropdown" v-if="showSuggest" @mousedown.prevent>
            <template v-if="keyword.trim()">
              <div class="suggest-search-row" @click="doSearch">
                <el-icon><Search /></el-icon>
                <span>搜索 “{{ keyword.trim() }}” 的商品和用户</span>
              </div>
              <!-- 相关用户联想 -->
              <div class="suggest-block" v-if="userSuggestions.length">
                <div class="suggest-block-head"><span>相关用户</span></div>
                <div
                  v-for="u in userSuggestions"
                  :key="'su-' + u.id"
                  class="suggest-user"
                  @click="goUserShop(u)"
                >
                  <img :src="resolveAvatar(u.avatar)" class="suggest-user-avatar" @error="handleAvatarError($event)" alt="" />
                  <span class="suggest-user-name">{{ u.username || '匿名用户' }}</span>
                  <span class="suggest-user-meta">{{ u.studentNo ? '学号 ' + u.studentNo : '信用分 ' + (u.credit || 0) }}</span>
                </div>
              </div>
              <div class="suggest-loading" v-if="suggestLoading">搜索中...</div>
              <template v-else>
                <div
                  v-for="g in suggestions"
                  :key="'sg-' + g.id"
                  class="suggest-item"
                  @click="pickSuggestion(g)"
                >
                  <span class="suggest-title">{{ g.title }}</span>
                  <span class="suggest-price">¥{{ g.price }}</span>
                </div>
                <div class="suggest-empty" v-if="suggestions.length === 0 && userSuggestions.length === 0">没有找到相关商品或用户，按回车直接搜索</div>
              </template>
            </template>
            <template v-else>
              <div class="suggest-block" v-if="searchHistory.length">
                <div class="suggest-block-head">
                  <span>搜索历史</span>
                  <span class="suggest-clear" @click.stop="clearHistory">清空</span>
                </div>
                <div class="history-tags">
                  <span
                    v-for="h in searchHistory"
                    :key="'his-' + h"
                    class="history-tag"
                    @click="applyKeyword(h)"
                  >{{ h }}</span>
                </div>
              </div>
              <div class="suggest-block" v-if="hotWords.length">
                <div class="suggest-block-head"><span>热门分类</span></div>
                <div class="history-tags">
                  <span
                    v-for="h in hotWords"
                    :key="'hot-' + h"
                    class="history-tag hot"
                    @click="applyKeyword(h)"
                  >{{ h }}</span>
                </div>
              </div>
              <div class="suggest-empty" v-if="!searchHistory.length && !hotWords.length">输入商品名或分类开始搜索</div>
            </template>
          </div>
        </div>

        <!-- 右侧：闪送入口 + 消息入口 -->
        <div class="header-right">
          <div class="flash-entry" @click="goFlash">
            <el-icon :size="15"><Van /></el-icon>
            <span>校园闪送</span>
          </div>
          <div class="message-icon" @click="goMessage">
            <el-icon :size="22"><Bell /></el-icon>
            <span class="message-badge" v-if="unreadCount > 0">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 首屏主区：分类 + 轮播广告 + 四大类 -->
    <div class="main-panel">
      <!-- 左侧分类（悬停展开细分类） -->
      <aside class="category-sidebar">
        <div class="sidebar-title">分类</div>
        <div
          class="cat-row"
          :class="{ active: activeCategory === null }"
          @click="selectCategory(null)"
        >
          <span class="cat-dot"></span>
          <span class="cat-name">全部商品</span>
          <el-icon class="cat-arrow"><ArrowRight /></el-icon>
        </div>
        <div
          v-for="cat in categoryList"
          :key="cat.id"
          class="cat-row"
          :class="{ active: activeCategory === cat.id }"
          @click="selectCategory(cat.id)"
          @mouseenter="hoverCatId = cat.id"
          @mouseleave="hoverCatId = null"
        >
          <span class="cat-dot"></span>
          <span class="cat-name">{{ cat.name }}</span>
          <el-icon class="cat-arrow"><ArrowRight /></el-icon>

          <!-- 更细的分类浮层 -->
          <div class="sub-panel" v-if="hoverCatId === cat.id">
            <div class="sub-head">{{ cat.name }}</div>
            <div class="sub-list">
              <span
                v-for="sub in getSubCategories(cat)"
                :key="sub"
                class="sub-item"
                @click.stop="selectSubCategory(cat, sub)"
              >{{ sub }}</span>
            </div>
            <div class="sub-more" @click.stop="selectCategory(cat.id)">查看全部 {{ cat.name }} ›</div>
          </div>
        </div>
      </aside>

      <!-- 中间自动轮播广告 -->
      <section class="banner" @mouseenter="pauseBanner" @mouseleave="resumeBanner">
        <div
          v-for="(slide, idx) in slides"
          :key="'slide-' + idx"
          class="slide"
          :class="[slide.theme, { active: currentSlide === idx }]"
          @click="goSlide(slide)"
        >
          <div class="slide-text">
            <span class="slide-tag">校园精选</span>
            <div class="slide-title">{{ slide.title }}</div>
            <div class="slide-desc">{{ slide.desc }}</div>
            <div class="slide-price" v-if="slide.price != null">
              <span class="slide-price-symbol">¥</span>
              <span class="slide-price-num">{{ slide.price }}</span>
            </div>
            <span class="slide-btn">立即查看</span>
          </div>
          <div class="slide-img-wrap" v-if="slide.image">
            <img :src="slide.image" class="slide-img" @error="handleImgError($event)" alt="" />
          </div>
        </div>
        <!-- 轮播指示点 -->
        <div class="slide-dots" v-if="slides.length > 1">
          <span
            v-for="(s, idx) in slides"
            :key="'dot-' + idx"
            class="slide-dot"
            :class="{ active: currentSlide === idx }"
            @click.stop="currentSlide = idx"
          ></span>
        </div>
      </section>

      <!-- 右侧四个大类（点击直接看该大类的商品） -->
      <aside class="quick-cats" v-if="quickCats.length">
        <div
          v-for="(cat, i) in quickCats"
          :key="'quick-' + cat.id"
          class="quick-card"
          :class="'quick-theme-' + (i % 4)"
          @click="selectCategory(cat.id)"
        >
          <div class="quick-text">
            <div class="quick-name">{{ cat.name }}</div>
            <div class="quick-sub">去逛逛 ›</div>
          </div>
          <img
            v-if="quickCovers[cat.id] || fallbackCover(i)"
            :src="quickCovers[cat.id] || fallbackCover(i)"
            class="quick-img"
            @error="handleImgError($event)"
            alt=""
          />
        </div>
      </aside>
    </div>

</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, ArrowRight, Bell, Van } from '@element-plus/icons-vue'
import { getGoodsList } from '../../api/goods'
import { searchUsers } from '../../api/user'
import { unreadState, refreshUnread } from '../../store/unread'
import { DEFAULT_AVATAR, resolveAvatar } from '../../utils/avatar'
import { getImageUrl, handleImgError } from '../../utils/image'

const props = defineProps({
  keyword: { type: String, default: '' },
  categories: { type: Array, default: () => [] },
  activeCategory: { type: [Number, String], default: null },
  featuredGoods: { type: Array, default: () => [] },
  quickCovers: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:keyword', 'search', 'open-goods', 'select-category', 'select-sub'])

const router = useRouter()

// 与父组件双向绑定的搜索词
const keyword = computed({
  get: () => props.keyword,
  set: (v) => emit('update:keyword', v)
})
const categoryList = computed(() => props.categories)
const quickCovers = computed(() => props.quickCovers)
const unreadCount = computed(() => unreadState.count)

const hoverCatId = ref(null)
const currentSlide = ref(0)
let bannerTimer = null

// 搜索联想与历史
const SEARCH_HISTORY_KEY = 'campus_search_history'
const showSuggest = ref(false)
const suggestions = ref([])
const suggestLoading = ref(false)
const searchHistory = ref(loadSearchHistory())
const userSuggestions = ref([])
let suggestTimer = null
let suggestSeq = 0
let userSuggestSeq = 0
let unreadTimer = null

function loadSearchHistory() {
  try {
    const raw = localStorage.getItem(SEARCH_HISTORY_KEY)
    const arr = raw ? JSON.parse(raw) : []
    return Array.isArray(arr) ? arr.slice(0, 8) : []
  } catch (e) {
    return []
  }
}

// 各大类下的细分类（大类名称来自数据库，按名称模糊匹配）
const SUB_CATEGORY_MAP = {
  教材书籍: ['教材教辅', '考研资料', '四六级', '考试考证', '小说文学', '课外读物'],
  数码产品: ['手机', '笔记本电脑', '平板', '耳机音箱', '相机', '智能设备'],
  生活用品: ['宿舍电器', '厨具餐具', '收纳整理', '清洁日用', '床品家纺', '个护洗护'],
  服饰鞋包: ['男装', '女装', '鞋靴', '箱包', '配饰', '运动鞋'],
  运动户外: ['健身器材', '球类', '骑行', '户外装备', '游泳', '轮滑'],
  其他: ['乐器', '美妆', '宠物用品', '手工制作', '闲置杂货', '票券']
}

const quickCats = computed(() => categoryList.value.slice(0, 4))
const hotWords = computed(() => categoryList.value.slice(0, 6).map(c => c.name))

// 中间轮播：优先使用已有商品图片与信息
const slides = computed(() => {
  const list = props.featuredGoods.slice(0, 3)
  if (list.length === 0) {
    return [{
      id: null,
      title: '校园好物 一键换新',
      desc: '毕业清仓 · 低价转让 · 当面交易更放心',
      price: null,
      image: '',
      theme: 'theme-green'
    }]
  }
  const themes = ['theme-green', 'theme-pink', 'theme-blue']
  return list.map((g, i) => ({
    id: g.id,
    title: g.title,
    desc: '九成新 · 当面验货 · 校园内自提',
    price: g.price,
    image: getImageUrl(g.coverImage || g.images),
    theme: themes[i % themes.length]
  }))
})

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const getSubCategories = (cat) => {
  if (!cat || !cat.name) return []
  const key = Object.keys(SUB_CATEGORY_MAP).find(k => cat.name.includes(k) || k.includes(cat.name))
  return key ? SUB_CATEGORY_MAP[key] : [cat.name]
}

// 右侧大类无对应商品图时，用已有商品图兜底
const fallbackCover = (i) => {
  const g = props.featuredGoods[i * 2] || props.featuredGoods[i]
  return g ? getImageUrl(g.coverImage || g.images) : ''
}

// ==================== 轮播 ====================
const startBanner = () => {
  stopBanner()
  if (slides.value.length > 1) {
    bannerTimer = setInterval(() => {
      currentSlide.value = (currentSlide.value + 1) % slides.value.length
    }, 4000)
  }
}
const stopBanner = () => {
  if (bannerTimer) {
    clearInterval(bannerTimer)
    bannerTimer = null
  }
}
const pauseBanner = () => stopBanner()
const resumeBanner = () => startBanner()
const goSlide = (slide) => {
  if (slide && slide.id) emit('open-goods', slide.id)
}

const selectCategory = (catId) => {
  emit('select-category', catId)
}
const selectSubCategory = (cat, sub) => {
  hoverCatId.value = null
  emit('select-sub', cat, sub)
}

// ==================== 搜索联想与历史 ====================
const onSearchFocus = () => {
  showSuggest.value = true
}
const closeSuggest = () => {
  showSuggest.value = false
}
// 延迟关闭，保证点击下拉项时不被 blur 提前关掉
const closeSuggestLater = () => {
  setTimeout(closeSuggest, 120)
}
const onKeywordInput = () => {
  showSuggest.value = true
  if (suggestTimer) clearTimeout(suggestTimer)
  const kw = keyword.value.trim()
  if (!kw) {
    suggestions.value = []
    suggestLoading.value = false
    userSuggestions.value = []
    return
  }
  suggestTimer = setTimeout(() => {
    fetchSuggestions(kw)
    fetchUserSuggestions(kw)
  }, 250)
}
const fetchSuggestions = async (kw) => {
  const seq = ++suggestSeq
  suggestLoading.value = true
  try {
    const res = await getGoodsList({ keyword: kw, pageNum: 1, pageSize: 6, sort: 'new' })
    if (seq !== suggestSeq) return
    suggestions.value = (res && res.list) ? res.list : []
  } catch (e) {
    if (seq === suggestSeq) suggestions.value = []
  } finally {
    if (seq === suggestSeq) suggestLoading.value = false
  }
}
// 用户联想（需登录；未登录直接跳过，不打断商品搜索）
const fetchUserSuggestions = async (kw) => {
  const seq = ++userSuggestSeq
  if (!localStorage.getItem('token')) {
    userSuggestions.value = []
    return
  }
  try {
    const res = await searchUsers(kw, { pageNum: 1, pageSize: 3 })
    if (seq !== userSuggestSeq) return
    userSuggestions.value = (res && res.list) ? res.list : (Array.isArray(res) ? res : [])
  } catch (e) {
    if (seq === userSuggestSeq) userSuggestions.value = []
  }
}
const saveHistory = (kw) => {
  const word = kw.trim()
  if (!word) return
  const next = [word, ...searchHistory.value.filter(h => h !== word)].slice(0, 8)
  searchHistory.value = next
  try {
    localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(next))
  } catch (e) { /* 忽略存储异常 */ }
}
const clearHistory = () => {
  searchHistory.value = []
  try {
    localStorage.removeItem(SEARCH_HISTORY_KEY)
  } catch (e) { /* 忽略存储异常 */ }
}
const applyKeyword = (word) => {
  keyword.value = word
  doSearch()
}
const doSearch = () => {
  const kw = keyword.value.trim()
  showSuggest.value = false
  suggestions.value = []
  userSuggestions.value = []
  if (kw) saveHistory(kw)
  emit('search', kw)
}
const pickSuggestion = (goods) => {
  showSuggest.value = false
  const kw = keyword.value.trim()
  if (kw) saveHistory(kw)
  router.push(`/goods/${goods.id}`)
}

// 搜索结果中的用户：进店 / 发消息
const goUserShop = (u) => {
  if (!u || !u.id) return
  router.push(`/profile?userId=${u.id}`)
}

const handleAvatarError = (e) => {
  e.target.src = DEFAULT_AVATAR
}

const goMessage = () => {
  router.push('/message')
}
const goFlash = () => {
  router.push('/flash-delivery')
}

// ==================== 未读消息角标 ====================
const startUnreadPolling = () => {
  stopUnreadPolling()
  unreadTimer = setInterval(refreshUnread, 10000)
}
const onUnreadVisible = () => {
  if (!document.hidden) refreshUnread()
}
const stopUnreadPolling = () => {
  if (unreadTimer) {
    clearInterval(unreadTimer)
    unreadTimer = null
  }
}

// 商品数据异步到达后启动轮播
watch(() => slides.value.length, () => startBanner())

onMounted(() => {
  refreshUnread()
  startUnreadPolling()
  window.addEventListener('focus', refreshUnread)
  document.addEventListener('visibilitychange', onUnreadVisible)
  startBanner()
})

onUnmounted(() => {
  stopUnreadPolling()
  window.removeEventListener('focus', refreshUnread)
  document.removeEventListener('visibilitychange', onUnreadVisible)
  stopBanner()
  if (suggestTimer) clearTimeout(suggestTimer)
})
</script>

<style scoped>
/* ========== 顶部导航栏 ========== */
.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  height: 56px;
  background: linear-gradient(135deg, #E1251B 0%, #C81E18 100%);
  box-shadow: 0 2px 8px rgba(225, 37, 27, 0.3);
}

/* 宽屏：为左侧功能导航让出一列，顶栏同步右移保持对齐 */
@media (min-width: 1100px) {
  .home-page {
    padding-left: 360px;
  }
  .header {
    left: 360px;
  }
}

/* 中等宽度：侧栏收起为 72px 图标导轨 */
@media (min-width: 768px) and (max-width: 1099px) {
  .home-page {
    padding-left: 72px;
  }
  .header {
    left: 72px;
  }
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  flex-shrink: 0;
}

.logo-box {
  width: 32px;
  height: 32px;
  background: #fff;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #E1251B;
  line-height: 1;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 1px;
}

.search-area {
  position: relative;
  flex: 1;
  max-width: 640px;
}

.search-wrapper {
  width: 100%;
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 21px;
  height: 42px;
  padding-left: 14px;
  border: 2px solid transparent;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.16);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.search-wrapper:focus-within {
  border-color: #FFD84D;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2);
}

.search-prefix {
  flex-shrink: 0;
  color: #bbb;
  font-size: 16px;
}

.search-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  color: #333;
  padding: 0 10px;
  height: 100%;
}

.search-input::placeholder {
  color: #b9b9b9;
}

.search-scope {
  flex-shrink: 0;
  padding: 2px 8px;
  margin-right: 8px;
  border-radius: 10px;
  background: #FFF1F0;
  color: #E1251B;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}

.search-btn {
  height: 34px;
  margin: 0 4px;
  padding: 0 18px;
  border-radius: 17px;
  background: linear-gradient(135deg, #E1251B 0%, #C81E18 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.search-btn:hover {
  background: linear-gradient(135deg, #C81E18 0%, #A81810 100%);
}

/* 搜索联想下拉 */
.search-dropdown {
  position: absolute;
  top: 50px;
  left: 0;
  right: 0;
  z-index: 1200;
  max-height: 400px;
  overflow-y: auto;
  padding: 6px 0;
  background: #fff;
  border: 1px solid #DDDDDD;
  border-radius: 10px;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.16);
}

.suggest-search-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 14px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
}

.suggest-search-row:hover {
  background: #FFF5F4;
  color: #E1251B;
}

.suggest-loading,
.suggest-empty {
  padding: 14px;
  font-size: 12px;
  color: #999;
  text-align: center;
}

.suggest-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 14px;
  cursor: pointer;
}

.suggest-item:hover {
  background: #FFF5F4;
}

.suggest-title {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.suggest-item:hover .suggest-title {
  color: #E1251B;
}

.suggest-price {
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 600;
  color: #E1251B;
}

/* 联想下拉中的用户行 */
.suggest-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 14px;
  cursor: pointer;
}

.suggest-user:hover {
  background: #FFF5F4;
}

.suggest-user-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  object-fit: cover;
  background: #f5f5f5;
  flex-shrink: 0;
}

.suggest-user-name {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.suggest-user:hover .suggest-user-name {
  color: #E1251B;
}

.suggest-user-meta {
  flex-shrink: 0;
  font-size: 11px;
  color: #aaa;
}

.suggest-block {
  padding: 8px 14px;
}

.suggest-block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12px;
  color: #999;
}

.suggest-clear {
  cursor: pointer;
}

.suggest-clear:hover {
  color: #E1251B;
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.history-tag {
  padding: 4px 12px;
  background: #f5f5f5;
  border-radius: 14px;
  font-size: 12px;
  color: #555;
  cursor: pointer;
  transition: all 0.15s ease;
}

.history-tag:hover {
  background: #E1251B;
  color: #fff;
}

.history-tag.hot {
  background: #FFF5F4;
  color: #E1251B;
}

.history-tag.hot:hover {
  background: #E1251B;
  color: #fff;
}

.header-right {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 顶部「校园闪送」入口 */
.flash-entry {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 30px;
  padding: 0 12px;
  margin-right: 4px;
  border-radius: 15px;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.4);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition: background 0.18s ease, transform 0.18s ease;
}

.flash-entry:hover {
  background: rgba(255, 255, 255, 0.3);
}

.flash-entry:active {
  transform: scale(0.96);
}

/* 中屏及以上已有左侧导航入口，顶栏不再重复展示闪送按钮；窄屏（<768px）才显示 */
@media (min-width: 768px) {
  .flash-entry {
    display: none;
  }
}

@media (max-width: 768px) {
  .flash-entry span {
    display: none;
  }
  .flash-entry {
    padding: 0 8px;
  }
}

.message-icon {
  position: relative;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  cursor: pointer;
  transition: opacity 0.2s;
}

.message-icon:hover {
  opacity: 0.85;
}

.message-badge {
  position: absolute;
  top: 2px;
  right: 0;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  background: #fff;
  color: #E1251B;
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;
  text-align: center;
  border-radius: 8px;
}

/* ========== 首屏主区 ========== */
.main-panel {
  position: relative;
  z-index: 2;
  width: calc(100% - 40px);
  max-width: 1160px;
  margin: 12px auto 0;
  padding: 10px;
  display: grid;
  grid-template-columns: 210px minmax(0, 1fr) 226px;
  gap: 10px;
  background: #fff;
  border: 1px solid #DCDCDC;
  border-radius: 10px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

/* 左侧分类 */
.category-sidebar {
  position: relative;
  z-index: 6;
  height: 100%;
  padding: 6px 4px;
  background: #fff;
  border: 1px solid #E2E2E2;
  border-radius: 8px;
}

.sidebar-title {
  padding: 6px 10px 8px;
  font-size: 13px;
  font-weight: 600;
  color: #999;
}

.cat-row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 7px;
  height: 36px;
  padding: 0 10px;
  border-radius: 6px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.cat-row:hover,
.cat-row:has(.sub-panel) {
  background: #FFF5F4;
  color: #E1251B;
}

.cat-row.active {
  background: #FFF0EE;
  color: #E1251B;
  font-weight: 600;
}

.cat-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #c9c9c9;
  flex-shrink: 0;
}

.cat-row:hover .cat-dot,
.cat-row.active .cat-dot {
  background: #E1251B;
}

.cat-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cat-arrow {
  font-size: 12px;
  color: #c8c8c8;
}

/* 更细分类浮层 */
.sub-panel {
  position: absolute;
  top: 0;
  left: 100%;
  z-index: 50;
  width: 430px;
  min-height: 170px;
  padding: 16px 18px;
  background: #fff;
  border-radius: 0 8px 8px 8px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.16);
  cursor: default;
}

.sub-head {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 700;
  color: #333;
}

.sub-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.sub-item {
  padding: 6px 14px;
  background: #f5f5f5;
  border-radius: 16px;
  font-size: 12px;
  color: #666;
  white-space: nowrap;
  transition: all 0.15s ease;
}

.sub-item:hover {
  background: #E1251B;
  color: #fff;
}

.sub-more {
  margin-top: 14px;
  font-size: 12px;
  color: #E1251B;
}

/* 中间轮播广告 */
.banner {
  position: relative;
  z-index: 1;
  height: 340px;
  border: 1px solid #E2E2E2;
  border-radius: 8px;
  overflow: hidden;
  background: #f7f7f7;
}

.slide {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.5s ease;
  cursor: pointer;
}

.slide.active {
  opacity: 1;
  pointer-events: auto;
}

.theme-green {
  background: linear-gradient(120deg, #e6f5d4 0%, #c5e79a 55%, #a9d96c 100%);
}

.theme-pink {
  background: linear-gradient(120deg, #ffe7ea 0%, #ffc6cf 55%, #ffa3b3 100%);
}

.theme-blue {
  background: linear-gradient(120deg, #e4efff 0%, #c2dbff 55%, #9dc2ff 100%);
}

.slide-text {
  flex: 1;
  min-width: 0;
  padding: 0 10px 0 40px;
}

.slide-tag {
  display: inline-block;
  padding: 2px 10px;
  margin-bottom: 12px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 10px;
  font-size: 12px;
  color: #E1251B;
  font-weight: 600;
}

.slide-title {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.35;
  color: #23301a;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  max-width: 380px;
}

.slide-desc {
  margin-top: 10px;
  font-size: 13px;
  color: rgba(35, 48, 26, 0.7);
}

.slide-price {
  display: flex;
  align-items: baseline;
  margin-top: 12px;
  color: #E1251B;
}

.slide-price-symbol {
  font-size: 16px;
  font-weight: 700;
}

.slide-price-num {
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
}

.slide-btn {
  display: inline-block;
  margin-top: 16px;
  padding: 7px 22px;
  background: #fff;
  border-radius: 18px;
  font-size: 13px;
  font-weight: 600;
  color: #E1251B;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.slide-img-wrap {
  width: 44%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.slide-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}

.slide-dots {
  position: absolute;
  left: 50%;
  bottom: 12px;
  z-index: 5;
  display: flex;
  gap: 6px;
  transform: translateX(-50%);
}

.slide-dot {
  width: 6px;
  height: 6px;
  border-radius: 3px;
  background: rgba(0, 0, 0, 0.2);
  cursor: pointer;
  transition: all 0.2s ease;
}

.slide-dot.active {
  width: 18px;
  background: #E1251B;
}

/* 右侧四个大类 */
.quick-cats {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.quick-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 0 14px;
  border: 1px solid #E2E2E2;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.quick-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}

.quick-theme-0 { background: linear-gradient(135deg, #fff4f2, #ffe6e2); }
.quick-theme-1 { background: linear-gradient(135deg, #fdf3ff, #f3e3ff); }
.quick-theme-2 { background: linear-gradient(135deg, #eef6ff, #e0edff); }
.quick-theme-3 { background: linear-gradient(135deg, #f3fbf0, #e6f5df); }

.quick-text {
  flex: 1;
  min-width: 0;
}

.quick-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-sub {
  margin-top: 2px;
  font-size: 11px;
  color: #999;
}

.quick-img {
  width: 54px;
  height: 54px;
  flex-shrink: 0;
  object-fit: contain;
  background: #fff;
  border-radius: 6px;
}

/* ========== 响应式适配 ========== */
@media (max-width: 1024px) {
  .main-panel {
    grid-template-columns: 180px minmax(0, 1fr);
  }

  .quick-cats {
    display: none;
  }

  .sub-panel {
    width: 340px;
  }
}

@media (max-width: 768px) {
  .header-content {
    padding: 0 12px;
    gap: 12px;
  }

  .header-title {
    display: none;
  }

  .main-panel {
    display: block;
    padding: 8px;
  }

  .category-sidebar {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 8px;
    padding: 0;
    background: transparent;
    overflow-x: auto;
    scrollbar-width: none;
  }

  .category-sidebar::-webkit-scrollbar {
    display: none;
  }

  .sidebar-title {
    display: none;
  }

  .cat-row {
    flex-shrink: 0;
    height: 30px;
    padding: 0 12px;
    background: #f5f5f5;
    border-radius: 15px;
  }

  .cat-dot,
  .cat-arrow {
    display: none;
  }

  .sub-panel {
    display: none;
  }

  .banner {
    height: 200px;
  }

  .slide-text {
    padding-left: 20px;
  }

  .slide-title {
    font-size: 17px;
    max-width: 100%;
  }

  .slide-desc {
    display: none;
  }

  .slide-btn {
    margin-top: 10px;
    padding: 5px 16px;
  }

  .slide-price-num {
    font-size: 20px;
  }
}

@media (max-width: 520px) {
  .search-scope {
    display: none;
  }

  .banner {
    height: 170px;
  }

  .slide-img-wrap {
    display: none;
  }

  .search-btn span {
    display: none;
  }

  .search-btn {
    padding: 0 12px;
  }
}
</style>
