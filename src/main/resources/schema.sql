-- Create database if not exists
CREATE DATABASE IF NOT EXISTS codon_opt
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE codon_opt;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    verification_code VARCHAR(10),
    verification_code_expiry TIMESTAMP NULL,
    role VARCHAR(20) DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL,
    INDEX idx_email (email),
    INDEX idx_username (username),
    INDEX idx_is_verified (is_verified)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tasks table
CREATE TABLE IF NOT EXISTS tasks (
    task_id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    task_name VARCHAR(200) NOT NULL DEFAULT '未命名任务',
    input_sequence TEXT NOT NULL,
    sequence_type VARCHAR(20) NOT NULL,
    target_species VARCHAR(50) NOT NULL,
    parameters JSON,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    queue_position INT DEFAULT 0,
    process_pid INT NULL,
    result_files_path VARCHAR(500),
    codon_frequency_file_path VARCHAR(500) DEFAULT NULL,
    result_summary JSON,
    error_message TEXT,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_submitted_at (submitted_at),
    INDEX idx_queue_position (queue_position),
    INDEX idx_task_name (task_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Access logs table
CREATE TABLE IF NOT EXISTS access_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ip_address VARCHAR(45) NOT NULL,
    user_id BIGINT NULL,
    endpoint VARCHAR(255) NOT NULL,
    method VARCHAR(10) NOT NULL,
    request_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status_code INT NOT NULL,
    response_time_ms INT NULL,
    user_agent VARCHAR(500),
    INDEX idx_ip_address (ip_address),
    INDEX idx_user_id (user_id),
    INDEX idx_request_time (request_time),
    INDEX idx_endpoint (endpoint),
    INDEX idx_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- System configuration table
CREATE TABLE IF NOT EXISTS sys_config (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value TEXT NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_description (description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default system configuration
INSERT INTO sys_config (config_key, config_value, description) VALUES
('daily_task_limit', '10', 'Maximum number of tasks a user can submit per day'),
('task_timeout_minutes', '120', 'Maximum time a task can run before being terminated'),
('max_concurrent_tasks', '1', 'Maximum number of tasks running simultaneously'),
('email_notifications_enabled', 'true', 'Whether email notifications are enabled'),
('maintenance_mode', 'false', 'Whether the system is in maintenance mode'),
('max_sequence_length', '10000', 'Maximum allowed sequence length'),
('allowed_file_types', 'fasta,txt', 'Allowed file types for sequence upload')
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- Create admin user (password: Admin@123 - should be changed immediately)
-- Password hash is for "Admin@123" using BCrypt (strength=10)
INSERT INTO users (username, email, password_hash, is_verified, is_active, role) VALUES
('admin', 'admin@codonopt.com', '$2a$10$5DF/dT4W6VjPzW8kZXOqeOCFHt9SgGWdPhWYfNNCgeIhP8m3xqjLi', TRUE, TRUE, 'ADMIN')
ON DUPLICATE KEY UPDATE email = email;
