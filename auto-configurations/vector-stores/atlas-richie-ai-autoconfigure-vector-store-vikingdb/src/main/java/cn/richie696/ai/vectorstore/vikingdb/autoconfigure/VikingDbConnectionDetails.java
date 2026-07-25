package cn.richie696.ai.vectorstore.vikingdb.autoconfigure;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * VikingDB 客户端连接契约，支持属性绑定或外部服务绑定。
 *
 * <p>默认由 {@code VikingDbClientProperties} 实现，也可由外部 Spring Boot
 * service connection（如 {@code spring-cloud-kubernetes-service-bindings}）实现。
 * 数据面与控制面 SDK 共享同一组凭证，但使用不同的 endpoint。</p>
 */
public interface VikingDbConnectionDetails extends ConnectionDetails {

    /**
     * @return 数据面 host（不含 scheme），例如
     *         {@code api-vikingdb.vikingdb.cn-beijing.volces.com}。
     */
    String getHost();

    /**
     * @return 控制面 endpoint（不含 scheme），例如
     *         {@code vikingdb.cn-beijing.volcengineapi.com}。与数据面 host 不同。
     */
    String getControlEndpoint();

    /**
     * @return 火山引擎区域，例如 {@code cn-beijing}。两个 SDK 共用；region 与 endpoint
     *         不匹配会触发签名拒绝。
     */
    String getRegion();

    /** @return 用于请求签名的 IAM Access Key。 */
    String getAccessKey();

    /** @return 用于请求签名的 IAM Secret Key。须与 {@link #getAccessKey()} 配对。 */
    String getSecretKey();

    /**
     * @return 传输协议名称（{@code "HTTPS"} 或 {@code "HTTP"}）。
     *         VikingDB SDK 将其暴露为 {@code Scheme} 枚举，未知值会导致 SDK 构造器抛出异常。
     */
    String getScheme();
}