-- 修复卡住的任务状态
-- Usage: mysql -u root -p12345678 -D codon_opt < fix_stuck_tasks.sql

-- 查看所有RUNNING状态的任务
SELECT task_id, task_name, status, started_at,
       TIMESTAMPDIFF(MINUTE, started_at, NOW()) as running_minutes
FROM tasks
WHERE status = 'RUNNING'
ORDER BY started_at DESC;

-- 将运行超过10分钟的任务标记为失败（根据需要调整时间）
UPDATE tasks
SET status = 'FAILED',
    completed_at = NOW(),
    error_message = 'Task timeout - manually marked as failed'
WHERE status = 'RUNNING'
  AND started_at < DATE_SUB(NOW(), INTERVAL 10 MINUTE);

-- 或者：将运行超过10分钟的任务标记为完成（如果你知道它们实际上已完成）
UPDATE tasks
SET status = 'COMPLETED',
    completed_at = started_at + INTERVAL 5 MINUTE
WHERE status = 'RUNNING'
  AND started_at < DATE_SUB(NOW(), INTERVAL 10 MINUTE);

-- 查看更新后的状态
SELECT task_id, task_name, status, started_at, completed_at
FROM tasks
WHERE status IN ('COMPLETED', 'FAILED')
ORDER BY completed_at DESC
LIMIT 10;
