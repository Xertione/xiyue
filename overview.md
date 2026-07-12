# 息悦生活家政平台 - 代码审查报告

> 审查日期：2026-07-13
> 审查范围：backend/ 全部 69 个 Java 文件 + schema.sql + 配置文件 + docker-compose.yml
> 审查重点：安全、事务、并发、状态机、资源归属、配置泄露

---

## 总体评价

项目工程质量 **良好**。核心安全基础扎实（JWT + BCrypt + 角色白名单 + 资源归属校验 + 防枚举），抢单事务设计到位（条件更新 + 唯一索引双重保障）。但存在 **5 处真实并发 bug** 和若干配置/校验缺陷，建议在阶段 5 前修复 P1 级问题。

> **2026-07-13 修复更新**：用户已确认修复 #3/#4/#5/#6/#7/#11/#12/#13/#14/#15/#16 共 11 项，编译通过（0 错误）。剩余未修：#1 验证码频率限制竞态、#2 校验非原子、#8 Filter 每次查库、#9 评价查询返回 userId、#10 取消退款 update 非条件更新。

| 等级 | 数量 | 含义 |
|---|---|---|
| P0 严重 | 0 | 无立即阻断的安全漏洞 |
| P1 高 | 5 | 真实 bug 或安全风险，建议优先修复 |
| P2 中 | 5 | 设计缺陷或配置问题 |
| P3 低 | 6 | 建议改进，MVP 可接受 |

---

## P1 — 高优先级（真实 bug / 安全风险）

### 1. SmsCodeService 频率限制竞态条件

- **文件**：`backend/src/main/java/com/xiyue/auth/service/SmsCodeService.java:58-71`
- **现象**：`sendCode` 用 `hasKey(limitKey)` 检查 + `set(key, code)` + `set(limitKey, "1")` 三步操作，非原子。
- **后果**：并发请求可同时通过 `hasKey` 检查（都返回 false），全部写入验证码，绕过 60 秒频率限制。攻击者可高频刷验证码接口（虽为模拟短信，但逻辑缺陷真实存在）。
- **修复**：用 `setIfAbsent` 原子操作：
  ```java
  Boolean acquired = redisTemplate.opsForValue()
      .setIfAbsent(limitKey, "1", Duration.ofSeconds(resendIntervalSeconds));
  if (Boolean.FALSE.equals(acquired)) {
      throw new BusinessException(..., "发送太频繁...");
  }
  // 再 set 验证码
  redisTemplate.opsForValue().set(key, FIXED_CODE, CODE_TTL);
  ```

### 2. SmsCodeService 校验非原子

- **文件**：`SmsCodeService.java:80-91`
- **现象**：`get(key)` 读取 + `delete(key)` 删除两步非原子。
- **后果**：两个并发请求可同时读到验证码，都校验通过。固定验证码 123456 下影响有限，但若未来接入真实短信，同一验证码可被多次使用。
- **修复**：用 `delete(key)` 的返回值判断（删除成功说明之前存在且已一次性消费）：
  ```java
  Boolean deleted = redisTemplate.delete(key);
  if (Boolean.FALSE.equals(deleted)) {
      throw new BusinessException(..., "验证码已过期或未发送");
  }
  // 再比对（或用 Lua 脚本 get+比对+delete 原子化）
  if (!cached.equals(inputCode)) { throw ... }
  ```

### 3. 订单号冲突无重试

- **文件**：`backend/src/main/java/com/xiyue/order/service/OrderService.java:87-97` + `order/util/OrderNoGenerator.java`
- **现象**：`OrderNoGenerator` 同毫秒 1/900 冲突概率，冲突时 `DuplicateKeyException` 冒泡到 `GlobalExceptionHandler`，返回 1004「数据冲突，请刷新后重试」。
- **后果**：用户创建订单偶发失败，且错误非用户责任，体验差。
- **修复**：`createOrder` 内 catch `DuplicateKeyException` 重试 2-3 次：
  ```java
  for (int i = 0; i < 3; i++) {
      order.setOrderNo(orderNoGenerator.generate());
      try {
          orderMapper.insert(order);
          break;
      } catch (DuplicateKeyException e) {
          if (i == 2) throw e;
      }
  }
  ```

### 4. 评价评分更新丢失（并发读-算-写）

