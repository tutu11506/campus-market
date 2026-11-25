package com.example.campusmarket.entity;

import lombok.Data;
import java.util.Date;

/**
 * 用户课表：导入后存储，用于匹配 textbook 主表。
 */
@Data
public class UserCourse {
    private Integer id;
    private Integer userId;
    private String courseName;
    private String teacher;
    private String semester;    // e.g. 2026-2027-1
    private String courseCode;
    private String remark;
    private Date createTime;
}
