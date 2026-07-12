# 息悦生活家政平台：Codex 协作规范

## 启动流程

处理本项目中的开发、排错、评审或文档更新任务前，先读取
`.codex/skills/xiyue-start/SKILL.md`，并按其中的步骤加载项目上下文。

## 工作边界

- 使用中文沟通、注释和文档。
- 一次只实现一个明确、可验证的功能模块；需求不明确时先提问。
- 以 `docs/04-agent-project-spec.md` 为最高项目规范；不得擅自引入未批准的技术或功能。
- 不删除、重命名或移动现有文件，除非用户明确要求；发现无关的工作区改动时不得覆盖或回退。
- 完成功能、解决问题、作出架构取舍、修改数据库或接口时，按项目 Skill 更新对应文档。

## 命令行与编码防坑规则

> 以下规则用于避免在 Windows + Git Bash 环境下反复被编码、路径、转义问题卡住。所有 Agent 必须遵守。

### 1. 编码：统一 UTF-8
- 本机默认 `platform encoding: GBK`，所有源码、配置、文档统一 UTF-8。
- Maven 项目 `pom.xml` 必须声明：
  ```xml
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  <maven.compiler.encoding>UTF-8</maven.compiler.encoding>
  ```
- Spring Boot `application.yml` 必须声明：
  ```yaml
  server.servlet.encoding.charset: UTF-8
  server.servlet.encoding.force: true
  ```
- 写文件一律用 Write 工具，不要用 `echo >` / `cat <<EOF`，避免 shell 转义和编码污染。

### 2. 路径格式：Windows 程序只认 `C:/`
- 传给 Windows 程序（java.exe、mvn.cmd 等）的路径必须用 `C:/...` 格式，**不要用 `/c/...` MSYS 格式**，否则程序找不到文件。
- Git Bash (MINGW) 下的脚本（如 mvn）在 `cygwin=false` 时不做路径转换，会原样把 `/c/...` 传给 java.exe 导致失败。
- 工具调用的参数路径用绝对路径；路径含空格用双引号包裹。

### 3. 不要用 `cmd /c` 调用命令
- `cmd /c`、`cmd //c` 会被安全策略拦截。如需在 cmd 环境执行命令，请用户在自己的终端跑，不要由 Agent 调用。

### 4. heredoc 与安全策略
- `cat <<'EOF'` 的内容里**不要出现** `cmd /c`、`rm -rf`、`wsl`、`reg query`、`schtasks` 等字符串，否则整条命令会被安全策略误拦。
- 需要写多行文件时一律用 Write 工具，不要用 heredoc。

### 5. 工具选择优先级（不要用命令行版本）
| 操作 | 用什么 | 不要用什么 |
|---|---|---|
| 写文件 | Write | echo / cat / Set-Content |
| 读文件 | Read | cat / head / tail / Get-Content |
| 编辑文件 | Edit | sed / awk |
| 找文件 | Glob | find / ls -R |
| 搜内容 | Grep | grep / rg / Select-String |
| 执行命令 | Bash 工具优先 | PowerShell 工具输出捕获不稳定，仅作备用 |

### 6. Maven 调用
- 本机 Maven 3.9.16 在 cmd / IDE 里正常；Git Bash 里 `mvn` 脚本因 MINGW 路径问题报 `ClassNotFoundException`。
- bash 下跑 Maven 命令用 wrapper：`/c/Users/Jodio/tools/mvn17.sh <args>`（已用 `C:/` 格式路径绕过）。
- Maven 启动器 jar 在 `boot/plexus-classworlds-*.jar`，**不是 `lib/`**。

### 7. 先确认真因，不写多余 workaround
- 遇到环境问题先定位根因，能用改配置/环境变量解决的就不要写绕过脚本。
- 用户能自己修复的环境问题（如 PATH），Agent 只提示，不替用户操作。
