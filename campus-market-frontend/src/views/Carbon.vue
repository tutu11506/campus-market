<template>
  <ServiceShell title="碳足迹" subtitle="每笔二手交易，都在为地球减负" badge="碳">
    <section class="hero">
      <div class="hero-left">
        <div class="hero-label">我的累计环保贡献</div>
        <div class="hero-value">
          {{ formatKg(summary.totalKg) }}<span>kg CO₂e</span>
        </div>
        <div class="hero-sub">来自 {{ summary.totalOrders || 0 }} 笔已完成的二手交易</div>
      </div>
      <div class="hero-right">
        <el-select v-model="year" size="small" class="year-select" @change="load">
          <el-option v-for="y in years" :key="y" :label="`${y} 年`" :value="y" />
        </el-select>
        <el-button type="primary" @click="openPoster">生成年度环保海报</el-button>
      </div>
    </section>

    <div class="stat-grid" v-loading="loading">
      <div class="stat-card">
        <div class="stat-label">{{ summary.year }} 年减碳</div>
        <div class="stat-value">{{ formatKg(summary.yearKg) }}<i>kg</i></div>
        <div class="stat-tip">{{ summary.yearOrders || 0 }} 笔交易</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">等效种树</div>
        <div class="stat-value">{{ formatKg(summary.trees) }}<i>棵</i></div>
        <div class="stat-tip">按每棵树每年吸收 18.3kg 计</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">少开燃油车</div>
        <div class="stat-value">{{ summary.carKm || 0 }}<i>km</i></div>
        <div class="stat-tip">按每公里 0.17kg 排放计</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">少给手机充电</div>
        <div class="stat-value">{{ summary.phoneCharges || 0 }}<i>次</i></div>
        <div class="stat-tip">按每次 0.012kg 排放计</div>
      </div>
    </div>

    <section class="panel">
      <div class="panel-head">
        <div class="panel-title">{{ summary.year }} 年每月减碳</div>
        <div class="panel-note">买家、卖家两侧的已完成订单都计入</div>
      </div>
      <div class="bars" v-if="hasData">
        <div v-for="m in months" :key="m.month" class="bar-col" :title="`${m.label}：${m.kg}kg · ${m.orders} 笔`">
          <span class="bar-kg" v-if="m.kg > 0">{{ m.kg }}</span>
          <div class="bar-track">
            <div class="bar-fill" :style="{ height: barHeight(m.kg) }"></div>
          </div>
          <span class="bar-label">{{ m.month }}</span>
        </div>
      </div>
      <el-empty v-else description="这一年还没有已完成的二手交易，去逛逛闲置吧" :image-size="80" />
    </section>

    <section class="panel" v-if="categories.length">
      <div class="panel-head">
        <div class="panel-title">减碳来源分类</div>
        <div class="panel-note">按品类估算，占比以当年减碳量为基准</div>
      </div>
      <div class="cat-list">
        <div v-for="(c, idx) in categories" :key="c.name" class="cat-row">
          <div class="cat-name">
            <span class="cat-dot" :style="{ background: colors[idx % colors.length] }"></span>
            {{ c.name }}
          </div>
          <div class="cat-bar">
            <span :style="{ width: (c.sharePct || 0) + '%', background: colors[idx % colors.length] }"></span>
          </div>
          <div class="cat-num">{{ c.sharePct }}%</div>
          <div class="cat-kg">{{ c.kg }}kg · {{ c.orders }} 笔</div>
        </div>
      </div>
    </section>

    <div class="note">{{ note }}</div>

    <el-dialog v-model="posterVisible" title="年度环保海报" width="420px">
      <div class="poster-wrap">
        <canvas ref="canvasRef" width="750" height="1000"></canvas>
      </div>
      <div class="poster-tip">长按或下载图片，分享到朋友圈 / 班级群</div>
      <template #footer>
        <el-button @click="posterVisible = false">关闭</el-button>
        <el-button type="primary" @click="downloadPoster">下载海报</el-button>
      </template>
    </el-dialog>
  </ServiceShell>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import ServiceShell from '../components/ServiceShell.vue'
import { getCarbonStats } from '../api/carbon'

