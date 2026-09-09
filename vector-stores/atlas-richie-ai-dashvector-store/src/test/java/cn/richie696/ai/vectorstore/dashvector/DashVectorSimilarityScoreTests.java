package cn.richie696.ai.vectorstore.dashvector;

import com.aliyun.dashvector.DashVectorClient;
import com.aliyun.dashvector.proto.CollectionInfo;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DashVectorSimilarityScoreTests {
    @Test
    void normalizesCosineDistanceToSpringAiSimilarity() {
        DashVectorVectorStore store = store(CollectionInfo.Metric.cosine);

        assertThat(store.similarityScore(0.0f)).isEqualTo(1.0);
        assertThat(store.similarityScore(1.0f)).isEqualTo(0.5);
        assertThat(store.similarityScore(2.0f)).isEqualTo(0.0);
    }

    @Test
    void normalizesEuclideanDistanceAndBoundsDotProduct() {
        assertThat(store(CollectionInfo.Metric.euclidean).similarityScore(3.0f)).isEqualTo(0.25);
        assertThat(store(CollectionInfo.Metric.dotproduct).similarityScore(2.0f)).isEqualTo(1.0);
        assertThat(store(CollectionInfo.Metric.dotproduct).similarityScore(-1.0f)).isEqualTo(0.0);
    }

    private DashVectorVectorStore store(CollectionInfo.Metric metric) {
        return new DashVectorVectorStore.Builder(mock(EmbeddingModel.class), mock(DashVectorClient.class))
                .metric(metric).build();
    }
}
