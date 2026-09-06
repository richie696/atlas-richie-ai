package cn.richie696.ai.vectorstore.vikingdb.model;

import java.util.List;

/**
 * Immutable complete index create definition.
 */
public final class VikingDbIndexDefinition {
    private final VikingDbResourceRef target;
    private final VikingDbIndexVectorOptions vector;
    private final List<String> scalarIndexes;
    private final Integer shardCount;
    private final String shardPolicy;
    private final Integer cpuQuota;
    private final String resourceId;
    private final Boolean deletionProtection;
    private final String description;

    private VikingDbIndexDefinition(Builder b) {
        this.target = b.target;
        this.vector = b.vector == null ? VikingDbIndexVectorOptions.defaults() : b.vector;
        this.scalarIndexes = b.scalarIndexes == null ? List.of() : List.copyOf(b.scalarIndexes);
        this.shardCount = b.shardCount;
        this.shardPolicy = b.shardPolicy;
        this.cpuQuota = b.cpuQuota;
        this.resourceId = b.resourceId;
        this.deletionProtection = b.deletionProtection;
        this.description = b.description;
    }

    public static Builder builder() {
        return new Builder();
    }

    public VikingDbResourceRef target() {
        return target;
    }

    public VikingDbIndexVectorOptions vector() {
        return vector;
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

    public Integer cpuQuota() {
        return cpuQuota;
    }

    public String resourceId() {
        return resourceId;
    }

    public Boolean deletionProtection() {
        return deletionProtection;
    }

    public String description() {
        return description;
    }

    public static final class Builder {
        private VikingDbResourceRef target;
        private VikingDbIndexVectorOptions vector;
        private List<String> scalarIndexes;
        private Integer shardCount;
        private String shardPolicy;
        private Integer cpuQuota;
        private String resourceId;
        private Boolean deletionProtection;
        private String description;

        public Builder target(VikingDbResourceRef value) {
            this.target = value;
            return this;
        }

        public Builder vector(VikingDbIndexVectorOptions value) {
            this.vector = value;
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

        public Builder cpuQuota(Integer value) {
            this.cpuQuota = value;
            return this;
        }

        public Builder resourceId(String value) {
            this.resourceId = value;
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

        public VikingDbIndexDefinition build() {
            return new VikingDbIndexDefinition(this);
        }
    }
}
