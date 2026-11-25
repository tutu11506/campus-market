package com.example.campusmarket.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 教材笔记/课件/试卷附件：可挂在 textbook/goods/rental 上。
 * delivery_mode: free 免费公开 / bonus 随书附赠 / paid 单独售卖
 */
@Data
public class TextbookMaterial {
    private Integer id;
    private String ownerType;    // textbook / goods / rental
    private Integer ownerId;
    private Integer uploaderId;

    private String title;
    private String description;
    private String fileType;     // pdf / ppt / doc / zip / image
    private String fileUrl;
    private Long fileSize;

    private String deliveryMode; // free / bonus / paid
    private BigDecimal price;

    private Integer views;
    private Integer downloads;
    private Integer status;

    private Date createTime;

    // 展示字段
    private String uploaderName;
    private String ownerTitle;   // 教材/闲置/租赁的标题
}
