<template>
  <!-- 我买到的 -->
  <div v-show="activeTab === 'bought'" class="tab-panel">
    <div v-if="buyerOrders.length === 0" class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ddd" stroke-width="1.5">
        <circle cx="9" cy="21" r="1"></circle>
        <circle cx="20" cy="21" r="1"></circle>
        <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
      </svg>
      <p>还没有买过东西</p>
    </div>
    <div v-else class="order-list">
      <div v-for="order in buyerOrders" :key="order.id" class="order-card">
        <SmartImage :src="order.goodsCover" class="order-cover" alt="" />
        <div class="order-info">
          <div class="order-title">{{ order.goodsTitle }}</div>
          <div class="order-price">&yen;{{ order.price }}</div>
          <div class="order-meta">
            <span class="order-status" :class="order.status">{{ orderStatusText(order) }}</span>
            <span v-if="order.escrowStatus" class="escrow-tag" :class="order.escrowStatus">{{ escrowText(order.escrowStatus) }}</span>
            <span v-if="order.refundStatus && order.status !== 'returned'" class="order-status refund" :class="'rs-' + order.refundStatus">{{ refundStatusMap[order.refundStatus] }}</span>
            <span class="order-time">{{ formatTime(order.createTime) }}</span>
          </div>
        </div>
        <div class="order-actions">
          <button
            v-if="order.status === 'pending' && !order.payTime"
            class="action-btn confirm"
            @click="emit('pay', order)"
          >去支付</button>
          <button
            v-if="order.status === 'pending'"
            class="action-btn cancel"
            @click="emit('cancel', order.id)"
          >取消订单</button>
          <button
            v-if="order.payTime && (order.status === 'pending' || order.status === 'dealing')"
            class="action-btn escrow"
            @click="emit('verify-code', order)"
          >自提核销码</button>
          <button
            v-if="order.status === 'dealing'"
            class="action-btn confirm"
            @click="emit('complete', order.id)"
          >确认收货</button>
          <button
            v-if="order.payTime && (order.status === 'dealing' || order.status === 'completed')"
            class="action-btn dispute"
            @click="emit('dispute', order)"
          >申请仲裁</button>
          <button
            v-if="order.status === 'completed' && order.refundStatus === 'returning'"
            class="action-btn refund-disabled"
            disabled
          >退货审核中</button>
          <button
            v-if="order.status === 'completed' && !order.refundStatus"
            class="action-btn refund"
            @click="emit('return', order)"
          >申请退货</button>
          <button
            v-if="order.status === 'completed' && order.refundStatus === 'return_rejected'"
            class="action-btn refund"
            @click="emit('return', order)"
          >再次申请退货</button>
          <button
            v-if="order.status === 'completed' && !order.isCommented"
            class="action-btn comment"
            @click="emit('comment', order)"
          >去评价</button>
          <span
            v-if="order.status === 'completed' && order.isCommented"
            class="comment-tag done"
          >已评价</span>
          <button
            v-if="order.status === 'completed' && order.refundStatus !== 'returned' && invoiceMap[order.id]"
            class="action-btn invoice"
            @click="emit('view-invoice', invoiceMap[order.id])"
          >查看发票</button>
          <button
            v-else-if="order.status === 'completed' && order.refundStatus !== 'returned'"
            class="action-btn invoice"
            @click="emit('apply-invoice', order)"
          >申请开票</button>
        </div>
      </div>
    </div>
    <div class="pagination-wrap" v-if="pageState.bought.total > 0">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="pageState.bought.total"
        :current-page="pageState.bought.pageNum"
        :page-size="pageState.bought.pageSize"
        @current-change="(p) => emit('page-change', 'bought', p)"
      />
    </div>
  </div>

  <!-- 我卖出的 -->
  <div v-show="activeTab === 'sold'" class="tab-panel">
    <div v-if="sellerOrders.length === 0" class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ddd" stroke-width="1.5">
        <line x1="12" y1="1" x2="12" y2="23"></line>
        <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
      </svg>
      <p>还没有卖出过东西</p>
    </div>
    <div v-else class="order-list">
      <div v-for="order in sellerOrders" :key="order.id" class="order-card">
        <SmartImage :src="order.goodsCover" class="order-cover" alt="" />
        <div class="order-info">
          <div class="order-title">{{ order.goodsTitle }}</div>
          <div class="order-price">&yen;{{ order.price }}</div>
          <div class="order-meta">
            <span class="order-status" :class="order.status">{{ orderStatusText(order) }}</span>
            <span v-if="order.escrowStatus" class="escrow-tag" :class="order.escrowStatus">{{ escrowText(order.escrowStatus) }}</span>
            <span class="order-buyer" v-if="order.buyerName">买家：{{ order.buyerName }}</span>
            <span v-if="order.refundStatus && order.status !== 'returned'" class="order-status refund" :class="'rs-' + order.refundStatus">{{ refundStatusMap[order.refundStatus] }}</span>
            <span v-if="order.isCommented" class="comment-tag done">已评价</span>
            <span class="order-time">{{ formatTime(order.createTime) }}</span>
          </div>
        </div>
        <div class="order-actions">
          <button
            v-if="order.status === 'pending' && !order.payTime"
            class="action-btn refund-disabled"
            disabled
          >等待买家支付</button>
          <button
            v-if="order.status === 'pending' && order.payTime"
            class="action-btn confirm"
            @click="emit('confirm', order.id)"
          >确认接单</button>
          <button
            v-if="order.payTime && (order.status === 'pending' || order.status === 'dealing')"
            class="action-btn escrow"
            @click="emit('verify', order)"
          >核销收款</button>
          <button
            v-if="order.status === 'completed' && !order.isCommented"
            class="action-btn comment"
            @click="emit('comment', order)"
          >去评价</button>
          <button
            v-if="order.refundStatus === 'returning'"
            class="action-btn refund"
            @click="emit('audit', order)"
          >退货审核</button>
          <button
            v-if="order.payTime && (order.status === 'dealing' || order.status === 'completed')"
            class="action-btn dispute"
            @click="emit('dispute', order)"
          >申请仲裁</button>
        </div>
      </div>
    </div>
    <div class="pagination-wrap" v-if="pageState.sold.total > 0">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="pageState.sold.total"
        :current-page="pageState.sold.pageNum"
        :page-size="pageState.sold.pageSize"
        @current-change="(p) => emit('page-change', 'sold', p)"
      />
    </div>
  </div>
