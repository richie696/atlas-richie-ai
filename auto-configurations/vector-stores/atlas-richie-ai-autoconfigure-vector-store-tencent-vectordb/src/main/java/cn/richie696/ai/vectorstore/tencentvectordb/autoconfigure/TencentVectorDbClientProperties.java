package cn.richie696.ai.vectorstore.tencentvectordb.autoconfigure;

import com.tencent.tcvectordb.model.param.enums.ReadConsistencyEnum;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Tencent VectorDB HTTP SDK client properties. */
@ConfigurationProperties(TencentVectorDbClientProperties.CONFIG_PREFIX)
public class TencentVectorDbClientProperties {
    public static final String CONFIG_PREFIX = "spring.ai.vectorstore.tencent-vectordb.client";

    private String url;
    private String username = "root";
    private String apiKey;
    private int timeoutSeconds = 10;
    private int connectTimeoutSeconds = 10;
    private int maxIdleConnections = 10;
    private int keepAliveDurationSeconds = 300;
    private ReadConsistencyEnum readConsistency = ReadConsistencyEnum.EVENTUAL_CONSISTENCY;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public int getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) { this.connectTimeoutSeconds = connectTimeoutSeconds; }
    public int getMaxIdleConnections() { return maxIdleConnections; }
    public void setMaxIdleConnections(int maxIdleConnections) { this.maxIdleConnections = maxIdleConnections; }
    public int getKeepAliveDurationSeconds() { return keepAliveDurationSeconds; }
    public void setKeepAliveDurationSeconds(int keepAliveDurationSeconds) { this.keepAliveDurationSeconds = keepAliveDurationSeconds; }
    public ReadConsistencyEnum getReadConsistency() { return readConsistency; }
    public void setReadConsistency(ReadConsistencyEnum readConsistency) { this.readConsistency = readConsistency; }
}
