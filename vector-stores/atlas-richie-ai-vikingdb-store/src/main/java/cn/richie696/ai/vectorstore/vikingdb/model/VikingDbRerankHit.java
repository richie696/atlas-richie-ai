package cn.richie696.ai.vectorstore.vikingdb.model;

import java.util.List;

public record VikingDbRerankHit(Integer providerIndex, Float score, List<VikingDbMediaInput> originData) {
    public VikingDbRerankHit {
        originData = originData == null ? List.of() : List.copyOf(originData);
    }
}
