package com.codonopt.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码生成工具类
 * 用于生成BCrypt加密的密码哈希
 */
public class PasswordGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 生成几个常用密码的哈希
        String[] passwords = {
            "admin123",
            "Admin@123",
            "admin@2024",
            "CodonOpt@2024",
            "Test@123"
        };

        System.out.println("=== BCrypt Password Hash Generator ===\n");
        System.out.println("以下密码哈希可以直接用于数据库更新:\n");

        for (String password : passwords) {
            String hash = encoder.encode(password);
            System.out.println("密码: " + password);
            System.out.println("哈希: " + hash);
            System.out.println();
        }

        System.out.println("=== SQL 更新语句 ===\n");
        System.out.println("-- 选择一个密码后，使用以下SQL更新管理员密码:");
        System.out.println("UPDATE users SET password_hash = '<YOUR_HASH_HERE>' WHERE email = 'admin@codonopt.com';");
    }
}
