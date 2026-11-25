package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.Goods;
import com.example.campusmarket.entity.PriceAlert;
import com.example.campusmarket.service.GoodsService;
import com.example.campusmarket.service.PriceGuardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 价格监控预警：买家侧价格洞察（公开）+ 管理端预警处理与巡检。
 */
@RestController
@RequestMapping("/risk")
public class PriceGuardController {

    @Autowired
    private PriceGuardService priceGuardService;
    @Autowired
    private GoodsService goodsService;

    /** 买家侧：该商品是否高于近期同类均价 */
    @GetMapping("/price/insight")
    public Result<Map<String, Object>> insight(@RequestParam Integer goodsId) {
        Goods goods = goodsService.getById(goodsId);
        if (goods == null) {
            return Result.error("商品不存在");
        }
        return Result.success(priceGuardService.insight(goods));
    }

    @GetMapping("/admin/price/list")
    public Result<PageResult<PriceAlert>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(priceGuardService.page(status, pageNum, pageSize));
    }

    @PutMapping("/admin/price/handle")
    public Result<String> handle(@RequestParam Integer id,
                                 @RequestParam(defaultValue = "handled") String status,
                                 @RequestParam(required = false) String note) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        return Result.success(priceGuardService.handle(id, status, note, uid));
    }

    /** 一键巡检全部在售商品（演示自动巡检） */
    @PostMapping("/admin/price/rescan")
    public Result<Map<String, Object>> rescan() {
        return Result.success(priceGuardService.rescanAll());
    }

    @GetMapping("/admin/price/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(priceGuardService.stats());
    }
}
