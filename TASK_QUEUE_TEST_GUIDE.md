# 任务队列功能测试指南

## 概述

系统已改造为使用外部脚本异步执行任务，用户提交任务后立即返回，任务在后台运行5分钟后完成。

## 快速测试步骤

### 1. 启动后端服务

```bash
cd backend
mvn spring-boot:run
```

### 2. 提交测试任务

使用 Postman 或 curl 提交任务：

```bash
curl -X POST http://localhost:8018/api/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "taskName": "测试任务1",
    "sequenceType": "AMINO_ACID",
    "targetSpecies": "HUMAN",
    "inputSequence": "MKLLILGLVSSLGSVAMAVTNGTH",
    "parameters": {
      "gcContent": 50,
      "caiThreshold": 0.8
    }
  }'
```

### 3. 观察立即返回

提交后应立即收到响应：

```json
{
  "code": 200,
  "message": "任务提交成功",
  "data": {
    "taskId": "abc123-def456-ghi789",
    "status": "QUEUED",
    "queuePosition": 1
  }
}
```

### 4. 查看任务状态

```bash
# 查看任务列表
curl -X GET http://localhost:8018/api/tasks \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# 查看任务详情
curl -X GET http://localhost:8018/api/tasks/{taskId} \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

状态变化：
- `QUEUED` → 排队中（0-30秒）
- `RUNNING` → 运行中（5分钟）
- `COMPLETED` 或 `FAILED` → 完成

### 5. 提交多个任务测试队列

快速提交3个任务：

```bash
# 任务1
curl -X POST http://localhost:8018/api/tasks \
  -H "Authorization: Bearer TOKEN" \
  -d '{"taskName":"任务1","inputSequence":"MKLLILGLV"}'

# 任务2（立即提交）
curl -X POST http://localhost:8018/api/tasks \
  -H "Authorization: Bearer TOKEN" \
  -d '{"taskName":"任务2","inputSequence":"MKLLILGLV"}'

# 任务3（立即提交）
curl -X POST http://localhost:8018/api/tasks \
  -H "Authorization: Bearer TOKEN" \
  -d '{"taskName":"任务3","inputSequence":"MKLLILGLV"}'
```

观察：
- 任务1: `QUEUED` → `RUNNING` → `COMPLETED` (5分钟)
- 任务2: `QUEUED` (排队) → `RUNNING` → `COMPLETED` (再等5分钟)
- 任务3: `QUEUED` (排队第2位) → ...

### 6. 查看后台执行日志

#### Windows
```powershell
# 查看任务目录
cd C:\Users\15651\Documents\huoyan\kaifa\codonOpt\analyze\taskPath

# 查看执行日志
type task_abc123\execution.log

# 查看状态文件
type task_abc123\run.txt
```

#### Linux/Unix
```bash
# 查看任务目录
cd /path/to/taskPath

# 查看执行日志
tail -f task_abc123/execution.log

# 查看状态文件
cat task_abc123/run.txt
```

### 7. 验证结果文件

成功场景的文件：
```
task_abc123/
├── execution.log    # 执行日志
├── run.txt         # 状态文件
├── result.txt      # 优化结果
└── task_abc123.zip # 压缩包
```

失败场景的文件：
```
task_abc123/
├── execution.log   # 执行日志
├── run.txt        # 状态文件（FAILED）
├── error.txt      # 错误信息
└── result.txt     # 空文件
```

## 前端测试

### 1. 启动前端服务

```bash
cd frontend
python -m http.server 8080
# 或使用其他静态服务器
```

### 2. 访问应用

打开浏览器访问: `http://localhost:8080`

### 3. 测试流程

1. 登录系统
2. 进入"序列优化"页面
3. 输入序列: `MKLLILGLVSSLGSVAMAVTNGTH`
4. 点击"开始优化"
5. 观察立即返回的提示
6. 进入"历史任务"页面查看状态
7. 刷新页面观察状态变化（排队 → 运行中 → 完成）

## 验证点

### ✅ 立即返回
- [ ] 提交任务后1秒内收到响应
- [ ] 响应中包含任务ID和状态
- [ ] 前端显示"任务已提交"提示

### ✅ 队列功能
- [ ] 第一个任务自动开始运行
- [ ] 后续任务显示排队位置
- [ ] 前一任务完成后，后一任务自动开始

### ✅ 异步执行
- [ ] 任务状态为RUNNING时，脚本在后台运行
- [ ] 可以提交新任务（不影响运行中的任务）
- [ ] 用户可以继续使用其他功能

### ✅ 状态更新
- [ ] 每30秒检查一次任务状态
- [ ] 脚本完成后自动更新数据库
- [ ] 前端刷新后显示最新状态

### ✅ 结果文件
- [ ] 成功任务生成ZIP压缩包
- [ ] 失败任务显示错误信息
- [ ] 可以下载结果文件

## 常见问题

### Q: 任务一直停留在QUEUED状态？
**A**: 检查：
1. 调度器是否正常工作（查看日志）
2. 是否有其他任务正在运行
3. 数据库连接是否正常

### Q: 任务停留在RUNNING状态超过5分钟？
**A**: 检查：
1. 脚本是否正常启动
2. 查看execution.log确认脚本是否在运行
3. 检查超时设置

### Q: 如何强制停止运行中的任务？
**A**: 目前只能通过：
1. 等待脚本自然完成（5分钟）
2. 或修改代码添加取消功能

### Q: 可以缩短执行时间用于测试吗？
**A**: 可以修改脚本中的 `STEP_DURATION` 变量：
- Shell: `STEP_DURATION=5` (改为5秒)
- Batch: `set STEP_DURATION=5`
- 总时间 = 10个步骤 × STEP_DURATION

## 性能测试

### 并发测试

使用 Apache Bench 或 JMeter 进行并发测试：

```bash
# 10个并发用户，总共提交20个任务
ab -n 20 -c 10 -p task.json -T application/json \
  -H "Authorization: Bearer TOKEN" \
  http://localhost:8018/api/tasks
```

预期结果：
- 所有请求在1秒内返回
- 任务按提交顺序排队执行
- 系统稳定，无崩溃

## 监控要点

1. **响应时间**: 提交接口应 < 1秒
2. **队列长度**: 正常情况 < 5个
3. **执行时间**: 约5分钟（可配置）
4. **成功率**: 约90%（可配置）
5. **系统资源**: CPU和内存使用正常

## 下一步

测试通过后，可以：
1. 部署到服务器
2. 配置真实的Perl脚本路径
3. 修改 `mock-mode: false` 启用真实执行
4. 调整超时和并发参数
