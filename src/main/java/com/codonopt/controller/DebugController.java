package com.codonopt.controller;

import com.codonopt.entity.User;
import com.codonopt.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 调试控制器 - 用于密码验证调试
 */
@Slf4j
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/test-password")
    @Operation(summary = "测试密码", description = "测试密码验证")
    public Map<String, Object> testPassword(@RequestParam String email, @RequestParam String password) {
        Map<String, Object> result = new HashMap<>();

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            result.put("error", "用户不存在");
            return result;
        }

        result.put("email", email);
        result.put("inputPassword", password);
        result.put("inputPasswordLength", password.length());
        result.put("storedHash", user.getPasswordHash());
        result.put("isActive", user.getIsActive());
        result.put("isVerified", user.getIsVerified());

        // 测试密码匹配
        boolean matches = passwordEncoder.matches(password, user.getPasswordHash());
        result.put("passwordMatches", matches);

        // 详细密码字符信息
        StringBuilder charInfo = new StringBuilder();
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            charInfo.append(String.format("[%d]='%s'(ASCII:%d) ", i, c, (int)c));
        }
        result.put("charDetails", charInfo.toString());

        // 尝试其他常见密码
        String[] testPasswords = {"admin123", "Admin@123", "admin", "123456"};
        Map<String, Boolean> otherMatches = new HashMap<>();
        for (String testPwd : testPasswords) {
            otherMatches.put(testPwd, passwordEncoder.matches(testPwd, user.getPasswordHash()));
        }
        result.put("otherPasswordsTest", otherMatches);

        return result;
    }

    @GetMapping("/generate-hash")
    @Operation(summary = "生成密码哈希", description = "为指定密码生成BCrypt哈希")
    public Map<String, Object> generateHash(@RequestParam String password) {
        Map<String, Object> result = new HashMap<>();

        String hash = passwordEncoder.encode(password);
        result.put("password", password);
        result.put("hash", hash);

        // 验证生成的哈希
        boolean matches = passwordEncoder.matches(password, hash);
        result.put("verified", matches);

        return result;
    }

    @PostMapping("/update-password")
    @Operation(summary = "更新用户密码", description = "为用户更新密码")
    public Map<String, Object> updatePassword(@RequestParam String email, @RequestParam String newPassword) {
        Map<String, Object> result = new HashMap<>();

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            result.put("error", "用户不存在");
            return result;
        }

        String newHash = passwordEncoder.encode(newPassword);
        user.setPasswordHash(newHash);
        userRepository.save(user);

        result.put("success", true);
        result.put("email", email);
        result.put("newPassword", newPassword);
        result.put("newHash", newHash);

        // 验证新密码
        boolean matches = passwordEncoder.matches(newPassword, newHash);
        result.put("verified", matches);

        return result;
    }
}
