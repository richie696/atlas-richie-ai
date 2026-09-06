package cn.richie696.ai.vectorstore.vikingdb.api;

import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbDocumentRecord;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbFetchResponse;

import java.util.List;

/**
 * Data-plane operations that are not expressible by Spring AI's VectorStore contract.
 */
public interface VikingDbDocumentOperations {
    void upsertRecords(List<VikingDbDocumentRecord> records);

    VikingDbFetchResponse fetchByIds(List<Object> ids, List<String> outputFields, String partition);

    void deleteByIds(List<Object> ids);
}
