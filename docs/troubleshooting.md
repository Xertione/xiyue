# 项目踩坑与解决记录

> 记录开发过程中遇到的问题、原因分析、排查过程和解决方案。  
> 每解决一个问题，在这里追加一条记录。

---

## T-001：Maven 报 `ClassNotFoundException: classworlds.launcher.Launcher`

- **日期：** 2026-07-12
- **现象：** Git Bash 里执行 `mvn -version` 报 `找不到或无法加载主类 org.codehaus.plexus.classworlds.launcher.Launcher`，但 cmd 里同样命令正常。
- **误判过程：**
  1. 一开始在 `apache-maven-3.9.16/lib/` 目录里 grep `classworlds`，没找到 → 误判「lib 缺 jar，Maven 损坏」。
  2. 建议用户重装 Maven。
- **真正原因：**
  1. `plexus-classworlds-2.11.0.jar` 在 **`boot/`** 目录（Maven 3.9.x 标准结构），不在 `lib/`。`lib/` 里 77 个文件是完整的。Maven 本体从未损坏。
  2. Git Bash (MINGW) 下 `mvn` 脚本的 `cygwin=false`，不执行 `cygpath` 路径转换，`CLASSWORLDS_JAR` 保持 `/c/...` MSYS 格式，而 `java.exe` 是 Windows 程序不认 `/c/`，导致找不到 jar。
- **解决方案：**
  - cmd / IDE 里 mvn 一直正常，无需修复。
  - bash 下用 wrapper `C:\Users\Jodio\tools\mvn17.sh`，内部用 `C:/` 格式路径直接调 `java.exe + classworlds Launcher`。
- **教训：**
  1. Maven 的启动器 jar 在 `boot/` 不是 `lib/`，排查时先看 `mvn` 脚本的 `CLASSWORLDS_JAR` 赋值行。
  2. Windows 程序不认 MSYS `/c/` 路径，传给 java.exe 的路径必须 `C:/` 格式。
  3. 先确认真因再下结论，不要因为「在某个目录没找到文件」就判定损坏。

---

## T-002：`cat <<'EOF'` heredoc 命令被安全策略整体拦截

- **日期：** 2026-07-12
- **现象：** 用 `cat >> file.md << 'EOF'` 追加记忆，内容里包含字符串 `cmd /c mvn`，整条命令被安全策略拦截，报 `Command blocked for security: Invoking cmd.exe from Bash bypasses all command validation`。
- **原因：** 安全策略对 heredoc 内容做关键词扫描，检测到 `cmd /c` 等系统级工具调用字符串就拦截整条命令，即使它只是文档内容而非真正要执行。
- **解决方案：** 改用 Edit 工具追加文件内容，不使用 heredoc。
- **规则固化：** 已写入 `AGENTS.md` §4——heredoc 内容不得出现 `cmd /c`、`rm -rf`、`wsl`、`reg query` 等字符串；写多行文件一律用 Write 工具。

---

## T-003：PowerShell 工具输出捕获异常

- **日期：** 2026-07-12
- **现象：** 用 PowerShell 工具执行 `Test-Path "C:\..."` / `Get-Command docker`，返回值不是 `True/False` 或命令路径，而是 `:\WINDOWS\System32\WindowsPowerShell\v1.0\powershell.EXE`（powershell.exe 自身路径）。
- **原因：** PowerShell 工具在某些命令下的 stdout 捕获不稳定，输出被 powershell 进程路径覆盖。
- **解决方案：** 改用 Bash 工具 + `test -f` / `ls` 做文件存在性检测；PowerShell 工具仅作为备用，不依赖其输出。
- **规则固化：** 已写入 `AGENTS.md` §5——执行命令优先用 Bash 工具。

---

## T-004：`platform encoding: GBK` 导致中文乱码风险

- **日期：** 2026-07-12
- **现象：** `mvn -version` 显示 `platform encoding: GBK`，本机默认编码非 UTF-8。
- **风险：** 源码中文注释、application.yml 中文、日志中文可能出现乱码。
- **解决方案：** 项目强制 UTF-8——
  1. `pom.xml` 声明 `project.build.sourceEncoding=UTF-8` 和 `maven.compiler.encoding=UTF-8`。
  2. `application.yml` 声明 `server.servlet.encoding.charset: UTF-8` + `force: true`。
  3. 写文件一律用 Write 工具（保证 UTF-8），不用 `echo >` / `cat <<EOF`。
- **规则固化：** 已写入 `AGENTS.md` §1。

---

## T-005：本机 memurai 占用 6379 导致 Docker Redis 无法启动

- **日期：** 2026-07-12
- **现象：** `docker compose up -d redis` 报 `ports are not available ... 127.0.0.1:6379: bind: Only one usage of each socket address`。
- **原因：** 本机安装了 Memurai（Windows Redis 兼容服务，`memurai.exe` PID 7048）作为开机自启服务监听 `127.0.0.1:6379`，与 Docker Redis 容器端口冲突。
- **尝试：** `sc stop Memurai` 被安全策略禁用（系统级工具）。
- **解决方案：** 将 docker-compose.yml 中 Redis 宿主映射改为 `127.0.0.1:6380:6379`（容器内仍是 6379），application.yml 中 `spring.data.redis.port` 默认 6380。生产环境 Redis 不暴露端口（容器内通信），不受影响。
- **教训：** 本机已有 Redis 兼容服务时，Docker Redis 需避让端口；开发用 6380，部署用容器网络。

---

## T-006：spring-boot:run 启动端口被覆盖成 2018

- **日期：** 2026-07-12
- **现象：** application.yml 配 `server.port: 8080`，但 `mvn spring-boot:run` 启动日志显示 `Tomcat initialized with port 2018`，且 2018 被 WorkBuddy.exe（PID 14988）占用导致启动失败。
- **排查：** `SERVER_PORT`/`MAVEN_OPTS`/`JAVA_TOOL_OPTIONS` 环境变量均为空；mvn17 wrapper 未传 `-Dserver.port`；target/classes/application.yml 编译后仍是 8080；无 application.properties 覆盖。根因（高于 application.yml 的隐藏配置源）未完全定位。
- **解决方案：** 启动时用最高优先级的命令行参数强制：`mvn17.sh spring-boot:run -Dspring-boot.run.arguments=--server.port=8080`。应用正确在 8080 启动，`GET /api/health` 返回 200。
- **教训：** 当 application.yml 的端口被未知源覆盖时，用 `--server.port` 命令行参数（优先级最高）强制；后续可排查 IDEA/Maven 全局配置或环境变量是否注入。

---

## T-007：Git Bash 下 taskkill 参数被路径转换破坏

- **日期：** 2026-07-12
- **现象：** `taskkill //PID 17376 //F` 报 `无效参数/选项 - '//PID'`，未终止进程。
- **原因：** Git Bash (MSYS) 的路径转换规则未将 `//PID` 还原为 `/PID`，taskkill.exe 收到字面 `//PID`。
- **解决方案：** 加 `MSYS_NO_PATHCONV=1` 前缀禁用路径转换：`MSYS_NO_PATHCONV=1 taskkill /PID <pid> /F`。
- **规则固化：** Git Bash 下调用带 `/` 前缀参数的 Windows 命令（taskkill 等）需 `MSYS_NO_PATHCONV=1`。
