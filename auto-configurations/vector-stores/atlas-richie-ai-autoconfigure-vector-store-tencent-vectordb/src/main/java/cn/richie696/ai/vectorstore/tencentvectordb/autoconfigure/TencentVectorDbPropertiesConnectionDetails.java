package cn.richie696.ai.vectorstore.tencentvectordb.autoconfigure;

import com.tencent.tcvectordb.model.param.enums.ReadConsistencyEnum;

/** Default connection details backed by application properties. */
final class TencentVectorDbPropertiesConnectionDetails implements TencentVectorDbConnectionDetails {
    private final TencentVectorDbClientProperties properties;

    TencentVectorDbPropertiesConnectionDetails(TencentVectorDbClientProperties properties) {
        this.properties = properties;
    }

    @Override public String getUrl() { return properties.getUrl(); }
    @Override public String getUsername() { return properties.getUsername(); }
    @Override public String getApiKey() { return properties.getApiKey(); }
    @Override public int getTimeoutSeconds() { return properties.getTimeoutSeconds(); }
    @Override public int getConnectTimeoutSeconds() { return properties.getConnectTimeoutSeconds(); }
    @Override public int getMaxIdleConnections() { return properties.getMaxIdleConnections(); }
    @Override public int getKeepAliveDurationSeconds() { return properties.getKeepAliveDurationSeconds(); }
    @Override public ReadConsistencyEnum getReadConsistency() { return properties.getReadConsistency(); }
}
