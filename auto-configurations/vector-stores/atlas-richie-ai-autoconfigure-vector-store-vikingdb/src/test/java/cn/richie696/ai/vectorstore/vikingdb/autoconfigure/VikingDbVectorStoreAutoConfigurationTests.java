package cn.richie696.ai.vectorstore.vikingdb.autoconfigure;

import cn.richie696.ai.vectorstore.vikingdb.VikingDbVectorStore;
import com.volcengine.vikingdb.VikingdbApi;
import com.volcengine.vikingdb.runtime.vector.service.VectorService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VikingDbVectorStoreAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VikingDbVectorStoreAutoConfiguration.class))
            .withBean(EmbeddingModel.class, () -> mock(EmbeddingModel.class))
            .withBean(VectorService.class, () -> mock(VectorService.class))
            .withBean(VikingdbApi.class, () -> mock(VikingdbApi.class))
            .withPropertyValues(
                    "spring.ai.vectorstore.type=vikingdb",
                    "spring.ai.vectorstore.vikingdb.client.access-key=test-ak",
                    "spring.ai.vectorstore.vikingdb.client.secret-key=test-sk");

    private final ApplicationContextRunner apiKeyContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VikingDbVectorStoreAutoConfiguration.class))
            .withBean(EmbeddingModel.class, () -> mock(EmbeddingModel.class))
            .withBean(VectorService.class, () -> mock(VectorService.class))
            .withPropertyValues(
                    "spring.ai.vectorstore.type=vikingdb",
                    "spring.ai.vectorstore.vikingdb.client.authentication-mode=api-key",
                    "spring.ai.vectorstore.vikingdb.client.api-key=test-api-key");

    @Test
    void configuresVectorStoreAndPropertiesConnectionDetails() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(VikingDbVectorStore.class);
            assertThat(context).hasSingleBean(VikingDbConnectionDetails.class);
        });
    }

    @Test
    void backsOffWhenApplicationProvidesConnectionDetails() {
        contextRunner.withBean(VikingDbConnectionDetails.class, () -> mock(VikingDbConnectionDetails.class))
                .run(context -> assertThat(context).hasSingleBean(VikingDbConnectionDetails.class));
    }

    @Test
    void bindsAdvancedIndexAndSearchDefaultsWithoutChangingStoreBinding() {
        contextRunner.withPropertyValues(
                        "spring.ai.vectorstore.vikingdb.index.type=HNSW_HYBRID",
                        "spring.ai.vectorstore.vikingdb.index.distance=IP",
                        "spring.ai.vectorstore.vikingdb.index.hnsw-m=32",
                        "spring.ai.vectorstore.vikingdb.search-defaults.limit=7",
                        "spring.ai.vectorstore.vikingdb.search-defaults.scale-k=2.0")
                .run(context -> {
                    VikingDbVectorStore store = context.getBean(VikingDbVectorStore.class);
                    assertThat(store.getIndexVectorOptions().type().name()).isEqualTo("HNSW_HYBRID");
                    assertThat(store.getIndexVectorOptions().distance().name()).isEqualTo("IP");
                    assertThat(store.getSearchDefaults().limit()).isEqualTo(7);
                    assertThat(store.getSearchAdvanceDefaults().scaleK()).isEqualTo(2.0);
                });
    }

    @Test
    void apiKeyDataPlaneDoesNotRequireControlPlaneCredentials() {
        apiKeyContextRunner.run(context -> {
            assertThat(context).hasSingleBean(VikingDbVectorStore.class);
            assertThat(context).doesNotHaveBean(VikingdbApi.class);
        });
    }
}
