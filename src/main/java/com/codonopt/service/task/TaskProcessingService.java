package com.codonopt.service.task;

import com.codonopt.entity.Task;
import com.codonopt.enums.TaskStatus;
import com.codonopt.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 任务处理服务 - 专门处理带事务的操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskProcessingService {

    private final TaskRepository taskRepository;
    private final CodonTaskExecutor taskExecutor;

    /**
     * 处理任务（带事务）
     */
    @Transactional
    public void processTaskWithTransaction(Task task) {
        try {
            log.info("========== 开始处理任务 ==========");
            log.info("任务ID: {}", task.getTaskId());
            log.info("任务名称: {}", task.getTaskName());
            log.info("当前状态: {}", task.getStatus());
            log.info("序列类型: {}", task.getSequenceType());
            log.info("目标物种: {}", task.getTargetSpecies());

            // 更新任务状态为运行中
            log.info("准备更新任务状态为 RUNNING");
            LocalDateTime now = LocalDateTime.now();
            log.info("更新时间: {}", now);

            int updated = taskRepository.updateTaskStatus(
                    task.getTaskId(),
                    TaskStatus.RUNNING.name(),
                    now
            );

            log.info("更新结果: {} 行被更新", updated);

            if (updated > 0) {
                log.info("任务状态更新成功，开始异步执行");
                log.info("异步执行已启动，任务将在后台运行");
                // 执行任务（异步执行）- 不传入task对象，避免实体状态干扰
                executeTaskAsyncById(task.getTaskId());
            } else {
                log.warn("任务状态更新失败 - 返回0行，可能任务已被删除或状态已改变");
                taskRepository.findById(task.getTaskId()).ifPresent(reloaded -> {
                    log.warn("重新加载的任务状态: {}", reloaded.getStatus());
                });
            }

            log.info("========== 任务处理完成 ==========");

        } catch (Exception e) {
            log.error("启动任务执行失败: {}", task.getTaskId(), e);
            throw e;
        }
    }

    /**
     * 根据任务ID异步执行任务
     */
    @org.springframework.scheduling.annotation.Async
    public void executeTaskAsyncById(String taskId) {
        try {
            // 重新从数据库加载任务，确保使用最新状态
            taskRepository.findById(taskId).ifPresent(task -> {
                log.info("开始执行任务: {} (状态: {})", taskId, task.getStatus());
                taskExecutor.executeTask(task);
            });
        } catch (Exception e) {
            log.error("Task execution failed: {}", taskId, e);
        }
    }

    /**
     * 异步执行任务
     */
    @org.springframework.scheduling.annotation.Async
    public void executeTaskAsync(Task task) {
        try {
            taskExecutor.executeTask(task);
        } catch (Exception e) {
            log.error("Task execution failed: {}", task.getTaskId(), e);
        }
    }

    /**
     * 标记任务为失败状态（带事务）
     */
    @Transactional
    public void markTaskAsFailed(String taskId, TaskStatus status, LocalDateTime completedAt, String errorMessage) {
        try {
            taskRepository.updateTaskFailed(taskId, status, completedAt, errorMessage);
            log.info("任务 {} 已标记为失败: {}", taskId, errorMessage);
        } catch (Exception e) {
            log.error("标记任务失败状态时出错: {}", taskId, e);
        }
    }
}
