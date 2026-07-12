# 息悦生活：项目文档与记录体系

> 目标：让项目开发过程可追踪、可复盘、可交接、可用于面试准备。  
> 原则：不是为了写很多文档，而是为了不迷路、不重复踩坑、不让 AI 忘记项目进度。

---

## 1. 推荐文档目录

```text
docs/
├── 01-current-plan.md
├── 02-open-questions.md
├── 03-mvp-and-roadmap.md
├── 04-agent-project-spec.md
├── 05-how-to-use-ai.md
├── 06-schedule.md
├── 07-documentation-system.md
│
├── progress.md
├── troubleshooting.md
├── decision-log.md
├── architecture.md
├── database.md
├── api.md
├── test-cases.md
├── deployment.md
├── changelog.md
└── interview-notes.md
```

---

## 2. `progress.md`：项目过程记录

### 用途

记录当前项目做到哪里、已完成什么、下一步做什么。

这是最应该频繁更新的文件。

它可以帮助：

- 你自己不会忘记进度；
- AI/Agent 理解当前状态；
- 你切换电脑或隔几天继续开发时快速恢复状态；
- 最终回顾项目开发过程。

### 建议模板

```md
# 项目开发进度

## 当前阶段

阶段：阶段 1 - 登录与权限  
状态：进行中  
最后更新时间：2026-xx-xx

---

## 已完成

- [x] Git 仓库初始化
- [x] Docker Compose 启动 MySQL 和 Redis
- [x] Spring Boot 连接 MySQL
- [x] 统一响应体
- [x] 全局异常处理
- [x] 用户模拟登录接口
- [ ] JWT 认证过滤器
- [ ] 管理员登录
- [ ] 权限控制

---

## 当前正在做

```text
实现 Spring Security + JWT Filter，
目标是让受保护接口能够识别当前登录用户。
```

---

## 当前阻塞问题

```text
暂无
```

或：

```text
问题：JwtAuthenticationFilter 没有被执行。
已检查：请求头、Token 格式。
待检查：SecurityConfig 中的过滤器注册位置。
```

---

## 下一步计划

1. 完成 JWT 过滤器；
2. 添加 `/api/auth/profile`；
3. 验证未登录、用户、管理员三种访问情况。

---

## 最近一次可运行状态

```text
后端可启动；
MySQL、Redis 可连接；
用户登录接口可正常返回 Token；
订单模块尚未开始。
```
```

---

## 3. `troubleshooting.md`：踩坑与问题解决记录

### 用途

这份文件是你最宝贵的"实战经验库"。

不要只记录最终解决方案，要记录：

- 表现；
- 原因；
- 排查过程；
- 最终方案；
- 如何避免；
- 对应知识点。

### 建议模板

```md
# 项目踩坑与解决记录

---

## 编号：T-001

### 日期

2026-xx-xx

### 问题标题

Spring Boot 无法连接 Docker 中的 MySQL。

### 问题现象

项目启动时报错：

```text
Communications link failure
```

### 原因分析

本地启动 Spring Boot 时，`localhost` 指向开发电脑本机；
Docker MySQL 映射到宿主机端口后，应连接映射端口。

如果 Spring Boot 也运行在 Docker 容器中，
则不能使用 `localhost` 连接 MySQL，
而应使用 Docker Compose 服务名，例如 `mysql`。

### 最终解决

本地启动时：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/xiyue_life
```

容器启动时：

```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/xiyue_life
```

通过环境变量区分开发与部署环境。

### 如何验证

- 本地运行后端连接成功；
- Docker Compose 运行后端连接成功；
- 执行健康检查接口正常。

### 学到的知识

```text
Docker Compose 中服务名可以作为容器网络内的主机名。
localhost 在不同运行环境中指向不同对象。
```

### 标签

`Docker` `MySQL` `环境配置`
```

---

## 4. `decision-log.md`：技术和需求决策记录

### 用途

记录"为什么这样选，而不是那样选"。

这份文件对面试特别有价值，因为面试官经常问：

```text
为什么选 MyBatis-Plus？
为什么不用 Redis 锁？
为什么不做微服务？
为什么先做 H5 而不是小程序？
```

### 建议模板

```md
# 项目决策记录

