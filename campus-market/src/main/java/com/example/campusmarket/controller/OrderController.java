package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.common.ForbiddenException;
import com.example.campusmarket.entity.Order;
import com.example.campusmarket.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // 创建订单（下单）（buyerId 以 token 为准，忽略前端传入）
    @PostMapping("/create")
    public Result<String> createOrder(@RequestBody Order order) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        order.setBuyerId(uid);
        String msg = orderService.createOrder(order);
        if ("下单成功".equals(msg)) {
            return Result.success(msg);
        }
        return Result.error(msg);
    }

    // 订单详情（仅买家或卖家可查看）
    @GetMapping("/{id}")
    public Result<Order> findById(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        Order order = orderService.findById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!uid.equals(order.getBuyerId()) && !uid.equals(order.getSellerId())) {
            throw new ForbiddenException("无权查看此订单");
        }
        // 核销码只给买家看，卖家需要买家当面出示后再输入核销
        if (!uid.equals(order.getBuyerId())) {
            order.setVerifyCode(null);
        }
        return Result.success(order);
    }

    // 卖家线下核销：输入买家出示的核销码，完成后托管资金结算给卖家
    @PostMapping("/verify/{id}")
    public Result<String> verifyOrder(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        String code = body == null ? null : body.get("code");
        return Result.success(orderService.verifyOrder(id, uid, code));
    }

    // 我买到的订单（忽略路径 userId，以 token 为准）
    @GetMapping("/buyer/{userId}")
    public Result<PageResult<Order>> findBuyerOrders(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        return Result.success(orderService.findBuyerOrders(uid, pageNum, pageSize));
    }

    // 我卖出的订单（忽略路径 userId，以 token 为准）
    @GetMapping("/seller/{userId}")
    public Result<PageResult<Order>> findSellerOrders(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        PageResult<Order> result = orderService.findSellerOrders(uid, pageNum, pageSize);
        // 卖家列表不返回核销码，避免卖家自己核销
        if (result.getList() != null) {
            result.getList().forEach(o -> o.setVerifyCode(null));
        }
        return Result.success(result);
    }

    // 卖家确认订单（仅卖家可操作）
    @PutMapping("/confirm/{id}")
    public Result<String> confirmOrder(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        Order order = orderService.findById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!uid.equals(order.getSellerId())) {
            throw new ForbiddenException("无权操作此订单");
        }
        return Result.success(orderService.confirmOrder(id));
    }

    // 模拟支付（仅买家可操作）
    @PutMapping("/pay/{id}")
    public Result<String> payOrder(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        return Result.success(orderService.payOrder(id, uid));
    }

    // 确认收货 / 完成交易（仅买家可操作）
    @PutMapping("/complete/{id}")
    public Result<String> completeOrder(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        Order order = orderService.findById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!uid.equals(order.getBuyerId())) {
            throw new ForbiddenException("仅买家可确认收货");
        }
        return Result.success(orderService.completeOrder(id));
    }

    // 取消订单（买家或卖家可操作）
    @PutMapping("/cancel/{id}")
    public Result<String> cancelOrder(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        Order order = orderService.findById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!uid.equals(order.getBuyerId()) && !uid.equals(order.getSellerId())) {
            throw new ForbiddenException("无权操作此订单");
        }
        return Result.success(orderService.cancelOrder(id));
    }
}
