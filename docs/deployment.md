# 部署手册

> 记录从一台新服务器开始部署项目的完整步骤。

---

## 服务器信息

- **配置：** 阿里云 2 核 2G
- **系统：** Linux（Ubuntu 22.04 LTS）
- **IP：** （待填写）
- **SSH 端口：** 22

---

## 前置条件

- [ ] Docker 安装
- [ ] Docker Compose 安装
- [ ] Git 安装
- [ ] 安全组/防火墙开放端口：80、443（后续）

---

## 部署步骤

```bash
# 1. 拉取代码
git clone <仓库地址>
cd xiyue-life

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env，填写强随机 MySQL/Redis 密码、管理员初始密码和 JWT_SECRET
# .env 不得提交到 Git；JWT_SECRET 至少 32 字节且不得使用默认短字符串

# 3. 启动服务
docker compose up -d

# 4. 查看日志
docker compose logs -f

# 5. 初始化数据库
# 执行 init.sql 或等 Spring Boot 自动初始化

# 6. 验证
curl http://<公网IP>/api/health
```

---

## Docker 服务

| 服务 | 镜像 | 内部端口 | 外部端口 | 说明 |
|---|---|---|---|---|
| mysql | mysql:8.0 | 3306 | 不暴露 | 仅 Compose 内部网络访问 |
| redis | redis:7.0 | 6379 | 不暴露 | 仅 Compose 内部网络访问 |
| backend | 自定义 | 8080 | - | Spring Boot 后端 |
| nginx | nginx:latest | 80 | 80 | 前端 + 反向代理 |

---

## 密钥与网络安全

以下内容不得写入源码、镜像、日志、截图或 Git：

- `JWT_SECRET`、MySQL root/业务账号密码、Redis 密码；
- 管理员初始密码、TLS 私钥、数据库备份；
- 完整 JWT、验证码和包含密码的数据库连接串。

约束：

- 仓库只保留无真实值的 `.env.example`；
- `.env`、`application-local.yml`、`*.key`、`*.pem`、`*.p12`、`*.sql`、备份目录必须被 `.gitignore` 排除；
- JWT 密钥使用密码学安全随机值，至少 32 字节；疑似泄露后立即轮换并使旧 Token 失效；
- 生产环境不映射 MySQL 和 Redis 端口；本地调试如需映射，只绑定 `127.0.0.1`；
- 日志输出前对手机号、Token 和敏感参数脱敏。

## 健康检查与资源限制

2 核 2G 服务器建议先采用保守上限，并根据实际监控调整：

- MySQL：内存上限约 `768m`；
- 后端：内存上限约 `512m`，JVM 最大堆建议 `256m`；
- Redis：内存上限约 `128m`，配置 `maxmemory`；
- Nginx：内存上限约 `64m`；
- 为 MySQL、Redis 和后端配置健康检查，后端只在依赖健康后启动；
- 所有容器配置日志滚动，避免磁盘被日志占满。

## 数据卷

| 服务 | 挂载路径 | 用途 |
|---|---|---|
| mysql | ./docker/mysql/data:/var/lib/mysql | 数据库持久化 |
| redis | ./docker/redis/data:/data | Redis 持久化 |

---

## 日志滚动

```yaml
logging:
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "3"
```

---

*待部署时补充具体命令和踩坑记录。*
