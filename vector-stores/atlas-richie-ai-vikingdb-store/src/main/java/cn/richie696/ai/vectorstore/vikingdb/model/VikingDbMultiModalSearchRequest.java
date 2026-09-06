package cn.richie696.ai.vectorstore.vikingdb.model;

import org.springframework.ai.vectorstore.filter.Filter;

public final class VikingDbMultiModalSearchRequest {
    private final VikingDbResourceRef target;
    private final String text;
    private final VikingDbMediaInput image;
    private final VikingDbMediaInput video;
    private final VikingDbSearchInstruction instruction;
    private final VikingDbTensorRerankOptions tensorRerank;
    private final VikingDbModelRerankOptions modelRerank;
    private final Filter.Expression mandatoryFilter;
    private final Filter.Expression queryFilter;
    private final VikingDbSearchCommonOptions common;
    private final VikingDbSearchAdvanceOptions advance;

    private VikingDbMultiModalSearchRequest(Builder b) {
        target = b.target;
        text = b.text;
        image = b.image;
        video = b.video;
        instruction = b.instruction;
        tensorRerank = b.tensorRerank;
        modelRerank = b.modelRerank;
        mandatoryFilter = b.mandatoryFilter;
        queryFilter = b.queryFilter;
        common = b.common == null ? VikingDbSearchCommonOptions.empty() : b.common;
        advance = b.advance == null ? VikingDbSearchAdvanceOptions.empty() : b.advance;
    }

    public static Builder builder() {
        return new Builder();
    }

    public VikingDbResourceRef target() {
        return target;
    }

    public String text() {
        return text;
    }

    public VikingDbMediaInput image() {
        return image;
    }

    public VikingDbMediaInput video() {
        return video;
    }

    public VikingDbSearchInstruction instruction() {
        return instruction;
    }

    public VikingDbTensorRerankOptions tensorRerank() {
        return tensorRerank;
    }

    public VikingDbModelRerankOptions modelRerank() {
        return modelRerank;
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
        private String text;
        private VikingDbMediaInput image;
        private VikingDbMediaInput video;
        private VikingDbSearchInstruction instruction;
        private VikingDbTensorRerankOptions tensorRerank;
        private VikingDbModelRerankOptions modelRerank;
        private Filter.Expression mandatoryFilter;
        private Filter.Expression queryFilter;
        private VikingDbSearchCommonOptions common;
        private VikingDbSearchAdvanceOptions advance;

        public Builder target(VikingDbResourceRef v) {
            target = v;
            return this;
        }

        public Builder text(String v) {
            text = v;
            return this;
        }

        public Builder image(VikingDbMediaInput v) {
            image = v;
            return this;
        }

        public Builder video(VikingDbMediaInput v) {
            video = v;
            return this;
        }

        public Builder instruction(VikingDbSearchInstruction v) {
            instruction = v;
            return this;
        }

        public Builder tensorRerank(VikingDbTensorRerankOptions v) {
            tensorRerank = v;
            return this;
        }

        public Builder modelRerank(VikingDbModelRerankOptions v) {
            modelRerank = v;
            return this;
        }

        public Builder mandatoryFilter(Filter.Expression v) {
            mandatoryFilter = v;
            return this;
        }

        public Builder queryFilter(Filter.Expression v) {
            queryFilter = v;
            return this;
        }

        public Builder common(VikingDbSearchCommonOptions v) {
            common = v;
            return this;
        }

        public Builder advance(VikingDbSearchAdvanceOptions v) {
            advance = v;
            return this;
        }

        public VikingDbMultiModalSearchRequest build() {
            return new VikingDbMultiModalSearchRequest(this);
        }
    }
}
