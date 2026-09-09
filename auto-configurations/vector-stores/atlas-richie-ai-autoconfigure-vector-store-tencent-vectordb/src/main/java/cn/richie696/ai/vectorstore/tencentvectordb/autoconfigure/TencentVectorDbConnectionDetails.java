package cn.richie696.ai.vectorstore.tencentvectordb.autoconfigure;

import com.tencent.tcvectordb.model.param.enums.ReadConsistencyEnum;

/** Connection settings required to construct the Tencent VectorDB HTTP SDK client. */
public interface TencentVectorDbConnectionDetails {
    String getUrl();
    String getUsername();
    String getApiKey();
    int getTimeoutSeconds();
    int getConnectTimeoutSeconds();
    int getMaxIdleConnections();
    int getKeepAliveDurationSeconds();
    ReadConsistencyEnum getReadConsistency();
}
