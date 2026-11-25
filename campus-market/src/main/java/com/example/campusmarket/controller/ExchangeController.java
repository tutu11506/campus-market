package com.example.campusmarket.controller;

import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.GoodsExchange;
import com.example.campusmarket.service.ExchangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/exchange")
public class ExchangeController {

    @Autowired
    private ExchangeService exchangeService;

    /** 发起换物申请 */
    @PostMapping("/apply")
    public Result<String> apply(@RequestBody Map<String, Object> body) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        Integer goodsId = intOf(body.get("goodsId"));
        Integer offerGoodsId = intOf(body.get("offerGoodsId"));
        Object msg = body.get("message");
        return Result.success(exchangeService.apply(uid, goodsId, offerGoodsId,
                msg == null ? null : String.valueOf(msg)));
    }

    /** 我收到的换物申请 */
    @GetMapping("/received")
    public Result<List<GoodsExchange>> received() {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(exchangeService.received(uid));
    }

    /** 我发起的换物申请 */
    @GetMapping("/sent")
    public Result<List<GoodsExchange>> sent() {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(exchangeService.sent(uid));
    }

    @PostMapping("/{id}/accept")
    public Result<String> accept(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(exchangeService.accept(uid, id));
    }

    @PostMapping("/{id}/reject")
    public Result<String> reject(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(exchangeService.reject(uid, id));
    }

    @PostMapping("/{id}/cancel")
    public Result<String> cancel(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(exchangeService.cancel(uid, id));
    }

    @PostMapping("/{id}/finish")
    public Result<String> finish(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(exchangeService.finish(uid, id));
    }

    private Integer intOf(Object v) {
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        if (v instanceof String && !((String) v).isBlank()) {
            try {
                return Integer.valueOf(((String) v).trim());
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        return null;
    }
}
