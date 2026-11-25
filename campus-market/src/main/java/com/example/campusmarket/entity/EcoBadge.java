package com.example.campusmarket.entity;

import lombok.Data;

/** 环保勋章定义 */
@Data
public class EcoBadge {
    private Integer id;
    private String code;
    private String name;
    private String icon;
    private String description;
    /** total_points / goods_sold / donation_count / rental_complete */
    private String conditionType;
    private Integer conditionValue;
    private Integer rewardPoints;
}
