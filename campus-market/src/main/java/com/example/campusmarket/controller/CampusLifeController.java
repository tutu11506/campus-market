package com.example.campusmarket.controller;

import com.example.campusmarket.common.Result;
import com.example.campusmarket.mapper.GoodsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 校园生活助手接口 —— 面交点热力 + 校园交易高峰时段。
 *
 * 竞争壁垒：
 *   闲鱼/转转的交易发生在"快递"里，没有"面交"这个概念；
 *   校园二手的主战场是操场门口、宿舍楼下、食堂门口。
 *   只有知道了学校里的真实面交热点和高频交易时段，买家卖家才能高效碰头。
 */
@RestController
@RequestMapping("/home")
public class CampusLifeController {

    @Autowired
    private GoodsMapper goodsMapper;

    /**
     * 校园面交点热力：按交易地点聚合在售商品，返回 top 8 热点。
     * 热度按排名分 3 档：hot / warm / normal。
     */
    @GetMapping("/pickup-spots")
    public Result<Map<String, Object>> pickupSpots() {
        List<Map<String, Object>> rows = goodsMapper.pickupSpots(8);

        List<Map<String, Object>> spots = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", row.get("name"));
            item.put("count", toInt(row.get("count")));
            item.put("avgPrice", row.get("avgPrice"));
            item.put("heat", heatLevel(i));
            spots.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("spots", spots);
        data.put("total", spots.size());
        data.put("hasData", !spots.isEmpty());
        return Result.success(data);
    }

    /**
     * 校园交易高峰时段：统计订单按小时的分布，找出连续 3 小时成交最多的窗口。
     * 用 pay_time 优先（真实成交时间），没有则回退 create_time。
     */
    @GetMapping("/active-hours")
    public Result<Map<String, Object>> activeHours() {
        List<Map<String, Object>> rows = goodsMapper.activeHours();

        int[] counts = new int[24];
        int total = 0;
        for (Map<String, Object> row : rows) {
            int hour = toInt(row.get("hour"));
            int count = toInt(row.get("count"));
            if (hour >= 0 && hour < 24) {
                counts[hour] += count;
                total += count;
            }
        }

        // 找连续 3 小时的最佳窗口；总量太少时窗口仍然给出，只是提示数据有限
        int bestStart = 0;
        int bestSum = -1;
        for (int start = 0; start < 24; start++) {
            int sum = 0;
            for (int i = 0; i < 3; i++) {
                sum += counts[(start + i) % 24];
            }
            if (sum > bestSum) {
                bestSum = sum;
                bestStart = start;
            }
        }
        int bestEnd = (bestStart + 3) % 24;

        List<Map<String, Object>> hours = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("hour", h);
            item.put("count", counts[h]);
            hours.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hours", hours);
        data.put("total", total);
        data.put("peakStart", bestStart);
        data.put("peakEnd", bestEnd);
        data.put("peakLabel", String.format("%02d:00-%02d:00", bestStart, bestEnd));
        data.put("peakCount", Math.max(bestSum, 0));
        data.put("peakTip", peakTip(bestStart));
        data.put("hasData", total > 0);
        return Result.success(data);
    }

    /**
     * 校园行情总览：各分类在售件数 + 均价 + 最低价。
     * 让同学一眼看清"校园里各类东西大概什么价"，再决定逛哪个分类。
     */
    @GetMapping("/market-overview")
    public Result<Map<String, Object>> marketOverview() {
        List<Map<String, Object>> rows = goodsMapper.categoryMarketOverview();
        List<Map<String, Object>> items = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            int count = toInt(row.get("count"));
            if (count <= 0) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("categoryId", row.get("categoryId"));
            item.put("categoryName", row.get("categoryName"));
            item.put("count", count);
            item.put("avgPrice", toInt(row.get("avgPrice")));
            item.put("minPrice", toInt(row.get("minPrice")));
            items.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("hasData", !items.isEmpty());
        return Result.success(data);
    }

