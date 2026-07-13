# 项目开发进度

> 记录当前项目做到哪里、已完成什么、下一步做什么。

---

## 当前阶段

阶段：阶段 5 — 前端联调与部署  
状态：**已完成**（Vue 3 前端工程 + 三角色页面 + Docker/Nginx 部署配置，dev 联调验证通过）  
最后更新时间：2026-07-13

---

## 已完成（5 阶段全部完成，MVP 闭环）

### 阶段 0：项目基础设施（2026-07-12）
- [x] 文档体系 + 本地环境 + Docker Compose + 配置收口 + Spring Boot 骨架 + 基础设施

### 阶段 1：认证与注册（2026-07-13）
- [x] sys_user+aunt 建表 + 注册/登录/找回密码 + JWT(HS512+pwdSig) + Security + sms频率限制

### 阶段 2：阿姨管理（2026-07-13）
- [x] aunt deleted @TableLogic + 列表/筛选/详情/编辑/上下架/禁用/逻辑删除 + @PreAuthorize 角色隔离

### 阶段 3：订单与抢单（2026-07-13）
- [x] service_order+aunt_booking_slot + 创建/支付/抢单事务/并发/指派/取消退款档期释放

### 阶段 4：服务履约与评价（2026-07-13）
- [x] review+complaint + start/complete/confirm + 评价评分更新 + 投诉处理（9状态机全打通）

### 阶段 5：前端联调与部署（2026-07-13）
- [x] Vue 3 + Vite + TS + Pinia + Vue Router + Vant 4（用户/阿姨端）+ Element Plus（管理后台）
- [x] API 封装（axios + JWT 拦截器 + 401 跳登录）+ auth store + 路由守卫（角色分发）
- [x] 登录/注册页（验证码倒计时、角色选择）
- [x] 用户端：阿姨列表/详情/发布订单/订单列表/订单详情（支付/取消/确认/评价/投诉）
- [x] 阿姨端：抢单大厅/我的订单/订单详情（开始服务/提交完成/接单状态切换）
- [x] 管理后台：阿姨管理/订单管理（指派）/投诉处理
- [x] teal-600 品牌色全站一致性（skill 设计原则：颜色 lock、反 AI-tell、完整状态）
- [x] 部署：frontend Dockerfile（多阶段构建）+ nginx.conf（SPA + API 反代）+ backend Dockerfile + docker-compose 四服务
- [x] dev 联调验证通过（index.html + API 代理 /api→8080 + 模块加载）

---

## 阶段 5 验证（2026-07-13）

| 验证项 | 结果 |
|---|---|
| Vite dev server 启动（5173） | ✅ |
| index.html 正确加载 | ✅ |
| API 代理 /api → backend:8080 | ✅ health up |
| main.ts 模块可访问 | ✅ HTTP 200 |
| npm install 完成（77 包） | ✅ |
| teal 主题定制（Vant + Element Plus） | ✅ |
| docker-compose 四服务配置 | ✅ |

### 阶段 5 后续修复（v0.7.2 → v0.7.4，2026-07-13）

**v0.7.2（5 项修复）：**
- [x] SMS 发送间隔 60s→30s，倒计时加固
- [x] 退出登录（用户端+阿姨端底部 tab"我的"→退出按钮）
- [x] 阿姨端导航栏小红点（待服务+服务中订单计数 badge）
- [x] 模拟图片上传（MockUploadController + van-uploader）
- [x] 阿姨个人信息设置（GET/PUT /api/aunts/me/profile + 前端表单）

**v0.7.3（6 项修复）：**
- [x] 上传裂图→真实落盘（WebMvcConfig + SecurityConfig 白名单 /mock-uploads/**）
- [x] 多图上传（max-count=9 + 逗号拼接 + ImagePreview 放大）
- [x] 小红点 bug 修复（r2.total 非 r2.data.total + onMounted）
- [x] 下拉刷新无效（Layout CSS min-height + overflow-y: auto）
- [x] 个人资料保存失败（前端校验姓名/年龄/年限 + 后端错误提示）
- [x] 可接单按钮说明文字

**v0.7.4（3 项修复）：**
- [x] 图片 URL 裂图（vite.config.ts 加 /mock-uploads 代理）
- [x] 模拟提交按钮（OrderDetail "模拟提交免传图"用占位 URL）
- [x] 我的订单 tab 红点（待服务/服务中/全部每个 tab 独立 badge 计数）

---

## 当前阻塞问题

```text
暂无
```

---

## 最近一次可运行状态

```text
开发模式（联调）：
  后端：cd backend && /c/Users/Jodio/tools/mvn17.sh spring-boot:run -Dspring-boot.run.arguments=--server.port=8080
  前端：cd frontend && npm run dev（或 node node_modules/vite/bin/vite.js）
  访问：http://127.0.0.1:5173

生产部署：
  cp .env.example .env && 填入真实值
  docker compose up -d --build
  访问：http://127.0.0.1（nginx 托管前端 + 反代后端）
```
