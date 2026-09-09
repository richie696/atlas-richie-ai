package cn.richie696.ai.vectorstore.tencentvectordb.api;

import com.tencent.tcvectordb.model.Collection;
import com.tencent.tcvectordb.model.param.collection.CreateCollectionParam;
import com.tencent.tcvectordb.model.param.entity.AffectRes;

import java.util.List;

/** Tencent VectorDB collection lifecycle and alias operations. */
public interface TencentVectorDbCollectionOperations {
    boolean collectionExists(String database, String collection);
    Collection createCollection(String database, CreateCollectionParam param);
    Collection createCollectionIfNotExists(String database, CreateCollectionParam param);
    List<Collection> listCollections(String database);
    Collection describeCollection(String database, String collection);
    AffectRes truncateCollection(String database, String collection);
    void dropCollection(String database, String collection);
    AffectRes setAlias(String database, String collection, String alias);
    AffectRes deleteAlias(String database, String alias);
}
