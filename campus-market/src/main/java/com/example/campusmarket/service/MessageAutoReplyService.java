package com.example.campusmarket.service;

import com.example.campusmarket.config.RealtimeHub;
import com.example.campusmarket.entity.Message;
import com.example.campusmarket.mapper.MessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 私信自动回复：买家发出消息后卖家长时间没回，机器人代卖家回一条礼貌提示。
 * 同一个会话只发一条自动回复，避免反复打扰买家。
 */
@Service
public class MessageAutoReplyService {

    private static final Logger log = LoggerFactory.getLogger(MessageAutoReplyService.class);

    /** 卖家超过该秒数未回复即触发 */
    private static final int WAIT_SECONDS = 60;

    /** 只处理最近这段时间内的消息，避免服务启动时给历史会话批量补发 */
    private static final int RECENT_HOURS = 2;

    private static final String AUTO_REPLY_TEXT =
            "同学你好～卖家这会儿可能在上课或忙，暂时没能及时回复。你的消息已经收到，看到后会第一时间答复你，感谢耐心等待呀！";

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private RealtimeHub realtimeHub;

    @Scheduled(initialDelay = 30_000, fixedDelay = 30_000)
    public void replyUnanswered() {
        List<Message> pending;
        try {
            pending = messageMapper.findPendingAutoReply(WAIT_SECONDS, RECENT_HOURS);
        } catch (Exception e) {
            log.warn("查询待自动回复消息失败", e);
            return;
        }
        for (Message origin : pending) {
            Message reply = new Message();
            // 以卖家身份回复买家，前端会带“自动回复”标识
            reply.setSenderId(origin.getReceiverId());
            reply.setReceiverId(origin.getSenderId());
            reply.setContent(AUTO_REPLY_TEXT);
            reply.setMsgType("text");
            reply.setAutoReply(1);
            try {
                messageMapper.insert(reply);
            } catch (Exception e) {
                log.warn("写入自动回复失败 originId={}", origin.getId(), e);
                continue;
            }
            realtimeHub.publish(reply.getReceiverId(), "message", reply);
            realtimeHub.publish(reply.getReceiverId(), "unread",
                    messageMapper.countUnread(reply.getReceiverId()));
            log.info("已自动回复: originId={} buyer={} seller={}",
                    origin.getId(), origin.getSenderId(), origin.getReceiverId());
        }
    }
}
