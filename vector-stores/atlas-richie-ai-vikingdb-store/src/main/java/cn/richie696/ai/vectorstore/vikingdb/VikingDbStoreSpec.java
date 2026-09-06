package cn.richie696.ai.vectorstore.vikingdb;

import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbFilterValidationMode;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbIndexVectorOptions;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbSearchAdvanceOptions;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbSearchCommonOptions;
import com.volcengine.vikingdb.model.FieldForCreateVikingdbCollectionInput;

import java.util.List;
import java.util.Map;

/**
 * Immutable specification used to create one independently bound Store.
 */
public final class VikingDbStoreSpec {
    private final String collectionName;
    private final String indexName;
    private final int embeddingDimension;
    private final boolean initializeSchema;
    private final String projectName;
    private final String description;
    private final Integer shardCount;
    private final List<String> scalarIndex;
    private final Map<String, FieldForCreateVikingdbCollectionInput.FieldTypeEnum> metadataFields;
    private final VikingDbFilterValidationMode filterValidationMode;
    private final VikingDbSearchCommonOptions searchDefaults;
    private final VikingDbSearchAdvanceOptions searchAdvanceDefaults;
    private final VikingDbIndexVectorOptions indexVectorOptions;

    private VikingDbStoreSpec(Builder b) {
        this.collectionName = b.collectionName;
        this.indexName = b.indexName;
        this.embeddingDimension = b.embeddingDimension;
        this.initializeSchema = b.initializeSchema;
        this.projectName = b.projectName;
        this.description = b.description;
        this.shardCount = b.shardCount;
        this.scalarIndex = b.scalarIndex == null ? List.of() : List.copyOf(b.scalarIndex);
        this.metadataFields = b.metadataFields == null ? Map.of() : Map.copyOf(b.metadataFields);
        this.filterValidationMode = b.filterValidationMode;
        this.searchDefaults = b.searchDefaults;
        this.searchAdvanceDefaults = b.searchAdvanceDefaults;
        this.indexVectorOptions = b.indexVectorOptions;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String collectionName() {
        return collectionName;
    }

    public String indexName() {
        return indexName;
    }

    public int embeddingDimension() {
        return embeddingDimension;
    }

    public boolean initializeSchema() {
        return initializeSchema;
    }

    public String projectName() {
        return projectName;
    }

    public String description() {
        return description;
    }

    public Integer shardCount() {
        return shardCount;
    }

    public List<String> scalarIndex() {
        return scalarIndex;
    }

    public Map<String, FieldForCreateVikingdbCollectionInput.FieldTypeEnum> metadataFields() {
        return metadataFields;
    }

    public VikingDbFilterValidationMode filterValidationMode() {
        return filterValidationMode;
    }

    public VikingDbSearchCommonOptions searchDefaults() {
        return searchDefaults;
    }

    public VikingDbSearchAdvanceOptions searchAdvanceDefaults() {
        return searchAdvanceDefaults;
    }

    public VikingDbIndexVectorOptions indexVectorOptions() {
        return indexVectorOptions;
    }

    public static final class Builder {
        private String collectionName = VikingDbVectorStore.DEFAULT_COLLECTION_NAME;
        private String indexName = VikingDbVectorStore.DEFAULT_COLLECTION_NAME;
        private int embeddingDimension = VikingDbVectorStore.OPENAI_EMBEDDING_DIMENSION_SIZE;
        private boolean initializeSchema;
        private String projectName;
        private String description;
        private Integer shardCount;
        private List<String> scalarIndex;
        private Map<String, FieldForCreateVikingdbCollectionInput.FieldTypeEnum> metadataFields;
        private VikingDbFilterValidationMode filterValidationMode = VikingDbFilterValidationMode.DECLARED_FIELDS;
        private VikingDbSearchCommonOptions searchDefaults = VikingDbSearchCommonOptions.empty();
        private VikingDbSearchAdvanceOptions searchAdvanceDefaults = VikingDbSearchAdvanceOptions.empty();
        private VikingDbIndexVectorOptions indexVectorOptions = VikingDbIndexVectorOptions.defaults();

        public Builder collectionName(String value) {
            this.collectionName = value;
            return this;
        }

        public Builder indexName(String value) {
            this.indexName = value;
            return this;
        }

        public Builder embeddingDimension(int value) {
            this.embeddingDimension = value;
            return this;
        }

        public Builder initializeSchema(boolean value) {
            this.initializeSchema = value;
            return this;
        }

        public Builder projectName(String value) {
            this.projectName = value;
            return this;
        }

        public Builder description(String value) {
            this.description = value;
            return this;
        }

        public Builder shardCount(Integer value) {
            this.shardCount = value;
            return this;
        }

        public Builder scalarIndex(List<String> value) {
            this.scalarIndex = value;
            return this;
        }

        public Builder metadataFields(Map<String, FieldForCreateVikingdbCollectionInput.FieldTypeEnum> value) {
            this.metadataFields = value;
            return this;
        }

        public Builder filterValidationMode(VikingDbFilterValidationMode value) {
            this.filterValidationMode = value;
            return this;
        }

        public Builder searchDefaults(VikingDbSearchCommonOptions value) {
            this.searchDefaults = value;
            return this;
        }

        public Builder searchAdvanceDefaults(VikingDbSearchAdvanceOptions value) {
            this.searchAdvanceDefaults = value;
            return this;
        }

        public Builder indexVectorOptions(VikingDbIndexVectorOptions value) {
            this.indexVectorOptions = value;
            return this;
        }

        public VikingDbStoreSpec build() {
            return new VikingDbStoreSpec(this);
        }
    }
}
