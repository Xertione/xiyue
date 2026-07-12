# 项目开发进度

> 记录当前项目做到哪里、已完成什么、下一步做什么。

---

## 当前阶段

阶段：阶段 0 — 项目基础设施  
状态：**已完成**（后端骨架 + 基础设施就绪，准备进入阶段 1）  
最后更新时间：2026-07-12

---

## 已完成

- [x] 文档体系初始化（docs/ 核心文件 + README）
- [x] 待确认事项全部确认（P/U/S/A/O/C/T/D 系列）
- [x] MVP 范围定稿（基于确认结果）
- [x] Agent 指令稿升级为正式版
- [x] 开发时间计划重写对齐
- [x] 文档全面审核与修复
- [x] 架构规则一致性修订（注册权限、抢单并发、价格、取消、投诉、部署安全）
- [x] AGENTS.md 增加命令行与编码防坑规则
- [x] 本地开发环境就绪（JDK 17 + Maven 3.9.16 + Docker + MySQL + Redis）
- [x] Docker Compose 配置补全（healthcheck / 资源限制 / 日志滚动 / 127.0.0.1 端口绑定 / 密码走 .env / 固定 name: xiyue）
- [x] 配置收口：库名统一 xiyue_platform + .env.example（入库）+ .env（本地）
- [x] Spring Boot 3 后端骨架（pom.xml + 启动类 + application.yml + application-local.yml + 分包）
- [x] MyBatis-Plus 集成（mybatis-plus-spring-boot3-starter 3.5.5）
- [x] 统一响应体 Result<T> + ResultCode 枚举
- [x] 全局异常处理 GlobalExceptionHandler（业务异常、参数校验、DuplicateKey、约束校验等）
- [x] Spring Security 临时配置（放开 health + 文档，BCryptPasswordEncoder，阶段1 收紧）
- [x] Springdoc OpenAPI + Knife4j 文档集成
- [x] 健康检查接口 GET /api/health（真探测 MySQL SELECT 1 + Redis ping）
- [x] 编译与启动验证通过（mvn clean compile + spring-boot:run + curl /api/health = 200 up）

---

## 环境就绪详情（2026-07-12 确认）

| 工具     | 版本                      | 说明                                                                         |
| ------ | ----------------------- | -------------------------------------------------------------------------- |
| JDK    | 17.0.8 LTS              | `C:\Program Files\Java\jdk-17`，JAVA_HOME 已指向                               |
| Maven  | 3.9.16                  | bash 用 mvn17 wrapper（`/c/Users/Jodio/tools/mvn17.sh`）                        |
| Docker | 29.6.1 + Compose v5.2.0 | daemon 运行中                                                                 |
| MySQL  | 8.0.46                  | 容器 xiyue-mysql，端口 127.0.0.1:3307→3306，库 xiyue_platform，utf8mb4              |
| Redis  | 7.0-alpine              | 容器 xiyue-redis，端口 127.0.0.1:6380→6379（本机 memurai 占 6379，改用 6380）          |
| Node   | 22.22.2 + npm 10.9.7    | 前端用                                                                        |
| Git    | 2.54.0                  | —                                                                          |

注意：`platform encoding: GBK`，项目强制 UTF-8（见 AGENTS.md §1）。

---

## 当前正在做

```text
阶段 0 已完成，准备进入阶段 1：认证与注册。
```

---

## 当前阻塞问题

```text
暂无
```

---

## 下一步计划（阶段 1：认证与注册）

1. `sys_user` 表建表 SQL（USER/AUNT/ADMIN 角色 + phone 唯一 + BCrypt 密码）
2. init.sql 初始化 ADMIN 账号（密码走环境变量 ADMIN_INIT_PASSWORD）
3. 注册接口（手机号 + BCrypt 密码 + 固定验证码 123456，角色白名单 USER/AUNT）
4. 阿姨注册事务（同时创建 sys_user + aunt 资料）
5. 密码登录 + 验证码登录接口（验证码缓 Redis，Key sms:code:{phone} TTL 5 分钟）
6. 找回密码接口
7. JWT 签发与解析（密钥走环境变量 JWT_SECRET，角色从数据库读）
8. Spring Security JWT Filter + 角色权限隔离（收紧 anyRequest().authenticated()）
9. 验证：注册/登录/JWT/受保护接口 401/403

---

## 最近一次可运行状态

```text
后端骨架已可启动：
  cd backend && mvn17.sh spring-boot:run -Dspring-boot.run.arguments=--server.port=8080
GET /api/health 返回 {"code":200,"data":{"mysql":"up","redis":"up","status":"up"}}
Knife4j 文档页 /doc.html 可访问（HTTP 200）
```
