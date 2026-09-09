# DashVector 实网验收

## 执行边界

- 测试类：`DashVectorNativeLiveIT`、`DashVectorAdvancedNativeLiveIT`
- 凭据：仅从 `DASHVECTOR_ENDPOINT`、`DASHVECTOR_API_KEY` 进程环境变量读取，不写入源码、配置或报告。
- 隔离：每次执行创建带时间戳的唯一临时 Collection。
- 清理：`finally` 删除全部临时 Collection，并轮询到服务返回 Collection 不存在。
- 一致性：文档可见性、更新、删除和资源删除使用最长 60 秒的有界轮询。

## 2026-09-09 验收矩阵

| 能力 | 真实厂商边界证据 | 结果 |
|---|---|---|
| Spring AI 标准新增/更新 | `VectorStore.add` 写入真实 Collection | 通过 |
| 标准相似度查询 | 外部 EmbeddingModel + Dense Vector 查询 | 通过 |
| 元数据过滤与阈值 | Spring Filter 转换为 DashVector Filter 并返回目标文档 | 通过 |
| 标准按 ID 删除 | 删除后轮询 Fetch 不可见 | 通过 |
| 原生 Insert / Update / Fetch | SDK 请求经插件高级接口执行并校验字段更新 | 通过 |
| 按 Filter 批量删除 | 成功响应后轮询目标文档不可见 | 通过 |
| 高级检索参数 | `numCandidates`、`ef`、`radius`、`linear=false` | 通过 |
| 输出控制 | `includeVector`、`outputFields` | 通过 |
| 字段排序 | `OrderByField` | 通过 |
| 稠密 + 稀疏混合检索 | Reserved Dense Vector 与 Sparse Vector 联合请求 | 通过 |
| Group By | 单向量 Collection 按字段分组 | 通过 |
| Partition 生命周期 | Create / Describe / List / Stats / Delete | 通过 |
| 多向量 Collection | 两个命名 Dense Vector + 一个命名 Sparse Vector | 通过 |
| RRF 融合排序 | 三路召回使用 `RrfRanker` | 通过 |
| Weighted 融合排序 | 三路召回使用完整权重映射 | 通过 |
| 多向量单字段 Group By | 指定 `vectorField` 后分组检索 | 通过 |
| Collection 管理与统计 | Create / List / Describe / Stats / Delete | 通过 |

## 已观察到的厂商一致性行为

- 文档读写结果会在短时间内达到可见，测试以有界轮询判断最终状态。
- Partition Stats 在本次测试中返回了实际文档数量。
- Collection Stats API 调用成功且返回默认分区，但刚写入后的 `totalDocCount` 在 60 秒观察窗内仍可能为 `0`；因此验收 Collection Stats 的响应契约和结构，不把聚合计数当作实时写后读契约。

## 执行命令

```bash
DASHVECTOR_ENDPOINT=... DASHVECTOR_API_KEY=... \
  mvn -q -pl vector-stores/atlas-richie-ai-dashvector-store \
  -Dtest=DashVectorNativeLiveIT,DashVectorAdvancedNativeLiveIT test
```
