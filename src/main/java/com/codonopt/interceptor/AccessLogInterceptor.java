package com.codonopt.interceptor;

import com.codonopt.entity.AccessLog;
import com.codonopt.repository.AccessLogRepository;
import com.codonopt.util.IpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.CompletableFuture;

/**
 * 访问日志拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLogInterceptor implements HandlerInterceptor {

    private final AccessLogRepository accessLogRepository;

    private static final String START_TIME_ATTR = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 记录请求开始时间
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            // 计算响应时间
            Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
            int responseTime = startTime != null ?
                    (int) (System.currentTimeMillis() - startTime) : 0;

            // 获取用户ID
            Long userId = null;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Long) {
                userId = (Long) authentication.getPrincipal();
            }

            // 获取IP地址
            String ipAddress = IpUtil.getClientIpAddress(request);

            // 构建访问日志
            AccessLog accessLog = AccessLog.builder()
                    .ipAddress(ipAddress)
                    .userId(userId)
                    .endpoint(request.getRequestURI())
                    .method(request.getMethod())
                    .statusCode(response.getStatus())
                    .responseTimeMs(responseTime)
                    .userAgent(request.getHeader("User-Agent"))
                    .build();

            // 异步写入数据库
            CompletableFuture.runAsync(() -> {
                try {
                    accessLogRepository.save(accessLog);
                } catch (Exception e) {
                    log.error("Failed to save access log", e);
                }
            });

            // 记录访问日志（调试用）
            if (response.getStatus() >= 400) {
                log.warn("Request: {} {} from {} - Status: {}",
                        request.getMethod(),
                        request.getRequestURI(),
                        ipAddress,
                        response.getStatus()
                );
            }

        } catch (Exception e) {
            log.error("Error in access log interceptor", e);
        }
    }
}
