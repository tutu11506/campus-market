package com.example.campusmarket.controller;

import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.mapper.RecommendationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * 复购提醒接口。
 *
 * 核心思想：识别用户买过的"消费品类商品"（会用完、需要换新的），
 * 根据典型消耗周期算出时间窗口，告诉用户"你 N 天前买的 X 该补了"。
 *
 * 各品类消耗周期参考（校园场景数据）：
 *   生活用品(3)   → 30~120 天（纸巾、拖把等）
 *   教材书籍(1)   → 90~400 天（每学期/每年新书）
 *   运动户外(5)   → 60~200 天（羽毛球、瑜伽砖等）
 */
@RestController
@RequestMapping("/recommend")
public class RepurchaseController {

    @Autowired
    private RecommendationMapper recommendationMapper;

    /** 会消耗需要复购的品类 ID */
    private static final List<Integer> CONSUMABLE_CATEGORIES = List.of(1, 3, 5);

    @GetMapping("/repurchase-reminders")
    public Result<Map<String, Object>> repurchaseReminders() {
        Integer userId;
        try {
            userId = SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            return Result.success(Map.of("hasReminders", false, "items", List.of()));
        }

        Map<String, Object> result = new HashMap<>();

        // 策略 A：刚好买了 30~120 天的生活用品 → "上次买的... 该补了"
        List<Map<String, Object>> consumableSoon = recommendationMapper.findRepurchaseReminders(
                userId, CONSUMABLE_CATEGORIES, 30, 120, 4);

        // 策略 B：90~400 天前买的教材 → "该换新学期教材了"
        List<Map<String, Object>> textbookCycle = recommendationMapper.findRepurchaseReminders(
                userId, List.of(1), 90, 400, 4);

        // 给每条提醒加一个人类可读的 reason 字段
        List<Map<String, Object>> all = new ArrayList<>();
        for (Map<String, Object> m : consumableSoon) {
            m.put("reason", buildReason(m, "该补了"));
            all.add(m);
        }
        for (Map<String, Object> m : textbookCycle) {
            // 避免重复（同一 goods 已在 consumableSoon 里）
            if (!containsGoodsId(all, ((Number) m.get("goodsId")).intValue())) continue;
            m.put("reason", buildReason(m, "该考虑换新款了"));
            all.add(m);
        }

        result.put("hasReminders", !all.isEmpty());
        result.put("items", all);
        return Result.success(result);
    }

    private boolean containsGoodsId(List<Map<String, Object>> list, int goodsId) {
        for (Map<String, Object> m : list) {
            if (((Number) m.get("goodsId")).intValue() == goodsId) return true;
        }
        return false;
    }

    private String buildReason(Map<String, Object> m, String action) {
        long days = ((Number) m.get("daysSinceBuy")).longValue();
        String cat = String.valueOf(m.getOrDefault("categoryName", ""));
        if (days < 45) return "你 " + days + " 天前买的" + cat + action;
        if (days < 90) return "你快 " + (days / 30) + " 个月前买的" + cat + action;
        return "你 " + (days / 30) + " 个月前买的" + cat + action;
    }
}
