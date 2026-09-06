package cn.richie696.ai.vectorstore.vikingdb.model;

/**
 * Provider-independent representation of VikingDB vector index options.
 */
public final class VikingDbIndexVectorOptions {
    public enum Type {HNSW, HNSW_HYBRID, FLAT, DISKANN}

    public enum Distance {COSINE, IP, L2}

    public enum Quantization {FLOAT, INT8, FIX16, PQ}

    private final Type type;
    private final Distance distance;
    private final Quantization quantization;
    private final Integer hnswM;
    private final Integer hnswCef;
    private final Integer hnswSef;
    private final Integer diskannM;
    private final Integer diskannCef;
    private final Float cacheRatio;
    private final Float pqCodeRatio;

    private VikingDbIndexVectorOptions(Builder b) {
        this.type = b.type;
        this.distance = b.distance;
        this.quantization = b.quantization;
        this.hnswM = b.hnswM;
        this.hnswCef = b.hnswCef;
        this.hnswSef = b.hnswSef;
        this.diskannM = b.diskannM;
        this.diskannCef = b.diskannCef;
        this.cacheRatio = b.cacheRatio;
        this.pqCodeRatio = b.pqCodeRatio;
        validate();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static VikingDbIndexVectorOptions defaults() {
        return builder().build();
    }

    public Type type() {
        return type;
    }

    public Distance distance() {
        return distance;
    }

    public Quantization quantization() {
        return quantization;
    }

    public Integer hnswM() {
        return hnswM;
    }

    public Integer hnswCef() {
        return hnswCef;
    }

    public Integer hnswSef() {
        return hnswSef;
    }

    public Integer diskannM() {
        return diskannM;
    }

    public Integer diskannCef() {
        return diskannCef;
    }

    public Float cacheRatio() {
        return cacheRatio;
    }

    public Float pqCodeRatio() {
        return pqCodeRatio;
    }

    private void validate() {
        boolean hnsw = type == Type.HNSW || type == Type.HNSW_HYBRID;
        boolean diskann = type == Type.DISKANN;
        if (!hnsw && (hnswM != null || hnswCef != null || hnswSef != null)) {
            throw new IllegalArgumentException("HNSW parameters require an HNSW index");
        }
        if (!diskann && (diskannM != null || diskannCef != null || cacheRatio != null)) {
            throw new IllegalArgumentException("DiskANN parameters require a DISKANN index");
        }
        if (quantization != Quantization.PQ && pqCodeRatio != null) {
            throw new IllegalArgumentException("pqCodeRatio requires PQ quantization");
        }
        if (hnswM != null && hnswM <= 0 || hnswCef != null && hnswCef <= 0 || hnswSef != null && hnswSef <= 0
                || diskannM != null && diskannM <= 0 || diskannCef != null && diskannCef <= 0) {
            throw new IllegalArgumentException("index construction/search parameters must be positive");
        }
        if (cacheRatio != null && (cacheRatio < 0 || cacheRatio > 1)) {
            throw new IllegalArgumentException("cacheRatio must be between 0 and 1");
        }
        if (pqCodeRatio != null && (pqCodeRatio <= 0 || pqCodeRatio > 1)) {
            throw new IllegalArgumentException("pqCodeRatio must be between 0 and 1");
        }
    }

    public static final class Builder {
        private Type type = Type.HNSW;
        private Distance distance = Distance.COSINE;
        private Quantization quantization = Quantization.FLOAT;
        private Integer hnswM;
        private Integer hnswCef;
        private Integer hnswSef;
        private Integer diskannM;
        private Integer diskannCef;
        private Float cacheRatio;
        private Float pqCodeRatio;

        public Builder type(Type value) {
            this.type = value == null ? Type.HNSW : value;
            return this;
        }

        public Builder distance(Distance value) {
            this.distance = value == null ? Distance.COSINE : value;
            return this;
        }

        public Builder quantization(Quantization value) {
            this.quantization = value == null ? Quantization.FLOAT : value;
            return this;
        }

        public Builder hnswM(Integer value) {
            this.hnswM = value;
            return this;
        }

        public Builder hnswCef(Integer value) {
            this.hnswCef = value;
            return this;
        }

        public Builder hnswSef(Integer value) {
            this.hnswSef = value;
            return this;
        }

        public Builder diskannM(Integer value) {
            this.diskannM = value;
            return this;
        }

        public Builder diskannCef(Integer value) {
            this.diskannCef = value;
            return this;
        }

        public Builder cacheRatio(Float value) {
            this.cacheRatio = value;
            return this;
        }

        public Builder pqCodeRatio(Float value) {
            this.pqCodeRatio = value;
            return this;
        }

        public VikingDbIndexVectorOptions build() {
            return new VikingDbIndexVectorOptions(this);
        }
    }
}
