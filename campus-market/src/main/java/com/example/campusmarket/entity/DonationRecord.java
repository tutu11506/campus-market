package com.example.campusmarket.entity;

import lombok.Data;

import java.util.Date;

/**
 * 公益捐赠登记与实收记录。
 */
@Data
public class DonationRecord {
    private Integer id;
    private Integer userId;
    private Integer orgId;
    private String itemType;
    private String itemName;
    private Integer quantity;
    private String conditionLevel;
    private String handoverMethod;
    private String handoverDetail;
    private String contact;
    private String remark;
    private String status;
    private Integer receivedQuantity;
    private Integer receivedBy;
    private Date receivedTime;
    private Date createTime;
    private Date updateTime;

    // 展示字段
    private String donorName;
    private String donorStudentNo;
    private String organizationName;
    private String receiverName;
}
