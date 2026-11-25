package com.example.campusmarket.controller;

import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.mapper.FinanceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 营收账单：把卖出收入、跑腿支出、积分变动汇总成一张可对账的月度账单。
 * 明细全部来自现有表，导出由前端生成 CSV。
 */
@RestController
@RequestMapping("/finance")
public class LedgerController {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter RANGE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private FinanceMapper financeMapper;

    @GetMapping("/ledger")
    public Result<Map<String, Object>> ledger(@RequestParam(required = false) String month) {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return Result.error("未登录");

        YearMonth ym;
        try {
            ym = (month == null || month.isBlank()) ? YearMonth.now() : YearMonth.parse(month.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("月份格式应为 YYYY-MM");
        }

        String start = ym.atDay(1).atStartOfDay().format(RANGE_FMT);
        String end = ym.plusMonths(1).atDay(1).atStartOfDay().format(RANGE_FMT);

        List<Map<String, Object>> rows = new ArrayList<>();
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal errandExpense = BigDecimal.ZERO;
        int pointsIncome = 0;
        int pointsExpense = 0;

        // 1. 卖出收入
        for (Map<String, Object> o : financeMapper.findIncome(userId, start, end)) {
            BigDecimal amount = toDecimal(o.get("amount"));
            income = income.add(amount);
            String buyer = o.get("buyerName") == null ? "" : String.valueOf(o.get("buyerName"));
            rows.add(buildRow(o.get("happenTime"), "income", "卖出收入",
                    o.get("title") == null ? "卖出的商品" : String.valueOf(o.get("title")),
                    amount, 1, buyer.isEmpty() ? "买家已确认收货" : "买家 " + buyer));
        }

        // 2. 跑腿支出
        for (Map<String, Object> t : financeMapper.findErrands(userId, start, end)) {
            BigDecimal amount = toDecimal(t.get("amount"));
            errandExpense = errandExpense.add(amount);
            String accepter = t.get("accepterName") == null ? "" : String.valueOf(t.get("accepterName"));
            rows.add(buildRow(t.get("happenTime"), "errand", "跑腿支出",
                    t.get("title") == null ? "跑腿任务" : String.valueOf(t.get("title")),
                    amount, -1, accepter.isEmpty() ? "任务已完成" : "接单人 " + accepter));
        }

        // 3. 积分变动
        for (Map<String, Object> p : financeMapper.findPointRecords(userId, start, end)) {
            int direction = toInt(p.get("direction"));
            int points = toInt(p.get("points"));
            if (direction > 0) pointsIncome += points;
            else pointsExpense += points;
            String type = p.get("type") == null ? "积分变动" : String.valueOf(p.get("type"));
            rows.add(buildRow(p.get("happenTime"), "points", "积分变动", type,
                    points, direction > 0 ? 1 : -1, p.get("remark") == null ? "" : String.valueOf(p.get("remark"))));
        }

        // 按时间倒序（时间相同保持来源顺序）
        rows.sort(Comparator.comparingLong((Map<String, Object> r) -> (Long) r.get("_ts")).reversed());
        rows.forEach(r -> r.remove("_ts"));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("income", income.setScale(2, RoundingMode.HALF_UP));
        summary.put("errandExpense", errandExpense.setScale(2, RoundingMode.HALF_UP));
        summary.put("cashNet", income.subtract(errandExpense).setScale(2, RoundingMode.HALF_UP));
        summary.put("pointsIncome", pointsIncome);
        summary.put("pointsExpense", pointsExpense);
        summary.put("pointsNet", pointsIncome - pointsExpense);
        summary.put("totalRecords", rows.size());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("month", ym.toString());
        data.put("summary", summary);
        data.put("list", rows);
        data.put("hasData", !rows.isEmpty());
        return Result.success(data);
    }

    /** 组装一条账单明细，_ts 仅用于排序（返回前移除） */
    private Map<String, Object> buildRow(Object time, String type, String typeLabel,
                                         String title, Object amount, int direction, String remark) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("time", formatTime(time));
        row.put("type", type);
        row.put("typeLabel", typeLabel);
        row.put("title", title);
        row.put("amount", amount);
        row.put("direction", direction);
        row.put("remark", remark == null ? "" : remark);
        row.put("_ts", timeMillis(time));
        return row;
    }

    private String formatTime(Object value) {
        LocalDateTime time = toLocalDateTime(value);
        return time == null ? "" : time.format(MONTH_FMT);
    }

    private long timeMillis(Object value) {
        LocalDateTime time = toLocalDateTime(value);
        // 没有时间的历史数据排到最后
        return time == null ? Long.MIN_VALUE : time.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
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

    private BigDecimal toDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal b) return b;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
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
