package com.example.campusmarket.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * 租赁订单：pending 待出租方确认 → renting 租用中 → returning 待收回 → completed 已完成。
 * 押金仅做登记，不涉及真实资金流；逾期由定时任务标记并提醒双方。
 */
@Data
public class RentalOrder {
    private Integer id;
    private String orderNo;
    private Integer itemId;
    private Integer renterId;
    private Integer ownerId;
    private String rentalType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal rentAmount;
    private BigDecimal deposit;
    private String status;
    private Integer overdueReminded;
    private Integer dueReminded;
    private String remark;
    private String handoverNote;
    private Date confirmTime;
    private Date returnApplyTime;
    private Date returnTime;
    private Date createTime;
    private Date updateTime;

    // 展示与计算字段
    private String itemTitle;
    private String itemCover;
    private String category;
    private String priceUnit;
    private String renterName;
    private String ownerName;
    private Integer days;
    private Integer overdueDays;
    private Integer remainingDays;
}
