<template>
  <ServiceShell title="教材循环站" subtitle="导入课表一键匹配教材，避雷标记让同学少走弯路" badge="书">

    <!-- 功能切换：旧"教材价格指数"保留但折叠 -->
    <section class="hero-panel">
      <div class="hero-copy">
        <span class="hero-eyebrow">🎒 开学季，一键把这学期要的书找齐</span>
        <h1>课表自动匹配教材</h1>
        <p>把你的学期课表一键导入，系统自动去平台找对应的二手闲置和可租版本，告诉你还差哪些。标注老师指定版本，其他人踩过的坑也能让你看见。</p>
        <div class="hero-tags">
          <span><el-icon><Document /></el-icon>粘贴课表或批量导入 JSON</span>
          <span><el-icon><Link /></el-icon>ISBN 聚合多件闲置</span>
          <span><el-icon><Warning /></el-icon>版本避雷不让买错</span>
        </div>
      </div>
      <div class="hero-actions">
        <button class="primary-btn" @click="openCourseImport">
          <el-icon><Plus /></el-icon>{{ hasSemesters ? '换个学期' : '导入课表' }}
        </button>
        <button class="ghost-btn" :disabled="!currentSemester" @click="runMatch">
          <el-icon><Search /></el-icon>一键匹配这学期教材
        </button>
        <select v-model="currentSemester" class="semester-select">
          <option value="">选择已导入的学期…</option>
          <option v-for="s in semesters" :key="s" :value="s">{{ s }}</option>
        </select>
      </div>
    </section>

    <!-- 匹配结果 -->
    <section class="panel match-panel" v-if="matchList.length">
      <div class="panel-head">
        <span class="panel-title">📚 这学期要用的 {{ matchList.length }} 门课</span>
        <span class="panel-sub" v-if="missingTotal > 0">还有 {{ missingTotal }} 门课没匹配到，按学科/关键词手动找或去发布新教材</span>
        <span class="panel-sub" v-else>所有匹配的教材平台上都有二手或可租版本啦 🎉</span>
      </div>
      <div class="match-grid">
        <article v-for="row in matchList" :key="row.courseName" class="match-card">
          <header>
            <h3>{{ row.courseName }}</h3>
            <span class="status-chip" :class="row.matched ? 'has' : 'miss'">
              {{ row.matched ? '匹配到了' : '未匹配' }}
            </span>
          </header>
          <div class="match-row" v-if="row.books.length">
            <div v-for="b in row.books" :key="b.textbookId" class="book-pill" :class="b.statusLabel === '缺' ? 'miss' : 'ok'" @click="openTextbook(b.textbookId)">
              <span class="pill-title">{{ b.title }} <em v-if="b.edition">{{ b.edition }}</em></span>
              <span class="pill-meta">
                <span v-if="b.teacherFlag" class="teacher-badge">老师指定</span>
                <span v-if="b.warnCount > 0" class="warn-badge">⚠ {{ b.warnCount }}</span>
                <span v-if="b.goodsCount > 0">二手 {{ b.goodsCount }}</span>
                <span v-if="b.rentalCount > 0">可租 {{ b.rentalCount }}</span>
                <span class="pill-state">{{ b.statusLabel === '有' ? '已有' : '缺' }}</span>
              </span>
            </div>
          </div>
          <div class="match-row empty" v-else>
            <span>暂时没匹配到，试试 <a @click="$router.push('/market?keyword=' + encodeURIComponent(row.courseName))">按课程名搜索闲置</a></span>
          </div>
        </article>
      </div>
    </section>

    <!-- 教材列表（按学科筛选 + 关键词） -->
    <section class="panel">
      <div class="panel-head">
        <span class="panel-title">📚 教材主表</span>
        <span class="panel-sub">共 {{ textbookTotal }} 本在售教材，老师指定版本置顶</span>
      </div>
      <div class="filter-row">
        <el-select v-model="textbookQuery.subject" clearable placeholder="全部学科" style="width: 140px" @change="reloadTextbooks">
          <el-option v-for="s in subjects" :key="s.label" :label="s.label + ' (' + s.count + ')'" :value="s.label" />
        </el-select>
        <el-select v-model="textbookQuery.teacherFlag" clearable placeholder="全部" style="width: 140px" @change="reloadTextbooks">
          <el-option label="老师指定版本" :value="1" />
          <el-option label="非指定版本" :value="0" />
        </el-select>
        <el-input
          v-model="textbookQuery.keyword"
          clearable
          placeholder="书名 / ISBN / 作者"
          style="max-width: 250px"
          @keyup.enter="reloadTextbooks"
          @clear="reloadTextbooks"
        >
          <template #append>
            <el-button @click="reloadTextbooks">搜索</el-button>
          </template>
        </el-input>
        <button class="ghost-btn small" @click="openPublishTextbook">
          <el-icon><Plus /></el-icon>新建教材条目
        </button>
      </div>
      <div v-loading="textbookLoading">
        <div v-if="textbooks.length" class="textbook-grid">
          <article v-for="tb in textbooks" :key="tb.id" class="tb-card" @click="openTextbook(tb.id)">
            <div class="tb-cover-wrap">
              <img v-if="tb.coverImage" :src="tb.coverImage" class="tb-cover" alt="" />
              <div v-else class="tb-cover placeholder"><el-icon><Reading /></el-icon></div>
              <span v-if="tb.teacherFlag" class="tb-badge teacher">老师指定</span>
              <span v-if="tb.warnCount > 0" class="tb-badge warn">⚠ {{ tb.warnCount }}</span>
            </div>
            <h4>{{ tb.title }} <em v-if="tb.edition">{{ tb.edition }}</em></h4>
            <p class="tb-author">{{ tb.author || '未知作者' }} · {{ tb.publisher || '—' }}</p>
            <div class="tb-stat">
              <span v-if="tb.goodsCount > 0">二手 {{ tb.goodsCount }}</span>
              <span v-if="tb.rentalCount > 0">可租 {{ tb.rentalCount }}</span>
              <span v-if="tb.goodsCount + tb.rentalCount === 0" class="empty">暂无二手 / 可租</span>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无匹配的教材" :image-size="90" />
      </div>
    </section>

    <!-- 旧功能：价格指数 & 学科（折叠保留） -->
    <section class="panel">
      <div class="panel-head">
        <span class="panel-title">📊 教材价格指数</span>
        <el-link type="primary" :underline="false" @click="showIndex = !showIndex">{{ showIndex ? '收起' : '展开旧价格指数' }}</el-link>
      </div>
      <template v-if="showIndex">
        <div class="index-body" v-if="index.hasData">
          <div class="index-main">
            <div class="index-price-row">
              <div><div class="index-label">近 30 天成交均价</div><div class="index-value">¥{{ index.dealAvg30d || 0 }}</div></div>
              <span class="index-change" :class="trendClass">
                <template v-if="index.trend === 'up'">↑ 上涨 {{ Math.abs(index.changePct).toFixed(1) }}%</template>
                <template v-else-if="index.trend === 'down'">↓ 下跌 {{ Math.abs(index.changePct).toFixed(1) }}%</template>
                <template v-else>— 基本持平</template>
              </span>
            </div>
            <div class="index-sub-row">
              <span>在售均价 ¥{{ index.onSaleAvgNow || 0 }}</span>
              <span>在售 {{ index.onSaleCountNow || 0 }} 本</span>
              <span>30 天成交 {{ index.deal30d || 0 }} 本</span>
            </div>
          </div>
        </div>
        <div class="rule-grid">
          <div class="rule-card"><span class="rule-num">1</span><div><div class="rule-title">毕业季扫货</div><div class="rule-desc">5–7 月学长学姐清仓，教材价格全年最低</div></div></div>
          <div class="rule-card"><span class="rule-num">2</span><div><div class="rule-title">开学前找书</div><div class="rule-desc">按学科筛选，同专业学长出的书版本最匹配</div></div></div>
          <div class="rule-card"><span class="rule-num">3</span><div><div class="rule-title">用完再传下去</div><div class="rule-desc">考完试挂回平台，一本书在校园里循环 4 年</div></div></div>
        </div>
      </template>
    </section>

    <!-- 导入课表对话框 -->
    <el-dialog v-model="importDialog" title="导入学期课表" width="580px" :close-on-click-modal="false">
      <div class="form-row">
        <label>学期（如 2026-2027-1）</label>
        <el-input v-model="importForm.semester" placeholder="格式：学年-学年-学期号" />
        <span class="row-tip">已有学期会跳过重复课程</span>
      </div>
      <div class="form-row">
        <label>课程列表（每行一条）</label>
        <el-input
          v-model="importForm.raw"
          type="textarea"
          :rows="8"
          placeholder="格式一（推荐）：每行 课程名,老师&#10;高等数学,李老师&#10;大学英语,王老师&#10;机械制图"
        />
      </div>
      <template #footer>
        <el-button @click="importDialog = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="doImport">导入</el-button>
      </template>
    </el-dialog>

    <!-- 教材详情对话框 -->
    <el-dialog v-model="textbookDetail" :title="detailTb ? (detailTb.title + (detailTb.edition ? ' · ' + detailTb.edition : '')) : '教材详情'" width="680px">
      <div v-if="detailTb" class="tb-detail">
        <div style="display:flex;justify-content:flex-end;margin-bottom:10px">
          <el-button
            size="small"
            :type="isWished(detailTb.id) ? 'success' : 'primary'"
            plain
            :loading="wishLoading"
            @click="toggleWish(detailTb)"
          >
            {{ isWished(detailTb.id) ? '已关注 · 上新提醒我' : '想要这本书，上新提醒我' }}
          </el-button>
        </div>
        <div class="tb-detail-head">
          <div class="tb-detail-tags">
            <span v-if="detailTb.teacherFlag" class="tag teacher">老师指定版本</span>
            <span class="tag subject">{{ detailTb.subject || '未分类' }}</span>
            <span v-if="detailTb.warnCount > 0" class="tag warn">⚠ 已有 {{ detailTb.warnCount }} 条避雷</span>
          </div>
          <p class="tb-publisher">{{ detailTb.author || '未知作者' }} · {{ detailTb.publisher || '—' }}</p>
          <p class="tb-isbn" v-if="detailTb.isbn">ISBN {{ detailTb.isbn }}</p>
          <p class="tb-note" v-if="detailTb.teacherNote">📝 老师备注：{{ detailTb.teacherNote }}</p>
        </div>

        <!-- 在售闲置 & 可租 -->
        <div class="tb-stat-grid">
          <div class="stat-box goods">
            <h5>二手闲置</h5>
            <div class="stat-num">{{ detailTb.goodsCount || 0 }}</div>
            <p v-if="detailTb.goodsMinPrice != null">最低 ¥{{ money(detailTb.goodsMinPrice) }}</p>
            <el-button size="small" type="primary" link :disabled="!detailTb.goodsCount" @click="$router.push('/market?keyword=' + encodeURIComponent(detailTb.title + ' ' + (detailTb.edition || '')))">查看全部</el-button>
          </div>
          <div class="stat-box rental">
            <h5>可租版本</h5>
            <div class="stat-num">{{ detailTb.rentalCount || 0 }}</div>
            <p v-if="detailTb.rentalMinPrice != null">起始 ¥{{ money(detailTb.rentalMinPrice) }}/周</p>
            <el-button size="small" type="primary" link :disabled="!detailTb.rentalCount" @click="$router.push('/rental?type=textbook&keyword=' + encodeURIComponent(detailTb.title))">查看全部</el-button>
          </div>
        </div>

        <!-- 避雷留言 -->
        <div class="tb-warn-section">
          <div class="section-head">
            <h5>版本避雷（{{ warningList.length }}）</h5>
            <button class="ghost-btn small" @click="openAddWarning">
              <el-icon><EditPen /></el-icon>我也来踩个坑
            </button>
          </div>
          <div v-if="warningList.length" class="warn-list">
            <div v-for="w in warningList" :key="w.id" class="warn-item">
              <span class="reason-tag" :class="w.reason">{{ reasonLabel(w.reason) }}</span>
              <span class="warn-detail">{{ w.detail || '—' }}</span>
              <span class="warn-user">— {{ w.userName || '同学' }}</span>
            </div>
          </div>
          <div v-else class="warn-empty">还没有避雷留言，买之前先看看同学有没有提醒过 ✨</div>
        </div>

        <!-- 笔记附件（关联该 textbook 的 free/bonus 资料） -->
        <div class="tb-material-section">
          <div class="section-head">
            <h5>笔记 / 课件 / 试卷（{{ materialList.length }}）</h5>
            <button class="ghost-btn small" @click="openAddMaterial">
              <el-icon><Upload /></el-icon>我要上传
            </button>
          </div>
          <div v-if="materialList.length" class="material-list">
            <div v-for="m in materialList" :key="m.id" class="material-item">
              <span class="material-icon">{{ fileIcon(m.fileType) }}</span>
              <span class="material-title">{{ m.title }}</span>
              <span class="material-tag" :class="m.deliveryMode">{{ deliveryLabel(m.deliveryMode) }}</span>
              <span class="material-price" v-if="m.deliveryMode === 'paid'">¥{{ money(m.price) }}</span>
              <el-button link size="small" type="primary" @click="viewMaterial(m)">查看</el-button>
            </div>
          </div>
          <div v-else class="warn-empty">还没有人上传相关资料，学霸的话可以上传给学弟学妹 📚</div>
        </div>
      </div>
    </el-dialog>

    <!-- 发布新教材条目 / 避雷 / 附件 弹窗 -->
    <el-dialog v-model="publishDialog" title="新建教材条目" width="520px" :close-on-click-modal="false">
      <div class="dialog-form">
        <div class="form-row"><label>书名 *</label><el-input v-model="publishForm.title" maxlength="120" /></div>
        <div class="form-two">
          <div class="form-row"><label>版本</label><el-input v-model="publishForm.edition" placeholder="第七版 / 2023版" /></div>
          <div class="form-row"><label>ISBN</label><el-input v-model="publishForm.isbn" placeholder="可空，如 9787123456789" /></div>
        </div>
        <div class="form-two">
          <div class="form-row"><label>作者</label><el-input v-model="publishForm.author" /></div>
          <div class="form-row"><label>出版社</label><el-input v-model="publishForm.publisher" /></div>
        </div>
        <div class="form-two">
          <div class="form-row"><label>学科</label>
            <el-input v-model="publishForm.subject" placeholder="数学 / 计算机 / 经济" />
          </div>
          <div class="form-row"><label>适用年级</label>
            <el-input v-model="publishForm.grade" placeholder="大二下 / 大三" />
          </div>
        </div>
        <div class="form-row">
          <el-checkbox v-model="publishForm.teacherFlag">标记为某老师指定版本</el-checkbox>
          <el-input v-if="publishForm.teacherFlag" v-model="publishForm.teacherNote" placeholder="老师备注：李老师只认第七版" style="margin-top:6px" />
        </div>
      </div>
      <template #footer>
        <el-button @click="publishDialog = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="doPublishTextbook">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="warnDialog" title="发布避雷留言" width="480px" :close-on-click-modal="false">
      <div class="form-row">
        <label>避雷原因 *</label>
        <el-select v-model="warnForm.reason" style="width:100%">
          <el-option v-for="(label, k) in reasonMap" :key="k" :label="label" :value="k" />
        </el-select>
      </div>
      <div class="form-row">
        <label>补充说明（可选）</label>
        <el-input v-model="warnForm.detail" type="textarea" :rows="3" maxlength="400" show-word-limit />
      </div>
      <template #footer>
        <el-button @click="warnDialog = false">取消</el-button>
        <el-button type="primary" :loading="warningSubmitting" @click="doAddWarning">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="materialDialog" title="上传资料（PDF / PPT / 试卷）" width="520px" :close-on-click-modal="false">
      <div class="form-row"><label>资料标题 *</label><el-input v-model="materialForm.title" maxlength="100" /></div>
      <div class="form-row"><label>说明</label><el-input v-model="materialForm.description" type="textarea" :rows="2" maxlength="300" /></div>
      <div class="form-two">
        <div class="form-row"><label>类型 *</label>
          <el-select v-model="materialForm.fileType" style="width:100%">
            <el-option label="PDF" value="pdf" />
            <el-option label="PPT / 课件" value="ppt" />
            <el-option label="Word / 试卷" value="doc" />
            <el-option label="压缩包" value="zip" />
          </el-select>
        </div>
        <div class="form-row"><label>链接（请用上传接口先获得 fileUrl）*</label>
          <el-input v-model="materialForm.fileUrl" placeholder="https://..." />
        </div>
      </div>
      <div class="form-two">
        <div class="form-row"><label>交付方式 *</label>
          <el-select v-model="materialForm.deliveryMode" style="width:100%">
            <el-option label="免费公开" value="free" />
            <el-option label="随书附赠" value="bonus" />
            <el-option label="单独售卖" value="paid" />
          </el-select>
        </div>
        <div class="form-row"><label>价格（仅单独售卖）</label>
          <el-input-number v-if="materialForm.deliveryMode === 'paid'" v-model="materialForm.price" :min="0.01" :max="999" :precision="2" style="width:100%" />
        </div>
      </div>
      <template #footer>
        <el-button @click="materialDialog = false">取消</el-button>
        <el-button type="primary" :loading="materialSubmitting" @click="doAddMaterial">上传</el-button>
      </template>
    </el-dialog>
  </ServiceShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { AlarmClock, Calendar, CircleCheck, Document, EditPen, Link, Loading, Location, Plus, Reading, Search, Upload, User, Warning } from '@element-plus/icons-vue'
