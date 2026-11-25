package com.example.campusmarket.controller;

import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.mapper.CarbonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * 碳足迹统计：把每一笔已完成的二手交易折算成减少的碳排放，
 * 累计个人环保贡献，并支持生成年度环保海报（海报由前端 canvas 绘制）。
 */
@RestController
@RequestMapping("/carbon")
public class CarbonController {

    /** 各分类商品的平均重量（kg），用于把「件」折算成「重量」 */
    private static final Map<String, Double> CATEGORY_WEIGHT = Map.of(
            "教材书籍", 0.6,
            "数码产品", 1.8,
            "生活用品", 1.0,
            "服饰鞋包", 0.7,
            "运动户外", 1.2,
            "其他", 0.8
    );

    /** 二手再利用相对新品生产的平均减排因子（kgCO₂e / kg） */
    private static final double EMISSION_FACTOR = 6.0;

    /** 一棵树一年约吸收 18.3 kgCO₂e */
    private static final double TREE_PER_YEAR = 18.3;

    /** 燃油车每公里约排放 0.17 kgCO₂e */
    private static final double CAR_PER_KM = 0.17;

    /** 给手机充一次电约排放 0.012 kgCO₂e */
    private static final double PHONE_CHARGE = 0.012;

    private static final DateTimeFormatter RANGE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private CarbonMapper carbonMapper;

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestParam(required = false) Integer year) {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return Result.error("未登录");

        int currentYear = LocalDateTime.now().getYear();
        int targetYear = year == null ? currentYear : year;
        if (targetYear < 2000 || targetYear > currentYear) {
            throw new IllegalArgumentException("年份不合法");
        }

        String start = LocalDateTime.of(targetYear, 1, 1, 0, 0, 0).format(RANGE_FMT);
        String end = LocalDateTime.of(targetYear + 1, 1, 1, 0, 0, 0).format(RANGE_FMT);
        List<Map<String, Object>> yearOrders = carbonMapper.findOrders(userId, start, end);
        List<Map<String, Object>> allOrders = carbonMapper.findAllOrders(userId);

        // 12 个月累计
        double[] monthKg = new double[12];
        int[] monthOrders = new int[12];
        double yearKg = 0;
        Map<String, Double> categoryKg = new LinkedHashMap<>();
        Map<String, Integer> categoryOrders = new LinkedHashMap<>();

        for (Map<String, Object> order : yearOrders) {
            double kg = estimateKg(order.get("categoryName"));
            yearKg += kg;
            LocalDateTime time = toLocalDateTime(order.get("happenTime"));
            if (time != null && time.getYear() == targetYear) {
                int idx = time.getMonthValue() - 1;
                monthKg[idx] += kg;
                monthOrders[idx]++;
            }
            String name = normalizeCategory(order.get("categoryName"));
            categoryKg.merge(name, kg, Double::sum);
            categoryOrders.merge(name, 1, Integer::sum);
        }

        List<Map<String, Object>> months = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", i + 1);
            m.put("label", (i + 1) + "月");
            m.put("kg", round1(monthKg[i]));
            m.put("orders", monthOrders[i]);
            months.add(m);
        }

        List<Map<String, Object>> categories = new ArrayList<>();
        for (Map.Entry<String, Double> e : categoryKg.entrySet()) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("name", e.getKey());
            c.put("kg", round1(e.getValue()));
            c.put("orders", categoryOrders.getOrDefault(e.getKey(), 0));
            c.put("sharePct", yearKg <= 0 ? 0 : round1(e.getValue() * 100 / yearKg));
            categories.add(c);
        }
        categories.sort(Comparator.comparingDouble((Map<String, Object> c) -> toDouble(c.get("kg"))).reversed());

        // 累计（全部年份）
        double totalKg = 0;
        TreeSet<Integer> yearSet = new TreeSet<>(Comparator.reverseOrder());
        yearSet.add(currentYear);
        for (Map<String, Object> order : allOrders) {
            totalKg += estimateKg(order.get("categoryName"));
            LocalDateTime time = toLocalDateTime(order.get("happenTime"));
            if (time != null) yearSet.add(time.getYear());
        }
        if (!yearOrders.isEmpty()) yearSet.add(targetYear);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("year", targetYear);
        summary.put("yearKg", round1(yearKg));
        summary.put("yearOrders", yearOrders.size());
        summary.put("totalKg", round1(totalKg));
        summary.put("totalOrders", allOrders.size());
        summary.put("trees", round1(totalKg / TREE_PER_YEAR));
        summary.put("carKm", Math.round(totalKg / CAR_PER_KM));
        summary.put("phoneCharges", Math.round(totalKg / PHONE_CHARGE));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summary", summary);
        data.put("months", months);
        data.put("categories", categories);
        data.put("years", new ArrayList<>(yearSet));
        data.put("note", "碳减排为估算值：按商品分类估算平均重量 × 6.0 kgCO₂e/kg，仅供参考");
        data.put("hasData", !yearOrders.isEmpty());
        return Result.success(data);
    }

    /** 一件商品折算的减排量（kgCO₂e） */
    private double estimateKg(Object categoryName) {
        String name = normalizeCategory(categoryName);
        double weight = CATEGORY_WEIGHT.getOrDefault(name, 0.8);
        return weight * EMISSION_FACTOR;
    }

    private String normalizeCategory(Object value) {
        String name = value == null ? "" : String.valueOf(value).trim();
        return name.isEmpty() ? "其他" : name;
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof Timestamp ts) return ts.toLocalDateTime();
        if (value instanceof LocalDateTime ldt) return ldt;
        if (value instanceof java.util.Date date) {
            return new Timestamp(date.getTime()).toLocalDateTime();
        }
        try {
            String s = String.valueOf(value).trim().replace('T', ' ');
            int dot = s.indexOf('.');
            if (dot > 0) s = s.substring(0, dot);
            if (s.length() <= 10) s = s + " 00:00:00";
            return LocalDateTime.parse(s, RANGE_FMT);
        } catch (Exception e) {
            return null;
        }
    }
}
