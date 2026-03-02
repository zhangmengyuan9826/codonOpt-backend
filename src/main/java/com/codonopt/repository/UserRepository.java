package com.codonopt.repository;

import com.codonopt.entity.User;
import com.codonopt.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * 根据ID查找用户
     */
    Optional<User> findById(Long id);
    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 验证用户邮箱
     */
    @Modifying
    @Query("UPDATE User u SET u.isVerified = true, u.verificationCode = null, u.verificationCodeExpiry = null WHERE u.email = :email AND u.verificationCode = :code AND u.verificationCodeExpiry > :now")
    int verifyUser(@Param("email") String email, @Param("code") String code, @Param("now") LocalDateTime now);

    /**
     * 更新用户最后登录时间
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginAt WHERE u.id = :userId")
    int updateLastLoginAt(@Param("userId") Long userId, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    /**
     * 设置验证码
     */
    @Modifying
    @Query("UPDATE User u SET u.verificationCode = :code, u.verificationCodeExpiry = :expiry WHERE u.email = :email")
    int setVerificationCode(@Param("email") String email, @Param("code") String code, @Param("expiry") LocalDateTime expiry);

    /**
     * 查找指定角色的用户
     */
    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findByRole(@Param("role") UserRole role);

    /**
     * 统计管理员数量
     */
    long countByRole(UserRole role);

    /**
     * 统计已激活用户数量
     */
    long countByIsActiveTrue();

    /**
     * 统计已验证用户数量
     */
    long countByIsVerifiedTrue();

    /**
     * 按创建时间降序查找所有用户
     */
    List<User> findAllByOrderByCreatedAtDesc();

    /**
     * 搜索用户（用户名或邮箱包含关键字）
     */
    @Query("SELECT u FROM User u WHERE u.username LIKE %:search% OR u.email LIKE %:search%")
    List<User> findByUsernameContainingOrEmailContaining(@Param("search") String username, @Param("search") String email);
}
