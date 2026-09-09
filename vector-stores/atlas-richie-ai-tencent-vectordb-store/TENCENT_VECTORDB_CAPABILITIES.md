# 腾讯云 VectorDB 插件底层接口能力清单

本文档面向插件使用者、业务开发和架构设计人员，说明腾讯云 VectorDB 插件可提供的
业务接口能力。内容按接口用途、输入、结果、适用场景和限制编排，不按源码类结构说明。

VectorDB 与 VikingDB、DashVector 是三套独立的 Spring AI VectorStore 实现。业务使用
Spring AI 标准 `VectorStore` 时可以通过替换 Starter 和配置切换；需要厂商专属能力时，
使用本插件暴露的 VectorDB 扩展接口。

> 当前腾讯云 VectorDB 没有可用的实网账号和 Endpoint，本文档描述的是 SDK 及当前插件
> 已接入的接口能力；真实云行为、权限和版本兼容性仍处于待测试状态。

## 1. 能力范围总览

| 能力域 | 提供内容 | 典型接口 | 当前状态 |
|---|---|---|---|
| Spring AI 标准向量库 | 文档写入、按 ID 删除、Filter 删除、相似度检索 | `VectorStore` | 已实现，待实网验证 |
| Database 管理 | 创建、幂等创建、存在性、列出、删除 | `TencentVectorDbDatabaseOperations` | 已实现，待实网验证 |
| Collection 管理 | 创建、幂等创建、列出、描述、清空、删除 | `TencentVectorDbCollectionOperations` | 已实现，待实网验证 |
| Alias 管理 | 设置和删除 Collection 别名 | `TencentVectorDbCollectionOperations` | 已实现，待实网验证 |
| 文档数据 | Upsert、Query、Update、Delete、Count | `TencentVectorDbDocumentOperations` | 已实现，待实网验证 |
| 向量检索 | 向量相似度搜索、按 ID 搜索 | `TencentVectorDbSearchOperations` | 已实现，待实网验证 |
| 托管 Embedding 检索 | 以文本/Embedding Items 作为查询输入 | `searchByText` | 已实现，待实网验证 |
| 全文检索 | Match 条件、过滤、字段选择、召回向量 | `fullTextSearch` | 已实现，待实网验证 |
| 混合检索 | ANN 与全文 Match 组合、过滤、重排 | `hybridSearch` | 已实现，待实网验证 |
| 索引管理 | 增加、删除、重建、修改向量索引 | `TencentVectorDbIndexOperations` | 已实现，待实网验证 |
| AI 数据库 | AI Database 创建、读取、删除 | `TencentVectorDbAiOperations` | 已实现，待实网验证 |
| 文件处理 | 上传文件、图片 URL、文件详情 | `TencentVectorDbAiOperations` | 已实现，待实网验证 |
| 原子 Embedding | 指定模型和数据类型生成向量 | `TencentVectorDbAiOperations` | 已实现，待实网验证 |
| 用户和权限 | 用户、密码、授权和撤销 | `TencentVectorDbPermissionOperations` | 已实现，待实网验证 |

## 2. Spring AI 标准 VectorStore 能力

### 2.1 文档写入：`add`

输入 Spring AI `Document` 列表后，可完成：

- 使用宿主提供的 `EmbeddingModel` 生成 Dense 向量；
- 将文档 ID 写入 VectorDB 主键字段；
- 将正文保存到 `content` 字段；
- 将 metadata 保存为 Collection 中的业务字段；
- 同一 ID 重复写入时执行 Upsert，保证幂等同步；
- 自动处理大批量写入；
- 在远程调用前检查向量维度和 Embedding 数量；
- 空列表作为成功的空操作处理。

适用场景：RAG 知识库导入、文档增量同步、消息幂等消费、离线向量回灌。

输入重点：`Document.id`、`Document.text`、`Document.metadata`。

返回语义：标准 `VectorStore.add` 无业务返回值；成功表示服务端接受写入请求，最终读
可见性取决于 VectorDB 的读一致性设置和服务端索引状态。

### 2.2 按 ID 删除：`delete(List<String>)`

- 删除一个或多个文档 ID；
- 长列表按 SDK 支持的批量限制拆分；
- 空列表不产生远程调用；
- 适合文档撤回、版本替换、租户清理和测试数据回收。

### 2.3 按 Filter 删除：`delete(Filter.Expression)`

- 使用 Spring AI Filter 表达式描述删除条件；
- 条件转换为 VectorDB Filter；
- 可按租户、文档类型、状态、标签等批量删除；
- 适合生命周期清理和条件性数据迁移；
- 删除完成时间受服务端执行和一致性策略影响。

