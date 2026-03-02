package com.codonopt.exception;

/**
 * 频率限制异常
 */
public class RateLimitExceededException extends BusinessException {

    public RateLimitExceededException(String message) {
        super(429, message);
    }

    public RateLimitExceededException() {
        super(429, "已超出每日提交任务限制");
    }
}
