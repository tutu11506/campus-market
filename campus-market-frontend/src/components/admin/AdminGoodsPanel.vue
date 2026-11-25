<template>
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
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getGoodsList, offlineGoods, deleteGoods } from '../../api/admin'
import { getStatusType, getStatusLabel } from '../../utils/admin'
import SmartImage from '../SmartImage.vue'

const props = defineProps({
  activeTab: { type: String, default: '' }
})
const emit = defineEmits(['refresh-stats'])

const goodsList = ref([])
const goodsLoading = ref(false)
const goodsPageNum = ref(1)
const goodsPageSize = ref(10)
const goodsTotal = ref(0)

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
    emit('refresh-stats')
  } catch (e) {
    // 取消或错误，静默
  }
}

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
    emit('refresh-stats')
  } catch (e) {
    // 取消或错误，静默
  }
}

watch(() => props.activeTab, tab => {
  if (tab === 'goods' && goodsList.value.length === 0) {
    loadGoodsList()
  }
}, { immediate: true })
</script>
