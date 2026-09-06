package cn.richie696.ai.vectorstore.vikingdb.model;

/**
 * Stable index information used by lifecycle and drift checks.
 */
public record VikingDbIndexInfo(
        VikingDbResourceRef target,
        String status,
        String type,
        String distance,
        String quantization,
        Integer hnswM,
        Integer hnswCef,
        Integer hnswSef,
        Integer diskannM,
        Integer diskannCef,
        Float cacheRatio,
        Float pqCodeRatio,
        Integer shardCount) {
}
