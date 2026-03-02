package com.codonopt.exception;

/**
 * 任务执行异常
 */
public class TaskExecutionException extends RuntimeException {

    public TaskExecutionException(String message) {
        super(message);
    }

    public TaskExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