### 2.4 相似度检索：`similaritySearch`

- 输入自然语言查询文本；
- 使用宿主 `EmbeddingModel` 生成查询向量；
- 在绑定的 Database/Collection 中执行向量搜索；
- 支持 `topK`、Filter 和 similarity threshold；
- 返回 Spring AI `Document` 列表；
- VectorDB 的距离分数转换为 Spring AI 使用的相似度分数；
- 没有命中或全部低于阈值时返回空列表。

标准接口面向可替换性，不自动启用全文检索、混合检索、别名、托管 Embedding 或索引
运维能力；这些能力通过扩展接口显式调用。

## 3. Database 管理接口

### 3.1 创建与存在性

- `createDatabase(name)`：创建指定 Database；
- `createDatabaseIfNotExists(name)`：不存在时创建，已存在时保持幂等；
- `databaseExists(name)`：检查 Database 是否存在。

适合多租户资源初始化、环境准备和部署脚本。

### 3.2 查询与删除

- `listDatabases()`：列出当前账号可见的 Database；
- `dropDatabase(name)`：删除 Database 及其 Collection。

`dropDatabase` 是破坏性操作，适合环境销毁或租户注销流程，不应替代文档级删除。

## 4. Collection 管理接口

### 4.1 存在、创建和列出

- `collectionExists(database, collection)`：检查指定 Collection；
- `createCollection(database, param)`：按完整 Schema 创建 Collection；
- `createCollectionIfNotExists(database, param)`：提供幂等创建；
- `listCollections(database)`：列出 Database 下的 Collection。

### 4.2 Collection Schema 能力

创建参数可以描述：

- Collection 名称、描述；
- Shard 数量和副本数量；
- 主键字段；
- 文本、数值、布尔、数组等字段；
- Dense 向量字段；
- 向量维度、IndexType 和 MetricType；
- Filter Index 配置；
- TTL 配置；
- 是否绑定 Database。

VectorDB SDK 支持的向量索引类型包括 FLAT、HNSW、IVF_FLAT、IVF_PQ、IVF_SQ4、IVF_SQ8、
IVF_SQ16、DISK_FLAT、IVF_RABITQ、BIN_FLAT 等；标量索引类型包括 PRIMARY_KEY 和 FILTER。
最终可用类型以目标 VectorDB 版本和 Collection 配额为准。

### 4.3 描述、清空和删除

- `describeCollection(database, collection)`：读取 Collection Schema、索引字段、状态、
  别名和创建信息；
- `truncateCollection(database, collection)`：清空 Collection 数据但保留 Collection
  结构；
- `dropCollection(database, collection)`：删除 Collection 及其数据。

### 4.4 Alias

- `setAlias(database, collection, alias)`：为 Collection 设置访问别名；
- `deleteAlias(database, alias)`：删除别名。

Alias 适合蓝绿 Collection、版本切换和减少业务配置变更。别名权限及切换原子性需要
目标集群实网确认。

## 5. 文档数据接口

### 5.1 Upsert：`upsert(InsertParam)`

- 一次提交一个或多个 VectorDB Document；
- 由文档 ID 判断新增或覆盖；
- 可提交 Dense 向量、字段和其他 SDK 支持的文档属性；
- `buildIndex` 可控制写入时是否构建已有数据索引；
- 返回 `AffectRes`，用于判断服务端操作 code 和 message。

适合幂等导入、实时同步和离线回灌。

### 5.2 通用查询：`query(QueryParam)`

- 按 Filter 查询文档；
- 支持 limit、offset 分页；
- 支持 outputFields 字段选择；
- 支持是否返回向量；
- 支持按字段排序；
- 返回 VectorDB 原生 Document 列表。

适合管理后台、数据核验、字段检索和非向量查询。

### 5.3 更新：`update(UpdateParam, Document/JSONObject)`

- 使用 VectorDB Document 或 JSONObject 作为更新内容；
- 可按更新参数指定目标文档或条件；
- 适合只修改 metadata、状态、标签或部分字段；
- 是否允许同时修改向量取决于目标 Collection Schema 和 SDK 参数。

### 5.4 删除：`delete(DeleteParam)`

- 可按文档 ID 或 Filter 删除；
- DeleteParam 支持限制删除数量；
- 返回影响结果，适合逐批重试和审计。

### 5.5 计数：`count(CountQueryParam)`

- 不带 Filter 时统计 Collection 文档数；
- 带 Filter 时统计满足条件的文档数；
- 适合导入核对、分页总数、租户用量和清理前预估。

## 6. 检索接口

### 6.1 向量相似度检索：`search(SearchByVectorParam)`

