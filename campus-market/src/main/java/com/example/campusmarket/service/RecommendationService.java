package com.example.campusmarket.service;

import com.example.campusmarket.entity.Goods;
import com.example.campusmarket.mapper.RecommendationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 个性化推荐引擎。
 *
 * 推荐策略分三层（按权重从高到低合并，去重后取前 N 条）：
 *
 *   1. 【配件/互补品推荐】 —— 买过分类 X → 推同分类在售商品（排除已买的）
 *      例：用户买过"数码产品"里的手机 → 推同分类下的耳机、手机壳、充电器等
 *
 *   2. 【时序新款推荐】 —— 购买超过 N 个月 → 推该分类下**近 M 个月新发布**的商品
 *      例：用户 8 个月前买过手机 → 推数码分类近 6 个月新上架的手机/配件
 *      目的："两年之后推新款"的轻量化实现，降低阈值让效果在校园二手场景更快显现
 *
 *   3. 【浏览兴趣推荐】 —— 浏览过分类但未购买 → 推该分类热门商品
 *
 *   兜底：全站浏览量 Top N 热门商品
 */
@Service
public class RecommendationService {

    @Autowired
    private RecommendationMapper recommendationMapper;

    @Autowired
    private CampusFeatureService campusFeatureService;

    // 每个子策略最多返回多少条，合并后去重截断
    private static final int STRATEGY_LIMIT = 8;
    // 最终返回的推荐数量
    private static final int FINAL_LIMIT = 12;

