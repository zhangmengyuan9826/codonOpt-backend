package com.codonopt.service.security;

import com.codonopt.constants.SecurityConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * IP黑名单服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlacklistService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 检查IP是否在黑名单中
     *
     * @param ipAddress IP地址
     * @return true-在黑名单中，false-不在黑名单中
     */
    public boolean isBlacklisted(String ipAddress) {
        String key = SecurityConstants.BLACKLIST_KEY_PREFIX + ipAddress;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 添加IP到黑名单
     *
     * @param ipAddress IP地址
     * @param reason    封禁原因
     * @param durationMs 封禁时长（毫秒），0表示永久封禁
     */
    public void addToBlacklist(String ipAddress, String reason, long durationMs) {
        String key = SecurityConstants.BLACKLIST_KEY_PREFIX + ipAddress;
        if (durationMs > 0) {
            redisTemplate.opsForValue().set(key, reason, durationMs, TimeUnit.MILLISECONDS);
            log.warn("IP {} added to blacklist for {} ms. Reason: {}", ipAddress, durationMs, reason);
        } else {
            redisTemplate.opsForValue().set(key, reason);
            log.warn("IP {} added to blacklist permanently. Reason: {}", ipAddress, reason);
        }
    }

    /**
     * 从黑名单移除IP
     *
     * @param ipAddress IP地址
     */
    public void removeFromBlacklist(String ipAddress) {
        String key = SecurityConstants.BLACKLIST_KEY_PREFIX + ipAddress;
        redisTemplate.delete(key);
        log.info("IP {} removed from blacklist", ipAddress);
    }

    /**
     * 获取IP封禁原因
     *
     * @param ipAddress IP地址
     * @return 封禁原因，如果不在黑名单中返回null
     */
    public String getBlacklistReason(String ipAddress) {
        String key = SecurityConstants.BLACKLIST_KEY_PREFIX + ipAddress;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取所有黑名单IP
     *
     * @return 黑名单IP集合
     */
    public Set<String> getAllBlacklistedIps() {
        Set<String> keys = redisTemplate.keys(SecurityConstants.BLACKLIST_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return new HashSet<>();
        }
        Set<String> result = new HashSet<>();
        for (String key : keys) {
            result.add(key.substring(SecurityConstants.BLACKLIST_KEY_PREFIX.length()));
        }
        return result;
    }

    /**
     * 清空黑名单
     */
    public void clearBlacklist() {
        Set<String> keys = redisTemplate.keys(SecurityConstants.BLACKLIST_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Blacklist cleared, {} IPs removed", keys.size());
        }
    }
}
