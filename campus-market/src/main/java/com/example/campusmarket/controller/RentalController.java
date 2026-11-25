package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.RentalItem;
import com.example.campusmarket.entity.RentalOrder;
import com.example.campusmarket.service.RentalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rental")
public class RentalController {

    @Autowired
    private RentalService rentalService;

    /** 租赁物品列表（公开） */
    @GetMapping("/items")
    public Result<PageResult<RentalItem>> items(@RequestParam(required = false) String type,
                                                @RequestParam(required = false) String category,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "12") Integer pageSize) {
        return Result.success(rentalService.list(type, category, keyword, pageNum, pageSize));
    }

    /** 租赁物品详情（公开） */
    @GetMapping("/items/{id}")
    public Result<RentalItem> detail(@PathVariable Integer id) {
        return Result.success(rentalService.detail(id));
    }

    /** 发布出租（登录） */
    @PostMapping("/items")
    public Result<RentalItem> publish(@RequestBody RentalItem form) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(rentalService.publish(uid, form));
    }

    /** 下单租用（登录） */
    @PostMapping("/orders")
    public Result<RentalOrder> createOrder(@RequestBody RentalOrder form) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(rentalService.createOrder(uid, form));
    }

    /** 我的租赁：role=renter 我租的 / lender 我出租的（登录） */
    @GetMapping("/orders/my")
    public Result<PageResult<RentalOrder>> myOrders(@RequestParam(defaultValue = "renter") String role,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(rentalService.myOrders(uid, role, status, pageNum, pageSize));
    }

    /** 出租方确认出租 */
    @PostMapping("/orders/{id}/confirm")
    public Result<RentalOrder> confirm(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(rentalService.confirm(uid, id));
    }

    /** 租客发起归还 */
    @PostMapping("/orders/{id}/return")
    public Result<RentalOrder> applyReturn(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(rentalService.applyReturn(uid, id));
    }

    /** 出租方确认收回（核销） */
    @PostMapping("/orders/{id}/complete")
    public Result<RentalOrder> complete(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(rentalService.complete(uid, id));
    }

    /** 取消待确认的订单 */
    @PostMapping("/orders/{id}/cancel")
    public Result<String> cancel(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(rentalService.cancel(uid, id));
    }
}
