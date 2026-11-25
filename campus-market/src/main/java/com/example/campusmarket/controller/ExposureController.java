package com.example.campusmarket.controller;

import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.mapper.FinanceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品曝光数据看板：卖家查看每个商品的浏览量、收藏数、咨询人数与来源宿舍区，
 * 用来判断该调价还是改描述。数据全部来自 goods / favorite / message。
 */
@RestController
@RequestMapping("/seller")
public class ExposureController {

    @Autowired
    private FinanceMapper financeMapper;

    @GetMapping("/exposure")
    public Result<Map<String, Object>> exposure() {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return Result.error("未登录");

        List<Map<String, Object>> rows = financeMapper.findGoodsExposure(userId);

        int totalViews = 0;
        int totalFavorites = 0;
        int totalConsults = 0;
        List<Map<String, Object>> goods = new ArrayList<>();
        Map<String, Map<String, Object>> areaMap = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            int views = toInt(row.get("views"));
            int favorites = toInt(row.get("favorites"));
            int consults = toInt(row.get("consults"));
            String location = row.get("location") == null ? "" : String.valueOf(row.get("location"));
            String area = areaOf(location);

            totalViews += views;
            totalFavorites += favorites;
            totalConsults += consults;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("title", row.get("title"));
            item.put("coverImage", row.get("coverImage"));
            item.put("price", row.get("price"));
            item.put("status", row.get("status"));
            item.put("views", views);
            item.put("favorites", favorites);
            item.put("consults", consults);
            item.put("location", location);
            item.put("area", area);
            goods.add(item);

            Map<String, Object> slot = areaMap.computeIfAbsent(area, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("area", k);
                m.put("goodsCount", 0);
                m.put("views", 0);
                m.put("favorites", 0);
                m.put("consults", 0);
                return m;
            });
            slot.put("goodsCount", toInt(slot.get("goodsCount")) + 1);
            slot.put("views", toInt(slot.get("views")) + views);
            slot.put("favorites", toInt(slot.get("favorites")) + favorites);
            slot.put("consults", toInt(slot.get("consults")) + consults);
        }

        List<Map<String, Object>> areas = new ArrayList<>(areaMap.values());
        areas.sort(Comparator.comparingInt((Map<String, Object> a) -> toInt(a.get("views"))).reversed());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("goodsCount", goods.size());
        summary.put("totalViews", totalViews);
        summary.put("totalFavorites", totalFavorites);
        summary.put("totalConsults", totalConsults);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summary", summary);
        data.put("goods", goods);
        data.put("areas", areas);
        data.put("areaNote", "宿舍区按商品交易地点（location）自动归类");
        data.put("hasData", !goods.isEmpty());
        return Result.success(data);
    }

    /** 从交易地点里解析宿舍区，解析不到归入「其他」 */
    private String areaOf(String location) {
        if (location == null || location.isBlank()) return "未填写地点";
        if (location.contains("研究生")) return "研究生区";
        if (location.contains("南区")) return "南区";
        if (location.contains("北区")) return "北区";
        if (location.contains("东区")) return "东区";
        if (location.contains("西区")) return "西区";
        return "其他";
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
