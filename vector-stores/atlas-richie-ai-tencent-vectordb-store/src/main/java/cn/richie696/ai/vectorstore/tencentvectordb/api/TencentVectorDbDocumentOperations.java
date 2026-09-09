package cn.richie696.ai.vectorstore.tencentvectordb.api;

import com.tencent.tcvectordb.model.Document;
import com.tencent.tcvectordb.model.param.dml.CountQueryParam;
import com.tencent.tcvectordb.model.param.dml.DeleteParam;
import com.tencent.tcvectordb.model.param.dml.InsertParam;
import com.tencent.tcvectordb.model.param.dml.QueryParam;
import com.tencent.tcvectordb.model.param.dml.UpdateParam;
import com.tencent.tcvectordb.model.param.entity.AffectRes;
import com.tencent.tcvectordb.model.param.entity.BaseRes;
import org.json.JSONObject;

import java.util.List;

/** Bound-database and bound-collection document operations. */
public interface TencentVectorDbDocumentOperations {
    AffectRes upsert(InsertParam param);
    List<Document> query(QueryParam param);
    AffectRes update(UpdateParam param, Document document);
    AffectRes update(UpdateParam param, JSONObject document);
    AffectRes delete(DeleteParam param);
    BaseRes count(CountQueryParam param);
}
