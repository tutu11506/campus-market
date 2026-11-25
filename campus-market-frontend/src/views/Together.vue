<template>
  <ServiceShell title="拼单搭子" subtitle="拼单分摊、找搭子组队，一起更省更热闹" badge="拼">
    <section class="together-hero">
      <div class="hero-copy">
        <span class="hero-eyebrow">校园一起 · 拼着买更划算</span>
        <h1>一个人不好意思买，那就一起拼</h1>
        <p>全新团购、拼分装、共享网盘视频会员、二手大件分摊都能开团；还能找装备搭子、跳蚤市场搭子、考研搭子、运动搭子，顺带把闲置用起来。</p>
        <div class="hero-tags">
          <span><el-icon><User /></el-icon>发起人自动占 1 份</span>
          <span><el-icon><AlarmClock /></el-icon>满员锁定、截止自动结算</span>
          <span><el-icon><CircleCheck /></el-icon>平台不收款，线下自行结算</span>
        </div>
      </div>
      <div class="hero-actions">
        <button class="primary-btn" @click="openPublish">
          <el-icon><Plus /></el-icon>{{ activeTab === 'group' ? '发起拼单' : '发起搭子' }}
        </button>
        <button class="ghost-btn" @click="switchBrowseMode('owner')">我发起的</button>
      </div>
    </section>

    <div class="main-tabs">
      <button class="main-tab" :class="{ active: activeTab === 'group' }" @click="switchTab('group')">
        <span class="tab-title">拼单</span>
        <span class="tab-sub">团购 · 分装 · 共享会员 · 二手分摊</span>
      </button>
      <button class="main-tab" :class="{ active: activeTab === 'partner' }" @click="switchTab('partner')">
        <span class="tab-title">搭子</span>
        <span class="tab-sub">装备拼买 · 跳蚤市场 · 考研学习 · 运动</span>
      </button>
    </div>

    <section class="panel filter-panel">
      <div class="type-tabs" v-if="browseMode === 'all'">
        <button
          v-for="tab in currentTypeTabs"
          :key="tab.value"
          class="type-tab"
          :class="{ active: currentType === tab.value }"
          @click="changeType(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>
      <div class="filter-row">
        <div class="mode-tabs">
          <button
            v-for="m in modes"
            :key="m.value"
            class="mode-tab"
            :class="{ active: browseMode === m.value }"
            @click="switchBrowseMode(m.value)"
          >
            {{ m.label }}
          </button>
        </div>
        <el-input
          v-if="browseMode === 'all'"
          v-model="currentKeyword"
          :placeholder="activeTab === 'group' ? '搜索拼单标题或说明' : '搜索搭子标题或说明'"
          clearable
          style="max-width: 250px"
          @keyup.enter="reloadList"
          @clear="reloadList"
        >
          <template #append>
            <el-button @click="reloadList">搜索</el-button>
          </template>
        </el-input>
        <span class="filter-note">
          共 {{ currentTotal }} 条{{ activeTab === 'group' ? '拼单' : '搭子' }}{{ browseMode === 'all' ? '，满员后自动锁定报名' : '' }}
        </span>
      </div>
    </section>

    <section class="panel">
      <div class="panel-head">
        <div>
          <h2>{{ listTitle }}</h2>
          <p>{{ listSubtitle }}</p>
        </div>
      </div>

      <div v-loading="listLoading">
        <div v-if="currentItems.length" class="card-grid">
          <!-- 拼单卡片 -->
          <template v-if="activeTab === 'group'">
            <article v-for="g in groups" :key="g.id" class="together-card">
              <div class="card-top">
                <h3>{{ g.title }}</h3>
                <div class="card-tags">
                  <span class="tag type-tag">{{ groupTypeText(g.type) }}</span>
                  <span class="tag status-tag" :class="g.status">{{ groupStatusText(g.status) }}</span>
                </div>
              </div>
              <p class="card-desc">{{ g.description || '发起人暂未填写拼单说明' }}</p>
              <div class="progress-block">
                <div class="progress-head">
                  <span>已拼 {{ g.joinedCount }} / {{ g.targetCount }} 份</span>
                  <span>剩 {{ g.remainingCount }} 份</span>
                </div>
                <div class="progress-track">
                  <div class="progress-bar" :style="{ width: progressOf(g.joinedCount, g.targetCount) + '%' }"></div>
                </div>
              </div>
              <div class="card-price">
                <span class="price-num">¥{{ money(g.pricePerShare) }}</span>
                <span class="price-unit">/份</span>
                <span class="deadline-tag" :class="{ over: isExpired(g.deadline) }">{{ deadlineInfo(g.deadline) }}</span>
              </div>
              <div class="card-meta">
                <span><el-icon><User /></el-icon>{{ g.ownerName || '同学' }}</span>
                <span><el-icon><Clock /></el-icon>截止 {{ formatDateTime(g.deadline) }}</span>
                <span v-if="g.location"><el-icon><Location /></el-icon>{{ g.location }}</span>
                <span v-if="g.goodsTitle" class="link-tag"><el-icon><Link /></el-icon>关联：{{ g.goodsTitle }}</span>
              </div>
              <div class="card-actions">
                <template v-if="g.ownerId === currentUserId">
                  <button
                    v-if="['open', 'full'].includes(g.status)"
                    class="primary-btn small"
                    @click="handleConfirmGroup(g)"
                  >确认成团</button>
                  <button
                    v-if="['open', 'full'].includes(g.status)"
                    class="ghost-btn small"
                    @click="handleCloseGroup(g)"
                  >关闭拼单</button>
                  <span v-if="g.status === 'formed'" class="done-tip">已成团，请与成员线下结算</span>
                </template>
                <template v-else>
                  <button
                    v-if="g.myShares > 0"
                    class="ghost-btn small"
                    :disabled="g.status !== 'open'"
                    @click="handleLeaveGroup(g)"
                  >退出（已参与 {{ g.myShares }} 份）</button>
                  <button
                    v-else
                    class="primary-btn small"
                    :disabled="!canJoinGroup(g)"
                    @click="openJoin(g)"
                  >{{ groupJoinText(g) }}</button>
                </template>
              </div>
            </article>
          </template>

          <!-- 搭子卡片 -->
          <template v-else>
            <article v-for="p in partners" :key="p.id" class="together-card">
              <div class="card-top">
                <h3>{{ p.title }}</h3>
                <div class="card-tags">
                  <span class="tag type-tag">{{ partnerTypeText(p.type) }}</span>
                  <span class="tag status-tag" :class="p.status">{{ partnerStatusText(p.status) }}</span>
                </div>
              </div>
              <p class="card-desc">{{ p.description || '发起人暂未填写搭子说明' }}</p>
              <div class="progress-block">
                <div class="progress-head">
                  <span>已加入 {{ p.memberCount }} / {{ p.memberLimit }} 人</span>
                  <span>剩 {{ p.remainingCount }} 个名额</span>
                </div>
                <div class="progress-track">
                  <div class="progress-bar" :style="{ width: progressOf(p.memberCount, p.memberLimit) + '%' }"></div>
                </div>
              </div>
              <div class="card-price">
                <span class="activity-label">活动时间</span>
                <span class="price-num small">{{ formatDateTime(p.activityTime) }}</span>
                <span class="deadline-tag" :class="{ over: isExpired(p.activityTime) }">{{ activityInfo(p.activityTime) }}</span>
              </div>
              <div class="card-meta">
                <span><el-icon><User /></el-icon>{{ p.ownerName || '同学' }}</span>
                <span v-if="p.location"><el-icon><Location /></el-icon>{{ p.location }}</span>
                <span v-if="p.linkTitle" class="link-tag">
                  <el-icon><Link /></el-icon>{{ p.linkType === 'groupbuy' ? '关联拼单：' : '关联闲置：' }}{{ p.linkTitle }}
                </span>
              </div>
              <div class="card-actions">
                <template v-if="p.ownerId === currentUserId">
                  <button
                    v-if="['open', 'full'].includes(p.status)"
                    class="ghost-btn small"
                    @click="handleCancelPartner(p)"
                  >取消搭子</button>
                  <span v-if="p.status === 'open'" class="done-tip">等同学加入中</span>
                  <span v-if="p.status === 'full'" class="done-tip">已满员，按约定时间集合</span>
                </template>
                <template v-else>
                  <button
                    v-if="p.myStatus === 1"
                    class="ghost-btn small"
                    :disabled="p.status !== 'open'"
                    @click="handleLeavePartner(p)"
                  >退出搭子</button>
                  <button
                    v-else
                    class="primary-btn small"
                    :disabled="!canJoinPartner(p)"
                    @click="handleJoinPartner(p)"
                  >{{ partnerJoinText(p) }}</button>
                </template>
              </div>
            </article>
          </template>
        </div>
        <el-empty v-else :description="emptyText" :image-size="90" />
        <div class="pagination-wrap" v-if="currentTotal > currentPageSize">
          <el-pagination
            background
            layout="total, prev, pager, next"
            :total="currentTotal"
            :current-page="currentPageNum"
            :page-size="currentPageSize"
            @current-change="changePage"
          />
        </div>
      </div>
    </section>

    <!-- 发起拼单 -->
    <el-dialog v-model="groupDialog" title="发起拼单" width="580px" :close-on-click-modal="false">
      <div class="dialog-form">
        <div class="form-row">
          <label>拼单类型 <i>*</i></label>
          <el-select v-model="groupForm.type" style="width: 100%">
            <el-option v-for="t in groupTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
          <div class="row-tip">{{ groupTypeHint }}</div>
        </div>
        <div class="form-row">
          <label>拼单标题 <i>*</i></label>
          <el-input v-model="groupForm.title" maxlength="100" show-word-limit placeholder="例如：拼一箱 24 卷抽纸、共享一年网盘会员" />
        </div>
        <div class="form-row">
          <label>拼单说明</label>
          <el-input v-model="groupForm.description" type="textarea" :rows="3" maxlength="600" show-word-limit placeholder="规格、分摊方式、取货与结算约定等" />
        </div>
        <div class="form-two">
          <div class="form-row">
            <label>目标份数 <i>*</i></label>
            <el-input-number v-model="groupForm.targetCount" :min="2" :max="50" :step="1" :precision="0" style="width: 100%" />
          </div>
          <div class="form-row">
            <label>每份费用（元）<i>*</i></label>
            <el-input-number v-model="groupForm.pricePerShare" :min="0.01" :max="99999" :precision="2" :controls="false" style="width: 100%" />
          </div>
        </div>
        <div class="form-row">
          <label>截止时间 <i>*</i></label>
          <el-date-picker
            v-model="groupForm.deadline"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择拼单截止时间"
            style="width: 100%"
          />
          <div class="row-tip">到截止时间仍未满员，拼单自动标记为未成团，并提醒已参与同学</div>
        </div>
        <div class="form-row">
          <label>取货 / 分摊地点</label>
          <el-input v-model="groupForm.location" maxlength="100" placeholder="例如：南苑 3 号楼、图书馆门口" />
        </div>
        <div class="form-row">
          <label>关联平台商品（可选）</label>
          <el-select
            v-model="groupForm.goodsId"
            filterable
            remote
            clearable
            :remote-method="searchGoods"
            :loading="goodsLoading"
            placeholder="输入商品关键词搜索"
            style="width: 100%"
          >
            <el-option v-for="g in goodsOptions" :key="g.id" :label="g.title" :value="g.id" />
          </el-select>
        </div>
        <div class="dialog-tip">发起人自动占 1 份；平台不代收费用，成团后请与成员线下结算。</div>
      </div>
      <template #footer>
        <el-button @click="groupDialog = false">取消</el-button>
        <el-button type="primary" :loading="groupSubmitting" @click="submitGroup">发起拼单</el-button>
      </template>
    </el-dialog>

    <!-- 参与拼单 -->
    <el-dialog v-model="joinDialog" title="参与拼单" width="480px" :close-on-click-modal="false">
      <div v-if="joinTarget" class="dialog-form">
        <div class="rent-summary">
          <strong>{{ joinTarget.title }}</strong>
          <span>¥{{ money(joinTarget.pricePerShare) }}/份 · 还剩 {{ joinTarget.remainingCount }} 份</span>
          <span class="sub">发起人：{{ joinTarget.ownerName }} · 截止 {{ formatDateTime(joinTarget.deadline) }}</span>
        </div>
        <div class="form-row">
          <label>参与份数</label>
          <el-input-number
            v-model="joinShares"
            :min="1"
            :max="Math.max(joinTarget.remainingCount, 1)"
            :step="1"
            :precision="0"
            style="width: 100%"
          />
        </div>
        <div class="rent-preview">
          <span>合计：<strong>¥{{ money(joinPreview) }}</strong></span>
          <span>满员后等待发起人确认成团</span>
        </div>
        <div class="dialog-tip">参与后可在「我参与的」中查看；拼单中可随时退出并释放份数。</div>
      </div>
      <template #footer>
        <el-button @click="joinDialog = false">取消</el-button>
        <el-button type="primary" :loading="joining" @click="submitJoin">确认参与</el-button>
      </template>
    </el-dialog>

    <!-- 发起搭子 -->
    <el-dialog v-model="partnerDialog" title="发起搭子" width="580px" :close-on-click-modal="false">
      <div class="dialog-form">
        <div class="form-row">
          <label>搭子类型 <i>*</i></label>
          <el-select v-model="partnerForm.type" style="width: 100%">
            <el-option v-for="t in partnerTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
          <div class="row-tip">{{ partnerTypeHint }}</div>
        </div>
        <div class="form-row">
          <label>搭子标题 <i>*</i></label>
          <el-input v-model="partnerForm.title" maxlength="100" show-word-limit placeholder="例如：周六一起去跳蚤市场淘货、考研英语互抽背单词" />
        </div>
        <div class="form-row">
          <label>搭子说明</label>
          <el-input v-model="partnerForm.description" type="textarea" :rows="3" maxlength="600" show-word-limit placeholder="集合方式、分工、携带物品、注意事项等" />
        </div>
        <div class="form-two">
          <div class="form-row">
            <label>活动时间 <i>*</i></label>
            <el-date-picker
              v-model="partnerForm.activityTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="选择活动时间"
              style="width: 100%"
            />
          </div>
          <div class="form-row">
            <label>人数上限 <i>*</i></label>
            <el-input-number v-model="partnerForm.memberLimit" :min="2" :max="30" :step="1" :precision="0" style="width: 100%" />
          </div>
        </div>
        <div class="form-row">
          <label>活动 / 集合地点</label>
          <el-input v-model="partnerForm.location" maxlength="100" placeholder="例如：学校东门、体育馆、图书馆 3 楼" />
        </div>
        <div class="form-row">
          <label>关联内容（可选）</label>
          <el-select v-model="partnerForm.linkType" style="width: 100%" @change="onLinkTypeChange">
            <el-option label="不关联" value="none" />
            <el-option label="关联我的闲置商品" value="goods" />
            <el-option label="关联我发起的拼单" value="groupbuy" />
          </el-select>
        </div>
        <div class="form-row" v-if="partnerForm.linkType === 'goods'">
          <label>选择闲置商品</label>
          <el-select
            v-model="partnerForm.linkId"
            filterable
            remote
            clearable
            :remote-method="searchGoods"
            :loading="goodsLoading"
            placeholder="输入商品关键词搜索"
            style="width: 100%"
          >
            <el-option v-for="g in goodsOptions" :key="g.id" :label="g.title" :value="g.id" />
          </el-select>
        </div>
        <div class="form-row" v-if="partnerForm.linkType === 'groupbuy'">
          <label>选择进行中的拼单</label>
          <el-select v-model="partnerForm.linkId" clearable placeholder="选择我发起的拼单" style="width: 100%">
            <el-option v-for="g in linkableGroups" :key="g.id" :label="g.title" :value="g.id" />
          </el-select>
          <div class="row-tip" v-if="!linkableGroups.length">暂无进行中的拼单，可先发起拼单再关联</div>
        </div>
        <div class="dialog-tip">发起人自动占 1 人；活动时间过后搭子自动结束，不再接受报名。</div>
      </div>
      <template #footer>
        <el-button @click="partnerDialog = false">取消</el-button>
        <el-button type="primary" :loading="partnerSubmitting" @click="submitPartner">发起搭子</el-button>
      </template>
    </el-dialog>
  </ServiceShell>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { AlarmClock, CircleCheck, Clock, Link, Location, Plus, User } from '@element-plus/icons-vue'
