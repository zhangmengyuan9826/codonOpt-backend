package com.codonopt.controller;

import com.codonopt.dto.response.ApiResponse;
import com.codonopt.dto.response.PageResponse;
import com.codonopt.dto.response.TaskResponse;
import com.codonopt.entity.AccessLog;
import com.codonopt.entity.Task;
import com.codonopt.entity.User;
import com.codonopt.enums.UserRole;
import com.codonopt.repository.TaskRepository;
import com.codonopt.repository.UserRepository;
import com.codonopt.service.security.BlacklistService;
import com.codonopt.service.security.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理员控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "管理员功能", description = "系统管理接口（需要管理员权限）")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final TaskRepository taskRepository;
    private final com.codonopt.repository.AccessLogRepository accessLogRepository;
    private final BlacklistService blacklistService;
    private final RateLimitService rateLimitService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 获取系统统计
     */
    @GetMapping("/stats/overview")
    @Operation(summary = "获取系统统计", description = "获取系统运行统计信息")
    public ResponseEntity<ApiResponse<SystemStats>> getSystemStats() {
        SystemStats stats = new SystemStats();

        // 用户统计
        stats.totalUsers = userRepository.count();
        stats.activeUsers = userRepository.countByIsActiveTrue();
        stats.adminUsers = userRepository.countByRole(UserRole.ADMIN);
        stats.verifiedUsers = userRepository.countByIsVerifiedTrue();

        // 任务统计
        stats.totalTasks = taskRepository.count();
        stats.queuedTasks = taskRepository.findByStatusOrderBySubmittedAtAsc(
                com.codonopt.enums.TaskStatus.QUEUED).size();
        stats.runningTasks = taskRepository.findByStatusOrderBySubmittedAtAsc(
                com.codonopt.enums.TaskStatus.RUNNING).size();
        stats.completedTasks = taskRepository.findByStatusOrderBySubmittedAtAsc(
                com.codonopt.enums.TaskStatus.COMPLETED).size();
        stats.failedTasks = taskRepository.findByStatusOrderBySubmittedAtAsc(
                com.codonopt.enums.TaskStatus.FAILED).size();

        // 今日请求数
        stats.todayRequests = accessLogRepository.countByRequestTimeAfter(LocalDateTime.now().toLocalDate().atStartOfDay());

        // 黑名单IP数量
        stats.blacklistedIps = blacklistService.getAllBlacklistedIps().size();

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ========== 用户管理 ==========

    /**
     * 获取所有用户列表
     */
    @GetMapping("/users")
    @Operation(summary = "获取用户列表", description = "获取系统中所有用户（管理员）")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {

        List<User> users;
        if (search != null && !search.isEmpty()) {
            // 搜索用户名或邮箱
            users = userRepository.findByUsernameContainingOrEmailContaining(search, search);
        } else {
            users = userRepository.findAllByOrderByCreatedAtDesc();
        }

        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/users/{userId}")
    @Operation(summary = "获取用户详情", description = "获取指定用户的详细信息")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * 更新用户角色
     */
    @PutMapping("/users/{userId}/role")
    @Operation(summary = "更新用户角色", description = "修改指定用户的角色（USER/ADMIN）")
    public ResponseEntity<ApiResponse<Void>> updateUserRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        String roleStr = request.get("role");
        if (roleStr == null || roleStr.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("角色不能为空"));
        }

        try {
            UserRole role = UserRole.valueOf(roleStr.toUpperCase());
            user.setRole(role);
            userRepository.save(user);

            log.info("Updated user {} role to {} by admin", user.getUsername(), role);
            return ResponseEntity.ok(ApiResponse.success("角色已更新"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("无效的角色值"));
        }
    }

    /**
     * 激活/禁用用户
     */
    @PutMapping("/users/{userId}/active")
    @Operation(summary = "激活/禁用用户", description = "启用或禁用指定用户账户")
    public ResponseEntity<ApiResponse<Void>> updateUserActiveStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Boolean isActive = request.get("isActive");
        if (isActive == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("状态不能为空"));
        }

        user.setIsActive(isActive);
        userRepository.save(user);

        String action = isActive ? "激活" : "禁用";
        log.info("{} user {} by admin", action, user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("用户状态已更新"));
    }

    /**
     * 重置用户密码
     */
    @PostMapping("/users/{userId}/reset-password")
    @Operation(summary = "重置用户密码", description = "为指定用户重置密码")
    public ResponseEntity<ApiResponse<Map<String, String>>> resetUserPassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 生成随机密码
        String newPassword = generateRandomPassword();

        // 加密密码
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Reset password for user {} by admin", user.getUsername());

        // 返回新密码（实际应用中应该通过邮件发送）
        Map<String, String> result = new HashMap<>();
        result.put("username", user.getUsername());
        result.put("newPassword", newPassword);

        return ResponseEntity.ok(ApiResponse.success("密码已重置", result));
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/users/{userId}")
    @Operation(summary = "删除用户", description = "删除指定用户账户")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 不允许删除自己
        // （当前用户ID需要从SecurityContext获取，这里简化处理）

        userRepository.delete(user);

        log.info("Deleted user {} by admin", user.getUsername());
        return ResponseEntity.ok(ApiResponse.success("用户已删除"));
    }

    /**
     * 获取所有任务（分页）
     */
    @GetMapping("/tasks")
    @Operation(summary = "获取所有任务", description = "查看系统中所有任务（管理员，支持分页）")
    public ResponseEntity<ApiResponse<PageResponse<TaskResponse>>> getAllTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "submittedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        // 创建分页对象
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // 查询分页数据
        Page<Task> taskPage = taskRepository.findAllByOrderBySubmittedAtDesc(pageable);

        // 转换为响应DTO
        Page<TaskResponse> responsePage = taskPage.map(task ->
            TaskResponse.builder()
                .taskId(task.getTaskId())
                    .taskName(task.getTaskName())
                .userName(userRepository.findById(task.getUserId()).map(User::getUsername).orElse("未知用户"))
                .sequenceType(task.getSequenceType().name())
                .targetSpecies(String.valueOf(task.getTargetSpecies()))
                .status(task.getStatus())
                .queuePosition(task.getQueuePosition())
                .submittedAt(task.getSubmittedAt())
                .completedAt(task.getCompletedAt())
                .build()
        );

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(responsePage)));
    }

    /**
     * 获取访问日志
     */
    @GetMapping("/access-logs")
    @Operation(summary = "获取访问日志", description = "查看系统访问日志")
    public ResponseEntity<ApiResponse<List<AccessLog>>> getAccessLogs(
            @RequestParam(defaultValue = "100") int limit) {
        List<AccessLog> logs = accessLogRepository.findRecentAccessLogs()
                .stream()
                .limit(limit)
                .collect(Collectors.toCollection(java.util.ArrayList::new));
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    /**
     * 获取黑名单
     */
    @GetMapping("/blacklist")
    @Operation(summary = "获取黑名单", description = "查看IP黑名单")
    public ResponseEntity<ApiResponse<Set<String>>> getBlacklist() {
        Set<String> blacklist = blacklistService.getAllBlacklistedIps();
        return ResponseEntity.ok(ApiResponse.success(blacklist));
    }

    /**
     * 添加IP到黑名单
     */
    @PostMapping("/blacklist")
    @Operation(summary = "添加IP到黑名单", description = "封禁指定IP地址")
    public ResponseEntity<ApiResponse<Void>> addToBlacklist(@RequestBody BlacklistRequest request) {
        // 默认封禁24小时
        long duration = request.getDurationMs() > 0 ? request.getDurationMs() : 86400000L;
        blacklistService.addToBlacklist(request.getIpAddress(), request.getReason(), duration);
        return ResponseEntity.ok(ApiResponse.success("IP已添加到黑名单"));
    }

    /**
     * 从黑名单移除IP
     */
    @DeleteMapping("/blacklist/{ipAddress}")
    @Operation(summary = "从黑名单移除IP", description = "解除IP封禁")
    public ResponseEntity<ApiResponse<Void>> removeFromBlacklist(@PathVariable String ipAddress) {
        blacklistService.removeFromBlacklist(ipAddress);
        return ResponseEntity.ok(ApiResponse.success("IP已从黑名单移除"));
    }

    /**
     * 清空黑名单
     */
    @DeleteMapping("/blacklist")
    @Operation(summary = "清空黑名单", description = "清空所有IP黑名单")
    public ResponseEntity<ApiResponse<Void>> clearBlacklist() {
        blacklistService.clearBlacklist();
        return ResponseEntity.ok(ApiResponse.success("黑名单已清空"));
    }

    /**
     * 重置用户频率限制
     */
    @PostMapping("/rate-limit/reset/{userId}")
    @Operation(summary = "重置频率限制", description = "重置指定用户的每日提交限制")
    public ResponseEntity<ApiResponse<Void>> resetRateLimit(@PathVariable Long userId) {
        rateLimitService.resetCounter(userId);
        return ResponseEntity.ok(ApiResponse.success("频率限制已重置"));
    }

    /**
     * 黑名单请求DTO
     */
    @Data
    public static class BlacklistRequest {
        private String ipAddress;
        private String reason;
        private Long durationMs;
    }

    /**
     * 系统统计DTO
     */
    @Data
    public static class SystemStats {
        // 用户统计
        private long totalUsers;
        private long activeUsers;
        private long adminUsers;
        private long verifiedUsers;

        // 任务统计
        private long totalTasks;
        private long queuedTasks;
        private long runningTasks;
        private long completedTasks;
        private long failedTasks;

        // 系统统计
        private long todayRequests;
        private int blacklistedIps;
    }

    /**
     * 生成随机密码
     */
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            int index = (int) (Math.random() * chars.length());
            password.append(chars.charAt(index));
        }
        return password.toString();
    }
}
