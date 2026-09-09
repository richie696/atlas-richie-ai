package cn.richie696.ai.vectorstore.tencentvectordb.autoconfigure;

import cn.richie696.ai.vectorstore.tencentvectordb.TencentVectorDbVectorStore;
import cn.richie696.ai.vectorstore.tencentvectordb.api.TencentVectorDbAiOperations;
import cn.richie696.ai.vectorstore.tencentvectordb.api.TencentVectorDbIndexOperations;
import cn.richie696.ai.vectorstore.tencentvectordb.api.TencentVectorDbSearchOperations;
import com.tencent.tcvectordb.client.VectorDBClient;
import com.tencent.tcvectordb.model.Collection;
import com.tencent.tcvectordb.model.param.collection.FieldType;
import com.tencent.tcvectordb.model.param.collection.FilterIndex;
import com.tencent.tcvectordb.model.param.collection.HNSWParams;
import com.tencent.tcvectordb.model.param.collection.IndexType;
import com.tencent.tcvectordb.model.param.collection.MetricType;
import com.tencent.tcvectordb.model.param.collection.VectorIndex;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TencentVectorDbVectorStoreAutoConfigurationTests {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TencentVectorDbVectorStoreAutoConfiguration.class))
            .withBean(EmbeddingModel.class, () -> mock(EmbeddingModel.class))
            .withBean(VectorDBClient.class, this::client)
            .withPropertyValues("spring.ai.vectorstore.type=tencent-vectordb");

    @Test
    void configuresStandardVectorStoreAndProviderExtensions() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(VectorStore.class);
            TencentVectorDbVectorStore store = context.getBean(TencentVectorDbVectorStore.class);
            assertThat(store).isInstanceOf(TencentVectorDbSearchOperations.class)
                    .isInstanceOf(TencentVectorDbIndexOperations.class)
                    .isInstanceOf(TencentVectorDbAiOperations.class);
            assertThat(store.getDatabaseName()).isEqualTo("spring_ai");
            assertThat(store.getCollectionName()).isEqualTo("vector_store");
        });
    }

    @Test
    void bindsCollectionAndIndexProperties() {
        contextRunner.withPropertyValues(
                        "spring.ai.vectorstore.tencent-vectordb.database-name=knowledge",
                        "spring.ai.vectorstore.tencent-vectordb.collection-name=documents")
                .run(context -> {
                    TencentVectorDbVectorStore store = context.getBean(TencentVectorDbVectorStore.class);
                    assertThat(store.getDatabaseName()).isEqualTo("knowledge");
                    assertThat(store.getCollectionName()).isEqualTo("documents");
                });
    }

    @Test
    void doesNotActivateForAnotherVectorStoreType() {
        contextRunner.withPropertyValues("spring.ai.vectorstore.type=dashvector")
                .run(context -> assertThat(context).doesNotHaveBean(TencentVectorDbVectorStore.class));
    }

    private VectorDBClient client() {
        VectorDBClient client = mock(VectorDBClient.class);
        Collection collection = mock(Collection.class);
        when(collection.getIndexes()).thenReturn(List.of(
                new FilterIndex("id", FieldType.String, IndexType.PRIMARY_KEY),
                new FilterIndex("content", FieldType.String, IndexType.FILTER),
                new VectorIndex("vector", 1536, IndexType.HNSW, MetricType.COSINE, new HNSWParams(16, 200))));
        when(client.describeCollection(anyString(), anyString())).thenReturn(collection);
        return client;
    }
}
