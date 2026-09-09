package cn.richie696.ai.vectorstore.dashvector.autoconfigure;

/** Connection information for an Alibaba Cloud DashVector cluster. */
public interface DashVectorConnectionDetails {
    String getEndpoint();
    String getApiKey();
    float getTimeoutSeconds();
}
