package com.codonopt.constants;

/**
 * 安全相关常量
 */
public class SecurityConstants {

    /**
     * JWT Token相关
     */
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String TOKEN_TYPE = "Bearer";

    /**
     * Token过期时间（毫秒）
     */
    public static final long ACCESS_TOKEN_EXPIRATION = 3600000; // 1小时
    public static final long REFRESH_TOKEN_EXPIRATION = 604800000; // 7天
    public static final long EMAIL_CODE_EXPIRATION = 300000; // 5分钟

    /**
     * 频率限制
     */
    public static final int DAILY_TASK_LIMIT = 10;
    public static final int RATE_LIMIT_BLOCK_DURATION = 86400000; // 24小时

    /**
     * Redis键前缀
     */
    public static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:user:";
    public static final String BLACKLIST_KEY_PREFIX = "blacklist:ip:";
    public static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";

    /**
     * 密码要求
     */
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_PASSWORD_LENGTH = 100;

    /**
     * 用户名长度限制
     */
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 50;

    /**
     * 验证码长度
     */
    public static final int VERIFICATION_CODE_LENGTH = 6;
}
