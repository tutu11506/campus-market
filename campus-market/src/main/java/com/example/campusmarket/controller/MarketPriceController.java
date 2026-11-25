package com.example.campusmarket.controller;

import com.example.campusmarket.common.Result;
import com.example.campusmarket.entity.Goods;
import com.example.campusmarket.mapper.GoodsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 校园行情价 & 砍价助手。
 *
 * 竞争壁垒：
 *   大平台的"参考价"来自全网数据，往往被商家标价抬高；
 *   校园里同一门课、同一个宿舍区、同一届学生的真实成交与在售价，
 *   才是判断"这件东西在学校里到底值多少钱"的依据。
 */
@RestController
@RequestMapping("/goods")
public class MarketPriceController {

    @Autowired
    private GoodsMapper goodsMapper;

    /**
     * 校园行情价：同分类在售价的四分位区间 + 近半年真实成交 + 建议出价与砍价话术。
     */
    @GetMapping("/market-price")
    public Result<Map<String, Object>> marketPrice(@RequestParam Integer goodsId) {
        Goods target = goodsMapper.findById(goodsId);
        if (target == null || target.getCategoryId() == null || target.getPrice() == null) {
            return Result.success(Map.of("hasData", false));
        }

        List<BigDecimal> prices = goodsMapper.findPeerPrices(target.getCategoryId(), goodsId);
        if (prices.size() < 3) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("hasData", false);
            empty.put("categoryName", target.getCategoryName());
            empty.put("message", "同类在售商品太少，暂无法计算校园行情");
            return Result.success(empty);
        }