const colors = ['#2FAE7A', '#4C9AFF', '#F5A623', '#E1251B', '#8E7BEF', '#27C1C1']

const loading = ref(false)
const year = ref(new Date().getFullYear())
const years = ref([new Date().getFullYear()])
const summary = ref({ year: new Date().getFullYear() })
const months = ref([])
const categories = ref([])
const note = ref('')
const hasData = ref(false)

const posterVisible = ref(false)
const canvasRef = ref(null)

const username = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || '{}').username || '同学'
  } catch (e) {
    return '同学'
  }
})

const maxKg = computed(() => Math.max(1, ...months.value.map(m => Number(m.kg) || 0)))

const formatKg = (v) => Number(v || 0).toFixed(1)

const barHeight = (kg) => {
  const ratio = (Number(kg) || 0) / maxKg.value
  return ratio > 0 ? Math.max(6, ratio * 100) + '%' : '0%'
}

const load = async () => {
  loading.value = true
  try {
    const res = await getCarbonStats(year.value)
    summary.value = res?.summary || {}
    months.value = Array.isArray(res?.months) ? res.months : []
    categories.value = Array.isArray(res?.categories) ? res.categories : []
    const ys = Array.isArray(res?.years) && res.years.length ? res.years : [new Date().getFullYear()]
    years.value = ys
    year.value = res?.summary?.year || year.value
    note.value = res?.note || ''
    hasData.value = !!res?.hasData
  } catch (e) {
    months.value = []
    categories.value = []
    hasData.value = false
  } finally {
    loading.value = false
  }
}

const openPoster = async () => {
  posterVisible.value = true
  await nextTick()
  drawPoster()
}

const roundRect = (ctx, x, y, w, h, r) => {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.arcTo(x + w, y, x + w, y + h, r)
  ctx.arcTo(x + w, y + h, x, y + h, r)
  ctx.arcTo(x, y + h, x, y, r)
  ctx.arcTo(x, y, x + w, y, r)
  ctx.closePath()
}

const fitText = (text, max) => {
  const s = String(text || '')
  return s.length > max ? s.slice(0, max) + '…' : s
}

