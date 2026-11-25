package com.example.campusmarket.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ForumPost {
    private Integer id;
    private String board;        // chat 校园闲聊 / lost 失物招领 / study 学习交流 / team 拼单组队
    private String title;
    private String content;
    private Integer userId;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Date createTime;

    // 关联展示字段
    private String authorName;
    private String authorAvatar;
    private Boolean liked;       // 当前登录用户是否已点赞
}
