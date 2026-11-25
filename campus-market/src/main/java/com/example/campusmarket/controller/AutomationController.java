package com.example.campusmarket.controller;

import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.mapper.AutomationMapper;
import com.example.campusmarket.service.AutomationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动化预警中心：库存 / 临期 / 滞销 / 缺货 / 任务到期。
 */
@RestController
@RequestMapping("/automation")
public class AutomationController {

    @Autowired
    private AutomationMapper automationMapper;

    @Autowired
    private AutomationService automationService;

    /** 规则定义（名称、说明、阈值、优先级） */
    private List<Map<String, Object>> ruleDefinitions() {
        List<Map<String, Object>> rules = new ArrayList<>();
        rules.add(rule("INVENTORY_HIGH", "库存积压预警", "high",
                "单个卖家在售商品数量超过阈值，提醒清理库存",
                "在售 ≥ " + AutomationService.INVENTORY_THRESHOLD + " 件"));
        rules.add(rule("GOODS_EXPIRING", "临期下架预警", "high",
                "商品挂牌接近 30 天有效期，提前 3 天提醒",
                "剩余 ≤ 3 天"));
        rules.add(rule("GOODS_STALE", "滞销预警", "medium",
                "上架超过 30 天仍未售出，建议降价或换图",
                "上架 > " + AutomationService.STALE_DAYS + " 天"));
        rules.add(rule("CATEGORY_STOCKOUT", "分类缺货预警", "medium",
                "某个分类在售件数过低，容易让求购同学落空",
                "在售 ≤ " + AutomationService.CATEGORY_STOCK_THRESHOLD + " 件"));
        rules.add(rule("TASK_DUE", "任务到期预警", "high",
                "任务大厅里 24 小时内到期且未完成的任务",
                "剩余 ≤ 24 小时"));
        return rules;
    }

    private Map<String, Object> rule(String code, String name, String level, String desc, String threshold) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", code);
        m.put("name", name);
        m.put("level", level);
        m.put("desc", desc);
        m.put("threshold", threshold);
        return m;
    }

    /** 概览：规则 + 统计 + 上次扫描时间 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Integer> todayMap = new LinkedHashMap<>();
        Map<String, Integer> openMap = new LinkedHashMap<>();
        for (Map<String, Object> row : automationMapper.ruleStats()) {
            String code = String.valueOf(row.get("ruleCode"));
            todayMap.put(code, toInt(row.get("todayCount")));
            openMap.put(code, toInt(row.get("openCount")));
        }

        List<Map<String, Object>> rules = ruleDefinitions();
        for (Map<String, Object> r : rules) {
            String code = String.valueOf(r.get("code"));
            r.put("todayCount", todayMap.getOrDefault(code, 0));
            r.put("openCount", openMap.getOrDefault(code, 0));
        }

        Map<String, Object> raw = automationMapper.globalStats();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("todayTotal", raw == null ? 0 : toInt(raw.get("todayTotal")));
        stats.put("openTotal", raw == null ? 0 : toInt(raw.get("openTotal")));
        stats.put("highOpen", raw == null ? 0 : toInt(raw.get("highOpen")));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("rules", rules);
        data.put("stats", stats);
        data.put("lastScanTime", AutomationService.getLastScanTime());
        data.put("lastGenerated", AutomationService.getLastGenerated());
        data.put("enabled", true);
        return Result.success(data);
    }

    /** 预警列表 */
    @GetMapping("/alerts")
    public Result<Map<String, Object>> alerts(@RequestParam(required = false) String ruleCode,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "20") int pageSize) {
        int safePage = Math.max(pageNum, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 50);
        String rule = (ruleCode == null || ruleCode.isEmpty() || "all".equals(ruleCode)) ? null : ruleCode;
        String st = (status == null || status.isEmpty() || "all".equals(status)) ? null : status;

        List<Map<String, Object>> list = automationMapper.findAlerts(rule, st, (safePage - 1) * safeSize, safeSize);
        int total = automationMapper.countAlerts(rule, st);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("pageNum", safePage);
        data.put("pageSize", safeSize);
        return Result.success(data);
    }

    /** 立即扫描一次（登录后可用） */
    @PostMapping("/scan")
    public Result<Map<String, Object>> scan() {
        if (SecurityUtils.getCurrentUserId() == null) {
            return Result.error("请先登录后再手动扫描");
        }
        int generated = automationService.runScan();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("generated", generated);
        data.put("scanTime", AutomationService.getLastScanTime());
        return Result.success(data);
    }

    /** 标记预警已处理 */
    @PostMapping("/alerts/{id}/resolve")
    public Result<Map<String, Object>> resolve(@PathVariable Integer id) {
        if (SecurityUtils.getCurrentUserId() == null) {
            return Result.error("请先登录");
        }
        int rows = automationMapper.resolve(id);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("resolved", rows > 0);
        return Result.success(data);
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
