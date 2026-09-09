package cn.richie696.ai.vectorstore.dashvector;

import com.aliyun.dashvector.proto.CollectionInfo;
import com.aliyun.dashvector.proto.FieldType;

import java.util.Map;

/** Immutable binding for one DashVector collection-backed store. */
public record DashVectorStoreSpec(String collectionName, int embeddingDimension,
                                  boolean initializeSchema, int schemaInitializationTimeoutSeconds,
                                  String partition,
                                  CollectionInfo.Metric metric,
                                  Map<String, FieldType> metadataFields) {
    public DashVectorStoreSpec {
        metadataFields = metadataFields == null ? Map.of() : Map.copyOf(metadataFields);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String collectionName = DashVectorVectorStore.DEFAULT_COLLECTION_NAME;
        private int embeddingDimension = DashVectorVectorStore.DEFAULT_EMBEDDING_DIMENSION;
        private boolean initializeSchema;
        private int schemaInitializationTimeoutSeconds = 120;
        private String partition = "default";
        private CollectionInfo.Metric metric = CollectionInfo.Metric.cosine;
        private Map<String, FieldType> metadataFields = Map.of();

        public Builder collectionName(String value) { collectionName = value; return this; }
        public Builder embeddingDimension(int value) { embeddingDimension = value; return this; }
        public Builder initializeSchema(boolean value) { initializeSchema = value; return this; }
        public Builder schemaInitializationTimeoutSeconds(int value) { schemaInitializationTimeoutSeconds = value; return this; }
        public Builder partition(String value) { partition = value; return this; }
        public Builder metric(CollectionInfo.Metric value) { metric = value; return this; }
        public Builder metadataFields(Map<String, FieldType> value) { metadataFields = value; return this; }
        public DashVectorStoreSpec build() {
            return new DashVectorStoreSpec(collectionName, embeddingDimension, initializeSchema,
                    schemaInitializationTimeoutSeconds, partition, metric, metadataFields);
        }
    }
}
