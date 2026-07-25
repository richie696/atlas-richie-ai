/*
 * Copyright (c) 2026 Richie (https://www.github.com/richie696)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.richie696.ai.vectorstore.vikingdb.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * VikingDB 向量库连接配置（host / region / ak / sk / scheme / 超时）。
 * <p>对应 VikingDB 数据面 SDK 构造参数 — 数据面与控制面共用同一组 IAM 凭证。</p>
 *
 * <p>配置前缀：{@code spring.ai.vectorstore.vikingdb.client.*}</p>
 *
 * @author richie696
 * @since 0.1.0
 */
@Data
@ConfigurationProperties(VikingDbClientProperties.CONFIG_PREFIX)
public class VikingDbClientProperties {

    public static final String CONFIG_PREFIX = "spring.ai.vectorstore.vikingdb.client";

    /** 数据面 host（不含 scheme），例如 {@code api-vikingdb.vikingdb.cn-beijing.volces.com} */
    private String host;

    /** 控制面 endpoint（不带 scheme），例如 {@code vikingdb.cn-beijing.volcengineapi.com} */
    private String controlEndpoint;

    /** 区域，例如 {@code cn-beijing}。数据面 + 控制面共用 */
    private String region;

    /** 访问 AK（IAM Access Key） */
    private String accessKey;

    /** 访问 SK（IAM Secret Key） */
    private String secretKey;

    /** Scheme（默认 HTTPS） */
    private String scheme = "HTTPS";

    /** 连接超时（毫秒） */
    private long connectTimeoutMs = 10_000L;

    /** 读超时（毫秒） */
    private long readTimeoutMs = 30_000L;

    public void setAccessKey(String accessKey) {
        Assert.hasText(accessKey, "VikingDB access key must not be blank");
        this.accessKey = accessKey;
    }

    public void setSecretKey(String secretKey) {
        Assert.hasText(secretKey, "VikingDB secret key must not be blank");
        this.secretKey = secretKey;
    }
}
