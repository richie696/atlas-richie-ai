package cn.richie696.ai.vectorstore.tencentvectordb.api;

import com.tencent.tcvectordb.model.param.dml.AddIndexParam;
import com.tencent.tcvectordb.model.param.dml.ModifyVectorIndexParam;
import com.tencent.tcvectordb.model.param.dml.RebuildIndexParam;
import com.tencent.tcvectordb.model.param.entity.BaseRes;

import java.util.List;

/** Bound-collection scalar/vector index management. */
public interface TencentVectorDbIndexOperations {
    BaseRes rebuildIndex(RebuildIndexParam param);
    BaseRes addIndex(AddIndexParam param);
    BaseRes dropIndex(List<String> fieldNames);
    BaseRes modifyVectorIndex(ModifyVectorIndexParam param);
}