- **文件**：`backend/src/main/java/com/xiyue/review/service/ReviewService.java:114-130`
- **现象**：`updateAuntRating` 采用读-算-写：读 `serviceCount` + `rating` → 计算 → 写回。两个用户同时评价同一阿姨（不同订单）时，后写覆盖先写。
- **后果**：评分和服务次数丢失更新。例：阿姨原 rating=4.0/count=5，两用户同时评 5 分和 3 分，预期 count=7/rating=4.0，实际 count=6/rating=3.83（后者覆盖）。
- **修复**：用 SQL 原子更新：
  ```sql
  UPDATE aunt SET
    rating = ROUND((rating * service_count + #{newRating}) / (service_count + 1), 1),
    service_count = service_count + 1
  WHERE id = #{auntId}
  ```
  或加 `@Version` 乐观锁。

### 5. 逻辑删除未检查进行中订单

- **文件**：`backend/src/main/java/com/xiyue/aunt/service/AuntService.java:192-199`
- **现象**：`deleteByAdmin` 直接逻辑删除，不检查阿姨是否有待服务(2)/服务中(3)/待确认(4)订单。
- **后果**：阿姨被逻辑删除后（`deleted=1`），`@TableLogic` 使后续 `auntMapper.selectOne/selectById` 返回 null。阿姨调用 `start`/`complete`/`grab` 会报「阿姨资料不存在」，待服务订单卡死无法流转。
- **修复**：删除前检查进行中订单：
  ```java
  Long activeCount = orderMapper.selectCount(
      new LambdaQueryWrapper<ServiceOrder>()
          .eq(ServiceOrder::getAuntId, id)
          .in(ServiceOrder::getStatus, 2, 3, 4));
  if (activeCount > 0) {
      throw new BusinessException(..., "该阿姨有进行中订单，无法删除");
  }
  ```

---

## P2 — 中优先级（设计缺陷 / 配置问题）

### 6. 生产日志级别过高，可能泄露敏感信息

- **文件**：`backend/src/main/resources/application.yml:45-49, 73-76`
- **现象**：`mybatis-plus.configuration.log-impl: StdOutImpl` 会打印所有 SQL 及参数到控制台；`logging.level.com.xiyue: debug` 输出调试日志。
- **后果**：生产环境控制台/日志会暴露密码哈希、手机号、订单信息、JWT pwdSig 等。`StdOutImpl` 还绕过日志框架的脱敏控制。
- **修复**：生产 profile 改用 `org.apache.ibatis.logging.slf4j.Slf4jImpl`，日志级别 `info`。建议拆分 `application-prod.yml`。

### 7. JwtUtil 注释与实际算法不符

- **文件**：`backend/src/main/java/com/xiyue/security/JwtUtil.java:18, 28` + `application.yml:32`
- **现象**：注释写「算法 HS256」，但 `Keys.hmacShaKeyFor(bytes)` + `signWith(key)` 会根据密钥字节数自动选择：≥32 HS256，≥48 HS384，≥64 HS512。`application-local.yml` 的 secret 71 字节 → 实际用 HS512。
- **后果**：文档误导，维护者可能误判安全等级。项目记忆已记录「HS512」，与代码注释矛盾。
- **修复**：更新注释为「根据密钥长度自动选择 HS256/384/512」，或显式 `.signWith(key, Jwts.SIG.HS512)`。

### 8. JwtAuthenticationFilter 每次请求查库

- **文件**：`backend/src/main/java/com/xiyue/security/JwtAuthenticationFilter.java:91`
- **现象**：每个受保护请求都 `sysUserMapper.selectById(userId)` 校验密码签名。
- **后果**：DB 压力随 QPS 线性增长，高并发下成为瓶颈。注释承认 MVP 可接受。
- **修复**：Redis 缓存 pwdSig，key=`user:pwdsig:{userId}`，TTL 与 token 一致或更短；改密码时删除缓存。

### 9. ReviewController 评价查询返回评价人 ID

