package cn.richie696.ai.vectorstore.tencentvectordb.api;

import com.tencent.tcvectordb.model.AIDatabase;
import com.tencent.tcvectordb.model.param.collection.UploadFileParam;
import com.tencent.tcvectordb.model.param.dml.AtomicEmbeddingParam;
import com.tencent.tcvectordb.model.param.dml.GetImageUrlParam;
import com.tencent.tcvectordb.model.param.dml.QueryFileDetailParam;
import com.tencent.tcvectordb.model.param.entity.AffectRes;
import com.tencent.tcvectordb.model.param.entity.AtomicEmbeddingRes;
import com.tencent.tcvectordb.model.param.entity.GetImageUrlRes;
import com.tencent.tcvectordb.model.param.entity.QueryFileDetailRes;

import java.util.Map;

/** HTTP-SDK-only AI database, file parsing and atomic embedding operations. */
public interface TencentVectorDbAiOperations {
    AIDatabase createAiDatabase(String name);
    AIDatabase aiDatabase(String name);
    AffectRes dropAiDatabase(String name);
    void uploadFile(String database, String collection, UploadFileParam param, Map<String, Object> metadata) throws Exception;
    GetImageUrlRes getImageUrl(String database, String collection, GetImageUrlParam param);
    QueryFileDetailRes queryFileDetails(String database, String collection, QueryFileDetailParam param);
    AtomicEmbeddingRes atomicEmbedding(AtomicEmbeddingParam param);
}
