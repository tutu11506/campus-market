package com.example.campusmarket.service;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.ForumComment;
import com.example.campusmarket.entity.ForumNotification;
import com.example.campusmarket.entity.ForumPost;
import com.example.campusmarket.mapper.ForumMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 校园论坛：发帖、回复、点赞。
 * 内容只在校园内流转，发帖人和回复人可以随时删除自己的内容，管理员可清理任意内容。
 */
@Service
public class ForumService {

    private static final Set<String> BOARDS = Set.of("chat", "lost", "study", "team", "help");
    public static final int MAX_TITLE = 120;
    public static final int MAX_CONTENT = 5000;
    public static final int MAX_COMMENT = 1000;

    @Autowired
    private ForumMapper forumMapper;

    public PageResult<ForumPost> list(String board, String keyword, int pageNum, int pageSize, Integer uid) {
        int[] p = PageResult.normalize(pageNum, pageSize, 30);
        String b = (board != null && BOARDS.contains(board)) ? board : null;
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        List<ForumPost> list = forumMapper.findPosts(b, kw, uid, (p[0] - 1) * p[1], p[1]);
        return PageResult.of(list, forumMapper.countPosts(b, kw), p[0], p[1]);
    }

    public ForumPost detail(Integer id, Integer uid) {
        if (id == null) return null;
        forumMapper.addViewCount(id);
        return forumMapper.findById(id, uid);
    }

    public String create(ForumPost post, Integer uid) {
        if (uid == null) return "请先登录";
        if (post == null || post.getTitle() == null || post.getTitle().trim().length() < 2) {
            return "标题至少 2 个字";
        }
        if (post.getTitle().trim().length() > MAX_TITLE) {
            return "标题最多 " + MAX_TITLE + " 个字";
        }
        if (post.getContent() == null || post.getContent().trim().isEmpty()) {
            return "正文不能为空";
        }
        if (post.getContent().length() > MAX_CONTENT) {
            return "正文最多 " + MAX_CONTENT + " 字";
        }
        post.setTitle(post.getTitle().trim());
        post.setContent(post.getContent().trim());
        post.setBoard(post.getBoard() != null && BOARDS.contains(post.getBoard()) ? post.getBoard() : "chat");
        post.setUserId(uid);
        forumMapper.insertPost(post);
        return "发布成功";
    }

    public String deletePost(Integer id, Integer uid) {
        if (uid == null) return "请先登录";
        Integer owner = forumMapper.findOwner(id);
        if (owner == null) return "帖子不存在或已删除";
        if (!owner.equals(uid) && !SecurityUtils.isAdmin()) {
            return "只能删除自己发布的帖子";
        }
        forumMapper.deletePost(id);
        return "删除成功";
    }

    /** 点赞 / 取消点赞，返回最新状态 */
    public Map<String, Object> toggleLike(Integer postId, Integer uid) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (uid == null) {
            data.put("needLogin", true);
            return data;
        }
        boolean liked;
        if (forumMapper.countLike(postId, uid) > 0) {
            forumMapper.deleteLike(postId, uid);
            forumMapper.addLikeCount(postId, -1);
            liked = false;
        } else {
            forumMapper.insertLike(postId, uid);
            forumMapper.addLikeCount(postId, 1);
            liked = true;
        }
        ForumPost post = forumMapper.findById(postId, uid);
        data.put("liked", liked);
        data.put("likeCount", post == null ? 0 : post.getLikeCount());
        return data;
    }

    public List<ForumComment> comments(Integer postId) {
        return forumMapper.findComments(postId);
    }

    /**
     * 发表回复：parentId 非空表示“回复某条回复”。
     * 回复后通知楼主；如果是回复某条回复，再通知被回复的人（去重且不通知自己）。
     */
    public String addComment(Integer postId, String content, Integer parentId, Integer uid) {
        if (uid == null) return "请先登录";
        if (content == null || content.trim().isEmpty()) return "回复内容不能为空";
        if (content.length() > MAX_COMMENT) return "回复最多 " + MAX_COMMENT + " 字";
        ForumPost post = forumMapper.findPostBrief(postId);
        if (post == null) return "帖子不存在或已删除";

        ForumComment parent = null;
        if (parentId != null) {
            parent = forumMapper.findCommentById(parentId);
            if (parent == null || !postId.equals(parent.getPostId())) {
                return "回复的评论不存在";
            }
        }

        String text = content.trim();
        ForumComment comment = new ForumComment();
        comment.setPostId(postId);
        comment.setUserId(uid);
        comment.setContent(text);
        if (parent != null) {
            comment.setParentId(parent.getId());
            comment.setReplyToUserId(parent.getUserId());
        }
        forumMapper.insertComment(comment);
        forumMapper.addCommentCount(postId, 1);

        String summary = text.length() > 60 ? text.substring(0, 60) + "…" : text;
        // 通知楼主（自己回复自己的帖子不通知）
        if (!uid.equals(post.getUserId())) {
            forumMapper.insertNotification(buildNotification(post.getUserId(), "post_reply",
                    post, comment, uid, summary));
        }
        // 通知被回复的人（与楼主重复或就是自己时不重复发）
        if (parent != null && !uid.equals(parent.getUserId())
                && !parent.getUserId().equals(post.getUserId())) {
            forumMapper.insertNotification(buildNotification(parent.getUserId(), "comment_reply",
                    post, comment, uid, summary));
        }
        return "回复成功";
    }

    private ForumNotification buildNotification(Integer userId, String type, ForumPost post,
                                                ForumComment comment, Integer fromUserId, String summary) {
        ForumNotification n = new ForumNotification();
        n.setUserId(userId);
        n.setType(type);
        n.setPostId(post.getId());
        n.setCommentId(comment.getId());
        n.setFromUserId(fromUserId);
        n.setPostTitle(post.getTitle());
        n.setContent(summary);
        return n;
    }

    /** 我发布的帖子 */
    public PageResult<ForumPost> myPosts(Integer uid, int pageNum, int pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 30);
        List<ForumPost> list = forumMapper.findMyPosts(uid, p[2], p[1]);
        return PageResult.of(list, forumMapper.countMyPosts(uid), p[0], p[1]);
    }

    /** 我的论坛通知 */
    public PageResult<ForumNotification> notifications(Integer uid, int pageNum, int pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 30);
        List<ForumNotification> list = forumMapper.findNotifications(uid, p[2], p[1]);
        return PageResult.of(list, forumMapper.countNotifications(uid), p[0], p[1]);
    }

    public int unreadNotificationCount(Integer uid) {
        return uid == null ? 0 : forumMapper.countUnreadNotifications(uid);
    }

    public void markNotificationsRead(Integer uid) {
        if (uid != null) {
            forumMapper.markNotificationsRead(uid);
        }
    }

    public String deleteComment(Integer id, Integer uid) {
        if (uid == null) return "请先登录";
        Integer owner = forumMapper.findCommentOwner(id);
        if (owner == null) return "回复不存在或已删除";
        if (!owner.equals(uid) && !SecurityUtils.isAdmin()) {
            return "只能删除自己的回复";
        }
        forumMapper.deleteComment(id);
        forumMapper.addCommentCount(forumMapper.findPostIdByComment(id), -1);
        return "删除成功";
    }
}
