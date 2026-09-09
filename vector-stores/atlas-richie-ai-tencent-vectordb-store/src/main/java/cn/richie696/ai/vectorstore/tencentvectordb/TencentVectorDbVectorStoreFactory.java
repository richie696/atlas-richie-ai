package cn.richie696.ai.vectorstore.tencentvectordb;

import com.tencent.tcvectordb.client.VectorDBClient;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.util.Assert;

/** Creates independently resource-bound Tencent VectorDB stores over one HTTP SDK client. */
public final class TencentVectorDbVectorStoreFactory {
    private final EmbeddingModel embeddingModel;
    private final VectorDBClient client;
    private final BatchingStrategy batchingStrategy;
    private final ObservationRegistry observationRegistry;
    private final VectorStoreObservationConvention observationConvention;
    public TencentVectorDbVectorStoreFactory(EmbeddingModel embeddingModel, VectorDBClient client,
                                             BatchingStrategy batchingStrategy,
                                             ObservationRegistry observationRegistry,
                                             VectorStoreObservationConvention observationConvention) {
        Assert.notNull(embeddingModel, "embeddingModel must not be null");
        Assert.notNull(client, "client must not be null");
        Assert.notNull(batchingStrategy, "batchingStrategy must not be null");
        this.embeddingModel = embeddingModel; this.client = client; this.batchingStrategy = batchingStrategy;
        this.observationRegistry = observationRegistry == null ? ObservationRegistry.NOOP : observationRegistry;
        this.observationConvention = observationConvention;
    }
    public TencentVectorDbVectorStore create(TencentVectorDbStoreSpec spec) {
        Assert.notNull(spec, "store spec must not be null");
        TencentVectorDbVectorStore.Builder builder = new TencentVectorDbVectorStore.Builder(embeddingModel, client)
                .databaseName(spec.databaseName()).collectionName(spec.collectionName())
                .embeddingDimension(spec.embeddingDimension()).initializeSchema(spec.initializeSchema())
                .shardNum(spec.shardNum()).replicaNum(spec.replicaNum()).description(spec.description())
                .indexType(spec.indexType()).metricType(spec.metricType())
                .batchingStrategy(batchingStrategy).observationRegistry(observationRegistry);
        if (observationConvention != null) builder.customObservationConvention(observationConvention);
        return builder.build();
    }
}
