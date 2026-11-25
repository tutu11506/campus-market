package com.example.campusmarket.controller;

import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.mapper.GoodsMapper;
import com.example.campusmarket.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.*;

/**
 * 卖家数据看板 —— 只给登录且发布过商品的用户看。
 * 所有数据 100% 来自现有表（goods + order），零新表。
 *
 * 指标设计：
 *   1. 发布概览：总数 / 在售 / 已成交
 *   2. 流量概览：总浏览量 / 平均浏览量 / 热门商品 Top3
 *   3. 交易概览：总交易额 / 成交订单数 / 成交率
 *   4. 分类分布（柱状图用）
 *   5. 最近成交趋势（近 7 天）
 *
 * 这些数字闲鱼/转转不会给卖家看 —— 它们只给大商家开付费工具。
 */
@RestController
@RequestMapping("/seller")
public class SellerStatsController {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private OrderMapper orderMapper;

    @GetMapping("/stats")
    public Result<Map<String, Object>> myStats() {
        Integer userId = SecurityUtils.getCurrentUserId();
        Map<String, Object> result = new HashMap<>();

        // ========== 1. 发布概览 ==========
        int totalPublished = goodsMapper.countBySeller(userId);
        int totalViewCount = goodsMapper.sumViewCountBySeller(userId);
        int activeCount = goodsMapper.countBySellerAndStatus(userId, "on_sale");
        int lockedCount = goodsMapper.countBySellerAndStatus(userId, "locked");
        int soldCount = goodsMapper.countBySellerAndStatus(userId, "sold");
        BigDecimal totalAmount = goodsMapper.sumSellerCompletedAmount(userId);
        int completedOrders = goodsMapper.countSellerCompletedOrders(userId);
        double dealRate = completedOrders > 0
                ? (double) completedOrders / (completedOrders + goodsMapper.countSellerFailedOrders(userId)) * 100
                : 0;

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalPublished", totalPublished);
        overview.put("activeCount", activeCount);
        overview.put("lockedCount", lockedCount);
        overview.put("soldCount", soldCount);
        overview.put("totalViewCount", totalViewCount);
        overview.put("avgViewPerGoods", totalPublished > 0 ? Math.round((double) totalViewCount / totalPublished * 10) / 10.0 : 0);
        overview.put("totalAmount", totalAmount);
        overview.put("completedOrders", completedOrders);
        overview.put("dealRate", Math.round(dealRate * 10.0) / 10.0);
        result.put("overview", overview);

        // ========== 2. 分类分布 ==========
        result.put("categoryDist", goodsMapper.sellerCategoryDist(userId));

        // ========== 3. 热门商品 Top3 ==========
        result.put("topGoods", goodsMapper.sellerTopGoods(userId, 3));

        // ========== 4. 最近 7 天成交趋势 ==========
        result.put("recentTrend", goodsMapper.sellerRecentTrend(userId));

        return Result.success(result);
    }
}
