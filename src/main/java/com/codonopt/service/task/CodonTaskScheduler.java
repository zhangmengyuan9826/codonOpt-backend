package com.codonopt.service.task;

import com.codonopt.entity.Task;
import com.codonopt.enums.TaskStatus;
import com.codonopt.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final TaskProcessingService taskProcessingService;

    // 确保同时只有一个任务在执行
    private final ReentrantLock executionLock = new ReentrantLock();

    /**
     * 定时检查并处理队列中的下一个任务
     * 每30秒执行一次
     */
    @Scheduled(fixedDelay = 30000)
    public void checkAndProcessNextTask() {
        log.info("===== 调度器触发: 检查任务队列 =====");

        try {
            // 检查是否有运行中的任务并更新状态
            checkRunningTasksStatus();

            // 检查是否有运行中的任务
            long runningCount = taskRepository.countByStatus(TaskStatus.RUNNING);
            if (runningCount > 0) {
                log.info("有 {} 个任务正在运行中，等待当前任务完成", runningCount);
                // 检查超时任务
                checkTimeoutTasks();
                return;
            }

            // 获取队列中的第一个任务
            Optional<Task> nextTask = taskRepository.findFirstQueuedTask();
            if (!nextTask.isPresent()) {
                log.info("队列中没有待处理的任务");
                return;
            }

            Task task = nextTask.get();
            log.info("发现排队任务: {} - {}", task.getTaskId(), task.getTaskName());

            // 尝试获取执行锁
            if (executionLock.tryLock()) {
                try {
                    log.info("成功获取执行锁，开始处理任务: {}", task.getTaskId());
                    taskProcessingService.processTaskWithTransaction(task);
                } finally {
                    executionLock.unlock();
                    log.info("释放执行锁");
                }
            } else {
                log.warn("无法获取执行锁，另一个任务正在被处理");
            }

        } catch (Exception e) {
            log.error("调度器处理出错", e);
        }

        log.info("===== 调度器检查完成 =====");
    }

    /**
     * 定时检查运行中任务的状态（用于异步脚本模式）
     * 每30秒执行一次
     */
//    @Scheduled(fixedDelay = 30000)
    @Scheduled(fixedDelay = 10000)
    public void checkRunningTasksStatus() {
        try {
            java.util.List<Task> runningTasks = taskRepository.findByStatus(TaskStatus.RUNNING);

            if (!runningTasks.isEmpty()) {
                log.debug("Checking status of {} running tasks", runningTasks.size());

                for (Task task : runningTasks) {
                    try {
                        // 使用TaskProcessingService来检查状态（带事务）
                        taskProcessingService.checkTaskStatusWithTransaction(task);
                    } catch (Exception e) {
                        log.error("Error checking status for task: {}", task.getTaskId(), e);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error checking running tasks status", e);
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

                // 使用 taskProcessingService 来更新失败状态（带事务）
                taskProcessingService.markTaskAsFailed(
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
