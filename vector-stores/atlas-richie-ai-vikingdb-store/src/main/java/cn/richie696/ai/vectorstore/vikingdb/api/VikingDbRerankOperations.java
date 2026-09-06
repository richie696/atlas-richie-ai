package cn.richie696.ai.vectorstore.vikingdb.api;

import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbRerankRequest;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbRerankResponse;

public interface VikingDbRerankOperations {
    VikingDbRerankResponse rerank(VikingDbRerankRequest request);
}
