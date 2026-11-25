package com.example.campusmarket.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/** 价格高于近期同类均价的预警记录 */
@Data
public class PriceAlert {
    private Integer id;
    private Integer goodsId;
    private Integer sellerId;
    private String title;
    private BigDecimal price;
    private BigDecimal avgPrice;
    private BigDecimal ratio;      // price / avg_price
    private Integer sampleSize;    // 参与均价的样本数
    private String level;          // warning/serious
    private String status;         // open/handled/dismissed
    private String note;
    private Integer handlerId;
    private Date createTime;
    private Date handledAt;

    // 展示字段
    private String sellerName;
    private String sellerAvatar;
    private String goodsCover;
    private String goodsStatus;
}
