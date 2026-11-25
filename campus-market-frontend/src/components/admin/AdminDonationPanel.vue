<template>
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

    <!-- 实收弹窗（仅限 pending 状态行打开） -->
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
        </div>
      </div>
      <template #footer>
        <el-button @click="receiveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="receiving" @click="submitReceive">确认实收</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getDonationList, receiveDonation } from '../../api/admin'
import {
  formatTime,
  getDonationItemTypeLabel,
  getDonationUnit,
  getDonationConditionLabel,
  getHandoverLabel,
  getDonationStatusType,
  getDonationStatusLabel
} from '../../utils/admin'

const props = defineProps({
  activeTab: { type: String, default: '' }
})
const emit = defineEmits(['refresh-stats'])

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
    emit('refresh-stats')
  } catch (e) {
    // 请求拦截器已提示
  } finally {
    receiving.value = false
  }
}

watch(() => props.activeTab, tab => {
  if (tab === 'donations' && donationList.value.length === 0) {
    loadDonationList()
  }
}, { immediate: true })
</script>
