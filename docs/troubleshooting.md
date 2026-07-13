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
- **规则固化：** 排查 Maven 问题时先看 mvn 脚本的 CLASSWORLDS_JAR 赋值行；Windows 程序不认 MSYS `/c/` 路径，传给 java.exe 的路径必须用 `C:/` 格式；先确认真因再动手。

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
- **规则固化：** 本机已有 Redis 兼容服务时，Docker Redis 需避让端口；开发用 6380，部署用容器网络。

---

## T-006：spring-boot:run 启动端口被覆盖成 2018

- **日期：** 2026-07-12
- **现象：** application.yml 配 `server.port: 8080`，但 `mvn spring-boot:run` 启动日志显示 `Tomcat initialized with port 2018`，且 2018 被 WorkBuddy.exe（PID 14988）占用导致启动失败。
- **排查：** `SERVER_PORT`/`MAVEN_OPTS`/`JAVA_TOOL_OPTIONS` 环境变量均为空；mvn17 wrapper 未传 `-Dserver.port`；target/classes/application.yml 编译后仍是 8080；无 application.properties 覆盖。根因（高于 application.yml 的隐藏配置源）未完全定位。
- **解决方案：** 启动时用最高优先级的命令行参数强制：`mvn17.sh spring-boot:run -Dspring-boot.run.arguments=--server.port=8080`。应用正确在 8080 启动，`GET /api/health` 返回 200。
- **规则固化：** 当 application.yml 的端口被未知源覆盖时，用 `--server.port` 命令行参数（优先级最高）强制覆盖。

---

## T-007：Git Bash 下 taskkill 参数被路径转换破坏

- **日期：** 2026-07-12
- **现象：** `taskkill //PID 17376 //F` 报 `无效参数/选项 - '//PID'`，未终止进程。
- **原因：** Git Bash (MSYS) 的路径转换规则未将 `//PID` 还原为 `/PID`，taskkill.exe 收到字面 `//PID`。
- **解决方案：** 加 `MSYS_NO_PATHCONV=1` 前缀禁用路径转换：`MSYS_NO_PATHCONV=1 taskkill /PID <pid> /F`。
- **规则固化：** Git Bash 下调用带 `/` 前缀参数的 Windows 命令（taskkill 等）需 `MSYS_NO_PATHCONV=1`。

---

## T-008：MySQL 命令行客户端中文显示乱码（非数据问题）

- **日期：** 2026-07-13
- **现象：** 阶段 1 验证时，`docker exec xiyue-mysql mysql -uroot -p<MYSQL_ROOT_PASSWORD> -e "SELECT nickname FROM sys_user"` 显示管理员昵称 `系统管理员` 为 `?????`，疑似数据乱码。
- **排查：**
  1. 通过 `GET /api/auth/profile` 接口返回 `"nickname":"系统管理员"` 正常显示；
  2. `GET /api/auth/login/password` 登录响应 `nickname` 字段也正常；
  3. 数据库表与连接均为 `utf8mb4`，`application.yml` 已配 `server.servlet.encoding.force: true`。
- **原因：** `docker exec mysql mysql -e` 在 Git Bash（GBK 终端）输出 UTF-8 数据时，客户端编码与终端编码不匹配，导致**显示**乱码；数据库**存储**的 UTF-8 字节本身正确。
- **解决方案：** 这不是数据问题，无需修复。需要确认中文数据时：
  1. 优先用接口返回验证（接口走 Spring Boot UTF-8 响应）；
  2. 或 `docker exec xiyue-mysql mysql --default-character-set=utf8mb4 -uroot -p<MYSQL_ROOT_PASSWORD> -e "..."` 显式指定客户端字符集。
- **规则固化：** mysql 命令行中文乱码时，先通过接口返回或 `--default-character-set=utf8mb4` 复核，不要直接判定数据损坏。

---

## T-009：curl 在 Git Bash 下发送含中文的 JSON body 返回 400

