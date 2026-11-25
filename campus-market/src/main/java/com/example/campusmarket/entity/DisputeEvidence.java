package com.example.campusmarket.entity;

import lombok.Data;

import java.util.Date;

/** 纠纷证据：聊天记录截图、商品照片、文字说明 */
@Data
public class DisputeEvidence {
    private Integer id;
    private Integer disputeId;
    private Integer uploaderId;
    private String type;       // chat/image/text
    private String content;    // 聊天记录文字/说明
    private String fileUrl;    // 截图/照片地址
    private Date createTime;

    // 展示字段
    private String uploaderName;
    private String uploaderAvatar;
}
