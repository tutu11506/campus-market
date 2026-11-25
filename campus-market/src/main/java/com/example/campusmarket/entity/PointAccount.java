package com.example.campusmarket.entity;

import lombok.Data;
import java.util.Date;

/** 用户积分账户 */
@Data
public class PointAccount {
    private Integer userId;
    private Integer balance;
    private Integer earnedTotal;
    private Integer spentTotal;
    private Date updatedAt;
    // 展示：用户名、学院
    private String username;
    private String college;
}
