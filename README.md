# WebTable — 个人云盘系统

一个功能完整的个人网盘后端系统。支持分片上传、秒传、断点续传、文件分享转存、回收站、文件预览、角色权限控制、操作审计等功能。

## 技术栈

| 层级 | 选型 |
|------|------|
| 语言 / 框架 | Java 17, Spring Boot 3.2.5 |
| ORM | MyBatis 3.0.3 |
| 数据库 | MySQL |
| 缓存 / 中间件 | Redis（上传进度、会话管理、分布式锁、日志队列） |
| 认证 | JWT + BCrypt |
| API 文档 | SpringDoc OpenAPI 2.5.0 |
| 构建工具 | Maven |
| 关键依赖 | Lombok, PageHelper, Hutool, Commons Codec, jjwt |

## 核心功能

### 文件上传

- **分片上传**：大文件切分为多个 chunk 并发上传，Redis Set 原子记录已上传分片索引
- **秒传**：基于 MD5 检测服务端已有相同文件，直接引用计数 +1，无需重复传输
- **断点续传**：上传中断后重新发起，自动比对 Redis 中已上传分片列表，只传缺失部分
- **并发控制**：Lua 脚本原子检查文件上传状态（`check.lua`），同一文件同时仅允许一个上传任务

### 文件管理

- 文件列表查询（分页、按文件名模糊搜索）
- 文件夹的创建、移动、重命名
- 单文件下载与批量 ZIP 打包下载
- 在线预览：支持 jpg / png / gif / bmp / webp / pdf / txt / md 等格式

### 文件分享

- 生成唯一分享链接，支持有效期（默认 7 天）
- 通过分享链接转存文件到自己的网盘（`store`）
- 链接过期、链接错误等异常处理

### 回收站

- 软删除：文件移入回收站，保留 30 天
- 恢复：从回收站还原至原目录（自动处理原目录已被删除的情况）
- 彻底删除：物理清除磁盘文件与数据库记录
- 自动清理：定时任务扫描过期回收站项并清除

### 用户系统

- 注册（邮箱验证码）与登录（BCrypt 加密 + JWT Token）
- 三级角色：管理员（`ROLE_ADMIN`）、普通用户（`ROLE_USER`）、游客（`ROLE_GUEST`）
- 账号阶梯封禁（7 天 / 30 天）
- 存储空间配额管理（每人 1GB 默认）
- 自动刷新 Redis 登录会话有效期

### 运维设施

- **操作审计日志**：AOP 切面拦截 `@Log` 注解，自动记录操作类型、操作人、耗时、状态（成功/失败），异步写入 Redis 队列后批量落库
- **性能监控**：AOP 切面记录每个接口的执行耗时
- **全局异常处理**：统一异常拦截，标准化 JSON 响应格式 `Result<T>`
- **全链路 TraceId**：每个请求生成唯一 TraceId，贯穿整个请求生命周期（拦截器 → 日志 → 响应）
- **CORS 跨域**：支持前后端分离部署

## 架构亮点

### MD5 文件去重与引用计数

```
物理文件（磁盘）由 MD5 唯一标识
       ↑
 file 表主记录（ref_count > 0）    —— 记录有多少人引用此物理文件
       ↑
 每个用户的逻辑文件记录            —— 各自拥有独立 ID、原始文件名、目录位置
```

- 上传新文件 → 创建主记录 `ref_count = 1`
- 转存已有文件 → 主记录 `ref_count + 1`，创建逻辑记录 `ref_count = 0`
- 删除文件 → 主记录 `ref_count - 1`，移入回收站
- 回收站彻底删除 → 仅当 `ref_count = 0` 且回收站中无其他引用时才删除物理文件

### 分片上传流程

```
客户端                          服务端
 │                               │
 ├─ POST /check ───────────────►  Lua 脚本原子检查 MD5 状态
 │                               │   → "秒传" / "新上传" / "续传" / "等待"
 │◄──── taskId ────────────────  │
 │                               │
 ├─ POST /chunk (×N) ──────────►  写入磁盘 → 成功后 Redis SADD 记录索引
 │                               │
 ├─ POST /merger ──────────────►  校验分片完整性
 │                               │  → mergeChunks 串行写入并流式计算 MD5
 │                               │  → 写数据库文件记录
 │                               │  → Redis unlock + 清理临时目录
```

