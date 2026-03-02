package com.codonopt.util;

import com.codonopt.entity.User;
import com.codonopt.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 认证调试工具 - 检查用户数据和密码验证
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthDebugTool implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("========================================");
        log.info("开始检查用户认证数据...");
        log.info("========================================");

        // 检查所有用户
        Iterable<User> allUsers = userRepository.findAll();
        log.info("数据库中的用户总数: {}",
            java.util.stream.StreamSupport.stream(allUsers.spliterator(), false).count());

        allUsers.forEach(user -> {
            log.info("----------------------------------------");
            log.info("用户ID: {}", user.getId());
            log.info("用户名: {}", user.getUsername());
            log.info("邮箱: {}", user.getEmail());
            log.info("密码哈希: {}", user.getPasswordHash());
            log.info("是否激活: {}", user.getIsActive());
            log.info("是否验证: {}", user.getIsVerified());
            log.info("角色: {}", user.getRole());

            // 测试密码匹配
            String[] testPasswords = {"admin123", "Admin@123", "admin", "123456"};
            for (String testPwd : testPasswords) {
                boolean matches = passwordEncoder.matches(testPwd, user.getPasswordHash());
                log.info("  密码 '{}' 匹配: {}", testPwd, matches);
            }
        });

        log.info("========================================");
        log.info("检查管理员用户是否存在...");
        log.info("========================================");

        // 检查admin用户
        java.util.Optional<User> adminUser = userRepository.findByEmail("admin@codonopt.com");
        if (adminUser.isPresent()) {
            log.info("✓ 管理员用户存在");
            User admin = adminUser.get();
            log.info("  邮箱: {}", admin.getEmail());
            log.info("  激活状态: {}", admin.getIsActive());

            // 生成新的密码哈希供参考
            String newHash = passwordEncoder.encode("Admin@123");
            log.info("  新生成的密码哈希 (Admin@123): {}", newHash);
        } else {
            log.error("✗ 管理员用户不存在! 需要创建管理员用户");

            // 创建管理员用户
            User newAdmin = User.builder()
                .username("admin")
                .email("admin@codonopt.com")
                .passwordHash(passwordEncoder.encode("Admin@123"))
                .isVerified(true)
                .isActive(true)
                .role(com.codonopt.enums.UserRole.ADMIN)
                .build();

            User saved = userRepository.save(newAdmin);
            log.info("✓ 已创建新的管理员用户");
            log.info("  ID: {}", saved.getId());
            log.info("  邮箱: {}", saved.getEmail());
            log.info("  登录密码: Admin@123");
        }

        log.info("========================================");
        log.info("调试检查完成");
        log.info("========================================");
    }
}
