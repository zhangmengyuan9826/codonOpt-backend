package com.codonopt.constants;

/**
 * 任务相关常量
 */
public class TaskConstants {

    /**
     * 任务超时时间（毫秒）- 默认2小时
     */
    public static final long TASK_TIMEOUT_MS = 7200000;

    /**
     * 任务结果文件名
     */
    public static final String OUTPUT_FILE = "output.fa";
    public static final String STATUS_LOG = "status.log";
    public static final String ERROR_LOG = "error.log";
    public static final String SUMMARY_FILE = "summary.json";

    /**
     * 模拟模式结果文件名
     */
    public static final String RUN_FILE = "run.txt";
    public static final String RESULT_FILE = "result.txt";

    /**
     * 任务状态日志标识
     */
    public static final String STATUS_SUCCESS = "[SUCCESS]";
    public static final String STATUS_ERROR = "[ERROR]";
    public static final String STATUS_RUNNING = "[RUNNING]";

    /**
     * 序列长度限制
     */
    public static final int MIN_SEQUENCE_LENGTH = 10;
    public static final int MAX_SEQUENCE_LENGTH = 10000;

    /**
     * 文件大小限制（字节）
     */
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
}