import ServiceShell from '../components/ServiceShell.vue'
import { getGoodsList } from '../api/goods'
import {
  cancelPartner,
  closeGroupBuy,
  confirmGroupBuy,
  createGroupBuy,
  createPartner,
  getGroupBuys,
  getLinkableGroupBuys,
  getMyGroupBuys,
  getMyPartners,
  getPartners,
  joinGroupBuy,
  joinPartner,
  leaveGroupBuy,
  leavePartner
} from '../api/together'

const router = useRouter()

const groupTypes = [
  { value: 'fresh', label: '拼全新团购' },
  { value: 'split', label: '拼分装' },
  { value: 'membership', label: '拼共享会员' },
  { value: 'secondhand', label: '拼二手大件分摊' }
]
const partnerTypes = [
  { value: 'gear', label: '装备拼买' },
  { value: 'flea', label: '跳蚤市场同行' },
  { value: 'exam', label: '考研 / 学习' },
  { value: 'sport', label: '运动搭子' },
  { value: 'other', label: '其他' }
]
const modes = [
  { value: 'all', label: '全部' },
  { value: 'owner', label: '我发起的' },
  { value: 'member', label: '我参与的' }
]
const groupTypeHints = {
  fresh: '例如整箱纸巾、整箱饮料，按份分掉更便宜。',
  split: '例如大包装洗衣液、散装零食，按份分装。',
  membership: '例如网盘、视频会员，按人头平分年费。',
  secondhand: '例如一套桌游、一台小家电，一起买下共用或分摊。'
}
const partnerTypeHints = {
  gear: '一起拼买装备，例如露营帐篷、球拍、护具。',
  flea: '约好一起去线下跳蚤市场淘货，可关联自己的闲置。',
  exam: '考研 / 考证搭子，交换资料、互相监督。',
  sport: '约跑步、羽毛球、健身，共享运动器材。',
  other: '其他需要结伴的校园活动。'
}

