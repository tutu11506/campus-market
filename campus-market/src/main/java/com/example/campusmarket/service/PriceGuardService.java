package com.example.campusmarket.service;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.entity.Goods;
import com.example.campusmarket.entity.PriceAlert;
import com.example.campusmarket.mapper.PriceAlertMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 价格监控预警：热门教材（教材分类或已关联教材主表）同书/同分类对比近期在售均价，
 * 明显高于均价时给商品打标并生成预警，管理端处理，买家侧展示「高于近期均价」。
 */
@Service
public class PriceGuardService {

    private static final Logger log = LoggerFactory.getLogger(PriceGuardService.class);

    /** 至少 3 个同类在售样本才有参考意义 */
    public static final int MIN_SAMPLE = 3;
    /** 达到均价的 1.5 倍生成预警 */
    public static final double ALERT_RATIO = 1.5;
    /** 达到均价的 2.5 倍视为严重 */
    public static final double SERIOUS_RATIO = 2.5;
    /** 达到均价的 1.2 倍，买家侧就提示「高于近期均价」 */
    public static final double HINT_RATIO = 1.2;

    @Autowired
    private PriceAlertMapper priceAlertMapper;

    /** 买家侧价格洞察（公开） */
    public Map<String, Object> insight(Goods goods) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (goods == null) return data;
        data.put("goodsId", goods.getId());
        data.put("price", goods.getPrice());
        data.put("avgPrice", null);
        data.put("sampleSize", 0);
        data.put("rate", BigDecimal.ZERO);
        data.put("percent", 0);
        data.put("overpriced", false);
        data.put("note", null);
        if (goods.getId() == null || goods.getCategoryId() == null || goods.getPrice() == null
                || goods.getPrice().doubleValue() <= 0) {
            return data;
        }
        // 只监控热门教材（教材分类或已关联教材主表），避免数码/服饰等大跨度品类误报
        boolean textbook = goods.getTextbookId() != null || goods.getCategoryId() == 1;
        if (!textbook) {
            return data;
        }

        int samples = priceAlertMapper.countSamples(goods.getCategoryId(), goods.getTextbookId(), goods.getId());
        BigDecimal avg = priceAlertMapper.avgPrice(goods.getCategoryId(), goods.getTextbookId(), goods.getId());
        data.put("sampleSize", samples);
        data.put("avgPrice", avg);
        if (samples < MIN_SAMPLE || avg == null || avg.doubleValue() <= 0) {
            return data;
        }

        BigDecimal ratio = goods.getPrice().divide(avg, 2, RoundingMode.HALF_UP);
        int percent = ratio.subtract(BigDecimal.ONE).multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP).intValue();
        boolean overpriced = ratio.doubleValue() >= HINT_RATIO;
        data.put("rate", ratio);
        data.put("percent", percent);
        data.put("overpriced", overpriced);
        if (overpriced) {
            data.put("note", "该商品价格高于近期同类均价 " + percent + "%（均价 ¥" + avg
                    + "，样本 " + samples + " 件），请注意甄别");
        }
        return data;
    }

    /**
     * 商品发布后自动巡检：达阈值则打标 + 生成预警。
     * 巡检失败不阻断发布，只记录日志。
     */
    public void inspectAfterPublish(Goods goods) {
        try {
            if (goods == null || goods.getId() == null) return;
            Map<String, Object> data = insight(goods);
            Integer samples = (Integer) data.get("sampleSize");
            BigDecimal avg = (BigDecimal) data.get("avgPrice");
            BigDecimal ratio = (BigDecimal) data.get("rate");
            boolean alert = samples != null && samples >= MIN_SAMPLE && avg != null && ratio != null
                    && ratio.doubleValue() >= ALERT_RATIO;

            if (!alert) {
                priceAlertMapper.updateGoodsFlag(goods.getId(), 0, null);
                return;
            }
            String level = ratio.doubleValue() >= SERIOUS_RATIO ? "serious" : "warning";
            String note = buildNote(ratio, avg, samples);
            priceAlertMapper.updateGoodsFlag(goods.getId(), 1, note);

            if (priceAlertMapper.countOpenByGoods(goods.getId()) == 0) {
                PriceAlert alertRow = new PriceAlert();
                alertRow.setGoodsId(goods.getId());
                alertRow.setSellerId(goods.getSellerId());
                alertRow.setTitle(goods.getTitle());
                alertRow.setPrice(goods.getPrice());
                alertRow.setAvgPrice(avg);
                alertRow.setRatio(ratio);
                alertRow.setSampleSize(samples);
                alertRow.setLevel(level);
                alertRow.setStatus("open");
                alertRow.setNote(note);
                priceAlertMapper.insert(alertRow);
            }
        } catch (Exception e) {
            log.warn("价格巡检失败，商品 {}：{}", goods == null ? null : goods.getId(), e.getMessage());
        }
    }

    private String buildNote(BigDecimal ratio, BigDecimal avg, int samples) {
        int percent = ratio.subtract(BigDecimal.ONE).multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP).intValue();
        return "高于近期同类均价 " + percent + "%（均价 ¥" + avg + "，样本 " + samples + " 件）";
    }

    /** 管理端一键巡检全部在售商品，返回扫描与命中数量 */
    public Map<String, Object> rescanAll() {
        List<Goods> goodsList = priceAlertMapper.findOnSaleGoods();
        int hit = 0;
        for (Goods goods : goodsList) {
            Map<String, Object> data = insight(goods);
            Integer samples = (Integer) data.get("sampleSize");
            BigDecimal ratio = (BigDecimal) data.get("rate");
            boolean alert = samples != null && samples >= MIN_SAMPLE && ratio != null
                    && ratio.doubleValue() >= ALERT_RATIO;
            if (alert) hit++;
            inspectAfterPublish(goods);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scanned", goodsList.size());
        result.put("flagged", hit);
        return result;
    }

    public PageResult<PriceAlert> page(String status, Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 50);
        List<PriceAlert> list = priceAlertMapper.findPage(status, p[1], p[2]);
        int total = priceAlertMapper.countPage(status);
        return PageResult.of(list, total, p[0], p[1]);
    }

    public String handle(Integer id, String status, String note, Integer adminId) {
        PriceAlert alert = priceAlertMapper.findById(id);
        if (alert == null) {
            throw new IllegalArgumentException("预警记录不存在");
        }
        if (!"handled".equals(status) && !"dismissed".equals(status)) {
            throw new IllegalArgumentException("非法处理状态");
        }
        priceAlertMapper.handle(id, status, note, adminId);
        if ("dismissed".equals(status)) {
            // 误报：撤销商品上的买家提示
            priceAlertMapper.updateGoodsFlag(alert.getGoodsId(), 0, null);
        }
        return "handled".equals(status) ? "已标记处理" : "已忽略该预警";
    }

    public Map<String, Object> stats() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("open", priceAlertMapper.countOpen());
        data.put("total", priceAlertMapper.countAll());
        data.put("alertGoods", priceAlertMapper.countAlertGoods());
        return data;
    }
}