### 回收站设计

- 删除本质是 `INSERT INTO recycle_bin` + `DELETE FROM file`
- 恢复本质是 `INSERT INTO file`（重新写入 file 表）
- 彻底删除时才真正调用 `Files.delete()` 删除磁盘文件
- `RecycleCleanupTask` 定时扫描并清除过期记录

## API 概览

### 认证

| 路径 | 方法 | 说明 |
|------|------|------|
| `/Mydisk/login` | POST | 用户登录（返回 JWT） |
| `/Mydisk/logout` | POST | 退出登录 |
| `/Mydisk/regi/sendcode` | POST | 发送邮箱验证码 |
| `/Mydisk/regi/register` | POST | 注册新用户 |

### 文件上传

| 路径 | 方法 | 说明 |
|------|------|------|
| `/Mydisk/upload/check` | POST | 上传前检查（秒传/续传判断） |
| `/Mydisk/upload/chunk` | POST | 上传单个分片 |
| `/Mydisk/upload/merger` | POST | 合并分片 / 秒传 |

### 文件管理

| 路径 | 方法 | 说明 |
|------|------|------|
| `/Mydisk/select` | GET | 文件列表（分页查询） |
| `/Mydisk/delete` | DELETE | 批量软删除（移入回收站） |
| `/Mydisk/download` | GET | 下载文件 |
| `/Mydisk/batchdownload` | GET | 批量打包下载 |
| `/Mydisk/preview` | GET | 在线预览文件 |
| `/Mydisk/mkdir` | POST | 创建文件夹 |
| `/Mydisk/rename` | GET | 重命名文件 |
| `/Mydisk/move` | POST | 移动文件 |

### 文件分享

| 路径 | 方法 | 说明 |
|------|------|------|
| `/Mydisk/share` | POST | 生成分享链接 |
| `/Mydisk/store/{link}` | GET | 根据链接转存文件 |
| `/Mydisk/getlinkinfo` | GET | 查询分享信息 |

### 回收站

| 路径 | 方法 | 说明 |
|------|------|------|
| `/Mydisk/recycleBin/selectall` | POST | 查询回收站文件 |
| `/Mydisk/recycleBin/goback` | POST | 恢复文件 |
| `/Mydisk/recycleBin/delete` | POST | 彻底删除 |

### 用户管理

| 路径 | 方法 | 说明 |
|------|------|------|
| `/Mydisk/updatevolume` | GET | 刷新用户存储空间 |
| `/Mydisk/getvolume` | GET | 获取当前用户空间信息 |

## 本地启动

### 依赖

- JDK 17+
- MySQL
- Redis

### 配置

修改 `src/main/resources/application.yml`：

```yaml
spring.datasource.url: jdbc:mysql://{your_host}:3306/webtable
spring.data.redis.host: {your_redis_host}
path.pathhead: {文件存储根目录}
```

### 运行

```bash
./mvnw spring-boot:run
```

启动后访问 http://localhost:8080/swagger-ui.html 查看 API 文档。

## 项目结构

```
src/main/java/com/xushu/webtable/
├── Aspect/                  # AOP 切面（操作日志、角色校验、性能监控）
├── Tasks/                   # 定时任务（回收站清理、日志落库、死信处理）
├── anno/                    # 自定义注解（@Log、@Role）
├── common/                  # 公共类（实体、DTO、常量、异常处理）
├── config/                  # 配置类（CORS、Redis、Swagger、拦截器注册）
├── controller/              # 控制器层
├── interceptor/             # 拦截器（JWT 鉴权、TraceId）
├── mapper/                  # MyBatis Mapper 接口
├── service/
│   ├── maker/               # 服务实现类
│   └── *.java               # 服务接口
└── utils/                   # 工具类（JWT、邮箱、Redis 锁、线程上下文）
```

## 设计决策记录

开发过程中遇到的关键设计决策记录在 [开发日志](#) 中，包括：

- 从"每人一个文件夹"到"全局 MD5 存储"的架构演进
- 引用计数的主副记录分离方案
- 下载与删除的并发冲突处理
- 分享链接独立表设计

## License

MIT
