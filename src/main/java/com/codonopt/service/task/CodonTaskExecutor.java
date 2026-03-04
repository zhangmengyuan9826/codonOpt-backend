package com.codonopt.service.task;

import com.codonopt.constants.TaskConstants;
import com.codonopt.entity.Task;
import com.codonopt.enums.TaskStatus;
import com.codonopt.exception.TaskExecutionException;
import com.codonopt.repository.TaskRepository;
import com.codonopt.service.notification.NotificationService;
import com.codonopt.service.file.FileUploadService;
import com.codonopt.util.FileUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Perl脚本执行器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodonTaskExecutor {

    @Value("${app.perl.script-path}")
    private String scriptPath;

    @Value("${app.perl.script-directory}")
    private String scriptDirectory;

    @Value("${app.perl.result-directory}")
    private String resultDirectory;

    @Value("${app.perl.timeout}")
    private long taskTimeout;

    @Value("${app.perl.mock-mode:true}")
    private boolean mockMode;

    @Value("${app.mock-script.shell-script:scripts/mock_task_executor.sh}")
    private String mockShellScript;

    @Value("${app.mock-script.batch-script:scripts\\mock_task_executor.bat}")
    private String mockBatchScript;

    @Value("${app.mock-script.timeout:600000}")
    private long mockScriptTimeout;

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;
    private final FileUploadService fileUploadService;
    private final ObjectMapper objectMapper;

    /**
     * 执行任务
     *
     * @param task 任务对象
     */
    public void executeTask(Task task) {
        String taskId = task.getTaskId();
        log.info("Starting task execution: {} (mockMode: {})", taskId, mockMode);

        if (mockMode) {
            // 模拟模式执行
            executeTaskMock(task);
        } else {
            // 真实Perl脚本执行
            executeTaskReal(task);
        }
    }

    /**
     * 模拟任务执行（用于测试和演示）
     * 调用外部脚本进行异步执行，立即返回
     *
     * @param task 任务对象
     */
    private void executeTaskMock(Task task) {
        String taskId = task.getTaskId();
        log.info("Executing task in MOCK mode (async script): {}", taskId);

        try {
            // 1. 创建任务结果目录
            String taskResultDir = Paths.get(resultDirectory, "task_" + taskId).toString();
            FileUtil.createDirectoryIfNotExists(taskResultDir);

            LocalDateTime startTime = LocalDateTime.now();

            // 2. 异步启动脚本进程（立即返回）
            Process process = startMockScriptProcess(task, taskResultDir, startTime);

            // 3. 记录进程PID
            int pid = getProcessId(process);
            task.setProcessPid(pid);
            task.setStartedAt(startTime);
            taskRepository.save(task);

            log.info("Mock task script started successfully with PID: {} for task: {}", pid, taskId);
            log.info("Task will run in background for 5 minutes. Status will be updated upon completion.");

            // 注意：脚本正在后台异步执行
            // 脚本完成后，状态更新由调度器通过检查结果文件来完成
            // 或者可以通过监听进程退出事件来更新状态

        } catch (Exception e) {
            log.error("Failed to start mock task script: {}", taskId, e);
            handleTaskFailure(task, "启动任务脚本失败: " + e.getMessage());
        }
    }

    /**
     * 启动模拟脚本进程（异步执行）
     */
    private Process startMockScriptProcess(Task task, String taskResultDir, LocalDateTime startTime) throws Exception {
        // 检测操作系统
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        String scriptPath;
        String[] command;

        if (isWindows) {
            // Windows: 使用批处理脚本
            scriptPath = Paths.get(mockBatchScript).toAbsolutePath().toString();
            command = new String[]{
                    "cmd",
                    "/c",
                    scriptPath,
                    task.getTaskId(),
                    taskResultDir,
                    task.getSequenceType().name(),
                    task.getTargetSpecies().name()
            };
        } else {
            // Linux/Unix: 使用shell脚本
            scriptPath = Paths.get(mockShellScript).toAbsolutePath().toString();
            command = new String[]{
                    "/bin/bash",
                    scriptPath,
                    task.getTaskId(),
                    taskResultDir,
                    task.getSequenceType().name(),
                    task.getTargetSpecies().name()
            };
        }

        log.debug("Executing mock script command: {}", String.join(" ", command));

        // 启动进程（不等待，立即返回）
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        // 在新窗口中启动Windows批处理脚本（可选，便于调试）
        // 如果想要在新窗口中看到执行过程，取消下面的注释
        // if (isWindows) {
        //     processBuilder.command("cmd", "/c", "start", "cmd", "/c", scriptPath,
        //             task.getTaskId(), taskResultDir,
        //             task.getSequenceType().name(), task.getTargetSpecies().name());
        // }

        return processBuilder.start();
    }

    /**
     * 生成模拟成功结果文件
     */
    private void generateMockSuccessFiles(Task task, String taskResultDir,
                                          LocalDateTime startTime, LocalDateTime endTime) throws Exception {
        // 生成run.txt
        String runContent = String.format(
                "startedTime:%s%n" +
                "completedTime:%s%n" +
                "status:SUCCESS%n",
                startTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                endTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
        FileUtil.writeFile(Paths.get(taskResultDir, "run.txt").toString(), runContent);

        // 生成result.txt
        // 根据输入序列长度生成模拟的优化序列
        String inputSeq = task.getInputSequence().replaceAll("[^ACGTUacgtu]", "");
        StringBuilder optimizedSeq = new StringBuilder();
        String[] codons = {"ATG", "GCT", "GGA", "TCC", "AAC", "GAG", "CAG", "GTG", "CCC", "AAG"};
        for (int i = 0; i < Math.min(inputSeq.length(), 50); i++) {
            optimizedSeq.append(codons[i % codons.length]);
        }

        // 生成随机的CAI和GC含量
        double cai = 0.65 + (Math.random() * 0.30); // 0.65-0.95
        double gcContent = 0.40 + (Math.random() * 0.25); // 0.40-0.65
        double mfi = -500000 + (Math.random() * 300000); // -500000到-200000

        String resultContent = String.format(
                "sequence:%s%n" +
                "CAI:%.2f%n" +
                "GCContent:%.2f%n" +
                "MFI:%.2f%n",
                optimizedSeq.toString(),
                cai,
                gcContent,
                mfi
        );
        FileUtil.writeFile(Paths.get(taskResultDir, "result.txt").toString(), resultContent);

        // 如果有密码子频率表文件，复制到任务结果目录
        if (task.getCodonFrequencyFilePath() != null && !task.getCodonFrequencyFilePath().isEmpty()) {
            try {
                fileUploadService.copyFileToTaskDirectory(task.getCodonFrequencyFilePath(), taskResultDir);
                log.info("Copied codon frequency file to task directory: {}", taskResultDir);
            } catch (Exception e) {
                log.warn("Failed to copy codon frequency file: {}", e.getMessage());
            }
        }

        log.info("Generated mock success files in: {}", taskResultDir);
    }

    /**
     * 生成模拟失败结果文件
     */
    private void generateMockFailureFiles(Task task, String taskResultDir,
                                          LocalDateTime startTime, LocalDateTime endTime,
                                          String errorMessage) throws Exception {
        // 生成run.txt（失败状态）
        String runContent = String.format(
                "startedTime:%s%n" +
                "completedTime:%s%n" +
                "status:FAILED%n" +
                "info:%s%n",
                startTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                endTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                errorMessage
        );
        FileUtil.writeFile(Paths.get(taskResultDir, "run.txt").toString(), runContent);

        // 生成空的result.txt
        FileUtil.writeFile(Paths.get(taskResultDir, "result.txt").toString(), "");

        log.info("Generated mock failure files in: {}", taskResultDir);
    }

    /**
     * 生成模拟结果摘要
     */
    private Map<String, Object> generateMockResultSummary(Task task) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("status", "SUCCESS");
        summary.put("sequenceType", task.getSequenceType().name());
        summary.put("targetSpecies", task.getTargetSpecies().getDisplayName());
        summary.put("inputLength", task.getInputSequence().length());
        summary.put("message", "密码子优化完成");

        // 生成随机的优化指标
        summary.put("cai", String.format("%.2f", 0.65 + Math.random() * 0.30));
        summary.put("gcContent", String.format("%.2f%%", (40 + Math.random() * 25)));
        summary.put("mfi", String.format("%.2f", -500000 + Math.random() * 300000));
        summary.put("optimizedLength", task.getInputSequence().length() * 3);

        return summary;
    }

    /**
     * 真实任务执行（调用实际Perl脚本）
     *
     * @param task 任务对象
     */
    private void executeTaskReal(Task task) {
        String taskId = task.getTaskId();
        log.info("Executing task in REAL mode: {}", taskId);

        Process process = null;
        try {
            // 1. 创建任务结果目录
            String taskResultDir = Paths.get(resultDirectory, taskId).toString();
            FileUtil.createDirectoryIfNotExists(taskResultDir);

            // 2. 构建Perl命令
            String[] command = buildPerlCommand(task, taskResultDir);

            log.debug("Executing command: {}", String.join(" ", command));

            // 3. 启动进程
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true); // 合并错误流和输出流
            process = processBuilder.start();

            // 4. 记录进程PID
            int pid = getProcessId(process);
            task.setProcessPid(pid);
            taskRepository.save(task);
            log.info("Task process started with PID: {} for task: {}", pid, taskId);

            // 5. 等待进程完成
            boolean finished = process.waitFor(taskTimeout, java.util.concurrent.TimeUnit.MILLISECONDS);

            if (!finished) {
                // 超时，强制终止进程
                log.warn("Task timeout, terminating process for task: {}", taskId);
                process.destroyForcibly();
                throw new TaskExecutionException("任务执行超时");
            }

            // 6. 读取进程输出
            String output = readProcessOutput(process);

            // 7. 检查退出码
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new TaskExecutionException("Perl脚本执行失败，退出码: " + exitCode + ", 输出: " + output);
            }

            // 8. 验证结果文件
            File statusFile = new File(taskResultDir, TaskConstants.STATUS_LOG);
            if (!statusFile.exists()) {
                throw new TaskExecutionException("结果文件不存在: " + TaskConstants.STATUS_LOG);
            }

            String statusContent = FileUtil.readFile(statusFile.getAbsolutePath());
            if (!statusContent.contains(TaskConstants.STATUS_SUCCESS)) {
                // 读取错误日志
                String errorMsg = "任务执行失败";
                File errorFile = new File(taskResultDir, TaskConstants.ERROR_LOG);
                if (errorFile.exists()) {
                    errorMsg = FileUtil.readFile(errorFile.getAbsolutePath());
                }
                throw new TaskExecutionException(errorMsg);
            }

            // 9. 读取并解析结果摘要
            Map<String, Object> resultSummary = readResultSummary(taskResultDir);

            // 10. 创建ZIP压缩包
            String zipFilePath = taskResultDir + ".zip";
            FileUtil.createZip(taskResultDir, zipFilePath);

            // 11. 更新任务状态为完成
            taskRepository.updateTaskCompleted(
                    taskId,
                    TaskStatus.COMPLETED.name(),
                    LocalDateTime.now(),
                    zipFilePath,
                    objectMapper.writeValueAsString(resultSummary)
            );

            log.info("Task completed successfully: {}", taskId);

            // 12. 发送完成通知
            File zipFile = new File(zipFilePath);
            notificationService.notifyTaskCompletion(task, zipFile);

        } catch (TaskExecutionException e) {
            log.error("Task execution failed: {}", taskId, e);
            handleTaskFailure(task, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during task execution: {}", taskId, e);
            handleTaskFailure(task, "系统错误: " + e.getMessage());
        } finally {
            // 清理：如果进程仍在运行，强制终止
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * 构建Perl命令
     */
    private String[] buildPerlCommand(Task task, String outputDir) {
        // 使用配置的脚本目录
        String scriptFullPath = scriptDirectory.isEmpty() ?
                scriptPath :
                Paths.get(scriptDirectory, scriptPath).toString();

        return new String[]{
                "perl",
                scriptFullPath,
                "--seq=" + task.getInputSequence(),
                "--type=" + task.getSequenceType().name().toLowerCase(),
                "--species=" + task.getTargetSpecies().name().toLowerCase(),
                "--output=" + outputDir
        };
    }

    /**
     * 获取进程ID（Java 9+有更可靠的方法，这里使用简化版）
     */
    private int getProcessId(Process process) {
        // Java 9+ 可以使用 process.pid()
        try {
            // 使用反射获取PID（兼容Java 8）
            java.lang.reflect.Field pidField = process.getClass().getDeclaredField("pid");
            pidField.setAccessible(true);
            return pidField.getInt(process);
        } catch (Exception e) {
            log.warn("Could not get process PID", e);
            return 0;
        }
    }

    /**
     * 读取进程输出
     */
    private String readProcessOutput(Process process) {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        } catch (Exception e) {
            log.error("Error reading process output", e);
        }
        return output.toString();
    }

    /**
     * 读取结果摘要
     */
    private Map<String, Object> readResultSummary(String resultDir) throws Exception {
        File summaryFile = new File(resultDir, TaskConstants.SUMMARY_FILE);

        if (summaryFile.exists()) {
            String summaryContent = FileUtil.readFile(summaryFile.getAbsolutePath());
            return objectMapper.readValue(summaryContent, HashMap.class);
        } else {
            // 如果没有summary.json，返回基本结果
            Map<String, Object> summary = new HashMap<>();
            summary.put("status", "completed");
            summary.put("message", "优化完成");
            return summary;
        }
    }

    /**
     * 处理任务失败
     */
    private void handleTaskFailure(Task task, String errorMessage) {
        try {
            taskRepository.updateTaskFailed(
                    task.getTaskId(),
                    TaskStatus.FAILED.name(),
                    LocalDateTime.now(),
                    errorMessage
            );

            // 发送失败通知
            notificationService.notifyTaskFailure(task, errorMessage);
        } catch (Exception e) {
            log.error("Failed to update task status or send failure notification for task: {}",
                    task.getTaskId(), e);
        }
    }

    /**
     * 终止任务进程
     *
     * @param pid 进程ID
     */
    public void terminateTask(int pid) {
        try {
            // Windows和Unix系统使用不同的命令
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            ProcessBuilder pb;

            if (isWindows) {
                pb = new ProcessBuilder("taskkill", "/F", "/PID", String.valueOf(pid));
            } else {
                pb = new ProcessBuilder("kill", "-9", String.valueOf(pid));
            }

            Process process = pb.start();
            process.waitFor();

            log.info("Process terminated: {}", pid);
        } catch (Exception e) {
            log.error("Failed to terminate process: {}", pid, e);
        }
    }

    /**
     * 检查任务状态并更新（由调度器调用）
     * 这个方法定期检查运行中的任务，查看脚本是否执行完成
     *
     * @param task 任务对象
     */
    public void checkAndUpdateTaskStatus(Task task) {
        if (task.getStatus() != TaskStatus.RUNNING) {
            return;
        }

        String taskId = task.getTaskId();
        String taskResultDir = Paths.get(resultDirectory, "task_" + taskId).toString();
        File statusFile = new File(taskResultDir, "run.txt");

        // 检查状态文件是否存在
        if (!statusFile.exists()) {
            log.debug("Status file not found for task: {}, task still running", taskId);
            return;
        }

        try {
            // 读取状态文件
            String statusContent = FileUtil.readFile(statusFile.getAbsolutePath());

            if (statusContent.contains("status:SUCCESS")) {
                // 任务成功完成
                log.info("Mock task completed successfully: {}", taskId);

                // 创建ZIP压缩包
                String zipFilePath = taskResultDir + ".zip";
                FileUtil.createZip(taskResultDir, zipFilePath);

                // 读取并生成结果摘要
                Map<String, Object> resultSummary = readMockResultSummary(taskResultDir);

                // 更新任务状态为完成
                taskRepository.updateTaskCompleted(
                        taskId,
                        TaskStatus.COMPLETED.name(),
                        LocalDateTime.now(),
                        zipFilePath,
                        objectMapper.writeValueAsString(resultSummary)
                );

                // 发送完成通知
                File zipFile = new File(zipFilePath);
                notificationService.notifyTaskCompletion(task, zipFile);

            } else if (statusContent.contains("status:FAILED")) {
                // 任务失败
                log.warn("Mock task failed: {}", taskId);

                // 从状态文件中提取错误信息
                String errorMsg = "任务执行失败";
                String[] lines = statusContent.split("\n");
                for (String line : lines) {
                    if (line.startsWith("info:")) {
                        errorMsg = line.substring(5);
                        break;
                    }
                }

                // 更新任务状态为失败
                taskRepository.updateTaskFailed(
                        taskId,
                        TaskStatus.FAILED.name(),
                        LocalDateTime.now(),
                        errorMsg
                );

                // 发送失败通知
                notificationService.notifyTaskFailure(task, errorMsg);
            } else {
                log.debug("Task {} still running (status file exists but no completion status)", taskId);
            }

        } catch (Exception e) {
            log.error("Error checking task status for: {}", taskId, e);
        }
    }

    /**
     * 从结果文件中读取摘要信息
     */
    private Map<String, Object> readMockResultSummary(String resultDir) throws Exception {
        File resultFile = new File(resultDir, "result.txt");
        Map<String, Object> summary = new HashMap<>();

        if (resultFile.exists()) {
            String resultContent = FileUtil.readFile(resultFile.getAbsolutePath());
            String[] lines = resultContent.split("\n");

            for (String line : lines) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String key = parts[0];
                    String value = parts[1];
                    summary.put(key, value);
                }
            }
        }

        summary.put("status", "SUCCESS");
        summary.put("message", "密码子优化完成（模拟模式）");

        return summary;
    }
}
