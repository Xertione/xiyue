# 接口约定文档

> 记录所有 API 接口的路径、方法、权限、参数和响应。  
> 即使有 Knife4j 文档，也保留此文件作为业务接口清单。

---

## 通用约定

- 基础路径：`/api`
- 请求体格式：`application/json`
- 统一响应体：`{ "code": 200, "message": "success", "data": ... }`
- 认证方式：`Authorization: Bearer <token>`
- 分页参数：`page`、`size`

---

## 认证模块

| 接口 | 方法 | 权限 | 说明 |
|---|---|---|---|
| `/api/auth/sms-code` | POST | 无需登录 | 发送验证码（固定 123456 写入 Redis Key `sms:code:{phone}`，TTL 5 分钟） |
| `/api/auth/register` | POST | 无需登录 | 用户/阿姨自行注册（角色仅允许 USER/AUNT，禁止 ADMIN） |
| `/api/auth/login/password` | POST | 无需登录 | 密码登录 |
| `/api/auth/login/code` | POST | 无需登录 | 验证码登录 |
| `/api/auth/reset-password` | POST | 无需登录 | 找回密码 |
| `/api/auth/profile` | GET | 登录 | 获取当前用户信息 |

---

## 阿姨模块

| 接口 | 方法 | 权限 | 说明 |
|---|---|---|---|
| `/api/aunts` | GET | USER | 阿姨列表（分页、筛选） |
| `/api/aunts/{id}` | GET | USER | 阿姨详情 |
| `/api/admin/aunts` | GET | ADMIN | 管理员查看全量阿姨 |
| `/api/admin/aunts/{id}` | GET | ADMIN | 管理员查看阿姨详情 |
| `/api/admin/aunts/{id}` | PUT | ADMIN | 编辑已注册阿姨资料 |
| `/api/admin/aunts/{id}` | DELETE | ADMIN | 逻辑删除/注销阿姨（存在历史订单时禁止物理删除） |
| `/api/admin/aunts/{id}/status` | PATCH | ADMIN | 上下架/禁用阿姨 |
| `/api/aunts/me/status` | PATCH | AUNT | 阿姨设置个人接单状态 |

---

## 订单模块

| 接口 | 方法 | 权限 | 说明 |
|---|---|---|---|
| `/api/orders` | POST | USER | 创建订单 |
| `/api/orders` | GET | USER | 用户订单列表 |
| `/api/orders/{id}` | GET | USER | 订单详情 |
| `/api/orders/{id}/pay` | POST | USER | 模拟支付 |
| `/api/orders/{id}/cancel` | POST | USER | 取消待支付/待抢单/待服务订单，按需模拟退款并释放档期 |
| `/api/orders/grab-list` | GET | AUNT | 抢单大厅（待抢单订单列表） |
| `/api/orders/{id}/grab` | POST | AUNT | 阿姨抢单 |
| `/api/orders/mine` | GET | AUNT | 阿姨订单列表 |
| `/api/orders/{id}/start` | POST | AUNT | 开始服务 |
| `/api/orders/{id}/complete` | POST | AUNT | 提交服务完成 |
| `/api/orders/{id}/confirm` | POST | USER | 用户确认服务完成 |
| `/api/admin/orders` | GET | ADMIN | 管理员查看全量订单 |
| `/api/admin/orders/{id}/assign` | POST | ADMIN | 管理员指派阿姨 |

---

## 评价模块

| 接口 | 方法 | 权限 | 说明 |
|---|---|---|---|
| `/api/reviews` | POST | USER | 提交评价 |
| `/api/orders/{id}/review` | GET | 登录 | 查看订单评价 |

---

## 投诉模块

| 接口 | 方法 | 权限 | 说明 |
|---|---|---|---|
| `/api/complaints` | POST | USER | 对待评价订单提交一次投诉 |
| `/api/admin/complaints` | GET | ADMIN | 查看投诉列表 |
| `/api/admin/complaints/{id}/handle` | POST | ADMIN | 条件更新处理投诉，订单进入已完成且不再允许评价 |

---

## 公共

| 接口 | 方法 | 权限 | 说明 |
|---|---|---|---|
| `/api/health` | GET | 无需登录 | 健康检查 |

---

*业务规则以 `docs/04-agent-project-spec.md` 为准；代码、OpenAPI 和本文件必须同步更新，不允许互相漂移。*
