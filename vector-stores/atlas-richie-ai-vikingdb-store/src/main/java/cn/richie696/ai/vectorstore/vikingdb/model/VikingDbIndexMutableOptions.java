package cn.richie696.ai.vectorstore.vikingdb.model;

import java.util.List;

/**
 * Fields accepted by the current UpdateVikingdbIndexRequest.
 */
public final class VikingDbIndexMutableOptions {
    private final Integer cpuQuota;
    private final Boolean deletionProtection;
    private final String description;
    private final List<String> scalarIndexes;
    private final Integer shardCount;
    private final String shardPolicy;

    private VikingDbIndexMutableOptions(Builder b) {
        this.cpuQuota = b.cpuQuota;
        this.deletionProtection = b.deletionProtection;
        this.description = b.description;
        this.scalarIndexes = b.scalarIndexes == null ? null : List.copyOf(b.scalarIndexes);
        this.shardCount = b.shardCount;
        this.shardPolicy = b.shardPolicy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integer cpuQuota() {
        return cpuQuota;
    }

    public Boolean deletionProtection() {
        return deletionProtection;
    }

    public String description() {
        return description;
    }

    public List<String> scalarIndexes() {
        return scalarIndexes;
    }

    public Integer shardCount() {
        return shardCount;
    }

    public String shardPolicy() {
        return shardPolicy;
    }

    public static final class Builder {
        private Integer cpuQuota;
        private Boolean deletionProtection;
        private String description;
        private List<String> scalarIndexes;
        private Integer shardCount;
        private String shardPolicy;

        public Builder cpuQuota(Integer value) {
            this.cpuQuota = value;
            return this;
        }

        public Builder deletionProtection(Boolean value) {
            this.deletionProtection = value;
            return this;
        }

        public Builder description(String value) {
            this.description = value;
            return this;
        }

        public Builder scalarIndexes(List<String> value) {
            this.scalarIndexes = value;
            return this;
        }

        public Builder shardCount(Integer value) {
            this.shardCount = value;
            return this;
        }

        public Builder shardPolicy(String value) {
            this.shardPolicy = value;
            return this;
        }

        public VikingDbIndexMutableOptions build() {
            return new VikingDbIndexMutableOptions(this);
        }
    }
}
