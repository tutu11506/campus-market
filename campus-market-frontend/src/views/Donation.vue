<template>
  <ServiceShell title="公益捐赠" subtitle="让闲置衣物与书籍继续发挥作用" badge="益">
    <section class="donation-hero">
      <div class="hero-copy">
        <span class="hero-eyebrow">校园公益 · 爱心接力</span>
        <h1>不卖的闲置，也能成为别人的需要</h1>
        <p>登记不再需要的衣物或书籍，选择校内公益社团完成交接。平台分别统计登记与实收，让每一份爱心都有记录。</p>
        <div class="hero-tags">
          <span><el-icon><CircleCheck /></el-icon>校内社团对接</span>
          <span><el-icon><CircleCheck /></el-icon>实收确认后计入成果</span>
        </div>
      </div>
      <div class="hero-symbol"><el-icon><Present /></el-icon></div>
    </section>

    <div class="demo-notice">
      当前社团名称、联系方式和接收点均为演示资料，仅用于体验捐赠流程。
    </div>

    <section class="stats-grid" v-loading="overviewLoading">
      <div class="stat-card clothes">
        <div class="stat-icon"><el-icon><Box /></el-icon></div>
        <div class="stat-content">
          <span class="stat-label">登记衣物</span>
          <div><strong>{{ stats.registeredClothes }}</strong><span class="stat-unit">件</span></div>
          <span class="stat-sub">实际接收 {{ stats.receivedClothes }} 件</span>
        </div>
      </div>
      <div class="stat-card books">
        <div class="stat-icon"><el-icon><Reading /></el-icon></div>
        <div class="stat-content">
          <span class="stat-label">登记书籍</span>
          <div><strong>{{ stats.registeredBooks }}</strong><span class="stat-unit">本</span></div>
          <span class="stat-sub">实际接收 {{ stats.receivedBooks }} 本</span>
        </div>
      </div>
      <div class="stat-card donors">
        <div class="stat-icon"><el-icon><User /></el-icon></div>
        <div class="stat-content">
          <span class="stat-label">爱心捐赠者</span>
          <div><strong>{{ stats.donorCount }}</strong><span class="stat-unit">人</span></div>
          <span class="stat-sub">按有效捐赠登记去重</span>
        </div>
      </div>
      <div class="stat-card records">
        <div class="stat-icon"><el-icon><Document /></el-icon></div>
        <div class="stat-content">
          <span class="stat-label">有效登记</span>
          <div><strong>{{ stats.recordCount }}</strong><span class="stat-unit">条</span></div>
          <span class="stat-sub">取消登记不计入统计</span>
        </div>
      </div>
    </section>

    <div class="donation-layout">
      <section class="panel form-panel" ref="formSection">
        <div class="panel-head">
          <div>
            <h2>我要捐赠</h2>
            <p>填写物资和交接信息，提交后可在“我的捐赠”查看进度。</p>
          </div>
          <span class="step-tag">登记捐赠</span>
        </div>

        <el-form label-position="top" @submit.prevent>
          <div class="field-group">
            <label class="field-label">物资类型</label>
            <el-radio-group v-model="form.itemType">
              <el-radio-button value="clothes">衣物</el-radio-button>
              <el-radio-button value="books">书籍</el-radio-button>
            </el-radio-group>
          </div>

          <div class="field-group">
            <label class="field-label required">物品名称</label>
            <el-input
              v-model="form.itemName"
              maxlength="100"
              :placeholder="form.itemType === 'clothes' ? '例如：冬季外套、运动服' : '例如：高等数学教材、课外读物'"
            />
          </div>

          <div class="form-two-columns">
            <div class="field-group">
              <label class="field-label required">数量（{{ form.itemType === 'clothes' ? '件' : '本' }}）</label>
              <el-input-number v-model="form.quantity" :min="1" :max="999" :precision="0" controls-position="right" />
            </div>
            <div class="field-group">
              <label class="field-label required">物品成色</label>
              <el-select v-model="form.conditionLevel">
                <el-option label="全新" value="new" />
                <el-option label="良好" value="good" />
                <el-option label="一般，仍可使用" value="fair" />
              </el-select>
            </div>
          </div>

          <div class="field-group">
            <label class="field-label required">接收社团</label>
            <el-select v-model="form.orgId" placeholder="选择对接的公益社团" no-data-text="暂无可接收此类物资的社团">
              <el-option
                v-for="org in availableOrganizations"
                :key="org.id"
                :label="org.name"
                :value="org.id"
              />
            </el-select>
            <div class="selected-org-tip" v-if="selectedOrganization">
              <span><el-icon><Location /></el-icon>{{ selectedOrganization.dropoffAddress }}</span>
              <span>{{ selectedOrganization.contact }}</span>
            </div>
          </div>

          <div class="field-group">
            <label class="field-label required">交接方式</label>
            <el-radio-group v-model="form.handoverMethod" class="handover-options">
              <el-radio value="dropoff" border>自行送到接收点</el-radio>
              <el-radio value="pickup" border>预约宿舍取件</el-radio>
            </el-radio-group>
          </div>

          <div class="field-group">
            <label class="field-label required">交接说明</label>
            <el-input
              v-model="form.handoverDetail"
              type="textarea"
              :rows="2"
              maxlength="255"
              show-word-limit
              :placeholder="form.handoverMethod === 'dropoff' ? '请填写预计送达时间，例如：工作日 18:00 后' : '请填写宿舍楼、房间及方便取件的时间'"
            />
          </div>

          <div class="field-group">
            <label class="field-label required">联系方式</label>
            <el-input v-model="form.contact" maxlength="50" placeholder="手机号或校内联系方式，仅供本人、管理员和对接使用" />
          </div>

          <div class="field-group">
            <label class="field-label">备注（选填）</label>
            <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="补充尺码、数量明细、书籍版本等信息" />
          </div>

          <button class="submit-donation" type="button" :disabled="submitting" @click="submitDonation">
            <el-icon v-if="!submitting"><Present /></el-icon>
            {{ submitting ? '正在提交…' : isLoggedIn ? '提交捐赠登记' : '登录后提交捐赠' }}
          </button>
          <p class="form-footnote">提交后属于捐赠意向登记；管理员依据社团回执确认后，才会计入实际接收。</p>
        </el-form>
      </section>

      <section class="panel organizations-panel">
        <div class="panel-head">
          <div>
            <h2>对接公益社团</h2>
            <p>按物资类型选择接收方，并提前确认交接要求。</p>
          </div>
          <span class="demo-tag">演示资料</span>
        </div>

        <div class="organization-list" v-loading="overviewLoading">
          <article
            v-for="org in organizations"
            :key="org.id"
            class="organization-card"
            :class="{ selected: form.orgId === org.id }"
          >
            <div class="org-title-row">
              <div>
                <h3>{{ org.name }}</h3>
                <div class="org-types">
                  <span v-for="type in acceptedTypes(org)" :key="type">{{ itemTypeText(type) }}</span>
                </div>
              </div>
              <el-icon class="org-icon"><Present /></el-icon>
            </div>
            <p class="org-intro">{{ org.intro }}</p>
            <div class="org-detail"><el-icon><Location /></el-icon><span>{{ org.dropoffAddress }}</span></div>
            <div class="org-detail"><el-icon><Phone /></el-icon><span>{{ org.contact }}</span></div>
            <div class="org-stats">
              <span>登记：衣物 {{ orgStatsFor(org).registeredClothes }} 件 / 书籍 {{ orgStatsFor(org).registeredBooks }} 本</span>
              <span>实收：衣物 {{ orgStatsFor(org).receivedClothes }} 件 / 书籍 {{ orgStatsFor(org).receivedBooks }} 本</span>
            </div>
            <button type="button" class="choose-org" @click="chooseOrganization(org)">
              {{ form.orgId === org.id ? '当前已选择' : '选择该社团' }}
            </button>
          </article>
        </div>

        <div class="donation-guide">
          <h3>捐赠前请注意</h3>
          <p>衣物请清洁、无严重破损；书籍请保持内容完整，不影响正常阅读。</p>
          <p>预约宿舍取件时，请准确填写楼栋和方便取件的时间，并以社团后续确认为准。</p>
        </div>
      </section>
    </div>

    <section class="panel my-donations">
      <div class="panel-head">
        <div>
          <h2>我的捐赠</h2>
          <p>待交接时可取消；完成实收后可查看实际接收数量。</p>
        </div>
        <el-button v-if="isLoggedIn" text type="success" :loading="myLoading" @click="loadMy">刷新记录</el-button>
      </div>

      <div v-if="!isLoggedIn" class="login-empty">
        <el-icon><User /></el-icon>
        <p>登录后查看自己的捐赠记录和实收进度</p>
        <el-button type="success" @click="goLogin">登录查看</el-button>
      </div>
      <div v-else v-loading="myLoading">
        <div v-if="myRecords.length" class="record-list">
          <article v-for="record in myRecords" :key="record.id" class="donation-record">
            <div class="record-head">
              <div class="record-title">
                <el-tag :type="record.itemType === 'clothes' ? 'success' : 'primary'" effect="light">
                  {{ itemTypeText(record.itemType) }}
                </el-tag>
                <strong>{{ record.itemName }}</strong>
              </div>
              <el-tag :type="statusMeta(record.status).type" effect="light">{{ statusMeta(record.status).label }}</el-tag>
            </div>
            <div class="record-grid">
              <span>登记数量：{{ record.quantity }} {{ unitFor(record.itemType) }}</span>
              <span>物品成色：{{ conditionText(record.conditionLevel) }}</span>
              <span>接收社团：{{ record.organizationName }}</span>
              <span>交接方式：{{ handoverText(record.handoverMethod) }}</span>
              <span class="wide">交接说明：{{ record.handoverDetail }}</span>
              <span>登记时间：{{ formatTime(record.createTime) }}</span>
            </div>
            <div v-if="record.status === 'received'" class="received-info">
              社团实际接收 {{ record.receivedQuantity }} {{ unitFor(record.itemType) }}，确认时间 {{ formatTime(record.receivedTime) }}
            </div>
            <div v-if="record.remark" class="record-remark">备注：{{ record.remark }}</div>
            <div v-if="record.status === 'pending'" class="record-actions">
              <el-button type="danger" plain size="small" @click="confirmCancel(record)">取消登记</el-button>
            </div>
          </article>
        </div>
        <el-empty v-else description="还没有捐赠记录，从上方提交第一份爱心物资吧" :image-size="90" />
        <div class="record-pagination" v-if="myTotal > myPageSize">
          <el-pagination
            background
            layout="total, prev, pager, next"
            :total="myTotal"
            :current-page="myPage"
            :page-size="myPageSize"
            @current-change="changeMyPage"
          />
        </div>
      </div>
    </section>
  </ServiceShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Box, CircleCheck, Document, Location, Phone, Present, Reading, User } from '@element-plus/icons-vue'
