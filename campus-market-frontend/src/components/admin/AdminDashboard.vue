<template>
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
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { getDashboard, getExportUrl } from '../../api/admin'

import * as echarts from 'echarts/core'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([LineChart, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const props = defineProps({
  activeTab: { type: String, default: '' }
})

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
  if (props.activeTab !== 'dashboard') return
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

    const userChart = echarts.init(userEl)
    userChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 8, right: 16, top: 20, bottom: 4, containLabel: true },
      xAxis: {
        type: 'category', boundaryGap: false, data: dates,
        axisLine: { lineStyle: { color: '#eee' } }, axisLabel
      },
      yAxis: { type: 'value', minInterval: 1, splitLine, axisLabel },
      series: [{
        name: '新增用户', type: 'line', smooth: true, symbolSize: 6,
        data: users, lineStyle: { width: 2, color: '#E1251B' },
        itemStyle: { color: '#E1251B' }, areaStyle: { color: 'rgba(225, 37, 27, 0.10)' }
      }]
    })

    const orderChart = echarts.init(orderEl)
    orderChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { top: 0, right: 0, itemWidth: 10, itemHeight: 10, textStyle: { color: '#666', fontSize: 11 } },
      grid: { left: 8, right: 8, top: 40, bottom: 4, containLabel: true },
      xAxis: { type: 'category', data: dates, axisLine: { lineStyle: { color: '#eee' } }, axisLabel },
      yAxis: [
        { type: 'value', name: '订单数', minInterval: 1, nameTextStyle: { color: '#999', fontSize: 11 }, splitLine, axisLabel },
        { type: 'value', name: '成交额(¥)', nameTextStyle: { color: '#999', fontSize: 11 }, splitLine: { show: false }, axisLabel }
      ],
      series: [
        { name: '订单数', type: 'bar', barWidth: 18, data: orders, itemStyle: { color: '#E1251B', borderRadius: [4, 4, 0, 0] } },
        { name: '成交额', type: 'line', yAxisIndex: 1, smooth: true, symbolSize: 6, data: amounts,
          lineStyle: { width: 2, color: '#FFA940' }, itemStyle: { color: '#FFA940' } }
      ]
    })

    const categoryChart = echarts.init(categoryEl)
    categoryChart.setOption({
      color: ['#E1251B', '#FF7A45', '#FFC53D', '#36CFC9', '#597EF7', '#9254DE'],
      tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 件（{d}%）' },
      series: [{
        type: 'pie', radius: ['46%', '68%'], center: ['50%', '46%'],
        avoidLabelOverlap: true, minAngle: 3,
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { color: '#666', fontSize: 11, formatter: '{b}\n{c}件' },
        labelLine: { length: 8, length2: 8 },
        data: dashboard.value.categoryStats.map(item => ({ name: item.name, value: Number(item.count) || 0 }))
      }]
    })

    chartInstances = [userChart, orderChart, categoryChart]
  })
}

const resizeCharts = () => chartInstances.forEach(chart => chart.resize())

const loadDashboard = async () => {
  dashboardLoading.value = true
  try {
    const res = await getDashboard(dashboardPeriod.value)
    dashboard.value = {
      userTrend: (res && res.userTrend) || [],
      orderTrend: (res && res.orderTrend) || [],
      categoryStats: (res && res.categoryStats) || []
    }
    if (props.activeTab === 'dashboard') renderCharts()
  } catch (e) {
    console.error('加载数据看板失败', e)
  } finally {
    dashboardLoading.value = false
  }
}

const onPeriodChange = () => loadDashboard()

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

watch(() => props.activeTab, tab => {
  if (tab === 'dashboard') {
    if (dashboard.value.userTrend.length === 0) {
      loadDashboard()
    } else {
      renderCharts()
    }
  } else {
    disposeCharts()
  }
})

onMounted(() => {
  window.addEventListener('resize', resizeCharts)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeCharts)
  disposeCharts()
})
</script>
