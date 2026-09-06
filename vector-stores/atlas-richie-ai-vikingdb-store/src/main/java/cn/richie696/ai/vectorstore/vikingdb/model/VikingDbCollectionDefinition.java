package cn.richie696.ai.vectorstore.vikingdb.model;

import java.util.List;

public record VikingDbCollectionDefinition(VikingDbResourceRef target, List<VikingDbCollectionField> fields,
                                           String description, Boolean deletionProtection) {
    public VikingDbCollectionDefinition {
        fields = fields == null ? List.of() : List.copyOf(fields);
    }
}
