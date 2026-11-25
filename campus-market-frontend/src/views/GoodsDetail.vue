<template>
  <div class="jd-detail">
    <!-- 顶部面包屑导航 -->
    <div class="jd-crumb">
      <div class="crumb-inner">
        <span class="crumb-back" @click="goBack"><el-icon><ArrowLeft /></el-icon> 返回</span>
        <span class="crumb-sep">|</span>
        <span class="crumb-link" @click="goHome">首页</span>
        <el-icon class="crumb-arrow"><ArrowRight /></el-icon>
        <span class="crumb-link" @click="goHome">{{ goodsInfo?.categoryName || '商品' }}</span>
        <el-icon class="crumb-arrow"><ArrowRight /></el-icon>
        <span class="crumb-current" :title="goodsInfo?.title">{{ goodsInfo?.title }}</span>
        <span class="crumb-share" @click="handleShare"><el-icon><Share /></el-icon> 分享</span>
      </div>
    </div>

    <!-- 加载中 -->
    <div class="loading-state" v-if="!goodsInfo && loading">
      <el-icon :size="40" class="loading-spin"><Loading /></el-icon>
      <p>加载中...</p>
    </div>

    <!-- 主体三栏 -->
    <div class="jd-container" v-if="goodsInfo">
      <!-- 左栏：卖家名片 + 同类推荐 -->
      <aside class="jd-left">
        <div class="seller-card">
          <img :src="getAvatarUrl(goodsInfo.sellerAvatar)" class="seller-avatar" @error="handleAvatarError" alt="" />
          <div class="seller-info">
            <div class="seller-name" :title="goodsInfo.sellerName">{{ goodsInfo.sellerName }}</div>
            <div class="seller-label">校园卖家</div>
          </div>
          <div class="shop-rating">
            <span class="rate-item">评价 <b>{{ goodsInfo.commentCount || 0 }}</b>条 <b class="rate-star">★{{ goodsInfo.avgRating || '—' }}</b></span>
            <span class="rate-sep">|</span>
            <span class="rate-item">物流 <b>暂无</b></span>
            <span class="rate-sep">|</span>
            <span class="rate-item">售后 <b>暂无</b></span>
          </div>
          <div class="seller-btns">
            <el-button size="small" class="enter-shop-btn" @click="viewSellerProfile">进店</el-button>
            <el-button size="small" class="contact-btn" @click="contactSeller"><el-icon><ChatDotRound /></el-icon> 联系卖家</el-button>
          </div>
        </div>

        <!-- 卖家信用档案：校园熟人社会的信任凭证 -->
        <div class="credit-profile" v-if="goodsInfo.sellerLevel">
          <div class="cp-head">
            <span class="cp-level" :class="creditLevelClass">{{ goodsInfo.sellerLevel }}</span>
            <span class="cp-credit">信用分 <b>{{ goodsInfo.sellerCredit || 0 }}</b></span>
          </div>
          <div class="cp-stats">
            <div class="cp-stat">
              <b>{{ goodsInfo.sellerSoldCount || 0 }}</b>
              <span>历史成交</span>
            </div>
            <div class="cp-stat">
              <b>{{ goodsInfo.sellerOnSaleCount || 0 }}</b>
              <span>在售件数</span>
            </div>
            <div class="cp-stat">
              <b>{{ sellerJoinText }}</b>
              <span>入校时长</span>
            </div>
          </div>
          <div class="cp-safe">
            <el-icon class="cp-safe-icon"><Location /></el-icon>
            面交提示：约在校内公共场所，当面验货后再付款
          </div>
        </div>

        <!-- 风控：平台共享黑名单提醒 -->
        <div class="blacklist-warning" v-if="sellerRisk.blocked">
          <span class="bw-icon">!</span>
          <div class="bw-body">
            <b>交易风险提醒</b>
            <div>{{ sellerRisk.message }}</div>
          </div>
        </div>

        <div class="related-box">
          <div class="related-title">同类推荐</div>
          <div class="related-list" v-if="relatedGoods.length > 0">
            <div
              v-for="g in relatedGoods"
              :key="g.id"
              class="related-item"
              @click="goGoods(g.id)"
            >
              <SmartImage :src="g.coverImage || g.images" class="related-img" alt="" />
              <div class="related-info">
                <div class="related-name">{{ g.title }}</div>
                <div class="related-price">&yen;{{ g.price }}</div>
              </div>
            </div>
          </div>
          <div class="empty-tip" v-else>
            <el-icon :size="32" color="#ddd"><Goods /></el-icon>
            <p>暂无同类推荐</p>
          </div>
        </div>
      </aside>

      <!-- 中栏：主图 + Tab 面板 -->
      <main class="jd-center">
        <div class="gallery">
          <!-- 竖向缩略图 -->
          <div class="thumb-col" v-if="imageList.length > 1">
            <div
              v-for="(img, idx) in imageList"
              :key="idx"
              class="thumb-item"
              :class="{ active: idx === currentImg }"
              @click="currentImg = idx"
            >
              <img :src="img" @error="handleImgError" alt="" />
            </div>
          </div>
          <!-- 主大图（鼠标移入放大，支持全屏查看） -->
          <div
            class="main-image-wrap"
            :class="{ 'is-zoomed': imgZooming }"
            @mouseenter="startZoom"
            @mousemove="moveZoom"
            @mouseleave="stopZoom"
          >
            <img
              v-if="imageList.length > 0"
              :src="imageList[currentImg]"
              class="main-image"
              :style="zoomStyle"
              @error="handleImgError"
              alt=""
            />
            <div class="image-placeholder" v-else>
              <el-icon :size="60" color="#ddd"><Picture /></el-icon>
            </div>
            <div class="img-arrow img-prev" v-if="imageList.length > 1" @click="prevImg"><el-icon><ArrowLeft /></el-icon></div>
            <div class="img-arrow img-next" v-if="imageList.length > 1" @click="nextImg"><el-icon><ArrowRight /></el-icon></div>
            <div class="img-indicator" v-if="imageList.length > 1">{{ currentImg + 1 }}/{{ imageList.length }}</div>
            <span class="zoom-tip" v-if="imageList.length > 0">移入图片可放大查看</span>
            <button class="img-fullscreen" v-if="imageList.length > 0" title="全屏查看" @click.stop="showViewer = true">
              <el-icon><FullScreen /></el-icon>
            </button>
          </div>
        </div>

        <!-- Tab 切换 -->
        <div class="tab-bar">
          <div
            v-for="t in tabs"
            :key="t.key"
            class="tab-item"
            :class="{ active: activeTab === t.key }"
            @click="activeTab = t.key"
          >{{ t.label }}<span v-if="t.key==='comments'" class="tab-count">({{ commentTotal || commentList.length }})</span></div>
        </div>

        <!-- Tab 面板 -->
        <div class="tab-panels">
          <!-- 商品介绍 -->
          <div class="tab-panel" v-show="activeTab === 'intro'">
            <h3 class="panel-title">商品介绍</h3>
            <p class="goods-desc" v-if="goodsInfo.description">{{ goodsInfo.description }}</p>
            <div class="empty-tip" v-else>卖家暂无补充介绍</div>
          </div>

          <!-- 商品参数 -->
          <div class="tab-panel" v-show="activeTab === 'params'">
            <h3 class="panel-title">商品参数</h3>
            <table class="params-table">
              <tr v-for="p in paramsList" :key="p.label">
                <td class="params-label">{{ p.label }}</td>
                <td class="params-value">{{ p.value }}</td>
              </tr>
            </table>
          </div>

          <!-- 评价 -->
          <div class="tab-panel" v-show="activeTab === 'comments'">
            <div class="section-header">
              <h3 class="panel-title">商品评价</h3>
              <span class="comment-count">{{ commentTotal || commentList.length }}条评价</span>
            </div>
            <div class="comment-list" v-if="commentList.length > 0">
              <div class="comment-item" v-for="comment in commentList" :key="comment.id">
                <img :src="getAvatarUrl(comment.fromUserAvatar)" class="comment-avatar" @error="handleAvatarError" alt="" />
                <div class="comment-body">
                  <div class="comment-header">
                    <span class="comment-user">{{ comment.fromUserName }}</span>
                    <div class="comment-stars">
                      <el-icon v-for="star in 5" :key="star" :style="{ color: star <= comment.rating ? '#E1251B' : '#ddd' }"><StarFilled /></el-icon>
                    </div>
                  </div>
                  <p class="comment-content">{{ comment.content }}</p>
                  <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                </div>
              </div>
              <div class="pagination-wrap" v-if="commentTotal > commentPageSize">
                <el-pagination background small layout="prev, pager, next" :total="commentTotal" :current-page="commentPageNum" :page-size="commentPageSize" @current-change="handleCommentPageChange" />
              </div>
            </div>
            <div class="empty-tip" v-else>
              <el-icon :size="40" color="#ddd"><ChatDotRound /></el-icon>
              <p>暂无评价</p>
            </div>
            <div class="comment-input-wrap" v-if="user && canComment">
              <el-input v-model="commentContent" class="comment-input" type="textarea" :rows="3" placeholder="写下你的评价..." maxlength="500" show-word-limit />
              <div class="comment-stars-input">
                <span class="stars-label">评分：</span>
                <el-icon v-for="star in 5" :key="star" class="star-icon" :style="{ color: star <= commentRating ? '#E1251B' : '#ddd' }" @click="commentRating = star"><StarFilled /></el-icon>
              </div>
              <el-button type="primary" class="submit-comment-btn" @click="submitComment" :loading="submittingComment">提交评价</el-button>
            </div>
          </div>

          <!-- 售后保障 -->
          <div class="tab-panel" v-show="activeTab === 'service'">
            <h3 class="panel-title">售后保障</h3>
            <ul class="service-list">
              <li>本平台为校园二手闲置交易，商品由卖家个人发布，请<b>当面验货</b>后再交易。</li>
              <li>建议在校内公共场所（如操场门口、校门口）见面交易，避免线上转账风险。</li>
              <li>如遇商品与描述严重不符，可在消息中与卖家协商，或向平台举报。</li>
              <li>商品成色、配件情况以卖家描述和实物为准，平台不参与定价与质量担保。</li>
            </ul>
          </div>

          <!-- 校园同款比价 -->
          <div class="tab-panel" v-show="activeTab === 'similar'">
            <h3 class="panel-title">🔍 校园同款比价</h3>
            <div v-if="similarLoading" class="similar-empty">加载中...</div>
            <div v-else-if="!similarData.hasSimilar" class="similar-empty">
              <p>校园内暂未发现同类在售商品</p>
              <small>当前分类：{{ similarData.categoryName || goodsInfo.categoryName }}</small>
            </div>
            <div v-else>
              <div class="similar-tip" v-if="similarData.priceTip">{{ similarData.priceTip }}</div>
              <div class="similar-list">
                <div
                  class="similar-item"
                  v-for="item in similarData.items"
                  :key="item.id"
                  @click="$router.push(`/goods/${item.id}`)"
                >
                  <div class="sim-img-wrap">
                    <img :src="item.coverImage" class="sim-img" alt="" />
                    <span class="sim-cheaper" v-if="item.price < similarData.targetPrice">
                      省¥{{ (similarData.targetPrice - item.price).toFixed(2) }}
                    </span>
                  </div>
                  <div class="sim-info">
                    <div class="sim-title">{{ item.title }}</div>
                    <div class="sim-meta">
                      <span class="sim-price">¥{{ item.price }}</span>
                      <span class="sim-seller">@{{ item.sellerName }}</span>
                      <span class="sim-tag" v-if="item.isGraduationSeller">🎓毕业</span>
                    </div>
                    <div class="sim-views">{{ item.viewCount }} 浏览</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>

      <!-- 右栏：商品信息 + 操作 -->
      <aside class="jd-right">
        <div class="info-card">
          <h1 class="goods-title">{{ goodsInfo.title }}</h1>

          <!-- 商品标签，一眼看清核心信息 -->
          <div class="title-tags">
            <span class="title-tag tag-orange" v-if="goodsInfo.categoryName">{{ goodsInfo.categoryName }}</span>
            <span class="title-tag" v-if="goodsInfo.location">
              <el-icon size="12"><Location /></el-icon>{{ goodsInfo.location }}
            </span>
            <span class="title-tag tag-green">当面交易</span>
            <span class="title-tag tag-green">支持验货</span>
            <span class="title-tag tag-blue" v-if="goodsInfo.canMail">可邮寄</span>
            <span class="title-tag tag-blue" v-if="goodsInfo.canDeliver">送宿舍</span>
            <span class="title-tag tag-exchange" v-if="goodsInfo.exchangeSupported">可换物</span>
            <span class="title-tag tag-exchange" v-if="goodsInfo.exchangeWant">想换：{{ goodsInfo.exchangeWant }}</span>
            <span class="title-tag tag-status" v-if="goodsStatusText">{{ goodsStatusText }}</span>
          </div>

          <!-- 价格区 -->
          <div class="price-box">
            <div class="price-row">
              <span class="jd-tag" :class="{ 'jd-tag-exchange': isExchange }">{{ isExchange ? '换物模式' : '校园价' }}</span>
              <template v-if="isExchange">
                <span class="price-value exchange-price">以物换物</span>
                <span class="original-price">参考价 &yen;{{ goodsInfo.price }}</span>
              </template>
              <template v-else>
                <span class="price-symbol">&yen;</span>
                <span class="price-value">{{ goodsInfo.price }}</span>
                <span class="original-price" v-if="goodsInfo.couponAmount > 0 && goodsInfo.listPrice">
                  &yen;{{ goodsInfo.listPrice }}
                </span>
                <span class="original-price" v-else-if="goodsInfo.originalPrice && +goodsInfo.originalPrice > +goodsInfo.price">
                  &yen;{{ goodsInfo.originalPrice }}
                </span>
              </template>
            </div>
            <div class="coupon-row" v-if="!isExchange && goodsInfo.couponAmount > 0">
              <span class="coupon-chip">🎟 优惠券</span>
              <span class="coupon-cut">-&yen;{{ goodsInfo.couponAmount }}</span>
              <span class="coupon-note">标价 &yen;{{ goodsInfo.listPrice }}，下单自动抵扣</span>
            </div>
            <div class="discount-row" v-if="!isExchange && discount">
              <span class="discount-tag">{{ discount.rate }}折</span>
              <span class="save-tip">立省 &yen;{{ discount.save }}</span>
            </div>
          </div>

          <!-- 风控：价格高于近期同类均价提示 -->
          <div class="risk-price-tip" v-if="goodsInfo.priceAlert === 1">
            <span class="rpt-icon">!</span>
            <span>{{ goodsInfo.priceAlertNote || '该商品价格高于近期同类均价，建议先比价再下单' }}</span>
          </div>
          <div class="risk-price-tip soft" v-else-if="priceInsight.overpriced">
            <span class="rpt-icon">i</span>
            <span>{{ priceInsight.note }}</span>
          </div>

          <!-- 信息行 -->
          <div class="info-rows">
            <div class="info-row" v-if="goodsInfo.location">
              <span class="info-label">送至</span>
              <span class="info-val">
                <el-icon size="14" color="#E1251B"><Location /></el-icon>
                {{ goodsInfo.location }}
                <span class="delivery-tip">· 校内当面交易</span>
              </span>
            </div>
            <div class="info-row">
              <span class="info-label">服务</span>
              <span class="info-val svc-tags">
                <span class="svc-tag">校内当面交易</span>
                <span class="svc-tag">支持当面验货</span>
                <span class="svc-tag">卖家实名认证</span>
              </span>
            </div>
            <div class="info-row">
              <span class="info-label">累计评价</span>
              <span class="info-val">{{ commentTotal || commentList.length }} 条</span>
            </div>
            <div class="info-row">
              <span class="info-label">浏览量</span>
              <span class="info-val">{{ goodsInfo.viewCount || 0 }} 次</span>
            </div>
            <div class="info-row">
              <span class="info-label">发布时间</span>
              <span class="info-val">{{ formatTime(goodsInfo.createTime) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">商品成色</span>
              <span class="info-val">二手（以实物为准）</span>
            </div>
          </div>
        </div>

        <!-- 校园行情价 & 砍价助手 -->
        <div class="market-card" v-if="marketData.hasData">
          <div class="market-head">
            <span class="market-title">📊 校园行情价</span>
            <span class="market-verdict" :class="marketVerdictClass">{{ marketData.verdict }}</span>
          </div>
          <div class="market-range">
            <span class="range-num">¥{{ marketData.p25 }}</span>
            <div class="range-bar">
              <span class="range-dot" :style="{ left: pricePosition + '%' }"></span>
            </div>
            <span class="range-num">¥{{ marketData.p75 }}</span>
          </div>
          <div class="market-meta">
            同类在售 {{ marketData.sampleCount }} 件 · 中位价 ¥{{ marketData.median }}
            <template v-if="marketData.dealCount > 0"> · 近半年成交 {{ marketData.dealCount }} 单，均价 ¥{{ marketData.dealAvg }}</template>
          </div>
          <div class="market-offer">
            <div class="offer-left">
              <span class="offer-label">建议出价</span>
              <span class="offer-price">¥{{ marketData.suggestedOffer }}</span>
              <span class="offer-note">已挂牌 {{ marketData.daysListed }} 天</span>
            </div>
            <button class="offer-copy" @click="copyBargainTip">复制砍价话术</button>
          </div>
        </div>

        <!-- 多平台比价 -->
        <div class="compare-card" v-if="compareData.hasData">
          <div class="compare-head">
            <span class="compare-title">🔍 其他平台比价</span>
            <span class="compare-verdict" :class="compareVerdictClass">{{ compareData.verdict }}</span>
          </div>
          <div class="compare-tip">{{ compareData.tip }}</div>
          <div class="compare-list">
            <div class="compare-row" v-for="p in compareData.platforms" :key="p.key">
              <span class="pf-logo" :class="'pf-' + p.key">{{ p.name.slice(0, 1) }}</span>
              <div class="pf-info">
                <div class="pf-name-row">
                  <span class="pf-name">{{ p.name }}</span>
                  <span class="pf-tag">{{ p.tag }}</span>
                </div>
                <div class="pf-range">¥{{ p.low }} - ¥{{ p.high }}</div>
              </div>
              <div class="pf-right">
                <span class="pf-price">¥{{ p.price }}</span>
                <span v-if="p.key === 'aihuishou'" class="pf-diff pf-note">回收参考价</span>
                <span v-else class="pf-diff" :class="Number(p.diff) >= 0 ? 'diff-down' : 'diff-up'">
                  {{ Number(p.diff) >= 0 ? '省 ¥' + p.diff : '贵 ¥' + Math.abs(Number(p.diff)) }}
                </span>
              </div>
            </div>
          </div>
          <div class="compare-foot">本平台 ¥{{ compareData.localPrice }} · 基于 {{ compareData.sampleCount || 0 }} 件同类在售估算，仅供参考</div>
        </div>

        <!-- 平台担保提示 -->
        <div class="escrow-hint">
          <span class="es-icon">🛡</span>
          <span>平台担保交易：付款先由平台托管，确认收货或当面核销后才打款给卖家</span>
        </div>

        <!-- 操作栏 -->
        <div class="action-bar">
          <div class="bar-icon-btn" @click="handleFavorite">
            <el-icon :size="22" :color="isFavorited ? '#E1251B' : '#666'"><StarFilled v-if="isFavorited" /><Star v-else /></el-icon>
            <span class="icon-label" :style="{ color: isFavorited ? '#E1251B' : '#666' }">{{ isFavorited ? '已收藏' : '收藏' }}</span>
          </div>
          <div class="bar-icon-btn" @click="contactSeller">
            <el-icon :size="22" color="#666"><ChatDotRound /></el-icon>
            <span class="icon-label">客服</span>
          </div>
          <template v-if="isExchange">
            <button class="btn-want" :disabled="!canBuy || exchangeLoading" @click="openExchangeDialog">我想换</button>
            <button class="btn-buy" :disabled="!canBuy || exchangeLoading" @click="openExchangeDialog">
              {{ exchangeLoading ? '读取商品中...' : (canBuy ? '申请换物' : (goodsStatusText || '不可换物')) }}
            </button>
          </template>
          <template v-else>
            <button class="btn-want" :disabled="!canBuy" @click="openOrderDialog">我想要</button>
            <button class="btn-buy" :disabled="!canBuy" @click="openOrderDialog">{{ canBuy ? '立即下单' : (goodsStatusText || '不可购买') }}</button>
          </template>
        </div>
      </aside>
    </div>

    <!-- 下单弹窗 -->
    <el-dialog v-model="showOrderDialog" title="确认订单" width="380px" center :close-on-click-modal="false" custom-class="order-dialog">
      <div class="order-dialog-body" v-if="goodsInfo">
        <div class="order-goods-preview">
          <SmartImage :src="goodsInfo.coverImage || goodsInfo.images" class="order-thumb" alt="" />
          <div class="order-goods-info">
            <div class="order-goods-title">{{ goodsInfo.title }}</div>
            <div class="order-goods-price">&yen;{{ goodsInfo.price }}</div>
          </div>
        </div>
        <div class="order-coupon-row" v-if="goodsInfo.couponAmount > 0">
          <span>🎟 商品已含券 &yen;{{ goodsInfo.couponAmount }}</span>
          <span class="order-coupon-note">已计入商品价</span>
        </div>
        <div class="order-coupon-row" v-if="orderCoupon">
          <span>🎫 满&yen;{{ orderCoupon.thresholdAmount }}减&yen;{{ orderCoupon.discountAmount }}券</span>
          <span class="order-coupon-cut">-&yen;{{ orderCoupon.discountAmount }}</span>
        </div>
        <div class="order-price-row">
          <span class="price-label-text">实付款</span>
          <span class="order-total">&yen;{{ finalPay }}</span>
        </div>
      </div>
      <template #footer>
        <div class="order-dialog-footer">
          <el-button class="cancel-btn" @click="showOrderDialog = false">取消</el-button>
          <el-button type="primary" class="order-confirm-btn" @click="confirmOrder" :loading="ordering">确认下单</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 申请换物 -->
    <el-dialog
      v-model="showExchangeDialog"
      title="申请换物"
      width="460px"
      :close-on-click-modal="false"
      :close-on-press-escape="!exchanging"
      :show-close="!exchanging"
    >
      <div class="exchange-form" v-if="goodsInfo">
        <el-alert
          class="ex-flow"
          type="info"
          :closable="false"
          show-icon
          title="卖家接受后双方商品将锁定；线下交换验收后，双方需各自确认完成。"
        />
        <div class="ex-target">
          <SmartImage :src="goodsInfo.coverImage || goodsInfo.images" class="ex-img" alt="" />
          <div class="ex-target-info">
            <div class="ex-target-title">{{ goodsInfo.title }}</div>
            <div class="ex-target-sub">选择你的一件在售商品来交换</div>
          </div>
        </div>
        <el-select v-model="exchangeOfferId" placeholder="选择你的在售商品" style="width: 100%" class="ex-select" :disabled="!exchangeCandidates.length">
          <el-option
            v-for="g in exchangeCandidates"
            :key="g.id"
            :label="g.title + ' · 参考 ¥' + g.price"
            :value="g.id"
          />
        </el-select>
        <div class="ex-empty" v-if="!exchangeCandidates.length">
          <span>你还没有可用于交换的在售商品，请先发布一件闲置。</span>
          <el-button type="primary" link @click="goPublishForExchange">去发布商品</el-button>
        </div>
        <div class="ex-selected" v-if="selectedExchangeOffer">
          你将拿出 <b>{{ selectedExchangeOffer.title }}</b>，交换 <b>{{ goodsInfo.title }}</b>
        </div>
        <el-input
          v-model="exchangeMessage"
          type="textarea"
          :rows="3"
          maxlength="200"
          show-word-limit
          placeholder="想怎么换？可以说明商品成色、配件和交换地点"
          class="ex-msg"
        />
        <div class="ex-cash-tip">换物不经过平台现金支付，请当面互相验货，确认无误后再完成换物。</div>
      </div>
      <template #footer>
        <el-button :disabled="exchanging" @click="showExchangeDialog = false">取消</el-button>
        <el-button type="primary" :loading="exchanging" :disabled="!exchangeOfferId" @click="submitExchange">发送换物申请</el-button>
      </template>
    </el-dialog>

    <!-- 全屏看图 -->
    <el-image-viewer
      v-if="showViewer"
      :url-list="imageList"
      :initial-index="currentImg"
      teleported
      @close="showViewer = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getGoodsDetail, getGoodsList, getCampusSimilar, getMarketPrice, getPlatformCompare, getMyGoods } from '../api/goods'
import { applyExchange } from '../api/exchange'
import { toggleFavorite, checkFavorite } from '../api/favorite'
import { getGoodsComments, addComment } from '../api/comment'
import { createOrder, getBuyerOrders } from '../api/order'
import { recordBrowse } from '../api/browse'
import { getApplicableCoupon } from '../api/coupon'
import { getPriceInsight, checkBlacklist } from '../api/risk'

const route = useRoute()
const router = useRouter()
import { baseURL } from '../config'
import { DEFAULT_AVATAR, resolveAvatar } from '../utils/avatar'
import SmartImage from '../components/SmartImage.vue'

// 当前登录用户
const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

// 商品信息
const goodsInfo = ref(null)
const loading = ref(false)

// 同类推荐
const relatedGoods = ref([])

// 图片相关
const currentImg = ref(0)

const imageList = computed(() => {
  if (!goodsInfo.value) return []
  const images = goodsInfo.value.images
  const cover = goodsInfo.value.coverImage
  let urls = []
  if (images) {
    urls = images.split(',').map(img => {
      const trimmed = img.trim()
      if (trimmed.startsWith('http')) return trimmed
      return baseURL + trimmed
    })
  }
  if (cover && !urls.some(u => u.includes(cover))) {
    const coverUrl = cover.startsWith('http') ? cover : baseURL + cover
    urls.unshift(coverUrl)
  }
  return urls
})

const prevImg = () => {
  if (imageList.value.length === 0) return
  currentImg.value = (currentImg.value - 1 + imageList.value.length) % imageList.value.length
}
const nextImg = () => {
  if (imageList.value.length === 0) return
  currentImg.value = (currentImg.value + 1) % imageList.value.length
}

// 主图放大镜与全屏看图
const imgZooming = ref(false)
const zoomOrigin = ref('50% 50%')
const showViewer = ref(false)
const zoomStyle = computed(() => (imgZooming.value ? { transformOrigin: zoomOrigin.value } : {}))

const startZoom = () => {
  if (imageList.value.length > 0) imgZooming.value = true
}
const moveZoom = (e) => {
  const rect = e.currentTarget.getBoundingClientRect()
  const x = Math.min(100, Math.max(0, ((e.clientX - rect.left) / rect.width) * 100))
  const y = Math.min(100, Math.max(0, ((e.clientY - rect.top) / rect.height) * 100))
  zoomOrigin.value = `${x.toFixed(2)}% ${y.toFixed(2)}%`
}
const stopZoom = () => {
  imgZooming.value = false
}

// 折扣信息（真实计算）
const discount = computed(() => {
  const g = goodsInfo.value
  if (!g) return null
  const p = +g.price || 0
  const o = +g.originalPrice || 0
  if (o > p && p > 0) {
    return { save: (o - p).toFixed(2), rate: ((p / o) * 10).toFixed(1) }
  }
  return null
})

// 商品是否可购买：被拍下后立即进入交易中，禁止再次购买
const canBuy = computed(() => goodsInfo.value?.status === 'on_sale')
const goodsStatusText = computed(() => {
  const status = goodsInfo.value?.status
  if (status === 'locked') return '交易中'
  if (status === 'sold') return '已售出'
  if (status === 'off_shelf') return '已下架'
  return ''
})

// 卖家信用档案：等级配色 + 入校时长可读化
const creditLevelClass = computed(() => {
  const level = goodsInfo.value?.sellerLevel || ''
  if (level === '金牌卖家') return 'level-gold'
  if (level === '可信卖家') return 'level-trust'
  if (level === '谨慎交易') return 'level-risk'
  return 'level-normal'
})
const sellerJoinText = computed(() => {
  const days = goodsInfo.value?.sellerJoinDays || 0
  if (days >= 365) return (days / 365).toFixed(1) + ' 年'
  if (days >= 30) return Math.floor(days / 30) + ' 个月'
  return days + ' 天'
})
const openOrderDialog = () => {
  if (!canBuy.value) {
    ElMessage.warning(`商品${goodsStatusText.value || '当前不可购买'}`)
    return
  }
  // 打开确认订单前查一下可用于本单的平台满减券
  orderCoupon.value = null
  if (localStorage.getItem('token') && goodsInfo.value) {
    getApplicableCoupon(goodsInfo.value.price)
      .then(res => { orderCoupon.value = res || null })
      .catch(() => { orderCoupon.value = null })
  }
  showOrderDialog.value = true
}

// 本单实付 = 商品券后价 - 平台满减券
const finalPay = computed(() => {
  const price = Number(goodsInfo.value?.price || 0)
  const cut = Number(orderCoupon.value?.discountAmount || 0)
  return Math.max(0, price - cut).toFixed(2)
})

// Tab
const tabs = [
  { key: 'intro', label: '商品介绍' },
  { key: 'params', label: '商品参数' },
  { key: 'comments', label: '评价' },
  { key: 'service', label: '售后保障' },
  { key: 'similar', label: '校园同款' }
]
const activeTab = ref('intro')

// 商品参数（全部真实字段）
const paramsList = computed(() => {
  const g = goodsInfo.value
  if (!g) return []
  const list = [
    { label: '商品分类', value: g.categoryName || '—' },
    { label: '售价', value: '¥' + g.price }
  ]
  if (g.originalPrice) list.push({ label: '原价', value: '¥' + g.originalPrice })
  if (discount.value) list.push({ label: '折扣', value: discount.value.rate + '折（省¥' + discount.value.save + '）' })
  list.push({ label: '交易地点', value: g.location || '—' })
  list.push({ label: '商品成色', value: '二手（以实物为准）' })
  list.push({ label: '浏览量', value: (g.viewCount || 0) + ' 次' })
  list.push({ label: '发布时间', value: formatTime(g.createTime) })
  list.push({ label: '卖家', value: g.sellerName || '—' })
  return list
})

const isFavorited = ref(false)
const commentList = ref([])
const commentContent = ref('')
const commentRating = ref(5)
const submittingComment = ref(false)
const canComment = ref(false)
const commentableOrderId = ref(null)
const commentPageNum = ref(1)
const commentPageSize = ref(10)
const commentTotal = ref(0)
const showOrderDialog = ref(false)
const orderCoupon = ref(null)   // 本单可用的平台满减券
const ordering = ref(false)

// 本地默认占位图（不依赖外网，断网也不会裂图）
const DEFAULT_IMG = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='300' height='300'><rect width='300' height='300' fill='%23f5f5f5'/><text x='50%25' y='50%25' font-size='18' fill='%23bbb' text-anchor='middle' dy='.3em'>暂无图片</text></svg>"

const getImageUrl = (path) => {
  if (!path) return DEFAULT_IMG
  if (path.startsWith('http')) return path
  return baseURL + path
}
const getAvatarUrl = (path) => resolveAvatar(path)
const handleImgError = (e) => { e.target.src = DEFAULT_IMG }
const handleAvatarError = (e) => { e.target.src = DEFAULT_AVATAR }

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}`
}

const goBack = () => { router.back() }
const goHome = () => { router.push('/') }
const goGoods = (id) => { router.push(`/goods/${id}`) }
const handleShare = () => { ElMessage.info('分享功能开发中') }
const viewSellerProfile = () => {
  if (goodsInfo.value && goodsInfo.value.sellerId) router.push(`/profile?userId=${goodsInfo.value.sellerId}`)
}
const contactSeller = () => {
  if (!user.value) { ElMessage.warning('请先登录'); router.push('/login'); return }
  const g = goodsInfo.value
  // 只把商品作为“快捷发送条”的来源透传，Message 端不再锁定商品；
  // 带 sellerName/sellerAvatar 是为了首次联系（尚无会话）时聊天头部也能显示卖家信息
  router.push({
    path: '/message',
    query: {
      goodsId: g.id,
      sellerId: g.sellerId,
      sellerName: g.sellerName || '',
      sellerAvatar: g.sellerAvatar || '',
      goodsTitle: g.title || '',
      goodsCover: g.coverImage || '',
      goodsPrice: g.price != null ? g.price : ''
    }
  })
}

const handleFavorite = async () => {
  if (!user.value) { ElMessage.warning('请先登录'); router.push('/login'); return }
  try {
    const msg = await toggleFavorite({ userId: user.value.id, goodsId: goodsInfo.value.id })
    isFavorited.value = !isFavorited.value
    ElMessage.success(msg || (isFavorited.value ? '收藏成功' : '已取消收藏'))
  } catch (err) { console.error('收藏操作失败', err) }
}

const confirmOrder = async () => {
  if (!canBuy.value) {
    ElMessage.warning('商品已被拍下或已售出，无法下单')
    showOrderDialog.value = false
    return
  }
  if (!user.value) { ElMessage.warning('请先登录'); showOrderDialog.value = false; router.push('/login'); return }
  ordering.value = true
  try {
    await createOrder({ goodsId: goodsInfo.value.id, sellerId: goodsInfo.value.sellerId, buyerId: user.value.id })
    ElMessage.success('下单成功!')
    showOrderDialog.value = false
    router.push('/profile')
  } catch (err) { console.error('下单失败', err) } finally { ordering.value = false }
}

const submitComment = async () => {
  if (!commentContent.value.trim()) { ElMessage.warning('请输入评价内容'); return }
  if (!user.value) { ElMessage.warning('请先登录'); return }
  submittingComment.value = true
  try {
    await addComment({ orderId: commentableOrderId.value, fromUserId: user.value.id, rating: commentRating.value, content: commentContent.value.trim() })
    ElMessage.success('评价成功')
    commentContent.value = ''
    commentRating.value = 5
    await loadComments()
    canComment.value = false
    commentableOrderId.value = null
  } catch (err) { console.error('提交评价失败', err) } finally { submittingComment.value = false }
}

// 加载同类推荐（真实数据：同分类在售，排除当前商品）
const loadRelated = async () => {
  if (!goodsInfo.value || !goodsInfo.value.categoryId) return
  try {
    const res = await getGoodsList({ categoryId: goodsInfo.value.categoryId, pageNum: 1, pageSize: 12, sort: 'views' })
    const list = (res && res.list) ? res.list : (Array.isArray(res) ? res : [])
    relatedGoods.value = list.filter(g => g.id !== goodsInfo.value.id).slice(0, 8)
  } catch (err) { console.error('获取同类推荐失败', err) }
}

const loadGoodsDetail = async () => {
  const goodsId = route.params.id
  if (!goodsId) return
  loading.value = true
  try {
    const res = await getGoodsDetail(goodsId)
    goodsInfo.value = res
    if (user.value) {
      // 记录浏览历史（不阻塞页面渲染，失败静默，不影响详情展示）
      recordBrowse(goodsId).catch(() => {})
      try {
        const favStatus = await checkFavorite(user.value.id, goodsId)
        isFavorited.value = favStatus === true || favStatus === 1
      } catch (err) { console.error('检查收藏失败', err) }
    }
    await Promise.all([loadComments(), loadRelated()])
    checkCommentPermission()
    loadMarketPrice()
    loadPlatformCompare()
    loadPriceInsight()
    loadSellerRisk()
  } catch (err) { console.error('获取商品详情失败', err) } finally { loading.value = false }
}

// 风控：价格高于近期同类均价提示
const priceInsight = ref({})
const loadPriceInsight = async () => {
  if (!goodsInfo.value?.id) return
  try {
    priceInsight.value = (await getPriceInsight(goodsInfo.value.id)) || {}
  } catch (err) { priceInsight.value = {} }
}

// 风控：卖家平台黑名单提醒
const sellerRisk = ref({ blocked: false })
const loadSellerRisk = async () => {
  if (!goodsInfo.value?.sellerId) return
  try {
    sellerRisk.value = (await checkBlacklist(goodsInfo.value.sellerId)) || { blocked: false }
  } catch (err) { sellerRisk.value = { blocked: false } }
}

const checkCommentPermission = async () => {
  if (!user.value || !goodsInfo.value) { canComment.value = false; commentableOrderId.value = null; return }
  try {
    const orders = await getBuyerOrders(user.value.id, { pageSize: 100 })
    const orderList = (orders && orders.list) ? orders.list : (Array.isArray(orders) ? orders : [])
    const matched = orderList.find(o => o.goodsId === goodsInfo.value.id && o.status === 'completed' && !o.isCommented)
    if (matched) { commentableOrderId.value = matched.id; canComment.value = true }
    else { canComment.value = false; commentableOrderId.value = null }
  } catch (err) { console.error('检查评价权限失败', err); canComment.value = false }
}

const loadComments = async (reset = true) => {
  if (!goodsInfo.value) return
  if (reset) commentPageNum.value = 1
  try {
    const res = await getGoodsComments(goodsInfo.value.id, { pageNum: commentPageNum.value, pageSize: commentPageSize.value })
    commentList.value = (res && res.list) ? res.list : (Array.isArray(res) ? res : [])
    commentTotal.value = (res && typeof res.total === 'number') ? res.total : commentList.value.length
  } catch (err) { console.error('获取评价失败', err) }
}

const handleCommentPageChange = (page) => { commentPageNum.value = page; loadComments(false) }

// 校园同款比价
const similarLoading = ref(false)
const similarData = ref({ hasSimilar: false, items: [], priceTip: '', targetPrice: 0 })
const loadSimilar = async () => {
  if (!goodsInfo.value?.id) return
  similarLoading.value = true
  try {
    const res = await getCampusSimilar(goodsInfo.value.id)
    similarData.value = res || { hasSimilar: false, items: [] }
  } catch (e) {
    similarData.value = { hasSimilar: false, items: [] }
  } finally {
    similarLoading.value = false
  }
}

// 校园行情价 & 砍价助手
const marketData = ref({ hasData: false })
const loadMarketPrice = async () => {
  if (!goodsInfo.value?.id) return
  try {
    const res = await getMarketPrice(goodsInfo.value.id)
    marketData.value = res && res.hasData ? res : { hasData: false }
  } catch (e) {
    marketData.value = { hasData: false }
  }
}
// 当前价在行情区间中的位置（2%~98% 之间，避免贴边）
const pricePosition = computed(() => {
  const d = marketData.value
  if (!d.hasData) return 50
  const p25 = Number(d.p25)
  const p75 = Number(d.p75)
  const price = Number(d.price)
  if (p75 <= p25) return 50
  return Math.min(98, Math.max(2, ((price - p25) / (p75 - p25)) * 100))
})
const marketVerdictClass = computed(() => {
  const diff = Number(marketData.value.diffPct) || 0
  if (diff <= -10) return 'verdict-down'
  if (diff >= 10) return 'verdict-up'
  return 'verdict-mid'
})
const copyBargainTip = async () => {
  const tip = marketData.value.bargainTip
  if (!tip) return
  try {
    await navigator.clipboard.writeText(tip)
    ElMessage.success('砍价话术已复制，去联系卖家吧')
  } catch (e) {
    ElMessage.info(tip)
  }
}

// 多平台比价
const compareData = ref({ hasData: false })
const loadPlatformCompare = async () => {
  if (!goodsInfo.value?.id) return
  try {
    const res = await getPlatformCompare(goodsInfo.value.id)
    compareData.value = res && res.hasData ? res : { hasData: false }
  } catch (e) {
    compareData.value = { hasData: false }
  }
}
const compareVerdictClass = computed(() => {
  const d = compareData.value
  if (!d.hasData) return ''
  if (d.lowest) return 'cmp-good'
  const local = Number(d.localPrice) || 1
  return Number(d.saving) / local >= 0.1 ? 'cmp-bad' : 'cmp-warn'
})

// ==================== 换物模式 ====================
const isExchange = computed(() => !!(goodsInfo.value && goodsInfo.value.tradeMode === 'exchange'))
const showExchangeDialog = ref(false)
const exchangeCandidates = ref([])
const exchangeOfferId = ref(null)
const exchangeMessage = ref('')
const exchanging = ref(false)
const exchangeLoading = ref(false)
const selectedExchangeOffer = computed(() => exchangeCandidates.value.find(g => g.id === exchangeOfferId.value))

const openExchangeDialog = async () => {
  if (exchangeLoading.value) return
  if (!localStorage.getItem('token')) {
    ElMessage.warning('登录后才能申请换物')
    router.push('/login?redirect=/goods/' + goodsInfo.value.id)
    return
  }
  exchangeLoading.value = true
  try {
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    const res = await getMyGoods(user.id, { pageNum: 1, pageSize: 50 })
    const list = (res && res.list) ? res.list : (Array.isArray(res) ? res : [])
    exchangeCandidates.value = list.filter(g => g.status === 'on_sale' && g.id !== goodsInfo.value.id)
  } catch (e) {
    exchangeCandidates.value = []
    ElMessage.error('暂时无法读取你的在售商品，请稍后重试')
    return
  } finally {
    exchangeLoading.value = false
  }
  exchangeOfferId.value = null
  exchangeMessage.value = ''
  showExchangeDialog.value = true
}

const goPublishForExchange = () => {
  showExchangeDialog.value = false
  router.push('/publish')
}

const submitExchange = async () => {
  if (exchanging.value) return
  const offer = selectedExchangeOffer.value
  if (!offer) {
    ElMessage.warning('请选择你要拿来交换的商品')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定用「${offer.title}」交换「${goodsInfo.value.title}」吗？发送后需等待卖家处理，接受前可在「我的换物」中取消。`,
      '确认换物申请',
      { confirmButtonText: '确认发送', cancelButtonText: '再想想', type: 'info' }
    )
  } catch (e) {
    return
  }

  exchanging.value = true
  try {
    await applyExchange({
      goodsId: goodsInfo.value.id,
      offerGoodsId: offer.id,
      message: exchangeMessage.value.trim()
    })
    showExchangeDialog.value = false
    ElMessage.success('换物申请已发送，可在「我的 → 我的换物」查看进度')
  } catch (e) {
    // 拦截器已提示
  } finally {
    exchanging.value = false
  }
}

