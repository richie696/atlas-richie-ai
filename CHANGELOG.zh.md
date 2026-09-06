# 变更日志

**语言：** [English](CHANGELOG.md) | [中文](CHANGELOG.zh.md)

本文档记录 `atlas-richie-ai` 所有显著变更。

格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
项目遵循 [语义化版本](https://semver.org/lang/zh-CN/spec/v2.0.0.html)。

## [1.0.0-SNAPSHOT] - 2026-07-25

> **Atlas Richie AI** 首个公开基线：Spring AI 2.0 适配火山引擎 VikingDB，
> 使用应用自带的 `EmbeddingModel`（外部嵌入模式）。构建于 JDK 17+ 与 Maven 4.0
> （原生 CI-friendly 版本机制）。

### 新增

- **Vector Store 核心** —— `atlas-richie-ai-vikingdb-store`
    - `VikingDbVectorStore` —— 生产级 Spring AI `VectorStore` 实现
    - Schema-first metadata：每个 metadata 键必须在 collection schema 中声明，
      携带未声明键的文档在 upsert 时被拒绝
    - 通过 `VikingDbFilterExpressionConverter` 集成 Spring AI 原生过滤器
      （`EQ` / `IN` / `NE` / `NIN` / `LT` / `LTE` / `GT` / `GTE` / `AND` / `OR` / `NOT`）
    - 分批 `upsertData` 与 `deleteData`（`MAX_UPSERT_BATCH_SIZE = 1000`，匹配 VikingDB 数据面上限）
    - 通过 `FILTER_DELETE` 任务实现服务端 filter delete（`needConfirm = false`），
      扩展方法 `deleteByFilter()` 返回异步 `taskId`
    - 可选控制面 `VikingdbApi` 用于 schema 初始化与 filter-delete；
      缺失时降级为日志告警
    - get-then-create 模式，collection 已存在时做字段级校验
    - `controlPlane` 字段可空 —— 支持只读 / 只写部署
- **Spring Boot 自动装配** —— `atlas-richie-ai-autoconfigure-vector-store-vikingdb`
    - `vikingDbDataPlaneClient` / `vikingDbControlPlaneClient` Bean
    - `VikingDbConnectionDetails` + `VikingDbPropertiesConnectionDetails`（支持属性绑定或外部服务绑定）
    - `VikingDbClientProperties`（`spring.ai.vectorstore.vikingdb.client.*`）+
      `VikingDbVectorStoreProperties`（`spring.ai.vectorstore.vikingdb.*`）含完整 schema 字段
- **Starter** —— `atlas-richie-ai-starter-vector-store-vikingdb`
    - 一站式依赖，捆绑 store + autoconfigure + Spring AI observation
- **BOM** —— `atlas-richie-ai-bom`
    - 一等公民的 Maven 3 `<packaging>bom</packaging>`，供下游消费者使用

### 文档

- 双语 `README.md` / （未来）`README.zh.md`
- `CODE_OF_CONDUCT.md` + `.zh.md`
- `CONTRIBUTING.md` + `.zh.md`
- `SECURITY.md` + `.zh.md`
- 本变更日志（双语）
- `LICENSE`（Apache 2.0）+ `NOTICE`

### 说明

- 构建要求 **Maven 3.9+**
- Java 基线：17+（Spring AI 2.0 要求）
- `vikingdb-java-sdk` 0.1.11（数据面，必需）
- `volcengine-java-sdk-vikingdb` 2.0.20（控制面，可选；`initialize-schema: true` 时建议引入）

[1.0.0-SNAPSHOT]: https://github.com/richie696/atlas-richie-ai/releases/tag/v1.0.0-SNAPSHOT