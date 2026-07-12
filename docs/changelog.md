# 更新记录

> 记录每次较大功能变化和版本演进。  
> 格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。

---

## [0.7.1] — 2026-07-13

### Fixed

- **前端无限重定向循环（T-011，根因）**：`/` 路径无路由定义，通配符 `/:pathMatch(.*)*` redirect `/` 导致 `pushWithRedirect` 无限递归（Maximum call stack size exceeded）。修复：加 `{ path: '/', redirect: '/login' }` + 通配符 redirect 改 `/login`
- router 守卫 Pinia 时序依赖：改用 localStorage 直接读取，`isLoggedIn` 校验 role 有效性
- index.html 加 localStorage 自动清理（token 存在但 role 无效时清除）+ window.onerror 错误捕获
- 后端 review 修复（16 文件）：OrderNoGenerator CAS 防时钟回拨、AuntMapper 原子评分更新（@Update SQL）、grabList 校验阿姨状态、cancel 档期释放去 oldStatus 依赖、DTO 加 @Size/@Pattern/@DecimalMin 校验、JWT 注释修正、日志降级 info

### Verified

- 前端：Vite 重启后访问 / → redirect /login → 登录页正常显示，无无限重定向
- 后端：编译通过，review 修复涉及事务/并发/校验加固

---

## [0.7.0] — 2026-07-13

### Added

- 前端工程：Vue 3 + Vite + TS + Pinia + Vue Router + Vant 4 + Element Plus + axios
- 基础设施：API 封装（JWT 拦截器 + 401 跳登录）+ auth store + 路由守卫（角色分发）+ format 工具
- 登录/注册页（验证码倒计时、角色选择）+ 用户端5页 + 阿姨端3页 + 管理后台3页
- 设计：teal-600 品牌色全站一致性（design-taste-frontend skill 原则：颜色 lock、反 AI-tell、完整状态）
- 部署：frontend/backend Dockerfile + nginx.conf + docker-compose 四服务

### Verified

- dev 联调通过（Vite 5173 + API 代理 /api→8080 + 模块加载）

---

## [0.6.0] — 2026-07-13

### Added

- 数据表：`review`（uk_review_order）+ `complaint`（uk_complaint_order）+ `service_order` 加 `complete_image` 字段
- 服务履约：OrderService.start/complete/confirm（条件更新防并发，规范 §5.4）+ OrderController 3 接口
- 评价：ReviewService（含阿姨评分加权平均更新 + service_count +1）+ ReviewController（POST /api/reviews + GET /api/orders/{id}/review）
- 投诉：ComplaintService（create/listForAdmin/handle）+ ComplaintController + AdminComplaintController
- OrderStatus 9 态全部打通：0→1→2→3→4→5→6（评价）/8→6（投诉处理），7取消

### Verified

- curl 14 项全通过：start→complete(imageUrl)→confirm→评价(评分5.0+count1)→重复评价拒→投诉→投诉中→重复投诉拒→管理员处理→已完成→投诉后评价拒→评分不变→越权403→非法状态1001→complaint表数据

### Decision

- ADR-019：投诉处理后不更新阿姨评分（规范 §7.9）
- ADR-020：评价评分采用读-算-写（MVP 并发量小可接受，后续可改 SQL 原子更新）

---

## [0.5.0] — 2026-07-13

### Added

- 数据表：`service_order`（订单含地址/价格/阿姨快照+支付退款字段）+ `aunt_booking_slot`（小时块档期+联合唯一索引 `uk_aunt_date_hour`）
- `OrderStatus` 枚举（9状态机 0-8，含 `isCancelable`/`isPaid`）+ `ServiceOrder`/`AuntBookingSlot` 实体 + Mapper
- 订单 DTO（CreateOrderRequest/OrderListItem/OrderDetail/GrabListItem/AssignRequest/PayResponse）+ `OrderNoGenerator`（SO+时间戳+随机）
- `MockPaymentService`（模拟支付/退款流水号，规范 §7.4）
- `OrderService`：创建订单(校验不跨天+快照)、模拟支付(条件更新防并发)、用户列表/详情(校验归属)、抢单事务(条件更新+档期预检+唯一索引兜底+阿姨快照)、管理员指派(兜底)、抢单大厅、阿姨订单列表/详情、取消(条件更新+模拟退款+档期释放)
- `OrderController`（8接口）+ `AdminOrderController`（2接口）

### Verified

- curl 全流程通过：创建+支付+抢单+档期2条(9,10)、重复抢单1002、档期冲突1003、并发抢单唯一归属(1成功1失败)、管理员指派、取消待服务退款字段齐全+档期释放0条、越权403、非法状态1001

### Decision

- ADR-017：抢单校验阿姨接单状态（休息不能抢），管理员指派不校验接单状态（兜底强制）
- ADR-018：待支付/待抢单订单不占档期，抢单/指派成功才锁档期，取消释放档期

---

## [0.4.1] — 2026-07-13

### Fixed

