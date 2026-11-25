package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.ImageModeration;
import com.example.campusmarket.entity.ProhibitedItem;
import com.example.campusmarket.service.ContentModerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 违禁品 AI 识别：识别记录查询、人工复核、词库维护。
 * 发布拦截在 /goods/add 内自动完成，管理端只处理复核队列。
 */
@RestController
@RequestMapping("/risk")
public class ModerationController {

    @Autowired
    private ContentModerationService moderationService;

    @GetMapping("/admin/moderation/list")
    public Result<PageResult<ImageModeration>> list(
            @RequestParam(required = false) String verdict,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(moderationService.page(verdict, pageNum, pageSize));
    }

    @PutMapping("/admin/moderation/confirm")
    public Result<String> confirm(@RequestParam Integer id,
                                  @RequestParam(defaultValue = "confirm") String action) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        return Result.success(moderationService.confirm(id, action, uid));
    }

    @GetMapping("/admin/moderation/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(moderationService.stats());
    }

    @GetMapping("/admin/moderation/items")
    public Result<List<ProhibitedItem>> items() {
        return Result.success(moderationService.allItems());
    }

    @PostMapping("/admin/moderation/refresh")
    public Result<String> refresh() {
        moderationService.refresh();
        return Result.success("违禁品词库缓存已刷新");
    }
}
