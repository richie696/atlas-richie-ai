package cn.richie696.ai.vectorstore.vikingdb.api;

import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbKeywordSearchRequest;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbMultiModalSearchRequest;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbSearchResponse;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbVectorSearchRequest;

/**
 * Provider-neutral entry point for VikingDB vector search modes.
 */
public interface VikingDbSearchOperations {
    VikingDbSearchResponse search(VikingDbVectorSearchRequest request);

    VikingDbSearchResponse search(VikingDbKeywordSearchRequest request);

    VikingDbSearchResponse search(VikingDbMultiModalSearchRequest request);
}
