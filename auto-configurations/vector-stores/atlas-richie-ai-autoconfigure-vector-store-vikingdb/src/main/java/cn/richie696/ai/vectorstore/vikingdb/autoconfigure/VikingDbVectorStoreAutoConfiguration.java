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

import cn.richie696.ai.vectorstore.vikingdb.VikingDbStoreSpec;
import cn.richie696.ai.vectorstore.vikingdb.VikingDbVectorStore;
import cn.richie696.ai.vectorstore.vikingdb.VikingDbVectorStoreFactory;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbIndexVectorOptions;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbSearchAdvanceOptions;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbSearchCommonOptions;
import com.volcengine.ApiClient;
import com.volcengine.sign.Credentials;
import com.volcengine.vikingdb.VikingdbApi;
import com.volcengine.vikingdb.runtime.core.ClientConfig;
import com.volcengine.vikingdb.runtime.core.auth.Auth;
import com.volcengine.vikingdb.runtime.core.auth.AuthWithAkSk;
import com.volcengine.vikingdb.runtime.core.auth.AuthWithApiKey;
import com.volcengine.vikingdb.runtime.enums.Scheme;
import com.volcengine.vikingdb.runtime.vector.service.VectorService;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.SpringAIVectorStoreTypes;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
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
@ConditionalOnClass({VikingDbVectorStore.class, EmbeddingModel.class})
@ConditionalOnBean(EmbeddingModel.class)
@EnableConfigurationProperties({VikingDbClientProperties.class, VikingDbVectorStoreProperties.class})
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
    public VectorService vikingDbDataPlaneClient(VikingDbConnectionDetails connection) {
        log.info("VikingDB 数据面 SDK 初始化: scheme={}, host={}, region={}",
                connection.getScheme(), connection.getHost(), connection.getRegion());
        try {
            Auth auth = connection.getAuthenticationMode() == VikingDbAuthenticationMode.API_KEY
                    ? new AuthWithApiKey(connection.getApiKey())
                    : new AuthWithAkSk(connection.getAccessKey(), connection.getSecretKey());
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout((int) connection.getConnectTimeoutMs())
                    .setSocketTimeout((int) connection.getReadTimeoutMs())
                    .setConnectionRequestTimeout((int) connection.getReadTimeoutMs())
                    .build();
            return new VectorService(
                    Scheme.valueOf(connection.getScheme()), connection.getHost(), connection.getRegion(), auth,
                    ClientConfig.builder().httpClient(HttpClients.custom().setDefaultRequestConfig(requestConfig).build()).build());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "VikingDB 数据面 SDK 初始化失败: host=" + connection.getHost()
                            + ", region=" + connection.getRegion() + ", error=" + e.getMessage(), e);
        }
    }

    /**
     * VikingDB 控制面 SDK 入口 — 留给运维 / 索引管理 API（createCollection / listIndexes 等），
     * 不进入 VectorStore 主路径。
     *
     * <p>仅在配置中存在 AK/SK 时注册。API Key-only 数据面不具备控制面认证能力，
     * 因而不会因为不需要 schema 管理而在启动期失败；若同时启用
     * {@code initializeSchema=true}，Store 会明确提示需要控制面 client。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${spring.ai.vectorstore.vikingdb.client.access-key:}') && T(org.springframework.util.StringUtils).hasText('${spring.ai.vectorstore.vikingdb.client.secret-key:}')")
    public VikingdbApi vikingDbControlPlaneClient(VikingDbConnectionDetails connection) {
        log.info("VikingDB 控制面 SDK 初始化: endpoint={}, region={}",
                connection.getControlEndpoint(), connection.getRegion());
        ApiClient apiClient = new ApiClient()
                .setEndpoint(connection.getControlEndpoint())
                .setCredentials(Credentials.getCredentials(connection.getAccessKey(), connection.getSecretKey()))
                .setRegion(connection.getRegion())
                .setConnectTimeout((int) connection.getConnectTimeoutMs())
                .setReadTimeout((int) connection.getReadTimeoutMs())
                .setWriteTimeout((int) connection.getWriteTimeoutMs());
        return new VikingdbApi(apiClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public VikingDbVectorStoreFactory vikingDbVectorStoreFactory(
            EmbeddingModel embeddingModel, VectorService vikingDbDataPlaneClient,
            ObjectProvider<VikingdbApi> vikingDbControlPlaneClient,
            BatchingStrategy batchingStrategy,
            ObjectProvider<ObservationRegistry> observationRegistry,
            ObjectProvider<VectorStoreObservationConvention> customObservationConvention) {
        return new VikingDbVectorStoreFactory(embeddingModel, vikingDbDataPlaneClient,
                vikingDbControlPlaneClient.getIfAvailable(), batchingStrategy,
                observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP),
                customObservationConvention.getIfAvailable());
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
    public VikingDbVectorStore vectorStore(VikingDbVectorStoreFactory factory,
                                           VikingDbVectorStoreProperties properties) {
        VikingDbStoreSpec spec = VikingDbStoreSpec.builder()
                .collectionName(properties.getCollectionName()).indexName(properties.getIndexName())
                .embeddingDimension(properties.getEmbeddingDimension()).initializeSchema(properties.isInitializeSchema())
                .projectName(properties.getProjectName()).description(properties.getDescription())
                .shardCount(properties.getShardCount()).scalarIndex(properties.getScalarIndex())
                .metadataFields(properties.getMetadataFields())
                .filterValidationMode(properties.getFilterValidationMode())
                .searchDefaults(VikingDbSearchCommonOptions.builder()
                        .limit(properties.getSearchDefaults().getLimit())
                        .offset(properties.getSearchDefaults().getOffset())
                        .partition(properties.getSearchDefaults().getPartition()).build())
                .searchAdvanceDefaults(VikingDbSearchAdvanceOptions.builder()
                        .denseWeight(properties.getSearchDefaults().getDenseWeight())
                        .scaleK(properties.getSearchDefaults().getScaleK())
                        .filterPreAnnLimit(properties.getSearchDefaults().getFilterPreAnnLimit())
                        .filterPreAnnRatio(properties.getSearchDefaults().getFilterPreAnnRatio()).build())
                .indexVectorOptions(VikingDbIndexVectorOptions.builder()
                        .type(properties.getIndex().getType()).distance(properties.getIndex().getDistance())
                        .quantization(properties.getIndex().getQuantization())
                        .hnswM(properties.getIndex().getHnswM()).hnswCef(properties.getIndex().getHnswCef())
                        .hnswSef(properties.getIndex().getHnswSef()).diskannM(properties.getIndex().getDiskannM())
                        .diskannCef(properties.getIndex().getDiskannCef()).cacheRatio(properties.getIndex().getCacheRatio())
                        .pqCodeRatio(properties.getIndex().getPqCodeRatio()).build()).build();
        return factory.create(spec);
    }
}
