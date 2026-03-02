package com.codonopt.enums;

/**
 * 任务状态枚举
 */
public enum TaskStatus {
    /**
     * 排队中
     */
    QUEUED,

    /**
     * 运行中
     */
    RUNNING,

    /**
     * 已完成
     */
    COMPLETED,

    /**
     * 失败
     */
    FAILED,

    /**
     * 已取消
     */
    CANCELLED
}