- **日期：** 2026-07-13
- **现象：** 阶段2验证时，`curl -d '{"name":"张阿姨",...}'` 含中文的 PUT 请求返回 `{"code":400,"message":"请求体格式错误或为空"}`，但全英文 body 的 PATCH 请求成功。
- **排查：**
  1. 用 python `urllib.request` 发送相同中文 JSON（`.encode('utf-8')`）→ 200 成功，详情接口确认中文存储正确；
  2. 英文 body 的 curl 请求 → 200 成功；
  3. 排除接口缺陷，定位为 curl 在 Git Bash（Windows）下对中文 body 的编码处理问题。
- **原因：** Windows 的 curl 在 Git Bash 下可能按系统编码（GBK）而非 UTF-8 编码中文 body，服务器按 UTF-8 解析 JSON 失败，抛 `HttpMessageNotReadableException` → 400。
- **解决方案：** 这不是接口缺陷，无需修复。需要测试含中文的请求时：
  1. 用 python `urllib.request` + `.encode('utf-8')` 发送（推荐）；
  2. 或前端 axios（浏览器自动 UTF-8）。
- **规则固化：** curl 中文 body 400 时，先用 python/前端验证排除接口缺陷，不要误判为代码问题。

---

## T-010：@PreAuthorize 越权返回 500 而非 403

- **日期：** 2026-07-13
- **现象：** 阶段2启用 `@EnableMethodSecurity` + `@PreAuthorize` 后，USER 访问 admin 接口返回 `{"code":500,"message":"服务器内部错误"}` 而非 403。
- **排查：**
  1. 查日志：`@PreAuthorize` 拒绝抛 `AccessDeniedException`，被 `GlobalExceptionHandler` 的 `@ExceptionHandler(Exception.class)` 捕获返回 500；
  2. Security 的 `RestAccessDeniedHandler` 未触发——它只处理 Filter 层异常，捕获不到 Controller 方法层的 `AccessDeniedException`。
- **原因：** `@PreAuthorize` AOP 拦截在 Controller 方法执行前抛异常，向上抛到 DispatcherServlet，被 `@RestControllerAdvice` 捕获；Filter 层的 `RestAccessDeniedHandler` 在更外层，无法拦截方法级异常。
- **解决方案：** 在 `GlobalExceptionHandler` 加 `@ExceptionHandler(AccessDeniedException.class)` 返回 `Result.error(ResultCode.FORBIDDEN)`（body code=403）。
- **验证：** 修复后越权请求返回 `{"code":403,"message":"无权限访问"}` ✓
- **规则固化：** 见 ADR-016——Controller 层权限异常由 GlobalExceptionHandler 处理，Filter 层由 RestAccessDeniedHandler 处理，两套分工。

---

## T-011：前端 Vue Router 无限重定向导致白屏（Maximum call stack size exceeded）

- **日期：** 2026-07-13
- **现象：** 前端页面白屏，浏览器控制台报 `Uncaught RangeError: Maximum call stack size exceeded`，错误堆栈在 `vue-router.js` 的 `pushWithRedirect` 无限递归。
- **排查过程：**
  1. 检查所有模块编译状态：Vite + 21 个路由引用模块全部返回 200，无编译错误；
  2. 检查依赖预构建：Vant/ElementPlus/icons 默认导出正常，CSS 存在；
  3. 怀疑 router 守卫 `useAuthStore()` 的 Pinia 时序问题 → 改为直接读 localStorage（未解决）；
  4. 怀疑 localStorage 有无效数据（token 存在但 role 为空）→ 加自动清理脚本（未解决）；
  5. **发现真正根因**：`/` 路径没有路由定义，访问 `/` 时匹配通配符 `/:pathMatch(.*)*`，其 `redirect: '/'` 指向 `/` 本身 → `/` 又匹配通配符 → redirect `/` → `resolve` 阶段无限循环（守卫 `beforeEach` 根本没机会执行）。
