package com.codonopt.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 生成正确的密码哈希
 */
public class GenerateCorrectHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String password = "admin123";
        String hash = encoder.encode(password);

        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        System.out.println();

        // 验证生成的哈希
        boolean matches = encoder.matches(password, hash);
        System.out.println("Verification: " + matches);

        System.out.println();
        System.out.println("SQL Update:");
        System.out.println("UPDATE users SET password_hash = '" + hash + "' WHERE email = 'admin@codonopt.com';");
    }
}
