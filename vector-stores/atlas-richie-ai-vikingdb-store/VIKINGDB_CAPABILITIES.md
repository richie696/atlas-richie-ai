# VikingDB 插件底层接口能力清单

本文档面向插件使用者和业务架构设计人员，描述 VikingDB 插件“能够完成什么”。
内容按可调用的接口能力、输入、输出、适用场景和限制组织，不以源码类结构作为说明主线。
VikingDB 与 DashVector 是两套独立的 Spring AI VectorStore 实现；业务使用标准
`VectorStore` 时可以通过替换 Starter 和配置切换实现，高级能力则使用各自厂商的扩展接口。

## 1. 能力范围总览

| 能力域 | 提供内容 | 典型接口 | 状态 |
|---|---|---|---|
| Spring AI 标准向量库 | 文档写入、按 ID 删除、条件删除、相似度检索 | `VectorStore` | 已实现 |
| Collection 管理 | 查询、创建、删除 Collection | `VikingDbCollectionOperations` | 已实现 |
| Index 管理 | 查询、创建、更新、启停、删除、变更计划、等待就绪 | `VikingDbIndexOperations` | 已实现 |
| 向量检索 | Dense、Sparse、Hybrid、分页、分区、过滤、输出字段 | `VikingDbSearchOperations` | 已实现 |
| 关键词检索 | 语义后匹配、BM25、字段和关键词控制 | `VikingDbSearchOperations` | 已实现 |
| 多模态检索 | 文本、图片、视频及 URI/字节输入 | `VikingDbSearchOperations` | 已实现 |
| 检索后处理 | 分数融合、字符串包含/匹配、枚举频率限制 | `VikingDbSearchOperations` | 已实现 |
| 独立重排 | 对候选媒体进行模型重排 | `VikingDbRerankOperations` | 已实现 |
| 预嵌入数据操作 | 直接写入、按 ID 读取、按 ID 删除 | `VikingDbDocumentOperations` | 已实现 |
| 权限需求说明 | 输出调用所需的权限动作，不执行探测写操作 | `VikingDbPermissionOperations` | 已实现 |

## 2. Spring AI 标准 VectorStore 能力

### 2.1 文档写入：`add`

业务可以提交一组 Spring AI `Document`，插件完成以下业务能力：

- 为每个文档生成向量并写入指定 Collection/Index；
- 保留文档 ID、文本内容和 metadata；
- 相同 ID 再次写入时执行 upsert，适合新增和更新共用一条业务链路；
- 支持大批量输入，插件按服务端允许的批量上限自动拆分；
- 在发送请求前检查向量数量、向量维度和非法浮点值，避免部分数据静默写入；
- 空文档列表直接返回，不产生远程写请求。

输入重点：`Document.id`、`Document.text`、`Document.metadata`。

输出语义：标准 `VectorStore.add` 无返回值；成功表示请求已被数据面接受，最终可见性仍
遵循 VikingDB 服务端的一致性模型。

### 2.2 按 ID 删除：`delete(List<String>)`

- 删除一个或多个文档 ID；
- 自动处理大于单批上限的 ID 列表；
- 空 ID 列表是成功的空操作；
- 适合用户删除、文档替换前清理、测试资源回收。

成功表示删除请求被接受，不承诺所有读副本在同一时刻已经刷新。

### 2.3 按条件删除：`delete(Filter.Expression)`

- 使用 Spring AI Filter 表达式描述删除条件；
- 将条件提交为 VikingDB 的过滤删除任务；
- 适合按租户、文档类型、业务状态或时间标签批量清理；
- 需要控制面权限；
- 删除是异步任务，调用成功表示任务已提交，不等同于物理删除立即完成。

需要任务编号或审计信息时，使用扩展的 `deleteByFilter`，它会返回服务端 taskId。

### 2.4 相似度检索：`similaritySearch`

- 输入自然语言查询文本；
- 返回按相似度排序的 Spring AI `Document` 列表；
- 支持 `topK`、metadata Filter 和相似度阈值；
- 命中记录的文本映射为 Document 内容，其余字段映射为 metadata；
- 阈值过滤后返回空列表是正常业务结果，不表示远程服务失败；
- 查询使用与 Store 绑定的 Project、Collection 和 Index。

## 3. Collection 管理接口

以下接口用于管理逻辑数据集合，适用于部署初始化、运维工具和租户资源管理。

### 3.1 查询 Collection：`getCollection`

可获得：

- Project、Collection 身份和描述；
- 全部字段名称及字段类型；
- 主键字段信息；
- 向量字段及维度；
- 关联 Index 名称；
- 是否具备关键词检索能力。

适用场景：启动前检查、控制台展示、schema 兼容性检查和故障诊断。

### 3.2 创建 Collection：`createCollection`

可定义：

- Collection 名称和所属 Project；
- 文本、数值、布尔、向量等字段；
- 主键字段；
- 向量维度；
- 描述信息；
- 删除保护开关。

创建返回标准化的 Collection 信息，便于调用方继续创建 Index 或记录资源元数据。

### 3.3 删除 Collection：`deleteCollection`

