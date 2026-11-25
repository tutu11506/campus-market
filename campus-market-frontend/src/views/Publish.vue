<template>
  <div class="publish-page">
    <!-- 顶部导航（叠在红色渐变上） -->
    <div class="nav-bar">
      <div class="nav-back" @click="$router.back()">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6"></polyline>
        </svg>
      </div>
      <div class="nav-title">发布闲置</div>
      <div class="nav-drafts" @click="goDrafts">草稿箱</div>
    </div>

    <!-- 内容区域 -->
    <div class="content-area">
      <div class="publish-card">
        <h1 class="page-heading">发闲置</h1>

        <!-- 草稿自动保存状态 -->
        <div class="draft-tip" :class="{ saved: !!draftSavedAt }">
          <span v-if="draftSaving">正在保存草稿…</span>
          <span v-else-if="draftSavedAt">草稿已自动保存 {{ draftSavedAt }}</span>
          <span v-else>填写内容后将自动保存草稿，可随时退出</span>
          <a class="draft-link" @click="goDrafts">查看草稿箱</a>
        </div>

        <!-- 基础信息 -->
        <section class="form-section">
          <h2 class="section-title">基础信息</h2>

          <div class="field">
            <label class="field-label">宝贝标题<span class="req">*</span></label>
            <input
              v-model="form.title"
              class="text-input"
              placeholder="请输入商品标题（必填）"
              maxlength="30"
            />
          </div>

          <div class="field">
            <label class="field-label">宝贝图片<span class="req">*</span></label>
            <div class="upload-grid">
              <div
                v-for="(img, index) in imageList"
                :key="index"
                class="upload-item has-image"
              >
                <img :src="img" class="preview-img" />
                <div class="cover-badge" v-if="index === 0">封面</div>
                <div class="delete-btn" @click="removeImage(index)">
                  <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                  </svg>
                </div>
              </div>
              <div
                v-if="imageList.length < 9"
                class="upload-item add-btn"
                @click="triggerUpload"
              >
                <input
                  ref="fileInput"
                  type="file"
                  accept="image/*"
                  multiple
                  style="display: none"
                  @change="handleFileChange"
                />
                <span class="add-plus">+</span>
                <span class="upload-tip">添加图片</span>
              </div>
            </div>
            <div class="field-sub">首张图片将作为封面，最多上传 9 张</div>
          </div>

          <div class="field">
            <label class="field-label">宝贝描述</label>
            <div class="desc-wrap">
              <textarea
                v-model="form.description"
                class="desc-textarea"
                placeholder="描述一下宝贝的品牌型号、成色、入手渠道、转让原因等~"
                rows="5"
                maxlength="500"
              ></textarea>
              <span class="desc-count">{{ form.description.length }}/500</span>
            </div>
          </div>
        </section>

        <!-- 价格 -->
        <section class="form-section">
          <h2 class="section-title">价格</h2>
          <div class="two-col">
            <div class="col">
              <label class="field-label">{{ form.tradeMode === 'exchange' ? '参考价' : '基础价' }}<span class="req">*</span></label>
              <div class="price-box">
                <span class="price-symbol">&yen;</span>
                <input
                  v-model="form.price"
                  class="price-input"
                  type="number"
                  placeholder="0.00"
                  min="0"
                  step="0.01"
                />
              </div>
            </div>
            <div class="col">
              <label class="field-label">原价</label>
              <div class="price-box">
                <span class="price-symbol gray">&yen;</span>
                <input
                  v-model="form.originalPrice"
                  class="price-input origin"
                  type="number"
                  placeholder="0.00"
                  min="0"
                  step="0.01"
                />
              </div>
            </div>
          </div>
          <!-- 涨价返券预览：加价的一半作为优惠券发给买家 -->
          <div class="coupon-preview" v-if="couponPreview && form.tradeMode !== 'exchange'">
            <span class="cp-tag">涨价返券</span>
            <span class="cp-text">平台标价 <b>&yen;{{ couponPreview.list }}</b></span>
            <span class="cp-sep">·</span>
            <span class="cp-coupon">优惠券 &yen;{{ couponPreview.coupon }}</span>
            <span class="cp-sep">·</span>
            <span class="cp-text">买家实付 <b>&yen;{{ couponPreview.pay }}</b></span>
          </div>
          <div class="field-hint">{{ form.tradeMode === 'exchange' ? '换物模式不发生现金支付，参考价仅用于双方衡量商品价值。' : '基础软件服务费：成交额（含运费）× 0.6%' }}</div>
        </section>

        <!-- 发货设置 -->
        <section class="form-section">
          <h2 class="section-title">发货设置</h2>
          <div class="two-col">
            <div class="col">
              <label class="field-label">商品分类<span class="req">*</span></label>
              <div class="select-box">
                <select v-model="form.categoryId" class="select-input">
                  <option value="" disabled>请选择分类</option>
                  <option v-for="c in categoryList" :key="c.id" :value="c.id">
                    {{ c.name }}
                  </option>
                </select>
                <svg class="select-arrow" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#bbb" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
              </div>
            </div>
            <div class="col">
              <label class="field-label">交易地点<span class="req">*</span></label>
              <input
                v-model="form.location"
                class="text-input"
                placeholder="如：南区食堂门口"
              />
            </div>
          </div>
        </section>
        <!-- 交易方式 -->
        <section class="form-section">
          <h2 class="section-title">交易方式</h2>
          <div class="mode-row">
            <div class="mode-card" :class="{ active: form.tradeMode === 'cash' }" @click="selectTradeMode('cash')">
              <b>现金出售</b>
              <span>买家下单付款</span>
            </div>
            <div class="mode-card" :class="{ active: form.tradeMode === 'exchange' }" @click="selectTradeMode('exchange')">
              <b>换物模式</b>
              <span>以物换物，不用现金</span>
            </div>
          </div>
          <div class="check-row">
            <label class="check-item"><input type="checkbox" v-model="form.canMail" /> 可邮寄</label>
            <label class="check-item"><input type="checkbox" v-model="form.canDeliver" /> 可送货到宿舍</label>
            <label class="check-item" :title="form.tradeMode === 'exchange' ? '换物模式默认支持换物' : '允许买家用自己的商品申请交换'">
              <input type="checkbox" v-model="form.exchangeSupported" :disabled="form.tradeMode === 'exchange'" /> 支持换物
            </label>
          </div>
          <div class="exchange-want" v-if="form.tradeMode === 'exchange' || form.exchangeSupported">
            <label class="field-label">希望换到什么（选填）</label>
            <input v-model="form.exchangeWant" class="text-input" maxlength="100" placeholder="如：机械键盘、台灯、高数教材" />
          </div>
          <div class="field-hint" v-if="form.tradeMode === 'exchange'">换物模式默认开启「支持换物」。对方接受申请后双方商品锁定，线下交换验收并由双方确认后才完成。</div>
          <div class="field-hint" v-else-if="form.exchangeSupported">开启后，买家可以现金购买，也可以拿在售商品向你申请换物。</div>
        </section>

      </div>

      <!-- 底部留白 -->
      <div class="bottom-safe"></div>
    </div>

    <!-- 底部发布按钮 -->
    <div class="bottom-bar">
      <div class="bottom-inner">
        <button class="publish-btn" :disabled="submitting" @click="submitPublish">
          {{ submitting ? '发布中...' : (form.tradeMode === 'exchange' ? '发布换物' : '发布') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getCategoryList, addGoods } from '../api/goods'
import { saveDraft, getDraft, deleteDraft } from '../api/draft'
import { uploadImage } from '../api/upload'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()
const fileInput = ref(null)
const categoryList = ref([])
const imageList = ref([])      // 预览用的完整 URL
const imagePaths = ref([])     // 存库用的相对路径（如 /upload/xxx.jpg），换域名不失效
const submitting = ref(false)

const form = ref({
  title: '',
  description: '',
  price: '',
  originalPrice: '',
  categoryId: '',
  location: '',
  tradeMode: 'cash',
  canMail: false,
  canDeliver: false,
  exchangeSupported: false,
  exchangeWant: ''
})

/**
 * 涨价返券预览（与后端 GoodsService.markupOf 档位保持一致）：
 * 平台标价 = 基础价 + 加价；优惠券 = 加价 / 2；买家实付 = 基础价 + 加价 / 2
 */
const couponPreview = computed(() => {
  const p = Number(form.value.price)
  if (!p || p <= 0) return null
  const markup = p < 50 ? 6 : p < 200 ? 16 : p < 1000 ? 40 : 100
  return {
    list: (p + markup).toFixed(2),
    coupon: (markup / 2).toFixed(2),
    pay: (p + markup / 2).toFixed(2)
  }
})

import { baseURL as BASE_URL } from '../config'

/* ==================== 草稿箱：自动保存与恢复 ==================== */
const DRAFT_KEY = 'publishDraftId'
const draftId = ref(null)
const draftSaving = ref(false)
const draftSavedAt = ref('')
let suppressAutosave = true
let saveTimer = null

const formatClock = (v) => {
  const d = v ? new Date(v) : new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}`
}

// 有内容才保存，避免空白草稿进草稿箱
const hasContent = () => {
  const f = form.value
  return !!(
    f.title.trim() || f.description.trim() || f.price || f.originalPrice ||
    f.categoryId || f.location.trim() || f.exchangeWant.trim() ||
    imagePaths.value.length
  )
}

const buildDraftPayload = () => ({
  id: draftId.value,
  title: form.value.title,
  description: form.value.description,
  price: form.value.price === '' ? null : Number(form.value.price),
  originalPrice: form.value.originalPrice === '' ? null : Number(form.value.originalPrice),
  images: imagePaths.value.join(','),
  coverImage: imagePaths.value.length ? imagePaths.value[0] : null,
  categoryId: form.value.categoryId || null,
  location: form.value.location,
  tradeMode: form.value.tradeMode,
  canMail: form.value.canMail ? 1 : 0,
  canDeliver: form.value.canDeliver ? 1 : 0,
  exchangeSupported: form.value.exchangeSupported ? 1 : 0,
  exchangeWant: form.value.exchangeWant
})

const saveDraftNow = async () => {
  if (suppressAutosave || !hasContent()) return
  draftSaving.value = true
  try {
    const res = await saveDraft(buildDraftPayload())
    if (res && res.id) {
      draftId.value = res.id
      localStorage.setItem(DRAFT_KEY, res.id)
    }
    draftSavedAt.value = formatClock(res && res.updateTime)
  } catch (e) {
    // 自动保存失败不打断填写，下次输入会继续重试
  } finally {
    draftSaving.value = false
  }
}

const scheduleSave = () => {
  if (suppressAutosave) return
  clearTimeout(saveTimer)
  saveTimer = setTimeout(saveDraftNow, 1000)
}

const loadDraft = async (id) => {
  try {
    const d = await getDraft(id)
    if (!d) return false
    suppressAutosave = true
    form.value.title = d.title || ''
    form.value.description = d.description || ''
    form.value.price = d.price != null ? String(d.price) : ''
    form.value.originalPrice = d.originalPrice != null ? String(d.originalPrice) : ''
    form.value.categoryId = d.categoryId || ''
    form.value.location = d.location || ''
    form.value.tradeMode = d.tradeMode === 'exchange' ? 'exchange' : 'cash'
    form.value.canMail = d.canMail === 1
    form.value.canDeliver = d.canDeliver === 1
    form.value.exchangeSupported = d.exchangeSupported === 1
    form.value.exchangeWant = d.exchangeWant || ''

    const raw = d.images || d.coverImage || ''
    const paths = raw.split(',').map(s => s.trim()).filter(Boolean)
    imagePaths.value = paths
    imageList.value = paths.map(p => (p.startsWith('http') ? p : BASE_URL + p))

    draftId.value = d.id
    localStorage.setItem(DRAFT_KEY, d.id)
    draftSavedAt.value = formatClock(d.updateTime)
    ElMessage.info('已恢复上次未发布完的草稿')
    setTimeout(() => { suppressAutosave = false }, 0)
    return true
  } catch (e) {
    if (Number(localStorage.getItem(DRAFT_KEY)) === id) localStorage.removeItem(DRAFT_KEY)
    return false
  }
}

const goDrafts = () => {
  saveDraftNow()
  router.push('/drafts')
}

watch(form, scheduleSave, { deep: true })
watch(imagePaths, scheduleSave, { deep: true })

onBeforeUnmount(() => {
  clearTimeout(saveTimer)
  saveDraftNow()
})

onMounted(async () => {
  try {
    const res = await getCategoryList()
    categoryList.value = res || []
  } catch (e) {
    categoryList.value = []
  }

  // 草稿箱「新建发布」清空绑定，其余情况恢复上次未完成的草稿
  if (route.query.new === '1') {
    localStorage.removeItem(DRAFT_KEY)
    suppressAutosave = false
    return
  }
  const queryId = route.query.draftId ? Number(route.query.draftId) : null
  const storedId = Number(localStorage.getItem(DRAFT_KEY)) || null
  const targetId = queryId || storedId
  if (targetId) {
    const ok = await loadDraft(targetId)
    if (!ok && queryId) ElMessage.warning('草稿不存在或已被删除')
  }
  suppressAutosave = false
})


const triggerUpload = () => {
  fileInput.value && fileInput.value.click()
}

const ALLOWED_IMG_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
const MAX_IMG_SIZE = 5 * 1024 * 1024

const handleFileChange = async (e) => {
  const files = Array.from(e.target.files)
  const remain = 9 - imageList.value.length
  if (remain <= 0) {
    ElMessage.warning('最多上传9张图片')
    return
  }
  const toUpload = files.slice(0, remain)
  for (const file of toUpload) {
    // 上传前先校验类型与大小，避免等到服务端才报错
    if (!ALLOWED_IMG_TYPES.includes(file.type)) {
      ElMessage.warning(`${file.name} 格式不支持，仅支持 jpg/png/webp/gif`)
      continue
    }
    if (file.size > MAX_IMG_SIZE) {
      ElMessage.warning(`${file.name} 超过 5MB，请压缩后上传`)
      continue
    }
    try {
      const res = await uploadImage(file)
      // 后端返回相对路径（如 /upload/xxx.jpg）
      const relPath = typeof res === 'string' ? res : (res && res.url ? res.url : '')
      if (relPath) {
        imagePaths.value.push(relPath)
        // 预览用完整地址；已是 http 开头的直接用（兼容历史绝对路径数据）
        imageList.value.push(relPath.startsWith('http') ? relPath : BASE_URL + relPath)
      }
    } catch (err) {
      ElMessage.error(`${file.name} 上传失败`)
    }
  }
  // 重置input，允许再次选择相同文件
  e.target.value = ''
}

const removeImage = (index) => {
  imageList.value.splice(index, 1)
  imagePaths.value.splice(index, 1)
}

const selectTradeMode = (mode) => {
  form.value.tradeMode = mode
  if (mode === 'exchange') form.value.exchangeSupported = true
}

const submitPublish = async () => {
  // 验证
  if (!form.value.title.trim()) {
    ElMessage.warning('请填写商品标题')
    return
  }
  if (!form.value.price || Number(form.value.price) <= 0) {
    ElMessage.warning(form.value.tradeMode === 'exchange' ? '请填写合理的参考价' : '请填写合理的售价')
    return
  }
  if (!form.value.categoryId) {
    ElMessage.warning('请选择商品分类')
    return
  }
  if (!form.value.location.trim()) {
    ElMessage.warning('请填写交易地点')
    return
  }

  const user = JSON.parse(localStorage.getItem('user') || '{}')
  if (!user.id) {
    ElMessage.error('请先登录')
    router.push('/login')
    return
  }

  if (form.value.tradeMode === 'exchange') {
    try {
      await ElMessageBox.confirm(
        '换物商品不会发生现金支付，填写的价格仅作价值参考。发布后，其他同学可以拿自己的在售商品向你申请交换。确认发布吗？',
        '确认发布换物商品',
        { confirmButtonText: '确认发布', cancelButtonText: '再检查一下', type: 'info' }
      )
    } catch (err) {
      return
    }
  }

  const goodsData = {
    title: form.value.title.trim(),
    description: form.value.description.trim(),
    price: Number(form.value.price),
    originalPrice: form.value.originalPrice ? Number(form.value.originalPrice) : null,
    images: imagePaths.value.join(','),
    coverImage: imagePaths.value.length > 0 ? imagePaths.value[0] : '',
    categoryId: form.value.categoryId,
    sellerId: user.id,
    sellerName: user.username,
    location: form.value.location.trim(),
    tradeMode: form.value.tradeMode,
    canMail: form.value.canMail ? 1 : 0,
    canDeliver: form.value.canDeliver ? 1 : 0,
    exchangeSupported: (form.value.tradeMode === 'exchange' || form.value.exchangeSupported) ? 1 : 0,
    exchangeWant: (form.value.tradeMode === 'exchange' || form.value.exchangeSupported) ? form.value.exchangeWant.trim() : null,
    status: 'on_sale'
  }

  submitting.value = true
  try {
    await addGoods(goodsData)
    // 发布成功：清除对应草稿，避免下次进入重复恢复
    suppressAutosave = true
    clearTimeout(saveTimer)
    if (draftId.value) deleteDraft(draftId.value).catch(() => {})
    localStorage.removeItem(DRAFT_KEY)
    ElMessage.success(form.value.tradeMode === 'exchange' ? '换物商品发布成功，可在「我的 → 我的换物」处理申请' : '发布成功！')
    router.push('/')
  } catch (err) {
    ElMessage.error('发布失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.publish-page {
  min-height: 100vh;
  background-color: #fff;
  background-image: linear-gradient(
    180deg,
    #FF0F23 0px,
    #FF3B3B 64px,
    #FF9A9A 146px,
    #FFD6D6 220px,
    #FFFFFF 300px
  );
  background-repeat: no-repeat;
}

/* 顶部导航（透明叠在渐变上） */
.nav-bar {
  position: relative;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 16px;
  color: #fff;
}
.nav-back {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #fff;
}
.nav-back:active {
  opacity: 0.6;
}
.nav-title {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
}
.nav-placeholder {
  width: 32px;
}
.nav-drafts {
  min-width: 32px;
  padding: 5px 10px;
  font-size: 12px;
  color: #fff;
  text-align: center;
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: 13px;
  cursor: pointer;
  transition: background 0.16s ease;
}
.nav-drafts:hover {
  background: rgba(255, 255, 255, 0.18);
}

/* 草稿自动保存提示 */
.draft-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin: -8px 0 18px;
  padding: 8px 12px;
  font-size: 12px;
  color: #9AA0A6;
  background: #FAFBFC;
  border: 1px dashed #E5E7EB;
  border-radius: 8px;
}
.draft-tip.saved {
  color: #059669;
  background: #F0FDF9;
  border-color: #BBF7E0;
}
.draft-link {
  flex-shrink: 0;
  color: #E1251B;
  cursor: pointer;
}
.draft-link:hover {
  text-decoration: underline;
}

/* 内容区域 */
.content-area {
  max-width: 720px;
  margin: 0 auto;
  padding: 4px 16px 0;
}

/* 白色主卡片 */
.publish-card {
  padding: 30px 34px 34px;
  background: #fff;
  border: 1px solid #DCDCDC;
  border-radius: 14px;
  box-shadow: 0 10px 34px rgba(0, 0, 0, 0.12);
}

.page-heading {
  margin: 0 0 22px;
  font-size: 22px;
  font-weight: 700;
  color: #1f1f1f;
}

.form-section + .form-section {
  margin-top: 26px;
  padding-top: 24px;
  border-top: 1px solid #f4f4f4;
}

.section-title {
  margin: 0 0 16px;
  font-size: 15px;
  font-weight: 700;
  color: #222;
}

.field + .field {
  margin-top: 18px;
}

.field-label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  color: #333;
}

.req {
  margin-left: 2px;
  color: #FF0F23;
}

.field-sub,
.field-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #b5b5b5;
}

/* 涨价返券预览条 */
.coupon-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
  padding: 10px 12px;
  background: linear-gradient(120deg, #FFF7ED, #FFF1F2);
  border: 1px dashed #FDBA74;
  border-radius: 8px;
  font-size: 12px;
  color: #9A3412;
}

.cp-tag {
  background: linear-gradient(135deg, #FF6A00, #E1251B);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 9px;
}

.cp-text b { color: #C2410C; font-weight: 700; }
.cp-sep { color: #FDBA74; }
.cp-coupon { color: #E1251B; font-weight: 700; }

.field-hint {
  margin-top: 10px;
}

/* 输入框 */
.text-input {
  width: 100%;
  height: 42px;
  padding: 0 12px;
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  background: #fff;
  font-size: 14px;
  color: #333;
  outline: none;
  font-family: inherit;
  transition: border-color 0.2s;
}
.text-input:focus {
  border-color: #FF8A8A;
}
.text-input::placeholder {
  color: #c4c4c4;
}

/* 图片上传 */
.upload-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.upload-item {
  width: 96px;
  height: 96px;
  border-radius: 6px;
  overflow: hidden;
  position: relative;
}
.upload-item.has-image {
  border: 1px solid #f0f0f0;
}
.preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover-badge {
  position: absolute;
  top: 0;
  left: 0;
  background: #FF0F23;
  color: #fff;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 0 0 6px 0;
  font-weight: 500;
}
.delete-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 18px;
  height: 18px;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.add-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  border: 1px dashed #d9d9d9;
  background: #fafafa;
  color: #bbb;
  cursor: pointer;
  transition: all 0.2s;
}
.add-btn:hover {
  border-color: #FF8A8A;
  background: #FFF7F7;
}
.add-plus {
  font-size: 24px;
  line-height: 1;
  font-weight: 300;
  color: #c9c9c9;
}
.upload-tip {
  font-size: 11px;
  color: #b9b9b9;
}

/* 描述 */
.desc-wrap {
  position: relative;
}
.desc-textarea {
  width: 100%;
  min-height: 120px;
  padding: 12px 14px 26px;
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  background: #fff;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  outline: none;
  resize: vertical;
  font-family: inherit;
  transition: border-color 0.2s;
}
.desc-textarea:focus {
  border-color: #FF8A8A;
}
.desc-textarea::placeholder {
  color: #c4c4c4;
}
.desc-count {
  position: absolute;
  right: 12px;
  bottom: 10px;
  font-size: 12px;
  color: #c4c4c4;
}

/* 两列布局 */
.two-col {
  display: flex;
  gap: 18px;
}
.col {
  flex: 1;
  min-width: 0;
}

/* 交易方式 */
.mode-row {
  display: flex;
  gap: 12px;
}

.mode-card {
  flex: 1;
  padding: 14px 16px;
  border: 1px solid #E5E7EB;
  border-radius: 10px;
  cursor: pointer;
  transition: border-color 0.16s ease, background 0.16s ease;
}

.mode-card b {
  display: block;
  font-size: 14px;
  color: #1F2937;
}

.mode-card span {
  display: block;
  margin-top: 4px;
  font-size: 11.5px;
  color: #9CA3AF;
}

.mode-card.active {
  border-color: #E1251B;
  background: #FFF7F5;
}

.mode-card.active b {
  color: #E1251B;
}

.check-row {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 14px;
}

.check-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #4B5563;
  cursor: pointer;
}

.check-item input {
  width: 15px;
  height: 15px;
  accent-color: #E1251B;
}

.exchange-want {
  margin-top: 14px;
}

/* 价格输入 */
.price-box {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 42px;
  padding: 0 12px;
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  transition: border-color 0.2s;
}
.price-box:focus-within {
  border-color: #FF8A8A;
}
.price-symbol {
  font-size: 15px;
  font-weight: 700;
  color: #FF0F23;
  flex-shrink: 0;
}
.price-symbol.gray {
  color: #c4c4c4;
}
.price-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  font-size: 15px;
  font-weight: 600;
  color: #222;
  font-family: inherit;
}
.price-input.origin {
  color: #999;
  font-weight: 500;
}
.price-input::placeholder {
  color: #c4c4c4;
  font-weight: 400;
}

/* 分类下拉 */
.select-box {
  position: relative;
  display: flex;
  align-items: center;
  height: 42px;
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  transition: border-color 0.2s;
}
.select-box:focus-within {
  border-color: #FF8A8A;
}
.select-input {
  flex: 1;
  height: 100%;
  padding: 0 32px 0 12px;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  color: #333;
  font-family: inherit;
  appearance: none;
  -webkit-appearance: none;
  cursor: pointer;
}
.select-arrow {
  position: absolute;
  right: 12px;
  pointer-events: none;
}

/* 底部留白 */
.bottom-safe {
  height: 96px;
}

/* 底部发布按钮 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 10px 16px;
  padding-bottom: calc(10px + env(safe-area-inset-bottom, 0px));
  background: #fff;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.08);
  z-index: 100;
}
.bottom-inner {
  max-width: 720px;
  margin: 0 auto;
}
.publish-btn {
  width: 100%;
  height: 44px;
  border: none;
  border-radius: 22px;
  background: linear-gradient(180deg, #FFE60F, #FFD400);
  color: #2b2b2b;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: filter 0.2s;
  font-family: inherit;
  letter-spacing: 1px;
}
.publish-btn:hover {
  filter: brightness(0.98);
}
.publish-btn:active {
  filter: brightness(0.95);
}
.publish-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .publish-card {
    padding: 22px 18px 24px;
    border-radius: 12px;
  }
  .page-heading {
    font-size: 19px;
    margin-bottom: 18px;
  }
  .two-col {
    flex-direction: column;
    gap: 14px;
  }
  .upload-item {
    width: 84px;
    height: 84px;
  }
  .bottom-inner {
    max-width: 100%;
  }
}
</style>
