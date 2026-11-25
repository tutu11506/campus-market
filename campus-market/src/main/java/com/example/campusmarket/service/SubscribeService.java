package com.example.campusmarket.service;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.config.RealtimeHub;
import com.example.campusmarket.entity.Goods;
import com.example.campusmarket.entity.GoodsWatch;
import com.example.campusmarket.entity.KeywordSubscription;
import com.example.campusmarket.entity.NotifyRecord;
import com.example.campusmarket.entity.TextbookWish;
import com.example.campusmarket.entity.User;
import com.example.campusmarket.mapper.SubscriptionMapper;
import com.example.campusmarket.mapper.UserMapper;
import com.example.campusmarket.utils.GradeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息提醒与个性化订阅。
 *
 * 1) 心愿单降价：收藏商品写入 goods_watch，改价后降价推送；
 * 2) 教材上新：教材心愿命中新发布商品即推送；
 * 3) 关键词订阅：新商品标题/描述命中订阅词即推送；
 * 4) 学期定时提醒：每天检查一次开学季/毕业季，按用户去重推送。
 */
@Service
public class SubscribeService {

    private static final Logger log = LoggerFactory.getLogger(SubscribeService.class);

    /** 单个用户最多订阅的关键词数，避免刷屏 */
    private static final int MAX_KEYWORDS = 20;

    private static volatile String lastSemesterScan = null;
    private static volatile int lastSemesterSent = 0;

    @Autowired
    private SubscriptionMapper subscriptionMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RealtimeHub realtimeHub;

    /* ==================== 关键词订阅 ==================== */

    public List<KeywordSubscription> listKeywords(Integer uid) {
        requireLogin(uid);
        return subscriptionMapper.findKeywords(uid);
    }

    public String addKeyword(Integer uid, String keyword) {
        requireLogin(uid);
        String kw = keyword == null ? null : keyword.trim();
        if (kw == null || kw.length() < 2) {
            throw new IllegalArgumentException("关键词至少 2 个字");
        }
        if (kw.length() > 20) {
            throw new IllegalArgumentException("关键词最多 20 个字");
        }
        if (subscriptionMapper.countKeywords(uid) >= MAX_KEYWORDS) {
            throw new IllegalArgumentException("最多订阅 " + MAX_KEYWORDS + " 个关键词，请先删除部分订阅");
        }
        if (subscriptionMapper.insertKeyword(uid, kw) == 0) {
            throw new IllegalArgumentException("已经订阅过该关键词");
        }
        return "订阅成功，平台上新带「" + kw + "」的商品会第一时间提醒你";
    }

    public String deleteKeyword(Integer uid, Integer id) {
        requireLogin(uid);
        if (subscriptionMapper.deleteKeyword(id, uid) == 0) {
            throw new IllegalArgumentException("订阅不存在或无权删除");
        }
        return "已取消订阅";
    }

    /* ==================== 心愿商品（降价提醒） ==================== */

    /** 收藏时自动关注；失败不影响收藏主流程 */
    public void watchGoods(Integer uid, Integer goodsId) {
        if (uid == null || goodsId == null) return;
        try {
            subscriptionMapper.watchFromGoods(uid, goodsId);
        } catch (Exception e) {
            log.warn("写入心愿关注失败 user={} goods={}：{}", uid, goodsId, e.getMessage());
        }
    }

    public void unwatchGoods(Integer uid, Integer goodsId) {
        if (uid == null || goodsId == null) return;
        try {
            subscriptionMapper.unwatch(uid, goodsId);
        } catch (Exception e) {
            log.warn("取消心愿关注失败 user={} goods={}：{}", uid, goodsId, e.getMessage());
        }
    }

    public List<GoodsWatch> listWatches(Integer uid) {
        requireLogin(uid);
        return subscriptionMapper.findWatches(uid);
    }

    /* ==================== 教材心愿（上新提醒） ==================== */

    public List<TextbookWish> listWishes(Integer uid) {
        requireLogin(uid);
        return subscriptionMapper.findWishes(uid);
    }

