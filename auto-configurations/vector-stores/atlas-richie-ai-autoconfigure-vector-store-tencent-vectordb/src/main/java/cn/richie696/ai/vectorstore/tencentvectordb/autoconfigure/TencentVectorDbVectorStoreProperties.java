package cn.richie696.ai.vectorstore.tencentvectordb.autoconfigure;

import com.tencent.tcvectordb.model.param.collection.IndexType;
import com.tencent.tcvectordb.model.param.collection.MetricType;
import org.springframework.ai.vectorstore.properties.CommonVectorStoreProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Tencent VectorDB database, collection, and index properties. */
@ConfigurationProperties(TencentVectorDbVectorStoreProperties.CONFIG_PREFIX)
public class TencentVectorDbVectorStoreProperties extends CommonVectorStoreProperties {
    public static final String CONFIG_PREFIX = "spring.ai.vectorstore.tencent-vectordb";

    private String databaseName = "spring_ai";
    private String collectionName = "vector_store";
    private int embeddingDimension = 1536;
    private int shardNum = 1;
    private int replicaNum = 2;
    private String description;
    private IndexType indexType = IndexType.HNSW;
    private MetricType metricType = MetricType.COSINE;

    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    public int getEmbeddingDimension() { return embeddingDimension; }
    public void setEmbeddingDimension(int embeddingDimension) { this.embeddingDimension = embeddingDimension; }
    public int getShardNum() { return shardNum; }
    public void setShardNum(int shardNum) { this.shardNum = shardNum; }
    public int getReplicaNum() { return replicaNum; }
    public void setReplicaNum(int replicaNum) { this.replicaNum = replicaNum; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public IndexType getIndexType() { return indexType; }
    public void setIndexType(IndexType indexType) { this.indexType = indexType; }
    public MetricType getMetricType() { return metricType; }
    public void setMetricType(MetricType metricType) { this.metricType = metricType; }
}
