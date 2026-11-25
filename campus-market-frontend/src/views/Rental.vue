<template>
  <ServiceShell title="租赁专区" subtitle="短期租赁、假期寄存、教材学期租，按需用、按时还" badge="租">
    <section class="rental-hero">
      <div class="hero-copy">
        <span class="hero-eyebrow">校园租赁 · 用得起也还得起</span>
        <h1>不常用的东西，租比买更划算</h1>
        <p>电动车、投影仪、正装、露营装备按天或按周租；假期大件可寄存，宿舍床位柜子可短租；教材也能只租一学期。</p>
        <div class="hero-tags">
          <span><el-icon><Calendar /></el-icon>自动算租期与押金</span>
          <span><el-icon><AlarmClock /></el-icon>到期与逾期提醒</span>
          <span><el-icon><CircleCheck /></el-icon>归还核销后完成</span>
        </div>
      </div>
      <div class="hero-actions">
        <button class="primary-btn" @click="openPublish">
          <el-icon><Plus /></el-icon>发布出租
        </button>
        <button class="ghost-btn" @click="scrollToMine">我的租赁</button>
      </div>
    </section>

    <section class="panel filter-panel">
      <div class="type-tabs">
        <button
          v-for="tab in typeTabs"
          :key="tab.value"
          class="type-tab"
          :class="{ active: query.type === tab.value }"
          @click="changeType(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>
      <div class="filter-row">
        <el-select v-model="query.category" placeholder="全部品类" clearable style="width: 150px" @change="reloadItems">
          <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
        </el-select>
        <el-input
          v-model="query.keyword"
          placeholder="搜索物品名称或说明"
          clearable
          style="max-width: 260px"
          @keyup.enter="reloadItems"
          @clear="reloadItems"
        >
          <template #append>
            <el-button @click="reloadItems">搜索</el-button>
          </template>
        </el-input>
        <span class="filter-note">共 {{ itemTotal }} 件出租物品，押金仅登记、线下交接</span>
      </div>
    </section>

    <section class="panel">
      <div class="panel-head">
        <div>
          <h2>可租物品</h2>
          <p>租赁类型不同，计价方式不同：按天、按周、整学期、假期寄存。</p>
        </div>
      </div>

      <div v-loading="itemsLoading">
        <div v-if="items.length" class="rental-grid">
          <article v-for="item in items" :key="item.id" class="rental-card">
            <div class="card-top">
              <div class="card-title">
                <h3>{{ item.title }}</h3>
                <div class="card-tags">
                  <span class="tag type-tag">{{ typeText(item.rentalType) }}</span>
                  <span class="tag">{{ categoryText(item.category) }}</span>
                  <span class="tag status-tag" :class="item.status">{{ item.status === 'available' ? '可租' : '已租出' }}</span>
                </div>
              </div>
            </div>
            <p class="card-desc">{{ item.description || '出租人暂未填写物品说明' }}</p>
            <div class="card-price">
              <span class="price-num">¥{{ money(item.price) }}</span>
              <span class="price-unit">/{{ unitText(item.priceUnit) }}</span>
              <span class="deposit">押金 ¥{{ money(item.deposit) }}</span>
            </div>
            <div class="card-meta">
              <span><el-icon><Location /></el-icon>{{ item.location || '交接地点待协商' }}</span>
              <span><el-icon><User /></el-icon>{{ item.ownerName || '同学' }}</span>
              <span v-if="item.rentalType === 'storage' && item.autoOfflineDate">
                <el-icon><AlarmClock /></el-icon>开学 {{ item.autoOfflineDate }} 自动下架
              </span>
            </div>
            <div class="card-actions">
              <button
                class="primary-btn small"
                :disabled="item.status !== 'available' || item.ownerId === currentUserId"
                @click="openRent(item)"
              >
                {{ item.ownerId === currentUserId ? '我发布的' : item.status === 'available' ? '立即租用' : '已租出' }}
              </button>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无符合条件的出租物品" :image-size="90" />
        <div class="pagination-wrap" v-if="itemTotal > query.pageSize">
          <el-pagination
            background
            layout="total, prev, pager, next"
            :total="itemTotal"
            :current-page="query.pageNum"
            :page-size="query.pageSize"
            @current-change="changeItemPage"
          />
        </div>
      </div>
    </section>

    <section class="panel mine-panel" ref="mineSection">
      <div class="panel-head">
        <div>
          <h2>我的租赁</h2>
          <p>待确认可取消；租用中可申请归还；归还后由出租方确认收回。</p>
        </div>
        <el-button v-if="isLoggedIn" text type="success" :loading="ordersLoading" @click="loadOrders">刷新</el-button>
      </div>

      <div v-if="!isLoggedIn" class="login-empty">
        <el-icon><User /></el-icon>
        <p>登录后查看租赁记录与到期提醒</p>
        <el-button type="success" @click="goLogin">登录查看</el-button>
      </div>
      <div v-else>
        <el-radio-group v-model="orderRole" class="role-switch" @change="reloadOrders">
          <el-radio-button value="renter">我租的</el-radio-button>
          <el-radio-button value="lender">我出租的</el-radio-button>
        </el-radio-group>

        <div v-loading="ordersLoading">
          <div v-if="orders.length" class="order-list">
            <article v-for="order in orders" :key="order.id" class="order-card">
              <div class="order-head">
                <div class="order-title">
                  <span class="tag type-tag">{{ typeText(order.rentalType) }}</span>
                  <strong>{{ order.itemTitle }}</strong>
                  <span class="tag status-tag" :class="order.status">{{ statusText(order.status) }}</span>
                  <span v-if="order.overdueDays > 0" class="tag overdue-tag">逾期 {{ order.overdueDays }} 天</span>
                </div>
                <span class="order-no">{{ order.orderNo }}</span>
              </div>
              <div class="order-grid">
                <span>租期：{{ order.startDate }} 至 {{ order.endDate }}（{{ order.days }} 天）</span>
                <span>租金：¥{{ money(order.rentAmount) }} · 押金：¥{{ money(order.deposit) }}</span>
                <span>{{ orderRole === 'renter' ? '出租方' : '租客' }}：{{ orderRole === 'renter' ? order.ownerName : order.renterName }}</span>
                <span v-if="order.status === 'renting' || order.status === 'returning'">
                  {{ order.overdueDays > 0 ? '已逾期 ' + order.overdueDays + ' 天' : '剩余 ' + order.remainingDays + ' 天' }}
                </span>
                <span class="wide" v-if="order.handoverNote">交接说明：{{ order.handoverNote }}</span>
                <span class="wide" v-if="order.remark">备注：{{ order.remark }}</span>
              </div>
              <div class="order-actions">
                <template v-if="orderRole === 'renter'">
                  <el-button v-if="order.status === 'pending'" size="small" @click="handleCancel(order)">取消申请</el-button>
                  <el-button v-if="order.status === 'renting'" size="small" type="primary" @click="handleApplyReturn(order)">申请归还</el-button>
                  <span v-if="order.status === 'returning'" class="wait-tip">已发起归还，等待出租方确认收回</span>
                </template>
                <template v-else>
                  <el-button v-if="order.status === 'pending'" size="small" type="primary" @click="handleConfirm(order)">确认出租</el-button>
                  <el-button v-if="order.status === 'pending'" size="small" @click="handleCancel(order)">取消</el-button>
                  <el-button v-if="order.status === 'returning'" size="small" type="success" @click="handleComplete(order)">确认收回</el-button>
                  <span v-if="order.status === 'renting'" class="wait-tip">租用中，等待租客发起归还</span>
                </template>
              </div>
            </article>
          </div>
          <el-empty v-else description="还没有租赁记录" :image-size="80" />
          <div class="pagination-wrap" v-if="orderTotal > orderQuery.pageSize">
            <el-pagination
              background
              layout="total, prev, pager, next"
              :total="orderTotal"
              :current-page="orderQuery.pageNum"
              :page-size="orderQuery.pageSize"
              @current-change="changeOrderPage"
            />
          </div>
        </div>
      </div>
    </section>

    <!-- 发布出租 -->
    <el-dialog v-model="publishVisible" title="发布出租" width="560px" :close-on-click-modal="false">
      <div class="dialog-form">
        <div class="form-row">
          <label>租赁类型</label>
          <el-select v-model="publishForm.rentalType" style="width: 100%" @change="syncPublishForm">
            <el-option v-for="t in publishTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </div>
        <div class="form-row">
          <label>物品品类</label>
          <el-select v-model="publishForm.category" style="width: 100%">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </div>
        <div class="form-row">
          <label>物品名称 <i>*</i></label>
          <el-input v-model="publishForm.title" maxlength="100" show-word-limit placeholder="例如：42 码正装一套、佳能投影仪、考研英语教材" />
        </div>
        <div class="form-row">
          <label>物品说明</label>
          <el-input v-model="publishForm.description" type="textarea" :rows="3" maxlength="600" show-word-limit placeholder="成色、配件、使用要求、注意事项等" />
        </div>
        <div class="form-two">
          <div class="form-row">
            <label>租金（元/{{ unitText(unitOf(publishForm.rentalType)) }}）<i>*</i></label>
            <el-input-number v-model="publishForm.price" :min="0.01" :max="99999" :precision="2" :controls="false" style="width: 100%" />
          </div>
          <div class="form-row">
            <label>押金（元）</label>
            <el-input-number v-model="publishForm.deposit" :min="0" :max="99999" :precision="2" :controls="false" style="width: 100%" />
          </div>
        </div>
        <div class="form-row">
          <label>交接 / 寄存地点</label>
          <el-input v-model="publishForm.location" maxlength="100" placeholder="例如：南苑 3 号楼、图书馆门口" />
        </div>
        <div class="form-row" v-if="publishForm.rentalType === 'storage'">
          <label>开学自动下架日期 <i>*</i></label>
          <el-date-picker v-model="publishForm.autoOfflineDate" type="date" value-format="YYYY-MM-DD" placeholder="选择开学日期" style="width: 100%" />
          <div class="row-tip">到达该日期后，空闲的寄存物品会自动下架</div>
        </div>
        <div class="dialog-tip">押金仅做登记，平台不代收代付，请在线下交接时自行确认。</div>
      </div>
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="submitPublish">发布</el-button>
      </template>
    </el-dialog>

    <!-- 下单租用 -->
    <el-dialog v-model="rentVisible" title="确认租用" width="520px" :close-on-click-modal="false">
      <div v-if="rentItem" class="dialog-form">
        <div class="rent-summary">
          <strong>{{ rentItem.title }}</strong>
          <span>¥{{ money(rentItem.price) }}/{{ unitText(rentItem.priceUnit) }} · 押金 ¥{{ money(rentItem.deposit) }}</span>
          <span class="sub">出租方：{{ rentItem.ownerName }} · {{ rentItem.location || '地点待协商' }}</span>
        </div>
        <div class="form-two">
          <div class="form-row">
            <label>{{ rentItem.rentalType === 'textbook' ? '学期开始日期' : '租期开始' }} <i>*</i></label>
            <el-date-picker v-model="rentForm.startDate" type="date" value-format="YYYY-MM-DD" placeholder="选择开始日期" style="width: 100%" />
          </div>
          <div class="form-row">
            <label>{{ rentItem.rentalType === 'textbook' ? '学期结束日期' : '租期结束' }} <i>*</i></label>
            <el-date-picker v-model="rentForm.endDate" type="date" value-format="YYYY-MM-DD" placeholder="选择结束日期" style="width: 100%" />
          </div>
        </div>
        <div class="rent-preview">
          <span>租期：{{ previewDays }} 天</span>
          <span>预计租金：<strong>¥{{ money(previewRent) }}</strong></span>
          <span>押金：¥{{ money(rentItem.deposit) }}（线下交接）</span>
        </div>
        <div class="form-row">
          <label>交接说明</label>
          <el-input v-model="rentForm.handoverNote" maxlength="255" placeholder="例如：今晚 19:00 在南苑 3 号楼门口取" />
        </div>
        <div class="form-row">
          <label>备注</label>
          <el-input v-model="rentForm.remark" maxlength="255" placeholder="选填" />
        </div>
        <div class="dialog-tip">提交后等待出租方确认；租期结束前 2 天和逾期时会收到站内提醒。</div>
      </div>
      <template #footer>
        <el-button @click="rentVisible = false">取消</el-button>
        <el-button type="primary" :loading="ordering" @click="submitOrder">提交租用申请</el-button>
      </template>
    </el-dialog>
  </ServiceShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { AlarmClock, Calendar, CircleCheck, Location, Plus, User } from '@element-plus/icons-vue'
