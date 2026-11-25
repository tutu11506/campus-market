<template>
  <div class="admin-page">
    <!-- 顶部标题 -->
    <div class="admin-header">
      <div class="header-back" @click="goBack">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </div>
      <div class="header-title">管理后台</div>
      <div class="header-placeholder"></div>
    </div>

    <!-- 数据统计卡片 -->
    <div class="stats-row">
      <div class="stat-card stat-red">
        <div class="stat-icon-wrap"><el-icon :size="24"><User /></el-icon></div>
        <div class="stat-info">
          <div class="stat-number">{{ statistics.userCount || 0 }}</div>
          <div class="stat-label">用户总数</div>
        </div>
      </div>
      <div class="stat-card stat-blue">
        <div class="stat-icon-wrap"><el-icon :size="24"><Goods /></el-icon></div>
        <div class="stat-info">
          <div class="stat-number">{{ statistics.goodsCount || 0 }}</div>
          <div class="stat-label">商品总数</div>
        </div>
      </div>
      <div class="stat-card stat-green">
        <div class="stat-icon-wrap"><el-icon :size="24"><Document /></el-icon></div>
        <div class="stat-info">
          <div class="stat-number">{{ statistics.orderCount || 0 }}</div>
          <div class="stat-label">订单总数</div>
        </div>
      </div>
      <div class="stat-card stat-purple">
        <div class="stat-icon-wrap"><el-icon :size="24"><Coin /></el-icon></div>
        <div class="stat-info">
          <div class="stat-number">{{ statistics.totalAmount || 0 }}</div>
          <div class="stat-label">交易总额</div>
        </div>
      </div>
      <div class="stat-card stat-orange">
        <div class="stat-icon-wrap"><el-icon :size="24"><TrendCharts /></el-icon></div>
        <div class="stat-info">
          <div class="stat-number">{{ statistics.completionRate ?? 0 }}%</div>
          <div class="stat-label">成交率</div>
        </div>
      </div>
      <div class="stat-card stat-teal">
        <div class="stat-icon-wrap"><el-icon :size="24"><Shop /></el-icon></div>
        <div class="stat-info">
          <div class="stat-number">{{ statistics.activeSellers || 0 }}</div>
          <div class="stat-label">近7天活跃卖家</div>
        </div>
      </div>
    </div>

    <!-- Tab切换 - 下划线式 -->
    <div class="admin-tabs">
      <div class="admin-tab" :class="{ active: activeTab === 'dashboard' }" @click="switchTab('dashboard')">数据看板</div>
      <div class="admin-tab" :class="{ active: activeTab === 'users' }" @click="switchTab('users')">用户管理</div>
      <div class="admin-tab" :class="{ active: activeTab === 'goods' }" @click="switchTab('goods')">商品管理</div>
      <div class="admin-tab" :class="{ active: activeTab === 'donations' }" @click="switchTab('donations')">捐赠接收</div>
      <div class="admin-tab" :class="{ active: activeTab === 'rentals' }" @click="switchTab('rentals')">租赁记录</div>
      <div class="admin-tab" :class="{ active: activeTab === 'risk' }" @click="switchTab('risk')">风控中心</div>
    </div>

    <!-- Tab内容区域：各面板独立子组件自管数据加载 -->
    <div class="admin-content">
      <AdminDashboard :active-tab="activeTab" />
      <AdminUserPanel :active-tab="activeTab" @refresh-stats="loadStatistics" />
      <AdminGoodsPanel :active-tab="activeTab" @refresh-stats="loadStatistics" />
      <AdminDonationPanel :active-tab="activeTab" @refresh-stats="loadStatistics" />
      <AdminRentalPanel :active-tab="activeTab" />
      <AdminRiskPanel v-if="activeTab === 'risk'" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Download, TrendCharts, Shop, ArrowLeft, User, Goods, Document, Coin } from '@element-plus/icons-vue'
import { getStatistics } from '../api/admin'

import AdminDashboard from '../components/admin/AdminDashboard.vue'
import AdminUserPanel from '../components/admin/AdminUserPanel.vue'
import AdminGoodsPanel from '../components/admin/AdminGoodsPanel.vue'
import AdminDonationPanel from '../components/admin/AdminDonationPanel.vue'
import AdminRentalPanel from '../components/admin/AdminRentalPanel.vue'
import AdminRiskPanel from '../components/AdminRiskPanel.vue'