import ServiceShell from '../components/ServiceShell.vue'
import { getTextbookIndex, getTextbookSubjects, getGoodsList } from '../api/goods'
import {
  addMaterial as apiAddMaterial,
  addTextbookWarning,
  createTextbook as apiCreateTextbook,
  downloadMaterial,
  getMaterial as apiGetMaterial,
  getMaterialsByOwner,
  getSemesters as apiGetSemesters,
  getTextbook as apiGetTextbook,
  getTextbooks as apiGetTextbooks,
  getTextbookSubjects as apiGetTextbookSubjects,
  getTextbookWarnings as apiGetTextbookWarnings,
  importCourses as apiImportCourses,
  matchTextbooks as apiMatchTextbooks
} from '../api/textbook'
import { addTextbookWish, deleteTextbookWish, getTextbookWishes } from '../api/subscribe'

const router = useRouter()
const route = useRoute()

/* ==================== 登录判断 ==================== */
const isLoggedIn = computed(() => !!(localStorage.getItem('token') && localStorage.getItem('user')))
const goLogin = () => router.push({ path: '/login', query: { redirect: '/textbook' } })
const money = v => { const n = Number(v || 0); return Number.isInteger(n) ? n : n.toFixed(2) }

/* ==================== 学期 & 课表 ==================== */
const semesters = ref([])
const currentSemester = ref('')
const matchList = ref([])
const matchLoading = ref(false)
const missingTotal = computed(() => matchList.value.filter(r => r.missing).length)