const drawPoster = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const W = 750
  const H = 1000
  const s = summary.value || {}
  const font = '"PingFang SC", "Microsoft YaHei", "Helvetica Neue", sans-serif'

  ctx.clearRect(0, 0, W, H)

  // 背景：深绿渐变 + 柔光圆
  const bg = ctx.createLinearGradient(0, 0, 0, H)
  bg.addColorStop(0, '#0E3B2C')
  bg.addColorStop(0.55, '#156B4B')
  bg.addColorStop(1, '#0A2A20')
  ctx.fillStyle = bg
  ctx.fillRect(0, 0, W, H)

  ctx.globalAlpha = 0.12
  ctx.fillStyle = '#8CF3BE'
  ctx.beginPath()
  ctx.arc(660, 110, 150, 0, Math.PI * 2)
  ctx.fill()
  ctx.beginPath()
  ctx.arc(70, 880, 190, 0, Math.PI * 2)
  ctx.fill()
  ctx.globalAlpha = 1

  // 顶部小标题
  ctx.fillStyle = 'rgba(255,255,255,0.72)'
  ctx.font = `600 20px ${font}`
  ctx.fillText('CAMPUS SECOND-HAND · CARBON FOOTPRINT', 80, 88)

  // 年份
  ctx.fillStyle = '#FFFFFF'
  ctx.font = `800 96px ${font}`
  ctx.fillText(String(s.year || year.value), 70, 196)
  ctx.fillStyle = 'rgba(255,255,255,0.85)'
  ctx.font = `700 32px ${font}`
  ctx.fillText('年度环保海报', 76, 246)

  // 用户名
  ctx.fillStyle = '#8CF3BE'
  ctx.font = `600 26px ${font}`
  ctx.fillText('@' + fitText(username.value, 16), 78, 298)

  // 分割线
  ctx.strokeStyle = 'rgba(255,255,255,0.22)'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.moveTo(80, 336)
  ctx.lineTo(670, 336)
  ctx.stroke()

  // 年度数据
  ctx.fillStyle = 'rgba(255,255,255,0.86)'
  ctx.font = `500 25px ${font}`
  ctx.fillText('这一年，我通过二手交易共减少碳排放', 80, 402)

  const big = formatKg(s.yearKg)
  ctx.fillStyle = '#8CF3BE'
  ctx.font = `800 96px ${font}`
  ctx.fillText(big, 74, 508)
  const bigWidth = ctx.measureText(big).width
  ctx.fillStyle = 'rgba(255,255,255,0.8)'
  ctx.font = `600 28px ${font}`
  ctx.fillText('kg CO₂e', 84 + bigWidth, 506)

  ctx.fillStyle = 'rgba(255,255,255,0.7)'
  ctx.font = `400 24px ${font}`
  ctx.fillText(`累计 ${formatKg(s.totalKg)} kg CO₂e · ${s.totalOrders || 0} 笔交易`, 80, 562)

  // 三个等效数据卡片
  const boxes = [
    { value: formatKg(s.trees), unit: '棵', label: '等效种树' },
    { value: String(s.carKm || 0), unit: 'km', label: '少开燃油车' },
    { value: String(s.phoneCharges || 0), unit: '次', label: '少给手机充电' }
  ]
  const boxW = 178
  const boxH = 122
  const gap = 30
  boxes.forEach((b, i) => {
    const x = 80 + i * (boxW + gap)
    const y = 612
    ctx.fillStyle = 'rgba(255,255,255,0.08)'
    roundRect(ctx, x, y, boxW, boxH, 16)
    ctx.fill()
    ctx.strokeStyle = 'rgba(255,255,255,0.16)'
    ctx.lineWidth = 1.5
    roundRect(ctx, x, y, boxW, boxH, 16)
    ctx.stroke()

    ctx.fillStyle = '#FFFFFF'
    ctx.font = `700 34px ${font}`
    ctx.fillText(b.value, x + 18, y + 56)
    const vw = ctx.measureText(b.value).width
    ctx.fillStyle = 'rgba(255,255,255,0.65)'
    ctx.font = `500 19px ${font}`
    ctx.fillText(b.unit, x + 22 + vw, y + 55)

    ctx.fillStyle = 'rgba(255,255,255,0.66)'
    ctx.font = `400 20px ${font}`
    ctx.fillText(b.label, x + 18, y + 94)
  })

  // Top3 分类
  ctx.fillStyle = 'rgba(255,255,255,0.86)'
  ctx.font = `600 24px ${font}`
  ctx.fillText('减碳主要来自', 80, 800)

  const top = categories.value.filter(c => Number(c.kg) > 0).slice(0, 3)
  if (top.length) {
    top.forEach((c, i) => {
      const y = 826 + i * 40
      ctx.fillStyle = 'rgba(255,255,255,0.82)'
      ctx.font = `500 22px ${font}`
      ctx.fillText(c.name, 80, y + 20)
      ctx.fillStyle = 'rgba(255,255,255,0.06)'
      roundRect(ctx, 210, y + 6, 320, 14, 7)
      ctx.fill()
      ctx.fillStyle = colors[i % colors.length]
      const w = Math.max(8, 320 * (Number(c.sharePct) || 0) / 100)
      roundRect(ctx, 210, y + 6, w, 14, 7)
      ctx.fill()
      ctx.fillStyle = '#8CF3BE'
      ctx.font = `600 22px ${font}`
      ctx.textAlign = 'right'
      ctx.fillText(`${c.kg}kg`, 670, y + 20)
      ctx.textAlign = 'left'
    })
  } else {
    ctx.fillStyle = 'rgba(255,255,255,0.6)'
    ctx.font = `400 22px ${font}`
    ctx.fillText('从下一件闲置开始，记录你的绿色足迹', 80, 850)
  }

  // 口号与落款
  ctx.fillStyle = '#FFFFFF'
  ctx.font = `700 30px ${font}`
  ctx.fillText(top.length ? '闲置不废，地球少碳一点。' : '让闲置继续发光。', 80, 946)

  ctx.fillStyle = 'rgba(255,255,255,0.5)'
  ctx.font = `400 20px ${font}`
  ctx.fillText('校园二手 · 让闲置继续发光', 80, 978)
}

