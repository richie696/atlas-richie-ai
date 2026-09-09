package cn.richie696.ai.vectorstore.tencentvectordb;

import com.tencent.tcvectordb.client.VectorDBClient;
import com.tencent.tcvectordb.model.param.collection.MetricType;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TencentVectorDbSimilarityScoreTests {
    @Test
    void normalizesDistanceMetricsToSpringAiSimilarity() {
        assertThat(store(MetricType.L2).similarityScore(0.0)).isEqualTo(1.0);
        assertThat(store(MetricType.L2).similarityScore(3.0)).isEqualTo(0.25);
        assertThat(store(MetricType.HAMMING).similarityScore(1.0)).isEqualTo(0.5);
    }

    @Test
    void boundsSimilarityMetricsToSpringAiRange() {
        assertThat(store(MetricType.COSINE).similarityScore(0.8)).isEqualTo(0.8);
        assertThat(store(MetricType.IP).similarityScore(2.0)).isEqualTo(1.0);
        assertThat(store(MetricType.IP).similarityScore(-1.0)).isEqualTo(0.0);
    }

    private TencentVectorDbVectorStore store(MetricType metricType) {
        return new TencentVectorDbVectorStore.Builder(mock(EmbeddingModel.class), mock(VectorDBClient.class))
                .metricType(metricType).build();
    }
}
