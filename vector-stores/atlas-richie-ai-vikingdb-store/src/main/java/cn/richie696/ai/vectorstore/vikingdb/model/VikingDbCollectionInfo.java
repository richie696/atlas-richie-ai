package cn.richie696.ai.vectorstore.vikingdb.model;

import java.util.List;
import java.util.Map;

public record VikingDbCollectionInfo(VikingDbResourceRef target, String description,
                                     Map<String, VikingDbCollectionField> fields,
                                     List<String> indexNames, Boolean keywordsSearchEnabled) {
    public VikingDbCollectionInfo {
        fields = fields == null ? Map.of() : Map.copyOf(fields);
        indexNames = indexNames == null ? List.of() : List.copyOf(indexNames);
    }
}
