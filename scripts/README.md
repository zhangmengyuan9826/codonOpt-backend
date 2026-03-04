# 任务执行脚本说明

本目录包含用于测试任务队列功能的模拟脚本。

## 脚本文件

### 1. mock_task_executor.sh (Linux/Unix)
用于在 Linux/Unix 系统上模拟任务执行的 Shell 脚本。

### 2. mock_task_executor.bat (Windows)
用于在 Windows 系统上模拟任务执行的批处理脚本。

## 功能特性

✅ **异步执行**: 脚本启动后立即返回，任务在后台运行
✅ **5分钟执行时间**: 模拟真实任务处理时间
✅ **进度记录**: 在执行日志中记录每个步骤的进度
✅ **结果文件生成**: 自动生成成功或失败的结果文件
✅ **90%成功率**: 随机模拟成功和失败场景
✅ **状态追踪**: 生成 run.txt 状态文件供 Java 程序检查

## 脚本参数

```bash
脚本名 <TASK_ID> <OUTPUT_DIR> <SEQUENCE_TYPE> <TARGET_SPECIES>
```

- `TASK_ID`: 任务唯一标识符
- `OUTPUT_DIR`: 结果文件输出目录
- `SEQUENCE_TYPE`: 序列类型（AMINO_ACID 或 DNA）
- `TARGET_SPECIES`: 目标物种（HUMAN, MOUSE, E_COLI 等）

## 生成的文件

### 成功场景
- `run.txt`: 包含开始时间、完成时间和状态（SUCCESS）
- `result.txt`: 包含优化序列、CAI值、GC含量、MFI值
- `execution.log`: 详细的执行日志

### 失败场景
- `run.txt`: 包含开始时间、完成时间和状态（FAILED）及错误信息
- `result.txt`: 空文件
- `error.txt`: 详细的错误信息和建议
- `execution.log`: 详细的执行日志

## 工作流程

```
用户提交任务
    ↓
Java 后端创建任务（状态：QUEUED）
    ↓
调度器检测到排队任务
    ↓
调用脚本启动任务（状态：RUNNING，立即返回）
    ↓
脚本在后台执行（5分钟）
    ↓
调度器每30秒检查任务状态
    ↓
检测到 run.txt 中的完成状态
    ↓
更新数据库并通知用户
```

## 配置

在 `application.yml` 中配置：

```yaml
app:
  perl:
    mock-mode: true  # 启用模拟模式
  mock-script:
    shell-script: scripts/mock_task_executor.sh
    batch-script: scripts\mock_task_executor.bat
    timeout: 600000  # 10分钟超时
```

## 使用场景

1. **开发测试**: 测试任务队列功能，无需等待长时间
2. **演示演示**: 展示系统异步处理能力
3. **压力测试**: 测试多任务并发处理
4. **故障模拟**: 通过修改脚本测试失败场景

## 自定义

### 修改执行时间

在脚本中修改以下变量：

- Shell: `STEP_DURATION=30` (每个步骤30秒)
- Batch: `set STEP_DURATION=30`

当前配置：10个步骤 × 30秒 = 5分钟

### 修改成功率

在脚本中修改随机数阈值：

- Shell: `if [ $RANDOM_NUMBER -lt 90 ]` (90%成功率)
- Batch: `if %RANDOM_NUMBER% lss 90` (90%成功率)

## 调试技巧

### Windows
1. 双击运行 `.bat` 文件查看执行过程
2. 检查 `execution.log` 查看详细日志
3. 查看任务结果目录中的文件

### Linux/Unix
```bash
# 直接运行脚本
./mock_task_executor.sh test123 /tmp/output AMINO_ACID HUMAN

# 查看日志
tail -f /tmp/output/execution.log

# 检查进程
ps aux | grep mock_task_executor
```

## 注意事项

⚠️ **Windows 兼容性**: 确保 Windows 脚本有执行权限
⚠️ **路径问题**: 使用绝对路径或相对于项目根目录的路径
⚠️ **权限问题**: 确保脚本对结果目录有写权限
⚠️ **超时设置**: 脚本执行时间应小于配置的超时时间

## 故障排除

### 脚本无法启动
- 检查脚本路径是否正确
- 检查文件权限
- 查看应用程序日志

### 任务状态未更新
- 检查调度器是否正常运行
- 查看任务结果目录是否生成了 run.txt
- 检查数据库连接

### 脚本执行卡住
- 检查系统资源
- 查看进程列表
- 检查脚本日志文件
