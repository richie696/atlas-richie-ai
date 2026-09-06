package cn.richie696.ai.vectorstore.vikingdb.internal;

import cn.richie696.ai.vectorstore.vikingdb.VikingDbVectorStore;
import cn.richie696.ai.vectorstore.vikingdb.model.*;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Local validation for requests before they reach the provider.
 */
public final class VikingDbSearchValidator {
    private final int embeddingDimension;
    private final Map<String, ?> metadataFields;
    private final Set<String> scalarIndex;
    private final VikingDbFilterValidationMode filterMode;

    public VikingDbSearchValidator(int embeddingDimension, Map<String, ?> metadataFields,
                                   Set<String> scalarIndex, VikingDbFilterValidationMode filterMode) {
        this.embeddingDimension = embeddingDimension;
        this.metadataFields = metadataFields == null ? Map.of() : metadataFields;
        this.scalarIndex = scalarIndex == null ? Set.of() : Set.copyOf(scalarIndex);
        this.filterMode = filterMode == null ? VikingDbFilterValidationMode.DECLARED_FIELDS : filterMode;
    }

    public void validate(VikingDbVectorSearchRequest request) {
        Assert.notNull(request, "search request must not be null");
        Assert.notNull(request.mode(), "search mode must not be null");
        switch (request.mode()) {
            case DENSE -> {
                assertDense(request.denseVector());
                Assert.isTrue(request.sparseVector().isEmpty(), "DENSE request must not contain sparseVector");
            }
            case SPARSE -> {
                Assert.isTrue(request.denseVector() == null, "SPARSE request must not contain denseVector");
                assertSparse(request.sparseVector());
            }
            case HYBRID -> {
                assertDense(request.denseVector());
                assertSparse(request.sparseVector());
            }
        }
        validateAdvance(request.advance(), request.mode());
        validateCommon(request.common());
        validateFilters(request.mandatoryFilter(), request.queryFilter());
        if (request.tensorRerank() != null) {
            Assert.isTrue(!request.tensorRerank().tensor().isEmpty(), "tensor rerank tensor must not be empty");
        }
    }

    public void validateCommon(VikingDbSearchCommonOptions common) {
        if (common == null) return;
        if (common.limit() != null) Assert.isTrue(common.limit() > 0, "limit must be positive");
        if (common.offset() != null) Assert.isTrue(common.offset() >= 0, "offset must not be negative");
        if (common.outputFields() != null) {
            for (String field : common.outputFields()) {
                Assert.hasText(field, "output field must not be blank");
                Assert.isTrue(isDeclaredField(field), "Output field is not declared in VikingDB schema: " + field);
            }
        }
    }

    public void validateKeywordFields(List<String> fields) {
        if (fields == null) return;
        for (String field : fields) {
            Assert.hasText(field, "keyword field must not be blank");
            Assert.isTrue(isDeclaredField(field), "Keyword field is not declared in VikingDB schema: " + field);
            Object type = metadataFields.get(field);
            Assert.isTrue(VikingDbVectorStore.CONTENT_FIELD_NAME.equals(field)
                            || (type != null && ("string".equalsIgnoreCase(String.valueOf(type))
                            || "text".equalsIgnoreCase(String.valueOf(type)))),
                    "Keyword field must be a string field: " + field);
        }
    }

    private boolean isDeclaredField(String field) {
        return VikingDbVectorStore.DOC_ID_FIELD_NAME.equals(field)
                || VikingDbVectorStore.CONTENT_FIELD_NAME.equals(field)
                || VikingDbVectorStore.EMBEDDING_FIELD_NAME.equals(field)
                || metadataFields.containsKey(field);
    }

    private void assertDense(float[] vector) {
        Assert.notNull(vector, "denseVector is required");
        Assert.isTrue(vector.length == embeddingDimension,
                "denseVector dimension must be " + embeddingDimension);
        for (float value : vector) Assert.isTrue(Float.isFinite(value), "denseVector must contain finite values");
    }

    private void assertSparse(Map<String, Float> vector) {
        Assert.notEmpty(vector, "sparseVector is required");
        vector.forEach((key, value) -> {
            Assert.hasText(key, "sparseVector key must not be blank");
            Assert.notNull(value, "sparseVector value must not be null");
            Assert.isTrue(Float.isFinite(value), "sparseVector must contain finite values");
        });
    }

    public void validateAdvance(VikingDbSearchAdvanceOptions advance, VikingDbVectorSearchRequest.Mode mode) {
        if (advance == null) advance = VikingDbSearchAdvanceOptions.empty();
        if (advance.denseWeight() != null) {
            Assert.isTrue(mode == VikingDbVectorSearchRequest.Mode.HYBRID,
                    "denseWeight is only valid for HYBRID search");
            Assert.isTrue(Double.isFinite(advance.denseWeight())
                            && advance.denseWeight() >= 0 && advance.denseWeight() <= 1,
                    "denseWeight must be between 0 and 1");
        }
        if (advance.postProcessInputLimit() != null) {
            Assert.isTrue(advance.postProcessInputLimit() > 0, "postProcessInputLimit must be positive");
        }
        if (advance.scaleK() != null) {
            Assert.isTrue(Double.isFinite(advance.scaleK()) && advance.scaleK() > 0, "scaleK must be positive");
        }
        if (advance.filterPreAnnLimit() != null) {
            Assert.isTrue(advance.filterPreAnnLimit() > 0, "filterPreAnnLimit must be positive");
        }
        if (advance.filterPreAnnRatio() != null) {
            Assert.isTrue(Double.isFinite(advance.filterPreAnnRatio())
                    && advance.filterPreAnnRatio() > 0, "filterPreAnnRatio must be positive");
        }
        Set<Object> intersection = new HashSet<>(advance.idsIn());
        intersection.retainAll(advance.idsNotIn());
        Assert.isTrue(intersection.isEmpty(), "idsIn and idsNotIn must not overlap");
        for (VikingDbPostProcessOperation operation : advance.postProcessOperations()) {
            Assert.notNull(operation, "post-process operation must not be null");
        }
    }

    public void validateFilters(Filter.Expression mandatoryFilter, Filter.Expression queryFilter) {
        validateFilter(mandatoryFilter);
        validateFilter(queryFilter);
    }

    private void validateFilter(Filter.Expression expression) {
        if (expression == null) return;
        Set<String> fields = VikingDbFilterExpressionFields.collect(expression);
        for (String field : fields) {
            Assert.isTrue(VikingDbVectorStore.DOC_ID_FIELD_NAME.equals(field)
                            || VikingDbVectorStore.CONTENT_FIELD_NAME.equals(field)
                            || metadataFields.containsKey(field),
                    "Filter field is not declared in VikingDB schema: " + field);
            if (filterMode == VikingDbFilterValidationMode.SCALAR_INDEXED_ONLY) {
                Assert.isTrue(scalarIndex.contains(field), "Filter field is not scalar indexed: " + field);
            }
        }
    }
}