const loadSemesters = async () => {
  if (!isLoggedIn.value) return
  try {
    semesters.value = await apiGetSemesters() || []
    if (semesters.value.length && !currentSemester.value) {
      currentSemester.value = semesters.value[0]
    }
  } catch (e) { semesters.value = [] }
}

const openCourseImport = () => {
  if (!isLoggedIn.value) { goLogin(); return }
  importForm.semester = currentSemester.value || new Date().getFullYear() + '-' + (new Date().getFullYear() + 1) + '-' + ((new Date().getMonth() + 1) > 6 ? 1 : 2)
  importForm.raw = ''
  importDialog.value = true
}

const importDialog = ref(false)
const importing = ref(false)
const importForm = reactive({ semester: '', raw: '' })

const doImport = async () => {
  if (!importForm.semester.trim()) { ElMessage.warning('请填写学期'); return }
  if (!importForm.raw.trim()) { ElMessage.warning('请粘贴课表'); return }
  importing.value = true
  try {
    const items = importForm.raw.split('\n').map(l => {
      const [courseName, teacher, courseCode] = l.split(',').map(s => (s || '').trim())
      return { courseName, teacher, courseCode }
    }).filter(it => it.courseName)
    const count = await apiImportCourses(importForm.semester.trim(), items, true)
    ElMessage.success(`已导入 ${count} 条课程`)
    importDialog.value = false
    await loadSemesters()
    currentSemester.value = importForm.semester.trim()
    await runMatch()
  } catch (e) { /* 拦截器已提示 */ }
  finally { importing.value = false }
}

