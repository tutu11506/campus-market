package com.example.campusmarket.common;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败限流：同一来源 + 账号在窗口期内失败次数超限后暂时拒绝，防止密码爆破。
 * 单实例内存实现，重启即清零；多实例部署时需换成 Redis 计数。
 */
@Component
public class LoginAttemptLimiter {

    /** 窗口期内允许的最大失败次数 */
    private static final int MAX_FAILURES = 5;
    /** 统计窗口：5 分钟 */
    private static final long WINDOW_MS = 5 * 60 * 1000L;
    /** 超过该规模时顺带清理过期条目，避免长期运行内存膨胀 */
    private static final int CLEANUP_THRESHOLD = 5000;

    private final Map<String, Deque<Long>> failures = new ConcurrentHashMap<>();

    /** 是否已被限流 */
    public boolean isBlocked(String key) {
        Deque<Long> times = failures.get(key);
        if (times == null) {
            return false;
        }
        synchronized (times) {
            prune(times);
            return times.size() >= MAX_FAILURES;
        }
    }

    /** 记录一次失败 */
    public void recordFailure(String key) {
        if (failures.size() > CLEANUP_THRESHOLD) {
            cleanup();
        }
        Deque<Long> times = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (times) {
            prune(times);
            times.addLast(System.currentTimeMillis());
        }
    }

    /** 登录成功后清除该来源的失败记录 */
    public void clear(String key) {
        failures.remove(key);
    }

    private void prune(Deque<Long> times) {
        long deadline = System.currentTimeMillis() - WINDOW_MS;
        while (!times.isEmpty() && times.peekFirst() < deadline) {
            times.pollFirst();
        }
    }

    private void cleanup() {
        failures.entrySet().removeIf(entry -> {
            Deque<Long> times = entry.getValue();
            synchronized (times) {
                prune(times);
                return times.isEmpty();
            }
        });
    }
}
