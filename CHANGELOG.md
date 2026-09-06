# Changelog

**Languages:** [English](CHANGELOG.md) | [中文](CHANGELOG.zh.md)

All notable changes to `atlas-richie-ai` are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased] - 2026-07-25

### Added

- `scripts/release.sh snapshot` 现在能端到端跑通：build + 推 SNAPSHOT 到 CNB 私服，文件 **真的可下载**（HTTP 200 + 真实文件大小）
- `scripts/release.sh release` 支持发到 Maven Central
- 发布脚本优先调用 `mvn3`（推荐 Maven 3.9.x），fallback 系统 PATH 上的 `mvn`

### Changed

- 部署从「CNB 私服假装 201 但实际 GET 404」改为「CNB 私服 **真正可下载**」
- `scripts/release.sh` 简化（去掉之前绕过 Maven 4 JdkTransporter 临时方案的代码）
- 项目版本控制改回硬编码 `1.0.0-SNAPSHOT`（不再使用 `${revision}`）

### Removed

- `scripts/cnb-deploy.py`（之前用于绕过 Maven 4 wagon 问题的临时方案，现已不需要）

## [1.0.0-SNAPSHOT] - 2026-07-25

> First public baseline of **Atlas Richie AI**: a Spring AI 2.0 adapter for Volcano Engine VikingDB,
> using the application-supplied `EmbeddingModel` (external-embedding mode). Built on JDK 17+ and
> Maven 4.0 with native CI-friendly versioning.

### Added

- **Vector Store core** — `atlas-richie-ai-vikingdb-store`
    - `VikingDbVectorStore` — production-ready Spring AI `VectorStore` implementation
    - Schema-first metadata: every metadata key must be declared in the collection schema;
      documents carrying undeclared keys are rejected at upsert
    - Native Spring AI filter integration via `VikingDbFilterExpressionConverter`
      (`EQ` / `IN` / `NE` / `NIN` / `LT` / `LTE` / `GT` / `GTE` / `AND` / `OR` / `NOT`)
    - Batched `upsertData` and `deleteData` (`MAX_UPSERT_BATCH_SIZE = 1000` per VikingDB's data-plane limit)
    - Server-side filter delete via `FILTER_DELETE` task with `needConfirm = false`
      and a `deleteByFilter()` extension returning the async `taskId`
    - Optional control-plane `VikingdbApi` for schema initialization and filter-delete;
      degrades to a logged warning when absent
    - Get-then-create pattern with field-level validation when the collection already exists
    - `controlPlane` field is nullable → supports read-only / write-only deployments
- **Spring Boot auto-configuration** — `atlas-richie-ai-autoconfigure-vector-store-vikingdb`
    - `vikingDbDataPlaneClient` / `vikingDbControlPlaneClient` beans
    - `VikingDbConnectionDetails` + `VikingDbPropertiesConnectionDetails` for properties / external service binding
    - `VikingDbClientProperties` (`spring.ai.vectorstore.vikingdb.client.*`) +
      `VikingDbVectorStoreProperties` (`spring.ai.vectorstore.vikingdb.*`) with full schema fields
- **Starter** — `atlas-richie-ai-starter-vector-store-vikingdb`
    - One-stop dependency that bundles store + autoconfigure + Spring AI observation
- **BOM** — `atlas-richie-ai-bom`
    - First-class Maven 4 `<packaging>bom</packaging>` for downstream consumers

### Build & Release

- Adopted **Maven 4.0+** native features:
    - `<modelVersion>4.1.0` + `${revision}` CI-friendly versioning (no `flatten-maven-plugin` needed)
    - Child modules omit `<parent><version></version>` — Maven 4 auto-resolves
    - BOM uses first-class `<packaging>bom</packaging>`
    - Automatic `consumer.pom` generation for downstream consumers
    - Reproducible builds enabled by default
- Configured publishing plugins (inherited by all children):
    - `maven-source-plugin` 3.4.0 — `*-sources.jar`
    - `maven-javadoc-plugin` 3.12.0 — `*-javadoc.jar` with `doclint=all,-missing,-reference`
    - `maven-gpg-plugin` 3.2.7 — GPG signature on all artifacts
    - `central-publishing-maven-plugin` 0.7.0 — Sonatype Central Portal (release); auto-skips SNAPSHOT
- Dual publishing targets:
    - SNAPSHOT → `https://maven.cnb.cool/richie696/repo-richie-snapshot/`
    - release → Maven Central via Sonatype Central Portal
- Complete POM metadata (`<licenses>` / `<developers>` / `<scm>` / `<organization>` /
  `<distributionManagement>` / `<repositories>`) — Maven Central ready

### Documentation

- Bilingual `README.md` / (future) `README.zh.md`
- `CODE_OF_CONDUCT.md` + `.zh.md`
- `CONTRIBUTING.md` + `.zh.md`
- `SECURITY.md` + `.zh.md`
- This changelog (bilingual)
- `LICENSE` (Apache 2.0) + `NOTICE`

### Notes

- Requires **Maven 4.0.0-rc-5+** to build (uses model version 4.1.0)
- Java baseline: 17+ (Spring AI 2.0 requirement)
- `vikingdb-java-sdk` 0.1.11 (data-plane, required)
- `volcengine-java-sdk-vikingdb` 2.0.20 (control-plane, optional but recommended for `initialize-schema: true`)

[1.0.0-SNAPSHOT]: https://github.com/richie696/atlas-richie-ai/releases/tag/v1.0.0-SNAPSHOT