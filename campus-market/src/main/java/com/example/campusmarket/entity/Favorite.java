package com.example.campusmarket.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Favorite {
    private Integer id;              // 收藏ID
    private Integer userId;          // 用户ID
    private Integer goodsId;         // 商品ID
    private Date createTime;         // 收藏时间

    // 额外字段
    private String goodsTitle;       // 商品标题
    private String goodsCover;       // 商品封面
    private String goodsPrice;       // 商品价格
    private String sellerName;       // 卖家昵称
}