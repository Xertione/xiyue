# 项目开发进度

> 记录当前项目做到哪里、已完成什么、下一步做什么。

---

## 当前阶段

阶段：阶段 4 — 服务履约与评价  
状态：**已完成**（阿姨开始服务/提交完成 + 用户确认 + 评价含评分更新 + 投诉含管理员处理，全流程验证通过）  
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

- [x] 数据库变更：aunt 表加 deleted 字段（@TableLogic 逻辑删除，ADR-015）
- [x] 状态枚举 + MyBatis-Plus 分页 + DTO + AuntService + Controller + @PreAuthorize 角色隔离
- [x] Bug 修复：@PreAuthorize 异常返回 403（ADR-016）
- [x] 巩固优化：分页 size 上限 + 稳定排序

### 阶段 3：订单与抢单（2026-07-13）

- [x] service_order + aunt_booking_slot 建表（含快照+支付退款字段+联合唯一索引）
- [x] 创建订单/模拟支付/抢单大厅/抢单事务(条件更新+档期唯一索引)/管理员指派/取消退款档期释放
- [x] 并发抢单验证（唯一归属）+ 档期冲突 + 越权 + 非法状态
- [x] ADR-017 抢单校验接单状态 + ADR-018 待支付/待抢单不占档期

### 阶段 4：服务履约与评价（2026-07-13）

- [x] 数据表：review（uk_review_order）+ complaint（uk_complaint_order）+ service_order 加 complete_image
- [x] 服务履约：OrderService.start/complete/confirm（条件更新防并发）+ OrderController 3 接口
- [x] 评价：ReviewService（含阿姨评分加权平均更新 + service_count）+ ReviewController
- [x] 投诉：ComplaintService（create/listForAdmin/handle）+ ComplaintController + AdminComplaintController
- [x] ADR-019 投诉处理后不更新阿姨评分 + ADR-020 评价评分读-算-写

---

## 阶段 4 验证清单（2026-07-13 curl 全通过）

| # | 场景 | 预期 | 结果 |
|---:|---|---|---|
| 1 | 创建→支付→抢单→start→complete(imageUrl)→confirm | 200 + 待评价(5) + completeImage | ✅ |
| 2 | 评价 rating=5 | 200 + 订单已完成(6) | ✅ |
| 3 | 重复评价 | 1001（状态已变） | ✅ |
| 4 | 查看评价 | rating=5 + content | ✅ |
| 5 | aunt.rating 更新 + service_count+1 | 5.0 / 1 | ✅ |
| 6 | 投诉（待评价订单） | PENDING + 订单投诉中(8) | ✅ |
| 7 | 重复投诉 | 1001 | ✅ |
| 8 | 管理员投诉列表 | total=1 | ✅ |
| 9 | 管理员处理投诉 | 200 + 订单已完成(6) | ✅ |
| 10 | 投诉后评价 | 1001（订单已已完成） | ✅ |
| 11 | 投诉处理后阿姨评分不变 | 5.0 / 1 | ✅ |
| 12 | 越权 USER 访问 admin 投诉 | 403 | ✅ |
| 13 | 非法状态 已完成订单再 confirm | 1001 | ✅ |
| 14 | complaint 表数据 | status=HANDLED + handle_remark | ✅ |

---

## 当前正在做

```text
阶段 4 已完成，准备进入阶段 5：前端联调与部署。
```

---

## 当前阻塞问题

```text
暂无
```

---

## 下一步计划（阶段 5：前端联调与部署）

1. Vue 3 前端工程初始化（Vite + TS + Pinia + Axios）
2. 用户端页面（登录/注册、主页、阿姨列表、发布订单、订单列表、评价、投诉）
3. 阿姨端页面（登录/注册、抢单大厅、我的订单、开始服务、提交完成）
4. 管理后台（登录、阿姨管理、订单管理、投诉处理）
5. Docker 镜像 + Nginx 配置 + 云服务器部署
6. 演示数据 + README 和接口文档完善

---

## 最近一次可运行状态

```text
阶段 4 服务履约与评价已可启动验证：
  cd backend && /c/Users/Jodio/tools/mvn17.sh spring-boot:run -Dspring-boot.run.arguments=--server.port=8080

启动后：
- GET /api/health → 200 up
- 服务履约：POST /api/orders/{id}/start|complete|confirm
- 评价：POST /api/reviews、GET /api/orders/{id}/review
- 投诉：POST /api/complaints、GET /api/admin/complaints、POST /api/admin/complaints/{id}/handle
- 订单状态机 9 态全部打通（0→1→2→3→4→5→6/8→6，7取消）
```
