<template>
  <div class="not-found">
    <div class="nf-card">
      <div class="nf-code">404</div>
      <p class="nf-text">页面走丢了～</p>
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.not-found {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}
.nf-card {
  text-align: center;
}
.nf-code {
  font-size: 80px;
  font-weight: 700;
  color: #E1251B;
  line-height: 1;
}
.nf-text {
  color: #666;
  margin: 16px 0 24px;
  font-size: 16px;
}
</style>
