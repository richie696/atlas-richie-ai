package cn.richie696.ai.vectorstore.dashvector.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** DashVector SDK client properties. */
@ConfigurationProperties(DashVectorClientProperties.CONFIG_PREFIX)
public class DashVectorClientProperties {
    public static final String CONFIG_PREFIX = "spring.ai.vectorstore.dashvector.client";
    private String endpoint;
    private String apiKey;
    private float timeoutSeconds = 10.0f;

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public float getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(float timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}
