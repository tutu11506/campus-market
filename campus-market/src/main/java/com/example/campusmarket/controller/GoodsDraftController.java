package com.example.campusmarket.controller;

import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.GoodsDraft;
import com.example.campusmarket.service.GoodsDraftService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商品草稿箱：发布中断自动保存，支持多份草稿继续编辑。
 */
@RestController
@RequestMapping("/goods-draft")
public class GoodsDraftController {

    @Autowired
    private GoodsDraftService draftService;

    @GetMapping
    public Result<List<GoodsDraft>> list() {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        return Result.success(draftService.list(uid));
    }

    @GetMapping("/{id}")
    public Result<GoodsDraft> get(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        return Result.success(draftService.get(uid, id));
    }

    @PostMapping
    public Result<Map<String, Object>> save(@RequestBody GoodsDraft draft) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        return Result.success(draftService.save(uid, draft));
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        return Result.success(draftService.delete(uid, id));
    }
}
