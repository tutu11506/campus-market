package com.example.campusmarket.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 消息实时推送中心（单实例内存实现）。
 * 每个登录用户可保持多个 SSE 连接（多标签页/多设备），推送时逐个下发；
 * 20 秒心跳注释帧防止代理或浏览器主动断开空闲连接。
 */
@Component
public class RealtimeHub {

    private static final Logger log = LoggerFactory.getLogger(RealtimeHub.class);

    private final Map<Integer, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /** 建立连接：0 表示不设超时，断线由前后端各自感知 */
    public SseEmitter register(Integer userId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> {
            remove(userId, emitter);
            emitter.complete();
        });
        emitter.onError(e -> remove(userId, emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (Exception e) {
            remove(userId, emitter);
        }
        log.debug("SSE 连接建立 userId={}", userId);
        return emitter;
    }

    /** 向某个用户的全部连接推送事件 */
    public void publish(Integer userId, String event, Object payload) {
        if (userId == null) {
            return;
        }
        List<SseEmitter> list = emitters.get(userId);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(event).data(payload));
            } catch (Exception e) {
                remove(userId, emitter);
            }
        }
    }

    @Scheduled(fixedDelay = 20000, initialDelay = 20000)
    public void heartbeat() {
        emitters.forEach((userId, list) -> {
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    remove(userId, emitter);
                }
            }
        });
    }

    private void remove(Integer userId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(userId);
        if (list != null) {
            list.remove(emitter);
        }
    }
}
