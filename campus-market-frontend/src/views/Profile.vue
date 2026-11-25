<template>
  <div class="profile-page">
    <AppSidebar />
    <div class="profile-shell" :class="{ 'shop-mode': !isSelf }">
      <!-- 左侧菜单（仅我的页面） -->
      <ProfileSidebar
        v-if="isSelf"
        :active-tab="activeTab"
        :forum-unread="forumUnread"
        @switch-tab="switchTab"
        @edit-profile="showEditDialog = true"
        @logout="handleLogout"
      />

      <main class="profile-main">
        <!-- 个人信息卡片（仅我的页面） -->
        <ProfileCard
          v-if="isSelf"
          :user-info="userInfo"
          :avatar-url="avatarUrl"
          :is-admin="isAdmin"
          :published-total="pageState.published.total || 0"
          :stats="stats"
          :stat-items="statItems"
          :seller-stats="sellerStats"
          :recent-max="sellerStatsRecentMax"
          @edit-profile="showEditDialog = true"
          @go-admin="router.push('/admin')"
          @go-stat-tab="goStatTab"
        />

        <!-- 店铺卡片（他人店铺 / 通过搜索进入自己的店铺） -->
        <ShopHeader
          v-else
          :user-info="userInfo"
          :avatar-url="avatarUrl"
          :published-total="pageState.published.total || 0"
          :is-own-shop="isOwnShop"
          @manage-shop="router.push('/profile')"
          @contact-seller="contactSeller"
        />

        <!-- Tab切换区域 -->
    <div class="tabs-area">
      <div class="tab-header">
        <div
          v-for="tab in tabs"
          :key="tab.key"
          class="tab-item"
          :class="{ active: activeTab === tab.key }"
          @click="switchTab(tab.key)"
        >
          {{ tab.label }}
        </div>
      </div>

      <div class="tab-content">
        <ProfileGoodsTab
          :active-tab="activeTab"
          :is-self="isSelf"
          :is-own-shop="isOwnShop"
          :my-goods-list="myGoodsList"
          :favorite-list="favoriteList"
          :browse-history-list="browseHistoryList"
          :page-state="pageState"
          @off-shelf="handleOffShelf"
          @on-shelf="handleOnShelf"
          @copy="handleCopyGoods"
          @edit="openEditGoods"
          @delete="handleDeleteGoods"
          @page-change="handlePageChange"
        />

        <ProfileOrdersTab
          :active-tab="activeTab"
          :buyer-orders="buyerOrders"
          :seller-orders="sellerOrders"
          :invoice-map="invoiceMap"
          :page-state="pageState"
          @pay="openPayDialog"
          @cancel="handleCancelOrder"
          @verify-code="openVerifyCodeDialog"
          @complete="handleCompleteOrder"
          @dispute="openDisputeDialog"
          @return="openReturnDialog"
          @comment="openCommentDialog"
          @view-invoice="viewInvoice"
          @apply-invoice="openInvoiceDialog"
          @confirm="handleConfirmOrder"
          @verify="openVerifyDialog"
          @audit="openAuditDialog"
          @page-change="handlePageChange"
        />

        <ProfileCommunityTab
          :active-tab="activeTab"
          :received-comment-list="receivedCommentList"
          :my-forum-posts="myForumPosts"
          :forum-notices="forumNotices"
          :page-state="pageState"
          @open-post="openForumPost"
          @page-change="handlePageChange"
        />

        <ProfileFinanceTab
          :active-tab="activeTab"
          :invoices="invoices"
          :current-exchanges="currentExchanges"
          :exchange-tab="exchangeTab"
          :exchange-operating-id="exchangeOperatingId"
          :exchange-operating-action="exchangeOperatingAction"
          :page-state="pageState"
          @view-invoice="viewInvoice"
          @switch-exchange-tab="exchangeTab = $event"
          @exchange-action="handleExchange"
          @page-change="handlePageChange"
        />
      </div>
        </div>
      </main>
    </div>

    <!-- 编辑资料弹窗 -->
    <EditProfileDialog
      v-model="showEditDialog"
      :user-info="userInfo"
      :avatar-url="avatarUrl"
      @saved="handleProfileSaved"
    />

    <!-- 申请交易仲裁弹窗 -->
    <DisputeDialog
      v-model="showDisputeDialog"
      :order="disputeOrder"
      @done="handleDisputeDone"
    />

    <!-- 申请退货弹窗 -->
    <ReturnDialog
      v-model="showReturnDialog"
      :order="returnOrder"
      @done="handleReturnDone"
    />

    <!-- 卖家退货审核弹窗 -->
    <AuditDialog
      v-model="showAuditDialog"
      :order="auditOrder"
      @done="handleAuditDone"
    />

    <!-- 模拟支付收银台 -->
    <PayDialog
      v-model="showPayDialog"
      :order="payTargetOrder"
      @done="loadTabData('bought')"
    />

    <!-- 评价弹窗 -->
    <CommentDialog
      v-model="showCommentDialog"
      :order="commentTargetOrder"
      @done="handleCommentDone"
    />

    <!-- 商品编辑弹窗（单一入口初始化：openEditGoods 收敛所有状态） -->
    <EditGoodsDialog
      v-model="showEditGoodsDialog"
      :goods="editGoodsTarget"
      @done="loadTabData('published')"
    />

    <!-- 买家：线下自提核销码 -->
    <VerifyCodeDialog
      v-model="showVerifyCodeDialog"
      :order="verifyOrderInfo"
    />

    <!-- 卖家：核销收款 -->
    <VerifyDialog
      v-model="showVerifyDialog"
      :order="verifyOrderInfo"
      @done="loadTabData('sold')"
    />

    <!-- 申请开票 -->
    <InvoiceDialog
      v-model="showInvoiceDialog"
      :order="invoiceOrder"
      :default-title="userInfo.username"
      @done="handleInvoiceDone"
    />

    <!-- 电子发票详情 -->
    <InvoiceDetailDialog
      v-model="showInvoiceDetail"
      :invoice="viewingInvoice"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import AppSidebar from '../components/AppSidebar.vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyGoods, getGoodsList, updateGoodsStatus, deleteGoods, getSellerStats, copyGoods } from '../api/goods'
