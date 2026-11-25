package com.example.campusmarket.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 生产环境启动自检：密钥、数据库密码仍在用默认值，或 Swagger 仍然开放时直接拒绝启动，
 * 避免带着开发默认配置上线。
 */
@Component
@Profile("prod")
public class ProductionSecurityCheck {

    private static final String DEFAULT_JWT_SECRET =
            "campus-market-jwt-secret-key-2024-please-change-in-production-env";
    private static final String DEFAULT_DB_PASSWORD = "luolei_2005";

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${springdoc.api-docs.enabled:false}")
    private boolean apiDocsEnabled;

    @PostConstruct
    public void check() {
        List<String> problems = new ArrayList<>();
        if (jwtSecret == null || jwtSecret.isBlank()
                || DEFAULT_JWT_SECRET.equals(jwtSecret)
                || jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            problems.add("JWT_SECRET 未通过环境变量注入，或长度不足 32 字节");
        }
        if (dbPassword == null || dbPassword.isBlank() || DEFAULT_DB_PASSWORD.equals(dbPassword)) {
            problems.add("DB_PASSWORD 仍在用默认值");
        }
        if (apiDocsEnabled) {
            problems.add("Swagger 未关闭（springdoc.api-docs.enabled 应为 false）");
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException("生产环境安全配置检查未通过：" + String.join("；", problems));
        }
    }
}