输入一个或多个查询向量，可获得按相似度排序的候选文档集合。参数可表达：

- 查询向量；
- 向量字段；
- TopK/limit；
- radius 距离阈值；
- Filter；
- outputFields；
- 是否返回原始向量；
- 其他 SDK `Params` 检索参数。

返回类型为 `List<List<Document>>`，外层列表对应多路查询向量，内层列表是每一路的
候选结果。每条 Document 可包含 ID、字段和 provider score。

### 6.2 按 ID 检索：`searchById(SearchByIdParam)`

- 以一个或多个已有文档 ID 作为查询基准；
- 返回每个 ID 对应的相似文档集合；
- 适合“找相似商品/相似文章/相似图片”和推荐场景；
- 返回结构与向量检索一致，支持多 ID 查询结果分组。

### 6.3 托管 Embedding/文本检索：`searchByText(SearchByEmbeddingItemsParam)`

- 输入文本或 Embedding Items；
- 由 VectorDB SDK/服务端按配置的 Embedding 能力完成查询向量化或处理；
- 返回 `SearchRes`，保留服务端结果和状态信息；
- 适合不希望业务自行管理 Embedding 请求的场景。

该能力是否可用取决于目标 VectorDB 版本、开通的模型和服务端配置；不能与本插件标准
`similaritySearch` 的外部 Embedding 流程混为一谈。

### 6.4 全文检索：`fullTextSearch(FullTextSearchParam)`

支持：

- Match 条件；
- Filter；
- outputFields；
- 是否返回向量；
- limit。

适合关键词、短语、产品型号、错误码和需要词面匹配的场景。返回 `FullTextSearchRes`，
保留 VectorDB 原生结果，不强制映射为 Spring AI 相似度列表。

### 6.5 混合检索：`hybridSearch(HybridSearchParam)`

混合检索可以组合：

- 一个或多个 ANN 向量召回条件；
- 一个或多个 Match/稀疏文本条件；
- Filter；
- outputFields；
- 是否返回向量；
- limit；
- Rerank 参数；
- 数组参数模式。

适合同时依赖语义相关性和关键词精确性的知识库、搜索和推荐业务。具体融合算法、
权重规则和可用 Rerank 方法以目标 VectorDB 服务端能力为准。

## 7. 索引管理接口

### 7.1 增加标量索引：`addIndex`

- 为已有字段增加 Filter 索引；
- 可一次提交多个字段；
- `buildExistedData` 控制是否扫描历史数据建立索引；
- 适合新增可过滤字段和优化条件查询。

### 7.2 删除标量索引：`dropIndex`

- 按字段名列表删除 Filter 索引；
- 删除后字段可能仍可存储，但过滤性能和可用性由服务端决定。

### 7.3 修改向量索引：`modifyVectorIndex`

可重新指定：

- 向量索引类型；
- 相似度计算方式；
- HNSW 的 M、efConstruction；
- IVF 的 nlist；
- IVF_PQ 的 M、nlist；
- 其他 `VectorIndex` 参数。

参数变化可能触发索引重建；调用方可通过 `rebuildRules` 指定重建策略。

### 7.4 重建索引：`rebuildIndex`

- 对指定 Collection/字段执行索引重建；
- 适合大批量写入后、索引参数变更后或索引异常恢复；
- 重建期间的查询可用性和状态转换需要实网确认。

## 8. AI Database、文件和原子 Embedding 能力

### 8.1 AI Database

- `createAiDatabase(name)`：创建 AI Database；
- `aiDatabase(name)`：获取 AI Database 句柄；
- `dropAiDatabase(name)`：删除 AI Database。

AI Database 用于承载面向文档理解、文件解析或 AI 检索的专用资源；其完整能力不等同于
标准 Database/Collection，需按 VectorDB AI 产品开通情况使用。

### 8.2 文件上传：`uploadFile`

支持：

- 本地文件路径；
- InputStream；
- 文件名和长度；
- 分片预处理参数；
- 文档解析参数；
- 字段映射；
- 指定 Embedding 模型；
- 绑定 Database 和 Collection；
- 附加 metadata。

适合“上传文件—解析—切分—向量化—写入 Collection”的托管知识库流程。

### 8.3 图片 URL：`getImageUrl`

- 根据文件名和文档 ID 获取图片访问地址；
- 适合图片预览、文档详情和多媒体回源。

### 8.4 文件详情：`queryFileDetails`

- 按文件名筛选；
- 支持 Filter；
- 支持 outputFields；
- 支持 limit/offset；
- 返回文件解析或入库详情。

### 8.5 原子 Embedding：`atomicEmbedding`