    /** 学科分组：名称 + 书名关键词（只匹配书名首词，出版社名不会参与归类） */
    private static final String[][] TEXTBOOK_SUBJECTS = {
            {"医学", "医学,口腔,临床,护理,药学,皮肤,内科,外科,齿"},
            {"数学", "数学,代数,概率,微积分,统计"},
            {"电子电气", "电路,电气,通信,PLC,电工,电子"},
            {"经管法学", "管理,经济,会计,营销,法学,法律,金融,市场"},
            {"计算机", "计算机,软件,编程,ROS,算法,数据,人工智能,程序"},
            {"机械", "机械,制图,数控,机电"}
    };

    /**
     * 教材循环站：按学科聚合教材分类的在售件数与价格。
     *
     * 竞争壁垒：
     *   闲鱼/转转的教材是"一本书"一条链接，学生得自己翻；
     *   校园里同一门课的教材高度重合，按学科聚合后才能一眼看到
     *   "我们专业这学期要用的书，学校里有多少人在出、最低多少钱"。
     */
    @GetMapping("/textbook-subjects")
    public Result<Map<String, Object>> textbookSubjects() {
        List<Map<String, Object>> rows = goodsMapper.findTextbookRows();

        Map<String, List<BigDecimal>> grouped = new LinkedHashMap<>();
        for (String[] def : TEXTBOOK_SUBJECTS) {
            grouped.put(def[0], new ArrayList<>());
        }

        int textbookTotal = 0;
        int matched = 0;
        for (Map<String, Object> row : rows) {
            String book = row.get("book") == null ? "" : String.valueOf(row.get("book")).trim().toLowerCase();
            BigDecimal price = toDecimal(row.get("price"));
            if (book.isEmpty() || price == null) continue;
            textbookTotal++;

            for (String[] def : TEXTBOOK_SUBJECTS) {
                boolean hit = false;
                for (String kw : def[1].split(",")) {
                    if (book.contains(kw.toLowerCase())) {
                        hit = true;
                        break;
                    }
                }
                if (hit) {
                    grouped.get(def[0]).add(price);
                    matched++;
                    break;
                }
            }
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (String[] def : TEXTBOOK_SUBJECTS) {
            List<BigDecimal> prices = grouped.get(def[0]);
            if (prices.isEmpty()) continue;

            BigDecimal sum = BigDecimal.ZERO;
            BigDecimal min = prices.get(0);
            for (BigDecimal p : prices) {
                sum = sum.add(p);
                if (p.compareTo(min) < 0) min = p;
            }
            BigDecimal avg = sum.divide(BigDecimal.valueOf(prices.size()), 0, RoundingMode.HALF_UP);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", def[0]);
            item.put("keyword", def[1].split(",")[0]);
            item.put("count", prices.size());
            item.put("minPrice", min.setScale(0, RoundingMode.HALF_UP));
            item.put("avgPrice", avg);
            items.add(item);
        }
        items.sort((a, b) -> Integer.compare(toInt(b.get("count")), toInt(a.get("count"))));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("textbookTotal", textbookTotal);
        data.put("matched", matched);
        data.put("hasData", !items.isEmpty());
        return Result.success(data);
    }

    /**
     * 校园捡漏榜：当前价低于同分类在售均价 40% 以上的商品。
     * 返回最多 6 件，附带"比同类均价便宜 X%"与同类均价，方便同学一眼判断值不值。
     */
    @GetMapping("/deal-radar")
    public Result<Map<String, Object>> dealRadar() {
        List<Map<String, Object>> rows = goodsMapper.findDealRadar(30);
        List<Map<String, Object>> items = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            BigDecimal price = toDecimal(row.get("price"));
            BigDecimal categoryAvg = toDecimal(row.get("categoryAvg"));
            if (price == null || categoryAvg == null || categoryAvg.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            // 过滤明显的测试/异常挂单，避免榜单被 ¥1、单字符标题污染
            String title = row.get("title") == null ? "" : String.valueOf(row.get("title")).trim();
            if (title.length() < 2 || price.compareTo(new BigDecimal("5")) < 0) {
                continue;
            }
            int savePct = (int) Math.round((1 - price.doubleValue() / categoryAvg.doubleValue()) * 100);
            if (savePct < 30) continue;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("title", row.get("title"));
            item.put("price", price.setScale(2, RoundingMode.HALF_UP));
            item.put("coverImage", row.get("coverImage"));
            item.put("images", row.get("images"));
            item.put("categoryId", row.get("categoryId"));
            item.put("categoryName", row.get("categoryName"));
            item.put("viewCount", toInt(row.get("viewCount")));
            item.put("categoryAvg", categoryAvg.setScale(0, RoundingMode.HALF_UP));
            item.put("savePct", savePct);
            items.add(item);
            if (items.size() >= 6) break;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("hasData", !items.isEmpty());
        return Result.success(data);
    }

    /** 根据真实校园作息给高峰时段配一句人话说明 */
    private String peakTip(int start) {
        if (start >= 6 && start < 10) return "早课前的空档，适合顺路面交";
        if (start >= 11 && start < 14) return "午饭时间，食堂门口最容易碰头";
        if (start >= 14 && start < 17) return "下午课间，宿舍楼下人来人往";
        if (start >= 17 && start < 20) return "晚饭后散步高峰，操场门口面交最多";
        if (start >= 20 && start < 23) return "晚自习后回宿舍，顺手就能交易";
        if (start >= 23 || start < 6) return "深夜交易人少，注意结伴与安全";
        return "这个时段校内同学比较活跃";
    }

    /** 按排名分热度：前三热门、四到六温热、其余普通，避免所有地点同色 */
    private String heatLevel(int rank) {
        if (rank < 3) return "hot";
        if (rank < 6) return "warm";
        return "normal";
    }

    /**
     * 同地点交易榜：点击某个面交地点，看这个地点的交易全貌。
     *
     * 竞争壁垒：
     *   大平台的二手交易发生在快递里，没有"同一个地点的交易"这个维度；
     *   校园里南区宿舍、北区食堂、图书馆门口的交易密度和内容完全不同，
     *   只有校园平台能告诉同学"你宿舍楼下在出什么、多久能卖出去"。
     */
    @GetMapping("/location-detail")
    public Result<Map<String, Object>> locationDetail(@RequestParam String name) {
        Map<String, Object> data = new LinkedHashMap<>();

        Map<String, Object> onSale = goodsMapper.locationOnSaleStats(name);
        int onSaleCount = toInt(onSale == null ? null : onSale.get("count"));
        if (onSaleCount == 0) {
            data.put("hasData", false);
            data.put("message", "这个地点暂时没有在售商品");
            return Result.success(data);
        }

        List<Map<String, Object>> topCats = goodsMapper.locationTopCategories(name);
        Map<String, Object> deals = goodsMapper.locationDealStats(name);
        List<Map<String, Object>> samples = goodsMapper.locationSampleGoods(name, 8);

        data.put("hasData", true);
        data.put("location", name);
        data.put("onSaleCount", onSaleCount);
        data.put("onSaleAvgPrice", onSale.get("avgPrice"));
        data.put("onSaleMinPrice", onSale.get("minPrice"));
        data.put("onSaleMaxPrice", onSale.get("maxPrice"));
        data.put("topCategories", topCats);
        data.put("dealCount90d", toInt(deals == null ? null : deals.get("dealCount")));
        data.put("dealAmount90d", deals == null ? 0 : deals.get("dealAmount"));
        data.put("sampleGoods", samples);
        data.put("summary", buildLocationSummary(name, onSaleCount, topCats));
        return Result.success(data);
    }

    /** 根据地点和交易情况生成一句人话总结 */
    private String buildLocationSummary(String name, int count, List<Map<String, Object>> topCats) {
        if (count <= 0) return name + "暂时没有在售商品";
        StringBuilder sb = new StringBuilder(name).append("现在有 ").append(count).append(" 件在售");
        if (topCats != null && !topCats.isEmpty()) {
            sb.append("，").append(topCats.get(0).get("categoryName")).append("最多");
            if (topCats.size() >= 2) {
                sb.append("，其次是").append(topCats.get(1).get("categoryName"));
            }
        }
        return sb.toString();
    }

    /**
     * 成交风向标：各分类近 12 周的成交趋势 + 在售均价对比。
     *
     * 竞争壁垒：
     *   大平台的"价格走势"是全网数据，掺杂了商家和溢价；
     *   校园里真实发生在同学之间的成交才有参考价值，
     *   开学季/毕业季哪些品类在涨价、哪些在清仓，一看便知。
     */
    @GetMapping("/deal-trend")
    public Result<Map<String, Object>> dealTrend() {
        List<Map<String, Object>> rows = goodsMapper.dealTrendByWeekCategory();
        List<Map<String, Object>> baseline = goodsMapper.categoryOnSaleBaseline();

        // 按分类聚合出：总成交量、成交均价、在售均价基准
        Map<String, Map<String, Object>> byCategory = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String cat = String.valueOf(row.get("categoryName"));
            Map<String, Object> slot = byCategory.computeIfAbsent(cat, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("categoryName", k);
                m.put("totalDeals", 0);
                m.put("priceSum", BigDecimal.ZERO);
                return m;
            });
            int cnt = toInt(row.get("dealCount"));
            slot.put("totalDeals", toInt(slot.get("totalDeals")) + cnt);
            slot.put("priceSum", ((BigDecimal) slot.get("priceSum"))
                    .add(toDecimal(row.get("avgPrice")).multiply(BigDecimal.valueOf(cnt))));
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> e : byCategory.entrySet()) {
            Map<String, Object> cat = e.getValue();
            int totalDeals = toInt(cat.get("totalDeals"));
            if (totalDeals < 1) continue;
            BigDecimal dealAvg = ((BigDecimal) cat.get("priceSum"))
                    .divide(BigDecimal.valueOf(totalDeals), 0, RoundingMode.HALF_UP);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("categoryName", e.getKey());
            item.put("dealCount", totalDeals);
            item.put("dealAvgPrice", dealAvg);

            // 找到在售均价基准
            BigDecimal onSaleAvg = BigDecimal.ZERO;
            for (Map<String, Object> b : baseline) {
                if (e.getKey().equals(b.get("categoryName"))) {
                    onSaleAvg = toDecimal(b.get("avgOnSalePrice"));
                    item.put("onSaleCount", toInt(b.get("onSaleCount")));
                    break;
                }
            }
            item.put("onSaleAvgPrice", onSaleAvg);

            // 判断"成交价比在售均价低/高 X%" —— 真实成交价 vs 挂单价的偏离度
            String verdict = "成交均价接近在售均价";
            if (onSaleAvg.compareTo(BigDecimal.ZERO) > 0) {
                double diff = (dealAvg.subtract(onSaleAvg).doubleValue() / onSaleAvg.doubleValue()) * 100;
                if (diff <= -15) verdict = String.format("成交价比挂单价低 %.0f%%，这个分类议价空间大", Math.abs(diff));
                else if (diff >= 15) verdict = String.format("成交价比挂单价高 %.0f%%，热门品类不愁卖", diff);
            }
            item.put("verdict", verdict);
            items.add(item);
        }
        items.sort((a, b) -> Integer.compare(toInt(b.get("dealCount")), toInt(a.get("dealCount"))));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("totalDeals", items.stream().mapToInt(it -> toInt(it.get("dealCount"))).sum());
        data.put("hasData", !items.isEmpty());
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

    private BigDecimal toDecimal(Object v) {
        if (v == null) return null;
        if (v instanceof BigDecimal b) return b;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 宿舍楼下好货 "邻里聚"：按宿舍区聚合在售闲置。
     *
     * 竞争壁垒：
     *   大平台的二手交易是"城市级别"的，不会告诉你"你住的那栋楼在出什么"；
     *   校园里真正高频的面交场景是"室友邻居间"，
     *   知道隔壁楼下有人在出二手显示器/台灯，走两步就能验机，体验完全不同。
     */
    @GetMapping("/dorm-goods")
    public Result<Map<String, Object>> dormGoods() {
        List<Map<String, Object>> rows = goodsMapper.dormAreaStats(10);

        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String area = row.get("dormArea") == null ? "" : String.valueOf(row.get("dormArea"));
            int count = toInt(row.get("count"));
            if (area.isEmpty() || count <= 0) continue;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("area", area);
            item.put("count", count);
            item.put("avgPrice", toInt(row.get("avgPrice")));
            item.put("minPrice", toInt(row.get("minPrice")));
            item.put("summary", buildDormSummary(area, count));
            items.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("totalDormAreas", items.size());
        data.put("hasData", !items.isEmpty());
        return Result.success(data);
    }

    /** 某个宿舍区的在售商品样本。 */
    @GetMapping("/dorm-detail")
    public Result<Map<String, Object>> dormDetail(@RequestParam String area) {
        Map<String, Object> data = new LinkedHashMap<>();
        List<Map<String, Object>> samples = goodsMapper.dormAreaSample(area, 8);

        if (samples.isEmpty()) {
            data.put("hasData", false);
            data.put("message", "这个宿舍区暂时没有在售闲置");
            return Result.success(data);
        }

        int totalCount = 0;
        for (Map<String, Object> s : samples) totalCount++;

        data.put("hasData", true);
        data.put("area", area);
        data.put("goods", samples);
        data.put("summary", String.format("%s附近有 %d 件闲置，点商品直接看详情", area, totalCount));
        return Result.success(data);
    }

    /** 根据宿舍区和件数生成一句人话总结 */
    private String buildDormSummary(String area, int count) {
        if (count <= 3) return area + "有几件邻居闲置，下楼就能看";
        if (count <= 10) return area + "的邻居们在出闲置，挑挑看有啥用得上的";
        return area + "的闲置特别多，逛逛说不定能淘到好东西";
    }

    /**
     * 求购热力榜 "缺口扫描"：找出大家都在看但没人卖的品类。
     *
     * 竞争壁垒：
     *   大平台都是"卖家中心"思路，告诉买家"有什么"；
     *   但校园里很多时候是"我想买个二手自行车，翻遍了都没人出" ——
     *   这个榜直接告诉卖家和平台："XX 分类有 52 人在看，只有 3 件在售"，
     *   反向撮合让供给自动匹配需求，这是大平台不会做的精细化粒度。
     *
     * 热度分 = log(浏览人数+1) * 浏览次数 / (在售件数+1) / max(近30天成交, 1)
     * → 浏览多、在售少、成交少 = 热度高 = 求购缺口大
     */
    @GetMapping("/demand-heat")
    public Result<Map<String, Object>> demandHeat() {
        List<Map<String, Object>> rows = goodsMapper.demandHeatByCategory(30);

        // 计算每个分类的求购热度分
        List<Map<String, Object>> scored = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            int browseUsers = toInt(row.get("browseUsers"));
            int browseCount = toInt(row.get("browseCount"));
            int onSaleCount = toInt(row.get("onSaleCount"));
            int deal30d = toInt(row.get("dealCount30d"));
            if (browseUsers < 1) continue;

            // 热度分：浏览人数对数增长 × 浏览次数 / 在售件数 / max(成交, 1)
            // 浏览多、在售少、成交少 → 热度高（大家想买但没人卖）
            double logUsers = Math.log(browseUsers + 1);
            double supplyFactor = 1.0 / (onSaleCount + 1);
            double dealFactor = 1.0 / Math.max(deal30d, 1);
            double heatScore = logUsers * browseCount * supplyFactor * dealFactor;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("categoryId", row.get("categoryId"));
            item.put("categoryName", row.get("categoryName"));
            item.put("browseUsers", browseUsers);
            item.put("browseCount", browseCount);
            item.put("onSaleCount", onSaleCount);
            item.put("dealCount30d", deal30d);
            item.put("heatScore", Math.round(heatScore * 1000) / 1000.0);
            item.put("heatLevel", heatLevelByScore(heatScore));
            item.put("tip", buildDemandTip(browseUsers, onSaleCount, deal30d));
            scored.add(item);
        }

        // 按热度分降序排，取 TOP 6
        scored.sort((a, b) -> Double.compare((Double) b.get("heatScore"), (Double) a.get("heatScore")));
        List<Map<String, Object>> top = scored.stream().limit(6).toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", top);
        data.put("total", top.size());
        data.put("hasData", !top.isEmpty());
        return Result.success(data);
    }

    /** 根据热度分分档：爆火/缺口/关注 */
    private String heatLevelByScore(double score) {
        if (score >= 50) return "exploding";
        if (score >= 15) return "gap";
        return "watch";
    }

    /** 生成面向用户的求购提示文案 */
    private String buildDemandTip(int users, int onSale, int deal) {
        if (onSale == 0) {
            return String.format("%d 位同学在找，但暂时没人卖", users);
        }
        double ratio = (double) users / (onSale + 1);
        if (ratio >= 10) {
            return String.format("%d 人在看，只有 %d 件在售，竞争激烈", users, onSale);
        }
        if (deal == 0 && ratio >= 3) {
            return String.format("近 30 天没成交，但 %d 人在关注", users);
        }
        return String.format("%d 人在看 · %d 件在售 · 30 天成交 %d", users, onSale, deal);
    }

    /**
     * 毕业清仓专区 "急出榜"：应届毕业生的在售商品。
     *
     * 竞争壁垒：
     *   大平台不会知道用户什么时候毕业，但我们能从学号推断届数；
     *   毕业生快离校时是最急着卖的，砍价成功率高，也是最诚实的（快毕业了没必要骗人）。
     *   展示"离校还有 X 天"，一眼看出哪些是真·急出。
     */
    @GetMapping("/graduation-sale")
    public Result<Map<String, Object>> graduationSale() {
        List<Map<String, Object>> rows = goodsMapper.graduationSaleGoods(12);

        // 当前是否是毕业季（5-8月）
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int month = cal.get(java.util.Calendar.MONTH) + 1;
        boolean isGradSeason = month >= 5 && month <= 8;

        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("title", row.get("title"));
            item.put("price", row.get("price"));
            item.put("coverImage", row.get("coverImage"));
            item.put("sellerName", row.get("sellerName"));

            // 算离校倒计时：7月1日为默认离校日
            String studentNo = row.get("sellerStudentNo") == null ? "" : String.valueOf(row.get("sellerStudentNo"));
            int years = parseEnrollYear(studentNo);
            if (years > 0) {
                java.util.Calendar cal2 = java.util.Calendar.getInstance();
                cal2.set(cal.get(java.util.Calendar.YEAR), java.util.Calendar.JULY, 1);
                if (cal2.before(cal)) cal2.add(java.util.Calendar.YEAR, 1);
                long diff = (cal2.getTimeInMillis() - cal.getTimeInMillis()) / (1000 * 60 * 60 * 24);
                item.put("daysToGrad", (int) diff);
                item.put("gradLabel", (int) diff <= 7 ? "马上离校" : (int) diff <= 30 ? "即将离校" : "毕业清仓");
            } else {
                item.put("daysToGrad", null);
                item.put("gradLabel", "毕业清仓");
            }
            items.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("isGradSeason", isGradSeason);
        data.put("hasData", !items.isEmpty());
        return Result.success(data);
    }

    /** 从学号前4位提取入学年份；非法学号返回 0 */
    private int parseEnrollYear(String studentNo) {
        if (studentNo == null || studentNo.length() < 4) return 0;
        try {
            String prefix = studentNo.substring(0, 4);
            if (!prefix.matches("\\d{4}")) return 0;
            int year = Integer.parseInt(prefix);
            return year >= 2000 && year <= 2100 ? year : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 校园教材价格指数：教材分类的成交均价趋势 + 时机判断。
     *
     * 竞争壁垒：
     *   大平台的教材价格是全网数据，混了商家溢价、二手书商炒价；
     *   但校园里真实的学生间成交才有参考价值。
     *   开学季（8-9月）教材涨价、毕业季（5-6月）跌价 ——
     *   知道这个时机，买家能省 30%，卖家能快速清仓。
     */
    @GetMapping("/textbook-index")
    public Result<Map<String, Object>> textbookIndex() {
        Map<String, Object> current = goodsMapper.textbookCurrentStats();
        Map<String, Object> prev = goodsMapper.textbookPrevStats();
        List<Map<String, Object>> weekly = goodsMapper.textbookWeeklyStats();

        int deal30d = toInt(current == null ? null : current.get("dealCount30d"));
        int dealAvg30d = toInt(current == null ? null : current.get("dealAvg30d"));
        int onSaleAvgNow = toInt(current == null ? null : current.get("onSaleAvgNow"));
        int onSaleCountNow = toInt(current == null ? null : current.get("onSaleCountNow"));
        int prevAvg = toInt(prev == null ? null : prev.get("prevAvg"));

        // 价格趋势判断
        String trend = "stable";   // up / down / stable
        double changePct = 0;
        if (prevAvg > 0 && dealAvg30d > 0) {
            changePct = ((double) dealAvg30d - prevAvg) / prevAvg * 100;
            if (changePct >= 8) trend = "up";
            else if (changePct <= -8) trend = "down";
        }

        // 时机建议：开学季 vs 毕业季
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int month = cal.get(java.util.Calendar.MONTH) + 1;
        String suggestion;
        String suggestionTag;
        if (month >= 8 && month <= 9) {
            suggestion = "开学季教材涨价中，建议先蹭学长学姐的旧书或等 10 月再买";
            suggestionTag = "买家等等";
        } else if (month >= 5 && month <= 7) {
            suggestion = "毕业季！学长学姐急着甩卖，现在入手全年最低价";
            suggestionTag = "黄金入手";
        } else if (changePct <= -8) {
            suggestion = "教材价格正在下跌，可以再等等看";
            suggestionTag = "观望";
        } else if (changePct >= 8) {
            suggestion = "教材价格涨了，急着用就买，不急可以等等";
            suggestionTag = "涨价中";
        } else {
            suggestion = "教材价格平稳，刚需可以入手";
            suggestionTag = "平稳期";
        }

        // 周数据（倒序→正序展示）
        List<Map<String, Object>> weeks = new ArrayList<>();
        for (int i = weekly.size() - 1; i >= 0; i--) {
            weeks.add(weekly.get(i));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deal30d", deal30d);
        data.put("dealAvg30d", dealAvg30d);
        data.put("onSaleAvgNow", onSaleAvgNow);
        data.put("onSaleCountNow", onSaleCountNow);
        data.put("prevAvg", prevAvg);
        data.put("trend", trend);
        data.put("changePct", Math.round(changePct * 10) / 10.0);
        data.put("suggestion", suggestion);
        data.put("suggestionTag", suggestionTag);
        data.put("weeks", weeks);
        data.put("currentMonth", month);
        data.put("hasData", deal30d > 0 || onSaleCountNow > 0);
        return Result.success(data);
    }
}
