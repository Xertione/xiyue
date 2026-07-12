# 系统架构

> 说明系统整体组成、各组件通信方式、模块划分和核心请求链路。

---

## 逻辑架构

```text
用户端 H5 ──────┐
                ├── Nginx ──> Spring Boot ──> MySQL
阿姨端 ─────────┘                  │
                                  └── Redis
管理后台 ────────> Nginx ──> Spring Boot
```

## 模块划分

```text
common：       通用工具类、常量、枚举
security：     Spring Security 配置、JWT Filter、认证上下文
auth：         注册、登录、找回密码
user：         用户信息（预留扩展）
aunt：         阿姨资料管理、列表、详情
order：        订单创建、状态流转、抢单、支付
review：       评价
complaint：    投诉
admin：        管理员后台接口
integration：  第三方能力抽象（MockPaymentService 等）
```

## 角色划分

| 角色 | 路由 | 功能范围 |
|---|---|---|
| USER | 前端用户路由 | 浏览阿姨、发布订单、支付、评价、投诉 |
| AUNT | 前端阿姨路由 | 抢单、开始服务、提交完成、设置接单状态 |
| ADMIN | 管理后台路由 | 阿姨管理、订单管理、投诉处理 |

## 核心请求链路

```text
浏览器 Axios
→ Nginx（前端静态资源 / 或 /api 反向代理）
→ Spring Security Filter（JWT 校验）
→ Controller（参数校验）
→ Service（业务逻辑 + 事务）
→ Mapper（MyBatis-Plus）
→ MySQL / Redis
→ 统一 JSON 响应返回前端
```

## 部署结构

```text
Docker Compose
├── mysql:8.0       内部端口 3306，不暴露公网，数据卷持久化
├── redis:7.0       内部端口 6379，不暴露公网，数据卷持久化
├── backend         端口 8080，Spring Boot 3 + JDK 17
└── nginx:latest    端口 80
    ├── /           前端静态资源（用户/阿姨共用）
    ├── /admin/     管理后台静态资源
    └── /api/       反向代理到 backend:8080
```

## 安全架构

```text
- 所有受保护接口通过 Spring Security FilterChain 拦截
- JWT Token 从 Authorization: Bearer <token> 获取
- 角色信息编码在 JWT claims 中，但签发角色必须来自数据库
- 公开注册角色仅允许 USER/AUNT，ADMIN 只能由初始化 SQL 创建
- 用户/阿姨/管理员接口通过 @PreAuthorize 或自定义注解隔离
- 资源归属校验在 Service 层手动完成
- 抢单使用订单状态条件更新保证唯一归属，档期唯一索引只保证时段不冲突
- JWT 密钥、数据库密码、Redis 密码只通过环境变量注入，禁止进入源码和日志
```
