package cn.richie696.ai.vectorstore.dashvector.autoconfigure;

import cn.richie696.ai.vectorstore.dashvector.DashVectorStoreSpec;
import cn.richie696.ai.vectorstore.dashvector.DashVectorVectorStore;
import cn.richie696.ai.vectorstore.dashvector.DashVectorVectorStoreFactory;
import com.aliyun.dashvector.DashVectorClient;
import com.aliyun.dashvector.DashVectorClientConfig;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.SpringAIVectorStoreTypes;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;

import java.net.URI;

/** Spring Boot auto-configuration for the independent DashVector Spring AI plugin. */
@AutoConfiguration
@ConditionalOnClass({DashVectorVectorStore.class, DashVectorClient.class, EmbeddingModel.class})
@ConditionalOnBean(EmbeddingModel.class)
@ConditionalOnProperty(name = SpringAIVectorStoreTypes.TYPE, havingValue = "dashvector")
@EnableConfigurationProperties({DashVectorClientProperties.class, DashVectorVectorStoreProperties.class})
public class DashVectorVectorStoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DashVectorConnectionDetails.class)
    DashVectorConnectionDetails dashVectorConnectionDetails(DashVectorClientProperties properties) {
        return new DashVectorPropertiesConnectionDetails(properties);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    DashVectorClient dashVectorClient(DashVectorConnectionDetails connection) {
        Assert.hasText(connection.getEndpoint(), "spring.ai.vectorstore.dashvector.client.endpoint must not be blank");
        Assert.hasText(connection.getApiKey(), "spring.ai.vectorstore.dashvector.client.api-key must not be blank");
        return new DashVectorClient(DashVectorClientConfig.builder().endpoint(normalizeEndpoint(connection.getEndpoint()))
                .apiKey(connection.getApiKey()).timeout(connection.getTimeoutSeconds()).build());
    }

    static String normalizeEndpoint(String endpoint) {
        String value = endpoint.trim();
        if (!value.contains("://")) return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
        URI uri = URI.create(value);
        Assert.isTrue(("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                        && uri.getHost() != null && (uri.getPath().isEmpty() || "/".equals(uri.getPath()))
                        && uri.getQuery() == null && uri.getFragment() == null,
                "spring.ai.vectorstore.dashvector.client.endpoint must be a DashVector host or root URL");
        return uri.getHost();
    }

    @Bean
    @ConditionalOnMissingBean(BatchingStrategy.class)
    BatchingStrategy dashVectorBatchingStrategy() { return new TokenCountBatchingStrategy(); }

    @Bean
    @ConditionalOnMissingBean
    DashVectorVectorStoreFactory dashVectorVectorStoreFactory(
            EmbeddingModel embeddingModel, DashVectorClient client, BatchingStrategy batchingStrategy,
            ObjectProvider<ObservationRegistry> observationRegistry,
            ObjectProvider<VectorStoreObservationConvention> observationConvention) {
        return new DashVectorVectorStoreFactory(embeddingModel, client, batchingStrategy,
                observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP),
                observationConvention.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    DashVectorVectorStore vectorStore(DashVectorVectorStoreFactory factory,
                                      DashVectorVectorStoreProperties properties) {
        return factory.create(DashVectorStoreSpec.builder()
                .collectionName(properties.getCollectionName())
                .embeddingDimension(properties.getEmbeddingDimension())
                .initializeSchema(properties.isInitializeSchema())
                .schemaInitializationTimeoutSeconds(properties.getSchemaInitializationTimeoutSeconds())
                .partition(properties.getPartition()).metric(properties.getMetric())
                .metadataFields(properties.getMetadataFields()).build());
    }
}