import ServiceShell from '../components/ServiceShell.vue'
import {
  applyRentalReturn,
  cancelRentalOrder,
  completeRentalOrder,
  confirmRentalOrder,
  createRentalOrder,
  getMyRentalOrders,
  getRentalItems,
  publishRentalItem
} from '../api/rental'

const router = useRouter()
const mineSection = ref(null)

const typeTabs = [
  { value: 'all', label: '全部' },
  { value: 'short', label: '短期租赁' },
  { value: 'storage', label: '假期寄存转租' },
  { value: 'textbook', label: '教材学期租' }
]
const publishTypes = [
  { value: 'daily', label: '按天租赁' },
  { value: 'weekly', label: '按周租赁' },
  { value: 'storage', label: '假期寄存转租' },
  { value: 'textbook', label: '教材学期租' }
]
const categories = [
  { value: 'vehicle', label: '电动车/代步' },
  { value: 'digital', label: '数码设备' },
  { value: 'photo', label: '摄影器材' },
  { value: 'formal', label: '正装礼服' },
  { value: 'camping', label: '露营装备' },
  { value: 'dorm', label: '宿舍用品' },
  { value: 'textbook', label: '教材书籍' },
  { value: 'storage', label: '床位/柜子' },
  { value: 'other', label: '其他' }
]

