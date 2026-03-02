-- 使用标准BCrypt测试向量更新管理员密码
-- 密码: admin123
-- 哈希: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

USE codon_opt;

UPDATE users
SET password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
WHERE email = 'admin@codonopt.com';

-- 验证更新
SELECT id, username, email, password_hash, is_active, is_verified, role FROM users;
