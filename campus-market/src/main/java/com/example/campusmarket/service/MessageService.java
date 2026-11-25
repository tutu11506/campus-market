package com.example.campusmarket.service;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.entity.Message;
import com.example.campusmarket.mapper.MessageMapper;
import com.example.campusmarket.mapper.UserMapper;
import com.example.campusmarket.vo.ConversationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

    // 发送消息
    public String send(Message message) {
        if (message == null) {
            return "参数错误";
        }
        // 消息类型：缺省按文本处理；允许 text/image/file/goods（goods 为商品卡片）
        String type = message.getMsgType();
        if (type == null || type.trim().isEmpty()) {
            type = "text";
        }
        if (!"text".equals(type) && !"image".equals(type) && !"file".equals(type) && !"goods".equals(type)) {
            return "不支持的消息类型";
        }
        message.setMsgType(type);
        // 商品卡片消息必须带 goodsId，否则无法渲染卡片与跳转详情
        if ("goods".equals(type) && message.getGoodsId() == null) {
            return "商品信息缺失";
        }
        if (message.getContent() == null || message.getContent().trim().isEmpty()) {
            return "消息内容不能为空";
        }
        if (message.getContent().length() > 1000) {
            return "消息内容不能超过 1000 字";
        }
        Integer senderId = message.getSenderId();
        Integer receiverId = message.getReceiverId();
        if (receiverId == null) {
            return "接收人不存在";
        }
        if (receiverId.equals(senderId)) {
            return "不能给自己发消息";
        }
        if (userMapper.findById(receiverId) == null) {
            return "接收人不存在";
        }
        messageMapper.insert(message);
        return "发送成功";
    }

    // 查询聊天记录（先标记已读再查询，加事务保证查询失败时已读状态回滚，避免丢未读）
    // 返回最新一页（按时间倒序），前端再反转成正序展示，支持“加载更多历史”
    @Transactional(rollbackFor = Exception.class)
    public PageResult<Message> findChatRecord(Integer userId, Integer otherId, Integer pageNum, Integer pageSize) {
        // 先标记为已读
        messageMapper.markAsRead(userId, otherId);
        int[] p = PageResult.normalize(pageNum, pageSize, 50);
        List<Message> list = messageMapper.findChatRecordPage(userId, otherId, p[2], p[1]);
        int total = messageMapper.countChatRecord(userId, otherId);
        return PageResult.of(list, total, p[0], p[1]);
    }

    // 查询会话列表
    public List<ConversationVO> findConversationList(Integer userId) {
        return messageMapper.findConversationList(userId);
    }

    // 查询未读数量
    public int countUnread(Integer userId) {
        return messageMapper.countUnread(userId);
    }
}