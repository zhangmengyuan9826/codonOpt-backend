# mRNA密码子优化系统 - 后端实现文档

## 项目概述

这是一个基于Spring Boot 2.7.18 + Java 1.8的密码子优化系统后端，提供完整的用户认证、任务管理、Perl脚本执行和邮件通知功能。

## 技术栈

- **框架**: Spring Boot 2.7.18
- **Java版本**: 1.8
- **数据库**: MySQL 8.0.33
- **缓存**: Redis
- **安全**: Spring Security + JWT
- **API文档**: Swagger/OpenAPI 3.0
- **构建工具**: Maven

## 功能模块

### 1. 用户认证模块
- 用户注册（邮箱验证）
- 用户登录
- Token刷新
- 邮箱验证码发送
- 密码加密存储

### 2. 任务管理模块
- 任务提交（序列验证、频率限制）
- 任务队列管理
- 任务状态查询
- 任务结果下载
- 任务取消

### 3. Perl脚本执行模块
- 异步脚本执行
- 进程管理（PID追踪）
- 超时处理
- 结果验证
- 错误处理

### 4. 通知模块
- 邮件验证码
- 任务完成通知（带附件）
- 任务失败通知

### 5. 管理员模块
- 查看所有任务
- 查看访问日志
- IP黑名单管理
- 系统统计信息
- 重置用户频率限制

## 快速开始

### 前置条件

1. **JDK 1.8+**
2. **Maven 3.6+**
3. **MySQL 8.0+**
4. **Redis**
5. **Perl环境**（用于执行codon_opt.pl脚本）

### 安装步骤

#### 1. 创建数据库

```sql
CREATE DATABASE codon_opt CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 2. 执行数据库脚本

```bash
mysql -u root -p codon_opt < src/main/resources/schema.sql
```

#### 3. 配置application.yml

编辑 `src/main/resources/application.yml`，配置以下内容：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/codon_opt
    username: your_db_username
    password: your_db_password

  redis:
    host: localhost
    port: 6379

  mail:
    host: smtp.gmail.com
    port: 587
    username: your_email@gmail.com
    password: your_app_password

app:
  jwt:
    secret: your_jwt_secret_key_min_256_bits_long

  perl:
    script-path: codon_opt.pl
    script-directory: /path/to/scripts
    result-directory: ./results
```

#### 4. 安装Perl脚本

将 `codon_opt.pl` 脚本放置在配置的目录中。

#### 5. 启动Redis

```bash
redis-server
```

#### 6. 运行应用

```bash
mvn spring-boot:run
```

或者打包后运行：

```bash
mvn clean package
java -jar target/codonOpt-1.0.0.jar
```

#### 7. 访问Swagger文档

打开浏览器访问：http://localhost:8008/swagger-ui.html

## API接口说明

### 认证接口 (`/api/auth`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/auth/register` | 用户注册 | 否 |
| POST | `/api/auth/login` | 用户登录 | 否 |
| POST | `/api/auth/send-verification` | 发送验证码 | 否 |
| POST | `/api/auth/verify-email` | 验证邮箱 | 否 |
| POST | `/api/auth/refresh` | 刷新Token | 否 |
| POST | `/api/auth/logout` | 用户登出 | 是 |

### 任务接口 (`/api/tasks`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/tasks` | 提交任务 | 是 |
| GET | `/api/tasks` | 获取任务列表 | 是 |
| GET | `/api/tasks/{taskId}` | 获取任务详情 | 是 |
| DELETE | `/api/tasks/{taskId}` | 取消任务 | 是 |
| GET | `/api/tasks/{taskId}/download` | 下载结果 | 是 |

### 管理员接口 (`/api/admin`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/admin/tasks` | 查看所有任务 | 管理员 |
| GET | `/api/admin/access-logs` | 查看访问日志 | 管理员 |
| GET | `/api/admin/blacklist` | 查看黑名单 | 管理员 |
| POST | `/api/admin/blacklist` | 添加IP到黑名单 | 管理员 |
| DELETE | `/api/admin/blacklist/{ip}` | 移除IP黑名单 | 管理员 |
| GET | `/api/admin/stats` | 获取系统统计 | 管理员 |

## 默认账户

**管理员账户**（请在生产环境中修改）：
- 用户名: admin
- 邮箱: admin@codonopt.com
- 密码: admin123

## 配置说明

- 默认端口: 8008
- 上下文路径: /
- 配置文件: src/main/resources/application.yml

## 项目结构

完整的项目结构包含以下包：

- `config/` - 配置类（安全、Redis、邮件等）
- `controller/` - REST API控制器
- `dto/` - 数据传输对象
- `entity/` - JPA实体类
- `enums/` - 枚举类
- `exception/` - 异常处理
- `interceptor/` - 拦截器
- `repository/` - 数据访问层
- `security/` - 安全组件
- `service/` - 业务逻辑层
- `util/` - 工具类
- `constants/` - 常量定义

## 核心功能说明

### 任务执行流程

1. **用户提交任务** → 验证序列 → 检查频率限制 → 创建任务记录
2. **任务调度** → 定时检查队列 → 获取排队任务 → 更新状态为RUNNING
3. **任务执行** → 启动Perl进程 → 等待完成 → 验证结果 → 打包ZIP
4. **通知发送** → 异步发送完成/失败邮件

### 安全机制

- **JWT认证**: 无状态Token认证
- **密码加密**: BCrypt加密存储
- **频率限制**: 每日10次任务限制（可配置）
- **IP黑名单**: 防止恶意请求
- **访问日志**: 记录所有API请求

## 部署检查清单

- [ ] MySQL数据库已创建并执行schema.sql
- [ ] Redis服务已启动
- [ ] Perl环境已安装并配置路径
- [ ] codon_opt.pl脚本已放置在指定目录
- [ ] 邮件服务器配置正确
- [ ] application.yml中所有配置项已填写
- [ ] 日志输出目录有写权限
- [ ] 结果文件存储目录有写权限

## 故障排查

### 常见问题

1. **数据库连接失败**
   - 检查MySQL服务是否启动
   - 确认application.yml中的数据库配置

2. **Redis连接失败**
   - 检查Redis服务是否启动
   - 确认端口配置（默认6379）

3. **邮件发送失败**
   - Gmail需要使用应用专用密码
   - 检查SMTP配置是否正确

4. **Perl脚本执行失败**
   - 确认Perl已安装并在PATH中
   - 检查脚本路径配置
   - 验证脚本文件权限

## 开发说明

项目使用 Spring Boot 框架，支持热部署和快速开发。完整的API文档可通过Swagger UI访问。
