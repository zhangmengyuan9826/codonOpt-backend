package com.codonopt.service.task;

import com.codonopt.dto.response.TaskResultResponse;
import com.codonopt.entity.Task;
import com.codonopt.enums.TaskStatus;
import com.codonopt.exception.BusinessException;
import com.codonopt.repository.TaskRepository;
import com.codonopt.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 任务结果服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskResultService {

    @Value("${app.perl.result-directory}")
    private String resultDirectory;

    private final TaskRepository taskRepository;

    /**
     * 获取任务结果内容
     *
     * @param taskId 任务ID
     * @param userId  用户ID
     * @return 任务结果响应
     */
    public TaskResultResponse getTaskResult(String taskId, Long userId) {
        // 1. 获取任务并验证权限
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("任务不存在"));

        if (!task.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该任务");
        }

        // 2. 检查任务状态
        if (task.getStatus() != TaskStatus.COMPLETED && task.getStatus() != TaskStatus.FAILED) {
            throw new BusinessException("任务尚未完成，无法获取结果");
        }

        // 3. 构建任务文件夹路径
        String taskFolderPath = Paths.get(resultDirectory, "task_" + taskId).toString();

        // 4. 检查文件夹是否存在
        if (!FileUtil.fileExists(taskFolderPath)) {
            throw new BusinessException("任务结果文件夹不存在");
        }

        // 5. 读取run.txt文件
        String runFilePath = Paths.get(taskFolderPath, "run.txt").toString();
        if (!FileUtil.fileExists(runFilePath)) {
            throw new BusinessException("运行状态文件不存在");
        }

        try {
            Map<String, String> runData = parseRunFile(runFilePath);
            String status = runData.get("status");

            TaskResultResponse.TaskResultResponseBuilder builder = TaskResultResponse.builder()
                    .taskId(taskId)
                    .status(status)
                    .startedTime(parseDateTime(runData.get("startedTime")))
                    .completedAt(parseDateTime(runData.get("completedTime")));

            if ("SUCCESS".equals(status)) {
                // 成功：读取result.txt
                String resultFilePath = Paths.get(taskFolderPath, "result.txt").toString();
                if (FileUtil.fileExists(resultFilePath)) {
                    Map<String, String> resultData = parseResultFile(resultFilePath);
                    builder.sequence(resultData.get("sequence"))
                            .cai(resultData.get("CAI"))
                            .gcContent(resultData.get("GCContent"))
                            .mfi(resultData.get("MFI"))
                            .downloadable(true)
                            .folderPath(taskFolderPath);
                } else {
                    throw new BusinessException("结果文件不存在");
                }
            } else if ("FAILED".equals(status)) {
                // 失败：读取错误信息
                builder.errorMessage(runData.get("info"))
                        .downloadable(false);
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Failed to read task result: {}", taskId, e);
            throw new BusinessException("读取任务结果失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务文件夹用于打包下载
     *
     * @param taskId 任务ID
     * @param userId  用户ID
     * @return 文件夹路径
     */
    public String getTaskFolderPathForDownload(String taskId, Long userId) {
        // 1. 获取任务并验证权限
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("任务不存在"));

        if (!task.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该任务");
        }

        // 2. 检查任务状态
        if (task.getStatus() != TaskStatus.COMPLETED) {
            throw new BusinessException("只有成功的任务可以下载");
        }

        // 3. 构建任务文件夹路径
        String taskFolderPath = Paths.get(resultDirectory, "task_" + taskId).toString();

        // 4. 检查文件夹是否存在
        if (!FileUtil.fileExists(taskFolderPath)) {
            throw new BusinessException("任务结果文件夹不存在");
        }

        // 5. 验证任务确实成功
        String runFilePath = Paths.get(taskFolderPath, "run.txt").toString();
        if (FileUtil.fileExists(runFilePath)) {
            try {
                Map<String, String> runData = parseRunFile(runFilePath);
                if (!"SUCCESS".equals(runData.get("status"))) {
                    throw new BusinessException("任务执行失败，无法下载");
                }
            } catch (Exception e) {
                throw new BusinessException("无法验证任务状态");
            }
        }

        return taskFolderPath;
    }

    /**
     * 解析run.txt文件
     */
    private Map<String, String> parseRunFile(String filePath) throws Exception {
        String content = FileUtil.readFile(filePath);
        Map<String, String> data = new HashMap<>();

        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.contains(":")) {
                int colonIndex = line.indexOf(":");
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                data.put(key, value);
            }
        }

        return data;
    }

    /**
     * 解析result.txt文件
     */
    private Map<String, String> parseResultFile(String filePath) throws Exception {
        String content = FileUtil.readFile(filePath);
        Map<String, String> data = new HashMap<>();

        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.contains(":")) {
                int colonIndex = line.indexOf(":");
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                data.put(key, value);
            }
        }

        return data;
    }

    /**
     * 解析日期时间字符串
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (Exception e) {
            log.warn("Failed to parse datetime: {}", dateTimeStr, e);
            return null;
        }
    }
}
