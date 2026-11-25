package com.example.campusmarket.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 评审团评价：订单完成后买卖双方互评，附理由与证据；
 * 由其他用户投票判断真实性，支持率超过 60%（至少 3 票）才公开展示。
 */
@Data
public class JuryReview {
    private Integer id;
    private Integer orderId;          // 关联订单
    private Integer reviewerId;       // 评价人
    private Integer revieweeId;       // 被评价人（交易对方）
    private String reviewerRole;      // buyer/seller
    private Integer rating;           // 1-5 星
    private String reason;            // 评价理由
    private String evidence;          // 证据图片，逗号分隔
    private String status;            // pending/displayed/hidden
    private Integer displayVotes;     // 支持展示票数
    private Integer hideVotes;        // 反对展示票数
    private Integer totalVotes;       // 总票数
    private BigDecimal approveRate;   // 支持率（百分比）
    private Date createTime;
    private Date updateTime;

    // 展示字段
    private String orderNo;
    private BigDecimal orderPrice;
    private Integer goodsId;
    private String goodsTitle;
    private String goodsCover;
    private String reviewerName;
    private String reviewerAvatar;
    private String revieweeName;
    private String revieweeAvatar;
    private Integer myVote;           // 当前登录用户投的票：1支持/0反对/null未投
}
