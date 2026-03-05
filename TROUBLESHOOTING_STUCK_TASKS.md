# 卡住任务处理指南

## 问题现象
- 任务在后台执行完成
- MySQL 数据库中状态已是 COMPLETED
- 但任务队列显示状态仍是 RUNNING
- 调度器无法自动更新状态

## 原因分析
1. 脚本进程已结束，但 Java 应用未能正确检测到
2. 调度器的 `checkRunningTasksStatus()` 未被触发
3. 结果文件 `run.txt` 未生成或格式不正确
4. 数据库更新失败但未记录错误

## 快速修复方案

### 方案1: 直接更新单个任务（推荐）

```bash
# 替换 <TASK_ID> 为实际的任务ID
mysql -u root -p12345678 -D codon_opt -e "UPDATE tasks SET status = 'COMPLETED', completed_at = NOW() WHERE task_id = '<TASK_ID>' AND status = 'RUNNING';"
```

### 方案2: 使用修复脚本

```bash
# Windows
fix_stuck_tasks.bat

# Linux/Unix
mysql -u root -p12345678 -D codon_opt < fix_stuck_tasks.sql
```

### 方案3: 查看并批量修复

```sql
-- 查看所有运行中的任务
SELECT task_id, task_name, status, started_at,
       TIMESTAMPDIFF(MINUTE, started_at, NOW()) as running_minutes
FROM tasks
WHERE status = 'RUNNING'
ORDER BY started_at DESC;

-- 将运行超过10分钟的任务标记为完成
UPDATE tasks
SET status = 'COMPLETED', completed_at = NOW()
WHERE status = 'RUNNING'
  AND started_at < DATE_SUB(NOW(), INTERVAL 10 MINUTE);
```

### 方案4: 重启应用（最后手段）

```bash
# 停止Java进程
taskkill /F /IM java.exe

# 重新启动
mvn spring-boot:run
```

## 预防措施

### 1. 检查调度器是否正常工作

查看日志中是否有以下内容：
```
===== 调度器触发: 检查任务队列 =====
检查状态 of X running tasks
```

### 2. 检查脚本执行结果

查看任务目录中的文件：
```
task_<TASK_ID>/
├── execution.log   - 执行日志
├── run.txt         - 必须包含 status:SUCCESS 或 status:FAILED
├── result.txt      - 结果数据
└── error.txt       - 错误信息（失败时）
```

### 3. 验证 `run.txt` 格式

正确格式：
```
startedTime:2026-03-05 10:00:00
completedTime:2026-03-05 10:05:00
status:SUCCESS
info:...
```

### 4. 增加调度器日志

在 `application.yml` 中调整日志级别：
```yaml
logging:
  level:
    com.codonopt.service.task: DEBUG
```

## 常见问题排查

### Q1: 为什么调度器没有检测到任务完成？

**检查项：**
1. `run.txt` 文件是否存在
2. `run.txt` 是否包含 `status:SUCCESS` 或 `status:FAILED`
3. 调度器定时任务是否正常运行（30秒间隔）
4. 是否有异常日志

### Q2: 脚本正在运行但想强制停止

**Windows:**
```bash
tasklist | findstr cmd.exe
taskkill /F /PID <PID>
```

**Linux/Unix:**
```bash
ps aux | grep mock_task_executor
kill -9 <PID>
```

### Q3: 如何批量清理所有卡住的任务

```sql
-- 查看所有RUNNING任务及其运行时长
SELECT task_id,
       task_name,
       started_at,
       TIMESTAMPDIFF(MINUTE, started_at, NOW()) as running_minutes
FROM tasks
WHERE status = 'RUNNING';

-- 标记所有超过6小时的任务为失败
UPDATE tasks
SET status = 'FAILED',
    completed_at = NOW(),
    error_message = 'Task timeout - marked as failed after 6 hours'
WHERE status = 'RUNNING'
  AND started_at < DATE_SUB(NOW(), INTERVAL 6 HOUR);
```

## 监控建议

### 1. 创建监控SQL

```sql
-- 查看任务状态统计
SELECT
    status,
    COUNT(*) as count,
    AVG(TIMESTAMPDIFF(MINUTE, started_at, completed_at)) as avg_duration_minutes
FROM tasks
WHERE submitted_at > DATE_SUB(NOW(), INTERVAL 1 DAY)
GROUP BY status;

-- 查找可能卡住的任务（运行超过30分钟）
SELECT task_id, task_name, started_at,
       TIMESTAMPDIFF(MINUTE, started_at, NOW()) as running_minutes
FROM tasks
WHERE status = 'RUNNING'
  AND started_at < DATE_SUB(NOW(), INTERVAL 30 MINUTE);
```

### 2. 设置自动清理（可选）

可以通过 MySQL 事件定时清理：
```sql
DELIMITER //
CREATE EVENT clean_stuck_tasks
ON SCHEDULE EVERY 1 HOUR
DO
BEGIN
    UPDATE tasks
    SET status = 'FAILED',
        completed_at = NOW(),
        error_message = 'Auto-cleanup: Task timeout after 1 hour'
    WHERE status = 'RUNNING'
      AND started_at < DATE_SUB(NOW(), INTERVAL 1 HOUR);
END//
DELIMITER ;

-- 启用事件调度器
SET GLOBAL event_scheduler = ON;
```

## 快速命令参考

```bash
# 查看当前任务状态
curl -s http://localhost:8018/api/debug/task-status

# 手动触发调度器
curl -s http://localhost:8018/api/debug/trigger-scheduler

# 查看特定任务详情
curl -s http://localhost:8018/api/tasks/<TASK_ID> \
  -H "Authorization: Bearer YOUR_TOKEN"

# 重置特定任务状态
mysql -u root -p12345678 -D codon_opt \
  -e "UPDATE tasks SET status = 'COMPLETED' WHERE task_id = '<TASK_ID>'"
```
