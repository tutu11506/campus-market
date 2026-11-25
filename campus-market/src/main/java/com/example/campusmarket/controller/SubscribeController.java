package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.GoodsWatch;
import com.example.campusmarket.entity.KeywordSubscription;
import com.example.campusmarket.entity.NotifyRecord;
import com.example.campusmarket.entity.TextbookWish;
import com.example.campusmarket.service.SubscribeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 消息提醒、个性化订阅：关键词订阅 / 心愿降价 / 教材上新 / 学期提醒。
 * 所有接口均以 token 中的用户为准，不接受前端传入 userId。
 */
@RestController
@RequestMapping("/subscribe")
public class SubscribeController {

    @Autowired
    private SubscribeService subscribeService;

    /* ==================== 关键词订阅 ==================== */

    @GetMapping("/keywords")
    public Result<List<KeywordSubscription>> keywords() {
        Integer uid = SecurityUtils.getCurrentUserId();
        return Result.success(subscribeService.listKeywords(uid));
    }

    @PostMapping("/keywords")
    public Result<String> addKeyword(@RequestBody Map<String, String> body) {
        Integer uid = SecurityUtils.getCurrentUserId();
        return Result.success(subscribeService.addKeyword(uid, body == null ? null : body.get("keyword")));
    }

    @DeleteMapping("/keywords/{id}")
    public Result<String> deleteKeyword(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        return Result.success(subscribeService.deleteKeyword(uid, id));
    }

    /* ==================== 心愿商品（收藏降价提醒） ==================== */

    @GetMapping("/watch/goods")
    public Result<List<GoodsWatch>> watches() {
        Integer uid = SecurityUtils.getCurrentUserId();
        return Result.success(subscribeService.listWatches(uid));
    }

    /* ==================== 教材心愿（上新提醒） ==================== */

    @GetMapping("/wish/textbooks")
    public Result<List<TextbookWish>> wishes() {
        Integer uid = SecurityUtils.getCurrentUserId();
        return Result.success(subscribeService.listWishes(uid));
    }

    @PostMapping("/wish/textbooks")
    public Result<String> addWish(@RequestBody Map<String, Object> body) {
        Integer uid = SecurityUtils.getCurrentUserId();
        Object v = body == null ? null : body.get("textbookId");
        Integer textbookId = v instanceof Number ? ((Number) v).intValue() : null;
        return Result.success(subscribeService.addWish(uid, textbookId));
    }

    @DeleteMapping("/wish/textbooks/{id}")
    public Result<String> deleteWish(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        return Result.success(subscribeService.deleteWish(uid, id));
    }

    @GetMapping("/wish/check")
    public Result<Boolean> checkWish(@RequestParam Integer textbookId) {
        Integer uid = SecurityUtils.getCurrentUserId();
        return Result.success(subscribeService.hasWish(uid, textbookId));
    }

    /* ==================== 提醒记录 ==================== */

    @GetMapping("/notifications")
    public Result<PageResult<NotifyRecord>> notifications(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer uid = SecurityUtils.getCurrentUserId();
        return Result.success(subscribeService.pageNotifications(uid, pageNum, pageSize));
    }

    @GetMapping("/notifications/unread")
    public Result<Integer> unread() {
        return Result.success(subscribeService.unreadCount(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/notifications/{id}/read")
    public Result<String> markRead(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        return Result.success(subscribeService.markRead(uid, id));
    }

    @PostMapping("/notifications/read-all")
    public Result<String> markAllRead() {
        Integer uid = SecurityUtils.getCurrentUserId();
        return Result.success(subscribeService.markAllRead(uid));
    }

    @DeleteMapping("/notifications/{id}")
    public Result<String> deleteNotification(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        return Result.success(subscribeService.deleteNotification(uid, id));
    }

    /* ==================== 概览与学期提醒 ==================== */

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        Integer uid = SecurityUtils.getCurrentUserId();
        return Result.success(subscribeService.summary(uid));
    }

    /** 用户主动检查：仅在本人的开学季/毕业季窗口推送一次 */
    @PostMapping("/semester/check")
    public Result<String> checkSemester() {
        Integer uid = SecurityUtils.getCurrentUserId();
        return Result.success(subscribeService.checkForCurrentUser(uid));
    }

    /** 管理员手动演练：忽略日期，为全平台用户发送开学/毕业提醒（唯一键去重） */
    @PostMapping("/semester/scan")
    public Result<Map<String, Object>> scanSemester() {
        return Result.success(subscribeService.runSemesterScan(true));
    }
}
