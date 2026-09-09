package cn.richie696.ai.vectorstore.dashvector;

import com.aliyun.dashvector.DashVectorClient;
import com.aliyun.dashvector.DashVectorClientConfig;
import com.aliyun.dashvector.common.ErrorCode;
import com.aliyun.dashvector.models.Doc;
import com.aliyun.dashvector.models.requests.FetchDocRequest;
import com.aliyun.dashvector.models.responses.Response;
import com.aliyun.dashvector.proto.FieldType;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Opt-in lifecycle acceptance against a real DashVector cluster.
 * Credentials are read only from the process environment and are never persisted.
 */
class DashVectorNativeLiveIT {
    private static final Duration VISIBILITY_TIMEOUT = Duration.ofSeconds(60);

    @Test
    void standardVectorStoreCompletesCreateUpsertFilteredSearchFetchDeleteLifecycle() throws Exception {
        String endpoint = requiredEnvironment("DASHVECTOR_ENDPOINT");
        String apiKey = requiredEnvironment("DASHVECTOR_API_KEY");
        String collectionName = "spring_ai_it_" + System.currentTimeMillis();
        DashVectorClient client = new DashVectorClient(DashVectorClientConfig.builder()
                .endpoint(endpoint.replaceFirst("^https?://", "").replaceFirst("/$", ""))
                .apiKey(apiKey).timeout(15.0f).build());
        try {
            DashVectorVectorStore store = new DashVectorVectorStore.Builder(new DeterministicEmbeddingModel(), client)
                    .collectionName(collectionName).embeddingDimension(4).initializeSchema(true)
                    .schemaInitializationTimeoutSeconds(120).metadataFields(Map.of("tenant", FieldType.STRING))
                    .batchingStrategy(new TokenCountBatchingStrategy()).build();
            store.afterPropertiesSet();

            store.add(List.of(
                    Document.builder().id("doc-alpha").text("alpha knowledge")
                            .metadata(Map.of("tenant", "acceptance")).build(),
                    Document.builder().id("doc-beta").text("beta knowledge")
                            .metadata(Map.of("tenant", "other")).build()));

            List<Document> hits = awaitSearch(store, SearchRequest.builder().query("alpha")
                    .topK(2).similarityThreshold(0.9)
                    .filterExpression(new Filter.Expression(Filter.ExpressionType.EQ,
                            new Filter.Key("tenant"), new Filter.Value("acceptance"))).build());
            assertThat(hits).extracting(Document::getId).containsExactly("doc-alpha");
            assertThat(hits.get(0).getText()).isEqualTo("alpha knowledge");

            Response<Map<String, Doc>> fetched = store.fetch(FetchDocRequest.builder().id("doc-alpha").build());
            assertThat(fetched.isSuccess()).isTrue();
            assertThat(fetched.getOutput()).containsKey("doc-alpha");
            assertThat(store.collectionStats().isSuccess()).isTrue();

            store.delete(List.of("doc-alpha"));
            awaitDeleted(store, "doc-alpha");
        } finally {
            try {
                Response<Void> cleanup = client.delete(collectionName);
                assertThat(cleanup.isSuccess() || cleanup.getCode() == ErrorCode.INEXISTENT_COLLECTION.getCode())
                        .as("temporary DashVector collection cleanup").isTrue();
                awaitCollectionDeleted(client, collectionName);
            } finally {
                client.close();
            }
        }
    }

    private List<Document> awaitSearch(DashVectorVectorStore store, SearchRequest request) throws InterruptedException {
        long deadline = System.nanoTime() + VISIBILITY_TIMEOUT.toNanos();
        List<Document> result = List.of();
        while (System.nanoTime() < deadline) {
            result = store.similaritySearch(request);
            if (!result.isEmpty()) return result;
            Thread.sleep(1_000);
        }
        return result;
    }

    private void awaitDeleted(DashVectorVectorStore store, String id) throws InterruptedException {
        long deadline = System.nanoTime() + VISIBILITY_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            Response<Map<String, Doc>> response = store.fetch(FetchDocRequest.builder().id(id).build());
            if (response.isSuccess() && !response.getOutput().containsKey(id)) return;
            Thread.sleep(1_000);
        }
        throw new AssertionError("DashVector document remained visible after delete timeout");
    }

    private void awaitCollectionDeleted(DashVectorClient client, String collectionName) throws InterruptedException {
        long deadline = System.nanoTime() + VISIBILITY_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (client.describe(collectionName).getCode() == ErrorCode.INEXISTENT_COLLECTION.getCode()) return;
            Thread.sleep(1_000);
        }
        throw new AssertionError("Temporary DashVector collection remained visible after cleanup timeout");
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " must be set");
        return value;
    }

    private static final class DeterministicEmbeddingModel implements EmbeddingModel {
        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = request.getInstructions().stream()
                    .map(DeterministicEmbeddingModel::vector)
                    .map(value -> new Embedding(value, 0)).toList();
            return new EmbeddingResponse(embeddings);
        }

        @Override public float[] embed(Document document) { return vector(document.getText()); }
        @Override public int dimensions() { return 4; }

        private static float[] vector(String text) {
            return text != null && text.toLowerCase().contains("alpha")
                    ? new float[]{1.0f, 0.0f, 0.0f, 0.0f}
                    : new float[]{0.0f, 1.0f, 0.0f, 0.0f};
        }
    }
}
