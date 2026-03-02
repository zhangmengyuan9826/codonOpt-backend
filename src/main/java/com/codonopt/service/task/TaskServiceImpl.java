package com.codonopt.service.task;

import com.codonopt.constants.SecurityConstants;
import com.codonopt.dto.request.TaskSubmitRequest;
import com.codonopt.dto.response.TaskDetailResponse;
import com.codonopt.dto.response.TaskResponse;
import com.codonopt.dto.response.PageResponse;
import com.codonopt.entity.Task;
import com.codonopt.entity.User;
import com.codonopt.enums.TaskStatus;
import com.codonopt.enums.TargetSpecies;
import com.codonopt.enums.UserRole;
import com.codonopt.exception.BusinessException;
import com.codonopt.exception.RateLimitExceededException;
import com.codonopt.repository.TaskRepository;
import com.codonopt.repository.UserRepository;
import com.codonopt.service.security.RateLimitService;
import com.codonopt.service.file.FileUploadService;
import com.codonopt.util.SequenceValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 任务服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl {

    private final TaskRepository taskRepository;
    private final CodonTaskScheduler taskScheduler;
    private final RateLimitService rateLimitService;
    private final FileUploadService fileUploadService;
    private final UserRepository userRepository;

    /**
     * 提交任务
     *
     * @param request 任务提交请求
     * @param userId  用户ID
     * @return 任务ID
     */
    @Transactional
    public String submitTask(TaskSubmitRequest request, Long userId) {
        // 1. 频率限制检查
        if (!rateLimitService.checkDailyLimit(userId, SecurityConstants.DAILY_TASK_LIMIT)) {
            throw new RateLimitExceededException();
        }

        // 2. 如果物种是OTHERS，验证密码子频率表文件
        if (request.getTargetSpecies() == TargetSpecies.OTHERS) {
            if (request.getCodonFrequencyFileName() == null || request.getCodonFrequencyFileName().isEmpty()) {
                throw new BusinessException("选择自定义物种时，必须上传密码子频率表文件");
            }
        }

        // 3. 验证序列格式
        if (!SequenceValidator.isValidSequence(request.getInputSequence(), request.getSequenceType())) {
            throw new BusinessException("序列格式不正确");
        }

        // 4. 验证序列长度
        if (!SequenceValidator.isLengthValid(
                request.getInputSequence(),
                com.codonopt.constants.TaskConstants.MIN_SEQUENCE_LENGTH,
                com.codonopt.constants.TaskConstants.MAX_SEQUENCE_LENGTH)) {
            throw new BusinessException(
                    "序列长度必须在" +
                    com.codonopt.constants.TaskConstants.MIN_SEQUENCE_LENGTH +
                    "-" +
                    com.codonopt.constants.TaskConstants.MAX_SEQUENCE_LENGTH +
                    "个字符之间"
            );
        }

        // 5. 创建任务实体
        Task task = Task.builder()
                .taskId(UUID.randomUUID().toString())
                .userId(userId)
                .taskName(request.getTaskName())
                .inputSequence(request.getInputSequence())
                .sequenceType(request.getSequenceType())
                .targetSpecies(request.getTargetSpecies())
                .codonFrequencyFilePath(request.getCodonFrequencyFileName())
                .parameters(request.getParameters() != null ?
                        convertParamsToJson(request.getParameters()) : null)
                .status(TaskStatus.QUEUED)
                .queuePosition(0)
                .build();

        // 6. 保存任务
        task = taskRepository.save(task);

        // 7. 计算队列位置
        int queuePosition = calculateQueuePosition(task.getTaskId());
        task.setQueuePosition(queuePosition);
        taskRepository.save(task);

        // 8. 增加用户今日提交计数
        rateLimitService.incrementCounter(userId);

        // 9. 触发任务调度器（异步）
        taskScheduler.checkAndProcessNextTask();

        log.info("Task submitted successfully: {} by user: {}", task.getTaskId(), userId);

        return task.getTaskId();
    }

    /**
     * 获取用户任务列表
     *
     * @param userId 用户ID
     * @return 任务列表
     */
    public List<TaskResponse> getUserTasks(Long userId) {
        List<Task> tasks = taskRepository.findByUserIdOrderBySubmittedAtDesc(userId);

        return tasks.stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toCollection(java.util.ArrayList::new));
    }

    /**
     * 获取用户任务列表（分页）
     *
     * @param userId    用户ID
     * @param pageable 分页对象
     * @return 分页任务响应
     */
    public PageResponse<TaskResponse> getUserTasksPaginated(Long userId, Pageable pageable) {
        Page<Task> taskPage = taskRepository.findByUserIdOrderBySubmittedAtDesc(userId, pageable);

        Page<TaskResponse> responsePage = taskPage.map(this::convertToTaskResponse);
        return PageResponse.of(responsePage);
    }

    /**
     * 获取任务详情
     *
     * @param taskId 任务ID
     * @param userId  用户ID
     * @return 任务详情
     */
    public TaskDetailResponse getTaskDetail(String taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("任务不存在"));

        // 验证任务所有权
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该任务");
        }

        return convertToTaskDetailResponse(task);
    }

    /**
     * 取消任务
     *
     * @param taskId 任务ID
     * @param userId  用户ID
     */
    @Transactional
    public void cancelTask(String taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("任务不存在"));

        // 验证任务所有权
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该任务");
        }

        // 只有排队中的任务可以取消
        if (task.getStatus() != TaskStatus.QUEUED) {
            throw new BusinessException("只有排队中的任务可以取消");
        }

        task.setStatus(TaskStatus.CANCELLED);
        task.setCompletedAt(LocalDateTime.now());
        taskRepository.save(task);

        log.info("Task cancelled: {} by user: {}", taskId, userId);
    }

    /**
     * 计算队列位置
     */
    private int calculateQueuePosition(String taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
        LocalDateTime submittedTime = task.getSubmittedAt();

        // 统计在此任务之前提交的排队任务数量
        List<Task> queuedTasks = taskRepository.findByStatusOrderBySubmittedAtAsc(TaskStatus.QUEUED);
        int position = 1;
        for (Task t : queuedTasks) {
            if (t.getTaskId().equals(taskId)) {
                break;
            }
            if (t.getSubmittedAt().isBefore(submittedTime)) {
                position++;
            }
        }
        return position;
    }

    /**
     * 转换参数为JSON字符串
     */
    private String convertParamsToJson(java.util.Map<String, Object> params) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(params);
        } catch (Exception e) {
            log.error("Failed to convert params to JSON", e);
            return null;
        }
    }
    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        throw new RuntimeException("未认证的用户");
    }
    /**
     * 转换为任务响应
     */
    private TaskResponse convertToTaskResponse(Task task) {
        // 获取当前用户角色
        Long userId = getCurrentUserId();
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
//        如果当前用户是ADMIN，则显示用户名，否则不显示
        if(isAdmin) {
            return TaskResponse.builder()
                .taskId(task.getTaskId())
                .userName(userRepository.findById(task.getUserId()).map(User::getUsername).orElse("未知用户"))
                .sequenceType(task.getSequenceType().name())
                .targetSpecies(String.valueOf(task.getTargetSpecies().getDisplayName()))
                .status(task.getStatus())
                .queuePosition(task.getQueuePosition())
                .submittedAt(task.getSubmittedAt())
                .completedAt(task.getCompletedAt())
                .build();
        }
        return TaskResponse.builder()
                .taskId(task.getTaskId())
                .taskName(task.getTaskName())
                .sequenceType(task.getSequenceType().name())
                .targetSpecies(String.valueOf(task.getTargetSpecies()))
                .status(task.getStatus())
                .queuePosition(task.getQueuePosition())
                .submittedAt(task.getSubmittedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }

    /**
     * 转换为任务详情响应
     */
    private TaskDetailResponse convertToTaskDetailResponse(Task task) {
        try {
            java.util.Map<String, Object> params = task.getParameters() != null ?
                    new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(task.getParameters(), java.util.Map.class) : null;

            java.util.Map<String, Object> summary = task.getResultSummary() != null ?
                    new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(task.getResultSummary(), java.util.Map.class) : null;

            return TaskDetailResponse.builder()
                    .taskId(task.getTaskId())
                    .taskName(task.getTaskName())
                    .inputSequence(task.getInputSequence())
                    .sequenceType(task.getSequenceType().name())
                    .targetSpecies(task.getTargetSpecies().getDisplayName())
                    .parameters(params)
                    .status(task.getStatus())
                    .queuePosition(task.getQueuePosition())
                    .processPid(task.getProcessPid())
                    .resultFilesPath(task.getResultFilesPath())
                    .resultSummary(summary)
                    .errorMessage(task.getErrorMessage())
                    .submittedAt(task.getSubmittedAt())
                    .startedAt(task.getStartedAt())
                    .completedAt(task.getCompletedAt())
                    .build();
        } catch (Exception e) {
            log.error("Failed to convert task to detail response", e);
            throw new BusinessException("转换任务详情失败");
        }
    }
}
