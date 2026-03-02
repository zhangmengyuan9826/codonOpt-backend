-- 添加任务名称字段到 tasks 表
-- Migration: Add task_name and codon_frequency_file_path columns
-- Date: 2025-02-26

-- 如果数据库不存在则创建
CREATE DATABASE IF NOT EXISTS codon_opt
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE codon_opt;

-- 检查并添加 task_name 列
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'codon_opt'
    AND TABLE_NAME = 'tasks'
    AND COLUMN_NAME = 'task_name'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE tasks ADD COLUMN task_name VARCHAR(200) NOT NULL DEFAULT ''未命名任务'' AFTER user_id',
    'SELECT ''Column task_name already exists'' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为 task_name 添加索引
SET @index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = 'codon_opt'
    AND TABLE_NAME = 'tasks'
    AND INDEX_NAME = 'idx_task_name'
);

SET @sql = IF(@index_exists = 0,
    'ALTER TABLE tasks ADD INDEX idx_task_name (task_name)',
    'SELECT ''Index idx_task_name already exists'' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 codon_frequency_file_path 列
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'codon_opt'
    AND TABLE_NAME = 'tasks'
    AND COLUMN_NAME = 'codon_frequency_file_path'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE tasks ADD COLUMN codon_frequency_file_path VARCHAR(500) DEFAULT NULL AFTER result_files_path',
    'SELECT ''Column codon_frequency_file_path already exists'' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 更新现有任务的默认名称
UPDATE tasks
SET task_name = CONCAT('任务_', LEFT(task_id, 8))
WHERE task_name = '未命名任务' OR task_name IS NULL;

-- 显示迁移完成信息
SELECT 'Migration completed successfully!' AS message;
SELECT COUNT(*) AS total_tasks_updated FROM tasks;
