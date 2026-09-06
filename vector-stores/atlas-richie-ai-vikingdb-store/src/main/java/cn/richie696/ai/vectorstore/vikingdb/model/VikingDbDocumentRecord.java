package cn.richie696.ai.vectorstore.vikingdb.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider-neutral pre-embedded record for direct VikingDB upsert.
 */
public record VikingDbDocumentRecord(Object id, String content, List<Float> denseVector,
                                     Map<String, Object> metadata) {
    public VikingDbDocumentRecord {
        denseVector = denseVector == null ? List.of() : List.copyOf(denseVector);
        metadata = metadata == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(metadata));
    }
}
