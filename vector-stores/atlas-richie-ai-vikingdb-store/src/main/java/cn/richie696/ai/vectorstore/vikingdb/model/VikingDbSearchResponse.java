package cn.richie696.ai.vectorstore.vikingdb.model;

import java.util.List;

/**
 * Stable response returned by VikingDB advanced search APIs.
 */
public record VikingDbSearchResponse(List<VikingDbSearchHit> hits,
                                     VikingDbSearchExecutionEvidence execution) {
    public VikingDbSearchResponse {
        hits = hits == null ? List.of() : List.copyOf(hits);
    }
}