const activeTab = ref('group')
const browseMode = ref('all')
const now = ref(Date.now())
let ticker = null

const groupQuery = reactive({ type: '', keyword: '', pageNum: 1, pageSize: 9 })
const partnerQuery = reactive({ type: '', keyword: '', pageNum: 1, pageSize: 9 })

const groups = ref([])
const partners = ref([])
const groupTotal = ref(0)
const partnerTotal = ref(0)
const groupLoading = ref(false)
const partnerLoading = ref(false)

const isLoggedIn = computed(() => !!(localStorage.getItem('token') && localStorage.getItem('user')))
const currentUserId = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || '{}').id
  } catch (e) {
    return null
  }
})

const currentTypeTabs = computed(() => [
  { value: '', label: '全部' },
  ...(activeTab.value === 'group' ? groupTypes : partnerTypes).map(t => ({ value: t.value, label: t.label }))
])
const currentType = computed(() => (activeTab.value === 'group' ? groupQuery.type : partnerQuery.type))
const currentKeyword = computed({
  get: () => (activeTab.value === 'group' ? groupQuery.keyword : partnerQuery.keyword),
  set: value => {
    if (activeTab.value === 'group') groupQuery.keyword = value
    else partnerQuery.keyword = value
  }
})
const currentItems = computed(() => (activeTab.value === 'group' ? groups.value : partners.value))
const currentTotal = computed(() => (activeTab.value === 'group' ? groupTotal.value : partnerTotal.value))
const currentPageSize = computed(() => (activeTab.value === 'group' ? groupQuery.pageSize : partnerQuery.pageSize))
const currentPageNum = computed(() => (activeTab.value === 'group' ? groupQuery.pageNum : partnerQuery.pageNum))
const listLoading = computed(() => (activeTab.value === 'group' ? groupLoading.value : partnerLoading.value))
const listTitle = computed(() => {
  if (browseMode.value === 'owner') return activeTab.value === 'group' ? '我发起的拼单' : '我发起的搭子'
  if (browseMode.value === 'member') return activeTab.value === 'group' ? '我参与的拼单' : '我参与的搭子'
  return activeTab.value === 'group' ? '拼单广场' : '搭子广场'
})
const listSubtitle = computed(() => {
  if (browseMode.value === 'owner') return '管理自己发起的拼单 / 搭子：确认成团、关闭或取消。'
  if (browseMode.value === 'member') return '查看已加入的拼单 / 搭子，拼单中可退出并释放份数。'
  return activeTab.value === 'group'
    ? '满员后自动锁定报名，发起人确认成团后线下结算。'
    : '按活动时间集合，人数满后锁定报名，活动时间过后自动结束。'
})
const emptyText = computed(() => {
  if (browseMode.value === 'owner') return activeTab.value === 'group' ? '还没有发起过拼单' : '还没有发起过搭子'
  if (browseMode.value === 'member') return activeTab.value === 'group' ? '还没有参与过拼单' : '还没有加入过搭子'
  return activeTab.value === 'group' ? '暂无符合条件的拼单' : '暂无符合条件的搭子'
})

