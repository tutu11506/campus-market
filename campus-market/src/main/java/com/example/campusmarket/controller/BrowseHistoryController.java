package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.entity.Goods;
import com.example.campusmarket.service.BrowseHistoryService;
import com.example.campusmarket.vo.BrowsedGoodsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 浏览历史接口（均需登录，未在 SecurityConfig 放行名单内）。
 */
@RestController
@RequestMapping("/browse")
public class BrowseHistoryController {

    @Autowired
    private BrowseHistoryService browseHistoryService;

    /** 记录当前用户浏览了某商品 */
    @PostMapping("/record")
    public Result<Void> record(@RequestParam Integer goodsId) {
        String err = browseHistoryService.record(goodsId);
        return err != null ? Result.error(err) : Result.success();
    }

    /** 当前用户在指定卖家店铺浏览过的在售商品（聊天“发送商品”选择器用） */
    @GetMapping("/shop/{sellerId}")
    public Result<List<Goods>> shopBrowsed(@PathVariable Integer sellerId) {
        return Result.success(browseHistoryService.shopBrowsedGoods(sellerId));
    }

    /** 我的浏览历史（分页） */
    @GetMapping("/my")
    public Result<PageResult<BrowsedGoodsVO>> my(@RequestParam(required = false) Integer pageNum,
                                                  @RequestParam(required = false) Integer pageSize) {
        return Result.success(browseHistoryService.myHistory(pageNum, pageSize));
    }
}
