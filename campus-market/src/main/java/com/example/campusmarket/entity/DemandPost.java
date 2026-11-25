package com.example.campusmarket.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 求购发布。
 */
@Data
public class DemandPost {
    private Integer id;
    private Integer userId;
    private String title;
    private String category;       // textbook/vehicle/furniture/digital/dorm/other
    private String keyword;        // 匹配在售商品的关键词
    private String description;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String targetMajor;    // 定向专业
    private String targetGrade;    // 定向年级
    private String status;         // open/matched/closed
    private Integer quoteCount;
    private BigDecimal dealPrice;
    private Date createTime;

    // 展示字段
    private String authorName;
    private String authorAvatar;
    private Integer matchCount;    // 系统匹配到的在售件数
}
