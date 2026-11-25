package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.ForumComment;
import com.example.campusmarket.entity.ForumNotification;
import com.example.campusmarket.entity.ForumPost;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface ForumMapper {

    String POST_COLUMNS =
            "SELECT p.id, p.board, p.title, p.content, p.user_id AS userId, p.view_count AS viewCount, " +
            "p.like_count AS likeCount, p.comment_count AS commentCount, p.create_time AS createTime, " +
            "u.username AS authorName, u.avatar AS authorAvatar, " +
            "(SELECT COUNT(*) FROM forum_like fl WHERE fl.post_id = p.id AND fl.user_id = #{uid}) > 0 AS liked " +
            "FROM forum_post p LEFT JOIN user u ON p.user_id = u.id ";

    /** 帖子列表（板块 + 关键词筛选） */
    @Select("<script>" + POST_COLUMNS +
            "WHERE p.is_deleted = 0 " +
            "<if test='board != null and board != \"\"'> AND p.board = #{board} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (p.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.content LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "ORDER BY p.create_time DESC LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<ForumPost> findPosts(@Param("board") String board,
                              @Param("keyword") String keyword,
                              @Param("uid") Integer uid,
                              @Param("offset") int offset,
                              @Param("pageSize") int pageSize);

    @Select("<script>" +
            "SELECT COUNT(*) FROM forum_post p WHERE p.is_deleted = 0 " +
            "<if test='board != null and board != \"\"'> AND p.board = #{board} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (p.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.content LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "</script>")
    int countPosts(@Param("board") String board, @Param("keyword") String keyword);

    /** 帖子详情 */
    @Select(POST_COLUMNS + "WHERE p.id = #{id} AND p.is_deleted = 0")
    ForumPost findById(@Param("id") Integer id, @Param("uid") Integer uid);

    @Select("SELECT user_id FROM forum_post WHERE id = #{id} AND is_deleted = 0")
    Integer findOwner(@Param("id") Integer id);

    @Insert("INSERT INTO forum_post(board, title, content, user_id) VALUES(#{board}, #{title}, #{content}, #{userId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPost(ForumPost post);

    @Update("UPDATE forum_post SET is_deleted = 1 WHERE id = #{id}")
    int deletePost(@Param("id") Integer id);

    @Update("UPDATE forum_post SET view_count = view_count + 1 WHERE id = #{id}")
    int addViewCount(@Param("id") Integer id);

    @Update("UPDATE forum_post SET like_count = GREATEST(like_count + #{delta}, 0) WHERE id = #{id}")
    int addLikeCount(@Param("id") Integer id, @Param("delta") int delta);

    @Update("UPDATE forum_post SET comment_count = GREATEST(comment_count + #{delta}, 0) WHERE id = #{id}")
    int addCommentCount(@Param("id") Integer id, @Param("delta") int delta);

    @Select("SELECT COUNT(*) FROM forum_like WHERE post_id = #{postId} AND user_id = #{userId}")
    int countLike(@Param("postId") Integer postId, @Param("userId") Integer userId);

    @Insert("INSERT IGNORE INTO forum_like(post_id, user_id) VALUES(#{postId}, #{userId})")
    int insertLike(@Param("postId") Integer postId, @Param("userId") Integer userId);

    @Delete("DELETE FROM forum_like WHERE post_id = #{postId} AND user_id = #{userId}")
    int deleteLike(@Param("postId") Integer postId, @Param("userId") Integer userId);

    /** 回复列表（带被回复人昵称，便于前端展示“回复 @某人”） */
    @Select("SELECT c.id, c.post_id AS postId, c.user_id AS userId, c.parent_id AS parentId, " +
            "c.reply_to_user_id AS replyToUserId, c.content, c.create_time AS createTime, " +
            "u.username AS authorName, u.avatar AS authorAvatar, ru.username AS replyToName " +
            "FROM forum_comment c LEFT JOIN user u ON c.user_id = u.id " +
            "LEFT JOIN user ru ON c.reply_to_user_id = ru.id " +
            "WHERE c.post_id = #{postId} AND c.is_deleted = 0 ORDER BY c.create_time ASC")
    List<ForumComment> findComments(@Param("postId") Integer postId);

    @Insert("INSERT INTO forum_comment(post_id, user_id, parent_id, reply_to_user_id, content) " +
            "VALUES(#{postId}, #{userId}, #{parentId}, #{replyToUserId}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertComment(ForumComment comment);

    /** 按ID查回复（校验被回复评论是否属于同一帖子） */
    @Select("SELECT id, post_id AS postId, user_id AS userId, parent_id AS parentId, content " +
            "FROM forum_comment WHERE id = #{id} AND is_deleted = 0")
    ForumComment findCommentById(@Param("id") Integer id);

    /** 帖子简要信息（回复时用于取楼主与标题） */
    @Select("SELECT id, title, user_id AS userId FROM forum_post WHERE id = #{id} AND is_deleted = 0")
    ForumPost findPostBrief(@Param("id") Integer id);

    /** 我发布的帖子 */
    @Select(POST_COLUMNS + "WHERE p.is_deleted = 0 AND p.user_id = #{uid} " +
            "ORDER BY p.create_time DESC LIMIT #{offset}, #{pageSize}")
    List<ForumPost> findMyPosts(@Param("uid") Integer uid,
                                @Param("offset") int offset,
                                @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM forum_post WHERE is_deleted = 0 AND user_id = #{uid}")
    int countMyPosts(@Param("uid") Integer uid);

    /** 写入论坛通知 */
    @Insert("INSERT INTO forum_notification(user_id, type, post_id, comment_id, from_user_id, post_title, content) " +
            "VALUES(#{userId}, #{type}, #{postId}, #{commentId}, #{fromUserId}, #{postTitle}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertNotification(ForumNotification notification);

    /** 我的论坛通知 */
    @Select("SELECT n.*, u.username AS fromUserName, u.avatar AS fromUserAvatar " +
            "FROM forum_notification n LEFT JOIN user u ON n.from_user_id = u.id " +
            "WHERE n.user_id = #{userId} ORDER BY n.create_time DESC LIMIT #{offset}, #{pageSize}")
    List<ForumNotification> findNotifications(@Param("userId") Integer userId,
                                              @Param("offset") int offset,
                                              @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM forum_notification WHERE user_id = #{userId}")
    int countNotifications(@Param("userId") Integer userId);

    @Select("SELECT COUNT(*) FROM forum_notification WHERE user_id = #{userId} AND is_read = 0")
    int countUnreadNotifications(@Param("userId") Integer userId);

    @Update("UPDATE forum_notification SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    int markNotificationsRead(@Param("userId") Integer userId);

    @Select("SELECT user_id FROM forum_comment WHERE id = #{id} AND is_deleted = 0")
    Integer findCommentOwner(@Param("id") Integer id);

    @Select("SELECT post_id FROM forum_comment WHERE id = #{id}")
    Integer findPostIdByComment(@Param("id") Integer id);

    @Update("UPDATE forum_comment SET is_deleted = 1 WHERE id = #{id}")
    int deleteComment(@Param("id") Integer id);
}
