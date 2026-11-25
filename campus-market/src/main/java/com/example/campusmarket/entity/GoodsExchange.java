package com.example.campusmarket.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 以物换物申请：买家拿自己的在售商品，申请换卖家的商品。
 */
@Data
public class GoodsExchange {
    private Integer id;
    private Integer goodsId;         // 目标商品
    private Integer requesterId;     // 发起人
    private Integer offerGoodsId;    // 发起人拿出的商品
    private String message;
    private String status;           // pending/accepted/rejected/completed/cancelled
    private Integer requesterDone;
    private Integer ownerDone;
    private Date createTime;

    // 展示字段
    private String goodsTitle;
    private String goodsCover;
    private BigDecimal goodsPrice;
    private Integer ownerId;
    private String offerTitle;
    private String offerCover;
    private BigDecimal offerPrice;
    private String requesterName;
}
