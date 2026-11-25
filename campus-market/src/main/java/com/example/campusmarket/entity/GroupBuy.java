package com.example.campusmarket.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 拼单：全新团购、拼分装、共享会员、二手大件分摊共用同一张表。
 * 发起人自动占 1 份，joined_count 含发起人份数。
 */
@Data
public class GroupBuy {
    private Integer id;
    private Integer ownerId;
    private String type;            // fresh / split / membership / secondhand
    private String title;
    private String description;
    private Integer targetCount;    // 目标份数
    private Integer joinedCount;    // 已拼份数（含发起人）
    private BigDecimal pricePerShare;
    private LocalDateTime deadline;
    private Integer goodsId;        // 关联商品（可选）
    private String location;
    private String status;          // open / full / formed / failed / closed
    private Date createTime;
    private Date updateTime;

    // 展示字段
    private String ownerName;
    private String ownerStudentNo;
    private String goodsTitle;
    private String goodsCover;
    private BigDecimal goodsPrice;
    private Integer myShares;       // 当前登录用户参与份数，未参与为 0
    private Integer remainingCount; // 剩余份数
    private List<GroupBuyMember> members;
}
