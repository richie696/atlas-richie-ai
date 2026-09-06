package cn.richie696.ai.vectorstore.vikingdb.model;

import java.util.List;

public record VikingDbRerankRequest(String projectName, String modelName, String modelVersion,
                                    List<List<VikingDbMediaInput>> data, List<VikingDbMediaInput> query,
                                    String instruction, Boolean returnOriginData, Integer maxRetryTime) {
    public VikingDbRerankRequest {
        data = data == null ? List.of() : data.stream().map(List::copyOf).toList();
        query = query == null ? List.of() : List.copyOf(query);
    }
}
