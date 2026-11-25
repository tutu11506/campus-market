package com.example.campusmarket.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ForumComment {
    private Integer id;
    private Integer postId;
    private Integer userId;
    private Integer parentId;        // 回复的评论ID，null 表示直接回复帖子
    private Integer replyToUserId;   // 被回复的用户ID
    private String content;
    private Date createTime;

    private String authorName;
    private String authorAvatar;
    private String replyToName;      // 被回复人昵称
}
