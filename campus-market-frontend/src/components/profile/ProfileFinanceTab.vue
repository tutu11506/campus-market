<template>
  <!-- 我的发票 -->
  <div v-show="activeTab === 'invoices'" class="tab-panel">
    <div v-if="invoices.length === 0" class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ddd" stroke-width="1.5">
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
        <path d="M14 2v6h6"></path>
        <path d="M8 13h8M8 17h5"></path>
      </svg>
      <p>还没有开过发票</p>
      <span class="invoice-empty-tip">在「我买到的」里对已完成订单申请开票</span>
    </div>
    <div v-else class="invoice-list">
      <div class="invoice-card" v-for="inv in invoices" :key="'inv-' + inv.id" @click="emit('view-invoice', inv)">
        <div class="inv-badge">票</div>
        <div class="inv-main">
          <div class="inv-title">{{ inv.title }}</div>
          <div class="inv-meta">
            <span class="inv-no">发票号 {{ inv.invoiceNo }}</span>
            <span class="inv-order">订单 {{ inv.orderNo }}</span>
          </div>
          <div class="inv-goods">{{ inv.goodsTitle || '校园二手交易' }}</div>
        </div>
        <div class="inv-right">
          <span class="inv-amount">¥{{ inv.amount }}</span>
          <span class="inv-status">{{ inv.status === 'issued' ? '已开票' : '开票中' }}</span>
          <span class="inv-time">{{ formatTime(inv.issueTime || inv.createTime) }}</span>
        </div>
      </div>
    </div>
    <div class="pagination-wrap" v-if="pageState.invoices.total > 0">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="pageState.invoices.total"
        :current-page="pageState.invoices.pageNum"
        :page-size="pageState.invoices.pageSize"
        @current-change="(p) => emit('page-change', 'invoices', p)"
      />
    </div>
  </div>

  <!-- 我的换物 -->
  <div v-show="activeTab === 'exchanges'" class="tab-panel">
    <el-alert
      class="ex-guide"
      type="info"
      :closable="false"
      show-icon
      title="换物流程：发起申请 → 卖家接受并锁定商品 → 线下交换验收 → 双方确认完成"
    />
    <div class="ex-switch">
      <span class="ex-switch-item" :class="{ active: exchangeTab === 'received' }" @click="emit('switch-exchange-tab', 'received')">收到的申请</span>
      <span class="ex-switch-item" :class="{ active: exchangeTab === 'sent' }" @click="emit('switch-exchange-tab', 'sent')">我发起的</span>
    </div>

    <div v-if="currentExchanges.length === 0" class="empty-state">
      <p>{{ exchangeTab === 'received' ? '还没有人申请换你的商品' : '你还没有发起过换物' }}</p>
      <span class="invoice-empty-tip">在支持换物的商品详情页点「申请换物」即可发起</span>
    </div>

    <div class="ex-list" v-else>
      <div class="ex-item" v-for="e in currentExchanges" :key="'ex-' + e.id">
        <SmartImage :src="e.offerCover" class="ex-thumb" />
        <span class="ex-arrow">⇄</span>
        <SmartImage :src="e.goodsCover" class="ex-thumb" />
        <div class="ex-info">
          <div class="ex-line"><b>拿出</b> {{ e.offerTitle }} <span class="ex-price">参考 ¥{{ e.offerPrice }}</span></div>
          <div class="ex-line"><b>想换</b> {{ e.goodsTitle }} <span class="ex-price">参考 ¥{{ e.goodsPrice }}</span></div>
          <div class="ex-msg" v-if="e.message">「{{ e.message }}」</div>
          <div class="ex-meta">
            <span>{{ e.requesterName || '同学' }}</span>
            <span class="ex-status" :class="e.status">{{ exStatusText(e.status) }}</span>
          </div>
          <div class="ex-item-tip" v-if="exchangeHint(e)">{{ exchangeHint(e) }}</div>
        </div>
        <div class="ex-actions">
          <template v-if="exchangeTab === 'received' && e.status === 'pending'">
            <button class="ex-btn primary" :disabled="exchangeOperatingId !== null" @click="emit('exchange-action', e, 'accept')">
              {{ exchangeOperatingId === e.id && exchangeOperatingAction === 'accept' ? '处理中...' : '接受' }}
            </button>
            <button class="ex-btn" :disabled="exchangeOperatingId !== null" @click="emit('exchange-action', e, 'reject')">
              {{ exchangeOperatingId === e.id && exchangeOperatingAction === 'reject' ? '处理中...' : '拒绝' }}
            </button>
          </template>
          <template v-else-if="exchangeTab === 'sent' && e.status === 'pending'">
            <button class="ex-btn" :disabled="exchangeOperatingId !== null" @click="emit('exchange-action', e, 'cancel')">
              {{ exchangeOperatingId === e.id && exchangeOperatingAction === 'cancel' ? '取消中...' : '取消申请' }}
            </button>
          </template>
          <template v-else-if="e.status === 'accepted'">
            <button
              class="ex-btn primary"
              :disabled="exchangeOperatingId !== null || (exchangeTab === 'received' ? e.ownerDone === 1 : e.requesterDone === 1)"
              @click="emit('exchange-action', e, 'finish')"
            >
              {{ exchangeOperatingId === e.id && exchangeOperatingAction === 'finish' ? '确认中...' : ((exchangeTab === 'received' ? e.ownerDone === 1 : e.requesterDone === 1) ? '等待对方确认' : '确认完成') }}
            </button>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import SmartImage from '../SmartImage.vue'

