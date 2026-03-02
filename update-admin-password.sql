-- ============================================
-- 管理员密码更新脚本
-- ============================================
-- 请选择以下其中一个密码，然后执行对应的SQL语句

-- 选项1：密码 = admin123 (原始密码，哈希可能有问题)
-- UPDATE users SET password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' WHERE email = 'admin@codonopt.com';

-- 选项2：密码 = Admin@123 (推荐：强度更高)
UPDATE users SET password_hash = '$2a$10$5DF/dT4W6VjPzW8kZXOqeOCFHt9SgGWdPhWYfNNCgeIhP8m3xqjLi' WHERE email = 'admin@codonopt.com';

-- 选项3：密码 = CodonOpt@2024
-- UPDATE users SET password_hash = '$2a$10$8K1p/a0dN3fZhVKWGvHRmOJjGNG8RzWGnGfPXmJzOYQhR5kLm9vDe' WHERE email = 'admin@codonopt.com';

-- 选项4：密码 = Test@123 (仅用于测试)
-- UPDATE users SET password_hash = '$2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRps.9cGLcZEiGDMVr5yUP1nTEkiC' WHERE email = 'admin@codonopt.com';

-- ============================================
-- 如果用户不存在，插入新的管理员用户
-- ============================================
-- INSERT INTO users (username, email, password_hash, is_verified, is_active, role)
-- VALUES ('admin', 'admin@codonopt.com', '$2a$10$5DF/dT4W6VjPzW8kZXOqeOCFHt9SgGWdPhWYfNNCgeIhP8m3xqjLi', TRUE, TRUE, 'ADMIN');

-- 更新后请使用以下凭据登录：
-- 邮箱：admin@codonopt.com
-- 密码：Admin@123 (选中选项2)
