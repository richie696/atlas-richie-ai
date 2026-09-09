package cn.richie696.ai.vectorstore.dashvector;

import com.aliyun.dashvector.DashVectorClient;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.util.Assert;

/** Creates independently collection-bound DashVector stores over one shared SDK client. */
public final class DashVectorVectorStoreFactory {
    private final EmbeddingModel embeddingModel;
    private final DashVectorClient client;
    private final BatchingStrategy batchingStrategy;
    private final ObservationRegistry observationRegistry;
    private final VectorStoreObservationConvention observationConvention;

    public DashVectorVectorStoreFactory(EmbeddingModel embeddingModel, DashVectorClient client,
                                        BatchingStrategy batchingStrategy,
                                        ObservationRegistry observationRegistry,
                                        VectorStoreObservationConvention observationConvention) {
        Assert.notNull(embeddingModel, "embeddingModel must not be null");
        Assert.notNull(client, "client must not be null");
        Assert.notNull(batchingStrategy, "batchingStrategy must not be null");
        this.embeddingModel = embeddingModel;
        this.client = client;
        this.batchingStrategy = batchingStrategy;
        this.observationRegistry = observationRegistry == null ? ObservationRegistry.NOOP : observationRegistry;
        this.observationConvention = observationConvention;
    }

    public DashVectorVectorStore create(DashVectorStoreSpec spec) {
        Assert.notNull(spec, "store spec must not be null");
        DashVectorVectorStore.Builder builder = new DashVectorVectorStore.Builder(embeddingModel, client)
                .collectionName(spec.collectionName()).embeddingDimension(spec.embeddingDimension())
                .initializeSchema(spec.initializeSchema())
                .schemaInitializationTimeoutSeconds(spec.schemaInitializationTimeoutSeconds()).partition(spec.partition())
                .metric(spec.metric()).metadataFields(spec.metadataFields())
                .batchingStrategy(batchingStrategy).observationRegistry(observationRegistry);
        if (observationConvention != null) builder.customObservationConvention(observationConvention);
        return builder.build();
    }
}