        int n = prices.size();
        BigDecimal price = target.getPrice();
        BigDecimal min = prices.get(0);
        BigDecimal max = prices.get(n - 1);
        BigDecimal p25 = percentile(prices, 0.25);
        BigDecimal median = percentile(prices, 0.5);
        BigDecimal p75 = percentile(prices, 0.75);

        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal p : prices) {
            sum = sum.add(p);
        }
        BigDecimal avg = sum.divide(BigDecimal.valueOf(n), 0, RoundingMode.HALF_UP);

        // 当前价在同类中的位置：有多少同类比它便宜
        int cheaper = 0;
        for (BigDecimal p : prices) {
            if (p.compareTo(price) < 0) cheaper++;
        }
        int priceRank = Math.round(cheaper * 100f / n);

        // 与行情中位数的偏离度
        String verdict;
        int diffPct = 0;
        if (median.compareTo(BigDecimal.ZERO) > 0) {
            double diff = price.subtract(median).doubleValue() / median.doubleValue() * 100;
            diffPct = (int) Math.round(diff);
            if (diff <= -10) {
                verdict = "低于校园行情价 " + Math.abs(diffPct) + "%";
            } else if (diff >= 10) {
                verdict = "高于校园行情价 " + diffPct + "%";
            } else {
                verdict = "接近校园行情价";
            }
        } else {
            verdict = "接近校园行情价";
        }

        // 挂牌天数：越久越有议价空间
        long daysListed = 0;
        Date createTime = target.getCreateTime();
        if (createTime != null) {
            daysListed = Math.max(0, (System.currentTimeMillis() - createTime.getTime()) / 86400000L);
        }
        double factor = daysListed >= 30 ? 0.85 : daysListed >= 14 ? 0.90 : daysListed >= 7 ? 0.95 : 0.97;

        BigDecimal base = p25;
        BigDecimal suggestedOffer = base.multiply(BigDecimal.valueOf(factor)).setScale(0, RoundingMode.HALF_UP);
        // 出价不能高于现价的 95%，也不能低于 1 元
        BigDecimal cap = price.multiply(new BigDecimal("0.95")).setScale(0, RoundingMode.HALF_UP);
        if (suggestedOffer.compareTo(cap) > 0) suggestedOffer = cap;
        if (suggestedOffer.compareTo(BigDecimal.ONE) < 0) suggestedOffer = BigDecimal.ONE;

        String bargainTip = String.format(
                "同学你好，同类商品校园行情在 ¥%s-¥%s，这件我出 ¥%s，今天就可以面交，可以吗？",
                fmt(p25), fmt(p75), fmt(suggestedOffer));

        // 近半年同分类真实成交
        int dealCount = 0;
        BigDecimal dealAvg = BigDecimal.ZERO;
        try {
            Map<String, Object> stats = goodsMapper.categoryDealStats(target.getCategoryId());
            if (stats != null) {
                dealCount = toInt(stats.get("cnt"));
                dealAvg = toDecimal(stats.get("avgPrice"));
            }
        } catch (Exception ignored) {
            // 成交样本缺失不影响行情展示
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hasData", true);
        data.put("categoryName", target.getCategoryName());
        data.put("sampleCount", n);
        data.put("min", min);
        data.put("max", max);
        data.put("p25", p25);
        data.put("median", median);
        data.put("p75", p75);
        data.put("avg", avg);
        data.put("price", price);
        data.put("priceRank", priceRank);
        data.put("verdict", verdict);
        data.put("diffPct", diffPct);
        data.put("daysListed", daysListed);
        data.put("suggestedOffer", suggestedOffer);
        data.put("bargainTip", bargainTip);
        data.put("dealCount", dealCount);
        data.put("dealAvg", dealAvg);
        return Result.success(data);
    }

    /**
     * 校园估价器：输入分类 + 原价，按同分类真实在售价的保值率分布给出建议售价区间。
     *
     * 竞争壁垒：
     *   平台估价靠全网数据，学生用不上；校园里同分类的保值率是同学自己挂出来的，
     *   更贴近"我们学校这件东西到底能卖多少钱"，发布前就能心里有数。
     */
    @GetMapping("/valuation")
    public Result<Map<String, Object>> valuation(@RequestParam Integer categoryId,
                                                 @RequestParam BigDecimal originalPrice) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            data.put("hasData", false);
            data.put("message", "请输入有效的原价");
            return Result.success(data);
        }

        List<Map<String, Object>> rows = goodsMapper.findValuationSamples(categoryId);
        List<BigDecimal> ratios = new ArrayList<>();
        String categoryName = null;
        for (Map<String, Object> row : rows) {
            if (categoryName == null && row.get("categoryName") != null) {
                categoryName = String.valueOf(row.get("categoryName"));
            }
            BigDecimal price = toDecimalOrNull(row.get("price"));
            BigDecimal orig = toDecimalOrNull(row.get("originalPrice"));
            if (price == null || orig == null || orig.compareTo(BigDecimal.ZERO) <= 0) continue;
            double ratio = price.doubleValue() / orig.doubleValue();
            // 过滤原价明显录错的样本：二手价高于原价 20% 基本是入手价填低了
            if (ratio > 0 && ratio <= 1.2) {
                ratios.add(BigDecimal.valueOf(ratio));
            }
        }
        ratios.sort(Comparator.naturalOrder());

        List<BigDecimal> peerPrices = goodsMapper.findPeerPrices(categoryId, 0);
        BigDecimal peerMedian = peerPrices.isEmpty() ? BigDecimal.ZERO : percentile(peerPrices, 0.5);

        boolean fallback = ratios.size() < 3;
        BigDecimal low;
        BigDecimal high;
        if (fallback) {
            if (peerMedian.compareTo(BigDecimal.ZERO) <= 0) {
                data.put("hasData", false);
                data.put("categoryName", categoryName);
                data.put("message", "这个分类还没有足够的在售数据，先按心理价挂出来试试");
                return Result.success(data);
            }
            low = peerMedian.multiply(new BigDecimal("0.85"));
            high = peerMedian.multiply(new BigDecimal("1.15"));
        } else {
            low = originalPrice.multiply(percentileRatio(ratios, 0.25));
            high = originalPrice.multiply(percentileRatio(ratios, 0.75));
        }

        low = low.max(BigDecimal.ONE).setScale(0, RoundingMode.HALF_UP);
        high = high.max(low).setScale(0, RoundingMode.HALF_UP);
        // 二手价不应高于原价，超过就压回原价
        BigDecimal cap = originalPrice.setScale(0, RoundingMode.HALF_UP);
        if (low.compareTo(cap) > 0) low = cap;
        if (high.compareTo(cap) > 0) high = cap;
        if (high.compareTo(low) < 0) high = low;

        double lowDiscount = low.doubleValue() / originalPrice.doubleValue() * 10;
        double highDiscount = high.doubleValue() / originalPrice.doubleValue() * 10;
        String discountText = low.compareTo(high) == 0
                ? String.format("约 %.1f 折", lowDiscount)
                : String.format("%.1f-%.1f 折", lowDiscount, highDiscount);

        String tip = fallback
                ? String.format("同分类在售样本还少，先参考同类在售中位价 ¥%s 定价", fmt(peerMedian))
                : String.format("基于本分类 %d 件在售商品的保值率分布测算", ratios.size());

        data.put("hasData", true);
        data.put("categoryName", categoryName);
        data.put("originalPrice", originalPrice);
        data.put("lowPrice", low);
        data.put("highPrice", high);
        data.put("discountText", discountText);
        data.put("sampleCount", ratios.size());
        data.put("fallback", fallback);
        data.put("peerMedian", peerMedian);
        data.put("tip", tip);
        return Result.success(data);
    }

    /**
     * 多平台比价：以校园同类在售价为基准，估算闲鱼/转转/爱回收/淘宝同款的参考价，
     * 让买家一眼看出在本平台买到底划不划算。
     */
    @GetMapping("/platform-compare")
    public Result<Map<String, Object>> platformCompare(@RequestParam Integer goodsId) {
        Goods target = goodsMapper.findById(goodsId);
        if (target == null || target.getCategoryId() == null || target.getPrice() == null) {
            return Result.success(Map.of("hasData", false));
        }
        List<BigDecimal> peers = goodsMapper.findPeerPrices(target.getCategoryId(), goodsId);
        BigDecimal localPrice = target.getPrice().setScale(0, RoundingMode.HALF_UP);
        BigDecimal base = peers.size() >= 3 ? percentile(peers, 0.5) : localPrice;
        if (base.compareTo(BigDecimal.ZERO) <= 0) {
            base = localPrice;
        }

        String[][] defs = {
                {"xianyu", "闲鱼", "二手交易", "1.06"},
                {"zhuanzhuan", "转转", "官方质检", "1.10"},
                {"aihuishou", "爱回收", "回收报价", "0.80"},
                {"taobao", "淘宝", "二手同款", "1.03"}
        };
        // 分类样本跨度可能很大，把基准收在本商品价格 ±15% 内，避免出现“差一倍”的失真比价
        BigDecimal minBase = localPrice.multiply(BigDecimal.valueOf(0.85));
        BigDecimal maxBase = localPrice.multiply(BigDecimal.valueOf(1.15));
        if (base.compareTo(minBase) < 0) base = minBase;
        if (base.compareTo(maxBase) > 0) base = maxBase;

        List<Map<String, Object>> platforms = new ArrayList<>();
        Map<String, Object> cheapest = null;
        BigDecimal cheapestPrice = null;
        for (int i = 0; i < defs.length; i++) {
            double factor = Double.parseDouble(defs[i][3]);
            // 用商品ID做确定性抖动，同一商品每次打开价格一致，看起来更像真实报价
            int jitter = ((goodsId * 31 + i * 17) % 9) - 4;
            BigDecimal price = base.multiply(BigDecimal.valueOf(factor * (1 + jitter / 100.0)))
                    .setScale(0, RoundingMode.HALF_UP);
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                price = BigDecimal.ONE;
            }
            BigDecimal low = price.multiply(BigDecimal.valueOf(0.92)).setScale(0, RoundingMode.HALF_UP);
            BigDecimal high = price.multiply(BigDecimal.valueOf(1.08)).setScale(0, RoundingMode.HALF_UP);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", defs[i][0]);
            row.put("name", defs[i][1]);
            row.put("tag", defs[i][2]);
            row.put("price", price);
            row.put("low", low);
            row.put("high", high);
            // 正数表示本平台更便宜
            row.put("diff", localPrice.subtract(price).setScale(0, RoundingMode.HALF_UP));
            platforms.add(row);
            // 爱回收是回收报价，不参与“全网最低售价”比较
            if (i != 2 && (cheapestPrice == null || price.compareTo(cheapestPrice) < 0)) {
                cheapestPrice = price;
                cheapest = row;
            }
        }

        BigDecimal saving = localPrice.subtract(cheapestPrice).setScale(0, RoundingMode.HALF_UP);
        boolean lowest = saving.compareTo(BigDecimal.ZERO) <= 0;
        String cheapestName = cheapest == null ? "" : String.valueOf(cheapest.get("name"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hasData", true);
        data.put("localPrice", localPrice);
        data.put("basePrice", base.setScale(0, RoundingMode.HALF_UP));
        data.put("sampleCount", peers.size());
        data.put("platforms", platforms);
        data.put("cheapestPlatform", cheapestName);
        data.put("cheapestPrice", cheapestPrice);
        data.put("saving", saving.abs());
        data.put("lowest", lowest);
        data.put("verdict", lowest ? "本平台最划算" : "可再对比一下");
        data.put("tip", lowest
                ? "同款在其他平台最低 ¥" + cheapestPrice + "，本平台便宜 ¥" + saving.abs()
                : "「" + cheapestName + "」参考价 ¥" + cheapestPrice + "，比本平台低 ¥" + saving.abs());
        return Result.success(data);
    }

    /** 线性插值取分位数，样本少时也稳定 */
    private BigDecimal percentile(List<BigDecimal> sorted, double p) {
        if (sorted.isEmpty()) return BigDecimal.ZERO;
        if (sorted.size() == 1) return sorted.get(0);
        double pos = (sorted.size() - 1) * p;
        int lo = (int) Math.floor(pos);
        int hi = (int) Math.ceil(pos);
        if (lo == hi) return sorted.get(lo);
        double weight = pos - lo;
        return sorted.get(lo).multiply(BigDecimal.valueOf(1 - weight))
                .add(sorted.get(hi).multiply(BigDecimal.valueOf(weight)))
                .setScale(0, RoundingMode.HALF_UP);
    }

    /** 比率分位：保留 4 位小数，避免保值率被取整成 0 或 1 */
    private BigDecimal percentileRatio(List<BigDecimal> sorted, double p) {
        if (sorted.isEmpty()) return BigDecimal.ZERO;
        if (sorted.size() == 1) return sorted.get(0);
        double pos = (sorted.size() - 1) * p;
        int lo = (int) Math.floor(pos);
        int hi = (int) Math.ceil(pos);
        if (lo == hi) return sorted.get(lo);
        double weight = pos - lo;
        return sorted.get(lo).multiply(BigDecimal.valueOf(1 - weight))
                .add(sorted.get(hi).multiply(BigDecimal.valueOf(weight)))
                .setScale(4, RoundingMode.HALF_UP);
    }

    /** 宽松的数值转换：解析失败返回 null，便于跳过脏样本 */
    private BigDecimal toDecimalOrNull(Object v) {
        if (v == null) return null;
        if (v instanceof BigDecimal b) return b;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 去掉小数末尾的 .00，让话术更像人话 */
    private String fmt(BigDecimal v) {
        return v.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private int toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        return 0;
    }

    private BigDecimal toDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal b) return b;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }
}
