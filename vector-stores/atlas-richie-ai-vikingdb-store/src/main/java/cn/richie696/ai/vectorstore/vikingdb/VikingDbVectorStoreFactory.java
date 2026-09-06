package cn.richie696.ai.vectorstore.vikingdb;

import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbSearchAdvanceOptions;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbSearchCommonOptions;
import com.volcengine.vikingdb.VikingdbApi;
import com.volcengine.vikingdb.runtime.vector.service.VectorService;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.util.Assert;

/**
 * Creates multiple independently bound Store instances over shared clients.
 */
public final class VikingDbVectorStoreFactory {
    private final EmbeddingModel embeddingModel;
    private final VectorService dataPlane;
    private final VikingdbApi controlPlane;
    private final BatchingStrategy batchingStrategy;
    private final ObservationRegistry observationRegistry;
    private final VectorStoreObservationConvention observationConvention;

    public VikingDbVectorStoreFactory(EmbeddingModel embeddingModel, VectorService dataPlane,
                                      VikingdbApi controlPlane, BatchingStrategy batchingStrategy,
                                      ObservationRegistry observationRegistry,
                                      VectorStoreObservationConvention observationConvention) {
        Assert.notNull(embeddingModel, "embeddingModel must not be null");
        Assert.notNull(dataPlane, "dataPlane must not be null");
        this.embeddingModel = embeddingModel;
        this.dataPlane = dataPlane;
        this.controlPlane = controlPlane;
        Assert.notNull(batchingStrategy, "batchingStrategy must not be null");
        this.batchingStrategy = batchingStrategy;
        this.observationRegistry = observationRegistry == null ? ObservationRegistry.NOOP : observationRegistry;
        this.observationConvention = observationConvention;
    }

    public VikingDbVectorStore create(VikingDbStoreSpec spec) {
        Assert.notNull(spec, "store spec must not be null");
        VikingDbVectorStore.Builder builder = new VikingDbVectorStore.Builder(embeddingModel, dataPlane)
                .collectionName(spec.collectionName()).indexName(spec.indexName())
                .embeddingDimension(spec.embeddingDimension()).initializeSchema(spec.initializeSchema())
                .projectName(spec.projectName()).description(spec.description()).shardCount(spec.shardCount())
                .scalarIndex(spec.scalarIndex()).metadataFields(spec.metadataFields())
                .filterValidationMode(spec.filterValidationMode())
                .searchDefaults(spec.searchDefaults() == null ? VikingDbSearchCommonOptions.empty() : spec.searchDefaults())
                .searchAdvanceDefaults(spec.searchAdvanceDefaults() == null ? VikingDbSearchAdvanceOptions.empty() : spec.searchAdvanceDefaults())
                .indexVectorOptions(spec.indexVectorOptions())
                .batchingStrategy(batchingStrategy).observationRegistry(observationRegistry);
        if (controlPlane != null) builder.controlPlane(controlPlane);
        if (observationConvention != null) builder.customObservationConvention(observationConvention);
        return builder.build();
    }
}
