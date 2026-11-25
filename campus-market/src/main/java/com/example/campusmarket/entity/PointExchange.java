package com.example.campusmarket.entity;

import lombok.Data;
import java.util.Date;

/** 积分兑换订单 */
@Data
public class PointExchange {
    private Integer id;
    private Integer userId;
    private Integer rewardId;
    private String rewardSnapshot;
    private Integer pointsCost;
    /** 1=已发放 0=处理中 -1=失败已退积分 */
    private Integer status;
    private Date deliveredAt;
    private Date createdAt;
    // 展示
    private String username;
    private String rewardName;
    private String rewardIcon;
    private String rewardType;
}