- 删除指定 Collection 及其数据资源；
- 必须由调用方显式执行；
- 适合临时环境和租户销毁流程；
- 属于破坏性操作，不能当作普通文档删除使用。

### 3.4 Schema 准备能力

启用 schema 初始化后，插件可以：

- Collection 不存在时按声明创建；
- 检查已有 Collection 是否包含 `doc_id`、`content`、`embedding` 和 metadata 字段；
- 检查向量维度是否与业务配置一致；
- Index 不存在时按索引声明创建；
- 发现资源不匹配时报告错误，避免把数据写入错误的 Collection/Index。

该能力用于环境准备，不会替业务自动迁移或重写已有生产索引。

## 4. Index 索引生命周期接口

### 4.1 查询与创建

`getIndex` 可以读取 Index 的状态、索引类型、距离度量、量化方式、构建参数和分片信息。

`createIndex` 可以声明：

- 索引类型：HNSW、HNSW_HYBRID、FLAT、DISKANN；
- 距离度量：COSINE、IP、L2；
- 量化：FLOAT、INT8、FIX16、PQ；
- HNSW 的 M、构建 ef、搜索 ef；
- DiskANN 的 M、构建 ef、缓存比例；
- PQ 编码比例；
- scalar index、分片数、分片策略、CPU 配额；
- 描述、资源 ID 和删除保护。

### 4.2 更新、启用、停用、删除

- `updateIndex`：调整允许变更的索引属性；
- `enableIndex`：启用索引服务；
- `disableIndex`：停用索引服务；
- `deleteIndex`：删除索引资源。

这些操作均是控制面操作，不会隐式改变业务写入或查询代码。

### 4.3 变更计划与就绪等待

`planChange` 可在真正更新前判断：

- `NOOP`：无需变更；
- `ONLINE_UPDATE`：可以在线调整；
- `REBUILD_REQUIRED`：需要重建索引；
- `INCOMPATIBLE`：目标定义不完整或不兼容。

`awaitState` 用于等待 Index 进入指定状态，适合发布、迁移和实网验收流程。
索引尚未就绪应被视为可重试的资源状态，而不是直接判断为权限失败。

## 5. 高级检索接口

统一检索接口支持向量、关键词和多模态三类请求。每类请求都可以指定资源、过滤条件、
分页、分区、输出字段和后处理选项。

### 5.1 Dense 向量检索

- 输入 Dense 向量；
- 按 COSINE、IP 或 L2 等 Index 度量召回；
- 支持 topK、offset、partition、output fields；
- 支持 mandatory filter 与本次 query filter；
- 可返回 schema、下载 URL、分析结果和详细检索信息；
- 适合常规语义搜索、RAG 召回和相似内容推荐。

### 5.2 Sparse 向量检索

- 输入非空 Sparse 向量（词项到权重的映射）；
- 适合关键词敏感、词面匹配较强的检索；
- 支持与 Dense 检索相同的过滤、分页、分区和输出控制；
- 可单独使用，也可作为 Hybrid 检索的一路输入。

### 5.3 Hybrid 检索

- 同时输入 Dense 和 Sparse 向量；
- 通过 `denseWeight` 控制 Dense 侧权重；
- 支持两路共同过滤、ID 包含/排除和后处理；
- 适合同时要求语义召回和关键词精确性的知识库检索。

### 5.4 通用高级参数

| 参数 | 能力说明 |
|---|---|
| `limit` / `offset` | 控制返回数量和分页位置 |
| `partition` | 将查询限制在指定分区 |
| `outputFields` | 指定返回字段，减少无关数据 |
| `returnSchema` | 返回字段 schema |
| `returnDownloadUrl` | 请求可下载资源地址 |
| `returnAnalyzedResult` | 返回服务端分析结果 |
| `returnDetailInfo` | 返回更完整的检索明细 |
| `idsIn` | 仅在指定 ID 集合内检索 |
| `idsNotIn` | 排除指定 ID 集合 |
| `postProcessInputLimit` | 限制进入后处理的候选数量 |
| `scaleK` | 调整后处理缩放参数 |
| `filterPreAnnLimit` | 设置过滤前 ANN 候选上限 |
| `filterPreAnnRatio` | 设置过滤前 ANN 候选比例 |

### 5.5 检索后处理

支持以下服务端后处理能力：

- `SCORE_FUSION`：融合多路或多阶段分数；
- `STRING_CONTAIN`：按字符串包含关系进一步筛选或调整；
- `STRING_MATCH`：按字符串匹配关系处理；
- `ENUM_FREQ_LIMITER`：限制枚举值频率，避免结果被单一类别占满。

后处理适合在召回之后加入业务相关性、字段匹配和结果分布控制。

## 6. 关键词检索接口

支持两种模式：

- `SEMANTIC_THEN_MATCH`：先进行语义召回，再进行关键词匹配；
- `BM25`：使用 BM25 进行词项相关性排序。

可控制：

- 查询文本和关键词列表；
- 参与检索的字段；
- 大小写敏感性；
- BM25 的 `k1`、`b` 参数；
- topK、分页、分区、过滤和输出字段。

