package cn.richie696.ai.vectorstore.dashvector.autoconfigure;

final class DashVectorPropertiesConnectionDetails implements DashVectorConnectionDetails {
    private final DashVectorClientProperties properties;
    DashVectorPropertiesConnectionDetails(DashVectorClientProperties properties) { this.properties = properties; }
    @Override public String getEndpoint() { return properties.getEndpoint(); }
    @Override public String getApiKey() { return properties.getApiKey(); }
    @Override public float getTimeoutSeconds() { return properties.getTimeoutSeconds(); }
}