const query = reactive({ type: 'all', category: '', keyword: '', pageNum: 1, pageSize: 12 })
const items = ref([])
const itemTotal = ref(0)
const itemsLoading = ref(false)

const orderRole = ref('renter')
const orderQuery = reactive({ pageNum: 1, pageSize: 8 })
const orders = ref([])
const orderTotal = ref(0)
const ordersLoading = ref(false)

const publishVisible = ref(false)
const publishing = ref(false)
const publishForm = reactive({
  rentalType: 'daily',
  category: 'vehicle',
  title: '',
  description: '',
  price: null,
  deposit: 0,
  location: '',
  autoOfflineDate: ''
})

const rentVisible = ref(false)
const ordering = ref(false)
const rentItem = ref(null)
const rentForm = reactive({ startDate: '', endDate: '', handoverNote: '', remark: '' })

const isLoggedIn = computed(() => !!(localStorage.getItem('token') && localStorage.getItem('user')))
const currentUserId = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || '{}').id
  } catch (e) {
    return null
  }
})

const typeText = type => ({ daily: '按天租赁', weekly: '按周租赁', storage: '假期寄存', textbook: '教材学期租' }[type] || type)
const categoryText = value => (categories.find(c => c.value === value) || {}).label || value
const unitText = unit => ({ day: '天', week: '周', semester: '学期' }[unit] || '天')
const statusText = status => ({
  pending: '待确认',
  renting: '租用中',
  returning: '待收回',
  completed: '已完成',
  cancelled: '已取消'
}[status] || status)
const money = value => {
  const num = Number(value || 0)
  return Number.isInteger(num) ? num : num.toFixed(2)
}
const unitOf = type => (type === 'weekly' ? 'week' : type === 'textbook' ? 'semester' : 'day')

