# DashVector 插件底层接口能力清单

本文档面向插件使用者和业务架构设计人员，说明 DashVector 插件能够提供的业务接口能力。
重点是“接口可以完成什么、需要什么输入、返回什么结果、适合哪些场景以及有哪些限制”，
不展开源码实现过程。DashVector 与 VikingDB 是两套独立的 Spring AI VectorStore 插件；
标准向量库能力可通过替换 Starter 和配置切换，高级能力使用 DashVector 扩展接口。

## 1. 能力范围总览

| 能力域 | 提供内容 | 典型接口 | 状态 |
|---|---|---|---|
| Spring AI 标准向量库 | 文档写入、按 ID 删除、条件删除、相似度检索 | `VectorStore` | 已实现 |
| Collection 管理 | 创建、列出、描述、统计、删除 | `DashVectorCollectionOperations` | 已实现 |
| 文档操作 | Insert、Upsert、Update、Fetch、Delete | `DashVectorDocumentOperations` | 已实现 |
| Partition 管理 | 创建、描述、列出、统计、删除 | `DashVectorPartitionOperations` | 已实现 |
| Dense 检索 | 单向量相似度检索及高级参数 | `DashVectorSearchOperations` | 已实现 |
| Sparse 检索 | 稀疏向量检索及高级参数 | `DashVectorSearchOperations` | 已实现 |
| 多向量检索 | 命名 Dense/Sparse 向量查询 | `DashVectorSearchOperations` | 已实现 |
| 融合排序 | RRF、Weighted | `DashVectorSearchOperations` | 已实现 |
| Group By | 分组召回、每组 TopK | `DashVectorSearchOperations` | 已实现 |
| Schema 能力 | Dense、Sparse、字段类型、多向量定义 | Collection/Document 请求 | 已实现 |
| Filter | 逻辑、比较、集合、空值过滤 | Spring AI Filter / SQL WHERE | 已实现 |

## 2. Spring AI 标准 VectorStore 能力

### 2.1 文档写入：`add`

输入 Spring AI `Document` 列表后，可完成：

- 为文本生成 Dense 向量并写入 DashVector；
- 保存 Document ID、正文内容和 metadata；
- 相同 ID 自动覆盖，统一支持新增和更新；
- 批量写入文档，适合知识库导入和增量同步；
- 在发送前检查向量维度和向量数量；
- 空列表直接视为成功的空操作。

字段映射约定：Document 文本保存为 `content`，metadata 保存为自定义字段，Embedding
结果保存为默认 Dense 向量。`content` 是保留字段，业务 metadata 不应覆盖它。

### 2.2 按 ID 删除：`delete(List<String>)`

- 删除一个或多个文档 ID；
- 大列表自动按服务端批量限制拆分；
- 空列表不产生远程调用；
- 适合用户删除、文档替换和测试资源回收。

返回成功表示删除请求已被服务端接受，读模型刷新可能存在短暂延迟。

### 2.3 按条件删除：`delete(Filter.Expression)`

- 以 Spring AI Filter 表达式描述删除条件；
- 转换为 DashVector 的过滤删除请求；
- 支持按租户、类型、状态、标签等批量清理；
- 适合数据生命周期管理和过期数据清理；
- 过滤删除的结果以服务端返回状态为准。

## 3. Collection 接口能力

### 3.1 创建：`createCollection`

可以定义一个 Collection 的完整数据结构：

- 单 Dense 向量及其维度、数据类型和距离度量；
- 多个命名 Dense 向量，每个向量拥有独立维度和度量；
- 保留 Sparse 向量或命名 Sparse 向量；
- 文档字段及字段类型；
- extra parameters 和索引参数；
- 创建等待超时。

适用场景：创建标准知识库、图文库、多模态库或需要多路召回的高级 Collection。

### 3.2 查询：`listCollections`、`describeCollection`

- `listCollections`：获得当前账号可见的 Collection 名称；
- `describeCollection`：获得状态、向量 Schema、字段 Schema、维度、度量和其他元数据。

适合控制台展示、启动前检查、租户资源盘点和 schema 兼容性验证。

### 3.3 统计：`collectionStats`

- 查询绑定 Collection 的文档统计；
- 也可以按 Collection 名查询任意可访问 Collection；
- 用于容量监控、导入进度和验收检查。

统计数据由服务端异步刷新，刚写入后不保证立即反映最新文档数量。

### 3.4 删除：`deleteCollection`

