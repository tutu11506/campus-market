package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.common.ForbiddenException;
import com.example.campusmarket.entity.Order;
import com.example.campusmarket.entity.Refund;
import com.example.campusmarket.service.OrderService;
import com.example.campusmarket.service.RefundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 退货申请接口。/refund/** 走 SecurityConfig 的 anyRequest().authenticated，需登录。
 * 申请仅买家、审核仅卖家，权限在 Service 内二次校验。
 */
@RestController
@RequestMapping("/refund")
public class RefundController {

    @Autowired
    private RefundService refundService;

    @Autowired
    private OrderService orderService;

    // 买家申请退货
    @PostMapping("/apply")
    public Result<String> apply(@RequestBody Refund refund) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        return Result.success(refundService.apply(refund, uid));
    }

    // 某订单的退货申请列表（买家或卖家可查看）
    @GetMapping("/order/{orderId}")
    public Result<List<Refund>> findByOrderId(@PathVariable Integer orderId) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        Order order = orderService.findById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!uid.equals(order.getBuyerId()) && !uid.equals(order.getSellerId())) {
            throw new ForbiddenException("无权查看");
        }
        return Result.success(refundService.findByOrderId(orderId));
    }

    // 卖家的退货审核列表（分页）
    @GetMapping("/seller")
    public Result<PageResult<Refund>> findSellerRefunds(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        return Result.success(refundService.findSellerRefunds(uid, pageNum, pageSize));
    }

    // 卖家同意退货
    @PutMapping("/approve/{id}")
    public Result<String> approve(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        return Result.success(refundService.approve(id, uid));
    }

    // 卖家拒绝退货（body: { rejectReason }）
    @PutMapping("/reject/{id}")
    public Result<String> reject(@PathVariable Integer id, @RequestBody(required = false) Map<String, String> body) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        String rejectReason = body == null ? null : body.get("rejectReason");
        return Result.success(refundService.reject(id, uid, rejectReason));
    }
}
