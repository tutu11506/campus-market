package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.JuryReview;
import com.example.campusmarket.service.JuryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 评审团：买卖双方互评，其他用户投票判断真实性，支持率超过 60% 才展示。
 */
@RestController
@RequestMapping("/jury")
public class JuryController {

    @Autowired
    private JuryService juryService;

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        return Result.success(juryService.summary(uid));
    }

    @GetMapping("/reviews")
    public Result<PageResult<JuryReview>> reviews(@RequestParam(required = false) String filter,
                                                  @RequestParam(defaultValue = "1") Integer pageNum,
                                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        return Result.success(juryService.page(uid, filter, pageNum, pageSize));
    }

    @GetMapping("/orders")
    public Result<List<Map<String, Object>>> orders() {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        return Result.success(juryService.reviewableOrders(uid));
    }

    @PostMapping("/reviews")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        Integer orderId = toInt(body.get("orderId"));
        Integer rating = toInt(body.get("rating"));
        String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason"));
        List<String> evidence = null;
        Object ev = body.get("evidence");
        if (ev instanceof List) {
            evidence = ((List<?>) ev).stream()
                    .filter(java.util.Objects::nonNull)
                    .map(String::valueOf)
                    .toList();
        }
        return Result.success(juryService.create(uid, orderId, rating, reason, evidence));
    }

    @PostMapping("/reviews/{id}/vote")
    public Result<Map<String, Object>> vote(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        Integer vote = toInt(body.get("vote"));
        if (vote == null) return Result.error("请选择投票选项");
        return Result.success(juryService.vote(uid, id, vote));
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
