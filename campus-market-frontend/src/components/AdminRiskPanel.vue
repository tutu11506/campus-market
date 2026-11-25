<template>
  <div class="risk-panel">
    <!-- 风控总览 -->
    <div class="risk-cards">
      <div class="risk-card" :class="{ active: sub === 'moderation' }" @click="switchSub('moderation')">
        <div class="rc-num">{{ modStats.pendingReview || 0 }}</div>
        <div class="rc-label">待复核违禁品</div>
        <div class="rc-sub">累计识别 {{ modStats.total || 0 }} 次</div>
      </div>
      <div class="risk-card" :class="{ active: sub === 'price' }" @click="switchSub('price')">
        <div class="rc-num">{{ priceStats.open || 0 }}</div>
        <div class="rc-label">待处理价格预警</div>
        <div class="rc-sub">已打标商品 {{ priceStats.alertGoods || 0 }} 件</div>
      </div>
      <div class="risk-card" :class="{ active: sub === 'dispute' }" @click="switchSub('dispute')">
        <div class="rc-num">{{ disputeStats.pending || 0 }}</div>
        <div class="rc-label">进行中纠纷</div>
        <div class="rc-sub">累计仲裁 {{ disputeStats.total || 0 }} 起</div>
      </div>
      <div class="risk-card" :class="{ active: sub === 'blacklist' }" @click="switchSub('blacklist')">
        <div class="rc-num">{{ blackStats.active || 0 }}</div>
        <div class="rc-label">生效黑名单</div>
        <div class="rc-sub">全平台失信提醒</div>
      </div>
    </div>

    <!-- 子页签 -->
    <div class="risk-subtabs">
      <div
        v-for="t in tabs"
        :key="t.key"
        class="risk-subtab"
        :class="{ active: sub === t.key }"
        @click="switchSub(t.key)"
      >{{ t.label }}</div>
    </div>

    <!-- ==================== 举报处理 ==================== -->
    <div v-show="sub === 'reports'" class="risk-block">
      <div class="risk-toolbar">
        <span class="toolbar-title">用户举报</span>
        <span class="toolbar-note">处理后可一键将该用户加入平台黑名单</span>
      </div>
      <el-table :data="reportList" stripe v-loading="reportLoading" class="risk-table" empty-text="暂无举报">
        <el-table-column label="举报人" width="120">
          <template #default="{ row }">{{ pick(row, 'reporterName', 'reporter_name') || '—' }}</template>
        </el-table-column>
        <el-table-column label="目标" width="150">
          <template #default="{ row }">
            {{ targetTypeText(pick(row, 'targetType', 'target_type')) }} #{{ pick(row, 'targetId', 'target_id') }}
          </template>
        </el-table-column>
        <el-table-column label="原因" width="130" prop="reason" show-overflow-tooltip />
        <el-table-column label="详细说明" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ pick(row, 'detail') || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="96" align="center">
          <template #default="{ row }">
            <el-tag :type="reportStatusType(pick(row, 'status'))" size="small" effect="light">
              {{ reportStatusText(pick(row, 'status')) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="150">
          <template #default="{ row }">{{ formatTime(pick(row, 'createTime', 'create_time')) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" align="center">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="handleReportRow(row, 'handled')">已处理</el-button>
            <el-button size="small" link type="danger" @click="handleReportRow(row, 'rejected')">驳回</el-button>
            <el-button size="small" link @click="openBlacklistForReport(row)">拉黑用户</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="risk-pagination" v-if="reportTotal > 0">
        <el-pagination background layout="total, prev, pager, next" :total="reportTotal"
          :current-page="reportPageNum" :page-size="reportPageSize" @current-change="onReportPage" />
      </div>
    </div>

    <!-- ==================== 违禁品识别 ==================== -->
    <div v-show="sub === 'moderation'" class="risk-block">
      <div class="risk-toolbar">
        <div class="toolbar-left">
          <span class="toolbar-title">违禁品识别记录</span>
          <el-select v-model="moderationVerdict" placeholder="全部判定" clearable size="small" style="width: 130px" @change="loadModeration(1)">
            <el-option label="直接拦截" value="block" />
            <el-option label="转人工复核" value="review" />
            <el-option label="通过" value="pass" />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-button size="small" @click="toggleWordLib">{{ showWordLib ? '收起词库' : '查看违禁品词库' }}</el-button>
          <el-button size="small" @click="refreshWords">刷新词库缓存</el-button>
        </div>
      </div>
      <el-table :data="moderationList" stripe v-loading="moderationLoading" class="risk-table" empty-text="暂无识别记录">
        <el-table-column label="判定" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="verdictType(row.verdict)" size="small" effect="dark">{{ verdictText(row.verdict) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="命中类别" width="150">
          <template #default="{ row }">{{ categoryText(row.riskCategory) }}</template>
        </el-table-column>
        <el-table-column label="命中标签" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.labels || '—' }}</template>
        </el-table-column>
        <el-table-column label="置信度" width="90" align="center">
          <template #default="{ row }">{{ row.confidence != null ? row.confidence + '%' : '—' }}</template>
        </el-table-column>
        <el-table-column label="上传者" width="110">
          <template #default="{ row }">{{ row.uploaderName || '—' }}</template>
        </el-table-column>
        <el-table-column label="关联商品" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.goodsTitle || '（已拦截，未发布）' }}</template>
        </el-table-column>
        <el-table-column label="处理状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'auto' ? 'warning' : 'info'" size="small" effect="plain">
              {{ recordStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="人工复核" width="150" align="center">
          <template #default="{ row }">
            <template v-if="row.status === 'auto' && row.verdict === 'review'">
              <el-button size="small" link type="danger" @click="confirmRecord(row, 'confirm')">确认拦截</el-button>
              <el-button size="small" link type="success" @click="confirmRecord(row, 'release')">放行</el-button>
            </template>
            <span v-else class="cell-muted">—</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="risk-pagination" v-if="moderationTotal > 0">
        <el-pagination background layout="total, prev, pager, next" :total="moderationTotal"
          :current-page="moderationPageNum" :page-size="moderationPageSize" @current-change="onModerationPage" />
      </div>

      <div v-if="showWordLib" class="word-lib">
        <div class="word-lib-title">违禁品词库（命中即自动识别）</div>
        <el-table :data="wordItems" size="small" stripe class="risk-table">
          <el-table-column prop="label" label="标签" width="150" />
          <el-table-column prop="category" label="类别" width="140" />
          <el-table-column prop="keywords" label="关键词" min-width="300" show-overflow-tooltip />
          <el-table-column label="处置" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.riskLevel === 'block' ? 'danger' : 'warning'" size="small">
                {{ row.riskLevel === 'block' ? '拦截' : '复核' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- ==================== 价格预警 ==================== -->
    <div v-show="sub === 'price'" class="risk-block">
      <div class="risk-toolbar">
        <div class="toolbar-left">
          <span class="toolbar-title">价格异常预警</span>
          <el-select v-model="priceStatus" placeholder="全部状态" clearable size="small" style="width: 130px" @change="loadPriceAlerts(1)">
            <el-option label="待处理" value="open" />
            <el-option label="已处理" value="handled" />
            <el-option label="已忽略" value="dismissed" />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-button size="small" type="primary" :loading="rescanning" @click="doRescan">立即巡检在售商品</el-button>
        </div>
      </div>
      <el-table :data="priceList" stripe v-loading="priceLoading" class="risk-table" empty-text="暂无价格预警">
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div class="goods-cell">
              <img v-if="row.goodsCover" :src="imgUrl(row.goodsCover)" class="goods-cell-img" alt="" />
              <div>
                <div class="goods-cell-title">{{ row.title }}</div>
                <div class="cell-muted">卖家：{{ row.sellerName || '—' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="售价 / 均价" width="170">
          <template #default="{ row }">
            <span class="price-now">¥{{ row.price }}</span>
            <span class="cell-muted"> / 均价 ¥{{ row.avgPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column label="溢价" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.level === 'serious' ? 'danger' : 'warning'" size="small">
              +{{ ratioPercent(row.ratio) }}%
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="样本" width="80" align="center">
          <template #default="{ row }">{{ row.sampleSize }} 件</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'open' ? 'danger' : 'info'" size="small" effect="plain">
              {{ priceStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预警说明" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.note || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <template v-if="row.status === 'open'">
              <el-button size="small" link type="primary" @click="handlePrice(row, 'handled')">已提醒</el-button>
              <el-button size="small" link @click="handlePrice(row, 'dismissed')">忽略</el-button>
            </template>
            <span v-else class="cell-muted">—</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="risk-pagination" v-if="priceTotal > 0">
        <el-pagination background layout="total, prev, pager, next" :total="priceTotal"
          :current-page="pricePageNum" :page-size="pricePageSize" @current-change="onPricePage" />
      </div>
    </div>

    <!-- ==================== 纠纷仲裁 ==================== -->
    <div v-show="sub === 'dispute'" class="risk-block">
      <div class="risk-toolbar">
        <div class="toolbar-left">
          <span class="toolbar-title">交易纠纷仲裁</span>
          <el-select v-model="disputeStatus" placeholder="全部状态" clearable size="small" style="width: 130px" @change="loadDisputes(1)">
            <el-option label="待受理" value="open" />
            <el-option label="处理中" value="processing" />
            <el-option label="已结案" value="resolved" />
          </el-select>
        </div>
        <span class="toolbar-note">发起仲裁时担保资金已自动冻结，裁决后按结果退款/结算</span>
      </div>
      <el-table :data="disputeList" stripe v-loading="disputeLoading" class="risk-table" empty-text="暂无纠纷">
        <el-table-column label="编号" width="80" prop="id" />
        <el-table-column label="商品 / 订单" min-width="200">
          <template #default="{ row }">
            <div class="goods-cell-title">{{ row.goodsTitle || '—' }}</div>
            <div class="cell-muted">{{ row.orderNo }}</div>
          </template>
        </el-table-column>
        <el-table-column label="发起方" width="150">
          <template #default="{ row }">
            {{ row.initiatorName || '—' }}
            <div class="cell-muted">{{ row.initiatorRole === 'buyer' ? '买家' : '卖家' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="对方" width="110">
          <template #default="{ row }">{{ row.respondentName || '—' }}</template>
        </el-table-column>
        <el-table-column label="纠纷类型" width="130">
          <template #default="{ row }">{{ reasonText(row.reasonType) }}</template>
        </el-table-column>
        <el-table-column label="证据" width="80" align="center">
          <template #default="{ row }">{{ row.evidenceCount || 0 }} 条</template>
        </el-table-column>
        <el-table-column label="担保资金" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.escrowFrozen === 1 ? 'danger' : 'info'" size="small" effect="plain">
              {{ row.escrowFrozen === 1 ? '已冻结' : '未冻结' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="disputeStatusType(row.status)" size="small">{{ disputeStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openDisputeDetail(row.id)">
              {{ row.status === 'resolved' ? '查看' : '仲裁' }}
            </el-button>
            <el-button v-if="row.status === 'open'" size="small" link @click="takeDispute(row)">介入</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="risk-pagination" v-if="disputeTotal > 0">
        <el-pagination background layout="total, prev, pager, next" :total="disputeTotal"
          :current-page="disputePageNum" :page-size="disputePageSize" @current-change="onDisputePage" />
      </div>
    </div>

    <!-- ==================== 平台黑名单 ==================== -->
    <div v-show="sub === 'blacklist'" class="risk-block">
      <div class="risk-toolbar">
        <div class="toolbar-left">
          <span class="toolbar-title">平台共享黑名单</span>
          <el-select v-model="blackStatus" placeholder="全部状态" clearable size="small" style="width: 130px" @change="loadBlacklist(1)">
            <el-option label="生效中" value="active" />
            <el-option label="已解除" value="revoked" />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-button size="small" type="danger" @click="openMarkBlacklist()">标记失信用户</el-button>
        </div>
      </div>
      <el-table :data="blackList" stripe v-loading="blackLoading" class="risk-table" empty-text="暂无黑名单记录">
        <el-table-column label="用户" min-width="160">
          <template #default="{ row }">
            <div class="user-cell">
              <img :src="avatarUrl(row.userAvatar)" class="user-cell-avatar" alt="" />
              <div>
                <div class="goods-cell-title">{{ row.userName || '—' }}</div>
                <div class="cell-muted">{{ row.studentNo || '未填写学号' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="信用分" width="90" align="center">
          <template #default="{ row }">{{ row.credit != null ? row.credit : '—' }}</template>
        </el-table-column>
        <el-table-column label="失信等级" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="levelType(row.level)" size="small" effect="dark">{{ levelText(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="失信原因" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reason }}</template>
        </el-table-column>
        <el-table-column label="关联举报" width="100" align="center">
          <template #default="{ row }">{{ row.reportCount || 0 }} 次</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'danger' : 'info'" size="small" effect="plain">
              {{ row.status === 'active' ? '生效中' : '已解除' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标记时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status === 'active'" size="small" link type="danger" @click="revokeRow(row)">解除</el-button>
            <span v-else class="cell-muted">—</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="risk-pagination" v-if="blackTotal > 0">
        <el-pagination background layout="total, prev, pager, next" :total="blackTotal"
          :current-page="blackPageNum" :page-size="blackPageSize" @current-change="onBlackPage" />
      </div>
    </div>

    <!-- 纠纷详情 / 裁决弹窗 -->
    <el-dialog v-model="disputeDialogVisible" title="纠纷仲裁" width="92%" style="max-width: 720px" :close-on-click-modal="false">
      <div v-if="disputeDetail.dispute" class="dispute-detail">
        <div class="dd-head">
          <span class="dd-title">{{ disputeDetail.dispute.goodsTitle }}</span>
          <el-tag :type="disputeStatusType(disputeDetail.dispute.status)" size="small">
            {{ disputeStatusText(disputeDetail.dispute.status) }}
          </el-tag>
        </div>
        <div class="dd-grid">
          <div>订单号：{{ disputeDetail.dispute.orderNo }}</div>
          <div>订单金额：¥{{ disputeDetail.dispute.orderPrice }}</div>
          <div>发起方：{{ disputeDetail.dispute.initiatorName }}（{{ disputeDetail.dispute.initiatorRole === 'buyer' ? '买家' : '卖家' }}）</div>
          <div>对方：{{ disputeDetail.dispute.respondentName }}</div>
          <div>纠纷类型：{{ reasonText(disputeDetail.dispute.reasonType) }}</div>
          <div>诉求金额：{{ disputeDetail.dispute.claimAmount != null ? '¥' + disputeDetail.dispute.claimAmount : '未填写' }}</div>
        </div>
        <div class="dd-section">
          <div class="dd-section-title">纠纷说明</div>
          <div class="dd-text">{{ disputeDetail.dispute.description }}</div>
        </div>
        <div class="dd-section">
          <div class="dd-section-title">证据（{{ (disputeDetail.evidence || []).length }}）</div>
          <div v-if="(disputeDetail.evidence || []).length === 0" class="cell-muted">双方暂未提交证据</div>
          <div v-for="e in disputeDetail.evidence" :key="e.id" class="evidence-item">
            <div class="evidence-head">
              <el-tag size="small" effect="plain">{{ evidenceTypeText(e.type) }}</el-tag>
              <span class="cell-muted">{{ e.uploaderName }} · {{ formatTime(e.createTime) }}</span>
            </div>
            <div v-if="e.content" class="dd-text">{{ e.content }}</div>
            <img v-if="e.fileUrl" :src="imgUrl(e.fileUrl)" class="evidence-img" alt="" />
          </div>
        </div>
        <div v-if="disputeDetail.dispute.status !== 'resolved'" class="dd-section">
          <div class="dd-section-title">裁决</div>
          <el-select v-model="verdictForm.verdict" placeholder="请选择裁决结果" style="width: 100%; margin-bottom: 10px">
            <el-option label="买家胜诉：担保资金退回买家，卖家信用-10" value="buyer_win" />
            <el-option label="卖家胜诉：担保资金结算给卖家" value="seller_win" />
            <el-option label="责任共担：资金退回买家" value="split" />
            <el-option label="证据不足：维持原交易" value="no_fault" />
          </el-select>
          <el-input v-model="verdictForm.note" type="textarea" :rows="3" maxlength="300" show-word-limit
            placeholder="裁决说明（会同步通知买卖双方）" />
        </div>
        <div v-else class="dd-section">
          <div class="dd-section-title">裁决结果</div>
          <div class="dd-text">{{ verdictResultText(disputeDetail.dispute.verdict) }}</div>
          <div class="dd-text" v-if="disputeDetail.dispute.verdictNote">{{ disputeDetail.dispute.verdictNote }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="disputeDialogVisible = false">关闭</el-button>
        <el-button
          v-if="disputeDetail.dispute && disputeDetail.dispute.status !== 'resolved'"
          type="danger"
          :loading="resolving"
          @click="submitVerdict"
        >提交裁决</el-button>
      </template>
    </el-dialog>

    <!-- 标记黑名单弹窗 -->
    <el-dialog v-model="blackDialogVisible" title="标记平台失信用户" width="92%" style="max-width: 480px" :close-on-click-modal="false">
      <div class="mark-form">
        <div class="mark-field">
          <label>用户 ID <span class="req">*</span></label>
          <el-input v-model="markForm.userId" placeholder="请输入被标记用户的 ID" />
        </div>
        <div class="mark-field">
          <label>失信等级</label>
          <el-select v-model="markForm.level" style="width: 100%">
            <el-option label="重点关注（watch）" value="watch" />
            <el-option label="失信警告（warning）" value="warning" />
            <el-option label="严重失信（serious）" value="serious" />
          </el-select>
        </div>
        <div class="mark-field">
          <label>失信原因 <span class="req">*</span></label>
          <el-input v-model="markForm.reason" type="textarea" :rows="2" maxlength="300" show-word-limit
            placeholder="如：多次约定面交放鸽子、收款后不发货" />
        </div>
        <div class="mark-field">
          <label>证据摘要</label>
          <el-input v-model="markForm.evidence" type="textarea" :rows="2" maxlength="500" show-word-limit
            placeholder="聊天记录/举报单号等（选填）" />
        </div>
      </div>
      <template #footer>
        <el-button @click="blackDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="marking" @click="submitMark">确认标记</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { baseURL } from '../config'
import { DEFAULT_AVATAR } from '../utils/avatar'
import {
  getReportList, handleReport,
  getModerationList, confirmModeration, getModerationStats, getProhibitedItems, refreshProhibitedWords,
  getPriceAlertList, handlePriceAlert, rescanPrice, getPriceAlertStats,
  getAdminDisputeList, getAdminDisputeDetail, startDisputeProcessing, resolveDispute, getDisputeStats,
  getBlacklistPage, markBlacklist, revokeBlacklist, getBlacklistStats
} from '../api/risk'

const tabs = [
  { key: 'reports', label: '举报处理' },
  { key: 'moderation', label: '违禁品识别' },
  { key: 'price', label: '价格预警' },
  { key: 'dispute', label: '纠纷仲裁' },
  { key: 'blacklist', label: '黑名单' }
]
const sub = ref('reports')

const modStats = ref({})
const priceStats = ref({})
const disputeStats = ref({})
const blackStats = ref({})

const pick = (row, ...names) => {
  for (const n of names) {
    if (row && row[n] !== undefined && row[n] !== null) return row[n]
  }
  return undefined
}

const imgUrl = (path) => {
  if (!path) return ''
  return path.startsWith('http') ? path : baseURL + path
}
const avatarUrl = (path) => {
  if (!path) return DEFAULT_AVATAR
  return path.startsWith('http') ? path : baseURL + path
}
const formatTime = (t) => {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 16)
}

/* ==================== 举报 ==================== */
const reportList = ref([])
const reportTotal = ref(0)
const reportPageNum = ref(1)
const reportPageSize = 10
const reportLoading = ref(false)

const loadReports = async (page = reportPageNum.value) => {
  reportLoading.value = true
  try {
    const res = await getReportList({ pageNum: page, pageSize: reportPageSize })
    reportList.value = (res && res.list) || []
    reportTotal.value = (res && res.total) || 0
    reportPageNum.value = (res && res.pageNum) || page
  } catch (e) { /* 拦截器已提示 */ } finally { reportLoading.value = false }
}
const onReportPage = (p) => loadReports(p)

const handleReportRow = async (row, status) => {
  const id = pick(row, 'id')
  const actionText = status === 'handled' ? '标记为已处理' : '驳回'
  try {
    await ElMessageBox.confirm(`确认将该举报${actionText}？`, '提示', { type: 'warning' })
  } catch (e) { return }
  try {
    await handleReport(id, status)
    ElMessage.success('处置完成')
    loadReports()
  } catch (e) { /* 拦截器已提示 */ }
}

const targetTypeText = (t) => ({ goods: '商品', user: '用户', comment: '评论' }[t] || '对象')
const reportStatusText = (s) => ({ pending: '待处理', handled: '已处理', rejected: '已驳回' }[s] || s || '待处理')
const reportStatusType = (s) => (s === 'handled' ? 'success' : s === 'rejected' ? 'info' : 'warning')

/* ==================== 违禁品 ==================== */
const moderationList = ref([])
const moderationTotal = ref(0)
const moderationPageNum = ref(1)
const moderationPageSize = 10
const moderationLoading = ref(false)
const moderationVerdict = ref('')
const showWordLib = ref(false)
const wordItems = ref([])

const loadModeration = async (page = moderationPageNum.value) => {
  moderationLoading.value = true
  try {
    const res = await getModerationList({
      verdict: moderationVerdict.value || undefined,
      pageNum: page,
      pageSize: moderationPageSize
    })
    moderationList.value = (res && res.list) || []
    moderationTotal.value = (res && res.total) || 0
    moderationPageNum.value = (res && res.pageNum) || page
  } catch (e) { /* 拦截器已提示 */ } finally { moderationLoading.value = false }
}
const onModerationPage = (p) => loadModeration(p)

const loadModerationStats = async () => {
  try { modStats.value = (await getModerationStats()) || {} } catch (e) { /* 忽略 */ }
}

const confirmRecord = async (row, action) => {
  const text = action === 'release' ? '放行该商品（解除拦截）' : '确认拦截（商品将被下架）'
  try {
    await ElMessageBox.confirm(`确认${text}？`, '人工复核', { type: 'warning' })
  } catch (e) { return }
  try {
    await confirmModeration(row.id, action)
    ElMessage.success(action === 'release' ? '已放行' : '已确认拦截')
    loadModeration()
    loadModerationStats()
  } catch (e) { /* 拦截器已提示 */ }
}

const toggleWordLib = async () => {
  showWordLib.value = !showWordLib.value
  if (showWordLib.value && wordItems.value.length === 0) {
    try { wordItems.value = (await getProhibitedItems()) || [] } catch (e) { /* 忽略 */ }
  }
}

const refreshWords = async () => {
  try {
    await refreshProhibitedWords()
    ElMessage.success('词库缓存已刷新')
  } catch (e) { /* 拦截器已提示 */ }
}

const verdictText = (v) => ({ block: '拦截', review: '复核', pass: '通过' }[v] || v || '—')
const verdictType = (v) => (v === 'block' ? 'danger' : v === 'review' ? 'warning' : 'success')
const recordStatusText = (s) => ({ auto: '待处理', confirmed: '已确认', released: '已放行' }[s] || s || '—')
const categoryText = (c) => ({
  appliance: '违规电器', e_cigarette: '电子烟', battery: '违规电池',
  dangerous: '危险品', medical: '药品器械', fake: '假冒品牌', exam: '作弊器材'
}[c] || c || '—')

/* ==================== 价格预警 ==================== */
const priceList = ref([])
const priceTotal = ref(0)
const pricePageNum = ref(1)
const pricePageSize = 10
const priceLoading = ref(false)
const priceStatus = ref('')
const rescanning = ref(false)

const loadPriceAlerts = async (page = pricePageNum.value) => {
  priceLoading.value = true
  try {
    const res = await getPriceAlertList({
      status: priceStatus.value || undefined,
      pageNum: page,
      pageSize: pricePageSize
    })
    priceList.value = (res && res.list) || []
    priceTotal.value = (res && res.total) || 0
    pricePageNum.value = (res && res.pageNum) || page
  } catch (e) { /* 拦截器已提示 */ } finally { priceLoading.value = false }
}
const onPricePage = (p) => loadPriceAlerts(p)

const loadPriceStats = async () => {
  try { priceStats.value = (await getPriceAlertStats()) || {} } catch (e) { /* 忽略 */ }
}

const handlePrice = async (row, status) => {
  try {
    await handlePriceAlert(row.id, status, status === 'handled' ? '已提醒卖家调整价格' : '误报忽略')
    ElMessage.success(status === 'handled' ? '已标记处理' : '已忽略该预警')
    loadPriceAlerts()
    loadPriceStats()
  } catch (e) { /* 拦截器已提示 */ }
}

const doRescan = async () => {
  rescanning.value = true
  try {
    const res = await rescanPrice()
    ElMessage.success(`巡检完成：扫描 ${res.scanned} 件在售商品，命中 ${res.flagged} 件价格异常`)
    loadPriceAlerts(1)
    loadPriceStats()
  } catch (e) { /* 拦截器已提示 */ } finally { rescanning.value = false }
}

const ratioPercent = (ratio) => {
  const r = Number(ratio || 0)
  return r > 0 ? Math.round((r - 1) * 100) : 0
}
const priceStatusText = (s) => ({ open: '待处理', handled: '已处理', dismissed: '已忽略' }[s] || s || '—')

/* ==================== 纠纷 ==================== */
const disputeList = ref([])
const disputeTotal = ref(0)
const disputePageNum = ref(1)
const disputePageSize = 10
const disputeLoading = ref(false)
const disputeStatus = ref('')
const disputeDialogVisible = ref(false)
const disputeDetail = ref({})
const verdictForm = ref({ id: null, verdict: '', note: '' })
const resolving = ref(false)

const loadDisputes = async (page = disputePageNum.value) => {
  disputeLoading.value = true
  try {
    const res = await getAdminDisputeList({
      status: disputeStatus.value || undefined,
      pageNum: page,
      pageSize: disputePageSize
    })
    disputeList.value = (res && res.list) || []
    disputeTotal.value = (res && res.total) || 0
    disputePageNum.value = (res && res.pageNum) || page
  } catch (e) { /* 拦截器已提示 */ } finally { disputeLoading.value = false }
}
const onDisputePage = (p) => loadDisputes(p)

const loadDisputeStats = async () => {
  try { disputeStats.value = (await getDisputeStats()) || {} } catch (e) { /* 忽略 */ }
}

const openDisputeDetail = async (id) => {
  disputeDialogVisible.value = true
  disputeDetail.value = {}
  try {
    disputeDetail.value = (await getAdminDisputeDetail(id)) || {}
    verdictForm.value = { id, verdict: '', note: '' }
  } catch (e) { /* 拦截器已提示 */ }
}

const takeDispute = async (row) => {
  try {
    await startDisputeProcessing(row.id)
    ElMessage.success('已介入该纠纷')
    loadDisputes()
    loadDisputeStats()
  } catch (e) { /* 拦截器已提示 */ }
}

const submitVerdict = async () => {
  if (!verdictForm.value.verdict) {
    ElMessage.warning('请选择裁决结果')
    return
  }
  try {
    await ElMessageBox.confirm('裁决后担保资金将按结果自动退款/结算，且不可撤销，确认提交？', '提交裁决', { type: 'warning' })
  } catch (e) { return }
  resolving.value = true
  try {
    const res = await resolveDispute(verdictForm.value)
    ElMessage.success(typeof res === 'string' ? res : '裁决已提交')
    disputeDialogVisible.value = false
    loadDisputes()
    loadDisputeStats()
  } catch (e) { /* 拦截器已提示 */ } finally { resolving.value = false }
}

const reasonText = (r) => ({
  quality: '质量问题', not_as_described: '与描述不符', no_delivery: '未发货/未交付',
  refund_refused: '拒绝退款', other: '其他纠纷'
}[r] || r || '其他')
const disputeStatusText = (s) => ({ open: '待受理', processing: '处理中', resolved: '已结案', rejected: '已驳回', withdrawn: '已撤回' }[s] || s || '—')
const disputeStatusType = (s) => (s === 'resolved' ? 'success' : s === 'processing' ? 'warning' : 'danger')
const evidenceTypeText = (t) => ({ chat: '聊天记录', image: '图片证据', text: '文字说明' }[t] || t || '说明')
const verdictResultText = (v) => ({
  buyer_win: '买家胜诉，担保资金退回买家，卖家信用-10',
  seller_win: '卖家胜诉，担保资金结算给卖家',
  split: '责任共担，资金退回买家',
  no_fault: '证据不足，维持原交易'
}[v] || '—')

/* ==================== 黑名单 ==================== */
const blackList = ref([])
const blackTotal = ref(0)
const blackPageNum = ref(1)
const blackPageSize = 10
const blackLoading = ref(false)
const blackStatus = ref('')
const blackDialogVisible = ref(false)
const markForm = ref({ userId: '', level: 'warning', reason: '', evidence: '' })
const marking = ref(false)

const loadBlacklist = async (page = blackPageNum.value) => {
  blackLoading.value = true
  try {
    const res = await getBlacklistPage({
      status: blackStatus.value || undefined,
      pageNum: page,
      pageSize: blackPageSize
    })
    blackList.value = (res && res.list) || []
    blackTotal.value = (res && res.total) || 0
    blackPageNum.value = (res && res.pageNum) || page
  } catch (e) { /* 拦截器已提示 */ } finally { blackLoading.value = false }
}
const onBlackPage = (p) => loadBlacklist(p)

const loadBlackStats = async () => {
  try { blackStats.value = (await getBlacklistStats()) || {} } catch (e) { /* 忽略 */ }
}

const openMarkBlacklist = () => {
  markForm.value = { userId: '', level: 'warning', reason: '', evidence: '' }
  blackDialogVisible.value = true
}

const openBlacklistForReport = (row) => {
  const targetType = pick(row, 'targetType', 'target_type')
  if (targetType && targetType !== 'user') {
    ElMessage.warning('该举报目标不是用户，无法直接拉黑')
    return
  }
  markForm.value = {
    userId: String(pick(row, 'targetId', 'target_id') || ''),
    level: 'warning',
    reason: pick(row, 'reason') || '',
    evidence: pick(row, 'detail') || ''
  }
  blackDialogVisible.value = true
}

const submitMark = async () => {
  if (!markForm.value.userId) {
    ElMessage.warning('请输入用户 ID')
    return
  }
  if (!markForm.value.reason) {
    ElMessage.warning('请填写失信原因')
    return
  }
  marking.value = true
  try {
    await markBlacklist({
      userId: Number(markForm.value.userId),
      level: markForm.value.level,
      reason: markForm.value.reason,
      evidence: markForm.value.evidence
    })
    ElMessage.success('已加入平台黑名单')
    blackDialogVisible.value = false
    loadBlacklist(1)
    loadBlackStats()
  } catch (e) { /* 拦截器已提示 */ } finally { marking.value = false }
}

const revokeRow = async (row) => {
  try {
    await ElMessageBox.confirm(`确认解除「${row.userName}」的失信标记？`, '解除黑名单', { type: 'warning' })
  } catch (e) { return }
  try {
    await revokeBlacklist(row.id)
    ElMessage.success('已解除')
    loadBlacklist()
    loadBlackStats()
  } catch (e) { /* 拦截器已提示 */ }
}

const levelText = (l) => ({ watch: '重点关注', warning: '失信警告', serious: '严重失信' }[l] || l || '—')
const levelType = (l) => (l === 'serious' ? 'danger' : l === 'warning' ? 'warning' : 'info')

/* ==================== 汇总 ==================== */
const switchSub = (key) => {
  sub.value = key
  if (key === 'reports') loadReports()
  if (key === 'moderation') loadModeration()
  if (key === 'price') loadPriceAlerts()
  if (key === 'dispute') loadDisputes()
  if (key === 'blacklist') loadBlacklist()
}

const loadAll = () => {
  loadReports()
  loadModeration()
  loadPriceAlerts()
  loadDisputes()
  loadBlacklist()
  loadModerationStats()
  loadPriceStats()
  loadDisputeStats()
  loadBlackStats()
}

onMounted(loadAll)
</script>

<style scoped>
.risk-panel { padding: 4px 0 8px; }

.risk-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
.risk-card {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 14px 16px;
  cursor: pointer;
  transition: all .2s;
}
.risk-card:hover, .risk-card.active {
  border-color: #E1251B;
  box-shadow: 0 6px 16px rgba(225, 37, 27, .1);
}
.rc-num { font-size: 22px; font-weight: 800; color: #E1251B; line-height: 1.2; }
.rc-label { font-size: 13px; color: #333; margin-top: 2px; }
.rc-sub { font-size: 12px; color: #999; margin-top: 2px; }

.risk-subtabs {
  display: flex;
  gap: 24px;
  border-bottom: 1px solid #eee;
  margin-bottom: 14px;
}
.risk-subtab {
  padding: 8px 2px 10px;
  font-size: 13.5px;
  color: #666;
  cursor: pointer;
  position: relative;
}
.risk-subtab.active { color: #E1251B; font-weight: 600; }
.risk-subtab.active::after {
  content: '';
  position: absolute;
  left: 0; right: 0; bottom: -1px;
  height: 2px;
  background: #E1251B;
  border-radius: 2px;
}

.risk-block { background: #fff; border-radius: 12px; padding: 14px 16px; }
.risk-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.toolbar-left { display: flex; align-items: center; gap: 10px; }
.toolbar-right { display: flex; align-items: center; gap: 8px; }
.toolbar-title { font-size: 14px; font-weight: 700; color: #333; }
.toolbar-note { font-size: 12px; color: #999; }

.risk-table { width: 100%; }
.cell-muted { color: #999; font-size: 12px; }

.goods-cell, .user-cell { display: flex; align-items: center; gap: 8px; }
.goods-cell-img { width: 40px; height: 40px; border-radius: 6px; object-fit: cover; background: #f5f5f5; }
.user-cell-avatar { width: 30px; height: 30px; border-radius: 50%; object-fit: cover; background: #f5f5f5; }
.goods-cell-title {
  font-size: 13px;
  color: #333;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.price-now { color: #E1251B; font-weight: 700; }

.risk-pagination { display: flex; justify-content: flex-end; margin-top: 12px; }

.word-lib { margin-top: 16px; border-top: 1px dashed #eee; padding-top: 12px; }
.word-lib-title { font-size: 13px; font-weight: 600; color: #333; margin-bottom: 8px; }

.dispute-detail { max-height: 60vh; overflow-y: auto; padding-right: 4px; }
.dd-head { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.dd-title { font-size: 15px; font-weight: 700; color: #333; }
.dd-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px 12px;
  font-size: 13px;
  color: #555;
  background: #FAFAFA;
  border-radius: 8px;
  padding: 10px 12px;
}
.dd-section { margin-top: 14px; }
.dd-section-title { font-size: 13px; font-weight: 700; color: #333; margin-bottom: 6px; }
.dd-text { font-size: 13px; color: #555; line-height: 1.7; white-space: pre-wrap; }
.evidence-item {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 8px 10px;
  margin-bottom: 8px;
}
.evidence-head { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.evidence-img { max-width: 100%; max-height: 220px; border-radius: 6px; margin-top: 6px; display: block; }

.mark-form .mark-field { margin-bottom: 14px; }
.mark-field label { display: block; font-size: 13px; color: #333; margin-bottom: 6px; }
.mark-field .req { color: #E1251B; }

@media (max-width: 900px) {
  .risk-cards { grid-template-columns: repeat(2, 1fr); }
  .dd-grid { grid-template-columns: 1fr; }
}
</style>
