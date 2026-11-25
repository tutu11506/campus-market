<template>
  <ServiceShell title="校友标签" subtitle="同频同学一键找到：考研 · 摄影 · 清仓 · 找搭子" badge="签">
    <section class="my-panel">
      <div class="my-left">
        <div class="my-title">我的标签</div>
        <div class="my-tags" v-if="myTags.length">
          <span v-for="tag in myTags" :key="tag" class="tag-chip mine">{{ tag }}</span>
        </div>
        <div class="my-empty" v-else>还没设置标签，选几个让同学更快找到你</div>
      </div>
      <el-button type="primary" plain @click="openEditor">编辑标签</el-button>
    </section>

    <section class="tag-panel">
      <div class="panel-head">
        <div class="panel-title">按标签找同学</div>
        <div class="panel-note">标签由同学自己设置，点击即可筛选</div>
      </div>
      <div v-loading="tagsLoading" class="tag-grid">
        <button
          v-for="t in tags"
          :key="t.tag"
          class="tag-card"
          :class="{ active: activeTag === t.tag, dim: t.userCount === 0 }"
          @click="selectTag(t.tag)"
        >
          <div class="tag-name">{{ t.tag }}</div>
          <div class="tag-tip">{{ t.tip }}</div>
          <div class="tag-counts">
            <span class="users">{{ t.userCount }} 位同学</span>
            <span>{{ t.goodsCount }} 件闲置</span>
          </div>
        </button>
      </div>
    </section>

    <section class="result-panel" v-if="activeTag">
      <div class="panel-head">
        <div class="panel-title">
          「{{ activeTag }}」的同学
          <span class="panel-count">{{ result.userCount ?? 0 }} 位 · {{ result.goodsCount ?? 0 }} 件在售</span>
        </div>
        <div class="panel-note">{{ result.tip }}</div>
      </div>

      <div v-loading="resultLoading" class="result-body">
        <div class="block" v-if="result.users && result.users.length">
          <div class="block-title">同学</div>
          <div class="user-grid">
            <div v-for="u in result.users" :key="u.id" class="user-card" @click="goUser(u.id)">
              <SmartImage :src="u.avatar" class="user-avatar" />
              <div class="user-main">
                <div class="user-name">{{ u.username }}</div>
                <div class="user-meta">
                  <span v-if="u.studentNo">学号 {{ u.studentNo }}</span>
                  <span v-if="u.credit != null">信用 {{ u.credit }}</span>
                  <span class="sale">{{ u.onSaleCount }} 件在售</span>
                </div>
                <div class="user-tags" v-if="u.tags && u.tags.length">
                  <span v-for="tag in u.tags" :key="tag" class="tag-chip small">{{ tag }}</span>
                </div>
              </div>
              <el-icon class="user-arrow" :size="14"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>

        <div class="block" v-if="result.goods && result.goods.length">
          <div class="block-title">TA 们的闲置</div>
          <div class="goods-grid">
            <article v-for="g in result.goods" :key="g.id" class="goods-card" @click="goGoods(g.id)">
              <SmartImage :src="g.coverImage" class="goods-cover" />
              <div class="goods-body">
                <div class="goods-title">{{ g.title }}</div>
                <div class="goods-price">¥{{ money(g.price) }}</div>
                <div class="goods-seller">
                  <span>{{ g.sellerName }}</span>
                  <span class="views">{{ g.viewCount }} 浏览</span>
                </div>
              </div>
            </article>
          </div>
        </div>

        <el-empty
          v-if="!resultLoading && !(result.users && result.users.length)"
          description="这个标签暂时还没有同学，快去设置一个吧"
          :image-size="80"
        />
      </div>
    </section>

    <el-dialog v-model="editorVisible" title="选择校友标签" width="560px">
      <div class="edit-tip">最多选择 {{ maxTags }} 个，标签会展示在同学列表与卖家主页，方便互相找到</div>
      <div class="edit-grid">
        <button
          v-for="t in tags"
          :key="t.tag"
          class="edit-chip"
          :class="{ active: draft.includes(t.tag) }"
          @click="toggleDraft(t.tag)"
        >
          <span>{{ t.tag }}</span>
          <el-icon v-if="draft.includes(t.tag)" :size="13"><Check /></el-icon>
        </button>
      </div>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </ServiceShell>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ServiceShell from '../components/ServiceShell.vue'
