package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.common.ForbiddenException;
import com.example.campusmarket.entity.Goods;
import com.example.campusmarket.service.ContentModerationService;
import com.example.campusmarket.service.GoodsService;
import com.example.campusmarket.service.PriceGuardService;
import com.example.campusmarket.service.RiskService;
import com.example.campusmarket.service.SubscribeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private RiskService riskService;

    @Autowired
    private ContentModerationService moderationService;

    @Autowired
    private PriceGuardService priceGuardService;

    @Autowired
    private SubscribeService subscribeService;

    // 发布商品（sellerId 以 token 为准，忽略前端传入）
    @PostMapping("/add")
    public Result<String> add(@Valid @RequestBody Goods goods) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        riskService.assertNoSensitive(goods.getTitle(), goods.getDescription());
        if (riskService.isMuted(uid)) {
            return Result.error("信用分过低（<60），已被禁止发布，请联系管理员");
        }
        // 违禁品自动识别：命中 block 直接拦截，review 转人工复核
        ContentModerationService.ScanResult scan = moderationService.scan(
                uid, goods.getTitle(), goods.getDescription(), goods.getCoverImage(), goods.getImages());
        moderationService.assertPublishable(scan);
        goods.setRiskLevel(scan.getLevel());
        goods.setRiskReason(scan.getReason());

        goods.setSellerId(uid);
        String msg = goodsService.add(goods);
        // 关联识别记录 + 发布后价格巡检（高于近期同类均价自动打标）
        moderationService.attachGoods(scan.getRecordId(), goods.getId());
        priceGuardService.inspectAfterPublish(goods);
        // 上新提醒：命中关键词订阅 / 教材心愿立即推送站内提醒
        subscribeService.onGoodsPublished(goods);
        return Result.success(msg);
    }

    // 一键复制商品：把自己的商品复制成 N 件在售商品，适合同款多件快速上新
    @PostMapping("/copy")
    public Result<Map<String, Object>> copy(@RequestBody Map<String, Object> body) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        if (riskService.isMuted(uid)) {
            return Result.error("信用分过低（<60），已被禁止发布，请联系管理员");
        }
        Integer id = toInt(body.get("id"));
        if (id == null) {
            return Result.error("缺少商品ID");
        }
        Integer count = toInt(body.get("count"));
        List<Integer> ids = goodsService.copy(uid, id, count);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("count", ids.size());
        data.put("ids", ids);
        data.put("message", "已复制 " + ids.size() + " 件商品，可在「我的发布」中修改数量或信息");
        return Result.success(data);
    }

    // 商品详情（公开）
    @GetMapping("/{id}")
    public Result<Goods> findById(@PathVariable Integer id) {
        return Result.success(goodsService.findById(id));
    }

    // 商品列表（分页+筛选+搜索+价格区间+排序+按卖家过滤）（公开）
    @GetMapping("/list")
    public Result<PageResult<Goods>> findList(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer sellerId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer canMail,
            @RequestParam(required = false) Integer canDeliver,
            @RequestParam(required = false) Integer exchangeSupported,
            @RequestParam(required = false, defaultValue = "new") String sort,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(goodsService.findList(categoryId, keyword, minPrice, maxPrice, sellerId, sort, pageNum, pageSize, location,
                canMail, canDeliver, exchangeSupported));
    }

    // 我发布的商品（忽略路径 sellerId，以 token 为准）
    @GetMapping("/my/{sellerId}")
    public Result<PageResult<Goods>> findMyGoods(
            @PathVariable Integer sellerId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        return Result.success(goodsService.findMyGoods(uid, pageNum, pageSize));
    }

    // 修改商品（仅卖家本人）
    @PutMapping("/update")
    public Result<String> update(@Valid @RequestBody Goods goods) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        Goods exist = goodsService.getById(goods.getId());
        if (exist == null) {
            return Result.error("商品不存在");
        }
        if (!uid.equals(exist.getSellerId())) {
            throw new ForbiddenException("无权修改此商品");
        }
        riskService.assertNoSensitive(goods.getTitle(), goods.getDescription());
        // 修改同样过一遍违禁品识别，防止先合规发布再改成违规内容
        ContentModerationService.ScanResult scan = moderationService.scan(
                uid, goods.getTitle(), goods.getDescription(), goods.getCoverImage(), goods.getImages());
        moderationService.assertPublishable(scan);
        goods.setRiskLevel(scan.getLevel());
        goods.setRiskReason(scan.getReason());
        goods.setSellerId(uid); // 防止篡改归属
        String msg = goodsService.update(goods);
        priceGuardService.inspectAfterPublish(goods);
        // 降价提醒：对比改价前后，给收藏用户推送降价消息
        subscribeService.onGoodsPriceChanged(exist, goods);
        return Result.success(msg);
    }

    // 修改商品状态（上下架，仅卖家本人）
    @PutMapping("/status")
    public Result<String> updateStatus(@RequestParam Integer id, @RequestParam String status) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        Goods exist = goodsService.getById(id);
        if (exist == null) {
            return Result.error("商品不存在");
        }
        if (!uid.equals(exist.getSellerId())) {
            throw new ForbiddenException("无权操作此商品");
        }
        return Result.success(goodsService.updateStatus(id, status));
    }

    // 删除商品（仅卖家本人）
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        Goods exist = goodsService.getById(id);
        if (exist == null) {
            return Result.error("商品不存在");
        }
        if (!uid.equals(exist.getSellerId())) {
            throw new ForbiddenException("无权删除此商品");
        }
        return Result.success(goodsService.delete(id));
    }

    private Integer toInt(Object v) {
        if (v == null) return null;
        try {
            return Integer.valueOf(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