/* ==================== 展示工具 ==================== */

const groupTypeText = type => (groupTypes.find(t => t.value === type) || {}).label || type
const partnerTypeText = type => (partnerTypes.find(t => t.value === type) || {}).label || type
const groupStatusText = status => ({
  open: '拼单中',
  full: '已满员',
  formed: '已成团',
  failed: '未成团',
  closed: '已关闭'
}[status] || status)
const partnerStatusText = status => ({
  open: '招募中',
  full: '已满员',
  finished: '已结束',
  cancelled: '已取消'
}[status] || status)
const money = value => {
  const num = Number(value || 0)
  return Number.isInteger(num) ? num : num.toFixed(2)
}
const formatDateTime = value => (value ? String(value).replace('T', ' ').slice(0, 16) : '-')
const isExpired = value => !!value && new Date(value).getTime() <= now.value
const progressOf = (current, target) => {
  const t = Number(target) || 0
  if (t <= 0) return 0
  return Math.min(100, Math.round(((Number(current) || 0) / t) * 100))
}
const deadlineInfo = deadline => {
  if (!deadline) return ''
  const diff = new Date(deadline).getTime() - now.value
  if (diff <= 0) return '已截止'
  const hours = Math.floor(diff / 3600000)
  if (hours >= 24) return `剩 ${Math.floor(hours / 24)} 天`
  if (hours >= 1) return `剩 ${hours} 小时`
  return `剩 ${Math.max(1, Math.floor(diff / 60000))} 分钟`
}
const activityInfo = time => {
  if (!time) return ''
  const diff = new Date(time).getTime() - now.value
  if (diff <= 0) return '已开始'
  const hours = Math.floor(diff / 3600000)
  if (hours >= 24) return `${Math.floor(hours / 24)} 天后`
  if (hours >= 1) return `${hours} 小时后`
  return `${Math.max(1, Math.floor(diff / 60000))} 分钟后`
}
const groupJoinText = g => {
  if (g.status === 'full') return '已满员'
  if (g.status === 'formed') return '已成团'
  if (g.status === 'failed') return '未成团'
  if (g.status === 'closed') return '已关闭'
  if (isExpired(g.deadline)) return '已截止'
  return '参与拼单'
}
const canJoinGroup = g => g.status === 'open' && !isExpired(g.deadline) && Number(g.remainingCount) > 0
const partnerJoinText = p => {
  if (p.status === 'full') return '已满员'
  if (p.status === 'finished') return '已结束'
  if (p.status === 'cancelled') return '已取消'
  if (isExpired(p.activityTime)) return '已开始'
  return '加入搭子'
}
const canJoinPartner = p => p.status === 'open' && !isExpired(p.activityTime) && Number(p.remainingCount) > 0

