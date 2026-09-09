package cn.richie696.ai.vectorstore.dashvector.api;

import com.aliyun.dashvector.models.Doc;
import com.aliyun.dashvector.models.Group;
import com.aliyun.dashvector.models.requests.QueryDocGroupByRequest;
import com.aliyun.dashvector.models.requests.QueryDocRequest;
import com.aliyun.dashvector.models.responses.Response;

import java.util.List;

/** Dense, sparse, multi-vector, fusion-rank and group-by search operations. */
public interface DashVectorSearchOperations {
    Response<List<Doc>> query(QueryDocRequest request);
    Response<List<Doc>> query(String collectionName, QueryDocRequest request);
    Response<List<Group>> queryGroupBy(QueryDocGroupByRequest request);
    Response<List<Group>> queryGroupBy(String collectionName, QueryDocGroupByRequest request);
}
