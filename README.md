# 息悦生活家政平台

> 一个面向"日常保洁预约"场景的前后端分离学习型项目。  
> 技术栈：Spring Boot 3 + Vue 3 + Vant 4 + Element Plus + MySQL + Redis + Docker Compose

---

## ⚡ 快速启动

### 前置条件

- JDK 17+
- Docker & Docker Compose
- Node.js 18+
- Maven 3.9+

### 开发模式（前后端联调）

```bash
# 0. 首次使用：复制环境变量模板
cp .env.example .env   # 编辑 .env 填入本地密码

# 1. 启动基础设施（MySQL + Redis）
docker compose up -d mysql redis

# 2. 启动后端（端口 18080，Windows 端口保留区避让，见 T-006）
cd backend
mvn spring-boot:run

# 3. 启动前端（端口 5173，自动代理 /api、/mock-uploads → 18080）
cd frontend
npm install
npm run dev

# 4. 浏览器访问 http://127.0.0.1:5173
```

> **提示**：Windows Git Bash 下 Maven 路径问题见 `docs/troubleshooting.md` T-001

### 停止开发服务器

```bash
# 前端 + 后端：在对应终端按 Ctrl+C
# 基础设施：docker compose down
```

### 生产部署（Docker Compose 四服务）

```bash
cp .env.example .env   # 填入真实密码和 JWT 密钥
docker compose up -d --build
# 本地访问 http://127.0.0.1
# 远程服务器访问 http://<服务器公网IP>
```

### 测试账号（本地开发）

> 仅 ADMIN 账号由系统自动创建。USER 和 AUNT 需通过注册页面自行创建。
> 注册方式：访问 /register，手机号 + 密码 + 验证码 123456 + 角色（USER/AUNT）。

| 角色 | 手机号 | 密码 | 说明 |
|---|---|---|---|
| ADMIN | 13800000000 | `.env` 中 `ADMIN_INIT_PASSWORD` 的值 | 启动时自动初始化（需先 `cp .env.example .env`） |
| USER | 13800000001 | 注册时设置 | 普通用户（先注册再使用） |
| AUNT | 13800000002 | 注册时设置 | 阿姨（先注册再使用） |

---

## 📂 项目结构

```
xiyue-life/
├── README.md                          ← 项目说明
├── docs/                              ← 项目文档（17 篇）
│   ├── 01~07-*.md                     ← 规划文档（想法/待确认/MVP/规范/AI协作/计划/文档体系）
│   ├── progress.md                    ← 开发进度（5 阶段全部完成）
│   ├── troubleshooting.md             ← 踩坑记录（T-001~T-013）
│   ├── decision-log.md                ← 架构决策（ADR-001~026）
│   ├── architecture.md                ← 系统架构
│   ├── database.md                    ← 数据库设计（6 表全建）
│   ├── api.md                         ← 接口清单（全部实现）
│   ├── test-cases.md                  ← 测试用例
│   ├── deployment.md                  ← 部署手册
│   └── changelog.md                   ← 更新记录（v0.7.5 最新）
├── backend/                           ← ✅ Spring Boot 后端（阶段0~4 全部完成）
│   ├── src/main/java/com/xiyue/      ← Java 源码（~80 文件）
│   │   ├── common/                    ← Result/异常/枚举/HealthController
│   │   ├── config/                    ← Security/OpenApi/AdminInit/MybatisPlus/WebMvc
│   │   ├── security/                  ← Jwt/Filter/EntryPoint/Denied/LoginUser/Context
│   │   ├── auth/                      ← 认证（controller/service/dto）
│   │   ├── user/                      ← 用户（entity/mapper）
│   │   ├── aunt/                      ← 阿姨（entity/enums/dto/service/controller/mapper）
│   │   ├── order/                     ← 订单（entity/enums/dto/service/controller/mapper/util）
│   │   ├── review/                    ← 评价（entity/dto/service/controller/mapper）
│   │   ├── complaint/                 ← 投诉（entity/dto/service/controller/mapper）
│   │   ├── integration/               ← MockPaymentService（模拟支付）
│   │   ├── upload/                    ← MockUploadController（模拟上传）
│   │   └── admin/                     ← 管理员 Controller（阿姨/订单/投诉）
│   ├── src/main/resources/
│   │   ├── application.yml            ← 配置（${VAR} 环境变量占位）
│   │   ├── application-local.yml      ← 本地配置（.gitignore 忽略）
│   │   └── db/schema.sql              ← 建表脚本（6 表，幂等）
│   ├── uploads/                        ← 模拟上传文件目录（.gitignore 忽略）
│   ├── Dockerfile                     ← 后端镜像（maven build → jre）
│   └── pom.xml
├── frontend/                          ← ✅ Vue 3 前端（阶段5 完成）
│   ├── src/
│   │   ├── api/                       ← axios 封装 + API 模块
│   │   ├── stores/                    ← Pinia（auth）
│   │   ├── router/                    ← Vue Router（守卫 + 角色分发）
│   │   ├── layouts/                   ← UserLayout/AuntLayout（Vant tabbar）/AdminLayout（Element Plus）
│   │   ├── views/                     ← 页面（登录/注册 + 用户5 + 阿姨4 + 管理3）
│   │   ├── utils/                     ← format 工具
│   │   └── styles/main.css            ← 全局样式 + teal 主题
│   ├── Dockerfile                     ← 前端镜像（node build → nginx）
│   ├── nginx.conf                     ← Nginx 配置（SPA + /api 反代）
│   ├── vite.config.ts                 ← Vite 配置（代理 /api + /mock-uploads → 18080）
│   └── package.json
├── docker-compose.yml                 ← ✅ 四服务（mysql + redis + backend + frontend）
├── .env.example                       ← 环境变量模板（入库）
├── .env                               ← 本地环境变量（.gitignore 忽略）
└── AGENTS.md                          ← Agent 协作规范
```

