# Atlas Richie AI — VikingDB Vector Store

Spring AI 2.0.0 adapter for Volcano Engine VikingDB, using an application supplied
`EmbeddingModel` (external-embedding mode).

Maven coordinates follow the Spring AI module model:

- `com.richie.ai:atlas-richie-ai-vikingdb-store`
- `com.richie.ai:atlas-richie-ai-autoconfigure-vector-store-vikingdb`
- `com.richie.ai:atlas-richie-ai-starter-vector-store-vikingdb`
- `com.richie.ai:atlas-richie-ai-bom`

## Contract

The adapter stores `doc_id`, `content`, and `embedding`. Metadata is schema-first:
declare every metadata key in `metadata-fields`; documents containing undeclared keys
are rejected. Filterable keys must also be listed in `scalar-index`.

```yaml
spring:
  ai:
    vectorstore:
      type: vikingdb
      vikingdb:
        collection-name: knowledge
        index-name: knowledge_hnsw
        embedding-dimension: 1536
        initialize-schema: false # production: provision separately, then validate
        metadata-fields:
          tenant: STRING
          published: BOOL
          created_at: INT64
        scalar-index: [ tenant, published ]
        client:
          host: api-vikingdb.vikingdb.cn-beijing.volces.com
          control-endpoint: vikingdb.cn-beijing.volcengineapi.com
          region: cn-beijing
          access-key: ${VIKINGDB_ACCESS_KEY}
          secret-key: ${VIKINGDB_SECRET_KEY}
```

`delete(Filter.Expression)` creates VikingDB's server-side `filter_delete` task with
`needConfirm=false`. The call submits the asynchronous task; it does not imply task
completion or immediate index visibility. For application-level monitoring, use the
adapter extension `deleteByFilter(expression)`, which returns the VikingDB `taskId`.
VikingDB's index updates are asynchronous, so applications needing read-after-write
semantics must wait/poll at the application boundary.

## Development verification

```bash
mvn test
```
