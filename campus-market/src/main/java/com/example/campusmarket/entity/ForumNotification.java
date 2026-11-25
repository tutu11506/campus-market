package com.example.campusmarket.entity;

import lombok.Data;

import java.util.Date;

/**
 * 论坛通知：帖子被回复、回复被回复时写入。
 */
@Data
public class ForumNotification {
    private Integer id;
    private Integer userId;        // 接收通知的用户
    private String type;           // post_reply 帖子被回复 / comment_reply 回复被回复
    private Integer postId;
    private Integer commentId;
    private Integer fromUserId;    // 触发通知的用户
    private String postTitle;
    private String content;        // 回复内容摘要
    private Integer isRead;
    private Date createTime;

    // 关联展示字段
    private String fromUserName;
    private String fromUserAvatar;
}
