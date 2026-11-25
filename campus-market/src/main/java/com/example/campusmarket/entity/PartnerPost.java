package com.example.campusmarket.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 搭子组队：装备拼买、跳蚤市场同行、考研学习、运动等；
 * 可关联闲置商品或进行中的拼单。发起人自动占 1 人，member_count 含发起人。
 */
@Data
public class PartnerPost {
    private Integer id;
    private Integer ownerId;
    private String type;            // gear / flea / exam / sport / other
    private String title;
    private String description;
    private LocalDateTime activityTime;
    private String location;
    private Integer memberLimit;
    private Integer memberCount;
    private String linkType;        // none / goods / groupbuy
    private Integer linkId;
    private String linkTitle;
    private String status;          // open / full / finished / cancelled
    private Date createTime;
    private Date updateTime;

    // 展示字段
    private String ownerName;
    private String ownerStudentNo;
    private Integer myStatus;       // 1 已加入 / 0 未加入
    private Integer remainingCount; // 剩余名额
    private List<PartnerMember> members;
}
