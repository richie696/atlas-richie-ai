package cn.richie696.ai.vectorstore.vikingdb.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provider result with all score stages retained.
 */
public record VikingDbSearchHit(String id, Map<String, Object> fields, Double score,
                                Double annScore, Double originScore, Double additionScore) {
    public VikingDbSearchHit {
        fields = fields == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(fields));
    }
}
