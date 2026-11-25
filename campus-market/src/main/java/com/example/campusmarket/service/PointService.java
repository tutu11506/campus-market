package com.example.campusmarket.service;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.entity.*;
import com.example.campusmarket.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 积分 & 环保激励中心。
 * 发放积分入口：
 *   - reward(userId, type, points, refType, refId, remark)     通用发放
 *   - consume(userId, rewardId)                                兑换扣减
 *   - 各业务 Service 在发布/成交/捐赠完成后直接调用 reward(...)
 */
@Service
public class PointService {

    /* ==================== 固定积分规则 ==================== */

    /** 发布闲置 +5 */
    public static final int POINT_POST_GOODS = 5;
    /** 成交 +15 */
    public static final int POINT_SELL_GOODS = 15;
    /** 发布可租 +8 */
    public static final int POINT_POST_RENTAL = 8;
    /** 完成租赁 +10 */
    public static final int POINT_COMPLETE_RENTAL = 10;
    /** 捐赠完成 +20 */
    public static final int POINT_DONATE = 20;

    public static final Set<String> INCOME_TYPES = Set.of(
            "post_goods", "sell_goods",
            "post_rental", "complete_rental",
            "donation", "admin_grant", "badge_reward"
    );

    public static final Set<String> EXPENSE_TYPES = Set.of("exchange", "admin_deduct");

    /* ==================== 依赖 ==================== */

    @Autowired PointMapper pointMapper;
    @Autowired RewardMapper rewardMapper;
    @Autowired EcoBadgeMapper badgeMapper;

    /* ==================== 积分账户 ==================== */

    public PointAccount myAccount(Integer uid) {
        if (uid == null) throw new IllegalArgumentException("请先登录");
        pointMapper.ensureAccount(uid);
        PointAccount a = pointMapper.findAccount(uid);
        return a;
    }

    /* ==================== 发放 ==================== */

    /**
     * 原子发放积分（获得）。
     * @param type 枚举见 INCOME_TYPES
     */
    @Transactional
    public int reward(Integer userId, String type, int points,
                      String refType, Integer refId, String remark) {
        if (userId == null) throw new IllegalArgumentException("用户不存在");
        if (points <= 0) throw new IllegalArgumentException("发放积分必须为正");
        if (!INCOME_TYPES.contains(type)) throw new IllegalArgumentException("未知积分类型 " + type);

        pointMapper.ensureAccount(userId);
        int affected = pointMapper.credit(userId, points);
        if (affected <= 0) throw new RuntimeException("发放积分失败：账户不存在或未更新");

        PointAccount acc = pointMapper.findAccount(userId);
        PointRecord r = new PointRecord();
        r.setUserId(userId);
        r.setDirection(1);
        r.setPoints(points);
        r.setBalanceAfter(acc.getBalance());
        r.setType(type);
        r.setRefType(refType);
        r.setRefId(refId);
        r.setRemark(remark);
        pointMapper.insertRecord(r);

        // 勋章检查（不抛异常，阻断发放）
        try { checkBadges(userId); } catch (Exception e) { /* 忽略 */ }

        return acc.getBalance();
    }

    /* ==================== 消费 ==================== */

    /**
     * 原子扣减积分（消费）。
     */
    @Transactional
    public int consume(Integer userId, String type, int points,
                       String refType, Integer refId, String remark) {
        if (userId == null) throw new IllegalArgumentException("用户不存在");
        if (points <= 0) throw new IllegalArgumentException("扣减积分必须为正");
        if (!EXPENSE_TYPES.contains(type)) throw new IllegalArgumentException("未知消费类型 " + type);

        pointMapper.ensureAccount(userId);
        int affected = pointMapper.debit(userId, points);
        if (affected <= 0) throw new IllegalArgumentException("积分不足");

        PointAccount acc = pointMapper.findAccount(userId);
        PointRecord r = new PointRecord();
        r.setUserId(userId);
        r.setDirection(-1);
        r.setPoints(points);
        r.setBalanceAfter(acc.getBalance());
        r.setType(type);
        r.setRefType(refType);
        r.setRefId(refId);
        r.setRemark(remark);
        pointMapper.insertRecord(r);

        return acc.getBalance();
    }

    /* ==================== 积分流水 ==================== */

    public PageResult<PointRecord> myRecords(Integer uid, Integer pageNum, Integer pageSize) {
        if (uid == null) throw new IllegalArgumentException("请先登录");
        int[] p = PageResult.normalize(pageNum, pageSize, 30);
        List<PointRecord> list = pointMapper.findByUser(uid, p[2], p[1]);
        long total = pointMapper.countByUser(uid);
        return PageResult.of(list, total, p[0], p[1]);
    }

