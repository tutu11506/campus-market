package com.example.campusmarket.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 求购报价（卖家接单报价）。
 */
@Data
public class DemandQuote {
    private Integer id;
    private Integer demandId;
    private Integer sellerId;
    private BigDecimal price;
    private String message;
    private String status;         // pending/accepted/rejected
    private Date createTime;

    // 展示字段
    private String sellerName;
    private String sellerAvatar;
}
