package com.example.campusmarket.entity;

import lombok.Data;

import java.util.Date;

/**
 * 搭子成员：同一用户在同一搭子下只有一条记录，退出后再次加入复用该记录。
 */
@Data
public class PartnerMember {
    private Integer id;
    private Integer partnerId;
    private Integer userId;
    private String status;      // joined / left
    private Date createTime;
    private Date updateTime;

    // 展示字段
    private String userName;
    private String userAvatar;
    private String userStudentNo;
}
