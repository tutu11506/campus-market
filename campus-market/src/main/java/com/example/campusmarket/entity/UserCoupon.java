package com.example.campusmarket.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class UserCoupon {
    private Integer id;
    private Integer userId;
    private BigDecimal thresholdAmount; // 满多少元可用
    private BigDecimal discountAmount;  // 减多少元
    private String status;              // pending待领取 / unused可用 / used已使用
    private Integer orderId;
    private Date expireTime;
    private Date usedTime;
    private Date createTime;
}