- 删除指定 Collection 及其中的数据；
- 适合临时环境销毁、租户注销和测试清理；
- 属于破坏性操作，必须由调用方显式触发。

## 4. 文档数据接口

所有文档操作都支持“使用默认 Collection”和“显式指定 Collection”两种调用方式，
因此同一个客户端可以服务多个独立 Collection。

### 4.1 `insert`

- 只接受不存在的文档 ID；
- ID 已存在时返回对应的 provider 操作结果；
- 可同时写入字段、默认 Dense、命名 Dense、默认 Sparse、命名 Sparse 和 Partition；
- 适合严格区分“首次创建”和“重复写入”的业务。

### 4.2 `upsert`

- ID 不存在时插入；
- ID 已存在时覆盖或更新文档；
- 支持完整向量和字段内容；
- 适合知识库同步、幂等消费和数据重放。

### 4.3 `update`

- 更新已存在文档；
- 可只变更字段或向量的一部分；
- 适合修改标签、标题、状态和业务属性，而不必重新构造整个文档。

### 4.4 `fetch`

- 根据一个或多个 ID 读取文档；
- 可指定 Partition；
- 返回 fields、默认/命名 Dense、默认/命名 Sparse 和 provider score；
- 适合详情页、数据校验、迁移和回源前检查；
- 不存在的 ID 不会被伪造成空文档。

### 4.5 `delete`

支持三类删除条件：

- 按 ID 删除；
- 按 Partition 限定删除；
- 按 Filter 批量删除。

每个文档的操作类型、ID、provider code 和 message 都会保留，适合逐条重试和审计。

## 5. Partition 接口能力

### 5.1 生命周期

提供以下能力：

- `createPartition`：创建分区；
- `describePartition`：查询分区状态；
- `listPartitions`：列出 Collection 下的分区；
- `partitionStats`：查询分区文档统计；
- `deletePartition`：删除分区。

### 5.2 状态与使用方式

可识别 `INITIALIZED`、`SERVING`、`DROPPING`、`ERROR` 等状态。

Partition 可用于：

- 租户隔离；
- 按时间分区；
- 灰度或版本数据隔离；
- 将查询和写入限定在指定数据子集。

创建和删除属于异步资源操作，调用方应等待目标状态并使用有界重试。

## 6. Dense、Sparse 和多向量检索

### 6.1 Dense 查询：`query`

输入一个 Dense 向量，可控制：

- `topk`：返回数量上限；
- `partition`：限定查询分区；
- `filter`：SQL WHERE 过滤条件；
- `includeVector`：是否在结果中返回向量；
- `outputFields`：返回字段白名单；
- `numCandidates`：候选集大小；
- `ef`：HNSW 探索参数；
- `linear`：是否使用线性检索；
- `radius`：距离阈值。

适合常规语义搜索、RAG 召回和相似内容推荐。

### 6.2 Sparse 查询

- 输入词项 ID 到权重的 Sparse 向量；
- 支持与 Dense 查询相同的 topK、过滤、Partition、字段和候选参数；
- 适合关键词、稀有词、错误码和精确术语检索；
- 可以单独检索，也可以与 Dense 结果融合。

### 6.3 命名 Dense/Sparse 查询

多向量 Collection 中，每个向量字段有独立名称：

- 按名称查询指定 Dense 向量；
- 按名称查询指定 Sparse 向量；
- 每一路可以有独立的候选参数；
- 可以将多路结果交给 RRF 或 Weighted 排序；
- 不同字段的维度、度量和语义可以分别管理。

适合标题向量、正文向量、图片向量、代码向量等多路召回场景。

## 7. 融合排序能力

### 7.1 RRF（Reciprocal Rank Fusion）

- 将多路 Dense/Sparse 结果按名次融合；
- 可设置 rank constant；
- 适合不同向量模型、不同字段或 Dense+Sparse 组合召回；
- 对分数尺度差异较大的多路结果更稳定。

### 7.2 Weighted

- 为每个命名向量设置权重；
- 按权重合并各路结果；
- 权重键必须与参与查询的向量字段对应；
- 适合明确表达“标题比正文重要”或“图片比文本重要”等业务策略。

高级查询响应保留 provider code、message、requestId、usage、原始 score 和完整 Doc，
方便业务自行判断融合结果。标准 Spring AI 查询才会将 provider score 转为常用的相似度分数。

## 8. Group By 分组检索

`queryGroupBy` 可以在召回后按字段分组：

