<template>
  <ServiceShell title="闲置盲盒" subtitle="许个愿，让卖家随机打包一份小惊喜" badge="盒">
    <section class="topbar">
      <div class="tabs">
        <button
          v-for="t in tabs"
          :key="t.key"
          class="tab"
          :class="{ active: scope === t.key }"
          @click="switchScope(t.key)"
        >
          {{ t.label }}
          <span v-if="counts[t.key]">{{ counts[t.key] }}</span>
        </button>
      </div>
      <el-button type="primary" @click="openWish">我要许愿</el-button>
    </section>

    <div class="hint">{{ note }}</div>

    <div v-loading="loading" class="grid">
      <article v-for="item in list" :key="item.id" class="box-card">
        <div class="card-head">
          <span class="cat-chip">{{ item.category }}</span>
          <span class="status" :class="item.status">{{ item.statusLabel }}</span>
        </div>

        <div class="wish-text">{{ item.wishNote || '这位同学没有留下具体说明，交给缘分吧' }}</div>

        <div class="meta">
          <span class="budget">预算 ¥{{ money(item.budget) }}</span>
          <span>{{ item.createTime }}</span>
        </div>

        <div class="buyer">
          <SmartImage :src="item.buyerAvatar" class="avatar" />
          <span class="buyer-name">{{ item.buyerName || '匿名同学' }}</span>
          <span class="mine-tag" v-if="item.mine">我的许愿</span>
        </div>

        <div class="box-info" v-if="item.boxTitle">
          <div class="box-title">盲盒：{{ item.boxTitle }}</div>
          <div class="box-desc" v-if="item.boxDesc">{{ item.boxDesc }}</div>
          <div class="box-price" v-if="item.price != null">
            成交价 ¥{{ money(item.price) }}
            <span v-if="item.sellerName">· 卖家 {{ item.sellerName }}</span>
          </div>
        </div>

        <div class="card-foot">
          <el-button v-if="item.canClaim" type="primary" size="small" @click="openClaim(item)">认领打包</el-button>
          <el-button v-if="item.canConfirm" type="success" size="small" @click="confirmItem(item)">确认收货</el-button>
          <el-button v-if="item.canCancel" size="small" @click="cancelItem(item)">取消许愿</el-button>
          <span class="wait" v-if="!item.canClaim && !item.canConfirm && !item.canCancel">
            {{ waitText(item) }}
          </span>
        </div>
      </article>
    </div>

    <el-empty
      v-if="!loading && !list.length"
      :description="emptyText"
      :image-size="90"
    />

    <!-- 许愿弹窗 -->
    <el-dialog v-model="wishVisible" title="许一个闲置盲盒愿望" width="460px">
      <div class="form-tip">写下你想要的品类和预算，卖家会随机打包，内容以实物为准</div>
      <el-form label-width="76px" label-position="left">
        <el-form-item label="许愿品类">
          <el-select v-model="wishForm.category" placeholder="选择品类" style="width: 100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="预算上限">
          <el-input-number v-model="wishForm.budget" :min="5" :max="200" :step="5" style="width: 160px" />
          <span class="unit">元</span>
        </el-form-item>
        <el-form-item label="许愿说明">
          <el-input
            v-model="wishForm.wishNote"
            type="textarea"
            :rows="3"
            maxlength="100"
            show-word-limit
            placeholder="例如：想要几支好写的黑笔和一本笔记本，期末复习用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="wishVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitWish">发布许愿</el-button>
      </template>
    </el-dialog>

    <!-- 认领打包弹窗 -->
    <el-dialog v-model="claimVisible" title="认领并打包盲盒" width="480px">
      <div class="claim-target" v-if="current">
        <div class="target-line">
          <span class="cat-chip">{{ current.category }}</span>
          <span class="target-budget">买家预算 ¥{{ money(current.budget) }}</span>
        </div>
        <div class="target-note">{{ current.wishNote || '买家没有留下具体说明' }}</div>
      </div>
      <el-form label-width="76px" label-position="left">
        <el-form-item label="盲盒标题">
          <el-input v-model="claimForm.boxTitle" maxlength="40" placeholder="例如：考研文具加油包" />
        </el-form-item>
        <el-form-item label="盲盒描述">
          <el-input
            v-model="claimForm.boxDesc"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="简单介绍会放哪些类型的东西，保留一点惊喜感"
          />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="claimForm.price" :min="1" :max="maxClaimPrice" :step="1" style="width: 160px" />
          <span class="unit">元（不高于买家预算）</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="claimVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitClaim">确认认领</el-button>
      </template>
    </el-dialog>
  </ServiceShell>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ServiceShell from '../components/ServiceShell.vue'