const runMatch = async () => {
  if (!currentSemester.value) { ElMessage.warning('请先选择学期'); return }
  if (!isLoggedIn.value) { goLogin(); return }
  matchLoading.value = true
  try {
    matchList.value = await apiMatchTextbooks(currentSemester.value) || []
    if (!matchList.value.length) ElMessage.info('该学期还没导入课表')
  } catch (e) { matchList.value = [] }
  finally { matchLoading.value = false }
}

/* ==================== 教材列表 ==================== */
const subjects = ref([])
const textbookQuery = reactive({ subject: '', teacherFlag: null, keyword: '', pageNum: 1, pageSize: 24 })
const textbooks = ref([])
const textbookTotal = ref(0)
const textbookLoading = ref(false)

const loadSubjects = async () => {
  try {
    subjects.value = await apiGetTextbookSubjects() || []
  } catch (e) { subjects.value = [] }
}

const loadTextbooks = async () => {
  textbookLoading.value = true
  try {
    const data = await apiGetTextbooks(textbookQuery)
    textbooks.value = data?.list || []
    textbookTotal.value = Number(data?.total) || 0
  } catch (e) { textbooks.value = [] }
  finally { textbookLoading.value = false }
}

const reloadTextbooks = () => { textbookQuery.pageNum = 1; loadTextbooks() }

