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

import cn.richie696.ai.vectorstore.vikingdb.VikingDbVectorStore;
import com.volcengine.ApiClient;
import com.volcengine.sign.Credentials;
import com.volcengine.vikingdb.VikingdbApi;
import com.volcengine.vikingdb.runtime.core.ClientConfig;
import com.volcengine.vikingdb.runtime.core.auth.AuthWithAkSk;
import com.volcengine.vikingdb.runtime.enums.Scheme;
import com.volcengine.vikingdb.runtime.vector.service.VectorService;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.SpringAIVectorStoreTypes;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * VikingDB VectorStore Spring Boot 自动装配。
 *
 * <p>提供 4 个 Bean：
 * <ul>
 *   <li>{@code vikingDbDataPlaneClient} — 数据面 {@link VectorService}（运行时读写 / 检索）</li>
 *   <li>{@code vikingDbControlPlaneClient} — 控制面 {@link VikingdbApi}（Collection / Index / Task 管理）</li>
 *   <li>{@code vectorStore} — Spring AI 标准 {@link VikingDbVectorStore} Bean</li>
 *   <li>{@code batchingStrategy} — 默认 {@link TokenCountBatchingStrategy}</li>
 * </ul>
 *
 * <p>触发条件：{@code spring.ai.vectorstore.type=vikingdb}（与 Spring AI 官方 starter 一致）
 * + classpath 含 {@code VikingDbVectorStore} + {@code EmbeddingModel}。</p>
 *
 * @author richie696
 * @since 0.1.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({ VikingDbVectorStore.class, EmbeddingModel.class })
@ConditionalOnBean(EmbeddingModel.class)
@EnableConfigurationProperties({ VikingDbClientProperties.class, VikingDbVectorStoreProperties.class })
@ConditionalOnProperty(name = SpringAIVectorStoreTypes.TYPE, havingValue = "vikingdb")
public class VikingDbVectorStoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(VikingDbConnectionDetails.class)
    VikingDbPropertiesConnectionDetails vikingDbConnectionDetails(VikingDbClientProperties properties) {
        return new VikingDbPropertiesConnectionDetails(properties);
    }

@Bean
    @ConditionalOnMissingBean
    public BatchingStrategy vikingDbBatchingStrategy() {
        return new TokenCountBatchingStrategy();
    }

    /**
     * VikingDB 数据面 SDK 入口 — 供 {@link VikingDbVectorStore} 运行时调用。
     * <p>对应 {@code spring.ai.vectorstore.vikingdb.client.host} 配置项。</p>
     *
     * <p>{@link VectorService} 在 Bean 创建时即构造，以便错误配置（错误的 scheme、格式异常的
     * endpoint）在启动期 fail-fast，而不是等到首次 upsert 调用时才暴露。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public VectorService vikingDbDataPlaneClient(VikingDbClientProperties properties) {
        log.info("VikingDB 数据面 SDK 初始化: scheme={}, host={}, region={}",
                properties.getScheme(), properties.getHost(), properties.getRegion());
        try {
            return new VectorService(
                    Scheme.valueOf(properties.getScheme()),
                    properties.getHost(),
                    properties.getRegion(),
                    new AuthWithAkSk(properties.getAccessKey(), properties.getSecretKey()),
                    ClientConfig.builder().build());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "VikingDB 数据面 SDK 初始化失败: host=" + properties.getHost()
                            + ", region=" + properties.getRegion() + ", error=" + e.getMessage(), e);
        }
    }

    /**
     * VikingDB 控制面 SDK 入口 — 留给运维 / 索引管理 API（createCollection / listIndexes 等），
     * 不进入 VectorStore 主路径。
     *
     * <p>始终注册，使 {@link VikingDbVectorStore} 能通过 {@link ObjectProvider} 在
     * {@code initializeSchema} 时解析它；store 将该 Bean 视为可选，缺失时降级为告警日志。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public VikingdbApi vikingDbControlPlaneClient(VikingDbClientProperties properties) {
        log.info("VikingDB 控制面 SDK 初始化: endpoint={}, region={}",
                properties.getControlEndpoint(), properties.getRegion());
        ApiClient apiClient = new ApiClient()
                .setEndpoint(properties.getControlEndpoint())
                .setCredentials(Credentials.getCredentials(properties.getAccessKey(), properties.getSecretKey()))
                .setRegion(properties.getRegion());
        return new VikingdbApi(apiClient);
    }

    /**
     * 构造装配了数据面 client、应用 {@link EmbeddingModel} 以及用户提供的
     * {@link ObservationRegistry} / {@link VectorStoreObservationConvention} 的
     * {@link VikingDbVectorStore} Bean。
     *
     * <p>属性逐字段透传；{@code null} 值会被跳过，以保留 Builder 默认值。仅当控制面
     * client 存在于上下文时才附加，保证未注册控制面 SDK 的环境也能正常工作。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public VikingDbVectorStore vectorStore(VectorService vikingDbDataPlaneClient,
                                           EmbeddingModel embeddingModel,
                                           VikingDbVectorStoreProperties properties,
                                           BatchingStrategy batchingStrategy,
                                           ObjectProvider<ObservationRegistry> observationRegistry,
                                           ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
                                           ObjectProvider<VikingdbApi> vikingDbControlPlaneClient) {
        VikingDbVectorStore.Builder builder = new VikingDbVectorStore.Builder(embeddingModel, vikingDbDataPlaneClient)
                .collectionName(properties.getCollectionName())
                .indexName(properties.getIndexName())
                .embeddingDimension(properties.getEmbeddingDimension())
                .initializeSchema(properties.isInitializeSchema())
                .batchingStrategy(batchingStrategy)
                .metadataFields(properties.getMetadataFields());
        if (properties.getProjectName() != null) {
            builder.projectName(properties.getProjectName());
        }
        if (properties.getDescription() != null) {
            builder.description(properties.getDescription());
        }
        if (properties.getShardCount() != null) {
            builder.shardCount(properties.getShardCount());
        }
        if (properties.getScalarIndex() != null && !properties.getScalarIndex().isEmpty()) {
            builder.scalarIndex(properties.getScalarIndex());
        }
        VikingdbApi controlPlane = vikingDbControlPlaneClient.getIfAvailable();
        if (controlPlane != null) {
            builder.controlPlane(controlPlane);
        }
        ObservationRegistry registry = observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP);
        builder.observationRegistry(registry);
        VectorStoreObservationConvention convention = customObservationConvention.getIfAvailable();
        if (convention != null) {
            builder.customObservationConvention(convention);
        }
        return builder.build();
    }
}
