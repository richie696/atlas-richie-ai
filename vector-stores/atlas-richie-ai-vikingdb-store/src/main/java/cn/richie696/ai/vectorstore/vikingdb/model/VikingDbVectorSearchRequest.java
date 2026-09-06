package cn.richie696.ai.vectorstore.vikingdb.model;

import org.springframework.ai.vectorstore.filter.Filter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable dense, sparse or hybrid search command.
 */
public final class VikingDbVectorSearchRequest {
    public enum Mode {DENSE, SPARSE, HYBRID}

    private final VikingDbResourceRef target;
    private final Mode mode;
    private final float[] denseVector;
    private final Map<String, Float> sparseVector;
    private final Filter.Expression mandatoryFilter;
    private final Filter.Expression queryFilter;
    private final VikingDbSearchCommonOptions common;
    private final VikingDbSearchAdvanceOptions advance;
    private final VikingDbTensorRerankOptions tensorRerank;

    private VikingDbVectorSearchRequest(Builder b) {
        this.target = b.target;
        this.mode = b.mode;
        this.denseVector = b.denseVector == null ? null : b.denseVector.clone();
        this.sparseVector = b.sparseVector == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(b.sparseVector));
        this.mandatoryFilter = b.mandatoryFilter;
        this.queryFilter = b.queryFilter;
        this.common = b.common == null ? VikingDbSearchCommonOptions.empty() : b.common;
        this.advance = b.advance == null ? VikingDbSearchAdvanceOptions.empty() : b.advance;
        this.tensorRerank = b.tensorRerank;
    }

    public static Builder builder() {
        return new Builder();
    }

    public VikingDbResourceRef target() {
        return target;
    }

    public Mode mode() {
        return mode;
    }

    public float[] denseVector() {
        return denseVector == null ? null : denseVector.clone();
    }

    public Map<String, Float> sparseVector() {
        return sparseVector;
    }

    public Filter.Expression mandatoryFilter() {
        return mandatoryFilter;
    }

    public Filter.Expression queryFilter() {
        return queryFilter;
    }

    public VikingDbSearchCommonOptions common() {
        return common;
    }

    public VikingDbSearchAdvanceOptions advance() {
        return advance;
    }

    public VikingDbTensorRerankOptions tensorRerank() {
        return tensorRerank;
    }

    public static final class Builder {
        private VikingDbResourceRef target;
        private Mode mode;
        private float[] denseVector;
        private Map<String, Float> sparseVector;
        private Filter.Expression mandatoryFilter;
        private Filter.Expression queryFilter;
        private VikingDbSearchCommonOptions common;
        private VikingDbSearchAdvanceOptions advance;
        private VikingDbTensorRerankOptions tensorRerank;

        public Builder target(VikingDbResourceRef value) {
            this.target = value;
            return this;
        }

        public Builder mode(Mode value) {
            this.mode = value;
            return this;
        }

        public Builder denseVector(float[] value) {
            this.denseVector = value;
            return this;
        }

        public Builder sparseVector(Map<String, Float> value) {
            this.sparseVector = value;
            return this;
        }

        public Builder mandatoryFilter(Filter.Expression value) {
            this.mandatoryFilter = value;
            return this;
        }

        public Builder queryFilter(Filter.Expression value) {
            this.queryFilter = value;
            return this;
        }

        public Builder common(VikingDbSearchCommonOptions value) {
            this.common = value;
            return this;
        }

        public Builder advance(VikingDbSearchAdvanceOptions value) {
            this.advance = value;
            return this;
        }

        public Builder tensorRerank(VikingDbTensorRerankOptions value) {
            this.tensorRerank = value;
            return this;
        }

        public VikingDbVectorSearchRequest build() {
            return new VikingDbVectorSearchRequest(this);
        }
    }
}