/* ==================== 教材详情弹窗 ==================== */
const textbookDetail = ref(false)
const detailTb = ref(null)
const warningList = ref([])
const materialList = ref([])

const openTextbook = async id => {
  textbookDetail.value = true
  warningList.value = []
  materialList.value = []
  try {
    detailTb.value = await apiGetTextbook(id)
    try { warningList.value = await apiGetTextbookWarnings(id) } catch (e) {}
    try { materialList.value = await getMaterialsByOwner('textbook', id) || [] } catch (e) {}
  } catch (e) { detailTb.value = null }
}

/* ==================== 教材心愿（上新提醒） ==================== */
const myWishes = ref([])
const wishLoading = ref(false)
const isWished = id => myWishes.value.some(w => w.textbookId === id)

const loadMyWishes = async () => {
  if (!isLoggedIn.value) { myWishes.value = []; return }
  try {
    myWishes.value = (await getTextbookWishes()) || []
  } catch (e) {
    myWishes.value = []
  }
}

const toggleWish = async tb => {
  if (!isLoggedIn.value) { goLogin(); return }
  if (!tb || !tb.id) return
  wishLoading.value = true
  try {
    if (isWished(tb.id)) {
      const w = myWishes.value.find(x => x.textbookId === tb.id)
      if (w) {
        await deleteTextbookWish(w.id)
        myWishes.value = myWishes.value.filter(x => x.id !== w.id)
        ElMessage.success('已取消关注')
      }
    } else {
      const msg = await addTextbookWish(tb.id)
      ElMessage.success(msg || '已关注')
      await loadMyWishes()
    }
  } catch (e) {
    // 请求拦截器已提示
  } finally {
    wishLoading.value = false
  }
}

const reasonMap = {
  why_not_allowed: '老师不让用',
  errors: '错误太多',
  too_difficult: '比老师要求难太多',
  outdated: '版本过旧，内容过时',
  other: '其他'
}
const reasonLabel = r => reasonMap[r] || r

const deliveryLabel = m => ({ free: '免费', bonus: '随书附赠', paid: '单独售卖' })[m] || m
const fileIcon = t => ({ pdf: '📕', ppt: '📊', doc: '📝', zip: '🗂️', image: '🖼️' })[t] || '📄'

/* ==================== 新建教材 / 避雷 / 附件 ==================== */
const publishDialog = ref(false)
const publishing = ref(false)
const publishForm = reactive({ title: '', edition: '', isbn: '', author: '', publisher: '', subject: '', grade: '', teacherFlag: false, teacherNote: '' })

const openPublishTextbook = () => {
  if (!isLoggedIn.value) { goLogin(); return }
  Object.assign(publishForm, { title: '', edition: '', isbn: '', author: '', publisher: '', subject: '', grade: '', teacherFlag: false, teacherNote: '' })
  publishDialog.value = true
}

