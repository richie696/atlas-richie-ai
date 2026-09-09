package cn.richie696.ai.vectorstore.dashvector.autoconfigure;

import com.aliyun.dashvector.proto.CollectionInfo;
import com.aliyun.dashvector.proto.FieldType;
import org.springframework.ai.vectorstore.properties.CommonVectorStoreProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/** DashVector collection binding and schema properties. */
@ConfigurationProperties(DashVectorVectorStoreProperties.CONFIG_PREFIX)
public class DashVectorVectorStoreProperties extends CommonVectorStoreProperties {
    public static final String CONFIG_PREFIX = "spring.ai.vectorstore.dashvector";
    private String collectionName = "vector_store";
    private int embeddingDimension = 1536;
    private int schemaInitializationTimeoutSeconds = 120;
    private String partition = "default";
    private CollectionInfo.Metric metric = CollectionInfo.Metric.cosine;
    private Map<String, FieldType> metadataFields = Map.of();

    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    public int getEmbeddingDimension() { return embeddingDimension; }
    public void setEmbeddingDimension(int embeddingDimension) { this.embeddingDimension = embeddingDimension; }
    public int getSchemaInitializationTimeoutSeconds() { return schemaInitializationTimeoutSeconds; }
    public void setSchemaInitializationTimeoutSeconds(int schemaInitializationTimeoutSeconds) {
        this.schemaInitializationTimeoutSeconds = schemaInitializationTimeoutSeconds;
    }
    public String getPartition() { return partition; }
    public void setPartition(String partition) { this.partition = partition; }
    public CollectionInfo.Metric getMetric() { return metric; }
    public void setMetric(CollectionInfo.Metric metric) { this.metric = metric; }
    public Map<String, FieldType> getMetadataFields() { return metadataFields; }
    public void setMetadataFields(Map<String, FieldType> metadataFields) { this.metadataFields = metadataFields; }
}