import { getUserInfo, getUserStats } from '../api/user'
import { getBuyerOrders, getSellerOrders, confirmOrder, completeOrder, cancelOrder } from '../api/order'
import { getMyFavorites } from '../api/favorite'
import { getMyBrowseHistory } from '../api/browse'
import { getMyComments, getSellerComments } from '../api/comment'
import { getMyPosts, getMyNotifications, getUnreadNotificationCount, readNotifications } from '../api/forum'
import { getMyInvoices } from '../api/invoice'
import {
  getReceivedExchanges, getSentExchanges, acceptExchange, rejectExchange, cancelExchange, finishExchange
} from '../api/exchange'

const route = useRoute()
const router = useRouter()
import { resolveAvatar } from '../utils/avatar'
import { disconnectRealtime } from '../store/realtime'
import { setUnread } from '../store/unread'
import ProfileSidebar from '../components/profile/ProfileSidebar.vue'
import ProfileCard from '../components/profile/ProfileCard.vue'
import ShopHeader from '../components/profile/ShopHeader.vue'
import ProfileGoodsTab from '../components/profile/ProfileGoodsTab.vue'
import ProfileOrdersTab from '../components/profile/ProfileOrdersTab.vue'
import ProfileCommunityTab from '../components/profile/ProfileCommunityTab.vue'
import ProfileFinanceTab from '../components/profile/ProfileFinanceTab.vue'
import EditProfileDialog from '../components/profile/EditProfileDialog.vue'
import DisputeDialog from '../components/profile/DisputeDialog.vue'
import ReturnDialog from '../components/profile/ReturnDialog.vue'
import AuditDialog from '../components/profile/AuditDialog.vue'
import PayDialog from '../components/profile/PayDialog.vue'
import CommentDialog from '../components/profile/CommentDialog.vue'
import EditGoodsDialog from '../components/profile/EditGoodsDialog.vue'
import VerifyCodeDialog from '../components/profile/VerifyCodeDialog.vue'
import VerifyDialog from '../components/profile/VerifyDialog.vue'
import InvoiceDialog from '../components/profile/InvoiceDialog.vue'
import InvoiceDetailDialog from '../components/profile/InvoiceDetailDialog.vue'