import ServiceShell from '../components/ServiceShell.vue'
import { cancelDonation, createDonation, getDonationOverview, getMyDonations } from '../api/donation'

const router = useRouter()
const formSection = ref(null)
const overviewLoading = ref(false)
const myLoading = ref(false)
const submitting = ref(false)
const organizations = ref([])
const organizationStats = ref([])
const stats = reactive({
  registeredClothes: 0,
  registeredBooks: 0,
  receivedClothes: 0,
  receivedBooks: 0,
  donorCount: 0,
  recordCount: 0
})
const form = reactive({
  itemType: 'clothes',
  itemName: '',
  quantity: 1,
  conditionLevel: 'good',
  orgId: null,
  handoverMethod: 'dropoff',
  handoverDetail: '',
  contact: '',
  remark: ''
})
const myRecords = ref([])
const myTotal = ref(0)
const myPage = ref(1)
const myPageSize = 5

const isLoggedIn = computed(() => !!(localStorage.getItem('token') && localStorage.getItem('user')))
const availableOrganizations = computed(() => organizations.value.filter(org => acceptedTypes(org).includes(form.itemType)))
const selectedOrganization = computed(() => organizations.value.find(org => org.id === form.orgId))

const acceptedTypes = org => String(org.acceptTypes || '').split(',').map(type => type.trim()).filter(Boolean)
const itemTypeText = type => ({ clothes: '衣物', books: '书籍' }[type] || type)
const unitFor = type => type === 'books' ? '本' : '件'
const conditionText = value => ({ new: '全新', good: '良好', fair: '一般，仍可使用' }[value] || value)
const handoverText = value => value === 'pickup' ? '预约宿舍取件' : '自行送到接收点'
const statusMeta = value => ({
  pending: { label: '待交接', type: 'warning' },
  received: { label: '已接收', type: 'success' },
  cancelled: { label: '已取消', type: 'info' }
}[value] || { label: value, type: 'info' })
const formatTime = value => {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false })
}
const orgStatsFor = org => organizationStats.value.find(item => item.orgId === org.id) || {
  registeredClothes: 0,
  registeredBooks: 0,
  receivedClothes: 0,
  receivedBooks: 0
}

