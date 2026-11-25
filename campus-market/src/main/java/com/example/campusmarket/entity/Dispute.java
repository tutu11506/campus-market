package com.example.campusmarket.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/** 交易纠纷仲裁 */
@Data
public class Dispute {
    private Integer id;
    private Integer orderId;
    private Integer initiatorId;    // 发起人
    private Integer respondentId;   // 对方
    private String initiatorRole;   // buyer/seller
    private String reasonType;      // quality/not_as_described/no_delivery/refund_refused/other
    private String description;
    private BigDecimal claimAmount; // 诉求金额
    private String status;          // open/processing/resolved/rejected/withdrawn
    private String verdict;         // buyer_win/seller_win/split/no_fault
    private String verdictNote;
    private Integer escrowFrozen;   // 1=担保资金已冻结
    private Integer adminId;
    private Date createTime;
    private Date updateTime;
    private Date resolvedAt;

    // 展示字段
    private String orderNo;
    private BigDecimal orderPrice;
    private String orderStatus;
    private String orderEscrowStatus;
    private String goodsTitle;
    private String goodsCover;
    private String initiatorName;
    private String initiatorAvatar;
    private String respondentName;
    private String respondentAvatar;
    private Integer evidenceCount;
}
