<template>
  <span class="smart-image" :class="{ 'is-loaded': loaded, 'is-failed': failed }" :style="ratio ? { aspectRatio: ratio } : null">
    <img
      v-if="!failed"
      :src="displaySrc"
      :alt="alt"
      :width="width"
      :height="height"
      loading="lazy"
      decoding="async"
      @load="onLoad"
      @error="onError"
    />
  </span>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { DEFAULT_IMG, resolveImage, thumbUrl } from '../utils/image'

const props = defineProps({
  src: { type: String, default: '' },
  alt: { type: String, default: '' },
  width: { type: [Number, String], default: undefined },
  height: { type: [Number, String], default: undefined },
  // 传 '1 / 1' 之类的比例可避免加载时布局抖动；容器已定高时无需传
  ratio: { type: String, default: '' },
  // 是否优先请求 320px 缩略图（列表/卡片场景）
  thumb: { type: Boolean, default: true }
})

const stage = ref('thumb')
const loaded = ref(false)
const failed = ref(false)

// 缩略图失败自动回退原图，原图也失败才显示占位图
const displaySrc = computed(() => {
  if (!props.src) return DEFAULT_IMG
  if (props.thumb && stage.value === 'thumb') return thumbUrl(props.src)
  return resolveImage(props.src)
})

const onLoad = () => {
  loaded.value = true
}

const onError = () => {
  if (props.thumb && stage.value === 'thumb') {
    stage.value = 'original'
    return
  }
  failed.value = true
}

watch(
  () => props.src,
  () => {
    stage.value = 'thumb'
    loaded.value = false
    failed.value = false
  }
)
</script>

<style scoped>
.smart-image {
  display: block;
  position: relative;
  overflow: hidden;
  background: #f5f6f8;
  background-image: linear-gradient(100deg, #f5f6f8 20%, #eceef2 40%, #f5f6f8 60%);
  background-size: 200% 100%;
  animation: smart-image-shimmer 1.2s ease-in-out infinite;
}

.smart-image.is-loaded,
.smart-image.is-failed {
  animation: none;
  background-image: none;
}

.smart-image.is-failed {
  background: #f5f6f8 url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0iI2Q4ZDhkOCI+PHJlY3QgeD0iMyIgeT0iMyIgd2lkdGg9IjE4IiBoZWlnaHQ9IjE4IiByeD0iMiIgcnk9IjIiPjwvcmVjdD48Y2lyY2xlIGN4PSI4LjUiIGN5PSI4LjUiIHI9IjEuNSI+PC9jaXJjbGU+PC9zdmc+') center / 40% no-repeat;
}

.smart-image img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 0.25s ease;
}

.smart-image.is-loaded img {
  opacity: 1;
}

@keyframes smart-image-shimmer {
  0% {
    background-position: 100% 0;
  }
  100% {
    background-position: -100% 0;
  }
}
</style>