/* ==================== 数据加载 ==================== */

const loadGroups = async () => {
  groupLoading.value = true
  try {
    const params = { pageNum: groupQuery.pageNum, pageSize: groupQuery.pageSize }
    let data
    if (browseMode.value === 'all') {
      params.type = groupQuery.type
      params.keyword = groupQuery.keyword
      data = await getGroupBuys(params)
    } else {
      params.role = browseMode.value
      data = await getMyGroupBuys(params)
    }
    groups.value = data?.list || []
    groupTotal.value = Number(data?.total) || 0
  } catch (e) {
    // 拦截器已提示
  } finally {
    groupLoading.value = false
  }
}

const loadPartners = async () => {
  partnerLoading.value = true
  try {
    const params = { pageNum: partnerQuery.pageNum, pageSize: partnerQuery.pageSize }
    let data
    if (browseMode.value === 'all') {
      params.type = partnerQuery.type
      params.keyword = partnerQuery.keyword
      data = await getPartners(params)
    } else {
      params.role = browseMode.value
      data = await getMyPartners(params)
    }
    partners.value = data?.list || []
    partnerTotal.value = Number(data?.total) || 0
  } catch (e) {
    // 拦截器已提示
  } finally {
    partnerLoading.value = false
  }
}

const reloadList = () => {
  if (activeTab.value === 'group') {
    groupQuery.pageNum = 1
    loadGroups()
  } else {
    partnerQuery.pageNum = 1
    loadPartners()
  }
}

const changePage = page => {
  if (activeTab.value === 'group') {
    groupQuery.pageNum = page
    loadGroups()
  } else {
    partnerQuery.pageNum = page
    loadPartners()
  }
}

const changeType = value => {
  if (activeTab.value === 'group') groupQuery.type = value
  else partnerQuery.type = value
  reloadList()
}

const switchTab = tab => {
  if (activeTab.value === tab) return
  activeTab.value = tab
  reloadList()
}

