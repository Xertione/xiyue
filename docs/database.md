# 数据库设计

> 记录数据库名称、表结构、字段说明、索引信息和初始化数据。

---

## 数据库信息

- **数据库名称：** `xiyue_platform`
- **字符集：** `utf8mb4`
- **排序规则：** `utf8mb4_unicode_ci`

---

## 核心表清单

| 表名 | 作用 | 状态 |
|---|---|---|
| `sys_user` | 用户/阿姨/管理员账号 | ✅ 已创建（阶段1） |
| `aunt` | 阿姨资料（个人信息 + 运营状态） | ✅ 已创建（阶段1，运营字段编辑留阶段2） |
| `service_order` | 预约订单（含地址快照、价格快照） | 🔲 待创建（阶段3） |
| `aunt_booking_slot` | 阿姨小时块档期占用记录 | 🔲 待创建（阶段3） |
| `review` | 用户评价 | 🔲 待创建（阶段4） |
| `complaint` | 用户投诉 | 🔲 待创建（阶段4） |

关系约束：

- `aunt.user_id` 唯一关联 `sys_user.id`，阿姨账号只能通过自行注册创建；
- 管理员不能新增阿姨账号，只能管理已注册阿姨的资料和状态；
- 阿姨删除默认采用逻辑删除，存在历史订单时禁止物理删除。

---

## 表结构详情

### sys_user — 用户/阿姨/管理员账号（阶段1已建）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键ID |
| phone | VARCHAR(11) | NOT NULL, UNIQUE(`uk_phone`) | 手机号（11位） |
| password | VARCHAR(72) | NOT NULL | BCrypt 密码哈希 |
| role | VARCHAR(10) | NOT NULL | 角色：USER / AUNT / ADMIN |
| nickname | VARCHAR(50) | NULL | 昵称 |
| create_time | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

引擎 InnoDB，字符集 utf8mb4 / utf8mb4_unicode_ci。

**ADMIN 账号初始化方式（ADR-013）：** 不通过 init.sql 写明文密码，而由 `AdminAccountInitializer`（ApplicationRunner）在应用启动时检查：若无 ADMIN 角色账号，则用环境变量 `ADMIN_INIT_PASSWORD` 经 BCrypt 加密后插入。默认手机号 `13800000000`（可由 `ADMIN_PHONE` 覆盖）。密码只走环境变量，不入 SQL/源码/Git。

### aunt — 阿姨资料（阶段1已建，运营字段编辑留阶段2）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键ID |
| user_id | BIGINT | NOT NULL, UNIQUE(`uk_user_id`) | 关联 sys_user.id（一对一） |
| name | VARCHAR(50) | NULL | 姓名（注册后可补充） |
| avatar | VARCHAR(255) | NULL | 头像URL |
| price | DECIMAL(10,2) | NULL | 标价（指定阿姨模式用） |
| rating | DECIMAL(2,1) | NOT NULL DEFAULT 0.0 | 星级（0.0 - 5.0） |
| service_count | INT | NOT NULL DEFAULT 0 | 服务次数 |
| skill_tags | VARCHAR(255) | NULL | 技能标签（逗号分隔） |
| intro | TEXT | NULL | 个人介绍 |
| admin_status | VARCHAR(20) | NOT NULL DEFAULT 'AVAILABLE' | 管理状态：AVAILABLE / OFF_SHELF / DISABLED |
| accept_status | VARCHAR(20) | NOT NULL DEFAULT 'AVAILABLE' | 接单状态：AVAILABLE / RESTING |
| deleted | TINYINT | NOT NULL DEFAULT 0 | 逻辑删除：0=未删 1=已删（MyBatis-Plus @TableLogic，ADR-015） |
| create_time | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

引擎 InnoDB，字符集 utf8mb4 / utf8mb4_unicode_ci。

**阶段1范围：** 随阿姨注册事务一并创建记录（name=null，admin_status/accept_status 默认 AVAILABLE，rating=0.0，service_count=0）。运营字段编辑、列表、详情、上下架、逻辑删除接口在阶段2「阿姨管理」实现。

建表脚本：`backend/src/main/resources/db/schema.sql`（幂等 `CREATE TABLE IF NOT EXISTS`，由 `spring.sql.init` 启动自动执行）。

---

## 订单状态枚举

| 值 | 名称 | 说明 |
|---:|---|---|
| 0 | 待支付 | 用户创建订单成功，尚未支付 |
| 1 | 待抢单 | 模拟支付成功，等待阿姨抢单或管理员指派 |
| 2 | 待服务 | 阿姨已抢单或被指派，等待阿姨开始服务 |
| 3 | 服务中 | 阿姨开始服务 |
| 4 | 待确认 | 阿姨提交服务完成，等待用户确认 |
| 5 | 待评价 | 用户确认服务完成，等待用户评价 |
| 6 | 已完成 | 用户已提交评价，订单闭环 |
| 7 | 已取消 | 用户主动取消 |
| 8 | 投诉中 | 用户提交投诉，管理员处理中 |

---

## 阿姨状态枚举

### 管理状态（管理员控制）

| 值 | 说明 |
|---|---|
| AVAILABLE | 可用 / 可接单 |
| OFF_SHELF | 下架 |
| DISABLED | 禁用 |

### 接单状态（阿姨或系统控制）

| 值 | 说明 |
|---|---|
| AVAILABLE | 可抢单 |
| RESTING | 休息，暂不接单 |

档期占用由 `aunt_booking_slot` 表管理，不与以上字段混用。

---

## 关键索引

| 表 | 索引名 | 字段 | 类型 |
|---|---|---|---|
| sys_user | `uk_phone` | `phone` | 唯一索引 |
| aunt | `uk_user_id` | `user_id` | 唯一索引 |
| service_order | `uk_order_no` | `order_no` | 唯一索引 |
| aunt_booking_slot | `uk_aunt_date_hour` | `aunt_id, service_date, hour_slot` | 联合唯一索引 |
| review | `uk_review_order` | `order_id` | 唯一索引 |
| complaint | `uk_complaint_order` | `order_id` | 唯一索引 |

建议查询索引：

- `service_order(user_id, status, create_time)`：用户订单列表；
- `service_order(aunt_id, status, service_date)`：阿姨订单列表；
- `service_order(status, service_date)`：抢单大厅和管理员筛选；
- `complaint(status, create_time)`：管理员投诉列表。

---

## 金额约定

- 数据库字段类型：`DECIMAL(10,2)`
- Java 字段类型：`BigDecimal`
- 禁止使用 `double` 存储金额

## 模拟退款字段

已支付订单取消时，`service_order` 至少记录：

- `refund_status`：未退款 / 模拟退款成功；
- `refund_no`：模拟退款流水号；
- `refund_time`：模拟退款时间。

模拟退款仅用于演示业务状态，不代表真实资金流转。

---

## 建表脚本

- 脚本位置：`backend/src/main/resources/db/schema.sql`（幂等 `CREATE TABLE IF NOT EXISTS`）
- 执行方式：Spring Boot `spring.sql.init.mode=always`，应用启动时自动执行
- 已建表（阶段1）：`sys_user`、`aunt`
- 待建表：`service_order`、`aunt_booking_slot`（阶段3）、`review`、`complaint`（阶段4）