    public String addWish(Integer uid, Integer textbookId) {
        requireLogin(uid);
        if (textbookId == null) {
            throw new IllegalArgumentException("请选择教材");
        }
        if (subscriptionMapper.wishTextbook(uid, textbookId) == 0) {
            throw new IllegalArgumentException("已经关注过这本教材");
        }
        return "已关注，这本教材有新闲置上架会立刻提醒你";
    }

    public String deleteWish(Integer uid, Integer id) {
        requireLogin(uid);
        if (subscriptionMapper.deleteWish(id, uid) == 0) {
            throw new IllegalArgumentException("心愿不存在或无权删除");
        }
        return "已取消教材心愿";
    }

    public boolean hasWish(Integer uid, Integer textbookId) {
        if (uid == null || textbookId == null) return false;
        return subscriptionMapper.hasWish(uid, textbookId) > 0;
    }

    /* ==================== 商品发布 / 改价钩子 ==================== */

    /**
     * 新商品发布后：匹配关键词订阅 + 教材心愿，命中即推送。
     * 推送失败不影响发布主流程。
     */
    public void onGoodsPublished(Goods goods) {
        if (goods == null || goods.getId() == null || goods.getSellerId() == null) return;
        try {
            String title = goods.getTitle() == null ? "" : goods.getTitle();
            String desc = goods.getDescription() == null ? "" : goods.getDescription();
            String text = title + " " + desc;

            // 1) 关键词订阅
            for (KeywordSubscription sub : subscriptionMapper.matchKeywords(text, goods.getSellerId())) {
                if (subscriptionMapper.countKeywordNotify(sub.getUserId(), "new_goods",
                        goods.getId(), sub.getKeyword()) > 0) {
                    continue;
                }
                NotifyRecord r = buildRecord(sub.getUserId(), "new_goods",
                        "「" + sub.getKeyword() + "」有新上架",
                        "你订阅的关键词「" + sub.getKeyword() + "」命中新商品「" + title
                                + "」，售价 ¥" + price(goods.getPrice()) + "，点击查看详情。",
                        "/goods/" + goods.getId(), goods.getId(), null, sub.getKeyword(), goods.getPrice());
                push(r);
                subscriptionMapper.markKeywordMatched(sub.getId());
            }

            // 2) 教材心愿
            for (TextbookWish wish : subscriptionMapper.matchTextbookWishes(
                    goods.getTextbookId(), title, goods.getSellerId())) {
                if (subscriptionMapper.countGoodsNotify(wish.getUserId(), "textbook_new", goods.getId()) > 0) {
                    continue;
                }
                String book = wish.getTextbookTitle() != null ? wish.getTextbookTitle()
                        : (wish.getTitle() != null ? wish.getTitle() : "你关注的教材");
                NotifyRecord r = buildRecord(wish.getUserId(), "textbook_new",
                        "想要的教材上新了",
                        "你关注的教材《" + book + "》有新的闲置上架：「" + title
                                + "」，售价 ¥" + price(goods.getPrice()) + "，先到先得。",
                        "/goods/" + goods.getId(), goods.getId(), wish.getTextbookId(), null, goods.getPrice());
                push(r);
                subscriptionMapper.markWishMatched(wish.getId());
            }
        } catch (Exception e) {
            log.warn("商品上新提醒推送失败 goods={}：{}", goods.getId(), e.getMessage());
        }
    }

