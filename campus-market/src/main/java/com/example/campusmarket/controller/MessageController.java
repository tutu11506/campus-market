package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.config.RealtimeHub;
import com.example.campusmarket.entity.Message;
import com.example.campusmarket.service.MessageService;
import com.example.campusmarket.service.RiskService;
import com.example.campusmarket.vo.ConversationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private RealtimeHub realtimeHub;

    @Autowired
    private RiskService riskService;

    // 实时消息通道（SSE）：EventSource 不能带自定义头，token 通过查询参数传入
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            throw new com.example.campusmarket.common.ForbiddenException("未登录");
        }
        return realtimeHub.register(uid);
    }

    // 发送消息（senderId 以 token 为准，忽略前端传入）
    @PostMapping("/send")
    public Result<String> send(@RequestBody Message message) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        // 风控：敏感词 + 拉黑
        riskService.assertNoSensitive(message.getContent());
        if (message.getReceiverId() != null && riskService.isBlocked(uid, message.getReceiverId())) {
            return Result.error("对方已拉黑你或你拉黑了对方，无法发送消息");
        }
        if (riskService.isMuted(uid)) {
            return Result.error("信用分过低（<60），已被禁止发消息");
        }
        message.setSenderId(uid);
        String result = messageService.send(message);
        if ("发送成功".equals(result) && message.getReceiverId() != null) {
            // 推给接收方：新消息 + 最新未读数（多标签页同时收到）
            realtimeHub.publish(message.getReceiverId(), "message", message);
            realtimeHub.publish(message.getReceiverId(), "unread", messageService.countUnread(message.getReceiverId()));
        }
        return Result.success(result);
    }

    // 查询聊天记录（userId 以 token 为准，忽略前端传入；分页，默认每页 20 条）
    @GetMapping("/chat")
    public Result<PageResult<Message>> findChatRecord(
            @RequestParam Integer userId,
            @RequestParam Integer otherId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        PageResult<Message> page = messageService.findChatRecord(uid, otherId, pageNum, pageSize);
        // 打开会话即视为已读，通知对方刷新“已读”状态
        realtimeHub.publish(otherId, "read", Map.of("readerId", uid));
        return Result.success(page);
    }

    // 查询会话列表（忽略路径 userId，以 token 为准）
    @GetMapping("/list/{userId}")
    public Result<List<ConversationVO>> findConversationList(@PathVariable Integer userId) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        return Result.success(messageService.findConversationList(uid));
    }

    // 查询未读消息数量（忽略路径 userId，以 token 为准）
    @GetMapping("/unread/{userId}")
    public Result<Integer> countUnread(@PathVariable Integer userId) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        return Result.success(messageService.countUnread(uid));
    }
}