const props = defineProps({
  activeTab: { type: String, default: 'invoices' },
  invoices: { type: Array, default: () => [] },
  currentExchanges: { type: Array, default: () => [] },
  exchangeTab: { type: String, default: 'received' },
  exchangeOperatingId: { type: Number, default: null },
  exchangeOperatingAction: { type: String, default: '' },
  pageState: { type: Object, required: true }
})
const emit = defineEmits(['view-invoice', 'switch-exchange-tab', 'exchange-action', 'page-change'])

const exStatusText = (s) => {
  if (s === 'accepted') return '已接受 · 待完成'
  if (s === 'completed') return '换物已完成'
  if (s === 'rejected') return '已拒绝'
  if (s === 'cancelled') return '已取消'
  return '待处理'
}
const exchangeHint = (e) => {
  if (e.status === 'pending') {
    return props.exchangeTab === 'received'
      ? '接受后双方商品将锁定，该商品的其他待处理申请会自动拒绝。'
      : '等待卖家处理；对方接受前可以取消申请。'
  }
  if (e.status === 'accepted') {
    const myDone = props.exchangeTab === 'received' ? e.ownerDone === 1 : e.requesterDone === 1
    return myDone
      ? '你已完成确认，请等待对方验收并确认。'
      : '请在线下交换并验收商品，确认无误后点击「确认完成」。'
  }
  if (e.status === 'completed') return '双方均已确认，两件商品已完成换物。'
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

.invoice-empty-tip {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #B0B4BA;
}

.invoice-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.invoice-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid #F0F1F3;
  border-radius: 10px;
  cursor: pointer;
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}
.invoice-card:hover {
  border-color: #BFDBFE;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.08);
}
.inv-badge {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  color: #2563EB;
  background: #EFF6FF;
  font-size: 16px;
  font-weight: 800;
}
.inv-main {
  flex: 1;
  min-width: 0;
}
.inv-title {
  font-size: 14px;
  font-weight: 700;
  color: #1F2937;
}
.inv-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 3px;
  font-size: 11px;
  color: #9CA3AF;
}
.inv-goods {
  margin-top: 4px;
  font-size: 12px;
  color: #6B7280;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.inv-right {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 3px;
}
.inv-amount {
  font-size: 15px;
  font-weight: 800;
  color: #E1251B;
}
.inv-status {
  font-size: 10px;
  color: #16A34A;
  background: #ECFDF3;
  padding: 1px 7px;
  border-radius: 8px;
}
.inv-time {
  font-size: 11px;
  color: #B0B4BA;
}

/* ==================== 我的换物 ==================== */
.ex-guide {
  margin-bottom: 14px;
}
.ex-guide :deep(.el-alert__title) {
  font-size: 12px;
  line-height: 1.6;
}
.ex-switch {
  display: inline-flex;
  gap: 4px;
  padding: 3px;
  margin-bottom: 12px;
  border-radius: 18px;
  background: #F5F6F8;
}
.ex-switch-item {
  padding: 6px 16px;
  border-radius: 15px;
  font-size: 12.5px;
  color: #6B7280;
  cursor: pointer;
}
.ex-switch-item.active {
  color: #E1251B;
  font-weight: 700;
  background: #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
}
.ex-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.ex-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid #F0F1F3;
  border-radius: 10px;
}
.ex-thumb {
  width: 52px;
  height: 52px;
  flex-shrink: 0;
  border-radius: 8px;
  object-fit: cover;
  background: #F5F5F5;
}
.ex-arrow {
  flex-shrink: 0;
  font-size: 16px;
  color: #7C3AED;
}
.ex-info {
  flex: 1;
  min-width: 0;
}
.ex-line {
  font-size: 12px;
  color: #6B7280;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ex-line b {
  display: inline-block;
  margin-right: 3px;
  padding: 0 5px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
  color: #7C3AED;
  background: #F5F3FF;
}
.ex-price {
  margin-left: 4px;
  color: #B0B4BA;
  font-size: 11px;
}
.ex-msg {
  margin-top: 4px;
  font-size: 11.5px;
  color: #9CA3AF;
}
.ex-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 5px;
  font-size: 11px;
  color: #9CA3AF;
}
.ex-item-tip {
  margin-top: 5px;
  color: #8B5CF6;
  font-size: 11px;
  line-height: 1.5;
}
.ex-status {
  padding: 1px 7px;
  border-radius: 8px;
  color: #B45309;
  background: #FFFBEB;
}
.ex-status.accepted {
  color: #2563EB;
  background: #EFF6FF;
}
.ex-status.completed {
  color: #16A34A;
  background: #ECFDF3;
}
.ex-status.rejected,
.ex-status.cancelled {
  color: #9CA3AF;
  background: #F3F4F6;
}
.ex-actions {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.ex-btn {
  padding: 4px 12px;
  border: 1px solid #E5E7EB;
  border-radius: 12px;
  background: #fff;
  color: #6B7280;
  font-size: 12px;
  cursor: pointer;
}
.ex-btn.primary {
  border: none;
  background: #E1251B;
  color: #fff;
}
.ex-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 16px 0 8px;
}
</style>
