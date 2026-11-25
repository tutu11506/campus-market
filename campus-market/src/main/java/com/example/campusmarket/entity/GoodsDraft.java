package com.example.campusmarket.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品草稿：发布页未提交的内容，支持一键继续编辑。
 */
@Data
public class GoodsDraft {
    private Integer id;
    private Integer userId;
    private String title;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String images;
    private String coverImage;
    private Integer categoryId;
    private Integer textbookId;
    private String location;
    private String tradeMode;
    private Integer canMail;
    private Integer canDeliver;
    private Integer exchangeSupported;
    private String exchangeWant;
    private Date createTime;
    private Date updateTime;

    // 展示字段
    private String categoryName;
}
