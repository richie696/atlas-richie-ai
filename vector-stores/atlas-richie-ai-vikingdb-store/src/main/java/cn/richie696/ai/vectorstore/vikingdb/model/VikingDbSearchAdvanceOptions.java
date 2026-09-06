package cn.richie696.ai.vectorstore.vikingdb.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional fields of the VikingDB SearchAdvance request.
 */
public final class VikingDbSearchAdvanceOptions {
    private final Double denseWeight;
    private final List<Object> idsIn;
    private final List<Object> idsNotIn;
    private final List<VikingDbPostProcessOperation> postProcessOperations;
    private final Integer postProcessInputLimit;
    private final Double scaleK;
    private final Integer filterPreAnnLimit;
    private final Double filterPreAnnRatio;

    private VikingDbSearchAdvanceOptions(Builder builder) {
        this.denseWeight = builder.denseWeight;
        this.idsIn = immutable(builder.idsIn);
        this.idsNotIn = immutable(builder.idsNotIn);
        this.postProcessOperations = builder.postProcessOperations == null
                ? List.of() : List.copyOf(builder.postProcessOperations);
        this.postProcessInputLimit = builder.postProcessInputLimit;
        this.scaleK = builder.scaleK;
        this.filterPreAnnLimit = builder.filterPreAnnLimit;
        this.filterPreAnnRatio = builder.filterPreAnnRatio;
    }

    private static List<Object> immutable(List<?> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static VikingDbSearchAdvanceOptions empty() {
        return builder().build();
    }

    public Double denseWeight() {
        return denseWeight;
    }

    public List<Object> idsIn() {
        return idsIn;
    }

    public List<Object> idsNotIn() {
        return idsNotIn;
    }

    public List<VikingDbPostProcessOperation> postProcessOperations() {
        return postProcessOperations;
    }

    public Integer postProcessInputLimit() {
        return postProcessInputLimit;
    }

    public Double scaleK() {
        return scaleK;
    }

    public Integer filterPreAnnLimit() {
        return filterPreAnnLimit;
    }

    public Double filterPreAnnRatio() {
        return filterPreAnnRatio;
    }

    public boolean isEmpty() {
        return denseWeight == null && idsIn.isEmpty() && idsNotIn.isEmpty()
                && postProcessOperations.isEmpty() && postProcessInputLimit == null
                && scaleK == null && filterPreAnnLimit == null && filterPreAnnRatio == null;
    }

    public static final class Builder {
        private Double denseWeight;
        private List<Object> idsIn;
        private List<Object> idsNotIn;
        private List<VikingDbPostProcessOperation> postProcessOperations;
        private Integer postProcessInputLimit;
        private Double scaleK;
        private Integer filterPreAnnLimit;
        private Double filterPreAnnRatio;

        public Builder denseWeight(Double value) {
            this.denseWeight = value;
            return this;
        }

        public Builder idsIn(List<Object> value) {
            this.idsIn = value;
            return this;
        }

        public Builder idsNotIn(List<Object> value) {
            this.idsNotIn = value;
            return this;
        }

        public Builder postProcessOperations(List<VikingDbPostProcessOperation> value) {
            this.postProcessOperations = value;
            return this;
        }

        public Builder addPostProcessOperation(VikingDbPostProcessOperation value) {
            if (this.postProcessOperations == null) this.postProcessOperations = new ArrayList<>();
            this.postProcessOperations.add(value);
            return this;
        }

        public Builder postProcessInputLimit(Integer value) {
            this.postProcessInputLimit = value;
            return this;
        }

        public Builder scaleK(Double value) {
            this.scaleK = value;
            return this;
        }

        public Builder filterPreAnnLimit(Integer value) {
            this.filterPreAnnLimit = value;
            return this;
        }

        public Builder filterPreAnnRatio(Double value) {
            this.filterPreAnnRatio = value;
            return this;
        }

        public VikingDbSearchAdvanceOptions build() {
            return new VikingDbSearchAdvanceOptions(this);
        }
    }
}
