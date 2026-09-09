package cn.richie696.ai.vectorstore.tencentvectordb.api;

import com.tencent.tcvectordb.model.Document;
import com.tencent.tcvectordb.model.param.dml.FullTextSearchParam;
import com.tencent.tcvectordb.model.param.dml.HybridSearchParam;
import com.tencent.tcvectordb.model.param.dml.SearchByEmbeddingItemsParam;
import com.tencent.tcvectordb.model.param.dml.SearchByIdParam;
import com.tencent.tcvectordb.model.param.dml.SearchByVectorParam;
import com.tencent.tcvectordb.model.param.entity.FullTextSearchRes;
import com.tencent.tcvectordb.model.param.entity.HybridSearchRes;
import com.tencent.tcvectordb.model.param.entity.SearchRes;

import java.util.List;

/** Dense, ID, managed-embedding, full-text and hybrid search operations. */
public interface TencentVectorDbSearchOperations {
    List<List<Document>> search(SearchByVectorParam param);
    List<List<Document>> searchById(SearchByIdParam param);
    SearchRes searchByText(SearchByEmbeddingItemsParam param);
    FullTextSearchRes fullTextSearch(FullTextSearchParam param);
    HybridSearchRes hybridSearch(HybridSearchParam param);
}
