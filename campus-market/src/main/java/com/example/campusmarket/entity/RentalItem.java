package com.example.campusmarket.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * 租赁物品：短期租赁、假期寄存转租、教材学期租共用同一张表。
 */
@Data
public class RentalItem {
    private Integer id;
    private String title;
    private String description;
    private String rentalType;      // daily / weekly / storage / textbook
    private String category;

    // 教材学期租可选关联教材主表
    private Integer textbookId;
    private BigDecimal price;       // 租金单价
    private String priceUnit;       // day / week / semester
    private BigDecimal deposit;     // 押金（仅登记）
    private String coverImage;
    private String location;
    private Integer ownerId;
    private String status;          // available / rented / offline
    private LocalDate autoOfflineDate;
    private Date createTime;
    private Date updateTime;

    // 展示字段
    private String ownerName;
    private String ownerStudentNo;
}
