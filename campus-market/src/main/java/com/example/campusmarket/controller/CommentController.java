package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.Comment;
import com.example.campusmarket.service.CommentService;
import com.example.campusmarket.service.RiskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private RiskService riskService;

    // 发布评价（fromUserId 以 token 为准，忽略前端传入）
    @PostMapping("/add")
    public Result<String> add(@RequestBody Comment comment) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        riskService.assertNoSensitive(comment.getContent());
        comment.setFromUserId(uid);
        String msg = commentService.add(comment);
        if ("评价成功".equals(msg)) {
            return Result.success(msg);
        }
        return Result.error(msg);
    }

    // 查询商品的评价列表（公开，分页）
    @GetMapping("/goods/{goodsId}")
    public Result<PageResult<Comment>> findByGoodsId(
            @PathVariable Integer goodsId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(commentService.findByGoodsId(goodsId, pageNum, pageSize));
    }

    // 查询某个卖家收到的评价（公开，分页）
    @GetMapping("/seller/{sellerId}")
    public Result<PageResult<Comment>> findBySeller(
            @PathVariable Integer sellerId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(commentService.findMyComments(sellerId, pageNum, pageSize));
    }

    // 查询我收到的评价（忽略路径 userId，以 token 为准）
    @GetMapping("/my/{userId}")
    public Result<PageResult<Comment>> findMyComments(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        return Result.success(commentService.findMyComments(uid, pageNum, pageSize));
    }
}
