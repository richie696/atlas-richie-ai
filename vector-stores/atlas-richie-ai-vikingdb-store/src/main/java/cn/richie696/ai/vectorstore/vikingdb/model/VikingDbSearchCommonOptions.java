package cn.richie696.ai.vectorstore.vikingdb.model;

import java.util.List;

/**
 * Common options shared by all VikingDB search modes. Null means provider default.
 */
public final class VikingDbSearchCommonOptions {
    private final Integer limit;
    private final Integer offset;
    private final String partition;
    private final List<String> outputFields;
    private final Boolean returnSchema;
    private final Boolean returnDownloadUrl;
    private final Boolean returnAnalyzedResult;
    private final Boolean returnDetailInfo;

    private VikingDbSearchCommonOptions(Builder builder) {
        this.limit = builder.limit;
        this.offset = builder.offset;
        this.partition = builder.partition;
        this.outputFields = builder.outputFields == null ? null : List.copyOf(builder.outputFields);
        this.returnSchema = builder.returnSchema;
        this.returnDownloadUrl = builder.returnDownloadUrl;
        this.returnAnalyzedResult = builder.returnAnalyzedResult;
        this.returnDetailInfo = builder.returnDetailInfo;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static VikingDbSearchCommonOptions empty() {
        return builder().build();
    }

    public Integer limit() {
        return limit;
    }

    public Integer offset() {
        return offset;
    }

    public String partition() {
        return partition;
    }

    public List<String> outputFields() {
        return outputFields;
    }

    public Boolean returnSchema() {
        return returnSchema;
    }

    public Boolean returnDownloadUrl() {
        return returnDownloadUrl;
    }

    public Boolean returnAnalyzedResult() {
        return returnAnalyzedResult;
    }

    public Boolean returnDetailInfo() {
        return returnDetailInfo;
    }

    public static final class Builder {
        private Integer limit;
        private Integer offset;
        private String partition;
        private List<String> outputFields;
        private Boolean returnSchema;
        private Boolean returnDownloadUrl;
        private Boolean returnAnalyzedResult;
        private Boolean returnDetailInfo;

        public Builder limit(Integer value) {
            this.limit = value;
            return this;
        }

        public Builder offset(Integer value) {
            this.offset = value;
            return this;
        }

        public Builder partition(String value) {
            this.partition = value;
            return this;
        }

        public Builder outputFields(List<String> value) {
            this.outputFields = value;
            return this;
        }

        public Builder returnSchema(Boolean value) {
            this.returnSchema = value;
            return this;
        }

        public Builder returnDownloadUrl(Boolean value) {
            this.returnDownloadUrl = value;
            return this;
        }

        public Builder returnAnalyzedResult(Boolean value) {
            this.returnAnalyzedResult = value;
            return this;
        }

        public Builder returnDetailInfo(Boolean value) {
            this.returnDetailInfo = value;
            return this;
        }

        public VikingDbSearchCommonOptions build() {
            return new VikingDbSearchCommonOptions(this);
        }
    }
}
