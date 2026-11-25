package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.*;
import com.example.campusmarket.service.PointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/points")
public class PointController {

    @Autowired PointService pointService;

    /* ==================== 我的（需登录） ==================== */

    @GetMapping("/account")
    public Result<PointAccount> myAccount() {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("请先登录");
        return Result.success(pointService.myAccount(uid));
    }

    @GetMapping("/records")
    public Result<PageResult<PointRecord>> myRecords(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("请先登录");
        return Result.success(pointService.myRecords(uid, pageNum, pageSize));
    }

    @GetMapping("/exchanges")
    public Result<PageResult<PointExchange>> myExchanges(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("请先登录");
        return Result.success(pointService.myExchanges(uid, pageNum, pageSize));
    }

    @GetMapping("/badges")
    public Result<List<EcoBadge>> myBadges() {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("请先登录");
        return Result.success(pointService.myBadges(uid));
    }

    @PostMapping("/badges/check")
    public Result<Integer> checkBadges() {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("请先登录");
        return Result.success(pointService.checkBadges(uid));
    }

    /* ==================== 积分商城（浏览公开） ==================== */

    @GetMapping("/rewards")
    public Result<List<PointReward>> listRewards() {
        return Result.success(pointService.listRewards());
    }

    /** 兑换（需登录） */
    @PostMapping("/exchange")
    public Result<PointExchange> exchange(@RequestBody Map<String, Integer> body) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("请先登录");
        Integer rewardId = body.get("rewardId");
        if (rewardId == null) return Result.error("请选择兑换商品");
        return Result.success(pointService.exchange(uid, rewardId));
    }

    /* ==================== 排行榜（公开） ==================== */

    @GetMapping("/leaderboard")
    public Result<Map<String, Object>> leaderboard(@RequestParam(defaultValue = "user_balance") String scope) {
        return Result.success(pointService.leaderboard(scope));
    }

    /* ==================== 全部勋章定义（公开） ==================== */

    @GetMapping("/badges/all")
    public Result<List<EcoBadge>> allBadges() {
        return Result.success(pointService.allBadges());
    }
}
