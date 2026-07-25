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
}
