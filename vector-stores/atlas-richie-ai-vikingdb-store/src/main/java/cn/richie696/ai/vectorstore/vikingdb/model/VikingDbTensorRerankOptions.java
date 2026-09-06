package cn.richie696.ai.vectorstore.vikingdb.model;

import java.util.List;

/**
 * Provider-native tensor rerank options without exposing SDK DTOs.
 */
public final class VikingDbTensorRerankOptions {
    private final List<List<Double>> tensor;
    private final Integer inputLimit;
    private final String maxSimilarityAlgo;

    public VikingDbTensorRerankOptions(List<List<Double>> tensor, Integer inputLimit, String maxSimilarityAlgo) {
        this.tensor = tensor == null ? List.of() : tensor.stream().map(List::copyOf).toList();
        this.inputLimit = inputLimit;
        this.maxSimilarityAlgo = maxSimilarityAlgo;
    }

    public List<List<Double>> tensor() {
        return tensor;
    }

    public Integer inputLimit() {
        return inputLimit;
    }

    public String maxSimilarityAlgo() {
        return maxSimilarityAlgo;
    }
}