    /**
     * 商品改价后：对收藏该商品的用户做降价提醒。
     * 每次价格变化都会更新 last_price；只有当新价格低于上次记录价才推送。
     */
    public void onGoodsPriceChanged(Goods before, Goods after) {
        if (before == null || after == null || after.getId() == null) return;
        BigDecimal oldPrice = before.getPrice();
        BigDecimal newPrice = after.getPrice();
        if (oldPrice == null || newPrice == null) return;
        try {
            for (GoodsWatch watch : subscriptionMapper.findWatchers(after.getId())) {
                if (watch.getUserId() == null || watch.getUserId().equals(after.getSellerId())) {
                    continue;
                }
                BigDecimal last = watch.getLastPrice() == null ? oldPrice : watch.getLastPrice();
                boolean drop = newPrice.compareTo(last) < 0;
                if (!drop) {
                    subscriptionMapper.updateWatchPrice(watch.getId(), newPrice);
                    continue;
                }
                if (subscriptionMapper.countPriceNotify(watch.getUserId(), "price_drop",
                        after.getId(), newPrice) == 0) {
                    String title = after.getTitle() == null ? "你收藏的商品" : after.getTitle();
                    NotifyRecord r = buildRecord(watch.getUserId(), "price_drop",
                            "收藏的商品降价了",
                            "你收藏的「" + title + "」从 ¥" + price(last) + " 降到 ¥" + price(newPrice)
                                    + "，降幅 ¥" + price(last.subtract(newPrice)) + "，点击查看。",
                            "/goods/" + after.getId(), after.getId(), null, null, newPrice);
                    push(r);
                }
                subscriptionMapper.updateWatchPriceNotified(watch.getId(), newPrice);
            }
        } catch (Exception e) {
            log.warn("降价提醒推送失败 goods={}：{}", after.getId(), e.getMessage());
        }
    }

    /* ==================== 学期定时提醒 ==================== */

    /** 每天 9:00 自动检查一次开学季/毕业季，首启动延迟 2 分钟 */
    @Scheduled(initialDelay = 120_000, fixedDelay = 24 * 60 * 60 * 1000)
    public void scheduledSemesterReminder() {
        try {
            runSemesterScan(false);
        } catch (Exception e) {
            log.warn("学期提醒定时扫描失败：{}", e.getMessage());
        }
    }

    /**
     * 扫描全部用户发送学期提醒。
     *
     * @param force true=忽略当前日期强制演练（管理端手动触发），false=仅在开学季/毕业季窗口执行
     */
    public Map<String, Object> runSemesterScan(boolean force) {
        String phase = GradeUtil.currentPhase();
        boolean inTerm = "summer_new".equals(phase) || "spring_new".equals(phase);
        boolean inGrad = "graduation".equals(phase);
        int sent = 0;

        if (force || inTerm || inGrad) {
            int year = LocalDate.now().getYear();
            // 1-6 月归入春季学期，7-12 月归入秋季学期；force 演练时按当前月份推断
            boolean springTerm = "spring_new".equals(phase)
                    || (force && LocalDate.now().getMonthValue() <= 6);
            String termKey = springTerm ? "spring-" + year : "autumn-" + year;
            String gradKey = "grad-" + year;
            for (User u : userMapper.findAll()) {
                if (u == null || "ban".equals(u.getStatus())) continue;
                if (force || inTerm) {
                    sent += sendSemesterStart(u.getId(), termKey);
                }
                if ((force || inGrad) && GradeUtil.isGraduationSale(u.getStudentNo())) {
                    sent += sendGraduation(u.getId(), gradKey);
                }
            }
        }

        lastSemesterScan = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        lastSemesterSent = sent;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("phase", phase);
        result.put("force", force);
        result.put("sent", sent);
        result.put("scanTime", lastSemesterScan);
        return result;
    }

    /** 用户主动检查：仅在开学季/毕业季窗口内为本人推送一次 */
    public String checkForCurrentUser(Integer uid) {
        requireLogin(uid);
        String phase = GradeUtil.currentPhase();
        int year = LocalDate.now().getYear();
        int sent = 0;
        if ("summer_new".equals(phase) || "spring_new".equals(phase)) {
            String key = "spring_new".equals(phase) ? "spring-" + year : "autumn-" + year;
            sent += sendSemesterStart(uid, key);
        } else if ("graduation".equals(phase)) {
            sent += sendGraduation(uid, "grad-" + year);
        } else {
            return "当前不在开学/毕业提醒时段（开学季：1.15-2.15、8.15-9.15；毕业季：5.15-6.15）";
        }
        return sent > 0 ? "已为你推送本期提醒，请到提醒记录查看" : "本期提醒已经发送过，无需重复接收";
    }

