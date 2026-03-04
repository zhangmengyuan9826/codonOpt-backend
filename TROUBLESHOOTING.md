# 任务调度问题排查指南

## 问题：任务一直停留在排队中（QUEUED）状态

### 原因分析

主要原因是调度器的定时间隔配置错误：
- **错误配置**: `@Scheduled(fixedDelay = 3000000)` = 50分钟
- **正确配置**: `@Scheduled(fixedDelay = 30000)` = 30秒

✅ **已修复**: 代码已更新为30秒间隔

---

## 快速测试步骤

### 1. 重启应用

```bash
# 停止当前运行的应用
# 然后重新启动
cd backend
mvn spring-boot:run
```

### 2. 提交测试任务

```bash
curl -X POST http://localhost:8018/api/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "taskName": "测试任务",
    "sequenceType": "AMINO_ACID",
    "targetSpecies": "HUMAN",
    "inputSequence": "MKLLILGLVSSLGSVAMAVTNGTH"
  }'
```

### 3. 立即查看任务状态

```bash
curl -X GET http://localhost:8018/api/debug/task-status \
  -H "Authorization: Bearer YOUR_TOKEN"
```

响应示例：
```json
{
  "QUEUED": 0,
  "RUNNING": 1,
  "COMPLETED": 0,
  "FAILED": 0,
  "runningTaskList": [
    {
      "taskId": "abc123",
      "taskName": "测试任务",
      "startedAt": "2025-03-04T17:30:00",
      "processPid": 12345
    }
  ]
}
```

### 4. 手动触发调度器（可选）

如果不想等待30秒，可以手动触发：

```bash
curl -X GET http://localhost:8018/api/debug/trigger-scheduler \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 检查调度器日志

在应用日志中查找以下关键日志：

### ✅ 正常运行的日志

```
2025-03-04 17:30:00 - ===== 调度器触发: 检查任务队列 =====
2025-03-04 17:30:00 - 发现排队任务: abc123 - 测试任务
2025-03-04 17:30:00 - 成功获取执行锁，开始处理任务: abc123
2025-03-04 17:30:00 - ========== 开始处理任务 ==========
2025-03-04 17:30:00 - 任务ID: abc123
2025-03-04 17:30:00 - 更新任务状态为 RUNNING
2025-03-04 17:30:00 - 任务状态更新成功，开始异步执行
2025-03-04 17:30:00 - ========== 任务处理完成 ==========
2025-03-04 17:30:00 - ===== 调度器检查完成 =====
```

### ❌ 有问题的日志

```
# 调度器没有触发
（找不到 "===== 调度器触发 =====" 日志）

# 或者调度器触发了但没有找到任务
2025-03-04 17:30:00 - ===== 调度器触发: 检查任务队列 =====
2025-03-04 17:30:00 - 队列中没有待处理的任务

# 或者任务一直处于运行状态
2025-03-04 17:30:00 - 任务正在运行中: abc123 (PID: 12345)
```

---

## 常见问题排查

### 问题1: 调度器没有运行

**检查点**:
1. 确认 `SchedulerConfig.java` 有 `@EnableScheduling` 注解
2. 确认 `AsyncConfig.java` 有 `@EnableAsync` 注解
3. 查看应用启动日志是否有错误

**验证方法**:
```bash
# 查看所有配置的Bean
curl http://localhost:8018/actuator/beans
```

### 问题2: 任务没有被创建到数据库

**检查点**:
1. 查看任务提交时的日志
2. 检查数据库中 `task` 表
3. 查看是否有异常信息

**验证方法**:
```sql
SELECT * FROM task WHERE status = 'QUEUED' ORDER BY submitted_at DESC;
```

### 问题3: 调度器运行但任务状态没有更新

**检查点**:
1. 检查 `TaskRepository.updateTaskStatus()` 是否正常
2. 检查数据库连接
3. 查看是否有事务问题

**验证方法**:
```bash
# 手动触发调度器
curl http://localhost:8018/api/debug/trigger-scheduler