输入：

- 模型名称；
- 数据类型；
- 模型参数；
- 文本数据列表。

返回 `AtomicEmbeddingRes`，用于直接获得 VectorDB 托管模型产生的 Embedding。适合
批量预计算、模型能力验证和不接入独立 EmbeddingModel 的数据处理链路。

## 9. Filter 能力

插件支持将 Spring AI Filter 转换为 VectorDB Filter 表达式，覆盖：

- AND、OR、NOT 逻辑组合；
- EQ、NE、GT、GTE、LT、LTE 比较；
- IN、NIN 集合判断；
- IS NULL、IS NOT NULL 空值判断；
- 嵌套条件组合。

安全规则：

- 字段名执行合法标识符校验；
- 字符串值按 VectorDB Filter 语法转义；
- 空集合、非法字段和不支持的操作数在远程调用前拒绝；
- Filter 能否高效执行取决于 Collection 是否建立相应标量索引。

Filter 可用于标准检索、原生 Query、全文检索、混合检索、Count 和 Delete。

## 10. 用户、授权与权限接口

### 10.1 用户管理

- `createUser`：创建数据库用户；
- `describeUser`：查询用户信息；
- `listUsers`：列出用户；
- `changePassword`：修改密码；
- `dropUser`：删除用户。

### 10.2 权限管理

- `grant`：向用户授予 Database/Collection 等资源权限；
- `revoke`：撤销已授予权限。

这些接口属于管理面能力，所需权限高于普通向量读写；生产环境应通过独立运维流程调用。

## 11. 配置可启用的能力

### 11.1 连接配置：`spring.ai.vectorstore.tencent-vectordb.client`

- `url`：VectorDB Endpoint；
- `username`：连接用户名，默认 `root`；
- `api-key`：访问密钥；
- `timeout-seconds`：请求超时；
- `connect-timeout-seconds`：连接超时；
- `max-idle-connections`：连接池空闲连接上限；
- `keep-alive-duration-seconds`：连接保活时间；
- `read-consistency`：`EVENTUAL_CONSISTENCY` 或 `STRONG_CONSISTENCY`。

### 11.2 Store 配置：`spring.ai.vectorstore.tencent-vectordb`

- `database-name`，默认 `spring_ai`；
- `collection-name`，默认 `vector_store`；
- `embedding-dimension`，默认 1536；
- `initialize-schema`；
- `shard-num`；
- `replica-num`；
- `description`；
- `index-type`，默认 HNSW；
- `metric-type`，默认 COSINE。

配置决定标准 VectorStore 的默认 Database/Collection 和 schema，不限制高级接口访问
其他已授权资源。

## 12. 响应、错误和一致性语义

- 原生接口保留 SDK 的 response code、message 和影响结果；
- VectorDB 业务 code 非成功时应视为失败，不能只依据 HTTP 状态判断；
- 插件异常包含操作名、Database、Collection 和底层原因；
- 写入、删除、索引构建、文件解析和统计可能存在异步完成窗口；
- `EVENTUAL_CONSISTENCY` 适合吞吐优先，`STRONG_CONSISTENCY` 适合读后写要求更强的场景；
- 调用方应对索引构建中、资源未就绪和服务端限流实施有界重试。

## 13. 实网验证状态

当前状态为“待测试”：

- 尚未取得可公开注册的腾讯云 VectorDB 账号；
- 没有可用的目标实例 Endpoint、用户名和 API Key；
- 未执行真实 Database、Collection、文档写入、检索、索引、AI 文件和权限验收；
- 本地源码、自动配置测试和 Maven 构建通过，不等于腾讯云服务端兼容性已经确认。

待获得账号后，建议至少验证：标准 VectorStore 全链路、Database/Collection/Alias、
Query/Count/Delete、向量/全文/混合检索、索引重建、Filter、读一致性、AI 文件流程及
用户授权接口。

## 14. 能力边界

- 标准 `VectorStore` 只代表外部 Embedding + 默认 Dense 向量这一条可替换业务路径；
- `searchByText`、全文检索、混合检索、托管文件解析和原子 Embedding 属于厂商扩展，
  不会自动改变标准接口行为；
- 高级接口返回 VectorDB 原生类型，不是与 VikingDB/DashVector 完全相同的统一高级模型；
- IndexType、MetricType、AI Database 和文件解析能力可能随 VectorDB 部署版本和套餐变化；
- Database、Collection、Alias、User 和权限操作具有管理面风险，应由授权运维流程调用；
- 在完成真实账号验收前，不能把本文档中的 SDK 能力清单当作腾讯云生产环境已验证结论。

