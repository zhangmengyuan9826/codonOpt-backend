-- 将卡住的任务状态更新为COMPLETED
UPDATE tasks 
SET status = 'COMPLETED', 
    completed_at = NOW()
WHERE task_id = 'e61df87d-08c8-4d93-84ca-a3ea4321ae78' 
  AND status = 'RUNNING';

-- 查看更新结果
SELECT task_id, task_name, status, started_at, completed_at 
FROM tasks 
WHERE task_id = 'e61df87d-08c8-4d93-84ca-a3ea4321ae78';
