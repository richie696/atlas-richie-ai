package cn.richie696.ai.vectorstore.vikingdb.model;

import java.util.List;

public record VikingDbIndexChangePlan(VikingDbIndexChangeKind kind, List<String> reasons) {
    public VikingDbIndexChangePlan {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
