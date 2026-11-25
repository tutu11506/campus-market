<template>
  <el-dialog
    v-model="visible"
    title="编辑商品"
    width="560px"
    :close-on-click-modal="false"
    @closed="resetEditGoods"
  >
    <div class="edit-goods-body">
      <div class="edit-field">
        <label>宝贝标题 <span class="req">*</span></label>
        <el-input v-model="goodsEditForm.title" placeholder="请输入商品标题" maxlength="30" show-word-limit />
      </div>
      <div class="edit-field">
        <label>宝贝描述</label>
        <el-input v-model="goodsEditForm.description" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="品牌型号、成色、转让原因等" />
      </div>
      <div class="edit-two-col">
        <div class="edit-field">
          <label>售价 <span class="req">*</span></label>
          <el-input-number v-model="goodsEditForm.price" :min="0.01" :precision="2" :step="10" controls-position="right" style="width:100%" />
        </div>
        <div class="edit-field">
          <label>原价</label>
          <el-input-number v-model="goodsEditForm.originalPrice" :min="0" :precision="2" :step="10" controls-position="right" style="width:100%" />
        </div>
      </div>
      <div class="edit-two-col">
        <div class="edit-field">
          <label>分类 <span class="req">*</span></label>
          <el-select v-model="goodsEditForm.categoryId" placeholder="请选择" style="width:100%">
            <el-option v-for="c in editCategoryList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </div>
        <div class="edit-field">
          <label>交易地点 <span class="req">*</span></label>
          <el-input v-model="goodsEditForm.location" placeholder="如：南区食堂门口" />
        </div>
      </div>
      <div class="edit-field">
        <label>商品图片 <span class="req">*</span></label>
        <div class="edit-image-grid">
          <div
            v-for="(img, index) in editImageList"
            :key="'ei-' + index"
            class="edit-image-item has"
          >
            <img :src="img" alt="" />
            <div class="cover-badge" v-if="index === 0">封面</div>
            <span class="edit-image-del" @click="removeEditImage(index)">×</span>
          </div>
          <div
            v-if="editImageList.length < 9"
            class="edit-image-item add"
            @click="triggerEditUpload"
          >
            <el-icon :size="22"><Plus /></el-icon>
            <span>添加</span>
          </div>
          <input
            ref="editFileInput"
            type="file"
            accept="image/*"
            multiple
            style="display:none"
            @change="handleEditFileChange"
          />
        </div>
        <div class="edit-sub">首张为封面，最多 9 张</div>
      </div>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="editGoodsSubmitting" @click="submitEditGoods">保存修改</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadImage } from '../../api/upload'
import { updateGoods } from '../../api/goods'
import { getCategoryList } from '../../api/category'
import { baseURL as BASE_URL } from '../../config'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  goods: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'done'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const ALLOWED_IMG_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
const MAX_IMG_SIZE = 5 * 1024 * 1024

const editGoodsSubmitting = ref(false)
const editFileInput = ref(null)
const editCategoryList = ref([])
const editGoodsId = ref(null)
const goodsEditForm = reactive({
  title: '',
  description: '',
  price: 0.01,
  originalPrice: null,
  categoryId: null,
  location: ''
})
const editImageList = ref([])   // 预览用完整 URL
const editImagePaths = ref([])  // 存库用相对路径

watch(() => props.modelValue, async (v) => {
  if (!v || !props.goods) return
  const goods = props.goods
  editGoodsId.value = goods.id
  goodsEditForm.title = goods.title || ''
  goodsEditForm.description = goods.description || ''
  goodsEditForm.price = Number(goods.basePrice || goods.price) || 0.01
  goodsEditForm.originalPrice = goods.originalPrice != null ? Number(goods.originalPrice) : null
  goodsEditForm.categoryId = goods.categoryId
  goodsEditForm.location = goods.location || ''

  const raw = goods.images || goods.coverImage || ''
  const paths = raw.split(',').map(s => s.trim()).filter(Boolean)
  editImagePaths.value = paths.slice()
  editImageList.value = paths.map(p => p.startsWith('http') ? p : BASE_URL + p)

  if (!editCategoryList.value.length) {
    try {
      const res = await getCategoryList()
      editCategoryList.value = res || []
    } catch (e) { /* 忽略，弹窗已打开 */ }
  }
})