const doPublishTextbook = async () => {
  if (!publishForm.title.trim()) { ElMessage.warning('请填写书名'); return }
  publishing.value = true
  try {
    await apiCreateTextbook({
      title: publishForm.title.trim(),
      subtitle: '',
      isbn: publishForm.isbn.trim() || '',
      author: publishForm.author.trim(),
      publisher: publishForm.publisher.trim(),
      edition: publishForm.edition.trim(),
      subject: publishForm.subject.trim(),
      grade: publishForm.grade.trim(),
      teacherFlag: publishForm.teacherFlag ? 1 : 0,
      teacherNote: publishForm.teacherNote.trim()
    })
    ElMessage.success('新教材条目已保存，刷新后可见')
    publishDialog.value = false
    await loadTextbooks()
  } catch (e) { /* 拦截器已提示 */ }
  finally { publishing.value = false }
}

const warnDialog = ref(false)
const warningSubmitting = ref(false)
const warnForm = reactive({ reason: '', detail: '' })

const openAddWarning = () => {
  if (!isLoggedIn.value) { goLogin(); return }
  warnForm.reason = ''
  warnForm.detail = ''
  warnDialog.value = true
}

const doAddWarning = async () => {
  if (!warnForm.reason) { ElMessage.warning('请选择避雷原因'); return }
  warningSubmitting.value = true
  try {
    await addTextbookWarning(detailTb.value.id, warnForm.reason, warnForm.detail.trim())
    ElMessage.success('避雷留言已发布，感谢你的提醒 🙏')
    warnDialog.value = false
    // 刷新：计数 + 列表
    try { warningList.value = await apiGetTextbookWarnings(detailTb.value.id) } catch (e) {}
    try { detailTb.value = await apiGetTextbook(detailTb.value.id) } catch (e) {}
    try { await loadTextbooks() } catch (e) {}
  } catch (e) { /* 拦截器已提示 */ }
  finally { warningSubmitting.value = false }
}

const materialDialog = ref(false)
const materialSubmitting = ref(false)
const materialForm = reactive({ title: '', description: '', fileType: 'pdf', fileUrl: '', deliveryMode: 'free', price: null })

const openAddMaterial = () => {
  if (!isLoggedIn.value) { goLogin(); return }
  Object.assign(materialForm, { title: '', description: '', fileType: 'pdf', fileUrl: '', deliveryMode: 'free', price: null })
  materialDialog.value = true
}

const doAddMaterial = async () => {
  if (!materialForm.title.trim()) { ElMessage.warning('请填写资料标题'); return }
  if (!materialForm.fileUrl.trim()) { ElMessage.warning('请先上传文件并填写 fileUrl'); return }
  materialSubmitting.value = true
  try {
    await apiAddMaterial({
      ownerType: 'textbook',
      ownerId: detailTb.value.id,
      title: materialForm.title.trim(),
      description: materialForm.description.trim(),
      fileType: materialForm.fileType,
      fileUrl: materialForm.fileUrl.trim(),
      deliveryMode: materialForm.deliveryMode,
      price: materialForm.price == null ? 0 : materialForm.price
    })
    ElMessage.success('资料已上传 🎉')
    materialDialog.value = false
    materialList.value = await getMaterialsByOwner('textbook', detailTb.value.id) || []
  } catch (e) { /* 拦截器已提示 */ }
  finally { materialSubmitting.value = false }
}

const viewMaterial = async m => {
  try {
    const info = await apiGetMaterial(m.id)
    window.open(info.fileUrl, '_blank')
    await downloadMaterial(m.id)
  } catch (e) { /* 拦截器已提示 */ }
}

/* ==================== 旧价格指数（折叠保留） ==================== */
const showIndex = ref(false)
const index = ref({})
const trendClass = computed(() => {
  if (index.value.trend === 'up') return 'up'
  if (index.value.trend === 'down') return 'down'
  return 'stable'
})
const loadOld = async () => {
  try { index.value = (await getTextbookIndex()) || {} } catch (e) { index.value = {} }
}

/* ==================== 启动 ==================== */
onMounted(async () => {
  await loadSubjects()
  await loadTextbooks()
  await loadSemesters()
  await loadOld()
  await loadMyWishes()
  if (semesters.value.length) {
    currentSemester.value = semesters.value[0]
    await runMatch()
  }
  // 首页引导跳转 ?action=import → 自动打开课表导入弹窗
  if (route.query.action === 'import') {
    if (!isLoggedIn.value) {
      ElMessage.info('请先登录再导入课表')
      goLogin()
    } else {
      importDialog.value = true
    }
  }
})
</script>

