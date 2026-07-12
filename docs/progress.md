# 项目开发进度

> 记录当前项目做到哪里、已完成什么、下一步做什么。

---

## 当前阶段

阶段：阶段 2 — 阿姨管理  
状态：**已完成**（管理员编辑/上下架/禁用/逻辑删除 + 用户端列表筛选详情 + 阿姨自设接单状态 + @PreAuthorize 角色隔离，全流程验证通过）  
最后更新时间：2026-07-13

---

## 已完成

### 阶段 0：项目基础设施（2026-07-12）

- [x] 文档体系 + 本地环境 + Docker Compose + 配置收口
- [x] Spring Boot 3 后端骨架 + MyBatis-Plus + Result/异常 + Knife4j + 健康检查

### 阶段 1：认证与注册（2026-07-13）

- [x] sys_user + aunt 建表 + ADMIN 初始化（ADR-013）
- [x] 注册/密码登录/验证码登录/找回密码 + JWT（HS512 + pwdSig 密码哈希签名，ADR-014）
- [x] Spring Security JWT Filter + 权限收紧 + sms-code 频率限制
- [x] Review 修复：AdminAccountInitializer 手机号冲突 + 验证码消费顺序

### 阶段 2：阿姨管理（2026-07-13）

- [x] 数据库变更：aunt 表加 deleted 字段（TINYINT，@TableLogic 逻辑删除）+ schema.sql 同步
- [x] 状态枚举：AuntAdminStatus（AVAILABLE/OFF_SHELF/DISABLED）+ AuntAcceptStatus（AVAILABLE/RESTING）
- [x] MyBatis-Plus 分页插件配置（PaginationInnerInterceptor）
- [x] DTO：AuntListItem/AuntDetail/AuntUpdateRequest/AuntStatusUpdateRequest/AuntAcceptStatusRequest + 通用 PageResponse
- [x] AuntService：用户端列表(筛选+排序+分页)/详情(仅AVAILABLE)、管理员全量列表/详情/编辑/状态/逻辑删除、阿姨自设接单状态
- [x] Controller：AuntController（用户端 GET /api/aunts + /{id}，AUNT PATCH /me/status）+ AdminAuntController（管理员 5 接口）
- [x] Security @EnableMethodSecurity + @PreAuthorize 角色隔离（USER/AUNT/ADMIN）
- [x] Bug 修复：@PreAuthorize 抛 AccessDeniedException 被 GlobalExceptionHandler 捕获返回 403（原 500，见 T-010）
- [x] 全流程验证通过

---

## 阶段 2 验证清单（2026-07-13 curl 全通过）

| # | 场景 | 预期 | 结果 |
|---:|---|---|---|
| 1 | 注册 2 个 AUNT | 200 | ✅ |
| 2 | 管理员全量列表 | 3 条 | ✅ |
| 3 | 管理员编辑阿姨（英文 body） | 200 + 详情更新 | ✅ |
| 4 | 管理员编辑阿姨（中文 body，python 测） | 200 + 中文存储正确 | ✅ |
| 5 | 用户端列表仅显示 AVAILABLE | OFF_SHELF 不显示 | ✅ |
| 6 | 用户端筛选 skillTag | 命中匹配 | ✅ |
| 7 | 用户端筛选 minPrice/minRating | 正确过滤 | ✅ |
| 8 | 用户端详情 AVAILABLE | 200 | ✅ |
| 9 | 用户端详情 OFF_SHELF | 404 不存在或已下架 | ✅ |
| 10 | AUNT 设接单状态 RESTING | 200 + 详情确认 | ✅ |
| 11 | 越权 USER 访问 admin 接口 | 403 | ✅ |
| 12 | 越权 AUNT 访问 USER 接口 | 403 | ✅ |
| 13 | 越权 USER 访问 AUNT 接口 | 403 | ✅ |
| 14 | 逻辑删除阿姨 | 200 + 列表不再显示 | ✅ |
| 15 | 禁用阿姨 DISABLED | 用户端列表不显示 | ✅ |
| 16 | 数据库 deleted 字段 | 逻辑删除=1，其余=0 | ✅ |

---

## 当前正在做

```text
阶段 2 已完成，准备进入阶段 3：订单与抢单。
```

---

## 当前阻塞问题

```text
暂无
```

---

## 下一步计划（阶段 3：订单与抢单）

1. service_order 表（含地址快照、价格快照）+ aunt_booking_slot 表（小时块 + 唯一索引）
2. 用户创建订单（选日期、整点开始时间、时长、填地址）→ 待支付
3. 模拟支付 → 待抢单
4. 抢单大厅（待抢单订单列表）
5. 阿姨抢单（事务 + 订单条件更新 + 档期唯一索引）
6. 管理员指派阿姨（兜底）
7. 用户订单列表/详情、阿姨订单列表/详情
8. 用户取消（待支付/待抢单/待服务，含模拟退款 + 档期释放）
9. 并发抢单测试
10. 补充：阿姨逻辑删除时检查历史订单（service_order 已建后）

---

## 最近一次可运行状态

```text
阶段 2 阿姨管理已可启动验证：
  cd backend && /c/Users/Jodio/tools/mvn17.sh spring-boot:run -Dspring-boot.run.arguments=--server.port=8080

启动后：
- GET /api/health → 200 up
- 阿姨模块接口可用：
  - 用户端：GET /api/aunts（列表+筛选）、GET /api/aunts/{id}（详情）
  - 阿姨自己：PATCH /api/aunts/me/status（设接单状态）
  - 管理员：GET/PUT/DELETE/PATCH /api/admin/aunts/**
- @PreAuthorize 角色隔离生效（越权返回 403）
- 测试账号（本地，密码见 .env / application-local.yml）：
  - ADMIN: 13800000000 / <ADMIN_INIT_PASSWORD>
  - USER : 13800000001 / <注册密码>
  - AUNT : 13800000002 / <注册密码>
```