    /**
     * 获取用户个性化推荐。
     *
     * @param userId 登录用户 ID；null 则直接走热门兜底
     * @return 按推荐强度排序的商品列表 + 每条商品的推荐理由（用于前端展示"为你推荐"的原因）
     */
    public Map<String, Object> getPersonalized(Integer userId) {
        String buyerStudentNo = userId != null ? campusFeatureService.findStudentNo(userId) : null;
        // 没登录 → 直接热门
        if (userId == null) {
            List<Goods> hot = recommendationMapper.findHotGoods(FINAL_LIMIT);
            return buildResult("hot", hot, null);
        }

        // 1) 用户已购商品 ID 集合（用于排除重复推荐）
        List<Integer> boughtIds = recommendationMapper.findUserBoughtGoodsIds(userId);

        // 2) 用户购买过的分类（含购买次数、最近购买时间）
        List<Map<String, Object>> buyCats = recommendationMapper.findUserBuyCategories(userId);

        // 3) 用户浏览过的分类
        List<Map<String, Object>> browseCats = recommendationMapper.findUserBrowseCategories(userId);

        // 合并去重的分类池
        Set<Integer> buyCategoryIds = buyCats.stream()
                .map(m -> ((Number) m.get("categoryId")).intValue())
                .collect(Collectors.toSet());
        Set<Integer> browseCategoryIds = browseCats.stream()
                .map(m -> ((Number) m.get("categoryId")).intValue())
                .collect(Collectors.toSet());

        // 用 LinkedHashMap 保持插入顺序 + 去重：购买过的分类优先
        List<Integer> allCategoryIds = new ArrayList<>();
        allCategoryIds.addAll(buyCategoryIds);
        for (Integer cid : browseCategoryIds) {
            if (!allCategoryIds.contains(cid)) allCategoryIds.add(cid);
        }

        // 没有任何行为 → 热门兜底
        if (allCategoryIds.isEmpty()) {
            List<Goods> hot = recommendationMapper.findHotGoods(FINAL_LIMIT);
            return buildResult("hot", hot, buyerStudentNo);
        }

        // ====== 四层策略依次取数 ======

        // ★ 策略 0（最高优先级）：ItemCF 协同过滤 —— "买了 A 的人也买了 B"
        // 只有用户买过东西才可能有协同数据；如果没有协同数据则自然跳过
        List<Goods> coPurchase = new ArrayList<>();
        if (!buyCategoryIds.isEmpty()) {
            coPurchase = recommendationMapper.findCoPurchasedGoods(userId, boughtIds, STRATEGY_LIMIT);
        }

        // 策略 1：配件/互补品 —— 从购买过的分类里推同分类商品
        List<Goods> complementary = new ArrayList<>();
        if (!buyCategoryIds.isEmpty()) {
            complementary = recommendationMapper.findGoodsByCategoriesExcluding(
                    new ArrayList<>(buyCategoryIds), boughtIds, STRATEGY_LIMIT);
        }

        // 策略 2：时序新款 —— 购买超过 6 个月 → 推近 6 个月新上架的同分类商品
        List<Goods> newArrivals = new ArrayList<>();
        if (!buyCategoryIds.isEmpty()) {
            newArrivals = recommendationMapper.findNewArrivalsByCategories(
                    new ArrayList<>(buyCategoryIds), boughtIds, 6, STRATEGY_LIMIT);
        }

        // 策略 3：浏览兴趣 —— 浏览过但没买的分类（从 browseCategoryIds 里去掉 buyCategoryIds）
        Set<Integer> interestOnly = new LinkedHashSet<>(browseCategoryIds);
        interestOnly.removeAll(buyCategoryIds);
        List<Goods> browseInterest = new ArrayList<>();
        if (!interestOnly.isEmpty()) {
            browseInterest = recommendationMapper.findGoodsByCategoriesExcluding(
                    new ArrayList<>(interestOnly), boughtIds, STRATEGY_LIMIT);
        }

        // ====== 合并去重，按策略优先级排序 ======
        // 优先级：协同过滤(ItemCF) > 互补品 > 时序新款 > 浏览兴趣
        List<Goods> merged = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();

        appendUnique(merged, coPurchase, seen);
        appendUnique(merged, complementary, seen);
        appendUnique(merged, newArrivals, seen);
        appendUnique(merged, browseInterest, seen);

        // 如果合并后仍不足 FINAL_LIMIT，用全站热门补齐
        if (merged.size() < FINAL_LIMIT) {
            List<Goods> filler = recommendationMapper.findHotGoods(FINAL_LIMIT);
            appendUnique(merged, filler, seen);
        }

        // 截断
        if (merged.size() > FINAL_LIMIT) {
            merged = merged.subList(0, FINAL_LIMIT);
        }

        // 判定推荐来源类型（用 id-set 交叉，避免 lambda effectively-final 问题）
        Set<Integer> coPurchaseIds = new HashSet<>();
        for (Goods g : coPurchase) coPurchaseIds.add(g.getId());
        Set<Integer> complementaryIds = new HashSet<>();
        for (Goods g : complementary) complementaryIds.add(g.getId());
        Set<Integer> newArrivalsIds = new HashSet<>();
        for (Goods g : newArrivals) newArrivalsIds.add(g.getId());

        // merged 里出现的 id 集合
        Set<Integer> mergedIds = new HashSet<>();
        for (Goods g : merged) mergedIds.add(g.getId());

        String source;
        if (!coPurchaseIds.isEmpty() && containsAny(mergedIds, coPurchaseIds)) {
            source = "collaborative";       // 协同过滤（买了 A 的人也买了 B）
        } else if (!complementaryIds.isEmpty() && containsAny(mergedIds, complementaryIds)) {
            source = "complementary";       // 有购买历史 → 配件推荐为主
        } else if (!newArrivalsIds.isEmpty() && containsAny(mergedIds, newArrivalsIds)) {
            source = "timely";             // 时序新款命中
        } else if (!browseInterest.isEmpty()) {
            source = "browse";             // 仅浏览历史
        } else {
            source = "hot";                // 兜底热门
        }

        return buildResult(source, merged, buyerStudentNo);
    }

    private boolean containsAny(Set<Integer> a, Set<Integer> b) {
        for (Integer id : b) {
            if (a.contains(id)) return true;
        }
        return false;
    }

    private void appendUnique(List<Goods> target, List<Goods> source, Set<Integer> seen) {
        for (Goods g : source) {
            if (seen.add(g.getId())) {
                target.add(g);
            }
        }
    }

    private Map<String, Object> buildResult(String source, List<Goods> goods) {
        return buildResult(source, goods, null);
    }

    private Map<String, Object> buildResult(String source, List<Goods> goods, String buyerStudentNo) {
        // enrich 校园专属标签（年级阶梯价/毕业清仓）
        campusFeatureService.enrichGoods(goods, buyerStudentNo);
        Map<String, Object> result = new HashMap<>();
        result.put("source", source);   // 告诉前端该推荐来自哪种策略，展示不同标题
        result.put("list", goods);
        return result;
    }
}
