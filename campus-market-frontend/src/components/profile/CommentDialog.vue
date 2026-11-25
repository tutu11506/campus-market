<template>
  <el-dialog v-model="visible" title="评价订单" width="420px" :close-on-click-modal="false">
    <div v-if="order" class="comment-dialog">
      <div class="comment-dialog-goods">
        <SmartImage :src="order.goodsCover" class="comment-dialog-cover" alt="" />
        <div class="comment-dialog-goods-info">
          <div class="comment-dialog-title">{{ order.goodsTitle }}</div>
          <div class="comment-dialog-amount">&yen;{{ order.price }}</div>
        </div>
      </div>
      <div class="comment-dialog-field">
        <label>评分 <span class="req">*</span></label>
        <div class="rating-stars">
          <span
            v-for="s in 5"
            :key="s"
            class="rating-star"
            :class="{ active: s <= commentRating }"
            @click="commentRating = s"
          >★</span>
          <span class="rating-text">{{ ['', '很差', '较差', '一般', '满意', '非常满意'][commentRating] }}</span>
        </div>
      </div>
      <div class="comment-dialog-field">
        <label>评价内容 <span class="req">*</span></label>
        <el-input v-model="commentContent" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="分享一下这次交易的感受吧～" />
      </div>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="commentSubmitting" @click="submitComment">提交评价</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import SmartImage from '../SmartImage.vue'
import { addComment } from '../../api/comment'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  order: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'done'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const commentRating = ref(5)
const commentContent = ref('')
const commentSubmitting = ref(false)

watch(() => props.modelValue, (v) => {
  if (v) {
    commentRating.value = 5
    commentContent.value = ''
  }
})

const submitComment = async () => {
  if (!props.order) return
  if (!commentContent.value.trim()) {
    ElMessage.warning('请填写评价内容')
    return
  }
  commentSubmitting.value = true
  try {
    await addComment({
      orderId: props.order.id,
      rating: commentRating.value,
      content: commentContent.value.trim()
    })
    ElMessage.success('评价成功')
    visible.value = false
    emit('done')
  } catch (e) { /* 错误由拦截器提示 */ } finally {
    commentSubmitting.value = false
  }
}
</script>

<style scoped>
.comment-dialog {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.comment-dialog-goods {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 10px;
  background: #fafafa;
  border-radius: 8px;
}
.comment-dialog-cover {
  width: 56px;
  height: 56px;
  border-radius: 6px;
  object-fit: cover;
  background: #f0f0f0;
  flex-shrink: 0;
}
.comment-dialog-goods-info {
  flex: 1;
  min-width: 0;
}
.comment-dialog-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.comment-dialog-amount {
  font-size: 14px;
  color: #E1251B;
  font-weight: 600;
  margin-top: 4px;
}
.comment-dialog-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.comment-dialog-field > label {
  font-size: 13px;
  color: #333;
  font-weight: 500;
}
.comment-dialog-field .req {
  color: #E1251B;
}
.rating-stars {
  display: flex;
  align-items: center;
  gap: 4px;
}
.rating-star {
  font-size: 28px;
  cursor: pointer;
  color: #ddd;
  transition: color 0.15s;
  line-height: 1;
}
.rating-star.active {
  color: #ff9800;
}
.rating-text {
  font-size: 13px;
  color: #ff9800;
  margin-left: 8px;
  font-weight: 500;
}
</style>
