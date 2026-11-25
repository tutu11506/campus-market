<template>
  <transition name="gift-fade">
    <div class="gift-mask" v-if="visible" @click.self="close">
      <div class="gift-card">
        <button class="gift-close" title="关闭" @click="close">✕</button>

        <div class="gift-head">
          <div class="gift-badge">🎁</div>
          <div class="gift-title">校园专属券礼包</div>
          <div class="gift-sub">{{ coupons.length }} 张不同档位优惠券，领取后下单自动抵扣</div>
        </div>

        <div class="gift-grid">
          <div class="gift-item" v-for="c in coupons" :key="'cp-' + c.id">
            <div class="gi-amount"><span class="gi-yen">¥</span>{{ fmt(c.discountAmount) }}</div>
            <div class="gi-cond">满 {{ fmt(c.thresholdAmount) }} 可用</div>
            <div class="gi-tag">全场通用</div>
          </div>
        </div>

        <div class="gift-note">有效期 30 天 · 每个订单限用一张 · 全场通用</div>

        <div class="gift-actions">
          <button class="gift-btn" :disabled="claiming" @click="claimAll">
            {{ claiming ? '领取中...' : '一键领取' }}
          </button>
          <button class="gift-skip" @click="close">暂不需要</button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPendingCoupons, claimAllCoupons } from '../api/coupon'

const route = useRoute()
const visible = ref(false)
const coupons = ref([])
const claiming = ref(false)

const fmt = (v) => {
  const n = Number(v || 0)
  return Number.isInteger(n) ? String(n) : n.toFixed(2)
}

let loaded = false
// 本次打开页面内点过“暂不需要”就不再打扰；刷新页面后会重新出现（未领取时）
let dismissed = false

const load = async () => {
  if (loaded || dismissed) return
  if (route.path !== '/' || !localStorage.getItem('token')) return
  loaded = true
  try {
    const res = await getPendingCoupons()
    const list = Array.isArray(res) ? res : []
    if (!list.length) return
    coupons.value = list
    visible.value = true
  } catch (e) {
    loaded = false
  }
}

const close = () => {
  dismissed = true
  visible.value = false
}

const claimAll = async () => {
  claiming.value = true
  try {
    const res = await claimAllCoupons()
    const n = res && res.claimed ? res.claimed : coupons.value.length
    visible.value = false
    ElMessage.success(`已领取 ${n} 张优惠券，下单时自动使用最划算的一张`)
  } catch (e) {
    // 拦截器已提示
  } finally {
    claiming.value = false
  }
}

onMounted(load)

watch(() => route.path, (p) => {
  if (p === '/' && !visible.value) load()
  if (p !== '/' && visible.value) visible.value = false
})
</script>

<style scoped>
.gift-mask {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(17, 24, 39, 0.62);
  backdrop-filter: blur(3px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.gift-card {
  position: relative;
  width: 100%;
  max-width: 620px;
  border-radius: 18px;
  background: #fff;
  overflow: hidden;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.28);
}

.gift-close {
  position: absolute;
  top: 10px;
  right: 12px;
  width: 26px;
  height: 26px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
  font-size: 12px;
  cursor: pointer;
  z-index: 2;
}

.gift-close:hover { background: rgba(255, 255, 255, 0.36); }

.gift-head {
  padding: 26px 24px 22px;
  text-align: center;
  color: #fff;
  background: linear-gradient(135deg, #FF5A3C 0%, #E1251B 62%, #B91C1C 100%);
}

.gift-badge { font-size: 26px; }
.gift-title { margin-top: 6px; font-size: 21px; font-weight: 800; letter-spacing: 1px; }
.gift-sub { margin-top: 6px; font-size: 12px; opacity: 0.92; }

.gift-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  padding: 22px 20px 6px;
}

.gift-item {
  position: relative;
  border: 1px solid #FFD9D3;
  border-radius: 10px;
  background: linear-gradient(180deg, #FFF7F5, #FFFDFC);
  padding: 14px 6px 10px;
  text-align: center;
  overflow: hidden;
}

.gift-item::before,
.gift-item::after {
  content: '';
  position: absolute;
  bottom: 26px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #fff;
  border: 1px solid #FFD9D3;
}

.gift-item::before { left: -6px; }
.gift-item::after { right: -6px; }

.gi-amount {
  color: #E1251B;
  font-size: 26px;
  font-weight: 800;
  line-height: 1;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Arial, sans-serif;
}

.gi-yen { font-size: 13px; font-weight: 700; margin-right: 1px; }

.gi-cond {
  margin-top: 8px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #FFD9D3;
  font-size: 11px;
  color: #9A3412;
}

.gi-tag { margin-top: 7px; font-size: 10px; color: #C2410C; }

.gift-note {
  padding: 14px 20px 0;
  text-align: center;
  font-size: 11px;
  color: #9CA3AF;
}

.gift-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 20px 22px;
}

.gift-btn {
  width: 100%;
  max-width: 320px;
  height: 44px;
  border: none;
  border-radius: 22px;
  background: linear-gradient(135deg, #FF6A00, #E1251B);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 1px;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(225, 37, 27, 0.28);
  transition: transform 0.16s ease;
}

.gift-btn:active { transform: scale(0.97); }
.gift-btn:disabled { opacity: 0.72; cursor: not-allowed; }

.gift-skip {
  border: none;
  background: transparent;
  color: #9CA3AF;
  font-size: 12px;
  cursor: pointer;
}

.gift-skip:hover { color: #6B7280; }

.gift-fade-enter-active,
.gift-fade-leave-active { transition: opacity 0.24s ease; }
.gift-fade-enter-from,
.gift-fade-leave-to { opacity: 0; }

.gift-fade-enter-active .gift-card,
.gift-fade-leave-active .gift-card {
  transition: transform 0.28s cubic-bezier(0.22, 1, 0.36, 1);
}

.gift-fade-enter-from .gift-card,
.gift-fade-leave-to .gift-card { transform: scale(0.94); }

@media (max-width: 640px) {
  .gift-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .gift-title { font-size: 18px; }
}
</style>
