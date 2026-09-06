package cn.richie696.ai.vectorstore.vikingdb.model;

import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;

/**
 * Keyword search command; BM25 tuning is explicit and never inferred from empty fields.
 */
public final class VikingDbKeywordSearchRequest {
    public enum Mode {
        SEMANTIC_THEN_MATCH("semantic_then_match"),
        BM25("bm25");

        private final String wireValue;

        Mode(String wireValue) {
            this.wireValue = wireValue;
        }

        public String wireValue() {
            return wireValue;
        }
    }

    private final VikingDbResourceRef target;
    private final Mode mode;
    private final String query;
    private final List<String> keywords;
    private final List<String> fields;
    private final Boolean caseSensitive;
    private final Double bm25K1;
    private final Double bm25B;
    private final Filter.Expression mandatoryFilter;
    private final Filter.Expression queryFilter;
    private final VikingDbSearchCommonOptions common;
    private final VikingDbSearchAdvanceOptions advance;

    private VikingDbKeywordSearchRequest(Builder b) {
        this.target = b.target;
        this.mode = b.mode;
        this.query = b.query;
        this.keywords = b.keywords == null ? List.of() : List.copyOf(b.keywords);
        this.fields = b.fields == null ? List.of() : List.copyOf(b.fields);
        this.caseSensitive = b.caseSensitive;
        this.bm25K1 = b.bm25K1;
        this.bm25B = b.bm25B;
        this.mandatoryFilter = b.mandatoryFilter;
        this.queryFilter = b.queryFilter;
        this.common = b.common == null ? VikingDbSearchCommonOptions.empty() : b.common;
        this.advance = b.advance == null ? VikingDbSearchAdvanceOptions.empty() : b.advance;
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

    public String query() {
        return query;
    }

    public List<String> keywords() {
        return keywords;
    }

    public List<String> fields() {
        return fields;
    }

    public Boolean caseSensitive() {
        return caseSensitive;
    }

    public Double bm25K1() {
        return bm25K1;
    }

    public Double bm25B() {
        return bm25B;
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

    public static final class Builder {
        private VikingDbResourceRef target;
        private Mode mode;
        private String query;
        private List<String> keywords;
        private List<String> fields;
        private Boolean caseSensitive;
        private Double bm25K1;
        private Double bm25B;
        private Filter.Expression mandatoryFilter;
        private Filter.Expression queryFilter;
        private VikingDbSearchCommonOptions common;
        private VikingDbSearchAdvanceOptions advance;

        public Builder target(VikingDbResourceRef value) {
            this.target = value;
            return this;
        }

        public Builder mode(Mode value) {
            this.mode = value;
            return this;
        }

        public Builder query(String value) {
            this.query = value;
            return this;
        }

        public Builder keywords(List<String> value) {
            this.keywords = value;
            return this;
        }

        public Builder fields(List<String> value) {
            this.fields = value;
            return this;
        }

        public Builder caseSensitive(Boolean value) {
            this.caseSensitive = value;
            return this;
        }

        public Builder bm25K1(Double value) {
            this.bm25K1 = value;
            return this;
        }

        public Builder bm25B(Double value) {
            this.bm25B = value;
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

        public VikingDbKeywordSearchRequest build() {
            return new VikingDbKeywordSearchRequest(this);
        }
    }
}