const router = useRouter()

// 当前Tab
const activeTab = ref('dashboard')

// 顶部统计卡片（父组件持有，子组件变更通过 emit('refresh-stats') 通知刷新）
const statistics = ref({
  userCount: 0,
  goodsCount: 0,
  orderCount: 0,
  totalAmount: 0
})

const loadStatistics = async () => {
  try {
    const res = await getStatistics()
    if (res) {
      statistics.value = {
        userCount: res.userCount || 0,
        goodsCount: res.goodsCount || 0,
        orderCount: res.orderCount || 0,
        totalAmount: res.totalAmount || 0,
        completionRate: res.completionRate ?? 0,
        activeSellers: res.activeSellers || 0
      }
    }
  } catch (err) {
    console.error('获取统计数据失败', err)
  }
}

const switchTab = (tab) => {
  activeTab.value = tab
}

const goBack = () => router.back()

// 页面初始化：权限检查 + 顶部统计 + Dashboard 懒加载
onMounted(() => {
  const userStr = localStorage.getItem('user')
  if (!userStr) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    const user = JSON.parse(userStr)
    if (user.role !== 'admin') {
      ElMessage.error('无权限访问管理后台')
      router.push('/')
      return
    }
  } catch (e) {
    router.push('/login')
    return
  }

  loadStatistics()
})
</script>

<style scoped>
.admin-page { min-height: 100vh; background: #f5f5f5; padding-bottom: 40px; }
.admin-header { position: sticky; top: 0; z-index: 100; display: flex; align-items: center; justify-content: space-between; height: 48px; padding: 0 12px; background: #fff; border-bottom: 1px solid #f0f0f0; }
.header-back { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; cursor: pointer; border-radius: 50%; transition: background 0.2s; color: #333; }
.header-back:active { background: #f5f5f5; }
.header-title { font-size: 16px; font-weight: 600; color: #333; }
.header-placeholder { width: 36px; flex-shrink: 0; }
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; padding: 12px; }
.stat-card { background: #fff; border-radius: 8px; padding: 16px 12px; display: flex; align-items: center; gap: 12px; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04); }
.stat-icon-wrap { width: 48px; height: 48px; border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-red .stat-icon-wrap { background: linear-gradient(135deg, #E1251B, #C81E18); color: #fff; }
.stat-blue .stat-icon-wrap { background: linear-gradient(135deg, #2196F3, #42A5F5); color: #fff; }
.stat-green .stat-icon-wrap { background: linear-gradient(135deg, #4CAF50, #66BB6A); color: #fff; }
.stat-purple .stat-icon-wrap { background: linear-gradient(135deg, #9C27B0, #AB47BC); color: #fff; }
.stat-orange .stat-icon-wrap { background: linear-gradient(135deg, #FF7A45, #FFA940); color: #fff; }
.stat-teal .stat-icon-wrap { background: linear-gradient(135deg, #36CFC9, #5CDBD3); color: #fff; }
.stat-info { flex: 1; min-width: 0; }
.stat-number { font-size: 22px; font-weight: 800; color: #333; line-height: 1.2; }
.stat-label { font-size: 12px; color: #999; margin-top: 2px; }
.admin-tabs { display: flex; padding: 0 12px; margin-bottom: 12px; background: #fff; border-radius: 8px 8px 0 0; margin: 0 12px; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04); }
.admin-tab { padding: 14px 24px; font-size: 14px; font-weight: 500; color: #666; cursor: pointer; transition: color 0.2s ease; text-align: center; position: relative; user-select: none; flex: 1; }
.admin-tab.active { color: #E1251B; font-weight: 600; }
.admin-tab.active::after { content: ''; position: absolute; bottom: 0; left: 50%; transform: translateX(-50%); width: 28px; height: 3px; background: #E1251B; border-radius: 2px; }
.admin-content { padding: 0 12px; }
.table-card { background: #fff; border-radius: 0 0 8px 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04); margin: 0 12px; }
.pagination-wrap { display: flex; justify-content: center; padding: 14px 0 10px; }
.tab-panel { width: 100%; overflow-x: auto; }
@media (max-width: 600px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .stat-card { padding: 14px 10px; }
  .stat-number { font-size: 18px; }
  .stat-icon-wrap { width: 40px; height: 40px; }
  .stat-icon-wrap .el-icon { font-size: 20px !important; }
}
</style>