import SmartImage from '../components/SmartImage.vue'
import { getBlindBoxList, publishWish, claimWish, confirmWish, cancelWish } from '../api/blindbox'

const tabs = [
  { key: 'open', label: '待认领' },
  { key: 'mine', label: '我的许愿' },
  { key: 'claimed', label: '我接的单' }
]

const scope = ref('open')
const list = ref([])
const counts = ref({ open: 0, mine: 0, claimed: 0 })
const categories = ref(['文具', '书籍', '数码配件', '生活杂物', '随机'])
const note = ref('')
const loading = ref(false)
const submitting = ref(false)

const wishVisible = ref(false)
const wishForm = ref({ category: '文具', budget: 20, wishNote: '' })

const claimVisible = ref(false)
const claimForm = ref({ boxTitle: '', boxDesc: '', price: 10 })
const current = ref(null)

const maxClaimPrice = computed(() => {
  const budget = Number(current.value?.budget || 0)
  return budget > 0 ? budget : 200
})

const emptyText = computed(() => {
  if (scope.value === 'open') return '暂时没有待认领的许愿，等等同学来许愿吧'
  if (scope.value === 'mine') return '还没有许过愿，点右上角「我要许愿」试试'
  return '还没有认领过许愿，去「待认领」看看吧'
})

const money = (v) => Number(v || 0).toFixed(2)

const waitText = (item) => {
  if (item.status === 'completed') return '盲盒已送达'
  if (item.status === 'cancelled') return '许愿已取消'
  if (item.status === 'open') return '等待卖家认领'
  if (item.status === 'matched') return '等待买家确认收货'
  return ''
}

