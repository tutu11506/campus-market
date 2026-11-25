package com.example.campusmarket.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 站内提醒记录：降价、关键词上新、教材上新、开学/毕业定时提醒统一落库。
 */
@Data
public class NotifyRecord {
    private Integer id;
    private Integer userId;
    private String notifyType; // price_drop/new_goods/textbook_new/semester_start/graduation
    private String title;
    private String content;
    private String link;
    private Integer goodsId;
    private Integer textbookId;
    private String keyword;
    private BigDecimal refPrice;
    private Integer isRead;
    private Date createTime;
}
