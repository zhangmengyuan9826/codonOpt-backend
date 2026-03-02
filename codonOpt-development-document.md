# mRNA密码子优化系统开发文档

## 1. 项目概述

### 1.1 项目背景

研究团队成功研发了一套优秀的mRNA密码子优化算法，该算法具备以下核心优势：
- XXXXXX

为促进技术转化和对外服务，需将此算法包装成可对外发布的Web应用系统。

### 1.2 系统目标

1. 提供用户友好的Web界面进行序列提交和参数配置
2. 实现异步任务处理机制处理计算密集型任务
3. 建立可靠的通知系统（邮件通知）
4. 确保系统安全性，防止恶意攻击
5. 提供任务状态查询和结果下载功能

## 2. 系统架构设计

### 2.1 整体架构图

> 注：此处待补充架构图

### 2.2 组件说明

#### 2.2.1 前端组件

1. **用户认证模块**：注册/登录/邮箱验证
2. **序列提交模块**：序列输入、参数配置
3. **任务管理模块**：任务状态查询、历史记录
4. **结果展示模块**：优化结果可视化、文件下载

#### 2.2.2 后端组件

1. **API服务**：RESTful接口提供业务功能
2. **任务调度器**：管理计算任务队列，确保单任务执行
3. **计算引擎**：投递任务，执行密码子优化算法
4. **邮件服务**：异步发送任务状态通知
5. **安全管理**：IP限制、频率控制、访问日志

## 3. 详细功能设计

### 3.1 用户管理

#### 3.1.1 用户注册流程

填写用户名和邮箱 → 邮件发送验证码 → 用户输入验证码 → 验证成功 → 完成注册

#### 3.1.2 用户个人页面

- 修改用户名
- 重置密码

### 3.2 序列提交

#### 3.2.1 必填参数

1. **输入序列**：纯文本的氨基酸序列或核酸序列，需勾选核酸&蛋白质，并验证序列内容的合规性
2. **目标物种**：Human, Mouse, E.coli等，默认human
3. **任务名称**：默认当前时间

#### 3.2.2 可选参数

1. **限制性酶切位点**：提供列表，多选
2. **权重值**：滑动条，范围：0%~100%

### 3.3 任务处理流程

1. 用户提交任务，后台生成唯一任务ID
2. 任务进入数据库队列，初始状态："排队中"
3. 任务调度器检查当前运行任务
   - 无运行任务：立即执行，状态→"分析中"
   - 有运行任务：排队等待
4. 任务执行完毕：
   - 状态→"已完成"
   - 生成结果文件（ZIP压缩包）
   - 发送邮件通知，将zip压缩包添加到附件
5. 异常处理：
   - 计算失败：状态→"状态异常"，记录错误日志
   - 超时处理：任务超时自动终止

### 3.4 任务执行和结果

#### 3.4.1 任务投递

后台投递运行perl脚本，执行命令：

```bash
/PERL_PATH/perl /SCRIPT_PATH/codon_opt.pl --seq=THE_SEQ --type=Nucleoacid_or_peptide --species=HUMAN_or_OTHERS
```

记录：pid、任务开始时间、任务目录等信息

#### 3.4.2 任务执行

定时器检查任务状态，判断是否有任务在执行，否则按序执行新任务

#### 3.4.3 任务结果

**完成标志**：检查pid、检查该任务的结果目录是否完整、检查结果目录下的日志状态文件是否有成功标识【SUCCES】

- 若结果目录不完整或状态日志文件出现【ERROR】，则任务运行失败，从日志文件获取失败信息
- 任务完成后（成功和失败都是完成），按照数据库记录的任务投递顺序执行下一个任务，并邮件通知用户

#### 3.4.4 任务查询

- **成功完成的任务**：可以在线查看密码子优化结果，包括优化后的序列、GC含量、二级结构、CAI值等，可下载zip包
- **失败的任务**：显示失败信息

### 3.5 管理功能

系统管理员的权限包括：

1. 查看所有任务列表
2. 管理用户：删除、禁用、更改密码等
3. 安全防护机制：提交任务数限制、黑名单IP等

### 3.6 安全防护机制（开发架构默认）

- **频率限制**：同一用户/IP每天最多提交10个任务
- **IP黑名单**：管理员可动态添加/移除黑名单IP
- **请求验证**：验证序列格式和参数合法性
- **会话管理**：JWT token过期时间设置

## 4. 数据库设计（概要设计）

### 4.1 用户表 (users)

```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    user_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    ip_address INET,
    is_active BOOLEAN DEFAULT TRUE
);
```

### 4.2 任务表 (tasks)

```sql
CREATE TABLE tasks (
    task_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id INTEGER REFERENCES users(id),
    input_sequence TEXT NOT NULL,
    sequence_type VARCHAR(10) CHECK (sequence_type IN ('amino_acid', 'nucleotide')),
    target_species VARCHAR(50),
    parameters JSONB DEFAULT '{}',
    status VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending', 'queued', 'running', 'completed', 'failed')),
    queue_position INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    result_files_path TEXT,
    error_message TEXT
);
```

### 4.3 访问日志表 (access_logs)

```sql
CREATE TABLE access_logs (
    id SERIAL PRIMARY KEY,
    ip_address INET NOT NULL,
    user_id INTEGER REFERENCES users(id),
    endpoint VARCHAR(100),
    request_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status_code INTEGER,
    user_agent TEXT
);
```

### 4.4 配置文件表 (sys_config)

> 注：此处待补充具体字段定义

## 5. API接口设计

### 5.1 认证相关（架构提供）

- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/verify-email` - 邮箱验证
- `POST /api/auth/logout` - 退出登录

### 5.2 任务相关

- `POST /api/tasks` - 提交新任务
- `GET /api/tasks` - 获取用户所有任务
- `GET /api/tasks/{task_id}` - 获取特定任务详情
- `GET /api/tasks/{task_id}/download` - 下载结果文件
- `DELETE /api/tasks/{task_id}` - 取消任务（仅限排队中状态）

### 5.3 管理员接口

- `GET /api/admin/tasks` - 获取所有任务（管理员）
- `GET /api/admin/ip-logs` - 查看访问日志
- `POST /api/admin/blacklist` - 添加IP到黑名单
- `DELETE /api/admin/blacklist` - 从黑名单移除IP

## 6. 部署架构

### 6.1 服务器配置

Web、应用、数据库、缓存/队列（Redis）可放在同一台服务器

### 6.2 技术栈

#### 6.2.1 后端技术栈（高集成度）

- 核心框架：Spring Boot 3.x + Java 18
- 安全框架：Spring Security + JWT
- 任务调度：Spring Batch + Quartz
- 异步处理：Spring Integration + Redis消息队列

#### 6.2.2 前端技术栈

- 核心框架：Vue 3 + TypeScript
- 构建工具：npm

#### 6.2.3 算法服务

- 计算：现有密码子优化算法，Perl

#### 6.2.4 数据存储

- 主数据库：MySQL 8.0
- 缓存/队列：Redis 7.x

#### 6.2.5 基础设施

- API网关：Nginx（开发环境）
- CI/CD：GitLab CI