---

## ADR-001：第一版采用 Spring Boot 单体架构

### 日期

2026-xx-xx

### 背景

项目是学习型 MVP，服务器配置为 2 核 2G，业务量很小。

### 候选方案

1. Spring Boot 单体；
2. Spring Cloud 微服务。

### 最终决策

采用 Spring Boot 单体 + 模块化分包。

### 原因

- 功能规模小；
- 当前没有独立部署、独立扩缩容需求；
- 微服务会增加服务注册、网关、配置中心、链路追踪等运维复杂度；
- 当前学习重点应放在业务闭环、权限、事务和订单状态上；
- 单体架构后续仍可按模块逐步拆分。

### 后果

优点：

- 开发和部署简单；
- 适合单机服务器；
- 更容易调试和理解。

限制：

- 后续业务增长时需要重新评估模块拆分和服务治理。
```

建议至少记录以下决策：

```text
- 为什么使用 Spring Boot 3；
- 为什么使用单体架构；
- 为什么使用 MyBatis-Plus；
- 为什么第一版不做小程序；
- 为什么第一版不接微信支付；
- 为什么不用 Redis 分布式锁作为预约最终保障；
- 为什么使用数据库唯一索引；
- 为什么不做地图和 LBS；
- 为什么使用 Docker Compose；
- 为什么采用 Nginx 反向代理。
```

---

## 5. `architecture.md`：架构说明

### 用途

说明系统整体由什么组成，各组件之间如何通信。

建议内容：

```md
# 系统架构

## 逻辑架构

```text
用户端 H5 ──────┐
                ├── Nginx ──> Spring Boot ──> MySQL
管理后台 ────────┘                  │
                                    └── Redis
```

## 模块划分

```text
auth：认证授权
user：用户
aunt：阿姨
address：地址
order：订单
review：评价
admin：后台管理
integration：第三方能力抽象
common：通用能力
security：安全认证
```

## 核心请求链路

```text
前端 Axios
→ Nginx /api 反向代理
→ Spring Security Filter
→ Controller
→ Service
→ Mapper
→ MySQL
→ 返回统一 JSON 响应
```
```

---

## 6. `database.md`：数据库设计文档

### 用途

记录每张表、字段含义、索引、关联关系、状态值。

建议内容：

- 数据库名称；
- 每张表的建表 SQL；
- 字段说明；
- 索引说明；
- 表关系；
- 订单状态定义；
- 初始化数据；
- 数据迁移记录。

特别要记录：

```text
为什么 service_order 要保存订单快照；
为什么 aunt_booking_slot 有唯一索引；
为什么 review.order_id 要唯一；
为什么金额采用 DECIMAL 和 BigDecimal。
```

---

## 7. `api.md`：接口约定文档

### 用途

即使有 Knife4j，也建议保留一份简洁的业务接口说明。

每个接口写清楚：

```text
接口名称
请求路径
请求方式
权限要求
请求参数
成功返回
失败场景
状态码或业务错误码
```

例如：

```md
## 创建订单

- 路径：`POST /api/orders`
- 权限：普通用户登录
- 说明：创建一笔待支付订单并保存地址、价格快照；创建时不占用阿姨档期。

### 请求体

```json
{
  "serviceDate": "2026-07-20",
  "startHour": 9,
  "durationHours": 2,
  "contactName": "张三",
  "contactPhone": "13800000000",
  "serviceAddress": "演示地址"
}
```

### 失败场景

