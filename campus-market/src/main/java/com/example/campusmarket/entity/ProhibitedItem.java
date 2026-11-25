package com.example.campusmarket.entity;

import lombok.Data;

/** 违禁品识别词库条目 */
@Data
public class ProhibitedItem {
    private Integer id;
    private String category;   // appliance/e_cigarette/battery/dangerous...
    private String label;      // 展示标签
    private String keywords;   // 逗号分隔关键词
    private String riskLevel;  // block/review
    private String advice;     // 拦截提示语
    private Integer status;    // 1 启用
}
