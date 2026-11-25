package com.example.campusmarket.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 校园任务大厅：同学之间发布的悬赏任务
 */
@Data
public class CampusTask {
    private Integer id;              // 任务ID
    private String title;            // 任务标题
    private String description;      // 任务描述
    private BigDecimal reward;       // 悬赏金额
    private String category;         // 任务类型：跑腿代取/学习辅导/体力帮忙等
    private String location;         // 任务地点
    private Date deadline;           // 期望完成时间
    private Integer publisherId;     // 发布人ID
    private String status;           // open待接单/accepted进行中/completed已完成/cancelled已取消
    private Integer accepterId;      // 接单人ID
    private Date acceptTime;         // 接单时间
    private Date completeTime;       // 完成时间
    private Integer isDeleted;       // 是否删除
    private Date createTime;         // 发布时间
    private Date updateTime;         // 更新时间
}
