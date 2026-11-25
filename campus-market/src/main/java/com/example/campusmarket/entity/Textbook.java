package com.example.campusmarket.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 教材主表：同一 ISBN 聚合多件 Goods/RentalItem。
 * 承载"老师指定版本"标记与版本避雷计数。
 */
@Data
public class Textbook {
    private Integer id;
    private String isbn;          // unique，空串表示未录入 ISBN
    private String title;
    private String subtitle;
    private String author;
    private String publisher;
    private String edition;       // 第七版 / 2023版
    private String subject;       // 数学 / 英语 / 计算机 / 物理 / 经济
    private String grade;         // 大二下 / 大三
    private String coverImage;

    private Integer teacherFlag;  // 是否老师指定版本 1=是 0=否
    private String teacherNote;   // 老师指定备注
    private Integer warnCount;    // 避雷留言计数

    private Integer status;       // 1=有效 0=隐藏
    private Date createTime;
    private Date updateTime;

    // 展示字段：同款在售闲置/可租数量，价格区间
    private Integer goodsCount;
    private Integer rentalCount;
    private BigDecimal goodsMinPrice;
    private BigDecimal rentalMinPrice;
}
