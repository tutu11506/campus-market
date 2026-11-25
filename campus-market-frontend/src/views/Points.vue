<template>
  <ServiceShell title="积分中心" subtitle="循环碳积分：每一次流转都在积累环保贡献" badge="绿">

    <!-- 总览卡片 -->
    <section class="hero-panel">
      <div class="hero-balance">
        <div class="hb-label">我的可用积分</div>
        <div class="hb-num">{{ account?.balance ?? 0 }}</div>
        <div class="hb-sub">累计获得 {{ account?.earnedTotal ?? 0 }} · 已消耗 {{ account?.spentTotal ?? 0 }}</div>
      </div>
      <div class="hero-stats">
        <div class="stat-card">
          <div class="stat-num">{{ myBadges.length }}</div>
          <div class="stat-label">已解锁勋章</div>
        </div>
        <div class="stat-card">
          <div class="stat-num">{{ totalPointsRules.postGoods }}</div>
          <div class="stat-label">发布闲置 +</div>
        </div>
        <div class="stat-card">
          <div class="stat-num">{{ totalPointsRules.sellGoods }}</div>
          <div class="stat-label">成交 +</div>
        </div>
        <div class="stat-card">
          <div class="stat-num">{{ totalPointsRules.donate }}</div>
          <div class="stat-label">捐赠 +</div>
        </div>
      </div>
    </section>

    <!-- Tab 切换 -->
    <el-tabs v-model="tab" class="main-tabs" @tab-change="onTabChange">
      <el-tab-pane label="📜 积分流水" name="records" />
      <el-tab-pane label="🎁 积分商城" name="rewards" />
      <el-tab-pane label="🏆 环保排行榜" name="leaderboard" />
      <el-tab-pane label="🏅 环保勋章" name="badges" />
    </el-tabs>

    <!-- Tab 1: 流水 -->
    <section v-if="tab === 'records'" class="tab-panel">
      <div v-if="recordsLoading" class="empty-state">加载中...</div>
      <div v-else-if="records.length === 0" class="empty-state">
        <div class="empty-emoji">🌱</div>
        <div>还没有积分记录哦，快去发布闲置/捐赠吧～</div>
      </div>
      <div v-else class="records-list">
        <div v-for="r in records" :key="r.id" class="record-row" :class="{ income: r.direction === 1, expense: r.direction === -1 }">
          <div class="rec-left">
            <div class="rec-type">{{ recordTypeLabel(r.type) }}</div>
            <div class="rec-remark">{{ r.remark || '-' }}</div>
            <div class="rec-time">{{ formatTime(r.createdAt) }}</div>
          </div>
          <div class="rec-right">
            <div class="rec-points" :class="r.direction === 1 ? 'plus' : 'minus'">
              {{ r.direction === 1 ? '+' : '-' }}{{ r.points }}
            </div>
            <div class="rec-balance">余额 {{ r.balanceAfter }}</div>
          </div>
        </div>
      </div>
      <div class="pagination" v-if="recordTotal > recordPageSize">
        <el-pagination background layout="prev, pager, next" :total="recordTotal" :page-size="recordPageSize" :current-page="recordPage" @current-change="onRecordPage" />
      </div>
    </section>

    <!-- Tab 2: 积分商城 -->
    <section v-if="tab === 'rewards'" class="tab-panel">
      <div v-if="rewardsLoading" class="empty-state">加载中...</div>
      <div v-else class="rewards-grid">
        <div v-for="r in rewards" :key="r.id" class="reward-card">
          <div class="reward-icon">{{ r.icon || giftEmoji(r.type) }}</div>
          <div class="reward-name">{{ r.name }}</div>
          <div class="reward-desc">{{ r.description || '' }}</div>
          <div class="reward-meta">
            <span class="reward-price">{{ r.pricePoints }} 分</span>
            <span v-if="r.stock != null" class="reward-stock">
              库存 {{ Math.max(0, r.stock - r.totalRedeemed) }}
            </span>
            <span v-else class="reward-stock unlimited">不限量</span>
          </div>
          <div class="reward-total">已兑 {{ r.totalRedeemed }}</div>
          <button class="exchange-btn" :disabled="!canExchange(r)" @click="onExchange(r)">
            {{ account?.balance < r.pricePoints ? '积分不足' : '立即兑换' }}
          </button>
        </div>
      </div>
    </section>

    <!-- Tab 3: 排行榜 -->
    <section v-if="tab === 'leaderboard'" class="tab-panel">
      <div class="lb-tabs">
        <button v-for="s in lbScopes" :key="s.value" class="lb-tab" :class="{ active: lbScope === s.value }" @click="switchLbScope(s.value)">{{ s.label }}</button>
      </div>
      <div v-if="lbLoading" class="empty-state">加载中...</div>
      <div v-else-if="!lbData.items || lbData.items.length === 0" class="empty-state">
        <div class="empty-emoji">🗳️</div>
        <div>排行榜数据还不够，快去流转闲置让榜单丰富起来～</div>
      </div>
      <div v-else>
        <div class="lb-title">{{ lbData.title }}</div>
        <table class="lb-table">
          <thead>
            <tr>
              <th style="width:60px">排名</th>
              <th>用户 / 学院</th>
              <th v-if="lbScope === 'user_balance'" class="right">积分余额</th>
              <th v-else-if="lbScope === 'user_cycled'" class="right">本月循环</th>
              <th v-else-if="lbScope === 'college'" class="right">总积分</th>
              <th v-else-if="lbScope === 'badge'" class="right">勋章数</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(it, idx) in lbData.items" :key="idx">
              <td class="lb-rank" :class="'rank-' + (idx + 1)">
                {{ idx < 3 ? ['🥇','🥈','🥉'][idx] : (idx + 1) }}
              </td>
              <td class="lb-user">
                <span v-if="lbScope.startsWith('user')" class="lb-name">
                  {{ it.username || ('学号 ' + (it.student_no || '***')) }}
                </span>
                <span v-else class="lb-name">{{ it.college || '未填写' }}</span>
                <span v-if="lbScope === 'college'" class="lb-sub">{{ it.user_count }} 人</span>
              </td>
              <td class="lb-num right" v-if="lbScope === 'user_balance'">{{ it.balance ?? '-' }}</td>
              <td class="lb-num right" v-else-if="lbScope === 'user_cycled'">{{ it.cycled_count ?? '-' }}</td>
              <td class="lb-num right" v-else-if="lbScope === 'college'">{{ it.total_balance ?? '-' }}</td>
              <td class="lb-num right" v-else-if="lbScope === 'badge'">{{ it.badge_count ?? '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- Tab 4: 勋章 -->
    <section v-if="tab === 'badges'" class="tab-panel">
      <div class="badge-head">
        <div>已解锁 {{ myBadges.length }} / {{ allBadges.length }}</div>
        <button class="ghost-btn-sm" @click="runCheckBadges" :disabled="!isLoggedIn">🔄 检查新勋章</button>
      </div>
      <div v-if="badgeLoading" class="empty-state">加载中...</div>
      <div v-else class="badge-grid">
        <div v-for="b in allBadges" :key="b.code" class="badge-card" :class="{ unlocked: hasUnlocked(b.id), locked: !hasUnlocked(b.id) }">
          <div class="badge-icon">{{ hasUnlocked(b.id) ? (b.icon || '🏅') : '🔒' }}</div>
          <div class="badge-name">{{ b.name }}</div>
          <div class="badge-desc">{{ b.description }}</div>
          <div class="badge-cond">
            达成条件：{{ badgeCondText(b) }}
            <span v-if="b.rewardPoints > 0" class="badge-cond-reward">· 额外奖励 {{ b.rewardPoints }} 分</span>
          </div>
        </div>
      </div>
    </section>

  </ServiceShell>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ServiceShell from '../components/ServiceShell.vue'
import {
  getPointAccount, getPointRecords, getPointRewards, exchangeReward,
  getLeaderboard, getMyBadges, getAllBadges, checkBadges
} from '../api/points'

const router = useRouter()

const isLoggedIn = computed(() => !!(localStorage.getItem('token') && localStorage.getItem('user')))
const goLogin = () => { ElMessage.info('请先登录'); router.push({ path: '/login', query: { redirect: '/points' } }) }

/* ==================== 固定积分规则 ==================== */
const totalPointsRules = { postGoods: 5, sellGoods: 15, postRental: 8, completeRental: 10, donate: 20 }

/* ==================== 总览 ==================== */
const account = ref(null)
const loadAccount = async () => {
  if (!isLoggedIn.value) return
  try { account.value = await getPointAccount() } catch (e) { account.value = null }
}

/* ==================== 流水 ==================== */
const tab = ref('records')
const records = ref([])
const recordsLoading = ref(false)
const recordPage = ref(1)
const recordPageSize = 20
const recordTotal = ref(0)

const loadRecords = async () => {
  if (!isLoggedIn.value) return
  recordsLoading.value = true
  try {
    const res = await getPointRecords(recordPage.value, recordPageSize)
    records.value = res?.list || []
    recordTotal.value = res?.total || 0
  } catch (e) { records.value = [] }
  finally { recordsLoading.value = false }
}
const onRecordPage = (p) => { recordPage.value = p; loadRecords() }

const recordTypeLabel = (t) => ({
  post_goods: '发布闲置', sell_goods: '成交闲置',
  post_rental: '发布可租', complete_rental: '完成租赁',
  donation: '公益捐赠', exchange: '积分兑换',
  admin_grant: '管理员发放', admin_deduct: '管理员扣减',
  badge_reward: '勋章奖励'
}[t] || t)

const formatTime = (s) => {
  if (!s) return ''
  const d = new Date(s.includes('T') ? s : s.replace(/-/g, '/'))
  if (isNaN(d.getTime())) return ''
  return `${d.getMonth() + 1}月${d.getDate()}日 ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

/* ==================== 商城 ==================== */
const rewards = ref([])
const rewardsLoading = ref(false)
const loadRewards = async () => {
  rewardsLoading.value = true
  try { rewards.value = await getPointRewards() || [] } catch (e) { rewards.value = [] }
  finally { rewardsLoading.value = false }
}
const giftEmoji = (t) => ({ coupon: '🎟️', top: '📌', stall: '🎪', badge: '🏅', cashback: '💰' }[t] || '🎁')
const canExchange = (r) => {
  if (!account.value) return false
  if (account.value.balance < r.pricePoints) return false
  if (r.stock != null && r.stock - r.totalRedeemed <= 0) return false
  return true
}
const onExchange = async (r) => {
  if (!isLoggedIn.value) return goLogin()
  try {
    await ElMessageBox.confirm(`确定用 ${r.pricePoints} 积分兑换「${r.name}」吗？`, '确认兑换', { type: 'warning' })
  } catch { return }
  try {
    await exchangeReward(r.id)
    ElMessage.success('兑换成功！')
    // 刷新账户 & 库存
    await loadAccount()
    await loadRewards()
    loadRecords()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '兑换失败')
  }
}

/* ==================== 排行榜 ==================== */
const lbScope = ref('user_balance')
const lbData = ref({ title: '', items: [] })
const lbLoading = ref(false)
const lbScopes = [
  { value: 'user_balance', label: '个人积分榜' },
  { value: 'user_cycled',  label: '本月循环榜' },
  { value: 'college',       label: '学院积分榜' },
  { value: 'badge',        label: '学院勋章榜' },
]
const switchLbScope = async (s) => { lbScope.value = s; await loadLb() }
const loadLb = async () => {
  lbLoading.value = true
  try { lbData.value = await getLeaderboard(lbScope.value) || { title: '', items: [] } }
  catch (e) { lbData.value = { title: '', items: [] } }
  finally { lbLoading.value = false }
}

/* ==================== 勋章 ==================== */
const myBadges = ref([])
const allBadges = ref([])
const badgeLoading = ref(false)
const loadMyBadges = async () => {
  if (!isLoggedIn.value) return
  try { myBadges.value = await getMyBadges() || [] } catch { myBadges.value = [] }
}
const loadAllBadges = async () => {
  try { allBadges.value = await getAllBadges() || [] } catch { allBadges.value = [] }
}
const hasUnlocked = (id) => myBadges.value.some(b => b.id === id)
const badgeCondText = (b) => ({
  total_points: `累计获得 ${b.conditionValue} 积分`,
  goods_sold:   `成交 ${b.conditionValue} 件闲置`,
  donation_count: `捐赠 ${b.conditionValue} 次`,
  rental_complete: `完成 ${b.conditionValue} 次租赁`
}[b.conditionType] || `${b.conditionType} ≥ ${b.conditionValue}`)
const runCheckBadges = async () => {
  badgeLoading.value = true
  try {
    const unlocked = await checkBadges()
    if (unlocked > 0) { ElMessage.success(`恭喜！解锁了 ${unlocked} 枚新勋章 🎉`) }
    else { ElMessage.info('暂未达成新勋章条件，继续加油～') }
    await loadMyBadges()
    await loadAccount()
  } catch (e) { ElMessage.error('检查失败') }
  finally { badgeLoading.value = false }
}

/* ==================== Tab 切换 ==================== */
const onTabChange = (n) => {
  if (n === 'records') loadRecords()
  else if (n === 'rewards') loadRewards()
  else if (n === 'leaderboard') loadLb()
  else if (n === 'badges') { loadAllBadges(); loadMyBadges() }
}

/* ==================== 启动 ==================== */
onMounted(async () => {
  await Promise.all([loadAccount(), loadRewards(), loadAllBadges(), loadLb()])
  if (isLoggedIn.value) {
    await Promise.all([loadRecords(), loadMyBadges()])
  }
})
</script>

<style scoped>
.main-tabs { margin-bottom: 20px; }
.tab-panel { padding: 6px 0 20px; }

/* Hero */
.hero-panel {
  display: flex; gap: 24px; padding: 24px 28px;
  margin-bottom: 16px;
  border-radius: 14px;
  background: linear-gradient(135deg, #ECFDF5 0%, #F0FDF4 55%, #FEF9C3);
  border: 1px solid #A7F3D0;
}
.hero-balance { flex: 1; }
.hb-label { font-size: 12px; font-weight: 700; color: #047857; letter-spacing: .5px; }
.hb-num { font-size: 52px; font-weight: 900; color: #065F46; font-variant-numeric: tabular-nums; line-height: 1; margin: 6px 0 8px; }
.hb-sub { font-size: 13px; color: #065F46; opacity: .75; }
.hero-stats { display: flex; gap: 14px; align-items: center; }
.stat-card {
  padding: 10px 16px; border-radius: 10px;
  background: rgba(255,255,255,0.8);
  text-align: center; min-width: 84px;
}
.stat-num { font-size: 20px; font-weight: 800; color: #059669; }
.stat-label { font-size: 11px; color: #065F46; opacity: .7; margin-top: 2px; }

/* 流水 */
.empty-state { padding: 40px 0; text-align: center; color: #9CA3AF; font-size: 14px; }
.empty-emoji { font-size: 48px; margin-bottom: 10px; }
.records-list { display: flex; flex-direction: column; gap: 10px; }
.record-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 18px; border-radius: 10px; background: #fff;
  border-left: 4px solid transparent; box-shadow: 0 1px 3px rgba(0,0,0,.03);
}
.record-row.income { border-left-color: #10B981; }
.record-row.expense { border-left-color: #F59E0B; }
.rec-left .rec-type { font-weight: 700; color: #1F2937; font-size: 14px; }
.rec-left .rec-remark { font-size: 12px; color: #6B7280; margin-top: 2px; }
.rec-left .rec-time { font-size: 11px; color: #9CA3AF; margin-top: 2px; }
.rec-right { text-align: right; }
.rec-points { font-size: 18px; font-weight: 800; font-variant-numeric: tabular-nums; }
.rec-points.plus { color: #059669; }
.rec-points.minus { color: #D97706; }
.rec-balance { font-size: 11px; color: #9CA3AF; margin-top: 2px; }
.pagination { display: flex; justify-content: center; margin-top: 20px; }

/* 商城 */
.rewards-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 16px; }
.reward-card {
  padding: 18px; border-radius: 12px; background: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,.04); display: flex; flex-direction: column;
  transition: transform .15s ease, box-shadow .15s ease;
}
.reward-card:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(0,0,0,.06); }
.reward-icon { font-size: 36px; margin-bottom: 8px; }
.reward-name { font-size: 15px; font-weight: 700; color: #1F2937; }
.reward-desc { font-size: 12px; color: #6B7280; margin-top: 6px; flex: 1; min-height: 32px; }
.reward-meta { display: flex; justify-content: space-between; margin-top: 10px; font-size: 12px; }
.reward-price { font-weight: 800; color: #DC2626; font-size: 15px; }
.reward-stock { color: #F59E0B; }
.reward-stock.unlimited { color: #10B981; }
.reward-total { font-size: 11px; color: #9CA3AF; margin-top: 4px; }
.exchange-btn {
  margin-top: 12px; padding: 8px 0; border-radius: 8px; border: none;
  background: linear-gradient(135deg, #10B981, #059669); color: #fff;
  font-weight: 700; cursor: pointer; font-size: 13px;
}
.exchange-btn:disabled { background: #D1D5DB; cursor: not-allowed; }

/* 排行榜 */
.lb-tabs { display: flex; gap: 10px; margin-bottom: 18px; }
.lb-tab {
  padding: 8px 16px; border-radius: 20px; border: 1px solid #E5E7EB;
  background: #fff; color: #4B5563; font-size: 13px; font-weight: 600;
  cursor: pointer; transition: all .15s ease;
}
.lb-tab.active { background: #059669; color: #fff; border-color: #059669; }
.lb-title { font-size: 16px; font-weight: 700; color: #065F46; margin-bottom: 10px; }
.lb-table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 10px; overflow: hidden; }
.lb-table th, .lb-table td { padding: 12px 16px; text-align: left; font-size: 14px; border-bottom: 1px solid #F3F4F6; }
.lb-table th { background: #F9FAFB; color: #374151; font-size: 12px; font-weight: 700; }
.lb-table tr:last-child td { border-bottom: none; }
.lb-num.right, th.right { text-align: right; font-variant-numeric: tabular-nums; }
.lb-rank { font-weight: 800; font-size: 14px; color: #6B7280; }
.lb-rank.rank-1, .lb-rank.rank-2, .lb-rank.rank-3 { font-size: 20px; }
.lb-user { display: flex; align-items: center; gap: 8px; }
.lb-name { font-weight: 600; color: #1F2937; }
.lb-sub { font-size: 11px; color: #9CA3AF; }

/* 勋章 */
.badge-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; font-size: 13px; color: #4B5563; }
.ghost-btn-sm {
  padding: 6px 12px; border-radius: 8px; border: 1px solid #10B981; background: #fff;
  color: #059669; font-size: 12px; font-weight: 600; cursor: pointer;
}
.ghost-btn-sm:disabled { opacity: .5; cursor: not-allowed; }
.badge-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 14px; }
.badge-card {
  padding: 18px; border-radius: 12px; text-align: center;
  border: 1px solid #E5E7EB; background: #fff;
  transition: all .15s ease;
}
.badge-card.unlocked { border-color: #A7F3D0; background: linear-gradient(135deg, #ECFDF5, #F0FDF4); }
.badge-card.locked { opacity: .5; filter: grayscale(60%); }
.badge-icon { font-size: 40px; margin-bottom: 6px; }
.badge-name { font-size: 14px; font-weight: 700; color: #1F2937; }
.badge-desc { font-size: 11px; color: #6B7280; margin-top: 4px; }
.badge-cond { font-size: 11px; color: #9CA3AF; margin-top: 8px; }
.badge-cond-reward { color: #DC2626; font-weight: 600; }
</style>