- 日期不是未来日期；
- 开始时间不是整点或时长无效；
- 联系人、电话或地址格式无效；
- 金额计算失败。
```

---

## 8. `test-cases.md`：测试用例

### 用途

让你不仅会"点一下页面"，还会系统地验证业务。

建议至少覆盖：

### 登录

```text
- 正确手机号 + 123456；
- 错误验证码；
- 无 Token 请求受保护接口；
- 无效 Token；
- 普通用户访问管理员接口。
```

### 订单地址快照

```text
- 创建订单时保存联系人、电话和详细地址；
- 第一版不建立独立地址表；
- 用户只能查看自己订单中的地址快照。
```

### 订单

```text
- 正常创建订单；
- 预约过去日期；
- 非整点开始时间；
- 非法服务时长；
- 联系人、电话或地址缺失；
- 创建订单时不占用阿姨档期。
```

### 支付和状态

```text
- 待支付订单支付；
- 已支付订单重复支付；
- 已取消订单支付；
- 服务中或待确认订单取消应失败；
- 待服务订单完成；
- 非待评价订单评价；
- 同一订单重复评价。
```

---

## 9. `deployment.md`：部署手册

### 用途

记录从一台新服务器开始，如何部署项目。

应包含：

```text
- 服务器准备；
- Docker 安装；
- Docker Compose 安装；
- Git 拉取项目；
- 环境变量配置；
- Docker Compose 启动；
- 数据库初始化；
- Nginx 配置；
- 防火墙/安全组；
- 日志查看；
- 数据备份；
- 服务更新步骤；
- 常见故障处理。
```

最终你应该可以根据这份文档，在新服务器上重新部署整个项目。

---

## 10. `interview-notes.md`：面试表达素材

### 用途

把你实际做过的技术点，整理成能说出口的语言。

建议按主题记录：

```text
- 项目背景和定位；
- 为什么做家政预约；
- 整体架构；
- JWT 和 Spring Security；
- 权限与水平越权；
- 创建订单事务；
- 数据库唯一索引防重复预约；
- 订单状态机；
- 为什么第一版不做超时自动取消；
- Redis 实际使用场景；
- Docker Compose 和 Nginx 部署；
- 项目中最难的问题；
- 项目中做得不够好的地方；
- 如果继续迭代会怎么做。
```

原则：

```text
只写自己真正做过、真正理解、能接受追问的内容。
```

---

## 11. `changelog.md`：版本变化记录

### 用途

记录每次较大功能变化，方便回顾版本演进。

示例：

```md
# 更新记录

## v0.1.0 - 2026-xx-xx

- 完成项目初始化；
- Docker Compose 启动 MySQL、Redis；
- 集成 MyBatis-Plus、统一响应体和全局异常处理。

## v0.2.0 - 2026-xx-xx

- 完成用户模拟登录；
- 完成管理员登录；
- 接入 JWT 和 Spring Security。

## v0.3.0 - 2026-xx-xx

- 完成阿姨自行注册和管理员状态管理；
- 完成订单地址快照。
```

---

## 12. 文档更新规则

| 场景 | 需要更新的文件 |
|---|---|
| 做完一个功能 | `progress.md`、`changelog.md` |
| 遇到并解决问题 | `troubleshooting.md` |
| 改变重要方案 | `decision-log.md`、必要时更新 MVP 文档 |
| 新增或修改表结构 | `database.md` |
| 新增或修改接口 | `api.md` |
| 新增测试场景 | `test-cases.md` |
| 修改部署方式 | `deployment.md` |
| 得到可讲的项目经验 | `interview-notes.md` |

---

## 13. 给 AI/Agent 的项目上下文恢复模板

当你隔了一段时间重新找 AI 协作时，可以先发送：

```text
我正在开发"息悦生活"家政预约平台。

请先以以下文档作为项目上下文：
1. docs/03-mvp-and-roadmap.md
2. docs/04-agent-project-spec.md
3. docs/progress.md
4. docs/decision-log.md
5. docs/troubleshooting.md

当前我正在做：
【填写当前模块】

本次只希望完成：
【填写一个具体、可验证的小目标】

请先不要生成完整项目。
请先分析：
1. 当前进度；
2. 本次目标涉及哪些模块；
3. 需要修改哪些文件；
4. 风险点和测试方式；
5. 我应该先理解的知识点。

等我确认方案后，再逐步实现。
```
