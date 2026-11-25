package com.example.campusmarket;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

// 项目使用无状态 JWT 自行管理认证，不需要 Spring Boot 自动配置的默认内存用户（避免生成默认密码告警）
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@MapperScan("com.example.campusmarket.mapper")
@EnableScheduling
public class CampusMarketApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusMarketApplication.class, args);
    }
}