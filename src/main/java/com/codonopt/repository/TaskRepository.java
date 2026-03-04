package com.codonopt.repository;

import com.codonopt.entity.Task;
import com.codonopt.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 任务数据访问接口
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, String> {

    /**
     * 根据用户ID查找所有任务
     */
    List<Task> findByUserIdOrderBySubmittedAtDesc(Long userId);

    /**
     * 根据用户ID分页查找任务
     */
    Page<Task> findByUserIdOrderBySubmittedAtDesc(Long userId, Pageable pageable);

    /**
     * 查找所有任务（分页）
     */
    Page<Task> findAllByOrderBySubmittedAtDesc(Pageable pageable);

    /**
     * 根据状态查找任务
     */
    List<Task> findByStatusOrderBySubmittedAtAsc(TaskStatus status);

    /**
     * 根据状态查找所有任务
     */
    List<Task> findByStatus(TaskStatus status);

    /**
     * 查找运行中的任务
     */
    @Query("SELECT t FROM Task t WHERE t.status = 'RUNNING'")
    Optional<Task> findRunningTask();

    /**
     * 查找第一个排队的任务
     */
    @Query("SELECT t FROM Task t WHERE t.status = 'QUEUED' ORDER BY t.submittedAt ASC")
    Optional<Task> findFirstQueuedTask();

    /**
     * 统计用户指定状态的任务数量
     */
    long countByUserIdAndStatus(Long userId, TaskStatus status);

    /**
     * 统计用户今日提交的任务数量
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.userId = :userId AND DATE(t.submittedAt) = DATE(:date)")
    long countByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDateTime date);

    /**
     * 更新任务状态
     */
    @Modifying
    @Query(value = "UPDATE tasks SET status = :status, started_at = :startedAt WHERE task_id = :taskId", nativeQuery = true)
    int updateTaskStatus(@Param("taskId") String taskId, @Param("status") String status, @Param("startedAt") LocalDateTime startedAt);

    /**
     * 更新任务完成状态
     */
    @Modifying
    @Query("UPDATE Task t SET t.status = :status, t.completedAt = :completedAt, t.resultFilesPath = :resultPath, t.resultSummary = :resultSummary WHERE t.taskId = :taskId")
    int updateTaskCompleted(@Param("taskId") String taskId, @Param("status") TaskStatus status, @Param("completedAt") LocalDateTime completedAt, @Param("resultPath") String resultPath, @Param("resultSummary") String resultSummary);

    /**
     * 更新任务失败状态
     */
    @Modifying
    @Query("UPDATE Task t SET t.status = :status, t.completedAt = :completedAt, t.errorMessage = :errorMessage WHERE t.taskId = :taskId")
    int updateTaskFailed(@Param("taskId") String taskId, @Param("status") TaskStatus status, @Param("completedAt") LocalDateTime completedAt, @Param("errorMessage") String errorMessage);

    /**
     * 更新队列位置
     */
    @Modifying
    @Query("UPDATE Task t SET t.queuePosition = :position WHERE t.taskId = :taskId")
    int updateQueuePosition(@Param("taskId") String taskId, @Param("position") Integer position);

    /**
     * 查找超时的运行中任务
     */
    @Query("SELECT t FROM Task t WHERE t.status = 'RUNNING' AND t.startedAt < :timeoutThreshold")
    List<Task> findTimeoutTasks(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);

    /**
     * 删除用户的所有任务
     */
    void deleteByUserId(Long userId);
}
