package com.codonopt.controller;

import com.codonopt.dto.request.TaskSubmitRequest;
import com.codonopt.dto.response.ApiResponse;
import com.codonopt.dto.response.TaskDetailResponse;
import com.codonopt.dto.response.TaskResponse;
import com.codonopt.dto.response.TaskResultResponse;
import com.codonopt.dto.response.PageResponse;
import com.codonopt.entity.Task;
import com.codonopt.entity.User;
import com.codonopt.enums.UserRole;
import com.codonopt.repository.UserRepository;
import com.codonopt.service.task.TaskServiceImpl;
import com.codonopt.service.task.TaskResultService;
import com.codonopt.util.FileUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.management.relation.Role;
import javax.validation.Valid;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * 任务控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "任务管理", description = "任务提交、查询、下载等接口")
public class TaskController {

    private final TaskServiceImpl taskService;
    private final com.codonopt.repository.TaskRepository taskRepository;
    private final TaskResultService taskResultService;
    private final UserRepository userRepository;

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
     * 提交任务
     */
    @PostMapping
    @Operation(summary = "提交任务", description = "提交新的密码子优化任务")
    public ResponseEntity<ApiResponse<String>> submitTask(@Valid @RequestBody TaskSubmitRequest request) {
        Long userId = getCurrentUserId();
        String taskId = taskService.submitTask(request, userId);
        return ResponseEntity.ok(ApiResponse.success("任务提交成功", taskId));
    }

    /**
     * 获取用户任务列表（分页）
     */
    @GetMapping
    @Operation(summary = "获取任务列表", description = "获取当前用户的任务（支持分页）")
    public ResponseEntity<ApiResponse<PageResponse<TaskResponse>>> getTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
//        管理员可以看到所有的任务列表，普通用户只能看到自己的任务列表
        Long userId = getCurrentUserId();
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        // 创建分页对象
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        if(isAdmin) {
            Page<Task> taskPage = taskRepository.findAllByOrderBySubmittedAtDesc(pageable);
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
        PageResponse<TaskResponse> tasks = taskService.getUserTasksPaginated(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "获取任务详情", description = "根据任务ID获取详细信息")
    public ResponseEntity<ApiResponse<TaskDetailResponse>> getTaskDetail(@PathVariable String taskId) {
        Long userId = getCurrentUserId();
        TaskDetailResponse task = taskService.getTaskDetail(taskId, userId);
        return ResponseEntity.ok(ApiResponse.success(task));
    }

    /**
     * 取消任务
     */
    @DeleteMapping("/{taskId}")
    @Operation(summary = "取消任务", description = "取消排队中的任务")
    public ResponseEntity<ApiResponse<Void>> cancelTask(@PathVariable String taskId) {
        Long userId = getCurrentUserId();
        taskService.cancelTask(taskId, userId);
        return ResponseEntity.ok(ApiResponse.success("任务已取消"));
    }

    /**
     * 下载任务结果文件
     */
    @GetMapping("/{taskId}/download")
    @Operation(summary = "下载结果文件", description = "下载任务执行结果（ZIP格式）")
    public ResponseEntity<Resource> downloadTaskResult(@PathVariable String taskId) {
        Long userId = getCurrentUserId();

        // 获取任务详情以验证权限和获取文件路径
        com.codonopt.entity.Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new com.codonopt.exception.BusinessException("任务不存在"));

        // 验证权限
        if (!task.getUserId().equals(userId)) {
            throw new com.codonopt.exception.BusinessException("无权访问该任务");
        }

        // 检查任务状态
        if (task.getStatus() != com.codonopt.enums.TaskStatus.COMPLETED) {
            throw new com.codonopt.exception.BusinessException("任务尚未完成");
        }

        // 检查文件是否存在
        String filePath = task.getResultFilesPath();
        if (filePath == null || filePath.isEmpty()) {
            throw new com.codonopt.exception.BusinessException("结果文件不存在");
        }

        File file = new File(filePath);
        if (!file.exists()) {
            throw new com.codonopt.exception.BusinessException("结果文件不存在");
        }

        Resource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .body(resource);
    }

    /**
     * 获取任务结果内容
     */
    @GetMapping("/{taskId}/result")
    @Operation(summary = "获取任务结果", description = "获取任务执行结果的详细内容（包含序列、CAI等）")
    public ResponseEntity<ApiResponse<TaskResultResponse>> getTaskResult(@PathVariable String taskId) {
        Long userId = getCurrentUserId();
        TaskResultResponse result = taskResultService.getTaskResult(taskId, userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 下载任务结果文件夹（打包成ZIP）
     */
    @GetMapping("/{taskId}/download-folder")
    @Operation(summary = "下载任务文件夹", description = "下载任务执行结果文件夹（ZIP格式，仅成功的任务可下载）")
    public ResponseEntity<Resource> downloadTaskFolder(@PathVariable String taskId) {
        Long userId = getCurrentUserId();

        // 获取任务文件夹路径
        String folderPath = taskResultService.getTaskFolderPathForDownload(taskId, userId);

        try {
            // 创建临时ZIP文件
            String zipFilePath = folderPath + ".zip";
            FileUtil.createZip(folderPath, zipFilePath);

            File zipFile = new File(zipFilePath);
            if (!zipFile.exists()) {
                throw new com.codonopt.exception.BusinessException("创建ZIP文件失败");
            }

            Resource resource = new FileSystemResource(zipFile);

            // 设置文件名
            String fileName = "task_" + taskId + ".zip";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(zipFile.length())
                    .body(resource);

        } catch (Exception e) {
            log.error("Failed to create zip file for task: {}", taskId, e);
            throw new com.codonopt.exception.BusinessException("打包下载失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查任务服务状态")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Task service is running"));
    }
}