适用场景：产品名、错误码、API 名称、法规条款和需要保留词面信息的检索。

## 7. 多模态检索接口

一次查询可以提供以下一种或多种输入：

- 文本；
- 图片；
- 视频。

每种媒体支持：

- `TEXT`：直接传文本；
- `URI`：传对象存储或可访问地址；
- `BYTES`：直接传二进制内容。

还可以配置：

- 是否自动补全多模态检索指令；
- Tensor Rerank；
- Model Rerank；
- mandatory/query 双过滤；
- 分页、分区、输出字段及后处理。

适用场景：图文检索、视频片段检索、跨模态知识库和多媒体内容推荐。

## 8. Rerank 接口

### 8.1 查询内 Rerank

多模态查询可以显式附带 Tensor Rerank 或 Model Rerank：

- Tensor Rerank：输入二维 tensor、候选上限和最大相似度算法；
- Model Rerank：指定模型名称/版本、指令、候选上限、阈值、失败策略和超时。

Rerank 是显式能力，不会自动改变普通相似度检索的排序行为。

### 8.2 独立 Rerank

`rerank` 接口可以对已有候选进行独立重排：

- 输入 query 媒体和候选媒体集合；
- 指定模型名称、版本和指令；
- 设置是否返回原始媒体、最大重试次数；
- 返回候选索引、重排分数、原始数据、requestId 和 token 使用量。

适合“先向量召回、再模型精排”的两阶段检索架构。

## 9. 直接文档数据接口

### 9.1 预嵌入写入：`upsertRecords`

当业务已经自行生成向量时，可以直接提交：

- 文档 ID；
- 文本内容；
- Dense 向量；
- Sparse 向量；
- metadata。

该接口不重复调用 EmbeddingModel，适合离线建库、跨模型迁移和批量导入。

### 9.2 按 ID 读取：`fetchByIds`

- 一次读取一个或多个文档；
- 可指定 partition 和 output fields；
- 返回文档字段、Dense/Sparse 向量；
- 同时告知不存在的 ID。

### 9.3 按 ID 删除：`deleteByIds`

- 支持对象 ID 列表；
- 支持分批处理；
- 适合数据修复、迁移和清理工具。

## 10. Filter 能力

Spring AI Filter 可表达并组合以下条件：

- 逻辑：AND、OR、NOT；
- 比较：EQ、NE、GT、GTE、LT、LTE；
- 集合：IN、NIN；
- 空值：ISNULL、ISNOTNULL。

支持嵌套逻辑组合。字段名、操作数类型和集合非空性会在请求发送前校验；字符串中的
单引号会进行安全转义。

过滤条件可用于相似度查询、关键词查询、多模态查询和过滤删除。VikingDB 的过滤能力
受 Collection 字段声明和 scalar index 配置影响，未声明字段不能当作可用过滤字段。

## 11. 权限与错误能力

### 11.1 权限需求查询

权限接口可以列出各类操作所需的权限动作，包括：

- Collection 读取、创建、删除；
- Index 读取、创建、更新、启停、删除；
- 文档 upsert、delete、fetch；
- 过滤删除；
- Dense、Sparse、Hybrid、关键词、多模态检索；
- Rerank。

该接口只返回声明，不通过创建资源或修改数据来探测权限。

### 11.2 错误分类

插件将错误区分为：

- `RESOURCE_NOT_READY`：Index 构建中或暂不可用，通常可以重试；
- `PERMISSION_DENIED`：账号、项目或资源授权不足；
- `PROVIDER_FAILURE`：其他服务端或网络错误。

错误信息包含操作、资源、provider code、requestId、是否可重试及修复提示，便于业务
重试、告警和问题定位。服务端返回 HTTP 成功但业务 code 失败时，仍按失败处理。

## 12. 配置所能启用的能力

### 12.1 连接配置

可配置数据面和控制面的：

- Host、Control Endpoint、Region；
- AK/SK 或 API Key 认证模式；
- HTTPS/HTTP；
- 连接、读、写超时时间。

### 12.2 Store 配置

可配置：

- Project、Collection、Index；
- 向量维度；
- 是否自动初始化 schema；
- Collection 描述、分片数和 scalar index；
- metadata 字段；
- Filter 校验模式；
- Index 类型、距离、量化和构建参数；
- 检索默认分页、分区、Hybrid 权重和预 ANN 参数。

配置的作用是确定默认资源和默认行为，不会改变高级接口的能力范围。

## 13. 能力边界

- 一个标准 Store 面向一个固定的 Project、Collection 和 Index；不是动态路由器；
- 当前没有 Partition 创建/删除、Alias、Backup/Restore 或跨 Collection 融合接口；
- 普通 `similaritySearch` 不会自动调用 Rerank；
- 过滤删除是异步任务；
- 未开启 schema 初始化时，不自动创建或修复生产资源；
- 编译和单元测试通过，只能证明插件可用性，不能证明具体账号、项目和 Index 已授权或
  已达到可查询状态；真实云验收需要单独进行。

