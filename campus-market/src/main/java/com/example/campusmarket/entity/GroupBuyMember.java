package com.example.campusmarket.entity;

import lombok.Data;

import java.util.Date;

/**
 * 拼单成员：同一用户在同一拼单下只有一条记录，退出后再次参与复用该记录。
 */
@Data
public class GroupBuyMember {
    private Integer id;
    private Integer groupId;
    private Integer userId;
    private Integer shares;
    private String status;      // joined / left
    private Date createTime;
    private Date updateTime;

    // 展示字段
    private String userName;
    private String userAvatar;
    private String userStudentNo;
}
