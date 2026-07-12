# 项目开发进度

> 记录当前项目做到哪里、已完成什么、下一步做什么。

---

## 当前阶段

阶段：阶段 1 — 认证与注册  
状态：**已完成**（建表 + 注册/登录/找回密码/JWT/Security 全流程验证通过）  
最后更新时间：2026-07-13

---

## 已完成

### 阶段 0：项目基础设施（2026-07-12）

- [x] 文档体系初始化（docs/ 核心文件 + README）
- [x] 待确认事项全部确认（P/U/S/A/O/C/T/D 系列）
- [x] MVP 范围定稿 + Agent 指令稿正式版 + 开发时间计划
- [x] 本地开发环境就绪（JDK 17 + Maven 3.9.16 + Docker + MySQL + Redis）
- [x] Docker Compose 配置补全（healthcheck / 资源限制 / 日志滚动 / 127.0.0.1 / .env / 固定 name:xiyue）
- [x] 配置收口：库名统一 xiyue_platform + .env.example + .env
- [x] Spring Boot 3 后端骨架（pom + 启动类 + application.yml + 分包）
- [x] MyBatis-Plus 集成 + 统一响应体 Result + 全局异常处理
- [x] Knife4j 文档 + 健康检查 GET /api/health（真探测 MySQL + Redis）

### 阶段 1：认证与注册（2026-07-13）

- [x] 数据库建表：`sys_user`（phone 唯一 + role + BCrypt 密码）+ `aunt`（user_id 唯一 + 双状态字段），schema.sql 幂等，spring.sql.init 启动自动执行
- [x] 实体与 Mapper：SysUser / Aunt / RoleEnum / SysUserMapper / AuntMapper
- [x] ADMIN 账号初始化：AdminAccountInitializer（ApplicationRunner），启动时检查无 ADMIN 则用 `ADMIN_INIT_PASSWORD` 环境变量 BCrypt 加密后插入（密码不入 SQL/源码/Git，见 ADR-013）
- [x] JWT 工具：JwtProperties + JwtUtil（HS512，密钥至少 32 字节启动校验，sub=userId + phone/role claims）
- [x] Redis 验证码服务：SmsCodeService（固定验证码 123456，Key `sms:code:{phone}` TTL 5 分钟，校验通过一次性删除）
- [x] 认证 DTO：SmsCode/Register/PasswordLogin/CodeLogin/ResetPassword 请求 + Login/Profile 响应（含参数校验注解）
- [x] 认证 Service：AuthService（注册事务含阿姨同步建 aunt、密码登录、验证码登录、找回密码、获取 profile；角色白名单禁 ADMIN；角色取自数据库不信客户端）
- [x] 认证 Controller：AuthController（6 个接口：sms-code/register/login/password/login/code/reset-password/profile）
- [x] Spring Security JWT 集成：JwtAuthenticationFilter + RestAuthenticationEntryPoint(401) + RestAccessDeniedHandler(403) + LoginUser + SecurityUserContext
- [x] SecurityConfig 收紧：STATELESS + 禁 CSRF + 公开接口 permitAll + 其余 authenticated() + JWT Filter
- [x] 配置补充：application.yml 加 xiyue.jwt/xiyue.admin + spring.sql.init；application-local.yml 补本地 JWT 密钥与 ADMIN 密码（不入库）
- [x] 全流程接口验证通过（见下方验证清单）

---

## 阶段 1 验证清单（2026-07-13 curl 全通过）

| # | 场景 | 预期 | 结果 |
|---:|---|---|---|
| 1 | 发送验证码 | 200 | ✅ |
| 2 | 注册 USER | 200 | ✅ |
| 3 | 注册 AUNT | 200 + 同事务建 aunt | ✅ |
| 4 | 注册 ADMIN | 403 角色白名单拒绝 | ✅ |
| 5 | 重复手机号注册 | 409 已注册 | ✅ |
| 6 | 错误验证码注册 | 1005 验证码错误 | ✅ |
| 7 | 密码登录 | 200 + token | ✅ |
| 8 | 错误密码登录 | 401 | ✅ |
| 9 | 验证码登录 | 200 + token | ✅ |
| 10 | profile 带 token | 200 用户信息 | ✅ |
| 11 | profile 无 token | HTTP 401 | ✅ |
| 12 | profile 错误 token | HTTP 401 | ✅ |
| 13 | 找回密码 | 200 | ✅ |
| 14 | 旧密码登录失败 / 新密码登录成功 | 401 / 200 | ✅ |
| 15 | ADMIN 账号登录（启动初始化） | 200 + token role=ADMIN | ✅ |
| 16 | 数据库：表/索引/数据 | sys_user+aunt 表、uk_phone/uk_user_id、3 账号+aunt 记录 | ✅ |
| 17 | Redis 验证码一次性 | 校验后删除 | ✅ |

---

## 环境就绪详情（2026-07-12 确认）

| 工具 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17.0.8 LTS | `C:\Program Files\Java\jdk-17`，JAVA_HOME 已指向 |
| Maven | 3.9.16 | bash 用 mvn17 wrapper（`/c/Users/Jodio/tools/mvn17.sh`） |
| Docker | 29.6.1 + Compose v5.2.0 | daemon 运行中 |
| MySQL | 8.0.46 | 容器 xiyue-mysql，端口 127.0.0.1:3307→3306，库 xiyue_platform，utf8mb4 |
| Redis | 7.0-alpine | 容器 xiyue-redis，端口 127.0.0.1:6380→6379（本机 memurai 占 6379，见 T-005） |
| Node | 22.22.2 + npm 10.9.7 | 前端用 |
| Git | 2.54.0 | — |

注意：`platform encoding: GBK`，项目强制 UTF-8（见 AGENTS.md §1）。

---

## 当前正在做

```text
阶段 1 已完成，准备进入阶段 2：阿姨管理。
```

---

## 当前阻塞问题

```text
暂无
```

---

## 下一步计划（阶段 2：阿姨管理）

1. `aunt` 运营字段编辑接口（管理员编辑姓名/头像/标价/技能标签/介绍）
2. 管理员上下架/禁用阿姨（admin_status：AVAILABLE/OFF_SHELF/DISABLED）
3. 管理员逻辑删除阿姨（存在历史订单禁止物理删除）
4. 阿姨设置个人接单状态（accept_status：AVAILABLE/RESTING）
5. 用户端阿姨列表（分页、按星级/价格/技能标签筛选）
6. 用户端阿姨详情
7. 角色细粒度权限：@PreAuthorize 按 USER/AUNT/ADMIN 隔离接口

---

## 最近一次可运行状态

```text
阶段 1 认证与注册已可启动验证：
  cd backend && /c/Users/Jodio/tools/mvn17.sh spring-boot:run -Dspring-boot.run.arguments=--server.port=8080

启动后：
- GET /api/health → {"code":200,"data":{"mysql":"up","redis":"up","status":"up"}}
- ADMIN 账号自动初始化（手机号 13800000000，密码=ADMIN_INIT_PASSWORD）
- 认证接口可用：/api/auth/{sms-code,register,login/password,login/code,reset-password,profile}
- Knife4j 文档 /doc.html（HTTP 200）

测试账号（本地，密码见 .env / application-local.yml，不入库）：
- ADMIN: 13800000000 / <ADMIN_INIT_PASSWORD 环境变量值>
- USER : 13800000001 / <注册时设置的密码>
- AUNT : 13800000002 / <注册时设置的密码>
```
