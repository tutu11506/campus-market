package com.example.campusmarket.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从 Spring Security 上下文中获取当前登录用户信息。
 * principal 存放的是 userId 字符串（由 JwtAuthenticationFilter 写入）。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户 ID；未登录或匿名访问返回 null
     */
    public static Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        // 匿名用户的 principal 是字符串 "anonymousUser"
        if (principal == null || "anonymousUser".equals(principal)) {
            return null;
        }
        try {
            return Integer.valueOf(principal.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 当前登录用户是否为管理员
     */
    public static boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "admin".equals(a.getAuthority()));
    }
}
