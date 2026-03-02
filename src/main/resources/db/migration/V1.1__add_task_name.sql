-- 添加任务名称字段到 tasks 表
-- Migration: V1.1
-- Description: Add task_name column to tasks table

USE codon_opt;

-- 添加 task_name 列
ALTER TABLE tasks
ADD COLUMN task_name VARCHAR(200) NOT NULL DEFAULT '未命名任务' AFTER user_id,
ADD COLUMN codon_frequency_file_path VARCHAR(500) DEFAULT NULL AFTER result_files_path;

-- 为 task_name 添加索引以提高搜索性能
ALTER TABLE tasks
ADD INDEX idx_task_name (task_name);

-- 更新现有任务的默认名称（可选）
UPDATE tasks
SET task_name = CONCAT('任务_', LEFT(task_id, 8))
WHERE task_name = '未命名任务';