const switchBrowseMode = mode => {
  if (mode !== 'all' && !isLoggedIn.value) {
    goLogin()
    return
  }
  if (browseMode.value === mode) return
  browseMode.value = mode
  reloadList()
}

const goLogin = () => router.push({ path: '/login', query: { redirect: '/together' } })

/* ==================== 发布 ==================== */

const goodsOptions = ref([])
const goodsLoading = ref(false)
const linkableGroups = ref([])

const searchGoods = async keyword => {
  if (!keyword) {
    goodsOptions.value = []
    return
  }
  goodsLoading.value = true
  try {
    const data = await getGoodsList({ keyword, pageNum: 1, pageSize: 10 })
    goodsOptions.value = data?.list || []
  } catch (e) {
    goodsOptions.value = []
  } finally {
    goodsLoading.value = false
  }
}

const groupDialog = ref(false)
const groupSubmitting = ref(false)
const groupForm = reactive({
  type: 'fresh',
  title: '',
  description: '',
  targetCount: 4,
  pricePerShare: null,
  deadline: '',
  location: '',
  goodsId: null
})

const resetGroupForm = () => {
  groupForm.type = 'fresh'
  groupForm.title = ''
  groupForm.description = ''
  groupForm.targetCount = 4
  groupForm.pricePerShare = null
  groupForm.deadline = ''
  groupForm.location = ''
  groupForm.goodsId = null
  goodsOptions.value = []
}

const openPublish = () => {
  if (!isLoggedIn.value) {
    goLogin()
    return
  }
  if (activeTab.value === 'group') {
    resetGroupForm()
    groupDialog.value = true
  } else {
    resetPartnerForm()
    loadLinkable()
    partnerDialog.value = true
  }
}

const submitGroup = async () => {
  if (!groupForm.title.trim()) {
    ElMessage.warning('请填写拼单标题')
    return
  }
  if (!groupForm.targetCount || groupForm.targetCount < 2) {
    ElMessage.warning('目标份数至少 2 份')
    return
  }
  if (!groupForm.pricePerShare || Number(groupForm.pricePerShare) <= 0) {
    ElMessage.warning('请填写每份费用')
    return
  }
  if (!groupForm.deadline) {
    ElMessage.warning('请选择截止时间')
    return
  }
  if (new Date(groupForm.deadline).getTime() <= Date.now()) {
    ElMessage.warning('截止时间需晚于当前时间')
    return
  }
  groupSubmitting.value = true
  try {
    await createGroupBuy({
      type: groupForm.type,
      title: groupForm.title.trim(),
      description: groupForm.description.trim(),
      targetCount: groupForm.targetCount,
      pricePerShare: groupForm.pricePerShare,
      deadline: groupForm.deadline,
      location: groupForm.location.trim(),
      goodsId: groupForm.goodsId
    })
    ElMessage.success('拼单已发布，等同学来拼')
    groupDialog.value = false
    browseMode.value = 'owner'
    activeTab.value = 'group'
    reloadList()
  } catch (e) {
    // 拦截器已提示
  } finally {
    groupSubmitting.value = false
  }
}

const partnerDialog = ref(false)
const partnerSubmitting = ref(false)
const partnerForm = reactive({
  type: 'gear',
  title: '',
  description: '',
  activityTime: '',
  location: '',
  memberLimit: 4,
  linkType: 'none',
  linkId: null
})

const resetPartnerForm = () => {
  partnerForm.type = 'gear'
  partnerForm.title = ''
  partnerForm.description = ''
  partnerForm.activityTime = ''
  partnerForm.location = ''
  partnerForm.memberLimit = 4
  partnerForm.linkType = 'none'
  partnerForm.linkId = null
  goodsOptions.value = []
}

const loadLinkable = async () => {
  try {
    linkableGroups.value = (await getLinkableGroupBuys()) || []
  } catch (e) {
    linkableGroups.value = []
  }
}

const onLinkTypeChange = () => {
  partnerForm.linkId = null
  if (partnerForm.linkType === 'goods') {
    goodsOptions.value = []
  }
  if (partnerForm.linkType === 'groupbuy' && !linkableGroups.value.length) {
    loadLinkable()
  }
}

const submitPartner = async () => {
  if (!partnerForm.title.trim()) {
    ElMessage.warning('请填写搭子标题')
    return
  }
  if (!partnerForm.activityTime) {
    ElMessage.warning('请选择活动时间')
    return
  }
  if (new Date(partnerForm.activityTime).getTime() <= Date.now()) {
    ElMessage.warning('活动时间需晚于当前时间')
    return
  }
  if (!partnerForm.memberLimit || partnerForm.memberLimit < 2) {
    ElMessage.warning('人数上限至少 2 人')
    return
  }
  if (partnerForm.linkType !== 'none' && !partnerForm.linkId) {
    ElMessage.warning('请选择要关联的内容')
    return
  }
  partnerSubmitting.value = true
  try {
    await createPartner({
      type: partnerForm.type,
      title: partnerForm.title.trim(),
      description: partnerForm.description.trim(),
      activityTime: partnerForm.activityTime,
      location: partnerForm.location.trim(),
      memberLimit: partnerForm.memberLimit,
      linkType: partnerForm.linkType,
      linkId: partnerForm.linkType === 'none' ? null : partnerForm.linkId
    })
    ElMessage.success('搭子已发布，等同学来组队')
    partnerDialog.value = false
    browseMode.value = 'owner'
    activeTab.value = 'partner'
    reloadList()
  } catch (e) {
    // 拦截器已提示
  } finally {
    partnerSubmitting.value = false
  }
}