<style scoped>
.hero-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 22px 26px;
  margin-bottom: 14px;
  border-radius: 12px;
  background: linear-gradient(135deg, #EFF6FF, #F0FDF4 55%, #FFF7ED);
  border: 1px solid #BFDBFE;
}
.hero-eyebrow { font-size: 12px; font-weight: 700; color: #1D4ED8; letter-spacing: 0.5px; }
.hero-copy h1 { margin: 6px 0; font-size: 22px; color: #1F2937; }
.hero-copy p { margin: 0; max-width: 720px; font-size: 13px; line-height: 1.8; color: #6B7280; }
.hero-tags { display: flex; flex-wrap: wrap; gap: 14px; margin-top: 12px; font-size: 12px; color: #1D4ED8; }
.hero-tags span { display: inline-flex; align-items: center; gap: 5px; }
.hero-actions { display: flex; flex-direction: column; gap: 8px; flex-shrink: 0; }
.semester-select { padding: 6px 10px; border: 1px solid #E5E7EB; border-radius: 8px; font-size: 13px; background: #fff; }
.primary-btn, .ghost-btn {
  height: 38px; padding: 0 20px; border-radius: 19px; font-size: 13.5px; font-weight: 700;
  cursor: pointer; display: inline-flex; align-items: center; justify-content: center; gap: 6px;
}
.primary-btn { border: none; color: #fff; background: linear-gradient(135deg, #3B82F6, #1D4ED8); box-shadow: 0 6px 16px rgba(29, 78, 216, 0.2); }
.primary-btn:disabled { opacity: 0.55; cursor: not-allowed; }
.ghost-btn { border: 1px solid #E5E7EB; background: #fff; color: #4B5563; }
.ghost-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.ghost-btn.small { height: 34px; padding: 0 14px; font-size: 12.5px; }

.panel { padding: 18px 22px; margin-bottom: 14px; border-radius: 12px; background: #fff; border: 1px solid #EFF0F2; }
.panel-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 14px; flex-wrap: wrap; }
.panel-title { font-size: 15px; font-weight: 700; color: #1F2937; }
.panel-sub { font-size: 11.5px; color: #9CA3AF; }

.filter-row { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 14px; }

.match-panel { background: #FFF7ED; border-color: #FDE68A; }
.match-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 12px; }
.match-card {
  padding: 12px 14px; border-radius: 10px; background: #fff; border: 1px solid #FED7AA;
}
.match-card header { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 8px; }
.match-card h3 { margin: 0; font-size: 14px; color: #1F2937; font-weight: 700; }
.status-chip { height: 22px; padding: 0 10px; border-radius: 11px; font-size: 11px; font-weight: 700; line-height: 22px; }
.status-chip.has { background: #DCFCE7; color: #166534; }
.status-chip.miss { background: #FEE2E2; color: #991B1B; }
.match-row { display: flex; flex-direction: column; gap: 6px; }
.match-row.empty { font-size: 12px; color: #9CA3AF; }
.match-row.empty a { color: #1D4ED8; text-decoration: underline; cursor: pointer; }
.book-pill {
  display: flex; flex-direction: column; gap: 3px;
  padding: 8px 10px; border-radius: 8px; border: 1px solid #E5E7EB; background: #FAFBFC;
  cursor: pointer; transition: all 0.15s;
}
.book-pill:hover { border-color: #3B82F6; background: #EFF6FF; }
.book-pill.miss { border-color: #FECACA; background: #FEF2F2; }
.book-pill.ok { border-color: #BBF7D0; background: #F0FDF4; }
.pill-title { font-size: 13px; font-weight: 600; color: #1F2937; }
.pill-title em { font-style: normal; color: #1D4ED8; font-size: 12px; }
.pill-meta { display: flex; flex-wrap: wrap; gap: 8px; font-size: 11px; color: #6B7280; align-items: center; }
.teacher-badge { background: #FEF3C7; color: #92400E; padding: 1px 6px; border-radius: 4px; font-weight: 600; }
.warn-badge { color: #B91C1C; font-weight: 600; }
.pill-state { margin-left: auto; font-weight: 700; font-size: 12px; }

.textbook-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px; }
.tb-card { padding: 10px; border-radius: 10px; border: 1px solid #EFF0F2; background: #FAFBFC; cursor: pointer; transition: all 0.15s; }
.tb-card:hover { border-color: #BFDBFE; background: #EFF6FF; transform: translateY(-2px); }
.tb-cover-wrap { position: relative; height: 110px; border-radius: 8px; overflow: hidden; background: #F3F4F6; margin-bottom: 8px; display: flex; align-items: center; justify-content: center; }
.tb-cover { width: 100%; height: 100%; object-fit: cover; }
.tb-cover.placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; color: #9CA3AF; font-size: 36px; }
.tb-badge { position: absolute; top: 6px; left: 6px; height: 22px; padding: 0 8px; border-radius: 11px; font-size: 11px; font-weight: 700; line-height: 22px; }
.tb-badge.teacher { background: #FEE2E2; color: #991B1B; }
.tb-badge.warn { right: 6px; left: auto; background: #FEF3C7; color: #92400E; }
.tb-card h4 { margin: 0; font-size: 13px; color: #1F2937; font-weight: 700; line-height: 1.4; }
.tb-card h4 em { font-style: normal; font-size: 11px; color: #1D4ED8; font-weight: 500; }
.tb-author { margin: 4px 0; font-size: 11.5px; color: #9CA3AF; }
.tb-stat { font-size: 11.5px; color: #6B7280; display: flex; gap: 8px; flex-wrap: wrap; }
.tb-stat .empty { color: #D1D5DB; }

.tag { display: inline-block; height: 20px; padding: 0 7px; border-radius: 5px; font-size: 11px; background: #F3F4F6; color: #6B7280; line-height: 20px; }
.tag.teacher { background: #FEE2E2; color: #991B1B; }
.tag.subject { background: #DBEAFE; color: #1E40AF; }
.tag.warn { background: #FEF3C7; color: #92400E; }

/* 详情 */
.tb-detail-head { margin-bottom: 14px; }
.tb-detail-head .tb-detail-tags { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 8px; }
.tb-publisher { margin: 0; font-size: 13px; color: #4B5563; }
.tb-isbn { margin: 4px 0 0; font-size: 11.5px; color: #9CA3AF; font-family: monospace; }
.tb-note { margin: 6px 0 0; font-size: 12.5px; color: #B45309; background: #FEF3C7; padding: 6px 10px; border-radius: 6px; }

.tb-stat-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 16px; }
.stat-box { padding: 12px; border-radius: 10px; background: #FAFBFC; border: 1px solid #EFF0F2; text-align: center; }
.stat-box h5 { margin: 0 0 6px; font-size: 12px; color: #6B7280; font-weight: 600; }
.stat-num { font-size: 24px; font-weight: 800; color: #1D4ED8; }
.stat-box.goods .stat-num { color: #E1251B; }
.stat-box.rental .stat-num { color: #059669; }

.section-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.section-head h5 { margin: 0; font-size: 13px; font-weight: 700; color: #1F2937; }
.tb-warn-section, .tb-material-section { margin-top: 16px; padding-top: 14px; border-top: 1px dashed #E5E7EB; }
.warn-list { display: flex; flex-direction: column; gap: 8px; }
.warn-item { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; padding: 8px 10px; border-radius: 8px; background: #FEF2F2; font-size: 12.5px; }
.reason-tag { height: 22px; padding: 0 8px; border-radius: 11px; font-size: 11px; font-weight: 700; line-height: 22px; }
.reason-tag.why_not_allowed { background: #DC2626; color: #fff; }
.reason-tag.errors { background: #F59E0B; color: #fff; }
.reason-tag.too_difficult { background: #8B5CF6; color: #fff; }
.reason-tag.outdated { background: #6B7280; color: #fff; }
.reason-tag.other { background: #9CA3AF; color: #fff; }
.warn-detail { flex: 1; min-width: 120px; color: #1F2937; }
.warn-user { color: #9CA3AF; }
.warn-empty { padding: 10px; border-radius: 8px; background: #F3F4F6; text-align: center; font-size: 12px; color: #9CA3AF; }

.material-list { display: flex; flex-direction: column; gap: 8px; }
.material-item { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; padding: 8px 10px; border-radius: 8px; background: #F0FDF4; font-size: 12.5px; }
.material-icon { font-size: 16px; }
.material-title { flex: 1; min-width: 120px; color: #1F2937; font-weight: 600; }
.material-tag { height: 22px; padding: 0 8px; border-radius: 11px; font-size: 11px; font-weight: 700; line-height: 22px; }
.material-tag.free { background: #DCFCE7; color: #166534; }
.material-tag.bonus { background: #DBEAFE; color: #1E40AF; }
.material-tag.paid { background: #FEF3C7; color: #92400E; }
.material-price { color: #E1251B; font-weight: 700; }

/* 旧指数 */
.index-body { display: flex; gap: 18px; flex-wrap: wrap; }
.index-main { flex: 1; min-width: 280px; }
.index-label { font-size: 11px; color: #6B7280; }
.index-value { font-size: 26px; font-weight: 800; color: #1E40AF; line-height: 1.2; }
.index-change { font-size: 13px; font-weight: 700; padding: 4px 10px; border-radius: 8px; }
.index-change.up { background: #FEF2F2; color: #DC2626; }
.index-change.down { background: #ECFDF5; color: #059669; }
.index-change.stable { background: #F3F4F6; color: #6B7280; }
.index-sub-row { display: flex; gap: 10px; flex-wrap: wrap; margin-top: 10px; }
.index-sub-row span { font-size: 12px; color: #4B5563; background: #F3F4F6; padding: 3px 10px; border-radius: 5px; }
.index-price-row { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; }
.rule-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-top: 12px; }
.rule-card { display: flex; gap: 10px; align-items: flex-start; background: #FAFBFC; border-radius: 10px; padding: 14px; }
.rule-num { width: 24px; height: 24px; flex-shrink: 0; border-radius: 50%; background: #3B82F6; color: #fff; font-size: 12px; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.rule-title { font-size: 13px; font-weight: 700; color: #1F2937; }
.rule-desc { margin-top: 4px; font-size: 12px; color: #6B7280; line-height: 1.5; }

/* 表单 */
.dialog-form .form-row { margin-bottom: 14px; }
.form-row label { display: block; margin-bottom: 6px; font-size: 12.5px; font-weight: 600; color: #4B5563; }
.form-two { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.row-tip { margin-top: 5px; font-size: 11.5px; color: #9CA3AF; }

@media (max-width: 860px) {
  .hero-panel { flex-direction: column; align-items: flex-start; }
  .rule-grid { grid-template-columns: 1fr; }
  .tb-stat-grid { grid-template-columns: 1fr; }
  .form-two { grid-template-columns: 1fr; }
}
</style>
