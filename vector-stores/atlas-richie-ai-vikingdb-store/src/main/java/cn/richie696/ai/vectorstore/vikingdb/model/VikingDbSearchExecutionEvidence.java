package cn.richie696.ai.vectorstore.vikingdb.model;

import java.util.Map;

/**
 * Redacted evidence from one provider search call.
 */
public record VikingDbSearchExecutionEvidence(
        String requestId,
        String providerCode,
        String providerMessage,
        String searchMode,
        int returnedCount,
        Integer filterMatchedCount,
        Integer providerTotalReturnCount,
        Integer embeddingTimeCostMs,
        Integer recallTimeCostMs,
        Integer rerankTimeCostMs,
        String rerankError,
        boolean mandatoryFilterApplied,
        String effectiveFilterDigest,
        Map<String, Object> effectiveOptions) {
    public VikingDbSearchExecutionEvidence {
        effectiveOptions = effectiveOptions == null ? Map.of() : Map.copyOf(effectiveOptions);
    }
}
