<template>
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
</template>

<script setup>
import { ref, watch } from 'vue'
import { getRentalList } from '../../api/admin'
import {
  formatTime,
  getRentalTypeLabel,
  getRentalStatusType,
  getRentalStatusLabel
} from '../../utils/admin'

const props = defineProps({
  activeTab: { type: String, default: '' }
})

const rentalList = ref([])
const rentalLoading = ref(false)
const rentalPageNum = ref(1)
const rentalPageSize = ref(10)
const rentalTotal = ref(0)
const rentalStatus = ref('')
const rentalType = ref('')
const rentalOverdue = ref('all')

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

watch(() => props.activeTab, tab => {
  if (tab === 'rentals' && rentalList.value.length === 0) {
    loadRentalList()
  }
}, { immediate: true })
</script>
