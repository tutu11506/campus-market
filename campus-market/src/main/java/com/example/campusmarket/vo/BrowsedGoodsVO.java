package com.example.campusmarket.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * “我的浏览历史”列表项：商品展示信息 + 浏览时间/次数。
 * 保留 status 字段，前端可对已售/下架商品打标签。
 */
@Data
public class BrowsedGoodsVO {
    private Integer id;            // 商品ID
    private String title;
    private String coverImage;
    private BigDecimal price;
    private String status;         // on_sale / sold / off_shelf
    private Integer sellerId;
    private String sellerName;
    private Date browseTime;       // 最近一次浏览时间
    private Integer browseCount;   // 累计浏览次数
}