// 当前登录用户（本地缓存）
const user = computed(() => JSON.parse(localStorage.getItem('user') || '{}'))
// 查看的目标用户：路由带 ?userId= 时为他人主页，否则为自己
const targetUserId = computed(() => {
  const v = route.query.userId
  return v ? Number(v) : null
})
// 无 userId 参数 = 我的个人中心；带 userId = 店铺模式（即使进的是自己的店）
const isSelf = computed(() => !targetUserId.value)
// 通过搜索/他人链接进入自己的店铺
const isOwnShop = computed(() => !!targetUserId.value && !!user.value && targetUserId.value === user.value.id)
// 仅管理员本人主页显示「管理后台」入口
const isAdmin = computed(() => isSelf.value && user.value.role === 'admin')
const userInfo = ref({ ...user.value })

const avatarUrl = computed(() => resolveAvatar(userInfo.value.avatar))

// Tab相关
const tabs = computed(() => {
  if (isSelf.value) {
    return [
      { key: 'published', label: '我发布的' },
      { key: 'bought', label: '我买到的' },
      { key: 'sold', label: '我卖出的' },
      { key: 'favorites', label: '我的收藏' },
      { key: 'browseHistory', label: '浏览历史' },
      { key: 'receivedComments', label: '收到的评价' },
      { key: 'invoices', label: '我的发票' },
      { key: 'exchanges', label: '我的换物' },
      { key: 'myPosts', label: '我的帖子' },
      { key: 'forumNotices', label: '论坛通知' }
    ]
  }
  // 他人主页：展示公开在售商品与收到的评价
  return [
    { key: 'published', label: '发布的商品' },
    { key: 'receivedComments', label: '收到的评价' }
  ]
})
const activeTab = ref('published')

// 数据列表
const myGoodsList = ref([])
const buyerOrders = ref([])
const invoices = ref([])
const invoiceMap = ref({})
const invoiceOrder = ref(null)
const showInvoiceDialog = ref(false)
const showInvoiceDetail = ref(false)
const viewingInvoice = ref(null)

// 我的换物
const exchangesReceived = ref([])
const exchangesSent = ref([])
const exchangeTab = ref('received')
const exchangeOperatingId = ref(null)
const exchangeOperatingAction = ref('')
const currentExchanges = computed(() => exchangeTab.value === 'received' ? exchangesReceived.value : exchangesSent.value)

// 担保交易 / 线下核销
const showVerifyCodeDialog = ref(false)
const showVerifyDialog = ref(false)
const verifyOrderInfo = ref(null)
const openVerifyCodeDialog = (order) => {
  verifyOrderInfo.value = order
  showVerifyCodeDialog.value = true
}
const openVerifyDialog = (order) => {
  verifyOrderInfo.value = order
  showVerifyDialog.value = true
}
const sellerOrders = ref([])
const favoriteList = ref([])
const browseHistoryList = ref([])
const receivedCommentList = ref([])
const myForumPosts = ref([])
const forumNotices = ref([])
const forumUnread = ref(0)

const openForumPost = (id) => router.push('/forum?postId=' + id)

const loadForumUnread = async () => {
  if (!localStorage.getItem('token')) return
  try {
    const n = await getUnreadNotificationCount()
    forumUnread.value = Number(n) || 0
  } catch (e) {
    forumUnread.value = 0
  }
}

// ==================== 电子发票 ====================
// 预加载发票，用于「我买到的」里显示申请/查看按钮
const loadInvoiceMap = async () => {
  if (!localStorage.getItem('token')) return
  try {
    const res = await getMyInvoices({ pageNum: 1, pageSize: 100 })
    const list = (res && res.list) ? res.list : []
    const map = {}
    list.forEach(inv => { map[inv.orderId] = inv })
    invoiceMap.value = map
  } catch (e) {
    invoiceMap.value = {}
  }
}

const openInvoiceDialog = (order) => {
  invoiceOrder.value = order
  showInvoiceDialog.value = true
}

const viewInvoice = (inv) => {
  if (!inv) return
  viewingInvoice.value = inv
  showInvoiceDetail.value = true
}

