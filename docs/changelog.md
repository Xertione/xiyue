# 更新记录

> 记录每次较大功能变化和版本演进。  
> 格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。

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
