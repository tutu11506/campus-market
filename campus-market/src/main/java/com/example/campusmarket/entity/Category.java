package com.example.campusmarket.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Category {
    private Integer id;          // 分类ID
    private String name;         // 分类名称
    private String icon;         // 分类图标
    private Integer sort;        // 排序
    private Date createTime;     // 创建时间
}