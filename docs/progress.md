# 项目开发进度

> 记录当前项目做到哪里、已完成什么、下一步做什么。

---

## 当前阶段

阶段：阶段 3 — 订单与抢单  
状态：**已完成**（建表+创建订单+模拟支付+抢单事务+并发抢单+管理员指派+取消退款档期释放，全流程验证通过）  
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
- [x] 全流程验证通过 + 巩固测试（分页size上限+稳定排序）

### 阶段 3：订单与抢单（2026-07-13）

- [x] 数据表：service_order（订单含地址/价格/阿姨快照+支付退款字段）+ aunt_booking_slot（小时块档期+联合唯一索引 uk_aunt_date_hour）
- [x] OrderStatus 枚举（9状态机 0-8，含 isCancelable/isPaid）+ ServiceOrder/AuntBookingSlot 实体 + Mapper
- [x] 订单 DTO（CreateOrderRequest/OrderListItem/OrderDetail/GrabListItem/AssignRequest/PayResponse）+ OrderNoGenerator（SO+时间戳+随机）
- [x] MockPaymentService（模拟支付/退款流水号，规范 §7.4）
- [x] OrderService：创建订单(校验不跨天+快照)、模拟支付(条件更新防并发)、用户列表/详情(校验归属)
- [x] 抢单事务（条件更新订单归属 + 档期预检 + 唯一索引兜底 + 阿姨快照，ADR-010/017）
- [x] 管理员指派（兜底，不校验接单状态，校验管理状态可用）
- [x] 抢单大厅 + 阿姨订单列表/详情
- [x] 用户取消（条件更新防并发 + 已支付模拟退款 + 待服务释放档期，规范 §7.6）
- [x] OrderController（8接口：创建/列表/详情/支付/取消/抢单大厅/抢单/我的订单）+ AdminOrderController（2接口：全量列表/指派）
- [x] AuntService.deleteByAdmin 注释更新（逻辑删除不检查历史订单，物理删除未提供接口）

---

## 阶段 3 验证清单（2026-07-13 curl 全通过）

| # | 场景 | 预期 | 结果 |
|---:|---|---|---|
| 1 | 创建订单（地址/价格快照） | 200 + status=0待支付 | ✅ |
| 2 | 模拟支付（条件更新） | 200 + status=1待抢单 + payNo | ✅ |
| 3 | 抢单大厅 | 包含待抢单订单 | ✅ |
| 4 | 阿姨抢单（事务+档期2条） | 200 + auntId填 + 档期9,10 | ✅ |
| 5 | 重复抢单 | 1002 已被抢走 | ✅ |
| 6 | 档期冲突（同AUNT同时段） | 1003 该时段已被预约 | ✅ |
| 7 | 并发抢单（2 AUNT 同时抢1单） | 仅1成功，另1返回1002 | ✅ |
| 8 | 管理员指派阿姨 | 200 + 订单进入待服务 | ✅ |
| 9 | 取消待服务订单 | 200 + 退款字段齐全 + 档期释放0条 | ✅ |
| 10 | 越权 USER 访问抢单大厅 | 403 | ✅ |
| 11 | 非法状态 取消已取消订单 | 1001 状态不允许 | ✅ |
| 12 | 阿姨订单列表/用户订单详情 | 归属校验正确 | ✅ |

---

## 当前正在做

```text
阶段 3 已完成，准备进入阶段 4：服务履约与评价。
```

---

## 当前阻塞问题

```text
暂无
```

---

## 下一步计划（阶段 4：服务履约与评价）

1. 阿姨开始服务（待服务 → 服务中）
2. 阿姨提交服务完成（服务中 → 待确认，上传演示图片 URL）
3. 用户确认服务完成（待确认 → 待评价）
4. review 表 + 用户评价（评分+文字，待评价 → 已完成，更新阿姨 service_count/rating）
5. complaint 表 + 用户投诉（待评价 → 投诉中）
6. 管理员处理投诉（投诉中 → 已完成，不更新阿姨评分）

---

## 最近一次可运行状态

```text
阶段 3 订单与抢单已可启动验证：
  cd backend && /c/Users/Jodio/tools/mvn17.sh spring-boot:run -Dspring-boot.run.arguments=--server.port=8080

启动后：
- GET /api/health → 200 up
- 订单模块接口可用：
  - 用户：POST /api/orders（创建）、GET /api/orders（列表）、GET /api/orders/{id}（详情）、POST /{id}/pay（支付）、POST /{id}/cancel（取消）
  - 阿姨：GET /api/orders/grab-list（抢单大厅）、POST /api/orders/{id}/grab（抢单）、GET /api/orders/mine（我的订单）
  - 管理员：GET /api/admin/orders（全量列表）、POST /api/admin/orders/{id}/assign（指派）
- 抢单事务 + 档期唯一索引 + 并发安全验证通过
- 测试账号（本地，密码见 .env / application-local.yml）：
  - ADMIN: 13800000000 / <ADMIN_INIT_PASSWORD>
  - USER : 13800000001 / <注册密码>
  - AUNT : 13800000002 / <注册密码>
```
