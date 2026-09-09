package cn.richie696.ai.vectorstore.tencentvectordb.autoconfigure;

import cn.richie696.ai.vectorstore.tencentvectordb.TencentVectorDbStoreSpec;
import cn.richie696.ai.vectorstore.tencentvectordb.TencentVectorDbVectorStore;
import cn.richie696.ai.vectorstore.tencentvectordb.TencentVectorDbVectorStoreFactory;
import com.tencent.tcvectordb.client.VectorDBClient;
import com.tencent.tcvectordb.model.param.database.ConnectParam;
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

/** Spring Boot auto-configuration for the independent Tencent VectorDB Spring AI plugin. */
@AutoConfiguration
@ConditionalOnClass({TencentVectorDbVectorStore.class, VectorDBClient.class, EmbeddingModel.class})
@ConditionalOnBean(EmbeddingModel.class)
@ConditionalOnProperty(name = SpringAIVectorStoreTypes.TYPE, havingValue = "tencent-vectordb")
@EnableConfigurationProperties({TencentVectorDbClientProperties.class, TencentVectorDbVectorStoreProperties.class})
public class TencentVectorDbVectorStoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TencentVectorDbConnectionDetails.class)
    TencentVectorDbConnectionDetails tencentVectorDbConnectionDetails(TencentVectorDbClientProperties properties) {
        return new TencentVectorDbPropertiesConnectionDetails(properties);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    VectorDBClient tencentVectorDbClient(TencentVectorDbConnectionDetails connection) {
        Assert.hasText(connection.getUrl(), "spring.ai.vectorstore.tencent-vectordb.client.url must not be blank");
        Assert.hasText(connection.getUsername(), "spring.ai.vectorstore.tencent-vectordb.client.username must not be blank");
        Assert.hasText(connection.getApiKey(), "spring.ai.vectorstore.tencent-vectordb.client.api-key must not be blank");
        ConnectParam connectParam = ConnectParam.newBuilder().withUrl(connection.getUrl())
                .withUsername(connection.getUsername()).withKey(connection.getApiKey())
                .withTimeout(connection.getTimeoutSeconds())
                .withConnectTimeout(connection.getConnectTimeoutSeconds())
                .withMaxIdleConnections(connection.getMaxIdleConnections())
                .withKeepAliveDuration(connection.getKeepAliveDurationSeconds()).build();
        return new VectorDBClient(connectParam, connection.getReadConsistency());
    }

    @Bean
    @ConditionalOnMissingBean(BatchingStrategy.class)
    BatchingStrategy tencentVectorDbBatchingStrategy() {
        return new TokenCountBatchingStrategy();
    }

    @Bean
    @ConditionalOnMissingBean
    TencentVectorDbVectorStoreFactory tencentVectorDbVectorStoreFactory(
            EmbeddingModel embeddingModel, VectorDBClient client, BatchingStrategy batchingStrategy,
            ObjectProvider<ObservationRegistry> observationRegistry,
            ObjectProvider<VectorStoreObservationConvention> observationConvention) {
        return new TencentVectorDbVectorStoreFactory(embeddingModel, client, batchingStrategy,
                observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP),
                observationConvention.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    TencentVectorDbVectorStore vectorStore(TencentVectorDbVectorStoreFactory factory,
                                           TencentVectorDbVectorStoreProperties properties) {
        return factory.create(TencentVectorDbStoreSpec.builder()
                .databaseName(properties.getDatabaseName()).collectionName(properties.getCollectionName())
                .embeddingDimension(properties.getEmbeddingDimension())
                .initializeSchema(properties.isInitializeSchema())
                .shardNum(properties.getShardNum()).replicaNum(properties.getReplicaNum())
                .description(properties.getDescription()).indexType(properties.getIndexType())
                .metricType(properties.getMetricType()).build());
    }
}
