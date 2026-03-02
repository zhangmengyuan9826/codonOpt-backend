package com.codonopt.service.auth;

import com.codonopt.constants.EmailConstants;
import com.codonopt.constants.SecurityConstants;
import com.codonopt.dto.request.LoginRequest;
import com.codonopt.dto.request.RefreshTokenRequest;
import com.codonopt.dto.request.RegisterRequest;
import com.codonopt.dto.request.VerifyEmailRequest;
import com.codonopt.dto.response.AuthResponse;
import com.codonopt.entity.User;
import com.codonopt.enums.UserRole;
import com.codonopt.exception.BusinessException;
import com.codonopt.repository.UserRepository;
import com.codonopt.service.mail.MailService;
import com.codonopt.service.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final MailService mailService;

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 认证响应（包含Token）
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("该邮箱已被注册");
        }

        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("该用户名已被使用");
        }

        // 创建新用户
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isVerified(false)
                .isActive(true)
                .role(UserRole.USER)
                .build();

        user = userRepository.save(user);

        // 生成并发送验证码
        sendVerificationCode(user.getEmail());

        log.info("New user registered: {}", user.getEmail());

        // 返回认证信息（但此时邮箱未验证，可以限制某些功能）
        String accessToken = tokenProvider.generateToken(user.getId(), user.getRole());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId());

        return AuthResponse.fromUser(user, accessToken, refreshToken);
    }

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 认证响应
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // 查找用户
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("邮箱或密码错误"));

        // 检查用户是否被禁用
        if (!user.getIsActive()) {
            throw new BusinessException("该账户已被禁用，请联系管理员");
        }

        // 验证密码
        System.out.println("Provided password: " + request.getPassword());
        System.out.println("Stored password hash: " + user.getPasswordHash());
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("邮箱或密码错误");
        }

        // 更新最后登录时间
        userRepository.updateLastLoginAt(user.getId(), LocalDateTime.now());

        log.info("User logged in: {} with role: {}", user.getEmail(), user.getRole());

        // 生成Token（包含用户角色）
        String accessToken = tokenProvider.generateToken(user.getId(), user.getRole());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId());

        return AuthResponse.fromUser(user, accessToken, refreshToken);
    }

    /**
     * 发送验证码
     *
     * @param email 邮箱地址
     */
    @Transactional
    public void sendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        // 生成6位数字验证码
        String verificationCode = generateVerificationCode();

        // 设置验证码和过期时间
        LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(SecurityConstants.EMAIL_CODE_EXPIRATION / 1000);
        userRepository.setVerificationCode(email, verificationCode, expiryTime);

        // 发送验证邮件
        String content = String.format(EmailConstants.VERIFICATION_EMAIL_TEMPLATE,
                user.getUsername(), verificationCode);
        mailService.sendEmail(email, EmailConstants.VERIFICATION_SUBJECT, content);

        log.info("Verification code sent to: {}", email);
    }

    /**
     * 验证邮箱
     *
     * @param request 验证请求
     * @return 是否验证成功
     */
    @Transactional
    public boolean verifyEmail(VerifyEmailRequest request) {
        int updated = userRepository.verifyUser(
                request.getEmail(),
                request.getVerificationCode(),
                LocalDateTime.now()
        );

        if (updated > 0) {
            log.info("Email verified successfully: {}", request.getEmail());
            return true;
        } else {
            throw new BusinessException("验证码无效或已过期");
        }
    }

    /**
     * 刷新Token
     *
     * @param request 刷新Token请求
     * @return 新的认证响应
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // 验证刷新令牌
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BusinessException("刷新令牌无效或已过期");
        }

        // 检查是否为刷新令牌类型
        if (!tokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException("令牌类型错误");
        }

        // 获取用户ID
        Long userId = tokenProvider.getUserIdFromToken(refreshToken);

        // 查找用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        // 检查用户是否被禁用
        if (!user.getIsActive()) {
            throw new BusinessException("该账户已被禁用");
        }

        // 生成新的Token（包含用户角色）
        String newAccessToken = tokenProvider.generateToken(user.getId(), user.getRole());
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getId());

        log.info("Token refreshed for user: {} with role: {}", user.getEmail(), user.getRole());

        return AuthResponse.fromUser(user, newAccessToken, newRefreshToken);
    }

    /**
     * 登出（客户端删除Token即可，服务端可选实现Token黑名单）
     *
     * @param userId 用户ID
     */
    public void logout(Long userId) {
        // 这里可以实现Token黑名单功能
        // 目前客户端删除Token即可
        log.info("User logged out: {}", userId);
    }

    /**
     * 生成6位数字验证码
     *
     * @return 验证码
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
