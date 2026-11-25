package com.example.campusmarket.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 心愿单商品关注：收藏即写入，商品降价时对比 last_price 推送提醒。
 */
@Data
public class GoodsWatch {
    private Integer id;
    private Integer userId;
    private Integer goodsId;
    private BigDecimal lastPrice;   // 上次记录/提醒时的价格
    private BigDecimal targetPrice; // 期望价（可空）
    private Integer status;         // 1关注中 0已取消
    private Date lastNotifyAt;
    private Date createTime;

    // 联表展示字段
    private String title;
    private String coverImage;
    private BigDecimal currentPrice;
    private String goodsStatus;
    private String sellerName;
}
