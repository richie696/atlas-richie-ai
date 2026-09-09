package cn.richie696.ai.vectorstore.tencentvectordb;

import com.tencent.tcvectordb.model.param.collection.IndexType;
import com.tencent.tcvectordb.model.param.collection.MetricType;

/** Immutable binding for one Tencent VectorDB database and collection. */
public record TencentVectorDbStoreSpec(String databaseName, String collectionName,
                                       int embeddingDimension, boolean initializeSchema,
                                       int shardNum, int replicaNum, String description,
                                       IndexType indexType, MetricType metricType) {
    public static Builder builder() { return new Builder(); }
    public static final class Builder {
        private String databaseName = TencentVectorDbVectorStore.DEFAULT_DATABASE_NAME;
        private String collectionName = TencentVectorDbVectorStore.DEFAULT_COLLECTION_NAME;
        private int embeddingDimension = TencentVectorDbVectorStore.DEFAULT_EMBEDDING_DIMENSION;
        private boolean initializeSchema;
        private int shardNum = 1;
        private int replicaNum = 2;
        private String description;
        private IndexType indexType = IndexType.HNSW;
        private MetricType metricType = MetricType.COSINE;
        public Builder databaseName(String value) { databaseName = value; return this; }
        public Builder collectionName(String value) { collectionName = value; return this; }
        public Builder embeddingDimension(int value) { embeddingDimension = value; return this; }
        public Builder initializeSchema(boolean value) { initializeSchema = value; return this; }
        public Builder shardNum(int value) { shardNum = value; return this; }
        public Builder replicaNum(int value) { replicaNum = value; return this; }
        public Builder description(String value) { description = value; return this; }
        public Builder indexType(IndexType value) { indexType = value; return this; }
        public Builder metricType(MetricType value) { metricType = value; return this; }
        public TencentVectorDbStoreSpec build() { return new TencentVectorDbStoreSpec(databaseName, collectionName,
                embeddingDimension, initializeSchema, shardNum, replicaNum, description, indexType, metricType); }
    }
}