- `groupByField`：分组字段；
- `groupCount`：最多返回多少组；
- `groupTopk`：每组最多返回多少文档；
- `vectorField`：多向量 Collection 中指定参与分组检索的向量字段；
- 支持 Dense 或 Sparse 查询；
- 支持 Partition、Filter、outputFields、includeVector；
- 支持按 ID 组织分组结果。

适用场景：每个品牌取若干结果、每个文档只保留多个切片、按租户或类别限制结果数量。

返回结果是分组列表，每组包含组标识和该组文档，不会扁平化为普通相似度列表。

## 9. Schema 与文档类型能力

### 9.1 向量 Schema

支持：

- 单 Dense 向量；
- 多命名 Dense 向量；
- 默认 Sparse 向量；
- 多命名 Sparse 向量；
- 每个向量的维度、数据类型和 Metric；
- extra parameters 和索引定义。

### 9.2 字段类型

可定义 BOOL、STRING、INT、FLOAT、LONG 及其数组类型，用于存储 metadata、标签、
租户号、时间、状态和业务属性。

### 9.3 文档内容

文档可以同时包含：

- ID；
- fields；
- 默认 Dense；
- 命名 Dense；
- 默认 Sparse；
- 命名 Sparse；
- provider score。

## 10. Filter 能力

过滤条件可用于查询、Group By 和批量删除，支持：

- 逻辑：AND、OR、NOT；
- 比较：EQ、NE、GT、GTE、LT、LTE；
- 集合：IN、NIN；
- 空值：IS NULL、IS NOT NULL；
- 任意嵌套组合。

安全和输入规则：

- 字段名只能使用合法标识符；
- 字符串使用单引号并转义内部单引号；
- IN/NIN 必须提供非空集合；
- 操作数类型必须与字段类型匹配；
- 非法条件在远程请求前就会被拒绝。

## 11. 配置可启用的能力

### 11.1 连接参数

- Endpoint；
- API Key；
- 请求超时时间。

Endpoint 支持带或不带 HTTP(S) scheme 的根地址；不能包含非根路径、query 或 fragment。

### 11.2 标准 Store 参数

- 默认 Collection；
- Embedding 维度；
- schema 初始化等待时间；
- 默认 Partition；
- 默认 Metric；
- metadata 字段及字段类型；
- 是否启用标准 schema 初始化。

这些参数确定标准 `VectorStore` 的默认目标和默认行为；显式高级请求仍可访问其他
已授权 Collection。

## 12. 响应、错误和一致性能力

### 12.1 响应信息

高级接口响应保留：

- provider code；
- provider message；
- requestId；
- usage；
- 原始 score；
- 完整文档字段和向量（按请求选择）。

这样可以在业务层实现精细的重试、审计、指标和降级策略。

### 12.2 错误处理

服务端业务 code 失败、请求未成功、资源未初始化和网络错误都会被明确报告；不能只依据
HTTP 状态判断成功。

### 12.3 异步一致性

Collection、Partition、文档写入、删除和统计存在服务端异步刷新窗口。调用成功表示操作
被接受，不保证下一次读取立即看到最终状态。生产编排和测试应使用有界等待与状态轮询。

## 13. 实网验证状态

DashVector 高级接口已在真实集群验证以下能力：

- 标准 add/search/filter/threshold/delete；
- Insert、Update、Fetch；
- Filter 批量删除；
- 候选数、ef、radius、linear、includeVector、outputFields、字段排序；
- Dense + Sparse 混合检索；
- Group By；
- Partition 创建、查询、统计、删除；
- 多 Dense/命名 Sparse Collection；
- RRF、Weighted；
- 多向量单字段 Group By；
- Collection List/Describe/Stats/Delete；
- 临时资源清理。

详细验收证据见 [DashVector 实网验收记录](../../docs/testing/dashvector-live-acceptance.md)。

## 14. 能力边界

- 标准 `VectorStore` 使用外部 EmbeddingModel 和默认 Dense 向量，不会自动推断业务应使用
  哪个命名向量或 Sparse 字段；
- 高级接口使用 DashVector 原生 request/response，不是跨厂商统一的高级抽象；
- 标准查询不会自动启用 RRF、Weighted、Group By 或 Rerank；
- Collection 统计不是严格实时计数；
- 多向量 Collection 需要显式定义 Schema，标准 schema 初始化只覆盖标准单向量场景；
- API Key 和 Endpoint 只能由运行时配置提供，不应写入源码、测试夹具或能力文档。