# 查看任务状态
curl http://localhost:8018/api/debug/task-status
```

### 问题4: 脚本没有启动

**检查点**:
1. 查看脚本路径是否正确
2. 检查脚本是否有执行权限
3. 查看错误日志

**验证方法**:
```bash
# Windows
dir scripts\
type scripts\mock_task_executor.bat

# Linux/Unix
ls -la scripts/
cat scripts/mock_task_executor.sh
```

---

## 调试端点说明

### 1. `/api/debug/trigger-scheduler`
手动触发调度器处理下一个任务

**响应示例**:
```json
{
  "success": true,
  "message": "调度器已触发",
  "queuedTaskCount": 1,
  "runningTaskCount": 0,
  "nextTaskId": "abc123",
  "nextTaskName": "测试任务"
}
```

### 2. `/api/debug/task-status`
查看所有任务的状态统计

**响应示例**:
```json
{
  "success": true,
  "QUEUED": 2,
  "RUNNING": 1,
  "COMPLETED": 5,
  "FAILED": 0,
  "queuedTaskList": [
    {
      "taskId": "def456",
      "taskName": "任务2",
      "submittedAt": "2025-03-04T17:31:00",
      "queuePosition": 1
    }
  ],
  "runningTaskList": [
    {
      "taskId": "abc123",
      "taskName": "任务1",
      "startedAt": "2025-03-04T17:30:00",
      "processPid": 12345
    }
  ]
}
```

---

## 完整测试流程

### Step 1: 启动应用并查看日志

```bash
mvn spring-boot:run

# 查看日志中是否有以下信息：
# - "CodonTaskScheduler" bean已创建
# - "启用定时任务" 相关信息
# - "启用异步执行" 相关信息
```

### Step 2: 提交任务

```bash
curl -X POST http://localhost:8018/api/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d @- <<'EOF'
{
  "taskName": "调试测试任务",
  "sequenceType": "AMINO_ACID",
  "targetSpecies": "HUMAN",
  "inputSequence": "MKLLILGLVSSLGSVAMAVTNGTH"
}
EOF
```

### Step 3: 等待30秒或手动触发

```bash
# 等待30秒让调度器自动触发
sleep 30

# 或者手动触发
curl http://localhost:8018/api/debug/trigger-scheduler
```

### Step 4: 检查任务状态

```bash
curl http://localhost:8018/api/debug/task-status

# 检查前端页面
curl http://localhost:8018/api/tasks
```

### Step 5: 查看脚本执行

```bash
# Windows
cd C:\Users\15651\Documents\huoyan\kaifa\codonOpt\analyze\taskPath
dir /s task_*

# Linux/Unix
cd /path/to/taskPath
ls -la task_*/
cat task_*/execution.log
```

---

## 时间线示例

```
17:30:00 - 提交任务（状态：QUEUED）
17:30:30 - 调度器触发，任务开始运行（状态：RUNNING）
17:30:31 - 脚本启动，在后台执行
17:35:31 - 脚本执行完成（5分钟）
17:35:32 - 调度器检测到完成状态
17:35:33 - 更新数据库（状态：COMPLETED）
17:35:34 - 用户刷新页面看到完成状态
```

---

## 修改调度间隔（如需更频繁检查）

如果需要更频繁地检查任务（例如每5秒），修改 `CodonTaskScheduler.java`:

```java
@Scheduled(fixedDelay = 5000)  // 5秒
public void checkAndProcessNextTask() {
```

**注意**: 太频繁的检查会增加数据库负载。

---

## 性能优化建议

1. **减少日志级别**: 生产环境将调度器相关日志改为 DEBUG
2. **增加检查间隔**: 从30秒改为1-5分钟
3. **使用索引**: 确保数据库的 status 字段有索引
4. **连接池优化**: 增加 HikariCP 连接池大小

---

## 仍然无法解决？

如果问题仍然存在，请提供以下信息：

1. 应用启动日志（完整）
2. 提交任务时的日志
3. `/api/debug/task-status` 的响应
4. 数据库 task 表的内容（`SELECT * FROM task`）
5. 操作系统和Java版本