const daysBetween = (start, end) => {
  if (!start || !end) return 0
  const s = new Date(start + 'T00:00:00')
  const e = new Date(end + 'T00:00:00')
  const diff = Math.round((e - s) / 86400000) + 1
  return diff > 0 ? diff : 0
}

const previewDays = computed(() => daysBetween(rentForm.startDate, rentForm.endDate))
const previewRent = computed(() => {
  if (!rentItem.value || previewDays.value <= 0) return 0
  const price = Number(rentItem.value.price || 0)
  if (rentItem.value.rentalType === 'weekly') {
    return price * Math.ceil(previewDays.value / 7)
  }
  if (rentItem.value.rentalType === 'textbook') {
    return price
  }
  return price * previewDays.value
})

const loadItems = async () => {
  itemsLoading.value = true
  try {
    const data = await getRentalItems({
      type: query.type,
      category: query.category,
      keyword: query.keyword,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    items.value = data?.list || []
    itemTotal.value = Number(data?.total) || 0
  } catch (e) {
    // 拦截器已提示
  } finally {
    itemsLoading.value = false
  }
}

const reloadItems = () => {
  query.pageNum = 1
  loadItems()
}

const changeType = value => {
  query.type = value
  reloadItems()
}

const changeItemPage = page => {
  query.pageNum = page
  loadItems()
}

const loadOrders = async () => {
  if (!isLoggedIn.value) return
  ordersLoading.value = true
  try {
    const data = await getMyRentalOrders({
      role: orderRole.value,
      pageNum: orderQuery.pageNum,
      pageSize: orderQuery.pageSize
    })
    orders.value = data?.list || []
    orderTotal.value = Number(data?.total) || 0
  } catch (e) {
    // 拦截器已提示
  } finally {
    ordersLoading.value = false
  }
}

const reloadOrders = () => {
  orderQuery.pageNum = 1
  loadOrders()
}

const changeOrderPage = page => {
  orderQuery.pageNum = page
  loadOrders()
}

const goLogin = () => router.push({ path: '/login', query: { redirect: '/rental' } })

const scrollToMine = () => {
  if (!isLoggedIn.value) {
    goLogin()
    return
  }
  mineSection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const syncPublishForm = () => {
  // 教材学期租默认归到教材品类，方便筛选
  if (publishForm.rentalType === 'textbook') {
    publishForm.category = 'textbook'
  }
  if (publishForm.rentalType === 'storage' && !publishForm.autoOfflineDate) {
    publishForm.autoOfflineDate = ''
  }
}

const openPublish = () => {
  if (!isLoggedIn.value) {
    goLogin()
    return
  }
  publishVisible.value = true
}

const submitPublish = async () => {
  if (!publishForm.title.trim()) {
    ElMessage.warning('请填写物品名称')
    return
  }
  if (!publishForm.price || Number(publishForm.price) <= 0) {
    ElMessage.warning('请填写正确的租金')
    return
  }
  if (publishForm.rentalType === 'storage' && !publishForm.autoOfflineDate) {
    ElMessage.warning('假期寄存请选择开学自动下架日期')
    return
  }
  publishing.value = true
  try {
    await publishRentalItem({
      rentalType: publishForm.rentalType,
      category: publishForm.category,
      title: publishForm.title.trim(),
      description: publishForm.description.trim(),
      price: publishForm.price,
      deposit: publishForm.deposit || 0,
      location: publishForm.location.trim(),
      autoOfflineDate: publishForm.rentalType === 'storage' ? publishForm.autoOfflineDate : null
    })
    ElMessage.success('发布成功，等待同学来租')
    publishVisible.value = false
    publishForm.title = ''
    publishForm.description = ''
    publishForm.price = null
    publishForm.deposit = 0
    publishForm.location = ''
    reloadItems()
    if (orderRole.value === 'lender') reloadOrders()
  } catch (e) {
    // 拦截器已提示
  } finally {
    publishing.value = false
  }
}

const openRent = item => {
  if (!isLoggedIn.value) {
    goLogin()
    return
  }
  rentItem.value = item
  rentForm.startDate = ''
  rentForm.endDate = ''
  rentForm.handoverNote = ''
  rentForm.remark = ''
  rentVisible.value = true
}

const submitOrder = async () => {
  if (!rentForm.startDate || !rentForm.endDate) {
    ElMessage.warning('请选择租期起止日期')
    return
  }
  if (previewDays.value <= 0) {
    ElMessage.warning('结束日期不能早于开始日期')
    return
  }
  if (previewDays.value > 365) {
    ElMessage.warning('单次租期不能超过 365 天')
    return
  }
  ordering.value = true
  try {
    await createRentalOrder({
      itemId: rentItem.value.id,
      startDate: rentForm.startDate,
      endDate: rentForm.endDate,
      handoverNote: rentForm.handoverNote.trim(),
      remark: rentForm.remark.trim()
    })
    ElMessage.success('租用申请已提交，等待出租方确认')
    rentVisible.value = false
    reloadItems()
    orderRole.value = 'renter'
    reloadOrders()
  } catch (e) {
    // 拦截器已提示
  } finally {
    ordering.value = false
  }
}

const afterOrderAction = (message, nextRole) => {
  ElMessage.success(message)
  if (nextRole) orderRole.value = nextRole
  loadOrders()
  loadItems()
}

const handleConfirm = async order => {
  try {
    await ElMessageBox.confirm(`确认将《${order.itemTitle}》出租给 ${order.renterName}？确认后进入租用中。`, '确认出租', {
      confirmButtonText: '确认出租',
      cancelButtonText: '再想想',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await confirmRentalOrder(order.id)
    afterOrderAction('已确认出租')
  } catch (e) {
    // 拦截器已提示
  }
}

const handleApplyReturn = async order => {
  try {
    await ElMessageBox.confirm(`确认归还《${order.itemTitle}》？请先与出租方约好交接时间。`, '申请归还', {
      confirmButtonText: '申请归还',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await applyRentalReturn(order.id)
    afterOrderAction('已发起归还，等待出租方确认收回')
  } catch (e) {
    // 拦截器已提示
  }
}

const handleComplete = async order => {
  try {
    await ElMessageBox.confirm(`确认已收回《${order.itemTitle}》并核对物品无误？确认后订单完成。`, '确认收回', {
      confirmButtonText: '确认收回',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await completeRentalOrder(order.id)
    afterOrderAction('已确认收回，租赁完成')
  } catch (e) {
    // 拦截器已提示
  }
}

const handleCancel = async order => {
  try {
    await ElMessageBox.confirm(`确认取消《${order.itemTitle}》的租赁订单？`, '取消订单', {
      confirmButtonText: '确认取消',
      cancelButtonText: '保留',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await cancelRentalOrder(order.id)
    afterOrderAction('租赁订单已取消')
  } catch (e) {
    // 拦截器已提示
  }
}

onMounted(() => {
  loadItems()
  loadOrders()
})
</script>

<style scoped>
.rental-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 22px 26px;
  margin-bottom: 14px;
  border-radius: 12px;
  background: linear-gradient(135deg, #FFF7ED, #FFF1F0 60%, #F0FDF4);
  border: 1px solid #FDE68A;
}

.hero-eyebrow {
  font-size: 12px;
  font-weight: 700;
  color: #B45309;
  letter-spacing: 0.5px;
}

.hero-copy h1 {
  margin: 6px 0 6px;
  font-size: 22px;
  color: #1F2937;
}

.hero-copy p {
  margin: 0;
  max-width: 720px;
  font-size: 13px;
  line-height: 1.8;
  color: #6B7280;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 12px;
  font-size: 12px;
  color: #92400E;
}

.hero-tags span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.hero-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
}

.primary-btn,
.ghost-btn {
  height: 38px;
  padding: 0 20px;
  border-radius: 19px;
  font-size: 13.5px;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.primary-btn {
  border: none;
  color: #fff;
  background: linear-gradient(135deg, #FF7A45, #E1251B);
  box-shadow: 0 6px 16px rgba(225, 37, 27, 0.24);
}

.primary-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

.primary-btn.small {
  height: 34px;
  padding: 0 16px;
  font-size: 13px;
}

.ghost-btn {
  border: 1px solid #E5E7EB;
  background: #fff;
  color: #4B5563;
}

.panel {
  padding: 18px 22px;
  margin-bottom: 14px;
  border-radius: 12px;
  background: #fff;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-head h2 {
  margin: 0 0 4px;
  font-size: 16px;
  color: #1F2937;
}

.panel-head p {
  margin: 0;
  font-size: 12px;
  color: #9CA3AF;
}

.filter-panel {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.type-tabs {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.type-tab {
  height: 32px;
  padding: 0 14px;
  border-radius: 16px;
  border: 1px solid #E5E7EB;
  background: #fff;
  color: #4B5563;
  font-size: 13px;
  cursor: pointer;
}

.type-tab.active {
  border-color: #E1251B;
  color: #E1251B;
  background: #FFF1F0;
  font-weight: 700;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.filter-note {
  font-size: 11.5px;
  color: #B0B4BA;
}

.rental-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(270px, 1fr));
  gap: 14px;
}

.rental-card {
  display: flex;
  flex-direction: column;
  padding: 16px;
  border-radius: 10px;
  border: 1px solid #EDEEF0;
  background: #FAFBFC;
  transition: box-shadow 0.16s ease, border-color 0.16s ease;
}

.rental-card:hover {
  border-color: #FECACA;
  box-shadow: 0 8px 20px rgba(31, 41, 55, 0.06);
}

.card-title h3 {
  margin: 0 0 8px;
  font-size: 15px;
  color: #1F2937;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 9px;
  font-size: 11px;
  background: #F3F4F6;
  color: #6B7280;
}

.tag.type-tag {
  background: #FFF1F0;
  color: #E1251B;
  font-weight: 700;
}

.tag.status-tag.available {
  background: #ECFDF5;
  color: #059669;
}

.tag.status-tag.rented,
.tag.status-tag.offline {
  background: #F3F4F6;
  color: #9CA3AF;
}

.tag.status-tag.pending {
  background: #FFFBEB;
  color: #B45309;
}

.tag.status-tag.renting {
  background: #EFF6FF;
  color: #2563EB;
}

.tag.status-tag.returning {
  background: #F5F3FF;
  color: #7C3AED;
}

.tag.status-tag.completed {
  background: #ECFDF5;
  color: #059669;
}

.tag.status-tag.cancelled {
  background: #F3F4F6;
  color: #9CA3AF;
}

.tag.overdue-tag {
  background: #FEF2F2;
  color: #DC2626;
  font-weight: 700;
}

.card-desc {
  margin: 10px 0;
  min-height: 38px;
  font-size: 12px;
  line-height: 1.6;
  color: #6B7280;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-price {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 8px;
}

.price-num {
  font-size: 20px;
  font-weight: 800;
  color: #E1251B;
}

.price-unit {
  font-size: 12px;
  color: #9CA3AF;
}

.deposit {
  margin-left: auto;
  font-size: 11.5px;
  color: #B45309;
}

.card-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 11.5px;
  color: #9CA3AF;
}

.card-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.card-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.mine-panel .role-switch {
  margin-bottom: 12px;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.order-card {
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid #EDEEF0;
  background: #FAFBFC;
}

.order-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.order-title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #1F2937;
}

.order-no {
  font-size: 11px;
  color: #B0B4BA;
}

.order-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 20px;
  font-size: 12px;
  color: #6B7280;
}

.order-grid .wide {
  width: 100%;
}

.order-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}

.wait-tip {
  font-size: 12px;
  color: #9CA3AF;
}

.login-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 26px 0;
  color: #9CA3AF;
  font-size: 13px;
}

.login-empty p {
  margin: 0;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.form-row {
  margin-bottom: 12px;
}

.form-row label {
  display: block;
  margin-bottom: 6px;
  font-size: 12.5px;
  font-weight: 600;
  color: #374151;
}

.form-row label i {
  color: #E1251B;
  font-style: normal;
}

.form-two {
  display: flex;
  gap: 12px;
}

.form-two .form-row {
  flex: 1;
}

.row-tip {
  margin-top: 5px;
  font-size: 11px;
  color: #B0B4BA;
}

.dialog-tip {
  padding: 9px 12px;
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.7;
  color: #B45309;
  background: #FFFBEB;
  border: 1px solid #FDE68A;
}

.rent-summary {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 12px 14px;
  margin-bottom: 14px;
  border-radius: 9px;
  background: #FFF7F5;
  border: 1px solid #FDE2DD;
  font-size: 12.5px;
  color: #6B7280;
}

.rent-summary strong {
  font-size: 15px;
  color: #1F2937;
}

.rent-summary .sub {
  font-size: 11.5px;
  color: #9CA3AF;
}

.rent-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 18px;
  padding: 10px 12px;
  margin-bottom: 12px;
  border-radius: 8px;
  background: #F8FAFC;
  font-size: 12.5px;
  color: #4B5563;
}

.rent-preview strong {
  color: #E1251B;
}

@media (max-width: 768px) {
  .rental-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-actions {
    flex-direction: row;
  }

  .rental-grid {
    grid-template-columns: 1fr;
  }

  .form-two {
    flex-direction: column;
    gap: 0;
  }
}
</style>
