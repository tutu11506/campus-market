package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.Favorite;
import com.example.campusmarket.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    // 收藏/取消收藏（userId 以 token 为准，忽略前端传入）
    @PostMapping("/toggle")
    public Result<String> toggle(@RequestBody Favorite favorite) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        favorite.setUserId(uid);
        return Result.success(favoriteService.toggle(favorite));
    }

    // 查询我的收藏列表（忽略路径 userId，以 token 为准）
    @GetMapping("/my/{userId}")
    public Result<PageResult<Favorite>> findMyFavorites(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        return Result.success(favoriteService.findMyFavorites(uid, pageNum, pageSize));
    }

    // 判断是否已收藏（userId 以 token 为准，忽略前端传入）
    @GetMapping("/check")
    public Result<Boolean> isFavorited(
            @RequestParam Integer userId,
            @RequestParam Integer goodsId) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        return Result.success(favoriteService.isFavorited(uid, goodsId));
    }
}