- **文件**：`backend/src/main/java/com/xiyue/review/controller/ReviewController.java:38-42` + `ReviewResponse`
- **现象**：`getByOrder` 用 `@PreAuthorize("isAuthenticated()")`，任意登录用户可查任意订单评价，且 `ReviewResponse` 返回 `userId`（评价人 ID）。
- **后果**：轻微信息泄露，暴露评价人身份。评价内容本身公开，但评价人 ID 是否需要保密看业务。
- **修复**：`ReviewResponse` 不返回 `userId`，或仅管理员可见。

### 10. 取消订单退款记录非条件更新

- **文件**：`backend/src/main/java/com/xiyue/order/service/OrderService.java:214-220`
- **现象**：第二次 update（写退款字段）只用 `eq(id)`，无状态条件。虽然此时订单已为已取消(7)，理论无并发，但不够严谨。
- **修复**：合并到第一次条件更新（一次 update 同时写 status + refund 字段），或加 `eq(status, CANCELLED)`。

---

## P3 — 低优先级（建议改进）

### 11. 投诉/评价内容无长度限制

- **文件**：`ComplaintCreateRequest.java` + `ReviewCreateRequest.java`
- **现象**：`reason`/`content` 仅 `@NotBlank`，无 `@Size(max=...)`。TEXT 字段无上限，用户可提交超长文本。
- **修复**：加 `@Size(max = 500, message = "内容不超过500字")`。

### 12. CompleteRequest.imageUrl 无 URL 格式校验

- **文件**：`backend/src/main/java/com/xiyue/order/dto/CompleteRequest.java`
- **现象**：仅 `@NotBlank`，未校验是否合法 URL。
- **修复**：加 `@Pattern(regexp = "^https?://.*")` 或自定义校验。

### 13. AuntUpdateRequest.price 无下限校验

- **文件**：`AuntUpdateRequest.java`（未读，推断）
- **现象**：price 可传 0 或负数。
- **修复**：加 `@DecimalMin(value = "0.01")`。

### 14. OrderNoGenerator 无防时钟回拨

- **文件**：`order/util/OrderNoGenerator.java`
- **现象**：依赖系统时钟，时钟回拨可能产生重复订单号（唯一索引兜底）。
- **影响**：MVP 可接受，生产环境建议引入序列号或雪花算法。

### 15. grabList 抢单大厅不校验阿姨状态

- **文件**：`OrderService.java:242-258`
- **现象**：休息/下架/禁用阿姨都能浏览抢单大厅（抢单时才校验）。
- **影响**：非 bug，前置校验更友好。

### 16. OrderService.cancel 释放档期依赖 oldStatus 而非当前 DB 状态

- **文件**：`OrderService.java:225-231`
- **现象**：用查询时的 `oldStatus` 判断是否释放档期。条件更新成功说明状态未变，逻辑正确，但耦合度高。
- **影响**：当前正确，重构时易出错。

---

## 亮点（做得好的地方）

1. **抢单事务设计扎实**：条件更新 `WHERE status=待抢单 AND aunt_id IS NULL` + 档期唯一索引双重保障，并发安全。
2. **密码签名 pwdSig 机制**：改密码后旧 token 立即失效（ADR-014），安全设计到位。
3. **角色取自数据库**：JWT 角色不信任客户端，register 角色白名单禁止 ADMIN。
4. **防枚举**：密码登录「手机号或密码错误」统一提示，不暴露手机号是否存在。
5. **资源归属校验**：`getOrderAndCheckOwnership` 不存在时不暴露存在性。
6. **逻辑删除 @TableLogic**：aunt 软删除，数据保留。
7. **状态机全条件更新**：所有状态流转都用 `WHERE status=?` 防并发。
8. **手机号脱敏日志**：日志只记录后 4 位。
9. **分页参数防护**：page≥1，size 限制 1~100。
10. **.gitignore 完善**：application-local.yml、.env、密钥、target/ 均忽略。

---

## 修复优先级建议

1. **立即修复（阶段 5 前）**：P1#1 频率限制竞态、P1#4 评分更新丢失、P1#5 逻辑删除检查
2. **尽快修复**：P1#2 校验非原子、P1#3 订单号重试、P2#6 生产日志级别
3. **迭代优化**：P2#7-10、P3 全部

---

## 第二轮：逻辑不自洽问题（非代码 bug）

> 以下问题不是代码层面的 bug，而是文档与实现、文档之间、业务逻辑上的矛盾或不自洽。
>
> **2026-07-13 修复更新**：L1-L13 全部已修复（文档同步 + ADR 补充 + 代码修复）。编译通过 0 错误。