// ==================== 我的换物 ====================
const handleExchange = async (e, action) => {
  if (exchangeOperatingId.value !== null) return
  const apis = {
    accept: acceptExchange,
    reject: rejectExchange,
    cancel: cancelExchange,
    finish: finishExchange
  }
  const otherDone = exchangeTab.value === 'received' ? e.requesterDone === 1 : e.ownerDone === 1
  const confirms = {
    accept: {
      title: '接受换物申请',
      message: `接受后，你的「${e.goodsTitle}」和对方的「${e.offerTitle}」都会锁定，该商品的其他待处理申请将自动拒绝。确定接受吗？`,
      confirmButtonText: '确认接受',
      type: 'warning'
    },
    reject: {
      title: '拒绝换物申请',
      message: '拒绝后本次申请将结束，双方商品状态不变。确定拒绝吗？',
      confirmButtonText: '确认拒绝',
      type: 'warning'
    },
    cancel: {
      title: '取消换物申请',
      message: '取消后本次申请将结束，双方商品状态不变。确定取消吗？',
      confirmButtonText: '确认取消',
      type: 'warning'
    },
    finish: {
      title: '确认完成换物',
      message: otherDone
        ? '对方已确认完成。请确认你已收到并验收对方商品；本次确认后，两件商品将同时成交。'
        : '请确认你已收到并验收对方商品。双方均确认后，两件商品才会同时成交。',
      confirmButtonText: '已验收，确认完成',
      type: 'warning'
    }
  }
  const confirm = confirms[action]
  if (!confirm) return

  try {
    await ElMessageBox.confirm(confirm.message, confirm.title, {
      confirmButtonText: confirm.confirmButtonText,
      cancelButtonText: '再想想',
      type: confirm.type
    })
  } catch (err) {
    return
  }

  exchangeOperatingId.value = e.id
  exchangeOperatingAction.value = action
  try {
    const msg = await apis[action](e.id)
    if (typeof msg === 'string' && /(无法|不存在|已处理|只有|尚未|无权|不能|失败)/.test(msg)) {
      ElMessage.warning(msg)
      return
    }
    ElMessage.success(typeof msg === 'string' && msg ? msg : '操作成功')
    await loadTabData('exchanges')
  } catch (err) {
    // 拦截器已提示
  } finally {
    exchangeOperatingId.value = null
    exchangeOperatingAction.value = ''
  }
}

// 个人中心数据概览
const stats = ref({ published: 0, onSale: 0, sold: 0, favorites: 0, reviews: 0 })
const statItems = [
  { key: 'published', label: '发布', tab: 'published' },
  { key: 'onSale', label: '在售', tab: 'published' },
  { key: 'sold', label: '已售', tab: 'published' },
  { key: 'favorites', label: '收藏', tab: 'favorites' },
  { key: 'reviews', label: '收到评价', tab: 'receivedComments' }
]

// 评价弹窗
const showCommentDialog = ref(false)
const commentTargetOrder = ref(null)

// 各 Tab 的分页状态（单一数据源，避免本地切片与后端分页混用）
const pageState = reactive({
  published: { pageNum: 1, pageSize: 10, total: 0 },
  bought: { pageNum: 1, pageSize: 10, total: 0 },
  sold: { pageNum: 1, pageSize: 10, total: 0 },
  favorites: { pageNum: 1, pageSize: 10, total: 0 },
  browseHistory: { pageNum: 1, pageSize: 10, total: 0 },
  receivedComments: { pageNum: 1, pageSize: 10, total: 0 },
  myPosts: { pageNum: 1, pageSize: 10, total: 0 },
  forumNotices: { pageNum: 1, pageSize: 10, total: 0 },
  invoices: { pageNum: 1, pageSize: 10, total: 0 },
  exchanges: { pageNum: 1, pageSize: 10, total: 0 }
})

const showReturnDialog = ref(false)
const returnOrder = ref(null)
const showAuditDialog = ref(false)
const auditOrder = ref(null)

// 交易仲裁（风控）
const showDisputeDialog = ref(false)
const disputeOrder = ref(null)
const openDisputeDialog = (order) => {
  disputeOrder.value = order
  showDisputeDialog.value = true
}
const handleDisputeDone = () => {
  loadTabData('bought')
  loadTabData('sold')
}