import SmartImage from '../components/SmartImage.vue'
import { getAlumniTags, getAlumniUsers, saveMyTags } from '../api/alumni'

const router = useRouter()
const tags = ref([])
const myTags = ref([])
const maxTags = ref(5)
const tagsLoading = ref(false)
const activeTag = ref('')
const result = ref({ users: [], goods: [] })
const resultLoading = ref(false)

const editorVisible = ref(false)
const draft = ref([])
const saving = ref(false)

const loadTags = async () => {
  tagsLoading.value = true
  try {
    const res = await getAlumniTags()
    tags.value = Array.isArray(res?.tags) ? res.tags : []
    myTags.value = Array.isArray(res?.myTags) ? res.myTags : []
    maxTags.value = res?.maxTags || 5
    // 默认选中第一个有同学的标签，直接展示内容
    if (!activeTag.value && tags.value.length) {
      const first = tags.value.find(t => t.userCount > 0) || tags.value[0]
      selectTag(first.tag)
    }
  } catch (e) {
    tags.value = []
  } finally {
    tagsLoading.value = false
  }
}

const selectTag = async (tag) => {
  activeTag.value = tag
  resultLoading.value = true
  try {
    const res = await getAlumniUsers(tag)
    result.value = {
      tip: res?.tip || '',
      userCount: res?.userCount ?? 0,
      goodsCount: res?.goodsCount ?? 0,
      users: Array.isArray(res?.users) ? res.users : [],
      goods: Array.isArray(res?.goods) ? res.goods : []
    }
  } catch (e) {
    result.value = { users: [], goods: [] }
  } finally {
    resultLoading.value = false
  }
}

const openEditor = () => {
  draft.value = [...myTags.value]
  editorVisible.value = true
}

const toggleDraft = (tag) => {
  const idx = draft.value.indexOf(tag)
  if (idx >= 0) {
    draft.value.splice(idx, 1)
    return
  }
  if (draft.value.length >= maxTags.value) {
    ElMessage.warning(`最多选择 ${maxTags.value} 个标签`)
    return
  }
  draft.value.push(tag)
}

const save = async () => {
  saving.value = true
  try {
    const saved = await saveMyTags(draft.value)
    myTags.value = Array.isArray(saved) ? saved : draft.value
    editorVisible.value = false
    ElMessage.success('标签已保存')
  } catch (e) {
    // 接口已提示
  } finally {
    saving.value = false
  }
}

const goUser = (id) => router.push(`/profile?userId=${id}`)
const goGoods = (id) => router.push(`/goods/${id}`)
const money = (v) => Number(v || 0).toFixed(2)

onMounted(loadTags)
</script>

<style scoped>
.my-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 16px 20px;
  margin-bottom: 14px;
  background: linear-gradient(135deg, #FFF1F0 0%, #FFFFFF 65%);
  border: 1px solid #FFE0DD;
  border-radius: 14px;
}

.my-title {
  font-size: 13px;
  font-weight: 700;
  color: #1F2937;
  margin-bottom: 8px;
}

.my-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.my-empty {
  font-size: 12px;
  color: #A5AAB3;
}

.tag-chip {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #FFD4D0;
  color: #E1251B;
  font-size: 12px;
  font-weight: 600;
}

.tag-chip.small {
  padding: 1px 7px;
  font-size: 11px;
  font-weight: 400;
  border-color: #EDEEF0;
  color: #6B7280;
  background: #F9FAFB;
}

.tag-panel,
.result-panel {
  background: #fff;
  border: 1px solid #EDEEF0;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 14px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 14px 18px;
  border-bottom: 1px solid #F2F3F5;
}

.panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #1F2937;
}

.panel-count {
  margin-left: 6px;
  font-size: 12px;
  font-weight: 400;
  color: #A5AAB3;
}

.panel-note {
  font-size: 12px;
  color: #A5AAB3;
}

.tag-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  padding: 14px 18px 18px;
  min-height: 80px;
}

.tag-card {
  text-align: left;
  padding: 12px 13px;
  border: 1px solid #EDEEF0;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  transition: all 0.16s ease;
}

.tag-card:hover {
  border-color: #FFD4D0;
  box-shadow: 0 4px 14px rgba(225, 37, 27, 0.06);
}

.tag-card.active {
  border-color: #E1251B;
  background: #FFF1F0;
}

.tag-card.dim {
  opacity: 0.6;
}

.tag-name {
  font-size: 14px;
  font-weight: 800;
  color: #1F2937;
}

.tag-card.active .tag-name {
  color: #E1251B;
}

.tag-tip {
  margin-top: 4px;
  font-size: 11px;
  color: #A5AAB3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-counts {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  font-size: 11px;
  color: #8A8F99;
}

.tag-counts .users {
  color: #E1251B;
  font-weight: 700;
}

.result-body {
  min-height: 160px;
  padding: 4px 18px 18px;
}

.block {
  margin-top: 14px;
}

.block-title {
  font-size: 13px;
  font-weight: 700;
  color: #4B5563;
  margin-bottom: 10px;
}

.user-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 11px 13px;
  border: 1px solid #EDEEF0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.16s ease;
}

.user-card:hover {
  border-color: #FFD4D0;
  box-shadow: 0 4px 14px rgba(225, 37, 27, 0.06);
}

.user-avatar {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  border-radius: 50%;
  overflow: hidden;
  background: #F6F7F9;
}

.user-main {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 14px;
  font-weight: 700;
  color: #1F2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-meta {
  display: flex;
  gap: 8px;
  margin-top: 3px;
  font-size: 11px;
  color: #A5AAB3;
}

.user-meta .sale {
  color: #E1251B;
  font-weight: 600;
}

.user-tags {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
  margin-top: 5px;
}

.user-arrow {
  flex-shrink: 0;
  color: #C4C8CE;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.goods-card {
  border: 1px solid #EDEEF0;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.16s ease;
  background: #fff;
}

.goods-card:hover {
  border-color: #FFD4D0;
  box-shadow: 0 6px 18px rgba(225, 37, 27, 0.08);
}

.goods-cover {
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #F6F7F9;
}

.goods-body {
  padding: 9px 10px 11px;
}

.goods-title {
  font-size: 13px;
  color: #1F2937;
  line-height: 1.4;
  height: 36px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.goods-price {
  margin-top: 5px;
  font-size: 15px;
  font-weight: 800;
  color: #E1251B;
}

.goods-seller {
  display: flex;
  justify-content: space-between;
  gap: 6px;
  margin-top: 4px;
  font-size: 11px;
  color: #A5AAB3;
}

.goods-seller .views {
  flex-shrink: 0;
}

.edit-tip {
  font-size: 12px;
  color: #8A8F99;
  margin-bottom: 12px;
}

.edit-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
}

.edit-chip {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 32px;
  padding: 0 13px;
  border: 1px solid #EDEEF0;
  border-radius: 16px;
  background: #fff;
  color: #4B5563;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.16s ease;
}

.edit-chip:hover {
  border-color: #FFD4D0;
  color: #E1251B;
}

.edit-chip.active {
  border-color: #E1251B;
  background: #FFF1F0;
  color: #E1251B;
  font-weight: 700;
}

@media (max-width: 1000px) {
  .tag-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .user-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .goods-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 700px) {
  .tag-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .user-grid,
  .goods-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 520px) {
  .user-grid,
  .goods-grid {
    grid-template-columns: 1fr;
  }
}
</style>
