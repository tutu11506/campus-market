package com.example.campusmarket.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * JWT 滑动续期：临期下发新 token，新鲜 token 不重复下发，过期 token 不续期。
 */
class JwtAuthenticationFilterTest {

    private static final String SECRET = "0123456789012345678901234567890123456789";

    // 过滤器会把认证写入 SecurityContext（ThreadLocal），测试间必须清理，否则后续用例走不到续期分支
    @BeforeEach
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private JwtUtil jwtUtil(long expirationMillis) {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret", SECRET);
        ReflectionTestUtils.setField(util, "expiration", expirationMillis);
        return util;
    }

    private MockHttpServletResponse call(JwtUtil util, String token) throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "jwtUtil", util);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/goods/list");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void renewsTokenWhenCloseToExpiry() throws Exception {
        JwtUtil util = jwtUtil(3000);
        String token = util.generateToken(7, "user");
        // 让剩余有效期降到阈值的一半（JWT 的 exp 精确到秒，按实际剩余时间等待更稳定）
        long remaining = util.parseToken(token).getExpiration().getTime() - System.currentTimeMillis();
        long target = util.getRenewThreshold() / 2;
        if (remaining > target) {
            Thread.sleep(remaining - target);
        }

        String refreshed = call(util, token).getHeader("X-Refresh-Token");

        assertNotNull(refreshed, "临期应下发新 token");
        assertNotEquals(token, refreshed);
        assertEquals(Integer.valueOf(7), util.getUserId(refreshed));
    }

    @Test
    void doesNotRenewFreshToken() throws Exception {
        JwtUtil util = jwtUtil(86400000L);
        String token = util.generateToken(8, "user");

        assertNull(call(util, token).getHeader("X-Refresh-Token"), "有效期充足时不应下发新 token");
    }

    @Test
    void ignoresExpiredToken() throws Exception {
        JwtUtil util = jwtUtil(1);
        String token = util.generateToken(9, "user");
        Thread.sleep(100);

        assertNull(call(util, token).getHeader("X-Refresh-Token"), "已过期不应续期");
    }
}
