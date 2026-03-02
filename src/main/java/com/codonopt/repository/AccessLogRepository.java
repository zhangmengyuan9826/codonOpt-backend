package com.codonopt.repository;

import com.codonopt.entity.AccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 访问日志数据访问接口
 */
@Repository
public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    /**
     * 根据IP地址查找访问日志
     */
    List<AccessLog> findByIpAddressOrderByRequestTimeDesc(String ipAddress);

    /**
     * 根据用户ID查找访问日志
     */
    List<AccessLog> findByUserIdOrderByRequestTimeDesc(Long userId);

    /**
     * 统计指定时间范围内的请求数量
     */
    long countByIpAddressAndRequestTimeAfter(String ipAddress, LocalDateTime after);

    /**
     * 统计IP在指定时间内的失败请求次数
     */
    @Query("SELECT COUNT(a) FROM AccessLog a WHERE a.ipAddress = :ipAddress AND a.requestTime > :after AND a.statusCode >= 400")
    long countFailedRequestsByIp(@Param("ipAddress") String ipAddress, @Param("after") LocalDateTime after);

    /**
     * 查找最近的访问日志
     */
    @Query("SELECT a FROM AccessLog a ORDER BY a.requestTime DESC")
    List<AccessLog> findRecentAccessLogs();

    /**
     * 统计指定时间范围内的总请求数
     */
    long countByRequestTimeAfter(LocalDateTime after);

    /**
     * 删除指定日期之前的日志
     */
    void deleteByRequestTimeBefore(LocalDateTime before);
}
