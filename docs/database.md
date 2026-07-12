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
| `sys_user` | 用户/阿姨/管理员账号 | 🔲 待创建 |
| `aunt` | 阿姨资料（个人信息 + 运营状态） | 🔲 待创建 |
| `service_order` | 预约订单（含地址快照、价格快照） | 🔲 待创建 |
| `aunt_booking_slot` | 阿姨小时块档期占用记录 | 🔲 待创建 |
| `review` | 用户评价 | 🔲 待创建 |
| `complaint` | 用户投诉 | 🔲 待创建 |

关系约束：

- `aunt.user_id` 唯一关联 `sys_user.id`，阿姨账号只能通过自行注册创建；
- 管理员不能新增阿姨账号，只能管理已注册阿姨的资料和状态；
- 阿姨删除默认采用逻辑删除，存在历史订单时禁止物理删除。

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

*待建表 SQL 生成后更新此文档。*
