package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.PlatformBlacklist;
import com.example.campusmarket.service.PlatformBlacklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 平台共享黑名单：公开风险查询 + 管理端标记/解除。
 */
@RestController
@RequestMapping("/risk")
public class BlacklistController {

    @Autowired
    private PlatformBlacklistService blacklistService;

    /** 全平台风险提醒（商品详情、聊天页等可直接调用） */
    @GetMapping("/blacklist/check")
    public Result<Map<String, Object>> check(@RequestParam Integer userId) {
        return Result.success(blacklistService.check(userId));
    }

    @GetMapping("/admin/blacklist/list")
    public Result<PageResult<PlatformBlacklist>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(blacklistService.page(status, pageNum, pageSize));
    }

    @PostMapping("/admin/blacklist/mark")
    public Result<String> mark(@RequestBody Map<String, Object> body) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        Integer userId = body.get("userId") == null ? null : Integer.valueOf(String.valueOf(body.get("userId")));
        if (userId == null) return Result.error("请选择要标记的用户");
        String level = (String) body.getOrDefault("level", "warning");
        String reason = (String) body.get("reason");
        String evidence = (String) body.get("evidence");
        return Result.success(blacklistService.mark(uid, userId, level, reason, evidence));
    }

    @PutMapping("/admin/blacklist/revoke")
    public Result<String> revoke(@RequestParam Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        return Result.success(blacklistService.revoke(uid, id));
    }

    @GetMapping("/admin/blacklist/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(blacklistService.stats());
    }
}
