package com.example.campusmarket.entity;

import lombok.Data;
import java.util.Date;

/** 积分流水 */
@Data
public class PointRecord {
    private Integer id;
    private Integer userId;
    /** 1=获得 -1=消费 */
    private Integer direction;
    private Integer points;
    private Integer balanceAfter;
    /** 发布闲置/成交/发布可租/完成租赁/捐赠/兑换/管理员调整 */
    private String type;
    private String refType;
    private Integer refId;
    private String remark;
    private Date createdAt;
    // 展示
    private String username;

    public boolean isIncome() { return direction != null && direction == 1; }
}
