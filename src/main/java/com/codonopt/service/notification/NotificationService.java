package com.codonopt.service.notification;

import com.codonopt.constants.EmailConstants;
import com.codonopt.entity.Task;
import com.codonopt.entity.User;
import com.codonopt.repository.TaskRepository;
import com.codonopt.repository.UserRepository;
import com.codonopt.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务通知服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final MailService mailService;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取邮件接收列表（测试模式返回测试邮箱列表）
     */
    private List<String> getRecipients(String originalEmail) {
        if (EmailConstants.TEST_MODE) {
            log.info("TEST MODE: Redirecting email from {} to test emails: {}", originalEmail, EmailConstants.TEST_EMAILS);
            return EmailConstants.TEST_EMAILS;
        }
        return java.util.Collections.singletonList(originalEmail);
    }

    /**
     * 发送任务提交成功通知
     *
     * @param task 任务对象
     */
    @Async
    public void notifyTaskSubmitted(Task task) {
        try {
            User user = userRepository.findById(task.getUserId())
                    .orElse(null);

            if (user == null) {
                log.warn("User not found for task: {}", task.getTaskId());
                return;
            }

            // 计算队列位置
            int queuePosition = calculateQueuePosition(task.getTaskId());

            Map<String, Object> variables = new HashMap<>();
            variables.put("username", user.getUsername());
            variables.put("taskId", task.getTaskId());
            variables.put("taskName", task.getTaskName() != null ? task.getTaskName() : "未命名任务");
            variables.put("submittedAt", task.getSubmittedAt().format(formatter));
            variables.put("sequenceType", task.getSequenceType().name());
            variables.put("targetSpecies", task.getTargetSpecies().getDisplayName());
            variables.put("queuePosition", queuePosition);
            variables.put("taskUrl", EmailConstants.FRONTEND_URL + "/tasks/" + task.getTaskId());

            // 获取收件人列表（测试模式）
            List<String> recipients = getRecipients(user.getEmail());

            for (String recipient : recipients) {
                mailService.sendTemplateEmail(
                        recipient,
                        EmailConstants.TASK_SUBMITTED_SUBJECT,
                        EmailConstants.TASK_SUBMITTED_TEMPLATE,
                        variables
                );
            }

            log.info("Task submitted notification sent for task: {}", task.getTaskId());
        } catch (Exception e) {
            log.error("Failed to send task submitted notification for task: {}", task.getTaskId(), e);
        }
    }

    /**
     * 发送任务完成通知
     *
     * @param task    任务对象
     * @param zipFile 结果ZIP文件
     */
    @Async
    public void notifyTaskCompletion(Task task, File zipFile) {
        try {
            User user = userRepository.findById(task.getUserId())
                    .orElse(null);

            if (user == null) {
                log.warn("User not found for task: {}", task.getTaskId());
                return;
            }

            // 解析结果摘要
            Map<String, Object> summary = parseResultSummary(task.getResultSummary());

            Map<String, Object> variables = new HashMap<>();
            variables.put("username", user.getUsername());
            variables.put("taskId", task.getTaskId());
            variables.put("taskName", task.getTaskName() != null ? task.getTaskName() : "未命名任务");
            variables.put("completedAt", task.getCompletedAt() != null ? task.getCompletedAt().format(formatter) : "N/A");
            variables.put("sequenceType", task.getSequenceType().name());
            variables.put("targetSpecies", task.getTargetSpecies().getDisplayName());
            variables.put("cai", summary.getOrDefault("cai", "N/A"));
            variables.put("gcContent", summary.getOrDefault("GCContent", "N/A") + "%");
            variables.put("optimizedLength", summary.getOrDefault("optimizedLength", "N/A"));
            variables.put("downloadUrl", EmailConstants.FRONTEND_URL + "/api/tasks/" + task.getTaskId() + "/download");
            variables.put("taskDetailUrl", EmailConstants.FRONTEND_URL + "/tasks/" + task.getTaskId());

            // 获取收件人列表（测试模式）
            List<String> recipients = getRecipients(user.getEmail());

            for (String recipient : recipients) {
                mailService.sendTemplateEmailWithAttachment(
                        recipient,
                        EmailConstants.TASK_COMPLETION_SUBJECT,
                        EmailConstants.TASK_COMPLETION_TEMPLATE,
                        variables,
                        zipFile
                );
            }

            log.info("Task completion notification sent for task: {}", task.getTaskId());
        } catch (Exception e) {
            log.error("Failed to send task completion notification for task: {}", task.getTaskId(), e);
        }
    }

    /**
     * 发送任务失败通知
     *
     * @param task      任务对象
     * @param errorMsg  错误消息
     */
    @Async
    public void notifyTaskFailure(Task task, String errorMsg) {
        try {
            User user = userRepository.findById(task.getUserId())
                    .orElse(null);

            if (user == null) {
                log.warn("User not found for task: {}", task.getTaskId());
                return;
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("username", user.getUsername());
            variables.put("taskId", task.getTaskId());
            variables.put("taskName", task.getTaskName() != null ? task.getTaskName() : "未命名任务");
            variables.put("failedAt", task.getCompletedAt() != null ? task.getCompletedAt().format(formatter) : "N/A");
            variables.put("sequenceType", task.getSequenceType().name());
            variables.put("targetSpecies", task.getTargetSpecies().getDisplayName());
            variables.put("errorMessage", errorMsg != null ? errorMsg : "未知错误");
            variables.put("minLength", 10);
            variables.put("maxLength", 10000);
            variables.put("taskDetailUrl", EmailConstants.FRONTEND_URL + "/tasks/" + task.getTaskId());

            // 获取收件人列表（测试模式）
            List<String> recipients = getRecipients(user.getEmail());

            for (String recipient : recipients) {
                mailService.sendTemplateEmail(
                        recipient,
                        EmailConstants.TASK_FAILURE_SUBJECT,
                        EmailConstants.TASK_FAILURE_TEMPLATE,
                        variables
                );
            }

            log.info("Task failure notification sent for task: {}", task.getTaskId());
        } catch (Exception e) {
            log.error("Failed to send task failure notification for task: {}", task.getTaskId(), e);
        }
    }

    /**
     * 发送邮箱验证通知
     *
     * @param email            邮箱地址
     * @param username         用户名
     * @param verificationCode 验证码
     */
    @Async
    public void notifyEmailVerification(String email, String username, String verificationCode) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("username", username);
            variables.put("verificationCode", verificationCode);
            variables.put("verificationUrl", EmailConstants.FRONTEND_URL + "/verify?email=" + email + "&code=" + verificationCode);

            // 获取收件人列表（测试模式）
            List<String> recipients = getRecipients(email);

            for (String recipient : recipients) {
                mailService.sendTemplateEmail(
                        recipient,
                        EmailConstants.VERIFICATION_SUBJECT,
                        EmailConstants.VERIFICATION_TEMPLATE,
                        variables
                );
            }

            log.info("Email verification notification sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send email verification notification to: {}", email, e);
        }
    }

    /**
     * 发送自定义通知
     *
     * @param userId  用户ID
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    @Async
    public void sendCustomNotification(Long userId, String subject, String content) {
        try {
            User user = userRepository.findById(userId)
                    .orElse(null);

            if (user == null) {
                log.warn("User not found: {}", userId);
                return;
            }

            mailService.sendEmail(user.getEmail(), subject, content);
            log.info("Custom notification sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send custom notification to user: {}", userId, e);
        }
    }

    /**
     * 计算队列位置
     */
    private int calculateQueuePosition(String taskId) {
        return taskRepository.findByStatusOrderBySubmittedAtAsc(com.codonopt.enums.TaskStatus.QUEUED)
                .stream()
                .filter(t -> t.getSubmittedAt().isBefore(
                        taskRepository.findById(taskId).get().getSubmittedAt()))
                .collect(Collectors.toList())
                .size() + 1;
    }

    /**
     * 解析结果摘要
     */
    private Map<String, Object> parseResultSummary(String resultSummary) {
        Map<String, Object> summary = new HashMap<>();
        if (resultSummary != null && !resultSummary.isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(resultSummary, HashMap.class);
            } catch (Exception e) {
                log.error("Failed to parse result summary", e);
            }
        }
        return summary;
    }
}
