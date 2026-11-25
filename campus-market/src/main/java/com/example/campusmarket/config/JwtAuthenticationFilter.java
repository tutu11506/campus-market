package com.example.campusmarket.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器：读取 Authorization: Bearer <token>，
 * 校验通过后把 userId 写入 SecurityContext，供后续鉴权与接口取当前用户使用。
 * 校验失败时不写入上下文，交由 Spring Security 的授权规则返回 401/403。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String token = (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
        // SSE（EventSource）无法自定义请求头，退化为从 token 查询参数取
        if (token == null || token.isEmpty()) {
            token = request.getParameter("token");
        }
        if (token != null && !token.isEmpty()) {
            Claims claims = jwtUtil.parseToken(token);
            if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                Integer userId = Integer.valueOf(claims.getSubject());
                String role = claims.get("role", String.class);
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role != null ? role : "user");
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                String.valueOf(userId), null, Collections.singletonList(authority));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 滑动续期：临期时下发新 token，前端读取该响应头后替换本地 token
                long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
                if (remaining > 0 && remaining < jwtUtil.getRenewThreshold()) {
                    response.setHeader("X-Refresh-Token", jwtUtil.generateToken(userId, role));
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