### L1. "ADMIN 由初始化 SQL 创建"三处文档与 ADR-013/代码矛盾

- **规范 `04-agent-project-spec.md` §7.1（第 248 行）**："管理员账号只由初始化 SQL 创建"
- **`architecture.md`（第 72 行）**："ADMIN 只能由初始化 SQL 创建"
- **ADR-013 决策**：应用启动时由 `AdminAccountInitializer`（ApplicationRunner）创建，init.sql 只建表
- **实际代码**：`AdminAccountInitializer.java` 用 ApplicationRunner 创建
- **矛盾**：规范和架构文档说"SQL 创建"，ADR-013 和代码是"应用启动初始化"。规范未随 ADR-013 更新。
- **修复**：规范 §7.1 和 architecture.md 改为"由应用启动时 ApplicationRunner 初始化"。

### L2. ADR-020"读-算-写"与代码"SQL 原子更新"矛盾

- **ADR-020 决策**："采用方案 1（读-算-写）"
- **`progress.md`（第 49 行）**："ADR-020 评价评分读-算-写"
- **实际代码**：已改为 `AuntMapper.updateRatingAndCount`（SQL 原子更新，即 ADR-020 的方案 2）
- **矛盾**：ADR 说选方案 1，代码改成了方案 2（修复 P1#4 时变更），ADR 和 progress 未更新。
- **修复**：ADR-020 状态改为 Deprecated，新增 ADR-021"评价评分改用 SQL 原子更新"；progress.md 更新描述。

### L3. ADR-015"逻辑删除不检查"与代码"检查进行中订单"矛盾

- **ADR-015 后果**："存在历史订单禁止物理删除的检查留待阶段3补充，逻辑删除本身不禁止"
- **实际代码**：`AuntService.deleteByAdmin` 已加进行中订单检查（status IN 2,3,4，修复 P1#5 时变更）
- **矛盾**：ADR 说逻辑删除不检查，代码已加了检查。ADR 未更新。
- **修复**：ADR-015 后果更新为"逻辑删除前检查进行中订单，存在则拒绝"。

### L4. api.md 订单详情权限标注遗漏 AUNT 角色

- **`api.md`（第 52 行）**："`/api/orders/{id}` GET USER 订单详情"
- **实际代码**：`@PreAuthorize("hasAnyRole('USER','AUNT')")` + 按角色分流到 `getDetailForUser`/`getDetailForAunt`
- **矛盾**：文档只标注 USER，遗漏 AUNT 也可访问此接口查自己接的订单。
- **修复**：api.md 权限列改为"USER/AUNT"。

### L5. api.md 逻辑删除说明与代码行为不符

- **`api.md`（第 40 行）**："逻辑删除/注销阿姨（存在历史订单时禁止物理删除）"
- **实际代码**：逻辑删除检查**进行中订单**（待服务/服务中/待确认），不是"历史订单"；且无物理删除接口
- **矛盾**：文档描述"历史订单禁止物理删除"，代码做的是"进行中订单禁止逻辑删除"，两者描述的检查对象和删除类型都不同。
- **修复**：api.md 说明改为"逻辑删除阿姨（存在进行中订单时拒绝，历史数据保留）"。

### L6. 规范 §4"物理删除"与代码无物理删除接口矛盾

- **规范（第 122 行）**："已有历史订单的阿姨禁止物理删除"
- **实际代码**：只有逻辑删除接口（`DELETE /api/admin/aunts/{id}` → `@TableLogic` update deleted=1），无物理删除接口
- **矛盾**：规范描述的"物理删除"场景在代码中根本不存在。规范在描述一个不存在的功能的限制条件。
- **修复**：规范 §4 改为"阿姨删除默认逻辑删除；存在进行中订单时拒绝删除"，移除"物理删除"描述，或补充"物理删除需直接操作数据库"。

### L7. database.md 建议索引未在建表脚本实现

- **`database.md`（第 183 行）**：建议 `complaint(status, create_time)` 索引用于管理员投诉列表
- **`schema.sql`**：complaint 表只有 `uk_complaint_order`，无 `status+create_time` 索引
- **矛盾**：文档建议的查询索引在建表脚本中不存在，管理员投诉列表查询（按 status 筛选 + 按 create_time 排序）会全表扫描。
- **修复**：schema.sql complaint 表加 `KEY idx_status_create (status, create_time)`。

