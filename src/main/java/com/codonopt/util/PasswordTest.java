package com.codonopt.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码验证测试工具
 */
public class PasswordTest {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 数据库中的密码哈希
        String dbHash = "$2a$10$5DF/dT4W6VjPzW8kZXOqeOCFHt9SgGWdPhWYfNNCgeIhP8m3xqjLi";

        // 测试不同密码
        String[] passwords = {
            "admin123",
            "Admin@123",
            "admin@2024",
            "Test@123",
            "admin"
        };

        System.out.println("=== 密码验证测试 ===");
        System.out.println("数据库哈希: " + dbHash);
        System.out.println();

        for (String pwd : passwords) {
            boolean matches = encoder.matches(pwd, dbHash);
            System.out.println("密码: [" + pwd + "] -> 匹配: " + matches);
        }

        System.out.println();
        System.out.println("=== 生成新的密码哈希 ===");

        // 为新密码生成哈希
        String newPassword = "admin123";
        String newHash = encoder.encode(newPassword);
        System.out.println("密码: " + newPassword);
        System.out.println("哈希: " + newHash);

        // 验证新哈希
        boolean verify = encoder.matches(newPassword, newHash);
        System.out.println("验证: " + verify);
    }
}