- **原因：** Vue Router 4 通配符路由 `redirect: '/'`，当 `/` 本身没有显式路由定义时，`resolve` 阶段会无限匹配通配符并重定向，导致 `pushWithRedirect` 递归溢出。这与守卫逻辑、Pinia、localStorage 无关。
- **解决方案：**
  1. 添加 `{ path: '/', redirect: '/login' }` 路由，使 `/` 有显式定义；
  2. 通配符 `/:pathMatch(.*)*` 的 redirect 从 `'/'` 改为 `'/login'`。
- **验证：** 修复后访问 `/` → redirect `/login` → 守卫检查（未登录）→ `next()` → 登录页正常显示 ✓
- **规则固化：** Vue Router 4 通配符路由的 `redirect` 目标**必须是已显式定义的路由路径**，不能 redirect 到会再次匹配通配符的路径（如 `/` 未定义时 redirect `/`）。排查路由问题时，先确认 `resolve` 阶段是否正常，再查守卫。

---

## T-012：图片上传裂图 / URL 格式不正确 / 黑屏无法退出

**现象：**

1. 点击"加图片"图标上传后，提示"URL 格式不正确"。
2. 上传后前端页面显示的图片是裂开的。
3. 点击裂图查看详情时，跳转至全黑界面，且无法退出。

**排查过程：**

1. 检查 MockUploadController → 最初返回 mock 路径 `/mock-uploads/uuid.jpg` 但不保存文件 → 图片 404 → 裂图；
2. 改为真正保存文件到 `backend/uploads/` + `WebMvcConfig` 映射静态路径 + `SecurityConfig` 白名单 → 后端可访问，但前端仍裂图；
3. **发现真正根因**：Vite 前端监听 5173 端口，`/mock-uploads/uuid.jpg` 从浏览器访问时走 Vite（5173），而文件在后端（8080）。Vite 没有 `/mock-uploads` 代理规则，导致 404。

**原因：** Vite dev server 和 Spring Boot 后端运行在不同端口。前端页面中的相对路径图片 URL 在没有代理的情况下会请求到错误的服务端。

**解决方案：**

1. `vite.config.ts` 加代理规则：`'/mock-uploads': { target: 'http://127.0.0.1:8080', changeOrigin: true }`
2. 重启 Vite 使代理生效。

**验证：** 修复后上传图片 → URL 可正常加载 → 不裂图 → 点击可放大查看 → 支持关闭退出 ✓

**规则固化：** 前后端分离开发时，后端生成的资源 URL（文件上传、静态资源等）必须在 Vite 代理中配置对应的路径转发，否则前端请求会发到 Vite 而非后端。

---

## T-013：提交服务完成始终报"完成图片URL格式不正确"

**现象：**

无论是真正上传图片后点击"确认提交"，还是点击"模拟提交（免传图）"，都提示"完成图片 URL 格式不正确"，无法完成订单。

**排查过程：**

1. 检查前端上传 → 返回 `/mock-uploads/uuid.jpg`，相对路径，正常；
2. 检查前端 complete 请求 → POST body `{"imageUrl": "/mock-uploads/uuid.jpg"}`，格式正常；
3. **发现根因**：`CompleteRequest.java` 的 `@Pattern` 注解写死了 `^https?://\\S+$`，要求 imageUrl 必须以 `http://` 或 `https://` 开头。模拟上传返回的相对路径 `/mock-uploads/uuid.jpg` 不匹配此正则，校验失败。

**原因：** MVP 阶段上传接口返回相对路径（由 WebMvcConfig 映射为静态资源），但 DTO 的正则校验假设 URL 一定是完整的 http(s) URL。两者假设不一致。

**解决方案：**

将 `CompleteRequest.java` 的正则改为同时接受完整 URL 和相对路径：

```java
// 修复前
@Pattern(regexp = "^https?://\\S+$", message = "完成图片URL格式不正确")

// 修复后
@Pattern(regexp = "^(https?://\\S+|/\\S+)$", message = "完成图片URL格式不正确")
```

**验证：** 修复后上传图片或模拟提交均可正常完成服务 ✓

**规则固化：** DTO 验证正则应与接口实际返回/使用的数据格式对齐。当 API 返回相对路径时，验证规则应同时接受相对路径和完整 URL。
