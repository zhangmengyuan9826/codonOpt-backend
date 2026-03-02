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
     *
     * @param task 任务对象
     */
    private void executeTaskMock(Task task) {
        String taskId = task.getTaskId();
        log.info("Executing task in MOCK mode: {}", taskId);

        try {
            // 1. 创建任务结果目录
            String taskResultDir = Paths.get(resultDirectory, "task_" + taskId).toString();
            FileUtil.createDirectoryIfNotExists(taskResultDir);

            LocalDateTime startTime = LocalDateTime.now();
            log.info("Task started at: {}", startTime);

            // 2. 模拟处理延迟（随机3-10秒）
            int delaySeconds = 3000 + (int) (Math.random() * 7000);
            log.info("Simulating processing delay: {}ms", delaySeconds);
            Thread.sleep(delaySeconds);

            // 3. 模拟随机成功或失败（90%成功率）
            boolean isSuccess = Math.random() < 0.9;
            LocalDateTime endTime = LocalDateTime.now();

            if (isSuccess) {
                // 成功场景：生成结果文件
                generateMockSuccessFiles(task, taskResultDir, startTime, endTime);

                // 创建ZIP压缩包
                String zipFilePath = taskResultDir + ".zip";
                FileUtil.createZip(taskResultDir, zipFilePath);

                // 生成结果摘要
                Map<String, Object> resultSummary = generateMockResultSummary(task);

                // 更新任务状态为完成
                taskRepository.updateTaskCompleted(
                        taskId,
                        TaskStatus.COMPLETED,
                        endTime,
                        zipFilePath,
                        objectMapper.writeValueAsString(resultSummary)
                );

                log.info("Task completed successfully: {}", taskId);

                // 发送完成通知
                File zipFile = new File(zipFilePath);
                notificationService.notifyTaskCompletion(task, zipFile);

            } else {
                // 失败场景：生成失败信息
                String errorMsg = "模拟分析失败：序列格式不符合" + task.getTargetSpecies().getDisplayName() + "的优化要求，" +
                        "请检查输入序列是否包含非法字符或序列长度是否在允许范围内。";
                generateMockFailureFiles(task, taskResultDir, startTime, endTime, errorMsg);

                // 更新任务状态为失败
                taskRepository.updateTaskFailed(
                        taskId,
                        TaskStatus.FAILED,
                        endTime,
                        errorMsg
                );

                log.warn("Task failed in mock mode: {}", taskId);

                // 发送失败通知
                notificationService.notifyTaskFailure(task, errorMsg);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Task execution interrupted: {}", taskId, e);
            handleTaskFailure(task, "任务执行被中断");
        } catch (Exception e) {
            log.error("Unexpected error during mock task execution: {}", taskId, e);
            handleTaskFailure(task, "系统错误: " + e.getMessage());
        }
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
                    TaskStatus.COMPLETED,
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
                    TaskStatus.FAILED,
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
}
