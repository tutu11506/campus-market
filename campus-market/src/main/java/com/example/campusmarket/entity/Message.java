package com.example.campusmarket.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class Message {
    private Integer id;              // 消息ID
    private Integer senderId;        // 发送者ID
    private Integer receiverId;      // 接收者ID
    private String content;          // 消息内容（文本为正文；图片/文件为服务器相对路径，如 /upload/xxx.jpg）
    private String msgType;          // 消息类型：text / image / file
    private String fileName;         // 文件名（file 类型用，便于前端展示与下载）
    private Long fileSize;           // 文件大小（字节，file 类型用）
    private Integer goodsId;         // 关联商品ID
    private Integer isRead;          // 是否已读：0未读 1已读
    private Integer autoReply;       // 是否为机器人自动回复：0否 1是
    private Date createTime;         // 发送时间

    // 额外字段
    private String senderName;       // 发送者昵称
    private String senderAvatar;     // 发送者头像
    private String goodsTitle;       // 关联商品标题
    private String goodsCover;       // 关联商品封面
    private BigDecimal goodsPrice;   // 关联商品价格（聊天记录 JOIN 带出，用于卡片展示）
}