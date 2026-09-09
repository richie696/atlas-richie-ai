package cn.richie696.ai.vectorstore.dashvector.api;

import com.aliyun.dashvector.models.CollectionMeta;
import com.aliyun.dashvector.models.CollectionStats;
import com.aliyun.dashvector.models.requests.CreateCollectionRequest;
import com.aliyun.dashvector.models.responses.Response;

import java.util.List;

/** DashVector collection control-plane operations exposed by this provider plugin. */
public interface DashVectorCollectionOperations {
    Response<Void> createCollection(CreateCollectionRequest request);
    Response<List<String>> listCollections();
    Response<CollectionMeta> describeCollection(String name);
    Response<Void> deleteCollection(String name);
    Response<CollectionStats> collectionStats();
    Response<CollectionStats> collectionStats(String collectionName);
}
