package com.example.campusmarket.common;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 上传限流：窗口内计数、按用户隔离、窗口过后恢复。
 */
class UploadRateLimiterTest {

    private UploadRateLimiter limiter(int maxPerWindow, long windowSeconds) {
        UploadRateLimiter limiter = new UploadRateLimiter();
        ReflectionTestUtils.setField(limiter, "maxPerWindow", maxPerWindow);
        ReflectionTestUtils.setField(limiter, "windowSeconds", windowSeconds);
        return limiter;
    }

    @Test
    void allowsUpToLimitThenRejects() {
        UploadRateLimiter limiter = limiter(3, 60);
        assertTrue(limiter.tryAcquire(1));
        assertTrue(limiter.tryAcquire(1));
        assertTrue(limiter.tryAcquire(1));
        assertFalse(limiter.tryAcquire(1), "超出阈值应拒绝");
        assertEquals(0, limiter.remaining(1));
    }

    @Test
    void quotaIsIsolatedPerUser() {
        UploadRateLimiter limiter = limiter(1, 60);
        assertTrue(limiter.tryAcquire(1));
        assertFalse(limiter.tryAcquire(1));
        assertTrue(limiter.tryAcquire(2), "其他用户不受影响");
    }

    @Test
    void quotaRecoversAfterWindow() throws Exception {
        UploadRateLimiter limiter = limiter(1, 1);
        assertTrue(limiter.tryAcquire(3));
        assertFalse(limiter.tryAcquire(3));
        Thread.sleep(1100);
        assertTrue(limiter.tryAcquire(3), "窗口过期后应恢复配额");
    }
}
