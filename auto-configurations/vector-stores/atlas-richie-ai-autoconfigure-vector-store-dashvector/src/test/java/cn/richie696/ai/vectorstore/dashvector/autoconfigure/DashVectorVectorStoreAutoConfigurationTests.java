package cn.richie696.ai.vectorstore.dashvector.autoconfigure;

import cn.richie696.ai.vectorstore.dashvector.DashVectorVectorStore;
import cn.richie696.ai.vectorstore.dashvector.api.DashVectorPartitionOperations;
import cn.richie696.ai.vectorstore.dashvector.api.DashVectorSearchOperations;
import com.aliyun.dashvector.DashVectorClient;
import com.aliyun.dashvector.DashVectorCollection;
import com.aliyun.dashvector.common.ErrorCode;
import com.aliyun.dashvector.models.CollectionMeta;
import com.aliyun.dashvector.models.responses.Response;
import com.aliyun.dashvector.proto.CollectionInfo;
import com.aliyun.dashvector.proto.FieldType;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashVectorVectorStoreAutoConfigurationTests {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DashVectorVectorStoreAutoConfiguration.class))
            .withBean(EmbeddingModel.class, () -> mock(EmbeddingModel.class))
            .withBean(DashVectorClient.class, this::client)
            .withPropertyValues("spring.ai.vectorstore.type=dashvector");

    @Test
    void configuresStandardVectorStoreAndProviderExtensions() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(VectorStore.class);
            DashVectorVectorStore store = context.getBean(DashVectorVectorStore.class);
            assertThat(store).isInstanceOf(DashVectorSearchOperations.class)
                    .isInstanceOf(DashVectorPartitionOperations.class);
            assertThat(store.getCollectionName()).isEqualTo("vector_store");
        });
    }

    @Test
    void doesNotActivateForAnotherVectorStoreType() {
        contextRunner.withPropertyValues("spring.ai.vectorstore.type=tencent-vectordb")
                .run(context -> assertThat(context).doesNotHaveBean(DashVectorVectorStore.class));
    }

    @Test
    void normalizesConsoleStyleEndpointUrlForTheSdk() {
        assertThat(DashVectorVectorStoreAutoConfiguration.normalizeEndpoint(
                "https://example.dashvector.cn-hangzhou.aliyuncs.com/"))
                .isEqualTo("example.dashvector.cn-hangzhou.aliyuncs.com");
    }

    private DashVectorClient client() {
        DashVectorClient client = mock(DashVectorClient.class);
        DashVectorCollection collection = mock(DashVectorCollection.class);
        CollectionInfo info = CollectionInfo.newBuilder().setName("vector_store").setDimension(1536)
                .setMetric(CollectionInfo.Metric.cosine).putFieldsSchema("content", FieldType.STRING).build();
        when(client.describe("vector_store")).thenReturn(Response.create(
                ErrorCode.SUCCESS.getCode(), "success", "request-id", new CollectionMeta(info)));
        when(client.get("vector_store")).thenReturn(collection);
        when(collection.isSuccess()).thenReturn(true);
        return client;
    }
}