---

## 📋 当前项目状态

| 项目 | 状态 |
|---|---|
| 文档体系 | ✅ 已完成（17 篇） |
| 后端开发 | 🟢 阶段4完成（9状态订单状态机全打通，含画像/上传/个人资料） |
| 前端开发 | 🟢 MVP 完成（v0.7.5，含上传/红点/下拉/退出/校验/一致性同步） |
| 部署 | 🟢 阶段5完成（Dockerfile + nginx + docker-compose 四服务） |

### 5 阶段 MVP 全部完成（v0.7.5）

1. ✅ 项目基础设施（Spring Boot 骨架 + Docker + 基础设施）
2. ✅ 认证与注册（JWT + Spring Security + 验证码）
3. ✅ 阿姨管理（列表/筛选/编辑/上下架/逻辑删除/角色隔离）
4. ✅ 订单与抢单（创建/支付/抢单事务/并发/指派/取消退款）
5. ✅ 服务履约与评价（start/complete/confirm/评价评分/投诉处理）
6. ✅ 前端联调与部署（三角色页面 + Docker/Nginx 部署）

---

## 📝 文档导航

| 做什么 | 看哪个文件 |
|---|---|
| 了解项目全貌与 MVP 范围 | `docs/03-mvp-and-roadmap.md` |
| 遵循开发规范 | `docs/04-agent-project-spec.md` |
| 了解当前进度 | `docs/progress.md` |
| 了解历史决策 | `docs/decision-log.md`（ADR-001~026） |
| 参考已踩过的坑 | `docs/troubleshooting.md`（T-001~T-013） |
| 查看接口清单 | `docs/api.md` |
| 查看数据库设计 | `docs/database.md`（6 表 + 状态枚举 + 索引） |
| 查看更新记录 | `docs/changelog.md`（v0.1.0~v0.7.5） |
| 部署到服务器 | `docs/deployment.md` |

---

## 🧩 核心设计决策

| 决策点 | 方案 | ADR |
|---|---|---|
| 架构 | Spring Boot 单体 + 模块化分包 | ADR-001 |
| 并发抢单 | 数据库唯一索引 + 订单条件更新 + 事务 | ADR-002/010 |
| 订单状态机 | 9 状态（待支付→待抢单→待服务→服务中→待确认→待评价→已完成/已取消/投诉中） | ADR-003 |
| 阿姨状态分离 | 管理状态（admin）+ 接单状态（aunt）+ 档期表 | ADR-008 |
| 敏感配置 | 仅环境变量注入，不入源码/Git | ADR-012 |
| ADMIN 初始化 | 应用启动时 BCrypt 加密入库，密码走环境变量 | ADR-013 |
| JWT 失效 | 密码哈希签名（改密码旧 token 立即失效） | ADR-014 |
| 逻辑删除 | MyBatis-Plus @TableLogic | ADR-015 |
| 模拟上传 | 本地文件落盘 + WebMvcConfig 静态映射 | ADR-024 |
| 阿姨自助编辑 | 阿姨可编辑个人资料（姓名/价格/年龄/年限等） | ADR-025 |
| 前端 | Vue3 + Vant4（移动端）+ Element Plus（后台），teal 品牌色 | - |

---

## 📝 工作流约定

```text
每完成一个功能 → 更新 progress.md + changelog.md
每解决一个问题 → 记录到 troubleshooting.md  
每做一个取舍 → 记录到 decision-log.md（ADR）
每个阶段完成 → git commit + push（至少 5 次 push）
```

---

*最后更新：2026-07-13*
