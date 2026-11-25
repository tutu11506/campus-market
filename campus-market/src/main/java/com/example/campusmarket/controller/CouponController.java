package com.example.campusmarket.controller;

import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.UserCoupon;
import com.example.campusmarket.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台满减券礼包：首次进首页弹窗展示 5 张不同档位的券，一键领取后下单自动抵扣。
 */
@RestController
@RequestMapping("/coupon")
public class CouponController {

    @Autowired
    private CouponService couponService;

    /** 待领取的券礼包（没有任何可用券时自动补发一整套） */
    @GetMapping("/pending")
    public Result<List<UserCoupon>> pending() {
        Integer uid = SecurityUtils.getCurrentUserId();
        return Result.success(couponService.listPending(uid));
    }

    /** 一键领取 */
    @PostMapping("/claim-all")
    public Result<Map<String, Object>> claimAll() {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        int claimed = couponService.claimAll(uid);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("claimed", claimed);
        return claimed > 0 ? Result.success(data) : Result.error("没有可领取的优惠券");
    }

    /** 我的可用券 */
    @GetMapping("/mine")
    public Result<List<UserCoupon>> mine() {
        Integer uid = SecurityUtils.getCurrentUserId();
        return Result.success(couponService.listUnused(uid));
    }

    /** 当前金额可用哪张券 */
    @GetMapping("/applicable")
    public Result<UserCoupon> applicable(@RequestParam BigDecimal amount) {
        Integer uid = SecurityUtils.getCurrentUserId();
        return Result.success(couponService.findApplicable(uid, amount));
    }
}
