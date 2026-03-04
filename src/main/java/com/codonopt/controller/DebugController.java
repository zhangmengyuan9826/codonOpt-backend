package com.codonopt.controller;

import com.codonopt.entity.Task;
import com.codonopt.entity.User;
import com.codonopt.enums.TaskStatus;
import com.codonopt.repository.TaskRepository;
import com.codonopt.repository.UserRepository;
import com.codonopt.service.task.CodonTaskScheduler;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
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
    private final TaskRepository taskRepository;
    private final CodonTaskScheduler taskScheduler;

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

    @GetMapping("/trigger-scheduler")
    @Operation(summary = "手动触发调度器", description = "手动触发任务调度器，处理下一个排队任务")
    public Map<String, Object> triggerScheduler() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 获取当前排队任务数量
            List<Task> queuedTasks = taskRepository.findByStatusOrderBySubmittedAtAsc(TaskStatus.QUEUED);
            List<Task> runningTasks = taskRepository.findByStatus(TaskStatus.RUNNING);

            result.put("queuedTaskCount", queuedTasks.size());
            result.put("runningTaskCount", runningTasks.size());

            if (!queuedTasks.isEmpty()) {
                result.put("nextTaskId", queuedTasks.get(0).getTaskId());
                result.put("nextTaskName", queuedTasks.get(0).getTaskName());
            }

            if (!runningTasks.isEmpty()) {
                result.put("runningTaskIds", runningTasks.stream()
                        .map(Task::getTaskId)
                        .toArray());
            }

            // 手动触发调度器
            log.info("手动触发任务调度器");
            taskScheduler.processNextTask();

            result.put("success", true);
            result.put("message", "调度器已触发");

        } catch (Exception e) {
            log.error("触发调度器失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    @GetMapping("/task-status")
    @Operation(summary = "查看任务状态", description = "查看所有任务的状态统计")
    public Map<String, Object> getTaskStatus() {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Task> queuedTasks = taskRepository.findByStatusOrderBySubmittedAtAsc(TaskStatus.QUEUED);
            List<Task> runningTasks = taskRepository.findByStatus(TaskStatus.RUNNING);
            List<Task> completedTasks = taskRepository.findByStatus(TaskStatus.COMPLETED);
            List<Task> failedTasks = taskRepository.findByStatus(TaskStatus.FAILED);

            result.put("QUEUED", queuedTasks.size());
            result.put("RUNNING", runningTasks.size());
            result.put("COMPLETED", completedTasks.size());
            result.put("FAILED", failedTasks.size());

            // 显示排队中的任务
            if (!queuedTasks.isEmpty()) {
                result.put("queuedTaskList", queuedTasks.stream()
                        .map(t -> {
                            Map<String, Object> taskInfo = new HashMap<>();
                            taskInfo.put("taskId", t.getTaskId());
                            taskInfo.put("taskName", t.getTaskName());
                            taskInfo.put("submittedAt", t.getSubmittedAt().toString());
                            taskInfo.put("queuePosition", t.getQueuePosition());
                            return taskInfo;
                        })
                        .toArray());
            }

            // 显示运行中的任务
            if (!runningTasks.isEmpty()) {
                result.put("runningTaskList", runningTasks.stream()
                        .map(t -> {
                            Map<String, Object> taskInfo = new HashMap<>();
                            taskInfo.put("taskId", t.getTaskId());
                            taskInfo.put("taskName", t.getTaskName());
                            taskInfo.put("startedAt", t.getStartedAt() != null ? t.getStartedAt().toString() : "N/A");
                            taskInfo.put("processPid", t.getProcessPid() != null ? t.getProcessPid() : 0);
                            return taskInfo;
                        })
                        .toArray());
            }

            result.put("success", true);

        } catch (Exception e) {
            log.error("获取任务状态失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }
}