const ensureOrganization = () => {
  if (!availableOrganizations.value.some(org => org.id === form.orgId)) {
    form.orgId = availableOrganizations.value[0]?.id || null
  }
}

watch(() => form.itemType, ensureOrganization)

const loadOverview = async () => {
  overviewLoading.value = true
  try {
    const data = await getDonationOverview()
    organizations.value = data?.organizations || []
    organizationStats.value = data?.organizationStats || []
    Object.keys(stats).forEach(key => { stats[key] = Number(data?.stats?.[key]) || 0 })
    ensureOrganization()
  } catch (e) {
    // 请求拦截器已提示
  } finally {
    overviewLoading.value = false
  }
}

const loadMy = async () => {
  if (!isLoggedIn.value) return
  myLoading.value = true
  try {
    const data = await getMyDonations({ pageNum: myPage.value, pageSize: myPageSize })
    myRecords.value = data?.list || []
    myTotal.value = Number(data?.total) || 0
    if (!myRecords.value.length && myPage.value > 1) {
      myPage.value -= 1
      await loadMy()
    }
  } catch (e) {
    // 请求拦截器已提示
  } finally {
    myLoading.value = false
  }
}

const goLogin = () => router.push({ path: '/login', query: { redirect: '/donation' } })

const chooseOrganization = org => {
  if (!acceptedTypes(org).includes(form.itemType)) {
    form.itemType = acceptedTypes(org)[0] || form.itemType
  }
  form.orgId = org.id
  formSection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const submitDonation = async () => {
  if (!isLoggedIn.value) {
    goLogin()
    return
  }
  if (!form.itemName.trim()) {
    ElMessage.warning('请填写物品名称')
    return
  }
  if (!form.orgId) {
    ElMessage.warning('请选择接收社团')
    return
  }
  if (!form.handoverDetail.trim()) {
    ElMessage.warning('请填写交接时间、地点或宿舍说明')
    return
  }
  if (!form.contact.trim()) {
    ElMessage.warning('请填写联系方式')
    return
  }
  submitting.value = true
  try {
    await createDonation({
      itemType: form.itemType,
      itemName: form.itemName.trim(),
      quantity: form.quantity,
      conditionLevel: form.conditionLevel,
      orgId: form.orgId,
      handoverMethod: form.handoverMethod,
      handoverDetail: form.handoverDetail.trim(),
      contact: form.contact.trim(),
      remark: form.remark.trim()
    })
    ElMessage.success('捐赠登记成功，请按交接说明与社团完成交接')
    form.itemName = ''
    form.quantity = 1
    form.handoverDetail = ''
    form.remark = ''
    myPage.value = 1
    await Promise.all([loadOverview(), loadMy()])
  } catch (e) {
    // 请求拦截器已提示
  } finally {
    submitting.value = false
  }
}

const confirmCancel = async record => {
  try {
    await ElMessageBox.confirm(
      `确定取消“${record.itemName}”的捐赠登记吗？取消后不再计入平台统计。`,
      '取消捐赠登记',
      { type: 'warning', confirmButtonText: '确定取消', cancelButtonText: '保留登记' }
    )
    await cancelDonation(record.id)
    ElMessage.success('捐赠登记已取消')
    await Promise.all([loadOverview(), loadMy()])
  } catch (e) {
    // 用户取消确认或请求失败时无需重复提示
  }
}

const changeMyPage = page => {
  myPage.value = page
  loadMy()
}

onMounted(() => {
  loadOverview()
  loadMy()
})
</script>

<style scoped>
.donation-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
  padding: 30px 34px;
  border: 1px solid #d7eee0;
  border-radius: 18px;
  background: linear-gradient(120deg, #effaf3 0%, #f8fcf8 58%, #eaf6ef 100%);
}
.hero-copy { max-width: 760px; }
.hero-eyebrow { color: #278254; font-size: 13px; font-weight: 700; letter-spacing: 1px; }
.hero-copy h1 { margin: 10px 0 12px; color: #174c32; font-size: 28px; line-height: 1.35; }
.hero-copy p { margin: 0; color: #5f7166; font-size: 14px; line-height: 1.8; }
.hero-tags { display: flex; flex-wrap: wrap; gap: 18px; margin-top: 18px; color: #278254; font-size: 13px; }
.hero-tags span { display: inline-flex; align-items: center; gap: 5px; }
.hero-symbol {
  width: 104px;
  height: 104px;
  flex: 0 0 104px;
  display: grid;
  place-items: center;
  border-radius: 28px;
  color: #fff;
  background: linear-gradient(145deg, #42ae72, #237a4d);
  box-shadow: 0 14px 32px rgba(35, 122, 77, .2);
  transform: rotate(4deg);
}
.hero-symbol .el-icon { font-size: 52px; }
.demo-notice { margin: 12px 2px 0; color: #8b6b2f; font-size: 12px; }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin: 16px 0; }
.stat-card { display: flex; align-items: center; gap: 14px; min-width: 0; padding: 18px; border: 1px solid #ecefeb; border-radius: 14px; background: #fff; }
.stat-icon { width: 42px; height: 42px; flex: 0 0 42px; display: grid; place-items: center; border-radius: 12px; color: #288355; background: #eaf7ef; font-size: 21px; }
.stat-card.books .stat-icon { color: #4778c7; background: #edf3fc; }
.stat-card.donors .stat-icon { color: #a06a25; background: #fff5e5; }
.stat-card.records .stat-icon { color: #7855ad; background: #f3eefb; }
.stat-content { min-width: 0; }
.stat-label { color: #70786f; font-size: 12px; }
.stat-content strong { margin-right: 5px; color: #25362c; font-size: 27px; line-height: 1.35; }
.stat-unit { color: #59665e; font-size: 12px; }
.stat-sub { display: block; overflow: hidden; color: #929990; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.donation-layout { display: grid; grid-template-columns: 1.06fr .94fr; gap: 16px; align-items: start; }
.panel { padding: 22px; border: 1px solid #ecefeb; border-radius: 16px; background: #fff; }
.panel-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 20px; }
.panel-head h2 { margin: 0 0 6px; color: #28382f; font-size: 19px; }
.panel-head p { margin: 0; color: #8a918a; font-size: 12px; line-height: 1.6; }
.step-tag, .demo-tag { flex-shrink: 0; padding: 4px 9px; border-radius: 20px; color: #2d8356; background: #eaf7ef; font-size: 11px; }
.demo-tag { color: #946927; background: #fff4df; }
.field-group { margin-bottom: 17px; }
.field-label { display: block; margin-bottom: 7px; color: #46544b; font-size: 13px; font-weight: 600; }
.field-label.required::before { margin-right: 3px; color: #d55252; content: '*'; }
.field-group :deep(.el-input), .field-group :deep(.el-select), .field-group :deep(.el-input-number) { width: 100%; }
.form-two-columns { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.handover-options { display: flex; width: 100%; }
.handover-options :deep(.el-radio) { flex: 1; margin-right: 10px; }
.handover-options :deep(.el-radio:last-child) { margin-right: 0; }
.selected-org-tip { display: flex; flex-direction: column; gap: 4px; margin-top: 8px; padding: 9px 11px; border-radius: 8px; color: #65756b; background: #f5f8f5; font-size: 12px; }
.selected-org-tip span { display: flex; align-items: center; gap: 5px; }
.submit-donation { display: flex; align-items: center; justify-content: center; gap: 7px; width: 100%; height: 44px; border: 0; border-radius: 10px; color: #fff; background: #278254; font-size: 15px; font-weight: 700; cursor: pointer; transition: background .2s; }
.submit-donation:hover { background: #1f6e46; }
.submit-donation:disabled { opacity: .65; cursor: not-allowed; }
.form-footnote { margin: 10px 0 0; color: #929990; font-size: 11px; line-height: 1.6; text-align: center; }
.organization-list { display: flex; flex-direction: column; gap: 13px; }
.organization-card { padding: 17px; border: 1px solid #e7ece7; border-radius: 13px; transition: border-color .2s, box-shadow .2s; }
.organization-card.selected { border-color: #66ad85; box-shadow: 0 0 0 2px rgba(39, 130, 84, .08); }
.org-title-row { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.org-title-row h3 { margin: 0 0 8px; color: #31463a; font-size: 16px; }
.org-icon { color: #54a377; font-size: 25px; }
.org-types { display: flex; gap: 6px; }
.org-types span { padding: 2px 7px; border-radius: 4px; color: #4a765c; background: #edf6f0; font-size: 10px; }
.org-intro { margin: 11px 0; color: #69766e; font-size: 12px; line-height: 1.7; }
.org-detail { display: flex; align-items: flex-start; gap: 6px; margin-top: 7px; color: #7a857d; font-size: 12px; }
.org-detail .el-icon { flex-shrink: 0; margin-top: 2px; color: #4f966e; }
.org-stats { display: flex; flex-direction: column; gap: 4px; margin-top: 12px; padding: 9px 10px; border-radius: 8px; color: #78847b; background: #f7f9f7; font-size: 11px; }
.choose-org { width: 100%; margin-top: 12px; padding: 8px; border: 1px solid #a8cdb6; border-radius: 8px; color: #278254; background: #fff; font-size: 12px; cursor: pointer; }
.organization-card.selected .choose-org { color: #fff; background: #278254; }
.donation-guide { margin-top: 16px; padding: 14px 15px; border-radius: 10px; background: #f7f8f6; }
.donation-guide h3 { margin: 0 0 8px; color: #59675e; font-size: 13px; }
.donation-guide p { margin: 5px 0 0; color: #899188; font-size: 11px; line-height: 1.65; }
.my-donations { margin-top: 16px; }
.login-empty { padding: 28px 0 20px; color: #879088; text-align: center; }
.login-empty > .el-icon { color: #65a982; font-size: 34px; }
.login-empty p { margin: 10px 0 15px; font-size: 13px; }
.record-list { display: flex; flex-direction: column; gap: 12px; }
.donation-record { padding: 16px; border: 1px solid #e8ece8; border-radius: 12px; }
.record-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.record-title { display: flex; align-items: center; gap: 9px; color: #35463b; font-size: 14px; }
.record-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px 24px; margin-top: 13px; color: #788279; font-size: 12px; }
.record-grid .wide { grid-column: 1 / -1; }
.received-info { margin-top: 12px; padding: 9px 11px; border-radius: 8px; color: #26774c; background: #edf8f1; font-size: 12px; }
.record-remark { margin-top: 10px; color: #8a918b; font-size: 12px; }
.record-actions { margin-top: 12px; text-align: right; }
.record-pagination { display: flex; justify-content: center; margin-top: 18px; }
@media (max-width: 1100px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 900px) {
  .donation-layout { grid-template-columns: 1fr; }
  .hero-symbol { width: 82px; height: 82px; flex-basis: 82px; }
}
@media (max-width: 600px) {
  .donation-hero { padding: 23px 20px; }
  .hero-copy h1 { font-size: 23px; }
  .hero-symbol { display: none; }
  .stats-grid { gap: 9px; }
  .stat-card { gap: 9px; padding: 13px 11px; }
  .stat-icon { width: 34px; height: 34px; flex-basis: 34px; font-size: 17px; }
  .stat-content strong { font-size: 22px; }
  .stat-sub { white-space: normal; }
  .panel { padding: 17px 15px; }
  .form-two-columns { grid-template-columns: 1fr; gap: 0; }
  .handover-options { flex-direction: column; gap: 8px; }
  .handover-options :deep(.el-radio) { width: 100%; margin-right: 0; }
  .record-grid { grid-template-columns: 1fr; }
  .record-grid .wide { grid-column: auto; }
  .record-head { align-items: flex-start; }
  .record-title { align-items: flex-start; }
}
</style>