const downloadPoster = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  const link = document.createElement('a')
  link.download = `校园碳足迹-${summary.value.year || year.value}-${username.value}.png`
  link.href = canvas.toDataURL('image/png')
  link.click()
}

onMounted(load)
</script>

<style scoped>
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 22px 24px;
  margin-bottom: 14px;
  border-radius: 14px;
  color: #fff;
  background: linear-gradient(135deg, #17694C 0%, #0F4C37 55%, #0B3427 100%);
  box-shadow: 0 10px 26px rgba(15, 76, 55, 0.22);
}

.hero-label {
  font-size: 13px;
  opacity: 0.78;
}

.hero-value {
  margin-top: 6px;
  font-size: 40px;
  font-weight: 800;
  letter-spacing: 0.5px;
  color: #8CF3BE;
  line-height: 1.1;
}

.hero-value span {
  margin-left: 8px;
  font-size: 15px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.78);
}

.hero-sub {
  margin-top: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.62);
}

.hero-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.year-select {
  width: 110px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 14px;
}

.stat-card {
  padding: 16px 18px;
  background: #fff;
  border: 1px solid #EDEEF0;
  border-radius: 12px;
}

.stat-label {
  font-size: 12px;
  color: #8A8F99;
}

.stat-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 800;
  color: #17694C;
  line-height: 1.1;
}

.stat-value i {
  margin-left: 4px;
  font-size: 13px;
  font-style: normal;
  font-weight: 500;
  color: #A5AAB3;
}

.stat-tip {
  margin-top: 6px;
  font-size: 11px;
  color: #B0B4BA;
}

.panel {
  background: #fff;
  border: 1px solid #EDEEF0;
  border-radius: 12px;
  margin-bottom: 14px;
  overflow: hidden;
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

.panel-note {
  font-size: 12px;
  color: #A5AAB3;
}

.bars {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  height: 210px;
  padding: 18px 18px 14px;
}

.bar-col {
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  min-width: 0;
}

.bar-kg {
  font-size: 10px;
  color: #8A8F99;
  white-space: nowrap;
}

.bar-track {
  flex: 1;
  width: 100%;
  max-width: 34px;
  display: flex;
  align-items: flex-end;
  background: #F6F7F9;
  border-radius: 6px;
  overflow: hidden;
}

.bar-fill {
  width: 100%;
  border-radius: 6px 6px 0 0;
  background: linear-gradient(180deg, #4BD69B 0%, #17694C 100%);
  transition: height 0.4s ease;
}

.bar-label {
  font-size: 11px;
  color: #A5AAB3;
}

.cat-list {
  padding: 8px 18px 16px;
}

.cat-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed #F2F3F5;
}

.cat-row:last-child {
  border-bottom: none;
}

.cat-name {
  width: 92px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
  font-weight: 600;
  color: #1F2937;
}

.cat-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.cat-bar {
  flex: 1;
  height: 8px;
  background: #F6F7F9;
  border-radius: 4px;
  overflow: hidden;
}

.cat-bar span {
  display: block;
  height: 100%;
  border-radius: 4px;
}

.cat-num {
  width: 52px;
  text-align: right;
  font-size: 12px;
  font-weight: 700;
  color: #17694C;
}

.cat-kg {
  width: 110px;
  text-align: right;
  font-size: 12px;
  color: #A5AAB3;
}

.note {
  font-size: 12px;
  color: #A5AAB3;
  line-height: 1.6;
  padding: 0 4px 6px;
}

.poster-wrap {
  display: flex;
  justify-content: center;
}

.poster-wrap canvas {
  width: 100%;
  max-width: 340px;
  height: auto;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(15, 76, 55, 0.24);
}

.poster-tip {
  margin-top: 10px;
  text-align: center;
  font-size: 12px;
  color: #A5AAB3;
}

@media (max-width: 900px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 560px) {
  .hero-value {
    font-size: 32px;
  }

  .bars {
    gap: 3px;
    padding: 14px 10px 10px;
  }

  .bar-kg {
    display: none;
  }

  .cat-kg {
    display: none;
  }
}
</style>
