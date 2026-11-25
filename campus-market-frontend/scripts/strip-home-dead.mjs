// 一次性脚本：删除 Home.vue 中模板已不存在的死代码（脚本与样式块）
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const dir = path.dirname(fileURLToPath(import.meta.url))
const file = path.join(dir, '..', 'src', 'views', 'Home.vue')
const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/)

// 1-based 闭区间，按倒序删除
const ranges = [
  // ---------- 样式块 ----------
  [4270, 4394], // 同地点交易榜弹窗（ld-filter-btn / ld-empty 保留）
  [4057, 4269], // 教材价格指数
  [3743, 3900], // 求购热力榜
  [3440, 3556], // 成交风向标
  [3343, 3439], // 校园估价器弹窗
  [3239, 3342], // 教材循环站
  [3142, 3238], // 校园行情总览
  [2992, 3141], // 校园捡漏榜
  [2808, 2991], // 校园生活助手
  // ---------- 脚本块 ----------
  [1471, 1474], // selectPickupLocation
  [1349, 1382], // 教材价格指数相关
  [1319, 1335], // 求购热力榜
  [1214, 1275], // 同地点交易榜 + 成交风向标 + 捡漏榜
  [1166, 1213], // 学科筛选 + 校园估价器
  [1069, 1119], // 校园生活助手 + 行情总览 + 教材循环站加载
]

const drop = new Set()
for (const [start, end] of ranges) {
  for (let i = start; i <= end; i++) drop.add(i)
}

const kept = lines.filter((_, idx) => !drop.has(idx + 1))
fs.writeFileSync(file, kept.join('\n'), 'utf8')
console.log(`删除 ${lines.length - kept.length} 行，剩余 ${kept.length} 行`)
