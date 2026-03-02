package com.codonopt.controller;

import com.codonopt.dto.request.LoginRequest;
import com.codonopt.dto.request.RefreshTokenRequest;
import com.codonopt.dto.request.RegisterRequest;
import com.codonopt.dto.request.VerifyEmailRequest;
import com.codonopt.dto.response.ApiResponse;
import com.codonopt.dto.response.AuthResponse;
import com.codonopt.service.auth.AuthServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户注册、登录、邮箱验证等接口")
public class AuthController {

    private final AuthServiceImpl authService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户，需要邮箱和密码")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("注册成功，请查收验证邮件", response));
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用邮箱和密码登录")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("登录成功", response));
    }

    /**
     * 发送验证码
     */
    @PostMapping("/send-verification")
    @Operation(summary = "发送验证码", description = "向用户邮箱发送验证码")
    public ResponseEntity<ApiResponse<Void>> sendVerification(@RequestParam String email) {
        authService.sendVerificationCode(email);
        return ResponseEntity.ok(ApiResponse.success("验证码已发送"));
    }

    /**
     * 验证邮箱
     */
    @PostMapping("/verify-email")
    @Operation(summary = "验证邮箱", description = "使用验证码验证邮箱")
    public ResponseEntity<ApiResponse<Boolean>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        boolean result = authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success("邮箱验证成功", result));
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新Token", description = "使用刷新令牌获取新的访问令牌")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token刷新成功", response));
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出（客户端删除Token即可）")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // 从SecurityContext获取用户ID
        Object principal = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();

        if (principal instanceof Long) {
            Long userId = (Long) principal;
            authService.logout(userId);
        }

        return ResponseEntity.ok(ApiResponse.success("登出成功"));
    }
}
