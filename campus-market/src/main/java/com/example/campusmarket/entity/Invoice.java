package com.example.campusmarket.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 电子发票。
 */
@Data
public class Invoice {
    private Integer id;
    private Integer orderId;
    private String orderNo;
    private Integer userId;        // 申请开票的买家
    private String titleType;      // personal 个人 / company 企业
    private String title;          // 发票抬头
    private String taxNo;          // 纳税人识别号
    private String email;          // 接收邮箱
    private BigDecimal amount;     // 开票金额（实付）
    private String invoiceNo;      // 发票号码
    private String status;         // issued 已开票
    private Date createTime;
    private Date issueTime;

    // 展示字段
    private String goodsTitle;
}
