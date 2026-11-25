package com.example.campusmarket.controller;

import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 个性化推荐接口。
 * 支持登录用户精准推荐；未登录时返回全站热门。
 */
@RestController
@RequestMapping("/recommend")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    /**
     * 获取个性化推荐列表。
     * 前端请求无需参数，后端自动从 SecurityUtils 拿当前用户（未登录则为 null）。
     *
     * 返回结构：
     * {
     *   source: "complementary" | "timely" | "browse" | "hot",
     *   list: [ Goods, Goods, ... ]
     * }
     *
     * source 含义：
     *   complementary —— 买过该类 → 推同分类配件（例：买了手机推手机壳）
     *   timely        —— 购买较久 → 推该分类近期新上架（例：8个月前买的，推近6个月新款）
     *   browse        —— 仅浏览历史 → 推浏览分类热门商品
     *   hot           —— 兜底 / 未登录 → 全站热门
     */
    @GetMapping("/personalized")
    public Result<Map<String, Object>> personalized() {
        Integer userId = null;
        try {
            userId = SecurityUtils.getCurrentUserId();
        } catch (Exception ignored) {
            // 未登录拿不到 userId，保持 null，服务层走热门兜底
        }
        return Result.success(recommendationService.getPersonalized(userId));
    }
}
