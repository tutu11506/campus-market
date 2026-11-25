package com.example.campusmarket.entity;

import lombok.Data;
import java.util.Date;

/**
 * 用户商品浏览历史。
 * 同一用户对同一商品只保留一条记录（uk_user_goods），重复浏览刷新时间并累加 browse_count，
 * 保证“发送商品”选择器与“我的浏览历史”里每件商品只出现一次。
 */
@Data
public class BrowseHistory {
    private Long id;
    private Integer userId;       // 浏览者用户ID
    private Integer goodsId;      // 被浏览商品ID
    private Integer sellerId;     // 商品卖家ID（冗余，便于按店铺筛选）
    private Integer browseCount;  // 累计浏览次数
    private Date createTime;      // 最近一次浏览时间
}
