package com.codonopt.service.security;

import com.codonopt.constants.SecurityConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 频率限制服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 检查用户是否超出每日任务提交限制
     *
     * @param userId 用户ID
     * @param limit  每日限制数量
     * @return true-未超限，false-已超限
     */
    public boolean checkDailyLimit(Long userId, int limit) {
        String key = buildRateLimitKey(userId);
        String count = redisTemplate.opsForValue().get(key);

        if (count == null) {
            // 第一次提交或新的一天
            return true;
        }

        int currentCount = Integer.parseInt(count);
        return currentCount < limit;
    }

    /**
     * 增加用户今日提交计数
     *
     * @param userId 用户ID
     * @return 当前的计数
     */
    public int incrementCounter(Long userId) {
        String key = buildRateLimitKey(userId);
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == null) {
            count = 1L;
        }

        // 设置过期时间为当天结束（秒为单位）
        long secondsUntilMidnight = getSecondsUntilMidnight();
        redisTemplate.expire(key, secondsUntilMidnight, TimeUnit.SECONDS);

        log.debug("User {} daily task count: {}", userId, count);
        return count.intValue();
    }

    /**
     * 获取用户今日已提交的任务数量
     *
     * @param userId 用户ID
     * @return 已提交数量
     */
    public int getTodayCount(Long userId) {
        String key = buildRateLimitKey(userId);
        String count = redisTemplate.opsForValue().get(key);
        return count != null ? Integer.parseInt(count) : 0;
    }

    /**
     * 重置用户今日计数（管理员功能）
     *
     * @param userId 用户ID
     */
    public void resetCounter(Long userId) {
        String key = buildRateLimitKey(userId);
        redisTemplate.delete(key);
        log.info("Reset daily task counter for user: {}", userId);
    }

    /**
     * 构建频率限制Redis键
     *
     * @param userId 用户ID
     * @return Redis键
     */
    private String buildRateLimitKey(Long userId) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        return SecurityConstants.RATE_LIMIT_KEY_PREFIX + userId + ":" + today;
    }

    /**
     * 计算距离今天结束还有多少秒
     *
     * @return 秒数
     */
    private long getSecondsUntilMidnight() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime tomorrow = now.toLocalDate().plusDays(1).atStartOfDay();
        return java.time.Duration.between(now, tomorrow).getSeconds();
    }

    /**
     * 检查IP是否被频率限制
     *
     * @param ipAddress IP地址
     * @return true-被限制，false-未被限制
     */
    public boolean isIpBlocked(String ipAddress) {
        String key = SecurityConstants.BLACKLIST_KEY_PREFIX + ipAddress;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 添加IP到黑名单
     *
     * @param ipAddress IP地址
     * @param durationMs 封禁时长（毫秒）
     */
    public void blockIp(String ipAddress, long durationMs) {
        String key = SecurityConstants.BLACKLIST_KEY_PREFIX + ipAddress;
        redisTemplate.opsForValue().set(key, "blocked", durationMs, TimeUnit.MILLISECONDS);
        log.warn("IP {} has been blocked for {} ms", ipAddress, durationMs);
    }

    /**
     * 从黑名单移除IP
     *
     * @param ipAddress IP地址
     */
    public void unblockIp(String ipAddress) {
        String key = SecurityConstants.BLACKLIST_KEY_PREFIX + ipAddress;
        redisTemplate.delete(key);
        log.info("IP {} has been unblocked", ipAddress);
    }
}
