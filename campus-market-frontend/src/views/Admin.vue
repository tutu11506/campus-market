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
        <div class="stat-icon-wrap">
          <el-icon :size="24"><User /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-number">{{ statistics.userCount || 0 }}</div>
          <div class="stat-label">用户总数</div>
        </div>
      </div>
      <div class="stat-card stat-blue">
        <div class="stat-icon-wrap">
          <el-icon :size="24"><Goods /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-number">{{ statistics.goodsCount || 0 }}</div>
          <div class="stat-label">商品总数</div>
        </div>
      </div>
      <div class="stat-card stat-green">
        <div class="stat-icon-wrap">
          <el-icon :size="24"><Document /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-number">{{ statistics.orderCount || 0 }}</div>
          <div class="stat-label">订单总数</div>
        </div>
      </div>
      <div class="stat-card stat-purple">
        <div class="stat-icon-wrap">
          <el-icon :size="24"><Coin /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-number">{{ statistics.totalAmount || 0 }}</div>
          <div class="stat-label">交易总额</div>
        </div>
      </div>
      <div class="stat-card stat-orange">
        <div class="stat-icon-wrap">
          <el-icon :size="24"><TrendCharts /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-number">{{ statistics.completionRate ?? 0 }}%</div>
          <div class="stat-label">成交率</div>
        </div>
      </div>
      <div class="stat-card stat-teal">
        <div class="stat-icon-wrap">
          <el-icon :size="24"><Shop /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-number">{{ statistics.activeSellers || 0 }}</div>
          <div class="stat-label">近7天活跃卖家</div>
        </div>
      </div>
    </div>

    <!-- Tab切换 - 下划线式 -->
    <div class="admin-tabs">
      <div
        class="admin-tab"
        :class="{ active: activeTab === 'dashboard' }"
        @click="switchTab('dashboard')"
      >
        数据看板
      </div>
      <div
        class="admin-tab"
        :class="{ active: activeTab === 'users' }"
        @click="switchTab('users')"
      >
        用户管理
      </div>
      <div
        class="admin-tab"
        :class="{ active: activeTab === 'goods' }"
        @click="switchTab('goods')"
      >
        商品管理
      </div>
      <div
        class="admin-tab"
        :class="{ active: activeTab === 'donations' }"
        @click="switchTab('donations')"
      >
        捐赠接收
      </div>
      <div
        class="admin-tab"
        :class="{ active: activeTab === 'rentals' }"
        @click="switchTab('rentals')"
      >
        租赁记录
      </div>
      <div
        class="admin-tab"
        :class="{ active: activeTab === 'risk' }"
        @click="switchTab('risk')"
      >
        风控中心
      </div>
    </div>

    <!-- Tab内容区域 -->
    <div class="admin-content">

      <!-- 数据看板 -->
      <div v-if="activeTab === 'dashboard'" class="tab-panel dashboard-panel" v-loading="dashboardLoading">
        <div class="dashboard-toolbar">
          <div class="period-toggle">
            <span class="period-label">统计周期</span>
            <el-radio-group v-model="dashboardPeriod" size="small" @change="onPeriodChange">
              <el-radio-button value="day">按日</el-radio-button>
              <el-radio-button value="week">按周</el-radio-button>
            </el-radio-group>
          </div>
          <el-button :icon="Download" size="small" type="primary" plain @click="exportCSV">
            导出 CSV
          </el-button>
        </div>
        <div class="charts-grid">
          <div class="chart-card">
            <div class="chart-title">{{ dashboardPeriod === 'week' ? '近 8 周' : '近 7 天' }}新增用户</div>
            <div ref="userTrendRef" class="chart-box"></div>
          </div>
          <div class="chart-card">
            <div class="chart-title">商品分类分布</div>
            <div ref="categoryRef" class="chart-box"></div>
          </div>
          <div class="chart-card wide">
            <div class="chart-title">{{ dashboardPeriod === 'week' ? '近 8 周' : '近 7 天' }}订单与成交额</div>
            <div ref="orderTrendRef" class="chart-box chart-box-wide"></div>
          </div>
        </div>
      </div>

      <!-- 用户管理 -->
      <div v-show="activeTab === 'users'" class="tab-panel">
        <div class="table-card">
          <el-table
            :data="userList"
            stripe
            style="width: 100%"
            v-loading="userLoading"
            :header-cell-style="{ background: '#FAFAFA', color: '#333', fontWeight: 600, fontSize: '13px' }"
            :cell-style="{ fontSize: '13px', color: '#555' }"
            size="default"
          >
            <el-table-column prop="id" label="ID" width="60" align="center" />
            <el-table-column prop="studentNo" label="学号" min-width="110" />
            <el-table-column prop="username" label="昵称" min-width="100">
              <template #default="{ row }">
                <div class="user-cell">
                  <img
                    :src="resolveAvatar(row.avatar)"
                    class="user-avatar"
                    @error="handleAvatarError($event)"
                    alt=""
                  />
                  <span>{{ row.username || '未设置' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="phone" label="手机号" min-width="120" />
            <el-table-column prop="credit" label="信用分" width="80" align="center" />
            <el-table-column prop="status" label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="row.status === 'normal' ? 'success' : 'danger'"
                  size="small"
                  effect="light"
                >
                  {{ row.status === 'normal' ? '正常' : '封禁' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="注册时间" min-width="140">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 'normal'"
                  type="danger"
                  size="small"
                  text
                  @click="handleBanUser(row)"
                >
                  封禁
                </el-button>
                <el-button
                  v-else
                  type="success"
                  size="small"
                  text
                  @click="handleUnbanUser(row)"
                >
                  解封
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrap" v-if="userTotal > 0">
            <el-pagination
              background
              layout="total, prev, pager, next, jumper"
              :total="userTotal"
              :current-page="userPageNum"
              :page-size="userPageSize"
              @current-change="handleUserPageChange"
            />
          </div>
        </div>
      </div>

      <!-- 商品管理 -->
      <div v-show="activeTab === 'goods'" class="tab-panel">
        <div class="table-card">
          <el-table
            :data="goodsList"
            stripe
            style="width: 100%"
            v-loading="goodsLoading"
            :header-cell-style="{ background: '#FAFAFA', color: '#333', fontWeight: 600, fontSize: '13px' }"
            :cell-style="{ fontSize: '13px', color: '#555' }"
            size="default"
          >
            <el-table-column prop="id" label="ID" width="60" align="center" />
            <el-table-column prop="title" label="标题" min-width="160">
              <template #default="{ row }">
                <div class="goods-cell">
                  <SmartImage
                    :src="row.coverImage || row.images"
                    class="goods-thumb"
                    alt=""
                  />
                  <span class="goods-title-text">{{ row.title }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="price" label="价格" width="90" align="center">
              <template #default="{ row }">
                <span class="price-text">&yen;{{ row.price }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="sellerName" label="卖家" min-width="90" />
            <el-table-column prop="status" label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="getStatusType(row.status)"
                  size="small"
                  effect="light"
                >
                  {{ getStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="viewCount" label="浏览量" width="80" align="center" />
            <el-table-column label="操作" width="140" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 'on_sale'"
                  type="warning"
                  size="small"
                  text
                  @click="handleOfflineGoods(row)"
                >
                  下架
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  text
                  @click="handleDeleteGoods(row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrap" v-if="goodsTotal > 0">
            <el-pagination
              background
              layout="total, prev, pager, next, jumper"
              :total="goodsTotal"
              :current-page="goodsPageNum"
              :page-size="goodsPageSize"
              @current-change="handleGoodsPageChange"
            />
          </div>
        </div>
      </div>

      <!-- 公益捐赠接收 -->
      <div v-show="activeTab === 'donations'" class="tab-panel">
        <div class="table-card">
          <div class="donation-filters">
            <el-select v-model="donationStatus" placeholder="全部状态" clearable @change="handleDonationFilter">
              <el-option label="待交接" value="pending" />
              <el-option label="已接收" value="received" />
              <el-option label="已取消" value="cancelled" />
            </el-select>
            <el-select v-model="donationItemType" placeholder="全部品类" clearable @change="handleDonationFilter">
              <el-option label="衣物" value="clothes" />
              <el-option label="书籍" value="books" />
            </el-select>
            <span class="filter-note">实收数量须依据社团回执填写，确认后不可重复操作</span>
          </div>
          <el-table
            :data="donationList"
            stripe
            style="width: 100%"
            v-loading="donationLoading"
            :header-cell-style="{ background: '#FAFAFA', color: '#333', fontWeight: 600, fontSize: '13px' }"
            :cell-style="{ fontSize: '13px', color: '#555' }"
          >
            <el-table-column prop="id" label="ID" width="60" align="center" />
            <el-table-column label="捐赠人" min-width="105">
              <template #default="{ row }">
                <div>{{ row.donorName || '同学' }}</div>
                <div class="cell-sub">{{ row.donorStudentNo || '未填写学号' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="捐赠物资" min-width="140">
              <template #default="{ row }">
                <div>{{ row.itemName }}</div>
                <div class="cell-sub">{{ getDonationItemTypeLabel(row.itemType) }} · {{ getDonationConditionLabel(row.conditionLevel) }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="organizationName" label="接收社团" min-width="125" />
            <el-table-column label="登记数量" width="90" align="center">
              <template #default="{ row }">{{ row.quantity }} {{ getDonationUnit(row.itemType) }}</template>
            </el-table-column>
            <el-table-column label="交接信息" min-width="190" show-overflow-tooltip>
              <template #default="{ row }">
                <div>{{ getHandoverLabel(row.handoverMethod) }}</div>
                <div class="cell-sub">{{ row.handoverDetail }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="contact" label="联系方式" min-width="110" show-overflow-tooltip />
            <el-table-column label="状态" width="86" align="center">
              <template #default="{ row }">
                <el-tag :type="getDonationStatusType(row.status)" size="small" effect="light">
                  {{ getDonationStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="实收数量" width="90" align="center">
              <template #default="{ row }">
                <span v-if="row.status === 'received'">{{ row.receivedQuantity }} {{ getDonationUnit(row.itemType) }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="登记时间" min-width="135">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="105" align="center" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 'pending'" type="success" size="small" text @click="openReceiveDialog(row)">
                  确认实收
                </el-button>
                <span v-else class="cell-sub">已处理</span>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrap" v-if="donationTotal > 0">
            <el-pagination
              background
              layout="total, prev, pager, next, jumper"
              :total="donationTotal"
              :current-page="donationPageNum"
              :page-size="donationPageSize"
              @current-change="handleDonationPageChange"
            />
          </div>
        </div>
      </div>
      <!-- 租赁记录 -->
      <div v-show="activeTab === 'rentals'" class="tab-panel">
        <div class="table-card">
          <div class="donation-filters">
            <el-select v-model="rentalStatus" placeholder="全部状态" clearable @change="handleRentalFilter">
              <el-option label="待确认" value="pending" />
              <el-option label="租用中" value="renting" />
              <el-option label="待收回" value="returning" />
              <el-option label="已完成" value="completed" />
              <el-option label="已取消" value="cancelled" />
            </el-select>
            <el-select v-model="rentalType" placeholder="全部类型" clearable @change="handleRentalFilter">
              <el-option label="按天租赁" value="daily" />
              <el-option label="按周租赁" value="weekly" />
              <el-option label="假期寄存" value="storage" />
              <el-option label="教材学期租" value="textbook" />
            </el-select>
            <el-select v-model="rentalOverdue" placeholder="全部订单" @change="handleRentalFilter">
              <el-option label="全部订单" value="all" />
              <el-option label="仅看逾期" value="overdue" />
            </el-select>
            <span class="filter-note">归还由出租方核销；逾期由系统标记并提醒双方</span>
          </div>
          <el-table
            :data="rentalList"
            stripe
            style="width: 100%"
            v-loading="rentalLoading"
            :header-cell-style="{ background: '#FAFAFA', color: '#333', fontWeight: 600, fontSize: '13px' }"
            :cell-style="{ fontSize: '13px', color: '#555' }"
          >
            <el-table-column prop="orderNo" label="订单号" min-width="170" show-overflow-tooltip />
            <el-table-column label="租赁物品" min-width="150">
              <template #default="{ row }">
                <div>{{ row.itemTitle }}</div>
                <div class="cell-sub">{{ getRentalTypeLabel(row.rentalType) }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="renterName" label="租客" min-width="90" />
            <el-table-column prop="ownerName" label="出租方" min-width="90" />
            <el-table-column label="租期" min-width="190">
              <template #default="{ row }">
                <div>{{ row.startDate }} 至 {{ row.endDate }}</div>
                <div class="cell-sub">{{ row.days }} 天</div>
              </template>
            </el-table-column>
            <el-table-column label="租金/押金" width="120" align="center">
              <template #default="{ row }">
                <div>¥{{ row.rentAmount }}</div>
                <div class="cell-sub">押金 ¥{{ row.deposit }}</div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="86" align="center">
              <template #default="{ row }">
                <el-tag :type="getRentalStatusType(row.status)" size="small" effect="light">
                  {{ getRentalStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="逾期" width="86" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.overdueDays > 0" type="danger" size="small" effect="light">
                  {{ row.overdueDays }} 天
                </el-tag>
                <span v-else class="cell-sub">-</span>
              </template>
            </el-table-column>
            <el-table-column label="下单时间" min-width="135">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrap" v-if="rentalTotal > 0">
            <el-pagination
              background
              layout="total, prev, pager, next, jumper"
              :total="rentalTotal"
              :current-page="rentalPageNum"
              :page-size="rentalPageSize"
              @current-change="handleRentalPageChange"
            />
          </div>
        </div>
      </div>
      <!-- 风控中心：违禁品识别 / 价格预警 / 纠纷仲裁 / 平台黑名单 -->
      <div v-if="activeTab === 'risk'" class="tab-panel">
        <AdminRiskPanel />
      </div>
    </div>

    <el-dialog
      v-model="receiveDialogVisible"
      title="确认公益物资实收"
      width="92%"
      style="max-width: 480px"
      :close-on-click-modal="false"
    >
      <div v-if="receivingRecord" class="receive-summary">
        <div class="receive-record-title">{{ receivingRecord.itemName }}</div>
        <p>捐赠人：{{ receivingRecord.donorName || '同学' }}（{{ receivingRecord.donorStudentNo || '未填写学号' }}）</p>
        <p>接收社团：{{ receivingRecord.organizationName }}</p>
        <p>登记数量：{{ receivingRecord.quantity }} {{ getDonationUnit(receivingRecord.itemType) }}</p>
        <p>交接信息：{{ getHandoverLabel(receivingRecord.handoverMethod) }} · {{ receivingRecord.handoverDetail }}</p>
        <el-alert title="请先核对社团回执，再填写实际接收数量。确认后该记录将转为已接收。" type="warning" :closable="false" show-icon />
        <div class="receive-quantity">
          <span>实际接收数量</span>
          <el-input-number v-model="receivedQuantity" :min="1" :max="receivingRecord.quantity" :precision="0" />
          <span>{{ getDonationUnit(receivingRecord.itemType) }}</span>
        </div>
      </div>
      <template #footer>
        <el-button :disabled="receiving" @click="receiveDialogVisible = false">取消</el-button>
        <el-button type="success" :loading="receiving" @click="submitReceive">确认实收</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, TrendCharts, Shop, ArrowLeft, User, Goods, Document, Coin } from '@element-plus/icons-vue'
import { getUserList, updateUserStatus, getGoodsList, offlineGoods, deleteGoods, getDonationList, receiveDonation, getRentalList, getStatistics, getDashboard, getExportUrl } from '../api/admin'
import * as echarts from 'echarts/core'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([LineChart, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const router = useRouter()
import { baseURL } from '../config'
import { DEFAULT_AVATAR, resolveAvatar } from '../utils/avatar'
import SmartImage from '../components/SmartImage.vue'
import AdminRiskPanel from '../components/AdminRiskPanel.vue'

// 当前Tab
const activeTab = ref('dashboard')

// 统计数据
const statistics = ref({
  userCount: 0,
  goodsCount: 0,
  orderCount: 0,
  totalAmount: 0
})

// 用户管理
const userList = ref([])
const userLoading = ref(false)
const userPageNum = ref(1)
const userPageSize = ref(10)
const userTotal = ref(0)

// 商品管理
const goodsList = ref([])
const goodsLoading = ref(false)
const goodsPageNum = ref(1)
const goodsPageSize = ref(10)
const goodsTotal = ref(0)

// 公益捐赠接收
const donationList = ref([])
const donationLoading = ref(false)
const donationPageNum = ref(1)
const donationPageSize = ref(10)
const donationTotal = ref(0)
const donationStatus = ref('')
const donationItemType = ref('')
const receiveDialogVisible = ref(false)
const receivingRecord = ref(null)
const receivedQuantity = ref(1)
const receiving = ref(false)

// 租赁记录
const rentalList = ref([])
const rentalLoading = ref(false)
const rentalPageNum = ref(1)
const rentalPageSize = ref(10)
const rentalTotal = ref(0)
const rentalStatus = ref('')
const rentalType = ref('')
const rentalOverdue = ref('all')

// ========== 数据看板 ==========
const dashboardLoading = ref(false)
const dashboardPeriod = ref('day')
const dashboard = ref({ userTrend: [], orderTrend: [], categoryStats: [] })
const userTrendRef = ref(null)
const orderTrendRef = ref(null)
const categoryRef = ref(null)
let chartInstances = []

const disposeCharts = () => {
  chartInstances.forEach(chart => chart.dispose())
  chartInstances = []
}

const renderCharts = () => {
  disposeCharts()
  if (activeTab.value !== 'dashboard') return
  nextTick(() => {
    const userEl = userTrendRef.value
    const orderEl = orderTrendRef.value
    const categoryEl = categoryRef.value
    if (!userEl || !orderEl || !categoryEl) return

    const dates = dashboard.value.userTrend.map(item => item.date)
    const users = dashboard.value.userTrend.map(item => Number(item.count) || 0)
    const orders = dashboard.value.orderTrend.map(item => Number(item.count) || 0)
    const amounts = dashboard.value.orderTrend.map(item => Number(item.amount) || 0)

    const axisLabel = { color: '#999', fontSize: 11 }
    const splitLine = { lineStyle: { color: '#f2f2f2' } }

    // 近 7 天新增用户
    const userChart = echarts.init(userEl)
    userChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 8, right: 16, top: 20, bottom: 4, containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: dates,
        axisLine: { lineStyle: { color: '#eee' } },
        axisLabel
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        splitLine,
        axisLabel
      },
      series: [{
        name: '新增用户',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: users,
        lineStyle: { width: 2, color: '#E1251B' },
        itemStyle: { color: '#E1251B' },
        areaStyle: { color: 'rgba(225, 37, 27, 0.10)' }
      }]
    })

    // 近 7 天订单与成交额
    const orderChart = echarts.init(orderEl)
    orderChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { top: 0, right: 0, itemWidth: 10, itemHeight: 10, textStyle: { color: '#666', fontSize: 11 } },
      grid: { left: 8, right: 8, top: 40, bottom: 4, containLabel: true },
      xAxis: {
        type: 'category',
        data: dates,
        axisLine: { lineStyle: { color: '#eee' } },
        axisLabel
      },
      yAxis: [
        { type: 'value', name: '订单数', minInterval: 1, nameTextStyle: { color: '#999', fontSize: 11 }, splitLine, axisLabel },
        { type: 'value', name: '成交额(¥)', nameTextStyle: { color: '#999', fontSize: 11 }, splitLine: { show: false }, axisLabel }
      ],
      series: [
        {
          name: '订单数',
          type: 'bar',
          barWidth: 18,
          data: orders,
          itemStyle: { color: '#E1251B', borderRadius: [4, 4, 0, 0] }
        },
        {
          name: '成交额',
          type: 'line',
          yAxisIndex: 1,
          smooth: true,
          symbolSize: 6,
          data: amounts,
          lineStyle: { width: 2, color: '#FFA940' },
          itemStyle: { color: '#FFA940' }
        }
      ]
    })

    // 商品分类分布
    const categoryChart = echarts.init(categoryEl)
    categoryChart.setOption({
      color: ['#E1251B', '#FF7A45', '#FFC53D', '#36CFC9', '#597EF7', '#9254DE'],
      tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 件（{d}%）' },
      series: [{
        type: 'pie',
        radius: ['46%', '68%'],
        center: ['50%', '46%'],
        avoidLabelOverlap: true,
        minAngle: 3,
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { color: '#666', fontSize: 11, formatter: '{b}\n{c}件' },
        labelLine: { length: 8, length2: 8 },
        data: dashboard.value.categoryStats.map(item => ({
          name: item.name,
          value: Number(item.count) || 0
        }))
      }]
    })

    chartInstances = [userChart, orderChart, categoryChart]
  })
}

const loadDashboard = async () => {
  dashboardLoading.value = true
  try {
    const res = await getDashboard(dashboardPeriod.value)
    dashboard.value = {
      userTrend: (res && res.userTrend) || [],
      orderTrend: (res && res.orderTrend) || [],
      categoryStats: (res && res.categoryStats) || []
    }
    if (activeTab.value === 'dashboard') renderCharts()
  } catch (e) {
    console.error('加载数据看板失败', e)
  } finally {
    dashboardLoading.value = false
  }
}

const onPeriodChange = () => {
  loadDashboard()
}

const exportCSV = () => {
  const url = getExportUrl(dashboardPeriod.value)
  const a = document.createElement('a')
  a.href = url
  a.download = ''
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  ElMessage.success('正在导出 CSV...')
}

const resizeCharts = () => {
  chartInstances.forEach(chart => chart.resize())
}

watch(activeTab, tab => {
  if (tab === 'dashboard') {
    renderCharts()
  } else {
    disposeCharts()
  }
})

// 图片URL处理
const getImageUrl = (path) => {
  if (!path) return 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0iI2YwZjBmMCI+PHJlY3QgeD0iMyIgeT0iMyIgd2lkdGg9IjE4IiBoZWlnaHQ9IjE4IiByeD0iMiIgcnk9IjIiPjwvcmVjdD48Y2lyY2xlIGN4PSI4LjUiIGN5PSI4LjUiIHI9IjEuNSI+PC9jaXJjbGU+PC9zdmc+'
  if (path.startsWith('http')) return path
  // images字段可能逗号分隔，取第一张
  const firstImg = path.split(',')[0].trim()
  if (firstImg.startsWith('http')) return firstImg
  return baseURL + firstImg
}

// 头像加载失败
const handleAvatarError = (e) => {
  e.target.src = DEFAULT_AVATAR
}

// 缩略图加载失败
const handleThumbError = (e) => {
  e.target.src = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0iI2YwZjBmMCI+PHJlY3QgeD0iMyIgeT0iMyIgd2lkdGg9IjE4IiBoZWlnaHQ9IjE4IiByeD0iMiIgcnk9IjIiPjwvcmVjdD48Y2lyY2xlIGN4PSI4LjUiIGN5PSI4LjUiIHI9IjEuNSI+PC9jaXJjbGU+PC9zdmc+'
}

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}`
}

// 获取商品状态标签类型
const getStatusType = (status) => {
  const map = {
    on_sale: 'success',
    sold: 'info',
    off_shelf: 'warning'
  }
  return map[status] || 'info'
}

// 获取商品状态标签文本
const getStatusLabel = (status) => {
  const map = {
    on_sale: '在售',
    sold: '已售',
    off_shelf: '下架'
  }
  return map[status] || status
}

// 返回
const goBack = () => {
  router.back()
}

// 切换Tab
const switchTab = (tab) => {
  activeTab.value = tab
  if (tab === 'users' && userList.value.length === 0) {
    loadUserList()
  } else if (tab === 'goods' && goodsList.value.length === 0) {
    loadGoodsList()
  } else if (tab === 'donations' && donationList.value.length === 0) {
    loadDonationList()
  } else if (tab === 'rentals' && rentalList.value.length === 0) {
    loadRentalList()
  } else if (tab === 'dashboard' && dashboard.value.userTrend.length === 0) {
    loadDashboard()
  }
}

// 加载统计数据
const loadStatistics = async () => {
  try {
    const res = await getStatistics()
    if (res) {
      statistics.value = {
        userCount: res.userCount || 0,
        goodsCount: res.goodsCount || 0,
        orderCount: res.orderCount || 0,
        totalAmount: res.totalAmount || 0
      }
    }
  } catch (err) {
    console.error('获取统计数据失败', err)
  }
}

// 加载用户列表
const loadUserList = async () => {
  userLoading.value = true
  try {
    const res = await getUserList({ pageNum: userPageNum.value, pageSize: userPageSize.value })
    userList.value = (res && res.list) ? res.list : (Array.isArray(res) ? res : [])
    userTotal.value = (res && typeof res.total === 'number') ? res.total : userList.value.length
  } catch (err) {
    console.error('获取用户列表失败', err)
    userList.value = []
    userTotal.value = 0
  } finally {
    userLoading.value = false
  }
}

const handleUserPageChange = (page) => {
  userPageNum.value = page
  loadUserList()
}

// 加载商品列表（分页）
const loadGoodsList = async () => {
  goodsLoading.value = true
  try {
    const res = await getGoodsList({ pageNum: goodsPageNum.value, pageSize: goodsPageSize.value })
    goodsList.value = (res && res.list) ? res.list : (Array.isArray(res) ? res : [])
    goodsTotal.value = (res && typeof res.total === 'number') ? res.total : goodsList.value.length
  } catch (err) {
    console.error('获取商品列表失败', err)
    goodsList.value = []
    goodsTotal.value = 0
  } finally {
    goodsLoading.value = false
  }
}

const handleGoodsPageChange = (page) => {
  goodsPageNum.value = page
  loadGoodsList()
}

// 公益捐赠列表与实收确认
const loadDonationList = async () => {
  donationLoading.value = true
  try {
    const res = await getDonationList({
      status: donationStatus.value || undefined,
      itemType: donationItemType.value || undefined,
      pageNum: donationPageNum.value,
      pageSize: donationPageSize.value
    })
    donationList.value = res?.list || []
    donationTotal.value = Number(res?.total) || 0
  } catch (err) {
    console.error('获取捐赠记录失败', err)
    donationList.value = []
    donationTotal.value = 0
  } finally {
    donationLoading.value = false
  }
}

const handleDonationFilter = () => {
  donationPageNum.value = 1
  loadDonationList()
}

const handleDonationPageChange = page => {
  donationPageNum.value = page
  loadDonationList()
}

const getDonationItemTypeLabel = type => type === 'books' ? '书籍' : '衣物'
const getDonationUnit = type => type === 'books' ? '本' : '件'
const getDonationConditionLabel = value => ({ new: '全新', good: '良好', fair: '一般' }[value] || value)
const getHandoverLabel = value => value === 'pickup' ? '预约宿舍取件' : '自行送到接收点'
const getDonationStatusType = status => ({ pending: 'warning', received: 'success', cancelled: 'info' }[status] || 'info')
const getDonationStatusLabel = status => ({ pending: '待交接', received: '已接收', cancelled: '已取消' }[status] || status)

// 租赁记录列表
const loadRentalList = async () => {
  rentalLoading.value = true
  try {
    const res = await getRentalList({
      status: rentalStatus.value || undefined,
      type: rentalType.value || undefined,
      overdue: rentalOverdue.value === 'overdue',
      pageNum: rentalPageNum.value,
      pageSize: rentalPageSize.value
    })
    rentalList.value = res?.list || []
    rentalTotal.value = Number(res?.total) || 0
  } catch (err) {
    console.error('获取租赁记录失败', err)
    rentalList.value = []
    rentalTotal.value = 0
  } finally {
    rentalLoading.value = false
  }
}

const handleRentalFilter = () => {
  rentalPageNum.value = 1
  loadRentalList()
}

const handleRentalPageChange = page => {
  rentalPageNum.value = page
  loadRentalList()
}

const getRentalTypeLabel = type => ({
  daily: '按天租赁',
  weekly: '按周租赁',
  storage: '假期寄存',
  textbook: '教材学期租'
}[type] || type)

const getRentalStatusType = status => ({
  pending: 'warning',
  renting: 'primary',
  returning: 'warning',
  completed: 'success',
  cancelled: 'info'
}[status] || 'info')

const getRentalStatusLabel = status => ({
  pending: '待确认',
  renting: '租用中',
  returning: '待收回',
  completed: '已完成',
  cancelled: '已取消'
}[status] || status)

const openReceiveDialog = row => {
  receivingRecord.value = row
  receivedQuantity.value = row.quantity
  receiveDialogVisible.value = true
}

const submitReceive = async () => {
  const record = receivingRecord.value
  if (!record) return
  if (!receivedQuantity.value || receivedQuantity.value < 1 || receivedQuantity.value > record.quantity) {
    ElMessage.warning('实收数量需大于 0 且不能超过登记数量')
    return
  }
  receiving.value = true
  try {
    await receiveDonation(record.id, receivedQuantity.value)
    ElMessage.success('已确认实收，公益统计已更新')
    receiveDialogVisible.value = false
    await loadDonationList()
  } catch (e) {
    // 请求拦截器已提示
  } finally {
    receiving.value = false
  }
}

// 封禁用户
const handleBanUser = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要封禁用户 "${row.username || '未设置'}" 吗？封禁后该用户将无法登录。`, '封禁用户', {
      confirmButtonText: '确定封禁',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await updateUserStatus(row.id, 'ban')
    ElMessage.success('已封禁该用户')
    loadUserList()
    loadStatistics()
  } catch (e) {
    // 取消或错误
  }
}

// 解封用户
const handleUnbanUser = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要解封用户 "${row.username || '未设置'}" 吗？`, '解封用户', {
      confirmButtonText: '确定解封',
      cancelButtonText: '取消',
      type: 'info'
    })
    await updateUserStatus(row.id, 'normal')
    ElMessage.success('已解封该用户')
    loadUserList()
    loadStatistics()
  } catch (e) {
    // 取消或错误
  }
}

// 下架商品
const handleOfflineGoods = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要下架商品 "${row.title}" 吗？`, '下架商品', {
      confirmButtonText: '确定下架',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await offlineGoods(row.id)
    ElMessage.success('已下架该商品')
    loadGoodsList()
    loadStatistics()
  } catch (e) {
    // 取消或错误
  }
}

// 删除商品
const handleDeleteGoods = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除商品 "${row.title}" 吗？删除后不可恢复。`, '删除商品', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'error'
    })
    await deleteGoods(row.id)
    ElMessage.success('已删除该商品')
    loadGoodsList()
    loadStatistics()
  } catch (e) {
    // 取消或错误
  }
}

// 页面初始化
onMounted(() => {
  // 权限检查
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

  // 加载数据
  loadStatistics()
  loadDashboard()
  loadUserList()
  window.addEventListener('resize', resizeCharts)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeCharts)
  disposeCharts()
})
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 40px;
}

/* ====== 顶部导航 ====== */
.admin-header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 12px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.header-back {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 50%;
  transition: background 0.2s;
  color: #333;
}

.header-back:active {
  background: #f5f5f5;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.header-placeholder {
  width: 36px;
  flex-shrink: 0;
}

/* ====== 统计卡片 ====== */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  padding: 12px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.stat-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-red .stat-icon-wrap {
  background: linear-gradient(135deg, #E1251B, #C81E18);
  color: #fff;
}

.stat-blue .stat-icon-wrap {
  background: linear-gradient(135deg, #2196F3, #42A5F5);
  color: #fff;
}

.stat-green .stat-icon-wrap {
  background: linear-gradient(135deg, #4CAF50, #66BB6A);
  color: #fff;
}

.stat-purple .stat-icon-wrap {
  background: linear-gradient(135deg, #9C27B0, #AB47BC);
  color: #fff;
}

.stat-orange .stat-icon-wrap {
  background: linear-gradient(135deg, #FF7A45, #FFA940);
  color: #fff;
}

.stat-teal .stat-icon-wrap {
  background: linear-gradient(135deg, #36CFC9, #5CDBD3);
  color: #fff;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-number {
  font-size: 22px;
  font-weight: 800;
  color: #333;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

/* ====== Tab切换 - 下划线式红色 ====== */
.admin-tabs {
  display: flex;
  padding: 0 12px;
  margin-bottom: 12px;
  background: #fff;
  border-radius: 8px 8px 0 0;
  margin: 0 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.admin-tab {
  padding: 14px 24px;
  font-size: 14px;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  transition: color 0.2s ease;
  text-align: center;
  position: relative;
  user-select: none;
  flex: 1;
}

.admin-tab.active {
  color: #E1251B;
  font-weight: 600;
}

.admin-tab.active::after {
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

/* ====== 内容区域 ====== */
.admin-content {
  padding: 0 12px;
}

.table-card {
  background: #fff;
  border-radius: 0 0 8px 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  margin: 0 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 14px 0 10px;
}

.tab-panel {
  width: 100%;
  overflow-x: auto;
}

.donation-filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.donation-filters :deep(.el-select) {
  width: 150px;
}

.filter-note {
  margin-left: auto;
  color: #999;
  font-size: 12px;
}

.cell-sub {
  margin-top: 3px;
  color: #999;
  font-size: 11px;
}

.receive-record-title {
  margin-bottom: 12px;
  color: #278254;
  font-size: 18px;
  font-weight: 700;
}

.receive-summary p {
  margin: 8px 0;
  color: #666;
  font-size: 13px;
  line-height: 1.6;
}

.receive-summary :deep(.el-alert) {
  margin: 16px 0;
}

.receive-quantity {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #444;
  font-size: 14px;
}

/* ====== 数据看板 ====== */
.dashboard-panel {
  padding: 12px;
  overflow: visible;
}

.dashboard-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding: 10px 14px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.period-toggle {
  display: flex;
  align-items: center;
  gap: 10px;
}

.period-label {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

.charts-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(0, 1fr);
  gap: 12px;
}

.chart-card {
  min-width: 0;
  background: #fff;
  border-radius: 8px;
  padding: 14px 14px 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.chart-card.wide {
  grid-column: 1 / -1;
}

.chart-title {
  margin-bottom: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.chart-box {
  width: 100%;
  height: 230px;
}

.chart-box-wide {
  height: 250px;
}

@media (max-width: 768px) {
  .charts-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

/* ====== 表格内自定义样式 ====== */
.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  background: #f5f5f5;
  flex-shrink: 0;
}

.goods-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.goods-thumb {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  object-fit: cover;
  background: #f5f5f5;
  flex-shrink: 0;
}

.goods-title-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.price-text {
  font-weight: 600;
  color: #E1251B;
}

/* ====== 响应式：小屏幕适配 ====== */
@media (max-width: 600px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .stat-card {
    padding: 14px 10px;
  }

  .stat-number {
    font-size: 18px;
  }

  .stat-icon-wrap {
    width: 40px;
    height: 40px;
  }

  .stat-icon-wrap .el-icon {
    font-size: 20px !important;
  }
}
</style>