    /* ==================== 兑换 ==================== */

    public List<PointReward> listRewards() {
        return rewardMapper.listActive();
    }

    @Transactional
    public PointExchange exchange(Integer userId, Integer rewardId) {
        if (userId == null) throw new IllegalArgumentException("请先登录");
        PointReward reward = rewardMapper.findById(rewardId);
        if (reward == null || reward.getStatus() != 1) {
            throw new IllegalArgumentException("兑换商品不存在或已下架");
        }
        // 先检查库存（乐观）
        if (reward.getStock() != null && reward.getStock() - reward.getTotalRedeemed() <= 0) {
            throw new IllegalArgumentException("兑换商品已被抢光");
        }
        PointAccount acc = myAccount(userId);
        if (acc.getBalance() < reward.getPricePoints()) {
            throw new IllegalArgumentException("积分不足（需要 " + reward.getPricePoints() + "，当前 " + acc.getBalance() + "）");
        }

        // 1. 扣减积分
        consume(userId, "exchange", reward.getPricePoints(), "reward", rewardId,
                "兑换 " + reward.getName());

        // 2. 库存 +1
        rewardMapper.consumeStock(rewardId);

        // 3. 记录订单
        PointExchange e = new PointExchange();
        e.setUserId(userId);
        e.setRewardId(rewardId);
        e.setRewardSnapshot(reward.getName() + "(" + reward.getType() + ")");
        e.setPointsCost(reward.getPricePoints());
        rewardMapper.insertExchange(e);

        return e;
    }

    public PageResult<PointExchange> myExchanges(Integer uid, Integer pageNum, Integer pageSize) {
        if (uid == null) throw new IllegalArgumentException("请先登录");
        int[] p = PageResult.normalize(pageNum, pageSize, 20);
        List<PointExchange> list = rewardMapper.findMyExchanges(uid, p[2], p[1]);
        long total = rewardMapper.countMyExchanges(uid);
        return PageResult.of(list, total, p[0], p[1]);
    }

    /* ==================== 排行榜 ==================== */

    public Map<String, Object> leaderboard(String scope) {
        Map<String, Object> data = new LinkedHashMap<>();
        String s = scope == null ? "user_balance" : scope;
        int limit = 20;

        switch (s) {
            case "user_balance":
                data.put("title", "🎯 个人积分总榜");
                data.put("items", pointMapper.userTopBalance(limit));
                break;
            case "user_cycled":
                data.put("title", "🔄 本月循环榜");
                data.put("items", pointMapper.userTopCycled(limit));
                break;
            case "college":
                data.put("title", "🏫 学院积分榜");
                data.put("items", pointMapper.collegeTopBalance(limit));
                break;
            case "badge":
                data.put("title", "🏅 学院勋章榜");
                data.put("items", badgeMapper.collegeTopBadges(limit));
                break;
            default:
                throw new IllegalArgumentException("未知榜单范围 " + scope);
        }
        return data;
    }

    /* ==================== 勋章 ==================== */

    public List<EcoBadge> allBadges() {
        return badgeMapper.listAll();
    }

    public List<EcoBadge> myBadges(Integer uid) {
        if (uid == null) throw new IllegalArgumentException("请先登录");
        return badgeMapper.findByUser(uid);
    }

    /**
     * 检查用户是否达到勋章触发阈值，未解锁则发放。
     * 由 reward() 自动调用，也可手动触发。
     */
    @Transactional
    public int checkBadges(Integer userId) {
        List<EcoBadge> all = badgeMapper.listAll();
        int unlocked = 0;

        for (EcoBadge b : all) {
            if (badgeMapper.countUserBadge(userId, b.getId()) > 0) continue;

            int current = 0;
            switch (b.getConditionType()) {
                case "donation_count":   current = badgeMapper.donationCount(userId); break;
                case "goods_sold":       current = badgeMapper.goodsSoldCount(userId); break;
                case "rental_complete":  current = badgeMapper.rentalCompleteCount(userId); break;
                case "total_points": {
                    Integer earned = badgeMapper.earnedTotal(userId);
                    current = earned == null ? 0 : earned;
                    break;
                }
                default: continue;
            }
            if (current >= b.getConditionValue()) {
                badgeMapper.grantBadge(userId, b.getId());
                unlocked++;
                // 额外奖励积分（积分来源：系统发放）
                if (b.getRewardPoints() != null && b.getRewardPoints() > 0) {
                    try { reward(userId, "badge_reward", b.getRewardPoints(), "badge", b.getId(), "解锁勋章 " + b.getName()); }
                    catch (Exception ignored) { }
                }
            }
        }
        return unlocked;
    }
}
