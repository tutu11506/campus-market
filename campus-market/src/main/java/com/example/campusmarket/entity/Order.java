package com.example.campusmarket.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class Order {
    private Integer id;              // 订单ID
    private String orderNo;          // 订单号
    private Integer goodsId;         // 商品ID
    private Integer buyerId;         // 买家ID
    private Integer sellerId;        // 卖家ID
    private BigDecimal price;        // 成交价格
    private BigDecimal discountAmount; // 平台优惠券抵扣
    private String status;           // 状态：pending待确认/dealing交易中/completed已完成/cancelled已取消/returned已退货
    private Date payTime;            // 支付时间（模拟支付，null 表示待支付）
    private String escrowStatus;     // 担保资金：held托管中/settled已结算/refunded已退款
    private String verifyCode;       // 线下自提核销码（买家出示，卖家核销）
    private Date verifyTime;         // 核销时间
    private Date settleTime;         // 资金结算给卖家的时间
    private String refundStatus;     // 退货进度：null无/returning退货审核中/returned已退货/return_rejected退货被拒
    private String remark;           // 备注
    private Date createTime;         // 创建时间
    private Date updateTime;         // 更新时间

    // 额外字段
    private String goodsTitle;       // 商品标题
    private String goodsCover;       // 商品封面
    private String buyerName;        // 买家昵称
    private String buyerAvatar;      // 买家头像
    private String sellerName;       // 卖家昵称
    private String sellerAvatar;     // 卖家头像
    private Integer isCommented;     // 是否已评价（0否/1是，关联 comment 表，展示用）
}