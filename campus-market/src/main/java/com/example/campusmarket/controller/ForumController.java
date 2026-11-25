package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.ForumComment;
import com.example.campusmarket.entity.ForumNotification;
import com.example.campusmarket.entity.ForumPost;
import com.example.campusmarket.service.ForumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 校园论坛：失物招领、学习交流、校园闲聊。
 */
@RestController
@RequestMapping("/forum")
public class ForumController {

    @Autowired
    private ForumService forumService;

    /** 帖子列表（所有人可看） */
    @GetMapping("/posts")
    public Result<PageResult<ForumPost>> posts(@RequestParam(required = false) String board,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(forumService.list(board, keyword, pageNum, pageSize,
                SecurityUtils.getCurrentUserId()));
    }

    /** 帖子详情（浏览量 +1） */
    @GetMapping("/posts/{id}")
    public Result<ForumPost> detail(@PathVariable Integer id) {
        ForumPost post = forumService.detail(id, SecurityUtils.getCurrentUserId());
        return post == null ? Result.error("帖子不存在或已删除") : Result.success(post);
    }

    /** 发帖 */
    @PostMapping("/posts")
    public Result<ForumPost> create(@RequestBody ForumPost post) {
        Integer uid = SecurityUtils.getCurrentUserId();
        String msg = forumService.create(post, uid);
        return "发布成功".equals(msg) ? Result.success(post) : Result.error(msg);
    }

    /** 删除自己的帖子（管理员可删任意） */
    @DeleteMapping("/posts/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        String msg = forumService.deletePost(id, SecurityUtils.getCurrentUserId());
        return "删除成功".equals(msg) ? Result.success(msg) : Result.error(msg);
    }

    /** 点赞 / 取消点赞 */
    @PostMapping("/posts/{id}/like")
    public Result<Map<String, Object>> like(@PathVariable Integer id) {
        return Result.success(forumService.toggleLike(id, SecurityUtils.getCurrentUserId()));
    }

    /** 回复列表 */
    @GetMapping("/posts/{id}/comments")
    public Result<List<ForumComment>> comments(@PathVariable Integer id) {
        return Result.success(forumService.comments(id));
    }

    /** 发表回复（parentId 非空表示回复某条回复） */
    @PostMapping("/posts/{id}/comments")
    public Result<String> addComment(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        String content = body.get("content") == null ? null : String.valueOf(body.get("content"));
        Integer parentId = null;
        Object p = body.get("parentId");
        if (p instanceof Number) {
            parentId = ((Number) p).intValue();
        } else if (p instanceof String && !((String) p).isBlank()) {
            try {
                parentId = Integer.valueOf(((String) p).trim());
            } catch (NumberFormatException ignore) {
                parentId = null;
            }
        }
        String msg = forumService.addComment(id, content, parentId, SecurityUtils.getCurrentUserId());
        return "回复成功".equals(msg) ? Result.success(msg) : Result.error(msg);
    }

    /** 我发布的帖子 */
    @GetMapping("/my/posts")
    public Result<PageResult<ForumPost>> myPosts(@RequestParam(defaultValue = "1") int pageNum,
                                                 @RequestParam(defaultValue = "10") int pageSize) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("请先登录");
        return Result.success(forumService.myPosts(uid, pageNum, pageSize));
    }

    /** 我的论坛通知 */
    @GetMapping("/notifications")
    public Result<PageResult<ForumNotification>> notifications(@RequestParam(defaultValue = "1") int pageNum,
                                                               @RequestParam(defaultValue = "10") int pageSize) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("请先登录");
        return Result.success(forumService.notifications(uid, pageNum, pageSize));
    }

    /** 未读通知数量 */
    @GetMapping("/notifications/unread")
    public Result<Integer> unreadNotifications() {
        return Result.success(forumService.unreadNotificationCount(SecurityUtils.getCurrentUserId()));
    }

    /** 全部标记为已读 */
    @PostMapping("/notifications/read")
    public Result<String> readNotifications() {
        forumService.markNotificationsRead(SecurityUtils.getCurrentUserId());
        return Result.success("已读");
    }

    /** 删除自己的回复 */
    @DeleteMapping("/comments/{id}")
    public Result<String> deleteComment(@PathVariable Integer id) {
        String msg = forumService.deleteComment(id, SecurityUtils.getCurrentUserId());
        return "删除成功".equals(msg) ? Result.success(msg) : Result.error(msg);
    }
}
