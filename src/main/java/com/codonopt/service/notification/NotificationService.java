package com.codonopt.service.notification;

import com.codonopt.constants.EmailConstants;
import com.codonopt.entity.Task;
import com.codonopt.entity.User;
import com.codonopt.repository.UserRepository;
import com.codonopt.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 任务通知服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final MailService mailService;
    private final UserRepository userRepository;

    /**
     * 发送任务完成通知
     *
     * @param task      任务对象
     * @param zipFile   结果ZIP文件
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

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            String content = String.format(
                    EmailConstants.TASK_COMPLETION_TEMPLATE,
                    user.getUsername(),
                    task.getTaskId(),
                    task.getSequenceType().name(),
                    task.getTargetSpecies().getDisplayName(),
                    task.getSubmittedAt().format(formatter),
                    task.getCompletedAt() != null ? task.getCompletedAt().format(formatter) : "N/A"
            );

            mailService.sendEmailWithAttachment(
                    user.getEmail(),
                    EmailConstants.TASK_COMPLETION_SUBJECT,
                    content,
                    zipFile
            );

            log.info("Task completion notification sent for task: {}", task.getTaskId());
        } catch (Exception e) {
            log.error("Failed to send task completion notification for task: {}", task.getTaskId(), e);
        }
    }

    /**
     * 发送任务失败通知
     *
     * @param task       任务对象
     * @param errorMsg   错误消息
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

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            String content = String.format(
                    EmailConstants.TASK_FAILURE_TEMPLATE,
                    user.getUsername(),
                    task.getTaskId(),
                    task.getSequenceType().name(),
                    task.getTargetSpecies().getDisplayName(),
                    task.getSubmittedAt().format(formatter),
                    errorMsg != null ? errorMsg : "未知错误"
            );

            mailService.sendEmail(
                    user.getEmail(),
                    EmailConstants.TASK_FAILURE_SUBJECT,
                    content
            );

            log.info("Task failure notification sent for task: {}", task.getTaskId());
        } catch (Exception e) {
            log.error("Failed to send task failure notification for task: {}", task.getTaskId(), e);
        }
    }

    /**
     * 发送自定义通知
     *
     * @param userId   用户ID
     * @param subject  邮件主题
     * @param content  邮件内容
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
}
