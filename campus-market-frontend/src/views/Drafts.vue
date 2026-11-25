<template>
  <ServiceShell title="草稿箱" subtitle="发布中断自动保存，随时接着编辑" badge="稿">
    <section class="draft-hero">
      <div>
        <div class="hero-num">{{ drafts.length }}</div>
        <div class="hero-label">份草稿</div>
        <div class="hero-sub">
          发布页填写内容后会自动保存到这里，适合一次性整理多件闲置、慢慢编辑。
        </div>
      </div>
      <div class="hero-actions">
        <el-button type="primary" @click="createNew">新建发布</el-button>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </section>

    <div v-loading="loading" class="draft-list">
      <article v-for="d in drafts" :key="d.id" class="draft-card">
        <SmartImage :src="d.coverImage" class="draft-cover" />
        <div class="draft-main">
          <div class="draft-title">{{ d.title || '未命名草稿' }}</div>
          <div class="draft-meta">
            <span class="price" v-if="d.price != null">&yen;{{ d.price }}</span>
            <span v-else class="muted">未填价格</span>
            <span class="sep">·</span>
            <span>{{ d.categoryName || '未选分类' }}</span>
            <span class="sep">·</span>
            <span>{{ imageCount(d) }} 张图</span>
          </div>
          <div class="draft-time">最近保存 {{ formatTime(d.updateTime) }}</div>
        </div>
        <div class="draft-actions">
          <el-button type="primary" plain size="small" @click="editDraft(d)">继续编辑</el-button>
          <el-button size="small" @click="removeDraft(d)">删除</el-button>
        </div>
      </article>

      <el-empty
        v-if="!loading && !drafts.length"
        description="还没有草稿，去发布页填写内容会自动保存"
        :image-size="80"
      />
    </div>
  </ServiceShell>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ServiceShell from '../components/ServiceShell.vue'
import SmartImage from '../components/SmartImage.vue'
import { getDrafts, deleteDraft } from '../api/draft'

const router = useRouter()
const drafts = ref([])
const loading = ref(false)

const load = async () => {
  loading.value = true
  try {
    const res = await getDrafts()
    drafts.value = Array.isArray(res) ? res : []
  } catch (e) {
    drafts.value = []
  } finally {
    loading.value = false
  }
}

const imageCount = (d) => {
  if (!d.images) return 0
  return String(d.images).split(',').filter(s => s.trim()).length
}

const editDraft = (d) => {
  router.push(`/publish?draftId=${d.id}`)
}

const createNew = () => {
  // 清除「上次编辑」绑定，进入空白发布页
  localStorage.removeItem('publishDraftId')
  router.push('/publish?new=1')
}

const removeDraft = async (d) => {
  try {
    await ElMessageBox.confirm('确定删除这份草稿吗？删除后不可恢复', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteDraft(d.id)
    drafts.value = drafts.value.filter(item => item.id !== d.id)
    if (Number(localStorage.getItem('publishDraftId')) === d.id) {
      localStorage.removeItem('publishDraftId')
    }
    ElMessage.success('草稿已删除')
  } catch (e) { /* 取消或接口已提示 */ }
}

const formatTime = (v) => {
  if (!v) return ''
  const d = new Date(v)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

onMounted(load)
</script>

<style scoped>
.draft-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 20px 22px;
  margin-bottom: 14px;
  border-radius: 14px;
  background: linear-gradient(135deg, #FFF1F0 0%, #FFFFFF 60%);
  border: 1px solid #FFE0DD;
}

.hero-num {
  font-size: 32px;
  font-weight: 800;
  color: #E1251B;
  line-height: 1.1;
}

.hero-label {
  font-size: 13px;
  color: #8A8F99;
  margin-top: 2px;
}

.hero-sub {
  font-size: 12px;
  color: #A5AAB3;
  margin-top: 8px;
}

.hero-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.draft-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 220px;
  padding-bottom: 30px;
}

.draft-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #EDEEF0;
  border-radius: 12px;
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.draft-card:hover {
  border-color: #FFE0DD;
  box-shadow: 0 4px 16px rgba(225, 37, 27, 0.06);
}

.draft-cover {
  width: 72px;
  height: 72px;
  flex-shrink: 0;
  border-radius: 10px;
  overflow: hidden;
  background: #F6F7F9;
}

.draft-main {
  flex: 1;
  min-width: 0;
}

.draft-title {
  font-size: 15px;
  font-weight: 700;
  color: #1F2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.draft-meta {
  margin-top: 6px;
  font-size: 12px;
  color: #6B7280;
  display: flex;
  align-items: center;
  gap: 5px;
  flex-wrap: wrap;
}

.draft-meta .price {
  color: #E1251B;
  font-weight: 700;
}

.draft-meta .muted {
  color: #B0B4BA;
}

.draft-meta .sep {
  color: #D9DCE1;
}

.draft-time {
  margin-top: 6px;
  font-size: 11px;
  color: #B0B4BA;
}

.draft-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

@media (max-width: 640px) {
  .draft-card {
    flex-wrap: wrap;
  }

  .draft-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