onMounted(() => {
  loadGoodsDetail()
  // 详情加载完再查同款，goodsInfo 需要先有值
  setTimeout(loadSimilar, 300)
})
</script>

<style scoped>
.jd-detail { min-height: 100vh; background: #f5f5f5; padding-bottom: 40px; }

/* ====== 顶部面包屑 ====== */
.jd-crumb { background: #fff; border-bottom: 1px solid #eee; position: sticky; top: 0; z-index: 100; }
.crumb-inner { max-width: 1200px; margin: 0 auto; height: 40px; display: flex; align-items: center; gap: 8px; padding: 0 16px; font-size: 12.5px; color: #999; }
.crumb-back, .crumb-link { cursor: pointer; color: #666; display: inline-flex; align-items: center; gap: 3px; }
.crumb-back:hover, .crumb-link:hover { color: #E1251B; }
.crumb-sep { color: #ddd; }
.crumb-arrow { font-size: 11px; color: #ccc; }
.crumb-current { color: #333; max-width: 360px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.crumb-share { margin-left: auto; cursor: pointer; color: #666; display: inline-flex; align-items: center; gap: 3px; }
.crumb-share:hover { color: #E1251B; }

/* ====== 主体三栏 ====== */
.jd-container { max-width: 1200px; margin: 12px auto 0; display: grid; grid-template-columns: 200px 1fr 360px; gap: 14px; padding: 0 16px; align-items: start; }

/* ====== 左栏 ====== */
.jd-left { display: flex; flex-direction: column; gap: 12px; }
.seller-card { background: #fff; border-radius: 4px; padding: 14px; }
.seller-avatar { width: 48px; height: 48px; border-radius: 50%; object-fit: cover; background: #f5f5f5; border: 1px solid #eee; }
.seller-info { margin-top: 8px; }
.seller-name { font-size: 14px; font-weight: 600; color: #222; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.seller-label { font-size: 11px; color: #999; margin-top: 2px; }
.shop-rating { margin-top: 8px; font-size: 11px; color: #999; display: flex; align-items: center; flex-wrap: wrap; }
.shop-rating b { color: #E1251B; font-weight: 600; }
.rate-sep { margin: 0 4px; color: #ddd; }
.seller-btns { display: flex; gap: 6px; margin-top: 10px; }
.enter-shop-btn { flex: 1; border-color: #E1251B; color: #E1251B; background: #fff; font-size: 12px; }
.enter-shop-btn:hover { background: #FEF0EF; border-color: #E1251B; color: #E1251B; }
.contact-btn { flex: 1; background: #E1251B; border-color: #E1251B; color: #fff; font-size: 12px; }
.contact-btn:hover { background: #C81E18; border-color: #C81E18; color: #fff; }

/* 卖家信用档案 */
.credit-profile { background: #fff; border-radius: 4px; padding: 14px; }
.cp-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.cp-level { font-size: 12px; font-weight: 700; padding: 2px 8px; border-radius: 10px; color: #fff; background: #999; }
.cp-level.level-gold { background: linear-gradient(90deg, #F59E0B, #E1251B); }
.cp-level.level-trust { background: #16A34A; }
.cp-level.level-normal { background: #6B7280; }
.cp-level.level-risk { background: #9CA3AF; }
.cp-credit { font-size: 12px; color: #999; }
.cp-credit b { color: #E1251B; font-size: 14px; }
.cp-stats { display: flex; margin-top: 12px; }
.cp-stat { flex: 1; text-align: center; border-right: 1px solid #f0f0f0; }
.cp-stat:last-child { border-right: none; }
.cp-stat b { display: block; font-size: 14px; color: #222; }
.cp-stat span { font-size: 11px; color: #999; }
.cp-safe { margin-top: 12px; padding-top: 10px; border-top: 1px dashed #f0f0f0; font-size: 11.5px; color: #B45309; line-height: 1.6; display: flex; align-items: flex-start; gap: 4px; }
.cp-safe-icon { flex-shrink: 0; margin-top: 2px; }

.related-box { background: #fff; border-radius: 4px; padding: 12px; }
.related-title { font-size: 14px; font-weight: 600; color: #222; padding-bottom: 8px; border-bottom: 1px solid #f0f0f0; margin-bottom: 8px; }
.related-list { display: flex; flex-direction: column; gap: 10px; }
.related-item { display: flex; gap: 8px; cursor: pointer; padding: 4px; border-radius: 4px; transition: background 0.15s; }
.related-item:hover { background: #fafafa; }
.related-img { width: 64px; height: 64px; object-fit: cover; border-radius: 3px; background: #f5f5f5; flex-shrink: 0; }
.related-info { flex: 1; min-width: 0; display: flex; flex-direction: column; justify-content: center; }
.related-name { font-size: 12px; color: #333; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.related-item:hover .related-name { color: #E1251B; }
.related-price { font-size: 14px; font-weight: 700; color: #E1251B; margin-top: 4px; }

.empty-tip { display: flex; flex-direction: column; align-items: center; padding: 24px 0; color: #ccc; font-size: 13px; }
.empty-tip p { margin-top: 8px; }

/* ====== 中栏 ====== */
.jd-center { min-width: 0; }
.gallery { display: flex; gap: 12px; background: #fff; border-radius: 4px; padding: 14px; }
.thumb-col { display: flex; flex-direction: column; gap: 8px; width: 60px; flex-shrink: 0; max-height: 480px; overflow-y: auto; }
.thumb-col::-webkit-scrollbar { width: 4px; }
.thumb-col::-webkit-scrollbar-thumb { background: #ddd; border-radius: 2px; }
.thumb-item { width: 60px; height: 60px; border-radius: 3px; overflow: hidden; border: 2px solid transparent; cursor: pointer; transition: border-color 0.2s, transform 0.2s; }
.thumb-item img { width: 100%; height: 100%; object-fit: cover; display: block; }
.thumb-item:hover { border-color: #f3a29d; transform: translateY(-1px); }
.thumb-item.active { border-color: #E1251B; box-shadow: 0 0 0 2px rgba(225, 37, 27, 0.12); }
.main-image-wrap { position: relative; flex: 1; aspect-ratio: 1; background: #fff; overflow: hidden; display: flex; align-items: center; justify-content: center; border-radius: 4px; cursor: zoom-in; }
.main-image { width: 100%; height: 100%; object-fit: cover; transition: transform 0.25s ease; will-change: transform; }
.main-image-wrap.is-zoomed .main-image { transform: scale(2); }
.image-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: #f5f5f5; }
.img-arrow { position: absolute; top: 50%; transform: translateY(-50%); width: 34px; height: 34px; background: rgba(0,0,0,.35); color: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; cursor: pointer; z-index: 3; transition: background 0.2s; }
.img-arrow:hover { background: rgba(225, 37, 27, .85); }
.img-arrow:active { background: rgba(0,0,0,.55); }
.img-prev { left: 10px; } .img-next { right: 10px; }
.img-indicator { position: absolute; right: 12px; bottom: 12px; background: rgba(0,0,0,.5); color: #fff; font-size: 12px; padding: 2px 8px; border-radius: 10px; z-index: 3; }
.zoom-tip { position: absolute; left: 12px; bottom: 12px; z-index: 3; padding: 3px 10px; background: rgba(0, 0, 0, .45); color: #fff; font-size: 12px; border-radius: 10px; opacity: 0.9; transition: opacity 0.2s ease; pointer-events: none; }
.main-image-wrap.is-zoomed .zoom-tip { opacity: 0; }
.img-fullscreen { position: absolute; top: 12px; right: 12px; z-index: 3; width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; border: none; border-radius: 4px; background: rgba(0, 0, 0, .45); color: #fff; cursor: pointer; opacity: 0; transition: opacity 0.2s ease, background 0.2s ease; }
.main-image-wrap:hover .img-fullscreen { opacity: 1; }
.img-fullscreen:hover { background: #E1251B; }

/* Tab 栏 */
.tab-bar { display: flex; background: #fff; margin-top: 12px; border-radius: 4px; border-bottom: 1px solid #f0f0f0; padding: 0 12px; }
.tab-item { padding: 13px 20px; font-size: 14px; color: #666; cursor: pointer; position: relative; }
.tab-item.active { color: #E1251B; font-weight: 600; }
.tab-item.active::after { content: ''; position: absolute; bottom: 0; left: 50%; transform: translateX(-50%); width: 28px; height: 3px; background: #E1251B; border-radius: 2px; }
.tab-count { color: inherit; }

/* Tab 面板 */
.tab-panels { background: #fff; border-radius: 4px; margin-top: 10px; padding: 18px; min-height: 200px; }
.panel-title { font-size: 16px; font-weight: 600; color: #222; margin: 0 0 12px; }
.goods-desc { font-size: 14px; color: #555; line-height: 1.9; white-space: pre-wrap; word-break: break-all; }

/* 参数表 */
.params-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.params-table tr { border-bottom: 1px solid #f5f5f5; }
.params-table td { padding: 12px 8px; }
.params-label { color: #999; width: 90px; }
.params-value { color: #333; }

/* 售后 */
.service-list { margin: 0; padding-left: 18px; font-size: 13px; color: #555; line-height: 2.1; }
.service-list b { color: #E1251B; }

/* 评价 */
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.comment-count { font-size: 12px; color: #999; }
.pagination-wrap { display: flex; justify-content: center; padding: 12px 0 4px; }
.comment-item { display: flex; gap: 12px; padding: 14px 0; border-bottom: 1px solid #f5f5f5; }
.comment-item:last-child { border-bottom: none; }
.comment-avatar { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; background: #f5f5f5; flex-shrink: 0; }
.comment-body { flex: 1; min-width: 0; }
.comment-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.comment-user { font-size: 13px; font-weight: 500; color: #333; }
.comment-stars { display: flex; gap: 1px; }
.comment-stars .el-icon { font-size: 12px; }
.comment-content { font-size: 14px; color: #555; line-height: 1.6; margin-bottom: 6px; }
.comment-time { font-size: 12px; color: #bbb; }
.comment-input-wrap { margin-top: 14px; padding-top: 14px; border-top: 1px solid #f0f0f0; }
.comment-input :deep(.el-textarea__inner) { border-radius: 6px; border-color: #ddd; }
.comment-input :deep(.el-textarea__inner:focus) { border-color: #E1251B; }
.comment-stars-input { display: flex; align-items: center; gap: 4px; margin: 10px 0; }
.stars-label { font-size: 14px; color: #666; margin-right: 4px; }
.star-icon { font-size: 20px; cursor: pointer; transition: transform 0.15s; }
.star-icon:active { transform: scale(1.3); }
.submit-comment-btn { width: 100%; background: linear-gradient(180deg, #E1251B, #C81E18); border-color: #E1251B; border-radius: 6px; height: 40px; font-size: 14px; }
.submit-comment-btn:hover { background: linear-gradient(180deg, #C81E18, #A81813); border-color: #C81E18; }

/* ========== 校园同款比价 ========== */
.similar-empty { text-align: center; padding: 40px 20px; color: #999; }
.similar-empty small { display: block; margin-top: 8px; color: #bbb; font-size: 12px; }
.similar-tip {
  background: linear-gradient(135deg, #FEF3C7, #FEF9C3);
  border: 1px solid #FDE68A;
  color: #92400E;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 14px;
}
.similar-list { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.similar-item {
  display: flex; gap: 10px; padding: 10px; background: #fafafa; border: 1px solid #eee;
  border-radius: 8px; cursor: pointer; transition: all 0.2s;
}
.similar-item:hover { background: #fff; border-color: #E1251B; box-shadow: 0 2px 8px rgba(225,37,27,0.1); }
.sim-img-wrap { width: 80px; height: 80px; flex-shrink: 0; position: relative; border-radius: 6px; overflow: hidden; background: #f0f0f0; }
.sim-img { width: 100%; height: 100%; object-fit: cover; }
.sim-cheaper {
  position: absolute; bottom: 2px; left: 2px;
  background: #E1251B; color: #fff; font-size: 10px; padding: 1px 5px;
  border-radius: 3px; font-weight: 500;
}
.sim-info { flex: 1; min-width: 0; display: flex; flex-direction: column; justify-content: space-between; }
.sim-title { font-size: 12px; color: #333; line-height: 1.3; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.sim-meta { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.sim-price { font-size: 14px; font-weight: 700; color: #E1251B; }
.sim-seller { font-size: 11px; color: #999; }
.sim-tag { font-size: 10px; color: #F59E0B; }
.sim-views { font-size: 11px; color: #bbb; }

/* ====== 右栏 ====== */
.jd-right { position: sticky; top: 52px; }
.info-card { background: #fff; border-radius: 4px; padding: 16px 18px; }
.goods-title { font-size: 17px; font-weight: 600; color: #222; line-height: 1.5; margin: 0 0 8px; }

/* 标题下方标签 */
.title-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 12px; }
.title-tag { display: inline-flex; align-items: center; gap: 2px; padding: 2px 8px; background: #f5f5f5; border-radius: 10px; font-size: 11px; color: #666; line-height: 1.6; }
.title-tag.tag-orange { background: #FFF3E8; color: #E67A18; }
.title-tag.tag-green { background: #EDF9F1; color: #12A150; }
.title-tag.tag-blue { background: #EFF6FF; color: #2563EB; }
.title-tag.tag-exchange { background: #F5F3FF; color: #7C3AED; }
.title-tag.tag-status { background: #FFF4E5; color: #C77700; font-weight: 600; }

.price-box { background: linear-gradient(180deg, #fff5f4, #fff); border-radius: 4px; padding: 12px; margin-bottom: 12px; }
.price-row { display: flex; align-items: baseline; gap: 6px; flex-wrap: wrap; }
.jd-tag { display: inline-block; background: #E1251B; color: #fff; font-size: 11px; font-weight: 500; padding: 2px 6px; border-radius: 2px; line-height: 1.4; }
.jd-tag.jd-tag-exchange { background: #7C3AED; }
.exchange-price { font-size: 26px !important; color: #7C3AED !important; }
.price-symbol { font-size: 14px; font-weight: 700; color: #E1251B; }
.price-value { font-size: 34px; font-weight: 800; color: #E1251B; line-height: 1; font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Arial, sans-serif; }
.original-price { font-size: 13px; color: #999; text-decoration: line-through; }
.discount-row { display: flex; align-items: center; gap: 8px; margin-top: 6px; }
.discount-tag { display: inline-block; background: #E1251B; color: #fff; font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 10px; }
.save-tip { font-size: 12px; color: #E1251B; }

/* 涨价返券：标价划线 + 随单优惠券 */
.coupon-row { display: flex; align-items: center; gap: 8px; margin-top: 8px; flex-wrap: wrap; }
.coupon-chip { display: inline-block; background: linear-gradient(135deg, #FF6A00, #E1251B); color: #fff; font-size: 11px; font-weight: 700; padding: 2px 9px; border-radius: 10px; }
.coupon-cut { font-size: 13px; font-weight: 700; color: #E1251B; }
.coupon-note { font-size: 11px; color: #9CA3AF; }

.order-coupon-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 2px;
  margin-top: 8px;
  border-top: 1px dashed #F0F0F0;
  font-size: 12px;
  color: #6B7280;
}
.order-coupon-cut { color: #E1251B; font-weight: 700; }
.order-coupon-note { color: #9CA3AF; font-size: 11px; }

.info-rows { border-top: 1px dashed #eee; padding-top: 6px; }
.info-row { display: flex; padding: 9px 0; font-size: 13px; border-bottom: 1px solid #f7f7f7; }
.info-row:last-child { border-bottom: none; }
.info-label { color: #999; width: 72px; flex-shrink: 0; }
.info-val { color: #333; flex: 1; display: flex; align-items: center; gap: 4px; flex-wrap: wrap; }
.delivery-tip { color: #E1251B; font-size: 12px; }
.svc-tags { display: flex; gap: 6px; flex-wrap: wrap; }
.svc-tag { font-size: 11px; color: #666; background: #f5f5f5; border: 1px solid #eee; border-radius: 3px; padding: 2px 8px; }

/* 校园行情价 & 砍价助手 */
.market-card { background: #fff; border-radius: 4px; padding: 14px 18px; margin-top: 12px; }
.market-head { display: flex; align-items: center; justify-content: space-between; }
.market-title { font-size: 13px; font-weight: 700; color: #222; }
.market-verdict { font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 10px; }
.market-verdict.verdict-down { color: #16A34A; background: #ECFDF3; }
.market-verdict.verdict-up { color: #E1251B; background: #FEF0EF; }
.market-verdict.verdict-mid { color: #6B7280; background: #F3F4F6; }
.market-range { display: flex; align-items: center; gap: 8px; margin-top: 12px; }
.range-num { font-size: 13px; font-weight: 700; color: #222; }
.range-bar { flex: 1; height: 6px; border-radius: 3px; background: linear-gradient(90deg, #FDBA74, #F59E0B, #E1251B); position: relative; }
.range-dot { position: absolute; top: 50%; width: 12px; height: 12px; border-radius: 50%; background: #fff; border: 2px solid #E1251B; transform: translate(-50%, -50%); box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2); }
.market-meta { margin-top: 8px; font-size: 11.5px; color: #999; line-height: 1.6; }
.market-offer { display: flex; align-items: center; justify-content: space-between; margin-top: 10px; padding-top: 10px; border-top: 1px dashed #f0f0f0; }
.offer-left { display: flex; align-items: baseline; gap: 6px; flex-wrap: wrap; }
.offer-label { font-size: 12px; color: #999; }
.offer-price { font-size: 20px; font-weight: 800; color: #E1251B; }
.offer-note { font-size: 11px; color: #bbb; }
.offer-copy { border: 1px solid #E1251B; background: #fff; color: #E1251B; font-size: 12px; border-radius: 14px; padding: 5px 12px; cursor: pointer; transition: all 0.2s; flex-shrink: 0; }
.offer-copy:hover { background: #FEF0EF; }

/* 多平台比价 */
.compare-card { background: #fff; border-radius: 4px; padding: 14px 18px; margin-top: 12px; }
.compare-head { display: flex; align-items: center; justify-content: space-between; }
.compare-title { font-size: 13px; font-weight: 700; color: #222; }
.compare-verdict { font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 10px; }
.cmp-good { color: #16A34A; background: #ECFDF3; }
.cmp-bad { color: #E1251B; background: #FEF0EF; }
.cmp-warn { color: #D97706; background: #FFFBEB; }
.compare-tip { margin-top: 8px; font-size: 12px; color: #666; line-height: 1.6; }
.compare-list { margin-top: 10px; border-top: 1px dashed #f0f0f0; padding-top: 6px; }
.compare-row { display: flex; align-items: center; gap: 9px; padding: 7px 0; }
.pf-logo { width: 26px; height: 26px; border-radius: 7px; color: #fff; font-size: 12px; font-weight: 700; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.pf-xianyu { background: #FFB300; }
.pf-zhuanzhuan { background: #E1251B; }
.pf-aihuishou { background: #00B578; }
.pf-taobao { background: #FF6A00; }
.pf-info { flex: 1; min-width: 0; }
.pf-name-row { display: flex; align-items: center; gap: 6px; }
.pf-name { font-size: 12.5px; font-weight: 600; color: #333; }
.pf-tag { font-size: 10px; color: #999; background: #F5F5F5; padding: 1px 6px; border-radius: 8px; }
.pf-range { margin-top: 2px; font-size: 11px; color: #aaa; }
.pf-right { text-align: right; }
.pf-price { display: block; font-size: 14px; font-weight: 700; color: #222; }
.pf-diff { font-size: 10.5px; }
.diff-down { color: #16A34A; }
.diff-up { color: #E1251B; }
.pf-note { color: #B0B4BA; }
.compare-foot { margin-top: 8px; padding-top: 8px; border-top: 1px dashed #f0f0f0; font-size: 10.5px; color: #aaa; }

/* 操作栏 */
.action-bar { display: flex; align-items: center; gap: 8px; background: #fff; border-radius: 4px; padding: 12px; margin-top: 12px; }

.escrow-hint {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 12px;
  padding: 9px 12px;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: #1D4ED8;
  background: #EFF6FF;
  border: 1px solid #DBEAFE;
}

.es-icon { font-size: 14px; }

/* 换物弹窗 */
.exchange-form { padding: 2px; }
.ex-flow { margin-bottom: 14px; }
.ex-flow :deep(.el-alert__title) { font-size: 12px; line-height: 1.6; }
.ex-target { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.ex-img { width: 54px; height: 54px; border-radius: 8px; object-fit: cover; background: #F5F5F5; }
.ex-target-title { font-size: 13px; font-weight: 600; color: #1F2937; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.ex-target-sub { margin-top: 3px; font-size: 11.5px; color: #9CA3AF; }
.ex-empty { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-top: 8px; font-size: 12px; color: #B45309; }
.ex-selected { margin-top: 10px; padding: 9px 11px; border-radius: 6px; background: #F5F3FF; color: #6D28D9; font-size: 12px; line-height: 1.6; }
.ex-msg { margin-top: 12px; }
.ex-cash-tip { margin-top: 8px; color: #9CA3AF; font-size: 11.5px; line-height: 1.6; }
.bar-icon-btn { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px; cursor: pointer; padding: 4px 10px; min-width: 46px; transition: transform 0.15s; }
.bar-icon-btn:active { transform: scale(0.92); }
.icon-label { font-size: 11px; line-height: 1.2; }
.btn-want { flex: 1; height: 42px; border: 1px solid #E1251B; background: #fff; color: #E1251B; border-radius: 21px; font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.btn-want:active { background: #FEF0EF; }
.btn-buy { flex: 1; height: 42px; border: none; background: linear-gradient(180deg, #E1251B, #C81E18); color: #fff; border-radius: 21px; font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.btn-buy:active { opacity: 0.9; transform: scale(0.98); }
.btn-want:disabled,
.btn-buy:disabled {
  background: #E5E5E5;
  border-color: #E5E5E5;
  color: #999;
  cursor: not-allowed;
  box-shadow: none;
}

/* ====== 加载中 ====== */
.loading-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 120px 20px; color: #ccc; }
.loading-state p { margin-top: 12px; font-size: 14px; }
.loading-spin { animation: spin 1s linear infinite; color: #E1251B; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

/* ====== 下单弹窗 ====== */
.order-dialog :deep(.el-dialog) { border-radius: 8px; overflow: hidden; }
.order-dialog :deep(.el-dialog__header) { text-align: center; padding: 16px 20px; border-bottom: 1px solid #f0f0f0; margin-right: 0; }
.order-dialog :deep(.el-dialog__title) { font-size: 16px; font-weight: 600; color: #222; }
.order-dialog :deep(.el-dialog__body) { padding: 16px 20px; }
.order-dialog :deep(.el-dialog__footer) { padding: 12px 20px 16px; border-top: 1px solid #f0f0f0; }
.order-dialog-body { padding: 0; }
.order-goods-preview { display: flex; gap: 12px; padding: 12px; background: #f9f9f9; border-radius: 6px; margin-bottom: 16px; }
.order-thumb { width: 64px; height: 64px; border-radius: 4px; object-fit: cover; flex-shrink: 0; background: #f0f0f0; }
.order-goods-info { flex: 1; display: flex; flex-direction: column; justify-content: center; min-width: 0; }
.order-goods-title { font-size: 14px; color: #333; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 4px; }
.order-goods-price { font-size: 16px; font-weight: 700; color: #E1251B; }
.order-price-row { display: flex; align-items: center; justify-content: space-between; padding-top: 12px; border-top: 1px dashed #eee; font-size: 14px; color: #666; }
.price-label-text { color: #666; }
.order-total { font-size: 20px; font-weight: 800; color: #E1251B; }
.order-dialog-footer { display: flex; gap: 10px; justify-content: flex-end; }
.cancel-btn { border-color: #ddd; color: #666; background: #fff; border-radius: 6px; }
.cancel-btn:hover { border-color: #ccc; color: #333; }
.order-confirm-btn { background: linear-gradient(180deg, #E1251B, #C81E18); border-color: #E1251B; min-width: 100px; border-radius: 6px; }
.order-confirm-btn:hover { background: linear-gradient(180deg, #C81E18, #A81813); border-color: #C81E18; }

/* ====== 风控提示：价格异常 / 卖家失信 ====== */
.risk-price-tip {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  background: #fff5f4;
  border: 1px solid #f7d5d2;
  color: #c0392b;
  font-size: 12.5px;
  line-height: 1.6;
}
.risk-price-tip.soft {
  background: #fffaf0;
  border-color: #f5e3bd;
  color: #a8761c;
}
.rpt-icon {
  flex-shrink: 0;
  width: 15px;
  height: 15px;
  margin-top: 1px;
  border-radius: 50%;
  background: #E1251B;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.risk-price-tip.soft .rpt-icon { background: #d99a1f; }

.blacklist-warning {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #fff1f0;
  border: 1px solid #ffccc7;
  color: #a8071a;
  font-size: 12.5px;
  line-height: 1.6;
}
.bw-icon {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #cf1322;
  color: #fff;
  font-weight: 700;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.bw-body b { display: block; margin-bottom: 2px; color: #820014; }

/* ====== 响应式：小屏降级为两栏/单栏 ====== */
@media (max-width: 1100px) {
  .jd-container { grid-template-columns: 1fr 340px; }
  .jd-left { display: none; }
}
@media (max-width: 760px) {
  .jd-container { grid-template-columns: 1fr; }
  .jd-right { position: static; }
  .crumb-current { max-width: 140px; }
}
</style>
