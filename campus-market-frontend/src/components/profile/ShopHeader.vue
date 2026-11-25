<template>
  <div class="shop-card">
    <div class="shop-banner">
      <span class="shop-bubble b1"></span>
      <span class="shop-bubble b2"></span>
      <span class="shop-bubble b3"></span>
      <span class="shop-bubble b4"></span>
    </div>
    <div class="shop-body">
      <div class="shop-avatar-wrap">
        <img :src="avatarUrl" class="shop-avatar" />
      </div>
      <div class="shop-main">
        <div class="shop-name-row">
          <span class="shop-name">{{ userInfo.username || '未设置昵称' }}</span>
          <span class="shop-badge credit">信用分 {{ userInfo.credit || 0 }}</span>
          <span class="shop-badge on-sale">在售 {{ publishedTotal || 0 }} 件</span>
          <button v-if="isOwnShop" class="shop-action" @click="emit('manage-shop')">
            管理店铺
          </button>
          <button v-else class="shop-action" @click="emit('contact-seller')">
            联系卖家
          </button>
        </div>
        <div class="shop-meta">
          <span>学号：{{ userInfo.studentNo || '未知' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  userInfo: { type: Object, default: () => ({}) },
  avatarUrl: { type: String, default: '' },
  publishedTotal: { type: Number, default: 0 },
  isOwnShop: { type: Boolean, default: false }
})
const emit = defineEmits(['manage-shop', 'contact-seller'])
</script>

<style scoped>
/* === 店铺卡片（闲鱼风格） === */
.shop-card {
  margin: 0;
  background: #fff;
  border: 1px solid #DCDCDC;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.07);
}
.shop-banner {
  position: relative;
  height: 112px;
  overflow: hidden;
  background: linear-gradient(115deg, #FFF8E6 0%, #FFE9EE 48%, #FFF4CE 100%);
}
.shop-bubble {
  position: absolute;
  border-radius: 50%;
  filter: blur(2px);
  opacity: 0.8;
}
.shop-bubble.b1 {
  width: 130px;
  height: 130px;
  right: 16%;
  top: -46px;
  background: #FFD9E3;
}
.shop-bubble.b2 {
  width: 96px;
  height: 96px;
  right: 4%;
  top: 34px;
  background: #FFE7A3;
}
.shop-bubble.b3 {
  width: 72px;
  height: 72px;
  left: 34%;
  bottom: -30px;
  background: #FFD8B8;
}
.shop-bubble.b4 {
  width: 44px;
  height: 44px;
  right: 28%;
  bottom: 10px;
  background: #FFC9D6;
}
.shop-body {
  position: relative;
  display: flex;
  gap: 12px;
  margin-top: -34px;
  padding: 0 16px 16px;
}
.shop-avatar-wrap {
  width: 76px;
  height: 76px;
  flex-shrink: 0;
  overflow: hidden;
  border: 3px solid #fff;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.1);
}
.shop-avatar {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.shop-main {
  flex: 1;
  min-width: 0;
  padding-top: 40px;
}
.shop-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.shop-name {
  max-width: 46%;
  overflow: hidden;
  font-size: 18px;
  font-weight: 700;
  color: #222;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.shop-badge {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
}
.shop-badge.credit {
  background: #FFF6D8;
  color: #A97400;
}
.shop-badge.on-sale {
  background: #FFF1F0;
  color: #E1251B;
}
.shop-action {
  margin-left: auto;
  padding: 7px 18px;
  border: none;
  border-radius: 999px;
  background: #FFD84D;
  color: #5C4300;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.1s ease;
}
.shop-action:hover {
  background: #FFCE1F;
}
.shop-action:active {
  transform: scale(0.96);
}
.shop-meta {
  margin-top: 6px;
  font-size: 12px;
  color: #999;
}
@media (max-width: 520px) {
  .shop-name {
    max-width: 100%;
  }
  .shop-action {
    margin-left: 0;
  }
}
</style>