const resetEditGoods = () => {
  editGoodsId.value = null
  goodsEditForm.title = ''
  goodsEditForm.description = ''
  goodsEditForm.price = 0.01
  goodsEditForm.originalPrice = null
  goodsEditForm.categoryId = null
  goodsEditForm.location = ''
  editImageList.value = []
  editImagePaths.value = []
}

const triggerEditUpload = () => { editFileInput.value && editFileInput.value.click() }
const removeEditImage = (index) => {
  editImageList.value.splice(index, 1)
  editImagePaths.value.splice(index, 1)
}
const handleEditFileChange = async (e) => {
  const files = Array.from(e.target.files)
  const remain = 9 - editImageList.value.length
  if (remain <= 0) { ElMessage.warning('最多上传9张图片'); return }
  const toUpload = files.slice(0, remain)
  for (const file of toUpload) {
    if (!ALLOWED_IMG_TYPES.includes(file.type)) { ElMessage.warning(`${file.name} 格式不支持`); continue }
    if (file.size > MAX_IMG_SIZE) { ElMessage.warning(`${file.name} 超过 5MB`); continue }
    try {
      const res = await uploadImage(file)
      const relPath = typeof res === 'string' ? res : (res && res.url ? res.url : '')
      if (relPath) {
        editImagePaths.value.push(relPath)
        editImageList.value.push(relPath.startsWith('http') ? relPath : BASE_URL + relPath)
      }
    } catch (err) { ElMessage.error(`${file.name} 上传失败`) }
  }
  e.target.value = ''
}

const submitEditGoods = async () => {
  if (!goodsEditForm.title.trim()) { ElMessage.warning('请填写商品标题'); return }
  if (!goodsEditForm.price || Number(goodsEditForm.price) <= 0) { ElMessage.warning('价格必须大于 0'); return }
  if (!goodsEditForm.categoryId) { ElMessage.warning('请选择分类'); return }
  if (!goodsEditForm.location.trim()) { ElMessage.warning('请填写交易地点'); return }
  if (editImagePaths.value.length === 0) { ElMessage.warning('请至少上传一张图片'); return }

  editGoodsSubmitting.value = true
  try {
    await updateGoods({
      id: editGoodsId.value,
      title: goodsEditForm.title.trim(),
      description: goodsEditForm.description.trim(),
      price: Number(goodsEditForm.price),
      originalPrice: goodsEditForm.originalPrice ? Number(goodsEditForm.originalPrice) : null,
      images: editImagePaths.value.join(','),
      coverImage: editImagePaths.value[0],
      categoryId: goodsEditForm.categoryId,
      location: goodsEditForm.location.trim()
    })
    ElMessage.success('商品已更新')
    visible.value = false
    emit('done')
  } catch (e) { /* interceptor 已处理 */ }
  finally { editGoodsSubmitting.value = false }
}
</script>

<style scoped>
.edit-goods-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 0 4px;
}
.edit-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.edit-field label {
  font-size: 13px;
  font-weight: 500;
  color: #666;
}
.edit-field .req {
  color: #E1251B;
}
.edit-two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.edit-image-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}
.edit-image-item {
  position: relative;
  aspect-ratio: 1;
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
}
.edit-image-item.has img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.edit-image-item.add {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border: 1px dashed #ddd;
  color: #bbb;
  font-size: 11px;
  cursor: pointer;
}
.edit-image-item.add:hover {
  border-color: #E1251B;
  color: #E1251B;
}
.cover-badge {
  position: absolute;
  left: 4px;
  bottom: 4px;
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 10px;
}
.edit-image-del {
  position: absolute;
  top: 3px;
  right: 3px;
  width: 18px;
  height: 18px;
  line-height: 16px;
  text-align: center;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}
.edit-sub {
  font-size: 11px;
  color: #999;
}
</style>
