package com.codonopt.service.task;

import com.codonopt.entity.Task;
import com.codonopt.enums.TaskStatus;
import com.codonopt.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 任务调度器
 * Renamed from TaskScheduler to avoid Spring bean naming conflict
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodonTaskScheduler {

    private final TaskRepository taskRepository;
    private final CodonTaskExecutor taskExecutor;

    // 确保同时只有一个任务在执行
    private final ReentrantLock executionLock = new ReentrantLock();

    /**
     * 定时检查并处理队列中的下一个任务
     * 每30秒执行一次
     */
    @Scheduled(fixedDelay = 3000000)
    public void checkAndProcessNextTask() {
        try {
            // 检查是否有运行中的任务
            Optional<Task> runningTask = taskRepository.findRunningTask();
            if (runningTask.isPresent()) {
                log.debug("Task is already running: {}", runningTask.get().getTaskId());
                // 检查超时任务
                checkTimeoutTasks();
                return;
            }

            // 获取队列中的第一个任务
            Optional<Task> nextTask = taskRepository.findFirstQueuedTask();
            if (!nextTask.isPresent()) {
                log.debug("No tasks in queue");
                return;
            }

            Task task = nextTask.get();
            log.info("Found task in queue: {}", task.getTaskId());

            // 尝试获取执行锁
            if (executionLock.tryLock()) {
                try {
                    processTask(task);
                } finally {
                    executionLock.unlock();
                }
            } else {
                log.debug("Could not acquire execution lock, another task is being processed");
            }

        } catch (Exception e) {
            log.error("Error in task scheduler", e);
        }
    }

    /**
     * 处理任务
     */
    private void processTask(Task task) {
        try {
            log.info("Processing task: {}", task.getTaskId());

            // 更新任务状态为运行中
            taskRepository.updateTaskStatus(
                    task.getTaskId(),
                    TaskStatus.RUNNING,
                    LocalDateTime.now()
            );

            // 执行任务（异步执行）
            executeTaskAsync(task);

        } catch (Exception e) {
            log.error("Failed to start task execution: {}", task.getTaskId(), e);
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
     * 检查超时任务
     */
    private void checkTimeoutTasks() {
        try {
            // 计算超时阈值（2小时前）
            LocalDateTime timeoutThreshold = LocalDateTime.now()
                    .minusSeconds(com.codonopt.constants.TaskConstants.TASK_TIMEOUT_MS / 1000);

            // 查找超时的运行中任务
            java.util.List<Task> timeoutTasks = taskRepository.findTimeoutTasks(timeoutThreshold);

            for (Task task : timeoutTasks) {
                log.warn("Found timeout task: {}", task.getTaskId());

                // 如果有进程ID，尝试终止进程
                if (task.getProcessPid() != null && task.getProcessPid() > 0) {
                    taskExecutor.terminateTask(task.getProcessPid());
                }

                // 更新任务状态为失败
                taskRepository.updateTaskFailed(
                        task.getTaskId(),
                        TaskStatus.FAILED,
                        LocalDateTime.now(),
                        "任务执行超时"
                );
            }

        } catch (Exception e) {
            log.error("Error checking timeout tasks", e);
        }
    }

    /**
     * 手动触发处理下一个任务（用于测试）
     */
    public void processNextTask() {
        checkAndProcessNextTask();
    }
}
