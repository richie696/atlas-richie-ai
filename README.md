# Atlas Richie AI Vector Stores

Spring AI 2.0.0 vector-store plugins for Volcano Engine VikingDB, Alibaba Cloud
DashVector, and Tencent Cloud VectorDB. Each provider is an independent
`vector-store` + `autoconfigure` + `starter` suite.

Application code depends only on Spring AI's `VectorStore` contract. To switch a
provider, replace the starter dependency and its configuration; application services
that use `VectorStore` do not change.

## Standard Spring AI contract

All three plugins implement the same standard operations:

- add/upsert Spring AI `Document` objects using the application's `EmbeddingModel`;
- delete by document ID or Spring AI filter expression;
- similarity search with `topK`, similarity threshold, and metadata filter;
- Micrometer/Spring AI vector-store observations;
- optional schema initialization followed by schema compatibility validation.

```java
@Service
class KnowledgeService {
    private final VectorStore vectorStore;

    KnowledgeService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }
}
```

Only one provider starter should normally be present in an application runtime.

## DashVector

```xml
<dependency>
    <groupId>cn.richie696.ai</groupId>
    <artifactId>atlas-richie-ai-starter-vector-store-dashvector</artifactId>
</dependency>
```

```yaml
spring:
  ai:
    vectorstore:
      type: dashvector
      dashvector:
        collection-name: knowledge
        embedding-dimension: 1536
        partition: default
        metric: cosine
        initialize-schema: false
        schema-initialization-timeout-seconds: 120
        metadata-fields:
          tenant: STRING
          published: BOOL
        client:
          endpoint: ${DASHVECTOR_ENDPOINT} # host or https:// root URL
          api-key: ${DASHVECTOR_API_KEY}
          timeout-seconds: 10
```

Provider-specific interfaces expose collection lifecycle, native document
upsert/update/delete/fetch, partitions and statistics, and dense/sparse/multi-vector
query, fusion rankers, and group-by through the official SDK request types:
`DashVectorCollectionOperations`, `DashVectorDocumentOperations`,
`DashVectorSearchOperations`, and `DashVectorPartitionOperations`.

## Tencent Cloud VectorDB

```xml
<dependency>
    <groupId>cn.richie696.ai</groupId>
    <artifactId>atlas-richie-ai-starter-vector-store-tencent-vectordb</artifactId>
</dependency>
```

```yaml
spring:
  ai:
    vectorstore:
      type: tencent-vectordb
      tencent-vectordb:
        database-name: spring_ai
        collection-name: knowledge
        embedding-dimension: 1536
        shard-num: 1
        replica-num: 2 # use 0 where the selected instance tier requires it
        index-type: HNSW
        metric-type: COSINE
        initialize-schema: false
        client:
          url: ${TENCENT_VECTORDB_URL}
          username: root
          api-key: ${TENCENT_VECTORDB_API_KEY}
          read-consistency: EVENTUAL_CONSISTENCY
          timeout-seconds: 10
          connect-timeout-seconds: 10
          max-idle-connections: 10
          keep-alive-duration-seconds: 300
```

The plugin intentionally uses the official HTTP SDK client because it exposes the
complete provider surface. Provider-specific interfaces cover database and collection
lifecycle, aliases, native documents, vector/ID/managed-embedding/full-text/hybrid
search, index lifecycle, users and grants, AI databases, file ingestion, image URL and
file-detail queries, and atomic embedding:
`TencentVectorDbDatabaseOperations`, `TencentVectorDbCollectionOperations`,
`TencentVectorDbDocumentOperations`, `TencentVectorDbSearchOperations`,
`TencentVectorDbIndexOperations`, `TencentVectorDbPermissionOperations`, and
`TencentVectorDbAiOperations`.

Real-cloud acceptance is currently **pending** because no registrable Tencent Cloud
VectorDB test account or credentials are available. Local tests and compilation do not
constitute provider acceptance. See [Tencent VectorDB live acceptance status](docs/testing/tencent-vectordb-live-acceptance.md).

## VikingDB

```xml
<dependency>
    <groupId>cn.richie696.ai</groupId>
    <artifactId>atlas-richie-ai-starter-vector-store-vikingdb</artifactId>
</dependency>
```

```yaml
spring:
  ai:
    vectorstore:
      type: vikingdb
      vikingdb:
        collection-name: knowledge
        index-name: knowledge_hnsw
        embedding-dimension: 1536
        initialize-schema: false
        metadata-fields:
          tenant: STRING
          published: BOOL
        scalar-index: [ tenant, published ]
        client:
          host: api-vikingdb.vikingdb.cn-beijing.volces.com
          control-endpoint: vikingdb.cn-beijing.volcengineapi.com
          region: cn-beijing
          access-key: ${VIKINGDB_ACCESS_KEY}
          secret-key: ${VIKINGDB_SECRET_KEY}
```

VikingDB filter deletion submits an asynchronous `filter_delete` task. Applications
requiring immediate visibility must monitor the returned task at the application
boundary.

## Multiple stores of the same provider

The auto-configuration creates the default singleton `VectorStore`. A provider factory
is also registered for applications that need additional database/collection bindings:
`DashVectorVectorStoreFactory`, `TencentVectorDbVectorStoreFactory`, or
`VikingDbVectorStoreFactory`. Each factory reuses the singleton SDK client,
`EmbeddingModel`, batching strategy, and observation infrastructure.

## Development verification

```bash
mvn test
```

DashVector real-cluster lifecycle acceptance is opt-in and reads credentials only from
the process environment:

```bash
DASHVECTOR_ENDPOINT=... DASHVECTOR_API_KEY=... \
  mvn -pl vector-stores/atlas-richie-ai-dashvector-store \
  -Dtest=DashVectorNativeLiveIT,DashVectorAdvancedNativeLiveIT test
```

Acceptance scope and latest evidence: [DashVector live acceptance](docs/testing/dashvector-live-acceptance.md).
