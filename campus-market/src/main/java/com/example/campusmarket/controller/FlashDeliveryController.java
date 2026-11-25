package com.example.campusmarket.controller;

import com.example.campusmarket.common.Result;
import com.example.campusmarket.mapper.GoodsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 校园闪送 —— 把校园二手做成"京东秒送"式的即时履约。
 *
 * 竞争壁垒：
 *   闲鱼/转转只能寄快递，最快也要隔天到；
 *   但校园里买卖双方就在同一片校区，走几步路就能送达。
 *   我们把每个真实面交地点当成一个"校园闪送仓"，
 *   按地点给出预计送达时间，急用的东西（充电器、教材、球拍）十几分钟就能拿到手。
 */
@RestController
@RequestMapping("/flash")
public class FlashDeliveryController {

    @Autowired
    private GoodsMapper goodsMapper;

    /**
     * 闪送总览：配送点 + 分类 + 履约数据 + 此刻最抢手。
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        List<Map<String, Object>> pointRows = goodsMapper.flashPoints(12);

        List<Map<String, Object>> points = new ArrayList<>();
        int totalOnSale = 0;
        int etaSum = 0;
        int fastest = Integer.MAX_VALUE;

        for (Map<String, Object> row : pointRows) {
            String location = row.get("location") == null ? "" : String.valueOf(row.get("location"));
            int count = toInt(row.get("count"));
            // 过滤掉历史脏数据（如 "??"、"x" 这类不含中文的地点）
            if (location.isEmpty() || count <= 0 || !location.matches(".*[\\u4e00-\\u9fa5].*")) continue;

            int eta = etaMinutes(location);
            totalOnSale += count;
            etaSum += eta;
            fastest = Math.min(fastest, eta);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("location", location);
            item.put("count", count);
            item.put("avgPrice", toInt(row.get("avgPrice")));
            item.put("minPrice", toInt(row.get("minPrice")));
            item.put("etaMinutes", eta);
            item.put("distanceMeters", eta * 60);
            points.add(item);
        }

        // 最近的配送点排前面（送达时间短的优先展示）
        points.sort((a, b) -> Integer.compare((int) a.get("etaMinutes"), (int) b.get("etaMinutes")));
        for (int i = 0; i < points.size(); i++) {
            points.get(i).put("tag", i == 0 ? "离你最近" : i == 1 ? "次近" : "");
        }

        List<Map<String, Object>> categories = new ArrayList<>();
        for (Map<String, Object> row : goodsMapper.flashCategories()) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("categoryId", toInt(row.get("categoryId")));
            c.put("categoryName", row.get("categoryName"));
            c.put("count", toInt(row.get("count")));
            c.put("minPrice", toInt(row.get("minPrice")));
            categories.add(c);
        }

        List<Map<String, Object>> hotNow = new ArrayList<>();
        for (Map<String, Object> row : goodsMapper.flashHotNow(6)) {
            Map<String, Object> h = new LinkedHashMap<>();
            h.put("id", toInt(row.get("id")));
            h.put("title", row.get("title"));
            h.put("price", row.get("price"));
            h.put("coverImage", row.get("coverImage"));
            h.put("location", row.get("location"));
            h.put("categoryName", row.get("categoryName"));
            h.put("hotCount", toInt(row.get("hotCount")));
            hotNow.add(h);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("points", points);
        data.put("pointsCount", points.size());
        data.put("totalOnSale", totalOnSale);
        data.put("avgEta", points.isEmpty() ? 0 : Math.round((float) etaSum / points.size()));
        data.put("fastestEta", points.isEmpty() ? 0 : fastest);
        data.put("deliveredToday", goodsMapper.flashDeliveredToday());
        data.put("categories", categories);
        data.put("hotNow", hotNow);
        data.put("hasData", !points.isEmpty());
        return Result.success(data);
    }

    /**
     * 闪送商品列表：按配送点 / 分类筛选。
     */
    @GetMapping("/goods")
    public Result<Map<String, Object>> goods(@RequestParam(required = false) String location,
                                             @RequestParam(required = false) Integer categoryId,
                                             @RequestParam(defaultValue = "48") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        List<Map<String, Object>> rows = goodsMapper.flashGoods(location, categoryId, safeLimit);

        String loc = location == null ? "" : location;
        int eta = loc.isEmpty() ? 12 : etaMinutes(loc);

        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String gLoc = row.get("location") == null ? "" : String.valueOf(row.get("location"));
            // 过滤掉不含中文的脏地点数据
            if (loc.isEmpty() && !gLoc.matches(".*[\\u4e00-\\u9fa5].*")) continue;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", toInt(row.get("id")));
            item.put("title", row.get("title"));
            item.put("price", row.get("price"));
            item.put("coverImage", row.get("coverImage"));
            item.put("location", row.get("location"));
            item.put("categoryName", row.get("categoryName"));
            item.put("viewCount", toInt(row.get("viewCount")));
            // 按商品所属地点的距离，给出该商品的可送达时间
            item.put("etaMinutes", gLoc.isEmpty() ? eta : etaMinutes(gLoc));
            items.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("total", items.size());
        data.put("location", loc);
        data.put("etaMinutes", eta);
        data.put("hasData", !items.isEmpty());
        return Result.success(data);
    }

    /**
     * 各面交地点到宿舍的预计送达分钟数。
     * 依据校园空间布局估算：宿舍区最近，校门口最远。
     */
    private int etaMinutes(String location) {
        if (location == null) return 12;
        String l = location.trim();
        if (l.contains("南区")) return 8;
        if (l.contains("北区")) return 9;
        if (l.contains("图书馆") || l.contains("实验楼")) return 10;
        if (l.contains("东区") || l.contains("西区")) return 12;
        if (l.contains("操场") || l.contains("体育馆")) return 13;
        if (l.contains("校门")) return 15;
        if (l.contains("食堂")) return 10;
        if (l.contains("教")) return 12;
        return 12;
    }

    private int toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
