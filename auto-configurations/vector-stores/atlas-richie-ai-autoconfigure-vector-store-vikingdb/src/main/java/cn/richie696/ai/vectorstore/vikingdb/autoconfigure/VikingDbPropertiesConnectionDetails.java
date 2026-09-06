package cn.richie696.ai.vectorstore.vikingdb.autoconfigure;

/**
 * 由配置属性支持的默认 {@link VikingDbConnectionDetails} 实现。
 *
 * <p>每个访问器都委托到 {@link VikingDbClientProperties} 上的对应 getter。Spring Boot 仅在
 * 应用未提供其他 {@link VikingDbConnectionDetails} 实现时才注册此 Bean，因此用户提供的绑定自动胜出。</p>
 */
final class VikingDbPropertiesConnectionDetails implements VikingDbConnectionDetails {

    /**
     * 六个访问器的真值来源。构造后不变。
     */
    private final VikingDbClientProperties properties;

    /**
     * @param properties 已绑定的配置属性；每个访问器每次调用都从该对象读取，
     *                   因此启用 live refresh 时无需重建此 holder。
     */
    VikingDbPropertiesConnectionDetails(VikingDbClientProperties properties) {
        this.properties = properties;
    }

    /**
     * @return 来自 {@link VikingDbClientProperties#getHost()} 的数据面 host。
     */
    @Override
    public String getHost() {
        return properties.getHost();
    }

    /**
     * @return 来自 {@link VikingDbClientProperties#getControlEndpoint()} 的控制面 endpoint。
     */
    @Override
    public String getControlEndpoint() {
        return properties.getControlEndpoint();
    }

    /**
     * @return 来自 {@link VikingDbClientProperties#getRegion()} 的区域。
     */
    @Override
    public String getRegion() {
        return properties.getRegion();
    }

    /**
     * @return 来自 {@link VikingDbClientProperties#getAccessKey()} 的访问密钥。
     */
    @Override
    public String getAccessKey() {
        return properties.getAccessKey();
    }

    /**
     * @return 来自 {@link VikingDbClientProperties#getSecretKey()} 的密钥。
     */
    @Override
    public String getSecretKey() {
        return properties.getSecretKey();
    }

    @Override
    public VikingDbAuthenticationMode getAuthenticationMode() {
        return properties.getAuthenticationMode();
    }

    @Override
    public String getApiKey() {
        return properties.getApiKey();
    }

    /**
     * @return 来自 {@link VikingDbClientProperties#getScheme()} 的协议名称。
     */
    @Override
    public String getScheme() {
        return properties.getScheme();
    }

    @Override
    public long getConnectTimeoutMs() {
        return properties.getConnectTimeoutMs();
    }

    @Override
    public long getReadTimeoutMs() {
        return properties.getReadTimeoutMs();
    }

    @Override
    public long getWriteTimeoutMs() {
        return properties.getWriteTimeoutMs();
    }
}
