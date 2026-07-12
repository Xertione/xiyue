# 息悦生活家政平台

> 一个面向"日常保洁预约"场景的前后端分离学习型项目。  
> 技术栈：Spring Boot 3 + Vue 3 + MySQL + Redis + Docker Compose

---

## 项目文档导航

本项目采用"项目文档仓库"方式管理，所有文档集中在 `docs/` 目录下。

### 文档分层关系

```text
原始想法（01）
    ↓
待确认问题（02）── 逐条确认 ──→ 决策记录（decision-log）
    ↓
确认后的 MVP 范围（03）
    ↓
给 Agent 的明确指令（04）─────→ 实际开发
    ↓                              ↓
进度记录（progress） ←──── 踩坑记录（troubleshooting）
    ↓
面试复盘（interview-notes）
```

---

## 📂 文档目录

```
xiyue-life/
├── README.md                          ← 你在这里
├── docs/
│   ├── 01-current-plan.md             ← 初始计划与原始调研
│   ├── 02-open-questions.md           ← 待确认事项清单（逐条确认）
│   ├── 03-mvp-and-roadmap.md          ← MVP 范围与演进路线
│   ├── 04-agent-project-spec.md       ← Agent 开发指令稿
│   ├── 05-how-to-use-ai.md            ← AI 协作原则（写给自己）
│   ├── 06-schedule.md                 ← 开发时间计划（4~6 周）
│   ├── 07-documentation-system.md     ← 本文档体系说明（含模板）
│   │
│   ├── progress.md                    ← 项目开发进度
│   ├── troubleshooting.md             ← [开发中追加] 踩坑记录
│   ├── decision-log.md                ← 13 条关键架构决策已记录（ADR-001~013）
│   ├── architecture.md                ← 系统架构说明
│   ├── database.md                    ← 数据库设计（sys_user + aunt 已建，阶段1）
│   ├── api.md                         ← 接口清单（认证模块已实现）
│   ├── test-cases.md                  ← 测试用例清单
│   ├── deployment.md                  ← 部署手册框架
│   ├── changelog.md                   ← v0.3.0 阶段1认证注册
│   └── interview-notes.md             ← 模板，待 MVP 完成后再填充
├── backend/                           ← ✅ Spring Boot 后端（阶段0骨架 + 阶段1认证）
├── frontend/                         ← [待创建] Vue 3 前端工程（用户/阿姨/管理员路由）
├── docker/                            ← [待创建] Docker 配置
└── docker-compose.yml                 ✅ 已就绪（mysql + redis，含 healthcheck/资源限制/日志滚动）
```

---

## 🚀 使用指南

### 如果你是项目开发者（你自己）

按以下顺序使用：

| 步骤 | 做什么 | 看哪个文件 |
|:---:|---|---|
| 1 | 了解项目原始想法和背景 | `docs/01-current-plan.md` |
| 2 | 逐条确认待定问题，写下决策 | `docs/02-open-questions.md` |
| 3 | 定稿 MVP 范围 | `docs/03-mvp-and-roadmap.md` |
| 4 | 明确 AI 协作方式 | `docs/05-how-to-use-ai.md` |
| 5 | 参考时间计划推进 | `docs/06-schedule.md` |
| 6 | 开发中维护进度和踩坑 | `docs/progress.md`、`docs/troubleshooting.md` |
| 7 | 记录重要决策 | `docs/decision-log.md` |
| 8 | 准备面试素材 | `docs/interview-notes.md` |

### 如果你是 AI / Agent

| 做什么 | 看哪个文件 |
|---|---|
| 理解项目全貌 | `docs/03-mvp-and-roadmap.md` |
| 遵循开发规范 | `docs/04-agent-project-spec.md` |
| 了解当前进度 | `docs/progress.md` |
| 了解历史决策 | `docs/decision-log.md` |
| 参考已踩过的坑 | `docs/troubleshooting.md` |

**Agent 禁止事项**：

- 不能把 `02-open-questions.md` 中的"待确认"内容当成最终需求直接开发；
- 不能引入 `04-agent-project-spec.md` 中列出的禁止技术；
- 不能在未确认需求时擅自扩展功能。

---

## 📋 当前项目状态

| 项目 | 状态 |
|---|---|
| 文档体系 | ✅ 已完成初始化 |
| 待确认事项 | ✅ 已全部确认（2026-07-12） |
| MVP 范围 | ✅ 已更新定稿（基于确认结果） |
| Agent 指令 | ✅ 已更新为正式版（基于确认结果） |
| 后端开发 | 🟡 阶段3完成（订单与抢单：建表/创建/支付/抢单事务/并发/指派/取消退款），待阶段4 |
| 前端开发 | 🔲 未开始 |
| 部署 | 🟡 基础设施就绪（mysql + redis 容器） |

---

## 🧭 推荐执行顺序

```text
第一步：创建 docs 目录和前六个核心文件
第二步：阅读并填写 02-open-questions.md，逐条确认
第三步：根据确认结果修改 03-mvp-and-roadmap.md
第四步：根据最终 MVP 更新 04-agent-project-spec.md
第五步：开始阶段 0：Docker、MySQL、Redis、Spring Boot 骨架
第六步：每完成一个小模块，更新 progress、troubleshooting、decision-log

核心关系：
原始想法 → 待确认问题 → 确认后的 MVP 范围 → 给 Agent 的明确指令 → 实际开发进度与踩坑复盘
```

---

## 🔗 核心设计决策速览

> 详见 `docs/03-mvp-and-roadmap.md` 和 `docs/02-open-questions.md`

| 决策点 | 当前倾向 |
|---|---|
| 项目定位 | 学习型、简历型项目，不商业化 |
| 用户端 | Vue 3 H5（非小程序） |
| 服务品类 | 仅"日常保洁" |
| 阿姨端 | 阿姨可登录抢单（与用户共用前端工程） |
| 支付 | 模拟支付 |
| 并发预约 | 数据库唯一索引 + 事务 |
| 架构 | Spring Boot 单体 |
| 部署 | Docker Compose（MySQL + Redis + Nginx + 后端） |
| 地图/LBS | 第一版不做 |

---

## 📝 工作流约定

```text
每完成一个功能 → 更新 progress.md
每解决一个问题 → 记录到 troubleshooting.md  
每做一个取舍 → 记录到 decision-log.md
每天结束前   → Git 提交 + 写一句当日学习点
```

---

*最后更新：2026-07-13*
