# 安全策略

**语言：** [English](SECURITY.md) | [中文](SECURITY.zh.md)

## 支持的版本

以下版本当前接收安全更新（补丁与修复）：

| 版本  | 是否支持           |
|-------|--------------------|
| 1.0.x | :white_check_mark: |
| < 1.0 | :x:                |

> 当前开发基线为 `1.0.0-SNAPSHOT`。官方安全公告基于已发布的 tag。

## 漏洞上报

如发现 **Atlas Richie AI** 的安全漏洞，请 **不要**在公开 Issue、Discussion 或 PR 中披露利用细节、PoC 或敏感数据。

通过以下任一私密渠道上报：

1. **GitHub Security Advisories**（推荐）
   打开仓库 → **Security** → **Report a vulnerability**。

2. **邮件联系维护者**
   收件人：richie696@icloud.com
   建议主题：`[SECURITY] atlas-richie-ai`

请尽量提供以下信息：

- 受影响模块（如 `atlas-richie-ai-vikingdb-store`）
- 版本（Git tag、`revision` 或产物版本）
- 复现步骤与影响范围
- 可能的缓解措施（如有）

## 流程承诺

- **确认收到**：目标在 **5 个工作日内**确认收悉。
- **评估与修复**：按严重程度排优先级，修复就绪后发布补丁版本。
- **协同披露**：修复发布后会协同公开披露（如 GitHub Security Advisory + `CHANGELOG.md`）。

## 不在范围内

以下情况通常 **不**属于本仓库安全响应范围：

- 上游 Spring AI、火山引擎 VikingDB SDK 等第三方依赖的漏洞（跟踪上游，我们跟随）
- 部署配置问题，如未启用 TLS、IAM 凭据过弱等（在部署文档中加固）
- `target/test-classes` 下的样例 / 演示配置

感谢您通过负责任的披露帮助维护项目安全。