// 编辑资料
const showEditDialog = ref(false)
const handleProfileSaved = (data) => {
  userInfo.value = { ...userInfo.value, ...data }
}

const handleReturnDone = () => loadTabData('bought')
const handleAuditDone = () => loadTabData('sold')
const handleCommentDone = () => {
  loadTabData('bought')
  loadTabData('sold')
}
const handleInvoiceDone = (inv) => {
  if (!inv) return
  invoiceMap.value = { ...invoiceMap.value, [inv.orderId]: inv }
  invoices.value = [inv, ...invoices.value]
  pageState.invoices.total += 1
}

// 切换Tab
const switchTab = (key) => {
  // 切 Tab 时回到第 1 页，再按新页码拉取（分页状态单一数据源）
  pageState[key].pageNum = 1
  activeTab.value = key
  loadTabData(key)
}

// 翻页（仅切换页码，不重置）
const handlePageChange = (key, page) => {
  pageState[key].pageNum = page
  loadTabData(key)
}

// 加载Tab数据（分页，后端返回 { list, total, pageNum, pageSize, pages }）
const loadTabData = async (key) => {
  const selfUid = user.value && user.value.id
  const ps = pageState[key]
  const params = { pageNum: ps.pageNum, pageSize: ps.pageSize }
  try {
    let res
    switch (key) {
      case 'published': {
        if (isSelf.value) {
          if (!selfUid) return
          res = await getMyGoods(selfUid, params)
        } else {
          // 他人主页：拉取该卖家公开在售商品（不暴露已售/下架）
          res = await getGoodsList({ sellerId: targetUserId.value, ...params })
        }
        myGoodsList.value = (res && res.list) ? res.list : (Array.isArray(res) ? res : [])
        break
      }
      case 'bought': {
        if (!selfUid) return
        res = await getBuyerOrders(selfUid, params)
        buyerOrders.value = (res && res.list) ? res.list : (Array.isArray(res) ? res : [])
        break
      }
      case 'sold': {
        if (!selfUid) return
        res = await getSellerOrders(selfUid, params)
        sellerOrders.value = (res && res.list) ? res.list : (Array.isArray(res) ? res : [])
        break
      }
      case 'favorites': {
        if (!selfUid) return
        res = await getMyFavorites(selfUid, params)
        favoriteList.value = (res && res.list) ? res.list : (Array.isArray(res) ? res : [])
        break
      }
      case 'browseHistory': {
        if (!selfUid) return
        res = await getMyBrowseHistory(params)
        browseHistoryList.value = (res && res.list) ? res.list : (Array.isArray(res) ? res : [])
        break
      }
      case 'receivedComments': {
        if (isSelf.value) {
          if (!selfUid) return
          res = await getMyComments(selfUid, params)
        } else {
          // 店铺模式：查询该卖家公开收到的评价
          res = await getSellerComments(targetUserId.value, params)
        }
        receivedCommentList.value = (res && res.list) ? res.list : (Array.isArray(res) ? res : [])
        break
      }
      case 'exchanges': {
        const [received, sent] = await Promise.all([getReceivedExchanges(), getSentExchanges()])
        exchangesReceived.value = Array.isArray(received) ? received : []
        exchangesSent.value = Array.isArray(sent) ? sent : []
        break
      }
      case 'invoices': {
        res = await getMyInvoices(params)
        invoices.value = (res && res.list) ? res.list : (Array.isArray(res) ? res : [])
        invoices.value.forEach(inv => { invoiceMap.value[inv.orderId] = inv })
        break
      }
      case 'myPosts': {
        res = await getMyPosts(params)
        myForumPosts.value = (res && res.list) ? res.list : (Array.isArray(res) ? res : [])
        break
      }
      case 'forumNotices': {
        res = await getMyNotifications(params)
        forumNotices.value = (res && res.list) ? res.list : (Array.isArray(res) ? res : [])
        // 打开通知列表即视为已读，红点清零
        if (res && res.list && res.list.length && ps.pageNum === 1) {
          readNotifications().catch(() => {})
          forumUnread.value = 0
        }
        break
      }
    }
    ps.total = (res && typeof res.total === 'number') ? res.total : 0
  } catch (e) {
    ElMessage.error('加载失败，请重试')
  }
}

