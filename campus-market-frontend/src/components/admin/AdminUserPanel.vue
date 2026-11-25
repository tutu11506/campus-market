<template>
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
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
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
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, updateUserStatus } from '../../api/admin'
import { formatTime, handleAvatarError, resolveAvatar } from '../../utils/admin'

const props = defineProps({
  activeTab: { type: String, default: '' }
})
const emit = defineEmits(['refresh-stats'])

const userList = ref([])
const userLoading = ref(false)
const userPageNum = ref(1)
const userPageSize = ref(10)
const userTotal = ref(0)

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
    emit('refresh-stats')
  } catch (e) {
    // 取消或错误，静默
  }
}

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
    emit('refresh-stats')
  } catch (e) {
    // 取消或错误，静默
  }
}

// 首次进入时懒加载
watch(() => props.activeTab, tab => {
  if (tab === 'users' && userList.value.length === 0) {
    loadUserList()
  }
}, { immediate: true })
</script>
