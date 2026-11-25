package com.example.campusmarket.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Comment {
    private Integer id;              // 评价ID
    private Integer orderId;         // 订单ID
    private Integer goodsId;         // 商品ID
    private Integer fromUserId;      // 评价人ID
    private Integer toUserId;        // 被评价人ID
    private Integer rating;          // 评分：1-5星
    private String content;          // 评价内容
    private Date createTime;         // 评价时间

    // 额外字段
    private String fromUserName;     // 评价人昵称
    private String fromUserAvatar;   // 评价人头像
    private String goodsTitle;       // 商品标题
    private String goodsCover;       // 商品封面（展示用）
}