// 商品操作
const handleOffShelf = async (id) => {
  try {
    await updateGoodsStatus(id, 'off_shelf')
    ElMessage.success('已下架')
    loadTabData('published')
  } catch (e) { /* error handled by interceptor */ }
}

const handleOnShelf = async (id) => {
  try {
    await updateGoodsStatus(id, 'on_sale')
    ElMessage.success('已重新上架')
    loadTabData('published')
  } catch (e) { /* error handled by interceptor */ }
}

const handleDeleteGoods = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该商品吗？删除后不可恢复', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteGoods(id)
    ElMessage.success('删除成功')
    loadTabData('published')
  } catch (e) { /* cancelled or error */ }
}

// 一键复制：同款多件快速上新，只需指定复制份数
const handleCopyGoods = async (item) => {
  try {
    const { value } = await ElMessageBox.prompt(
      '将复制该商品的信息生成新的在售商品，只需指定份数即可快速上新。',
      '一键复制商品',
      {
        confirmButtonText: '复制',
        cancelButtonText: '取消',
        inputValue: '1',
        inputPattern: /^([1-9]|10)$/,
        inputErrorMessage: '请输入 1-10 之间的数量',
        inputPlaceholder: '复制份数'
      }
    )
    const res = await copyGoods(item.id, Number(value))
    ElMessage.success((res && res.message) || '复制成功')
    loadTabData('published')
  } catch (e) { /* cancelled or error */ }
}

// ============ 商品编辑弹窗 ============
const showEditGoodsDialog = ref(false)
const editGoodsTarget = ref(null)
const openEditGoods = (goods) => {
  editGoodsTarget.value = goods
  showEditGoodsDialog.value = true
}

// 订单操作
const handleConfirmOrder = async (id) => {
  try {
    await ElMessageBox.confirm('确认接单？', '提示', {
      confirmButtonText: '确定接单',
      cancelButtonText: '取消',
      type: 'info'
    })
    await confirmOrder(id)
    ElMessage.success('已接单')
    loadTabData('sold')
  } catch (e) { /* cancelled or error */ }
}

const handleCompleteOrder = async (id) => {
  try {
    await ElMessageBox.confirm('确认已收到商品？', '提示', {
      confirmButtonText: '确认收货',
      cancelButtonText: '取消',
      type: 'info'
    })
    await completeOrder(id)
    ElMessage.success('已确认收货，托管资金已结算给卖家')
    loadTabData('bought')
  } catch (e) { /* cancelled or error */ }
}

const handleCancelOrder = async (id) => {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
      confirmButtonText: '确定取消',
      cancelButtonText: '再想想',
      type: 'warning'
    })
    await cancelOrder(id)
    ElMessage.success('已取消订单')
    loadTabData('bought')
  } catch (e) { /* cancelled or error */ }
}

// 模拟支付：打开收银台
const showPayDialog = ref(false)
const payTargetOrder = ref(null)

const openPayDialog = (order) => {
  payTargetOrder.value = order
  showPayDialog.value = true
}

// 评价：买家打开评价弹窗
const openCommentDialog = (order) => {
  commentTargetOrder.value = order
  showCommentDialog.value = true
}

// 退货：买家打开申请弹窗
const openReturnDialog = (order) => {
  returnOrder.value = order
  showReturnDialog.value = true
}

// 卖家打开退货审核弹窗
const openAuditDialog = (order) => {
  auditOrder.value = order
  showAuditDialog.value = true
}

// 退出登录
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    // 关闭实时通道并清空未读，避免换账号后仍接收上一个账号的推送
    disconnectRealtime()
    setUnread(0)
    router.push('/login')
  } catch (e) { /* cancelled */ }
}

// 联系卖家（进店场景下没有具体商品，仅带 sellerId）
const contactSeller = () => {
  router.push(`/message?sellerId=${targetUserId.value}`)
}

// 个人中心数据概览
const loadStats = async () => {
  if (!isSelf.value) return
  try {
    const res = await getUserStats()
    if (res) {
      stats.value = {
        published: res.published || 0,
        onSale: res.onSale || 0,
        sold: res.sold || 0,
        favorites: res.favorites || 0,
        reviews: res.reviews || 0
      }
    }
  } catch (e) {
    // 概览加载失败不影响主页其他内容
  }
}