</template>

<script setup>
import SmartImage from '../SmartImage.vue'

defineProps({
  activeTab: { type: String, default: 'bought' },
  buyerOrders: { type: Array, default: () => [] },
  sellerOrders: { type: Array, default: () => [] },
  invoiceMap: { type: Object, default: () => ({}) },
  pageState: { type: Object, required: true }
})
const emit = defineEmits([
  'pay', 'cancel', 'verify-code', 'complete', 'dispute', 'return',
  'comment', 'view-invoice', 'apply-invoice', 'confirm', 'verify', 'audit', 'page-change'
])

const orderStatusMap = {
  pending: '待确认',
  dealing: '交易中',
  completed: '已完成',
  cancelled: '已取消',
  returned: '已退货'
}
// 待确认订单按是否支付区分：待支付 / 待卖家接单
const orderStatusText = (order) => {
  if (order.status === 'pending') {
    return order.payTime ? '待卖家接单' : '待支付'
  }
  return orderStatusMap[order.status] || order.status
}
// 退货进度（order.refund_status）
const refundStatusMap = {
  returning: '退货审核中',
  returned: '已退货',
  return_rejected: '退货被拒'
}
const escrowText = (s) => {
  if (s === 'held') return '平台托管中'
  if (s === 'settled') return '已结算给卖家'
  if (s === 'refunded') return '已退款'
  return ''
}
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

/* === 订单卡片 - 白色卡片 8px圆角 === */
.order-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.order-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
.order-cover {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
  background: #f5f5f5;
}
.order-info {
  flex: 1;
  min-width: 0;
}
.order-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}
.order-price {
  font-size: 16px;
  font-weight: 700;
  color: #E1251B;
  margin-bottom: 4px;
}
.order-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.order-status {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}
.order-status.pending {
  background: #fff3e0;
  color: #ff9800;
}
.order-status.dealing {
  background: #e3f2fd;
  color: #2196f3;
}
.order-status.completed {
  background: #e8f5e9;
  color: #4caf50;
}
.order-status.cancelled {
  background: #f5f5f5;
  color: #999;
}
.order-buyer {
  font-size: 12px;
  color: #666;
}
.order-time {
  font-size: 11px;
  color: #bbb;
}
.order-actions {
  flex-shrink: 0;
}
.action-btn {
  padding: 4px 10px;
  border: none;
  border-radius: 4px;
  font-size: 11px;
  cursor: pointer;
  font-family: inherit;
}
.order-actions .action-btn {
  padding: 6px 12px;
  font-size: 12px;
  border-radius: 6px;
}
.order-actions .confirm {
  background: linear-gradient(135deg, #E1251B, #C81E18);
  color: #fff;
  border: none;
}
.order-actions .cancel {
  background: transparent;
  color: #999;
  border: 1px solid #ddd;
}

/* 退货状态徽章 */
.order-status.refund { margin-left: 0; }
.order-status.rs-returning { background: #fff3e0; color: #ff9800; }
.order-status.rs-returned { background: #e8f5e9; color: #4caf50; }
.order-status.rs-return_rejected { background: #ffebee; color: #e1251b; }
.order-actions .refund { background: #fff; color: #E1251B; border: 1px solid #E1251B; }
.order-actions .refund-disabled { background: #f5f5f5; color: #bbb; border: 1px solid #eee; cursor: not-allowed; }

/* ===== 评价功能 ===== */
.comment-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}
.comment-tag.done {
  background: #f0f9eb;
  color: #67c23a;
}
.order-actions .action-btn.comment {
  background: linear-gradient(135deg, #E1251B, #C81E18);
  color: #fff;
  border: none;
}

/* ==================== 电子发票 ==================== */
.action-btn.invoice {
  color: #2563EB;
  border-color: #BFDBFE;
  background: #EFF6FF;
}
.action-btn.invoice:hover {
  background: #DBEAFE;
}

/* 风控：交易仲裁按钮 */
.action-btn.dispute {
  color: #C2410C;
  border-color: #FED7AA;
  background: #FFF7ED;
}
.action-btn.dispute:hover {
  background: #FFEDD5;
}

/* 担保交易与核销 */
.escrow-tag {
  padding: 1px 7px;
  font-size: 10px;
  border-radius: 8px;
  color: #2563EB;
  background: #EFF6FF;
}
.escrow-tag.settled {
  color: #16A34A;
  background: #ECFDF3;
}
.escrow-tag.refunded {
  color: #9CA3AF;
  background: #F3F4F6;
}
.action-btn.escrow {
  color: #7C3AED;
  border-color: #DDD6FE;
  background: #F5F3FF;
}
.action-btn.escrow:hover {
  background: #EDE9FE;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 16px 0 8px;
}
</style>
