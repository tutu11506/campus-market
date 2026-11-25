package com.example.campusmarket.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 退货申请单。
 * 本项目无在线支付，退货为纯业务状态流转：买家申请 → 卖家审核 → 同意则订单标记已退货、商品重新上架。
 * 一单可多次申请（被拒后可再次申请），以本表保留审核痕迹，order.refund_status 冗余展示当前进度。
 */
@Data
public class Refund {
    private Integer id;              // 退货申请ID
    private String refundNo;         // 退货单号 RF+时间戳
    private Integer orderId;         // 关联订单ID
    private Integer goodsId;         // 商品ID（冗余）
    private Integer buyerId;         // 申请人（买家）ID
    private Integer sellerId;        // 被申请人（卖家/审核人）ID
    private BigDecimal amount;       // 退款金额（=订单 price，仅记录，线下退还）
    private String reason;           // 退货原因（必填）
    private String description;      // 详细说明（选填）
    private String evidenceUrls;     // 凭证图片URL，逗号分隔（选填）
    private String status;           // 状态：pending待审核/approved已同意/rejected已拒绝
    private String rejectReason;     // 拒绝原因（拒绝时必填）
    private Integer auditorId;       // 审核人ID（=卖家ID）
    private Date auditTime;          // 审核时间
    private Date createTime;         // 申请时间
    private Date updateTime;         // 更新时间

    // 展示字段（关联查询带出）
    private String orderNo;          // 订单号
    private String goodsTitle;       // 商品标题
    private String goodsCover;       // 商品封面
    private String buyerName;        // 买家昵称
    private String buyerAvatar;      // 买家头像
    private String sellerName;       // 卖家昵称
    private String sellerAvatar;     // 卖家头像
}