// 点击数据卡片跳到对应 Tab
const goStatTab = (key) => {
  if (!tabs.value.some(t => t.key === key)) return
  switchTab(key)
}

// 加载当前主页数据（自己 / 他人）
const loadProfile = async () => {
  activeTab.value = 'published'
  if (isSelf.value) {
    const uid = user.value.id
    if (!uid) {
      router.push('/login')
      return
    }
    userInfo.value = { ...user.value }
    // 尝试从后端获取最新用户信息
    try {
      const res = await getUserInfo(uid)
      if (res) {
        userInfo.value = res
        localStorage.setItem('user', JSON.stringify(res))
      }
    } catch (e) {
      // 使用本地缓存
    }
    loadStats()
  } else {
    // 他人主页：拉取卖家公开信息，不写入本地登录缓存
    userInfo.value = {}
    try {
      const res = await getUserInfo(targetUserId.value)
      if (res) userInfo.value = res
    } catch (e) {
      ElMessage.error('用户信息加载失败')
    }
  }
  // 重置列表与分页，避免残留上一个查看对象的数据
  myGoodsList.value = []
  Object.keys(pageState).forEach(k => {
    pageState[k].pageNum = 1
    pageState[k].total = 0
  })
  loadTabData('published')
}

// 卖家数据看板
const sellerStats = ref(null)
const sellerStatsRecentMax = computed(() => {
  if (!sellerStats.value?.recentTrend?.length) return 1
  return Math.max(1, ...sellerStats.value.recentTrend.map(d => d.count || 0))
})
const loadSellerStats = async () => {
  if (!isSelf.value) return
  try {
    const res = await getSellerStats()
    sellerStats.value = res || null
  } catch (e) {
    sellerStats.value = null
  }
}

// 初始化
onMounted(() => {
  loadProfile()
  loadSellerStats()
  loadForumUnread()
  loadInvoiceMap()
})

// 路由 query.userId 变化时重新加载（切换查看不同卖家）
watch(() => route.query.userId, () => {
  loadProfile()
})
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background-color: #fff;
  background-image: linear-gradient(
    180deg,
    #FF0F23 0px,
    #FF3B3B 60px,
    #FF9090 140px,
    #FFD4D4 214px,
    #FFFFFF 290px
  );
  background-repeat: no-repeat;
}

/* 宽屏 / 中屏：为左侧导航让出空间 */
@media (min-width: 1100px) {
  .profile-page {
    padding-left: 360px;
  }
}

@media (min-width: 768px) and (max-width: 1099px) {
  .profile-page {
    padding-left: 72px;
  }
}

/* === 页面骨架：左侧菜单 + 右侧内容 === */
.profile-shell {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  max-width: 1120px;
  margin: 0 auto;
  padding: 16px 16px 90px;
}
.profile-shell.shop-mode {
  display: block;
  max-width: 920px;
}

.profile-main {
  flex: 1;
  min-width: 0;
}

/* === Tab区域 - 下划线式 === */
.tabs-area {
  margin: 14px 0 0;
  background: #fff;
  border: 1px solid #DCDCDC;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}
.tab-header {
  display: flex;
  border-bottom: 1px solid #f0f0f0;
  overflow-x: auto;
  scrollbar-width: none;
}
.tab-header::-webkit-scrollbar {
  display: none;
}
.tab-item {
  flex: 1 0 auto;
  min-width: 88px;
  text-align: center;
  padding: 14px 10px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
  font-weight: 500;
  white-space: nowrap;
}
.tab-item.active {
  color: #E1251B;
  font-weight: 600;
}
.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 28px;
  height: 3px;
  background: #E1251B;
  border-radius: 2px;
}

.tab-content {
  padding: 12px;
  min-height: 300px;
}
/* === 移动端 === */
@media (max-width: 560px) {
  .profile-shell {
    display: block;
    padding: 10px 10px 80px;
  }
  .tab-header {
    overflow-x: auto;
    scrollbar-width: none;
  }
  .tab-header::-webkit-scrollbar {
    display: none;
  }
  .tab-item {
    flex-shrink: 0;
    min-width: 84px;
    padding: 12px 8px;
    font-size: 13px;
  }
}

</style>
