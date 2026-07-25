# 贡献指南

**语言：** [English](CONTRIBUTING.md) | [中文](CONTRIBUTING.zh.md)

感谢您有兴趣为 `atlas-richie-ai` 做出贡献。

## 原则

- 贡献应契合项目目标，遵守模块边界。
- 提交前确保代码可编译、测试通过、文档同步更新。
- 所有贡献遵循仓库根目录 [LICENSE](./LICENSE)（Apache License 2.0）。

## 提交流程

1. Fork 仓库并创建特性分支（如 `feature/<name>` 或 `fix/<name>`）。
2. 本地开发与自测，避免引入构建失败。
3. 提交 Pull Request，说明：
   - 动机
   - 主要变更
   - 验证方式
   - 兼容性影响（如有）
4. 根据评审反馈迭代直至合并。

## 提交规范

- 使用清晰、命令式的提交信息，先解释**为什么**再说**做了什么**。
- 修改公共 API、配置或行为时同步更新 README 或模块文档。
- 新增配置项必须有合理默认值与示例，避免破坏现有用户。

## 代码风格

- 所有 public 类、字段、方法均带**中文 Javadoc**（`@param` / `@return` / `@throws`）。
- 遵循已有模块结构 —— 每个 Spring AI 适配器都以 4 模块形式交付：
  - `*-store`（`VectorStore` 实现）
  - `*-autoconfigure-*`（Spring Boot 自动装配）
  - `*-starter`（BOM 风格便捷启动器）
  - `*-bom`（可选，仅当父 POM 管理多个适配器时存在）
- 一个 VectorStore 对应一个顶级包；适配器之间不互相依赖。

## 行为准则与安全

- 保持专业、尊重、建设性。参见 [CODE_OF_CONDUCT.md](./CODE_OF_CONDUCT.md) / [CODE_OF_CONDUCT.zh.md](./CODE_OF_CONDUCT.zh.md)。
- **不要**公开披露安全问题。遵循 [SECURITY.md](./SECURITY.md) / [SECURITY.zh.md](./SECURITY.zh.md)。

## 本地构建

本项目要求 **Maven 3.9+ 或 Maven 4.0+**（使用 `<modelVersion>4.0.0` + 硬编码 `<version>1.0.0-SNAPSHOT</version>`）。**部署到 CNB 推荐 Maven 3.9.x** —— Maven 4.0.0-RC5 的 JdkTransporter 与 CNB 的 401 响应（无 `WWW-Authenticate` 头）有 auth-flow 兼容问题。

```bash
# 单模块单元测试
mvn -pl vector-stores/atlas-richie-ai-vikingdb-store test

# 整 reactor 构建 + 测试
mvn clean verify
```

## 发布（仅维护者）

请使用 `scripts/` 目录下的脚本，不要直接跑 `mvn deploy`。脚本封装了所有必要的开关和前置检查。

```bash
# SNAPSHOT → CNB 私服（集成测试）
./scripts/release.sh snapshot

# release → 通过 Sonatype Central Portal 发布到 Maven Central
./scripts/release.sh release

# 仅跑校验不部署（CI 演练 / PR 检查）
./scripts/release.sh dry-run
```

Windows / IDEA 用户：`scripts\release.bat <mode>` —— 行为一致。

### 一次性环境准备（release 机器）

首次发布前，在将执行签名的机器上生成 GPG key：

```bash
./scripts/setup-gpg.sh
# 1. 自动检测 gpg 是否安装（macOS / Debian / CNB 都覆盖）
# 2. 交互式生成新 key
# 3. 公钥上传到 keys.openpgp.org
# 4. 打印要追加到 ~/.m2/settings.xml 的 <profile> 片段
```

服务器凭据（`~/.m2/settings.xml`）需要三组 `<server>` 条目：

| `<server><id>` | 用途 |
|---|---|
| `richie696-repo-richie-snapshot` | CNB SNAPSHOT 仓库 |
| `richie696-repo-richie-release` | CNB release 仓库 |
| `central` | Sonatype Central Portal |

外加 `setup-gpg.sh` 生成的 `gpg` profile（`<gpg.keyname>` + `<gpg.passphrase>`）。

### 为什么用脚本而不是直接 `mvn deploy`？

IDEA 自带的打包器无法传 Maven 参数（`-Dgpg.skip=true` / `-Dmaven.deploy.skip=true`）。脚本把这些开关固化下来，确保不会漏。`release.sh` 还做：

- 检查 `mvn` 和 `gpg` 在 `PATH` 里
- 按模式选用正确的开关组合
- 输出清晰的 banner 让执行人知道接下来会发生什么

## 相关文档

- [SECURITY.md](./SECURITY.md) / [SECURITY.zh.md](./SECURITY.zh.md) — 漏洞上报与支持版本
- [CHANGELOG.md](./CHANGELOG.md) / [CHANGELOG.zh.md](./CHANGELOG.zh.md) — 发布说明
- [CODE_OF_CONDUCT.md](./CODE_OF_CONDUCT.md) / [CODE_OF_CONDUCT.zh.md](./CODE_OF_CONDUCT.zh.md) — 社区准则