- 阿姨列表分页 size 无上限（可传 10000 拖垮 DB）→ 限制 1~100（`Math.min(Math.max(size,1),100)`）
- 阿姨列表排序不稳定（rating 相同时顺序不定）→ 加 `orderByDesc(id)` 二级排序保证稳定

### Verified

- 阶段1+2 轻量巩固测试通过：越权 403、sms-code 频率限制、改密码旧 token 失效、用户端列表仅 AVAILABLE

---

## [0.4.0] — 2026-07-13

### Added

- 数据库变更：aunt 表加 `deleted` 字段（TINYINT，逻辑删除）+ schema.sql 同步
- 状态枚举：`AuntAdminStatus`（AVAILABLE/OFF_SHELF/DISABLED）+ `AuntAcceptStatus`（AVAILABLE/RESTING），含 isValid 校验
- MyBatis-Plus 分页插件配置（`MybatisPlusConfig` + `PaginationInnerInterceptor`）+ 全局逻辑删除配置（logic-delete-field/value）
- 阿姨 DTO：`AuntListItem`/`AuntDetail`/`AuntUpdateRequest`/`AuntStatusUpdateRequest`/`AuntAcceptStatusRequest` + 通用 `PageResponse<T>`
- `AuntService`：用户端列表(筛选+排序+分页)/详情(仅AVAILABLE)、管理员全量列表/详情/编辑运营字段/上下架禁用/逻辑删除、阿姨按 user_id 自设接单状态
- `AuntController`（用户端 GET /api/aunts + /{id}，AUNT PATCH /me/status）+ `AdminAuntController`（管理员 GET/PUT/DELETE/PATCH /api/admin/aunts/**）
- `SecurityConfig` 加 `@EnableMethodSecurity`，全部阿姨接口加 `@PreAuthorize` 角色隔离（USER/AUNT/ADMIN）

### Fixed

- `@PreAuthorize` 抛 `AccessDeniedException` 被 `GlobalExceptionHandler` 的 `@ExceptionHandler(Exception.class)` 捕获返回 500 → 加 `@ExceptionHandler(AccessDeniedException.class)` 返回 403（见 T-010）

### Verified

- 全流程 curl 16 项通过：注册阿姨、管理员列表/编辑(英文+中文)/上下架/禁用/逻辑删除、用户端列表仅AVAILABLE/筛选(星级/价格/技能标签)/详情、AUNT 设接单状态、越权 403 ×3、数据库 deleted 字段

### Decision

- ADR-015：aunt 逻辑删除用 MyBatis-Plus @TableLogic（deleted 字段，查询自动过滤，删除自动 update）
- ADR-016：@PreAuthorize 的 AccessDeniedException 在 Controller 层抛出，由 GlobalExceptionHandler 处理为 403（Filter 层仍由 RestAccessDeniedHandler 处理）

### Known

- curl 在 Git Bash 下发送含中文的 JSON body 会 400（curl 编码问题，非接口缺陷；前端 axios 发 UTF-8 正常，python 测试正常，见 T-009）
- 阿姨逻辑删除时检查历史订单禁止物理删除留待阶段3（service_order 表建立后）

---

## [0.3.0] — 2026-07-13

### Added

- 数据库建表：`sys_user`（id/phone 唯一/password/role/nickname/create_time/update_time）+ `aunt`（id/user_id 唯一/name/avatar/price/rating/service_count/skill_tags/intro/admin_status/accept_status/create_time/update_time），`schema.sql` 幂等，`spring.sql.init` 启动自动执行
- 实体与 Mapper：`SysUser` / `Aunt` / `RoleEnum`（含 `isRegisterable` 白名单方法）/ `SysUserMapper` / `AuntMapper`
- ADMIN 账号初始化：`AdminAccountInitializer`（ApplicationRunner），启动时检查无 ADMIN 则用 `ADMIN_INIT_PASSWORD` 环境变量 BCrypt 加密后插入，密码不入 SQL/源码/Git（ADR-013）
- JWT 工具：`JwtProperties` + `JwtUtil`（HS512，密钥至少 32 字节启动校验，claims 含 sub=userId/phone/role）
- Redis 验证码服务：`SmsCodeService`（固定验证码 123456，Key `sms:code:{phone}` TTL 5 分钟，校验通过一次性删除）
- 认证 DTO：`SmsCodeRequest` / `RegisterRequest` / `PasswordLoginRequest` / `CodeLoginRequest` / `ResetPasswordRequest` / `LoginResponse` / `ProfileResponse`（含 jakarta validation 注解）
- 认证 Service：`AuthService`（注册事务含阿姨同步建 aunt、密码登录、验证码登录、找回密码、获取 profile；角色白名单禁 ADMIN；角色取自数据库不信客户端）
- 认证 Controller：`AuthController`（6 接口：sms-code / register / login/password / login/code / reset-password / profile）
- Spring Security JWT 集成：`JwtAuthenticationFilter` + `RestAuthenticationEntryPoint`(401) + `RestAccessDeniedHandler`(403) + `LoginUser`(UserDetails) + `SecurityUserContext`
- 配置：`application.yml` 加 `xiyue.jwt` / `xiyue.admin` / `spring.sql.init`；`application-local.yml` 补本地 JWT 密钥与 ADMIN 密码（不入库）

### Changed

- `SecurityConfig` 收紧：STATELESS + 禁 CSRF + 公开接口 permitAll（health/文档/auth 公开接口）+ 其余 `anyRequest().authenticated()` + 加 JWT Filter + 挂 EntryPoint/DeniedHandler
- 阶段 0 临时 `permitAll` 全部移除，受保护接口现在强制 JWT 认证

### Verified

- 全流程 curl 验证 17 项全部通过：注册(USER/AUNT/ADMIN拒绝/重复拒绝/错误码拒绝)、密码登录、验证码登录、profile、401(无token/错误token)、找回密码、ADMIN 启动初始化登录、数据库表/索引/数据、Redis 验证码一次性
- Review 后补充验证：改密码后旧 token 立即失效(401)、新 token 有效(200)；sms-code 60 秒频率限制生效

### Fixed

- AdminAccountInitializer 手机号冲突致启动崩溃：加 phone 占用预检 + try-catch `DuplicateKeyException`（review 发现）
- 验证码消费顺序不当：register/loginByCode/resetPassword 调整为先做无副作用校验（角色白名单/手机号存在性），最后 verifyCode（review 发现）
- 改密码后旧 JWT 不失效：JWT claims 加密码哈希前 16 位（pwdSig），JwtAuthenticationFilter 校验比对数据库当前哈希，改密码后旧 token 立即失效（ADR-014）
- sms-code 无频率限制：Redis limit key + 配置化间隔 `xiyue.sms.resend-interval-seconds`（默认 60 秒）

### Decision

- ADR-013：ADMIN 账号由应用启动初始化（密码走环境变量，非 init.sql 明文），符合 ADR-012 敏感配置仅环境变量注入
- ADR-014：JWT 密码哈希签名使改密码后旧 token 失效

---

## [0.2.0] — 2026-07-12

### Added

- Spring Boot 3 后端骨架：`backend/pom.xml`（JDK17、UTF-8、Spring Security、MyBatis-Plus、Redis、Knife4j、JJWT、Lombok）+ 启动类 + `application.yml`/`application-local.yml`
- 统一响应体 `Result<T>` + `ResultCode` 枚举
- 业务异常 `BusinessException` + 全局异常处理 `GlobalExceptionHandler`（参数校验、DuplicateKey、约束校验等）
- 临时 Spring Security 配置（放开 health + 文档，`BCryptPasswordEncoder`，阶段1 收紧）
- Knife4j OpenAPI 文档配置 + `OpenApiConfig` 元信息
- 健康检查接口 `GET /api/health`（真探测 MySQL `SELECT 1` + Redis `ping`）

### Changed

- `docker-compose.yml` 补全：healthcheck、资源限制（MySQL 768M/Redis 128M）、日志滚动（10m/3）、端口绑定 `127.0.0.1`、密码走 `${VAR}` 环境变量、固定 `name: xiyue`
- Docker Redis 宿主端口改 `6380→6379`（本机 memurai 占用 6379，见 troubleshooting T-005）
- 数据库名统一为 `xiyue_platform`（修正 database.md 原写的 xiyue_life）
- 配置收口：新增 `.env.example`（入库模板）+ `.env`（本地开发，gitignore 忽略）

### Known

- `spring-boot:run` 启动时 `server.port` 被某隐藏配置源覆盖成 2018，需 `--server.port=8080` 命令行参数强制（见 troubleshooting T-006）

---

## [0.1.1] — 2026-07-12

### Changed

- 阿姨改为自行注册，管理员只管理资料和运营状态
- 并发抢单拆分为订单状态条件更新与档期唯一索引两道保障
- 抢单价格不再与阿姨个人标价比较
- 取消范围收紧为待支付、待抢单、待服务，服务中和待确认由管理员线下介入
- 投诉限制为待评价订单一次，处理后不再评价
- 增加敏感配置、端口隔离、健康检查和 2 核 2G 容器资源约束

### Added

- 模拟退款状态、流水号和时间字段规范
- 阿姨账号一对一关系、投诉唯一索引和订单查询索引建议
- ADR-009 至 ADR-012

---

## [0.1.0] — 2026-07-12

### Added

- 文档体系初始化（7 个核心规划文档 + 10 个补充文档）
- 待确认事项全部确认并归档（P/U/S/A/O/C/T/D 系列）
- MVP 范围定稿（9 状态订单状态机、阿姨抢单、小时块档期）
- Agent 指令稿升级为正式版
- 开发时间计划对齐确认结果
- 决策日志记录 8 条关键架构决策（ADR-001 ~ ADR-008）
- 架构文档、数据库设计、接口草案、测试用例清单、部署手册框架

### Changed

- 项目定位从"管理员完成服务"调整为"阿姨抢单 + 用户确认"模式
- 服务时间规则从固定时段调整为整点小时块
- 用户角色从双角色扩展为三角色（USER / AUNT / ADMIN）

### Known

- 文档阶段完成，开发阶段尚未开始
