package com.example.campusmarket.service;

import com.example.campusmarket.mapper.AutomationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 自动化预警引擎。
 *
 * 定时扫描校园市场里的经营数据，自动生成预警：
 *   1. 库存积压：单个卖家在售 ≥ 10 件
 *   2. 临期下架：商品挂牌满 27 天（30 天到期，剩 3 天）
 *   3. 滞销预警：上架超过 30 天仍未售出
 *   4. 分类缺货：某分类在售 ≤ 2 件
 *   5. 任务到期：任务大厅里 24 小时内到期的未完成任务
 *
 * 启动 1 分钟后自动跑一次，之后每 6 小时跑一次；也支持手动触发。
 */
@Service
public class AutomationService {

    /** 阈值配置：集中放在这里，后续可改为读取配置中心 */
    public static final int INVENTORY_THRESHOLD = 10;
    public static final int STALE_DAYS = 30;
    public static final int EXPIRE_DAYS = 30;
    public static final int CATEGORY_STOCK_THRESHOLD = 2;

    /** 单条规则每天最多生成的条数，避免历史数据一次性刷屏 */
    private static final int DAILY_CAP_PER_RULE = 10;

    @Autowired
    private AutomationMapper automationMapper;

    private static volatile String lastScanTime = null;
    private static volatile int lastGenerated = 0;

    @Scheduled(initialDelay = 60_000, fixedDelay = 6 * 60 * 60 * 1000)
    public void scheduledScan() {
        runScan();
    }

    /**
     * 执行一次全量扫描，返回本次新增的预警条数。
     * 同一规则 + 同一对象 + 同一天只会生成一条，重复扫描不会重复刷屏。
     */
    public synchronized int runScan() {
        int generated = 0;

        // 规则一：库存积压
        for (Map<String, Object> row : automationMapper.sellersWithStock(INVENTORY_THRESHOLD)) {
            int sellerId = toInt(row.get("sellerId"));
            int count = toInt(row.get("cnt"));
            String name = str(row.get("sellerName"), "同学");
            generated += automationMapper.insertAlert(
                    "INVENTORY_HIGH", "high",
                    "卖家「" + name + "」库存积压 " + count + " 件",
                    "在售商品已达到 " + count + " 件，建议提醒 TA 做打包优惠或降价清理，避免长期占位。",
                    "seller", sellerId, sellerId);
        }

        // 规则二：临期下架
        int expiringCount = 0;
        for (Map<String, Object> row : automationMapper.expiringGoods(EXPIRE_DAYS, DAILY_CAP_PER_RULE)) {
            if (expiringCount++ >= DAILY_CAP_PER_RULE) break;
            int id = toInt(row.get("id"));
            int leftDays = toInt(row.get("leftDays"));
            generated += automationMapper.insertAlert(
                    "GOODS_EXPIRING", "high",
                    "商品「" + str(row.get("title"), "未命名商品") + "」即将到期",
                    "已挂牌 " + toInt(row.get("days")) + " 天，还有 " + Math.max(leftDays, 1)
                            + " 天到期，建议尽快降价出手或重新发布。",
                    "goods", id, null);
        }

        // 规则三：滞销预警
        for (Map<String, Object> row : automationMapper.staleGoods(STALE_DAYS, DAILY_CAP_PER_RULE)) {
            int id = toInt(row.get("id"));
            generated += automationMapper.insertAlert(
                    "GOODS_STALE", "medium",
                    "商品「" + str(row.get("title"), "未命名商品") + "」滞销 " + toInt(row.get("days")) + " 天",
                    "当前售价 ¥" + str(row.get("price"), "0") + "，建议降价 10%-20% 或更换封面图提高曝光。",
                    "goods", id, null);
        }

        // 规则四：分类缺货
        for (Map<String, Object> row : automationMapper.lowStockCategories(CATEGORY_STOCK_THRESHOLD)) {
            int categoryId = toInt(row.get("categoryId"));
            int count = toInt(row.get("cnt"));
            String name = str(row.get("categoryName"), "该分类");
            generated += automationMapper.insertAlert(
                    "CATEGORY_STOCKOUT", "medium",
                    "分类「" + name + "」库存告急",
                    "当前仅 " + count + " 件在售，需求容易落空，建议引导同学发布或推送求购提醒。",
                    "category", categoryId, null);
        }

        // 规则五：任务到期
        for (Map<String, Object> row : automationMapper.dueTasks()) {
            int id = toInt(row.get("id"));
            int leftMinutes = toInt(row.get("leftMinutes"));
            String statusText = "open".equals(str(row.get("status"), "")) ? "还没人接单" : "正在进行中";
            generated += automationMapper.insertAlert(
                    "TASK_DUE", "high",
                    "任务「" + str(row.get("title"), "未命名任务") + "」即将到期",
                    statusText + "，约 " + Math.max(1, leftMinutes / 60) + " 小时后到期，悬赏 ¥"
                            + str(row.get("reward"), "0") + "，建议尽快推进。",
                    "task", id, null);
        }

        lastGenerated = generated;
        lastScanTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        return generated;
    }

    public static String getLastScanTime() {
        return lastScanTime;
    }

    public static int getLastGenerated() {
        return lastGenerated;
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

    private String str(Object v, String fallback) {
        if (v == null) return fallback;
        String s = String.valueOf(v);
        return s.isEmpty() ? fallback : s;
    }
}
