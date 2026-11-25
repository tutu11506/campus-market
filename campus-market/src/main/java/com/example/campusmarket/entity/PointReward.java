package com.example.campusmarket.entity;

import lombok.Data;
import java.util.Date;

/** 积分兑换商品 */
@Data
public class PointReward {
    private Integer id;
    private String name;
    private String icon;
    /** coupon/top/stall/badge/cashback */
    private String type;
    private Integer pricePoints;
    private Integer stock;
    private Integer totalRedeemed;
    private String description;
    /** 配置 JSON：如置顶天数、优惠券面额 */
    private String configJson;
    private Integer sortOrder;
    private Integer status;
    private Date createdAt;
}
