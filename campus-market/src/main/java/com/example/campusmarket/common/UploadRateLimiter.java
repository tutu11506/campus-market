package com.example.campusmarket.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 上传接口的用户维度限流（单实例内存实现，够用且无额外依赖）。
 * 在滑动窗口内统计每个用户的上传次数，超过阈值则拒绝。
 * 阈值可通过 upload.rate.max-per-window / upload.rate.window-seconds 调整。
 */
@Component
public class UploadRateLimiter {

    @Value("${upload.rate.max-per-window:30}")
    private int maxPerWindow;

    @Value("${upload.rate.window-seconds:600}")
    private long windowSeconds;

    private final Map<Integer, Deque<Long>> hits = new ConcurrentHashMap<>();

    /**
     * 尝试占用一次上传配额
     *
     * @param userId 当前登录用户
     * @return true 允许上传；false 超出频率限制
     */
    public boolean tryAcquire(Integer userId) {
        if (userId == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        long windowMs = windowSeconds * 1000L;
        Deque<Long> queue = hits.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && now - queue.peekFirst() >= windowMs) {
                queue.pollFirst();
            }
            if (queue.size() >= maxPerWindow) {
                return false;
            }
            queue.addLast(now);
            return true;
        }
    }

    /** 剩余配额（用于提示，不参与判断） */
    public int remaining(Integer userId) {
        Deque<Long> queue = hits.get(userId);
        if (queue == null) {
            return maxPerWindow;
        }
        synchronized (queue) {
            return Math.max(0, maxPerWindow - queue.size());
        }
    }
}
