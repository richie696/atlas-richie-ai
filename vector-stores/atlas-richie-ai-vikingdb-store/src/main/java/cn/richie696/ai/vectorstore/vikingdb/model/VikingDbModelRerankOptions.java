package cn.richie696.ai.vectorstore.vikingdb.model;

/**
 * Model rerank options embedded in a multimodal search request.
 */
public record VikingDbModelRerankOptions(String modelName, String modelVersion,
                                         String instruction, Integer inputLimit,
                                         Double scoreThreshold, String failStrategy,
                                         Integer timeoutMs) {
}