### L8. ADR-016 两套权限拒绝机制 HTTP 状态码不一致

- **Controller 层**（@PreAuthorize 拒绝）：GlobalExceptionHandler 返回 **HTTP 200** + body code=403
- **Filter 层**（URL 级拒绝）：RestAccessDeniedHandler 返回 **HTTP 403** + body
- **矛盾**：同一个"无权限"场景，前端会收到两种不同的 HTTP 状态码（200 vs 403），前端拦截器需特殊处理两套逻辑。
- **修复**：统一为 HTTP 403（GlobalExceptionHandler 的 AccessDeniedException 也设置 `response.setStatus(403)`），或统一为 HTTP 200 + body code（RestAccessDeniedHandler 改为 200）。建议前者，符合 HTTP 语义。

### L9. OrderStatus.isPaid 方法语义与命名不符

- **方法**：`isPaid(code)` 返回 `code != PENDING_PAY && code != CANCELLED`
- **问题**：已取消(7)的订单可能曾经支付过（已退款），但 `isPaid(7)` 返回 false。方法名"isPaid"暗示"是否已支付"，但实际语义是"取消时是否需要记录退款"。
- **矛盾**：命名误导维护者。投诉中(8)返回 true（确实已支付），但已取消(7)返回 false（曾支付但已退款）——按"是否已支付"语义，已取消曾支付的应为 true。
- **修复**：重命名为 `isRefundable(code)` 或 `needsRefund(code)`，语义更准确。

### L10. 规范 §4 阿姨"设置具体日期和小时块不可接单"未实现且无裁剪说明

- **规范（第 116 行）**：阿姨角色支持"设置具体日期和小时块不可接单"
- **实际代码**：只有 `PATCH /api/aunts/me/status` 设置整体接单状态（AVAILABLE/RESTING），无按日期/小时块设置不可接单的接口
- **矛盾**：规范明确列出的功能完全未实现，且无 ADR 说明为何裁剪。`aunt_booking_slot` 表设计上可支持此功能（档期占用记录），但未开放阿姨自主设置接口。
- **修复**：要么补实现（新增接口让阿姨标记某日期某小时块不可接单），要么在规范中标注"MVP 不实现，阶段5+ 考虑"并补 ADR。

### L11. 规范 §5.2"指定阿姨订单"未实现且无裁剪说明

- **规范（第 144 行）**："指定阿姨订单（辅助）：按阿姨当前标价计算"
- **实际代码**：`CreateOrderRequest` 无指定阿姨字段，创建订单时 amount 由用户传入，不支持"指定阿姨"模式
- **矛盾**：规范提到"指定阿姨订单"模式（用户选特定阿姨下单，按阿姨标价计算），代码完全未实现。aunt 表有 price 字段但仅用于列表展示，未用于订单计算。
- **修复**：规范 §5.2 标注"指定阿姨模式 MVP 不实现，仅支持抢单模式"，或补实现。

### L12. schema.sql 标题"阶段 1"但包含全部阶段建表

- **`schema.sql`（第 3 行）**："数据库建表脚本（阶段 1）"
- **实际内容**：sys_user/aunt（阶段1）+ service_order/aunt_booking_slot（阶段3）+ review/complaint（阶段4）
- **矛盾**：标题说"阶段 1"，实际包含 4 个阶段的所有表。
- **修复**：标题改为"全量建表脚本（阶段 1-4）"。

### L13. 抢单时阿姨姓名快照可能为 null

- **`AuthService.register`**：阿姨注册时创建 aunt 记录，`name=null`（需管理员后续编辑补充）
- **`OrderService.grab`**：抢单时 `order.setAuntName(aunt.getName())` → 可能写入 null
- **矛盾**：抢单成功后订单快照的 aunt_name 为 null，用户查看订单详情看不到阿姨姓名。业务逻辑上，阿姨注册后名字为空，但抢单后订单应显示阿姨姓名——快照了空值。
- **修复**：抢单时若 name 为 null，用手机号尾号或"阿姨{id}"兜底；或注册时强制要求填姓名。