const load = async () => {
  loading.value = true
  try {
    const res = await getBlindBoxList(scope.value)
    list.value = Array.isArray(res?.list) ? res.list : []
    counts.value = res?.counts || { open: 0, mine: 0, claimed: 0 }
    if (Array.isArray(res?.categories) && res.categories.length) {
      categories.value = res.categories
    }
    note.value = res?.note || ''
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

const switchScope = (key) => {
  if (scope.value === key) return
  scope.value = key
  load()
}

const openWish = () => {
  if (!categories.value.includes(wishForm.value.category)) {
    wishForm.value.category = categories.value[0]
  }
  wishForm.value.budget = wishForm.value.budget || 20
  wishForm.value.wishNote = ''
  wishVisible.value = true
}

const submitWish = async () => {
  if (!wishForm.value.category) {
    ElMessage.warning('请选择许愿品类')
    return
  }
  const budget = Number(wishForm.value.budget)
  if (!budget || budget < 5 || budget > 200) {
    ElMessage.warning('预算需在 5 ~ 200 元之间')
    return
  }
  submitting.value = true
  try {
    await publishWish({
      category: wishForm.value.category,
      budget,
      wishNote: wishForm.value.wishNote.trim()
    })
    wishVisible.value = false
    ElMessage.success('许愿成功，等待卖家认领')
    scope.value = 'mine'
    await load()
  } catch (e) {
    // 接口已提示
  } finally {
    submitting.value = false
  }
}

const openClaim = (item) => {
  current.value = item
  claimForm.value = {
    boxTitle: '',
    boxDesc: '',
    price: Math.min(Number(item.budget) || 10, Number(item.budget) || 10)
  }
  claimVisible.value = true
}

const submitClaim = async () => {
  if (!current.value) return
  const title = claimForm.value.boxTitle.trim()
  if (!title) {
    ElMessage.warning('请填写盲盒标题')
    return
  }
  const price = Number(claimForm.value.price)
  if (!price || price <= 0) {
    ElMessage.warning('请填写合理的价格')
    return
  }
  if (price > maxClaimPrice.value) {
    ElMessage.warning('价格不能高于买家预算')
    return
  }
  submitting.value = true
  try {
    await claimWish(current.value.id, {
      boxTitle: title,
      boxDesc: claimForm.value.boxDesc.trim(),
      price
    })
    claimVisible.value = false
    ElMessage.success('认领成功，记得尽快把盲盒交给买家')
    scope.value = 'claimed'
    await load()
  } catch (e) {
    // 接口已提示（可能已被别人抢先认领）
  } finally {
    submitting.value = false
  }
}

const confirmItem = async (item) => {
  try {
    await ElMessageBox.confirm('确认已经收到盲盒了吗？确认后交易完成。', '确认收货', {
      confirmButtonText: '确认收货',
      cancelButtonText: '再等等',
      type: 'info'
    })
  } catch (e) {
    return
  }
  try {
    await confirmWish(item.id)
    ElMessage.success('已确认收货，感谢参与闲置循环')
    await load()
  } catch (e) {
    // 接口已提示
  }
}

const cancelItem = async (item) => {
  try {
    await ElMessageBox.confirm('取消后该许愿将不再被卖家看到，确定取消吗？', '取消许愿', {
      confirmButtonText: '确定取消',
      cancelButtonText: '再想想',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await cancelWish(item.id)
    ElMessage.success('许愿已取消')
    await load()
  } catch (e) {
    // 接口已提示
  }
}

onMounted(load)
</script>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 16px;
  margin-bottom: 10px;
  background: #fff;
  border: 1px solid #EDEEF0;
  border-radius: 12px;
}

.tabs {
  display: flex;
  gap: 6px;
  background: #F6F7F9;
  padding: 4px;
  border-radius: 20px;
}

.tab {
  display: flex;
  align-items: center;
  gap: 5px;
  height: 30px;
  padding: 0 14px;
  border: none;
  border-radius: 16px;
  background: transparent;
  color: #6B7280;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.16s ease;
}

.tab span {
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  line-height: 16px;
  font-size: 10px;
  border-radius: 8px;
  background: #E5E7EB;
  color: #6B7280;
}

.tab.active {
  background: #fff;
  color: #E1251B;
  box-shadow: 0 2px 8px rgba(31, 41, 55, 0.08);
}

.tab.active span {
  background: #FFF1F0;
  color: #E1251B;
}

.hint {
  padding: 0 4px 12px;
  font-size: 12px;
  color: #A5AAB3;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  min-height: 120px;
}

.box-card {
  display: flex;
  flex-direction: column;
  padding: 14px 16px 16px;
  background: #fff;
  border: 1px solid #EDEEF0;
  border-radius: 12px;
  transition: all 0.16s ease;
}

.box-card:hover {
  border-color: #FFD4D0;
  box-shadow: 0 6px 18px rgba(225, 37, 27, 0.07);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.cat-chip {
  display: inline-block;
  padding: 2px 9px;
  border-radius: 10px;
  background: #FFF1F0;
  border: 1px solid #FFD4D0;
  color: #E1251B;
  font-size: 11px;
  font-weight: 700;
}

.status {
  font-size: 11px;
  font-weight: 600;
  color: #8A8F99;
}

.status.open {
  color: #D97706;
}

.status.matched {
  color: #2563EB;
}

.status.completed {
  color: #17694C;
}

.status.cancelled {
  color: #B0B4BA;
}

.wish-text {
  margin-top: 10px;
  font-size: 14px;
  color: #1F2937;
  line-height: 1.55;
  min-height: 44px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
  font-size: 11px;
  color: #A5AAB3;
}

.meta .budget {
  color: #E1251B;
  font-weight: 700;
  font-size: 13px;
}

.buyer {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed #F2F3F5;
}

.avatar {
  width: 26px;
  height: 26px;
  flex-shrink: 0;
  border-radius: 50%;
  overflow: hidden;
  background: #F6F7F9;
}

.buyer-name {
  font-size: 12px;
  color: #4B5563;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mine-tag {
  margin-left: auto;
  flex-shrink: 0;
  font-size: 10px;
  color: #E1251B;
  background: #FFF1F0;
  padding: 1px 6px;
  border-radius: 8px;
}

.box-info {
  margin-top: 10px;
  padding: 10px 11px;
  background: #F7FBF9;
  border: 1px solid #DFF1E8;
  border-radius: 9px;
}

.box-title {
  font-size: 12px;
  font-weight: 700;
  color: #17694C;
}

.box-desc {
  margin-top: 4px;
  font-size: 11px;
  color: #6B7280;
  line-height: 1.5;
}

.box-price {
  margin-top: 5px;
  font-size: 11px;
  color: #17694C;
}

.card-foot {
  margin-top: auto;
  padding-top: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
}

.wait {
  font-size: 11px;
  color: #B0B4BA;
}

.form-tip {
  margin-bottom: 14px;
  font-size: 12px;
  color: #8A8F99;
  line-height: 1.5;
}

.unit {
  margin-left: 8px;
  font-size: 12px;
  color: #A5AAB3;
}

.claim-target {
  margin-bottom: 16px;
  padding: 12px 13px;
  background: #FFF9F8;
  border: 1px solid #FFE4E0;
  border-radius: 10px;
}

.target-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.target-budget {
  font-size: 12px;
  font-weight: 700;
  color: #E1251B;
}

.target-note {
  margin-top: 7px;
  font-size: 12px;
  color: #6B7280;
  line-height: 1.5;
}

@media (max-width: 1000px) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 620px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .topbar {
    align-items: stretch;
  }

  .tabs {
    justify-content: space-between;
  }

  .tab {
    flex: 1;
    justify-content: center;
    padding: 0 6px;
  }
}
</style>
