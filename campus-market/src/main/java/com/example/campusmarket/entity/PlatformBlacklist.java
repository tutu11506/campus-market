package com.example.campusmarket.entity;

import lombok.Data;

import java.util.Date;

/** 平台共享黑名单（校内失信记录，全平台提醒） */
@Data
public class PlatformBlacklist {
    private Integer id;
    private Integer userId;
    private String level;       // watch/warning/serious
    private String reason;
    private String evidence;
    private Integer reportCount;
    private String status;      // active/revoked
    private Integer createdBy;
    private Date createTime;
    private Integer revokedBy;
    private Date revokedAt;

    // 展示字段
    private String userName;
    private String userAvatar;
    private String studentNo;
    private Integer credit;
    private String createdByName;
}