    private int sendSemesterStart(Integer uid, String periodKey) {
        if (subscriptionMapper.insertSemesterLog(uid, "semester_start", periodKey) == 0) return 0;
        push(buildRecord(uid, "semester_start", "🎒 开学季教材采购提醒",
                "开学在即，别忘了备齐新学期教材：教材专区支持按课表一键匹配缺书，"
                        + "订阅关键词后，平台上新会第一时间提醒你。",
                "/textbook", null, null, null, null));
        return 1;
    }

    private int sendGraduation(Integer uid, String periodKey) {
        if (subscriptionMapper.insertSemesterLog(uid, "graduation", periodKey) == 0) return 0;
        push(buildRecord(uid, "graduation", "🎓 毕业清仓提醒",
                "毕业季到了：把闲置教材、宿舍用品一键发布，平台会给你的商品打上「毕业清仓」标签，"
                        + "让学弟学妹更快找到你。",
                "/publish", null, null, null, null));
        return 1;
    }

    /* ==================== 提醒记录 ==================== */

    public PageResult<NotifyRecord> pageNotifications(Integer uid, Integer pageNum, Integer pageSize) {
        requireLogin(uid);
        int[] p = PageResult.normalize(pageNum, pageSize, 50);
        List<NotifyRecord> list = subscriptionMapper.findNotifyPage(uid, p[2], p[1]);
        int total = subscriptionMapper.countNotify(uid);
        return PageResult.of(list, total, p[0], p[1]);
    }

    public int unreadCount(Integer uid) {
        if (uid == null) return 0;
        return subscriptionMapper.countUnread(uid);
    }

    public String markRead(Integer uid, Integer id) {
        requireLogin(uid);
        subscriptionMapper.markRead(id, uid);
        return "已读";
    }

    public String markAllRead(Integer uid) {
        requireLogin(uid);
        int n = subscriptionMapper.markAllRead(uid);
        return "已全部标记为已读（" + n + " 条）";
    }

    public String deleteNotification(Integer uid, Integer id) {
        requireLogin(uid);
        if (subscriptionMapper.deleteNotify(id, uid) == 0) {
            throw new IllegalArgumentException("提醒不存在或无权删除");
        }
        return "已删除";
    }

    public Map<String, Object> summary(Integer uid) {
        requireLogin(uid);
        String phase = GradeUtil.currentPhase();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("unread", subscriptionMapper.countUnread(uid));
        data.put("keywords", subscriptionMapper.countKeywords(uid));
        data.put("wishes", subscriptionMapper.countWishes(uid));
        data.put("watches", subscriptionMapper.countWatches(uid));
        data.put("phase", phase);
        data.put("phaseDesc", GradeUtil.phaseDescription(phase));
        return data;
    }

    /* ==================== 内部工具 ==================== */

    private NotifyRecord buildRecord(Integer userId, String type, String title, String content,
                                     String link, Integer goodsId, Integer textbookId,
                                     String keyword, BigDecimal refPrice) {
        NotifyRecord r = new NotifyRecord();
        r.setUserId(userId);
        r.setNotifyType(type);
        r.setTitle(title);
        r.setContent(content);
        r.setLink(link);
        r.setGoodsId(goodsId);
        r.setTextbookId(textbookId);
        r.setKeyword(keyword);
        r.setRefPrice(refPrice);
        r.setIsRead(0);
        return r;
    }

    /** 落库 + 实时推送（用户在线时侧栏红点即时刷新） */
    private void push(NotifyRecord r) {
        subscriptionMapper.insertNotify(r);
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("id", r.getId());
            payload.put("type", r.getNotifyType());
            payload.put("title", r.getTitle());
            realtimeHub.publish(r.getUserId(), "notify", payload);
        } catch (Exception ignored) {
            // 实时通道失败不影响落库
        }
    }

    private String price(BigDecimal v) {
        if (v == null) return "0";
        return v.stripTrailingZeros().toPlainString();
    }

    private void requireLogin(Integer uid) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
    }
}
