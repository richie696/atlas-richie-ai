package cn.richie696.ai.vectorstore.dashvector.api;

import com.aliyun.dashvector.models.Doc;
import com.aliyun.dashvector.models.DocOpResult;
import com.aliyun.dashvector.models.requests.DeleteDocRequest;
import com.aliyun.dashvector.models.requests.FetchDocRequest;
import com.aliyun.dashvector.models.requests.InsertDocRequest;
import com.aliyun.dashvector.models.requests.UpdateDocRequest;
import com.aliyun.dashvector.models.requests.UpsertDocRequest;
import com.aliyun.dashvector.models.responses.Response;

import java.util.List;
import java.util.Map;

/** Bound-collection document operations. */
public interface DashVectorDocumentOperations {
    Response<List<DocOpResult>> insert(InsertDocRequest request);
    Response<List<DocOpResult>> insert(String collectionName, InsertDocRequest request);
    Response<List<DocOpResult>> upsert(UpsertDocRequest request);
    Response<List<DocOpResult>> upsert(String collectionName, UpsertDocRequest request);
    Response<List<DocOpResult>> update(UpdateDocRequest request);
    Response<List<DocOpResult>> update(String collectionName, UpdateDocRequest request);
    Response<List<DocOpResult>> delete(DeleteDocRequest request);
    Response<List<DocOpResult>> delete(String collectionName, DeleteDocRequest request);
    Response<Map<String, Doc>> fetch(FetchDocRequest request);
    Response<Map<String, Doc>> fetch(String collectionName, FetchDocRequest request);
}
