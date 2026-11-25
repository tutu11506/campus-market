package com.example.campusmarket.vo;

import lombok.Data;

import java.util.Date;

/**
 * 会话列表视图对象
 * 与前端 Message.vue 模板字段对齐：otherId / otherName / otherAvatar /
 * lastContent / lastTime / unreadCount / goodsId / goodsTitle / goodsCover
 */
@Data
public class ConversationVO {
    private Integer otherId;       // 对方用户ID
    private String otherName;      // 对方昵称
    private String otherAvatar;    // 对方头像
    private String lastContent;    // 最后一条消息内容
    private String msgType;        // 最后一条消息类型：text / image / file（前端据此展示 [图片]/[文件] 预览）
    private Date lastTime;         // 最后一条消息时间
    private Integer unreadCount;   // 该会话未读消息数
    private Integer goodsId;       // 关联商品ID
    private String goodsTitle;     // 关联商品标题
    private String goodsCover;     // 关联商品封面
}