const groupTypeHint = computed(() => groupTypeHints[groupForm.type] || '')
const partnerTypeHint = computed(() => partnerTypeHints[partnerForm.type] || '')

/* ==================== 参与 / 退出 / 确认 ==================== */

const joinDialog = ref(false)
const joinTarget = ref(null)
const joinShares = ref(1)
const joining = ref(false)

const joinPreview = computed(() => {
  if (!joinTarget.value) return 0
  return Number(joinTarget.value.pricePerShare || 0) * (Number(joinShares.value) || 0)
})

const openJoin = g => {
  if (!isLoggedIn.value) {
    goLogin()
    return
  }
  joinTarget.value = g
  joinShares.value = 1
  joinDialog.value = true
}

const submitJoin = async () => {
  if (!joinTarget.value) return
  joining.value = true
  try {
    await joinGroupBuy(joinTarget.value.id, joinShares.value)
    ElMessage.success('已参与拼单')
    joinDialog.value = false
    browseMode.value = 'member'
    reloadList()
  } catch (e) {
    // 拦截器已提示
  } finally {
    joining.value = false
  }
}

const handleLeaveGroup = async g => {
  try {
    await ElMessageBox.confirm(`确认退出《${g.title}》？已参与的 ${g.myShares} 份会释放回剩余份数。`, '退出拼单', {
      confirmButtonText: '确认退出',
      cancelButtonText: '再想想',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await leaveGroupBuy(g.id)
    ElMessage.success('已退出拼单')
    reloadList()
  } catch (e) {
    // 拦截器已提示
  }
}

const handleConfirmGroup = async g => {
  try {
    await ElMessageBox.confirm(`确认《${g.title}》成团？确认后会通知所有参与成员线下结算。`, '确认成团', {
      confirmButtonText: '确认成团',
      cancelButtonText: '再等等',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await confirmGroupBuy(g.id)
    ElMessage.success('已成团，请与成员线下结算')
    reloadList()
  } catch (e) {
    // 拦截器已提示
  }
}

const handleCloseGroup = async g => {
  try {
    await ElMessageBox.confirm(`确认关闭《${g.title}》？关闭后将不再接受报名，并通知已参与成员。`, '关闭拼单', {
      confirmButtonText: '确认关闭',
      cancelButtonText: '再想想',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await closeGroupBuy(g.id)
    ElMessage.success('拼单已关闭')
    reloadList()
  } catch (e) {
    // 拦截器已提示
  }
}

const handleJoinPartner = async p => {
  if (!isLoggedIn.value) {
    goLogin()
    return
  }
  try {
    await ElMessageBox.confirm(`确认加入《${p.title}》？活动时间 ${formatDateTime(p.activityTime)}。`, '加入搭子', {
      confirmButtonText: '确认加入',
      cancelButtonText: '再想想',
      type: 'info'
    })
  } catch (e) {
    return
  }
  try {
    await joinPartner(p.id)
    ElMessage.success('已加入搭子')
    browseMode.value = 'member'
    reloadList()
  } catch (e) {
    // 拦截器已提示
  }
}

const handleLeavePartner = async p => {
  try {
    await ElMessageBox.confirm(`确认退出《${p.title}》？`, '退出搭子', {
      confirmButtonText: '确认退出',
      cancelButtonText: '再想想',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await leavePartner(p.id)
    ElMessage.success('已退出搭子')
    reloadList()
  } catch (e) {
    // 拦截器已提示
  }
}

const handleCancelPartner = async p => {
  try {
    await ElMessageBox.confirm(`确认取消《${p.title}》？取消后将通知已加入成员。`, '取消搭子', {
      confirmButtonText: '确认取消',
      cancelButtonText: '再想想',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await cancelPartner(p.id)
    ElMessage.success('搭子已取消')
    reloadList()
  } catch (e) {
    // 拦截器已提示
  }
}

onMounted(() => {
  loadGroups()
  ticker = setInterval(() => {
    now.value = Date.now()
  }, 60000)
})

onBeforeUnmount(() => {
  if (ticker) clearInterval(ticker)
})
</script>

<style scoped>
.together-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 22px 26px;
  margin-bottom: 14px;
  border-radius: 12px;
  background: linear-gradient(135deg, #FFF7ED, #FEF2F2 55%, #EFF6FF);
  border: 1px solid #FDE68A;
}

.hero-eyebrow {
  font-size: 12px;
  font-weight: 700;
  color: #B45309;
  letter-spacing: 0.5px;
}

.hero-copy h1 {
  margin: 6px 0;
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

.primary-btn.small,
.ghost-btn.small {
  height: 34px;
  padding: 0 16px;
  font-size: 13px;
}

.ghost-btn {
  border: 1px solid #E5E7EB;
  background: #fff;
  color: #4B5563;
}

.ghost-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.main-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 14px;
}

.main-tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
  padding: 14px 18px;
  border-radius: 12px;
  border: 1px solid #EDEEF0;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.main-tab:hover {
  border-color: #FECACA;
}

.main-tab.active {
  border-color: #E1251B;
  background: #FFF7F6;
  box-shadow: 0 6px 16px rgba(225, 37, 27, 0.08);
}

.tab-title {
  font-size: 17px;
  font-weight: 800;
  color: #1F2937;
}

.main-tab.active .tab-title {
  color: #E1251B;
}

.tab-sub {
  font-size: 12px;
  color: #9CA3AF;
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

.mode-tabs {
  display: flex;
  gap: 4px;
  padding: 3px;
  border-radius: 9px;
  background: #F3F4F6;
}

.mode-tab {
  height: 26px;
  padding: 0 12px;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: #6B7280;
  font-size: 12.5px;
  cursor: pointer;
}

.mode-tab.active {
  background: #fff;
  color: #E1251B;
  font-weight: 700;
  box-shadow: 0 1px 3px rgba(31, 41, 55, 0.1);
}

.filter-note {
  font-size: 11.5px;
  color: #B0B4BA;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(290px, 1fr));
  gap: 14px;
}

.together-card {
  display: flex;
  flex-direction: column;
  padding: 16px;
  border-radius: 10px;
  border: 1px solid #EDEEF0;
  background: #FAFBFC;
  transition: box-shadow 0.16s ease, border-color 0.16s ease;
}

.together-card:hover {
  border-color: #FECACA;
  box-shadow: 0 8px 20px rgba(31, 41, 55, 0.06);
}

.card-top h3 {
  margin: 0 0 8px;
  font-size: 15px;
  color: #1F2937;
  line-height: 1.4;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag {
  display: inline-block;
  height: 20px;
  line-height: 20px;
  padding: 0 7px;
  border-radius: 5px;
  font-size: 11px;
  background: #F3F4F6;
  color: #6B7280;
}

.type-tag {
  background: #FFF7ED;
  color: #B45309;
}

.status-tag.open {
  background: #ECFDF5;
  color: #047857;
}

.status-tag.full {
  background: #EFF6FF;
  color: #1D4ED8;
}

.status-tag.formed,
.status-tag.finished {
  background: #F3F4F6;
  color: #4B5563;
}

.status-tag.failed,
.status-tag.cancelled {
  background: #FEF2F2;
  color: #B91C1C;
}

.status-tag.closed {
  background: #F3F4F6;
  color: #6B7280;
}

.card-desc {
  margin: 10px 0 12px;
  font-size: 12.5px;
  line-height: 1.7;
  color: #6B7280;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
}

.progress-block {
  margin-bottom: 12px;
}

.progress-head {
  display: flex;
  justify-content: space-between;
  font-size: 11.5px;
  color: #6B7280;
  margin-bottom: 5px;
}

.progress-track {
  height: 7px;
  border-radius: 4px;
  background: #EEF0F3;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  border-radius: 4px;
  background: linear-gradient(90deg, #FF9A62, #E1251B);
  transition: width 0.3s ease;
}

.card-price {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 10px;
}

.price-num {
  font-size: 20px;
  font-weight: 800;
  color: #E1251B;
}

.price-num.small {
  font-size: 14px;
}

.price-unit {
  font-size: 12px;
  color: #9CA3AF;
}

.activity-label {
  font-size: 12px;
  color: #9CA3AF;
}

.deadline-tag {
  margin-left: auto;
  font-size: 11.5px;
  color: #B45309;
  background: #FFF7ED;
  border-radius: 5px;
  padding: 2px 8px;
}

.deadline-tag.over {
  color: #9CA3AF;
  background: #F3F4F6;
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  margin-bottom: 12px;
  font-size: 11.5px;
  color: #9CA3AF;
}

.card-meta span {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.card-meta .link-tag {
  color: #1D4ED8;
}

.card-actions {
  margin-top: auto;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.done-tip {
  font-size: 12px;
  color: #9CA3AF;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.dialog-form .form-row {
  margin-bottom: 14px;
}

.form-row label {
  display: block;
  margin-bottom: 6px;
  font-size: 12.5px;
  font-weight: 600;
  color: #4B5563;
}

.form-row label i {
  color: #E1251B;
  font-style: normal;
}

.form-two {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.row-tip {
  margin-top: 5px;
  font-size: 11.5px;
  color: #9CA3AF;
}

.dialog-tip {
  padding: 9px 12px;
  border-radius: 8px;
  background: #FFF7ED;
  color: #B45309;
  font-size: 11.5px;
  line-height: 1.7;
}

.rent-summary {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  margin-bottom: 14px;
  border-radius: 9px;
  background: #F9FAFB;
  font-size: 13px;
  color: #4B5563;
}

.rent-summary strong {
  font-size: 15px;
  color: #1F2937;
}

.rent-summary .sub {
  font-size: 12px;
  color: #9CA3AF;
}

.rent-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  padding: 10px 12px;
  margin-bottom: 14px;
  border-radius: 8px;
  background: #FFF1F0;
  font-size: 12.5px;
  color: #92400E;
}

@media (max-width: 768px) {
  .together-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-actions {
    flex-direction: row;
  }

  .main-tabs {
    flex-direction: column;
  }

  .form-two {
    grid-template-columns: 1fr;
  }
}
</style>
