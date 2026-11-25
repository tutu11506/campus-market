package com.example.campusmarket.entity;

import lombok.Data;

import java.util.Date;

/**
 * 关键词订阅：平台新上架商品命中关键词时，向订阅用户推送站内提醒。
 */
@Data
public class KeywordSubscription {
    private Integer id;
    private Integer userId;
    private String keyword;
    private Integer notifyNew;   // 1=新上架命中提醒
    private Integer matchCount;  // 累计命中次数
    private Integer status;      // 1启用 0停用
    private Date lastNotifyAt;
    private Date createTime;
}
