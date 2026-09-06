package cn.richie696.ai.vectorstore.vikingdb.model;

import java.util.List;

public record VikingDbRerankResponse(String requestId, List<VikingDbRerankHit> hits, Object tokenUsage) {
    public VikingDbRerankResponse {
        hits = hits == null ? List.of() : List.copyOf(hits);
    }
}
