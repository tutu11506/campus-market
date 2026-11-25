package com.example.campusmarket.entity;

import lombok.Data;

import java.util.Date;

/**
 * 教材心愿：用户在教材详情标记「想要这本书」后，同名教材有新闲置上架即提醒。
 */
@Data
public class TextbookWish {
    private Integer id;
    private Integer userId;
    private Integer textbookId;
    private String textbookTitle;
    private Integer status;
    private Date lastNotifyAt;
    private Date createTime;

    // 联表展示字段
    private String coverImage;
    private String subject;
    private String edition;
    private String isbn;
    private String title; // 教材主表书名（匹配提醒时使用）
}
