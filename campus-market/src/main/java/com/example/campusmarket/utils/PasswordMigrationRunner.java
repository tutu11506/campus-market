package com.example.campusmarket.utils;

import com.example.campusmarket.entity.User;
import com.example.campusmarket.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 密码迁移工具
 * 在应用启动时自动检测数据库中是否还有明文密码，
 * 如果有则用 BCrypt 加密后更新，确保所有密码都是加密存储的。
 *
 * 判断明文的依据：BCrypt 哈希值都以 "$2" 开头（如 $2a$、$2b$、$2y$），
 * 明文密码不会以 "$2" 开头。
 */
@Component
public class PasswordMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PasswordMigrationRunner.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("开始检查用户密码加密情况...");

        List<User> users = userMapper.findAll();
        int migratedCount = 0;

        for (User user : users) {
            String password = user.getPassword();
            // BCrypt 哈希值以 "$2" 开头
            if (password != null && !password.startsWith("$2")) {
                // 明文密码，需要加密
                String encodedPassword = passwordEncoder.encode(password);
                user.setPassword(encodedPassword);
                // 更新数据库
                userMapper.updatePassword(user.getId(), encodedPassword);
                migratedCount++;
                log.info("已加密用户 [{}] 的密码", user.getUsername());
            }
        }

        if (migratedCount > 0) {
            log.info("密码迁移完成，共加密了 {} 个用户的密码", migratedCount);
        } else {
            log.info("所有用户密码均已加密，无需迁移");
        }
    }
}
