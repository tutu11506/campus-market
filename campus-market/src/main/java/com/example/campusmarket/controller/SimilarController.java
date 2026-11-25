package com.example.campusmarket.controller;

import com.example.campusmarket.common.Result;
import com.example.campusmarket.entity.Goods;
import com.example.campusmarket.mapper.GoodsMapper;
import com.example.campusmarket.service.CampusFeatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.*;

/**
 * 校园同款比价 —— 买家在商品详情页可以看到"校园内同款最低价"。
 *
 * 竞争壁垒：
 *   闲鱼/转转/拼多多给的是全网同款，而校园场景内"校内同款"是真正有价值的 ——
 *   你不想和千里之外的人比，你想知道"学校里有没有人卖得更便宜"。
 */
@RestController
@RequestMapping("/goods")
public class SimilarController {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private CampusFeatureService campusFeatureService;

    /**
     * 同款比价接口。
     * 策略：同分类 + 价格区间 [price*0.5, price*1.5] + 排除自己和卖家自己。
     * 排序按价格升序 —— 最低价排第一，告诉买家"你可以用更低价买到"。
     */
    @GetMapping("/similar")
    public Result<Map<String, Object>> similar(@RequestParam Integer goodsId) {
        Goods target = goodsMapper.findById(goodsId);
        if (target == null) {
            return Result.success(Map.of("hasSimilar", false, "message", "商品不存在"));
        }
        if (target.getCategoryId() == null || target.getPrice() == null) {
            return Result.success(Map.of("hasSimilar", false, "message", "信息不足"));
        }

        BigDecimal price = target.getPrice();
        BigDecimal min = price.multiply(new BigDecimal("0.5")).max(BigDecimal.ZERO);
        BigDecimal max = price.multiply(new BigDecimal("1.5"));

        List<Map<String, Object>> items = goodsMapper.findCampusSimilar(
                target.getCategoryId(), goodsId, target.getSellerId(), min, max);

        // enrich 卖家年级标签
        for (Map<String, Object> item : items) {
            String sno = String.valueOf(item.getOrDefault("sellerStudentNo", ""));
            item.put("isGraduationSeller", com.example.campusmarket.utils.GradeUtil.isGraduationSale(sno));
        }

        // 统计最低价便宜了多少
        String priceTip = "";
        if (!items.isEmpty()) {
            BigDecimal lowest = (BigDecimal) items.get(0).get("price");
            double save = price.subtract(lowest).doubleValue();
            double savePct = save / price.doubleValue() * 100;
            if (savePct >= 5) {
                priceTip = String.format("校园同款最低 ¥%.2f，可省 ¥%.2f (%.0f%%)", lowest, save, savePct);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasSimilar", !items.isEmpty());
        result.put("count", items.size());
        result.put("items", items);
        result.put("priceTip", priceTip);
        result.put("targetPrice", price);
        result.put("categoryName", target.getCategoryName());
        return Result.success(result);
    }
}
