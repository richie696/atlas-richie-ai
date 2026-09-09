package cn.richie696.ai.vectorstore.dashvector;

import com.aliyun.dashvector.DashVectorClient;
import com.aliyun.dashvector.DashVectorClientConfig;
import com.aliyun.dashvector.common.ErrorCode;
import com.aliyun.dashvector.models.Doc;
import com.aliyun.dashvector.models.DocOpResult;
import com.aliyun.dashvector.models.Group;
import com.aliyun.dashvector.models.OrderByField;
import com.aliyun.dashvector.models.RrfRanker;
import com.aliyun.dashvector.models.SparseVector;
import com.aliyun.dashvector.models.SparseVectorQuery;
import com.aliyun.dashvector.models.Vector;
import com.aliyun.dashvector.models.VectorParam;
import com.aliyun.dashvector.models.VectorQuery;
import com.aliyun.dashvector.models.WeightedRanker;
import com.aliyun.dashvector.models.requests.CreateCollectionRequest;
import com.aliyun.dashvector.models.requests.DeleteDocRequest;
import com.aliyun.dashvector.models.requests.FetchDocRequest;
import com.aliyun.dashvector.models.requests.InsertDocRequest;
import com.aliyun.dashvector.models.requests.QueryDocGroupByRequest;
import com.aliyun.dashvector.models.requests.QueryDocRequest;
import com.aliyun.dashvector.models.requests.UpdateDocRequest;
import com.aliyun.dashvector.models.responses.Response;
import com.aliyun.dashvector.proto.CollectionInfo;
import com.aliyun.dashvector.proto.FieldType;
import com.aliyun.dashvector.proto.Status;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Opt-in real-cloud acceptance for DashVector provider-specific APIs.
 * Credentials are read only from the process environment and are never persisted.
 */
class DashVectorAdvancedNativeLiveIT {
    private static final Duration VISIBILITY_TIMEOUT = Duration.ofSeconds(60);
    private static final List<Float> ALPHA = List.of(1.0f, 0.0f, 0.0f, 0.0f);
    private static final List<Float> BETA = List.of(0.0f, 1.0f, 0.0f, 0.0f);

    @Test
    void advancedApisCompleteRealCloudLifecycle() throws Exception {
        String endpoint = requiredEnvironment("DASHVECTOR_ENDPOINT");
        String apiKey = requiredEnvironment("DASHVECTOR_API_KEY");
        long suffix = System.currentTimeMillis();
        String standardCollection = "dv_adv_std_" + suffix;
        String multiCollection = "dv_adv_multi_" + suffix;
        List<String> createdCollections = new ArrayList<>();
        DashVectorClient client = new DashVectorClient(DashVectorClientConfig.builder()
                .endpoint(endpoint.replaceFirst("^https?://", "").replaceFirst("/$", ""))
                .apiKey(apiKey).timeout(15.0f).build());
        try {
            createdCollections.add(standardCollection);
            DashVectorVectorStore store = new DashVectorVectorStore.Builder(new DeterministicEmbeddingModel(), client)
                    .collectionName(standardCollection).embeddingDimension(4).initializeSchema(true)
                    .schemaInitializationTimeoutSeconds(120).metric(CollectionInfo.Metric.dotproduct)
                    .metadataFields(Map.of(
                            "tenant", FieldType.STRING,
                            "group_key", FieldType.STRING,
                            "rank_value", FieldType.INT))
                    .batchingStrategy(new TokenCountBatchingStrategy()).build();
            store.afterPropertiesSet();

            verifyDocumentSearchGroupByAndPartitionApis(store, standardCollection);
            createdCollections.add(multiCollection);
            verifyMultiVectorAndFusionApis(store, multiCollection);

            assertThat(assertSuccess(store.listCollections(), "listCollections").getOutput())
                    .contains(standardCollection, multiCollection);
            assertSuccess(store.describeCollection(standardCollection), "describeStandardCollection");
            assertSuccess(store.describeCollection(multiCollection), "describeMultiCollection");
            assertSuccess(store.deleteCollection(multiCollection), "deleteMultiCollection");
            awaitCollectionDeleted(store, multiCollection);
            assertSuccess(store.deleteCollection(standardCollection), "deleteStandardCollection");
            awaitCollectionDeleted(store, standardCollection);
        } finally {
            try {
                for (int i = createdCollections.size() - 1; i >= 0; i--) {
                    String name = createdCollections.get(i);
                    if (client.describe(name).getCode() == ErrorCode.INEXISTENT_COLLECTION.getCode()) {
                        continue;
                    }
                    Response<Void> cleanup = client.delete(name);
                    assertThat(cleanup.isSuccess())
                            .as("temporary DashVector collection cleanup: %s", name).isTrue();
                    awaitCollectionDeleted(client, name);
                }
            } finally {
                client.close();
            }
        }
    }

    private void verifyDocumentSearchGroupByAndPartitionApis(DashVectorVectorStore store,
                                                               String collectionName) throws Exception {
        Response<List<DocOpResult>> inserted = store.insert(InsertDocRequest.builder().docs(List.of(
                doc("alpha-1", ALPHA, Map.of("content", "alpha one", "tenant", "keep",
                        "group_key", "group-a", "rank_value", 1), Map.of(1L, 1.0f)),
                doc("alpha-2", List.of(0.9f, 0.1f, 0.0f, 0.0f), Map.of("content", "alpha two",
                        "tenant", "delete", "group_key", "group-a", "rank_value", 2), Map.of(2L, 1.0f)),
                doc("beta-1", BETA, Map.of("content", "beta one", "tenant", "keep",
                        "group_key", "group-b", "rank_value", 3), Map.of(2L, 1.0f))))
                .build());
        assertWriteSuccess(inserted, "insert");
        awaitFetchCount(store, collectionName, List.of("alpha-1", "alpha-2", "beta-1"), "default", 3);

        Response<List<DocOpResult>> updated = store.update(UpdateDocRequest.builder()
                .doc(Doc.builder().id("alpha-1")
                        .fields(Map.of("content", "alpha one updated", "tenant", "keep",
                                "group_key", "group-a", "rank_value", 9)).build())
                .build());
        assertWriteSuccess(updated, "update");
        awaitFieldValue(store, collectionName, "alpha-1", "content", "alpha one updated");

        Response<List<Doc>> advanced = awaitQuery(store, collectionName, QueryDocRequest.builder()
                .vectorQuery(VectorQuery.builder().vector(vector(ALPHA)).numCandidates(3)
                        .ef(64).linear(false).radius(0.5f).build())
                .topk(3).includeVector(true).outputFields(List.of("content", "rank_value"))
                .orderByField(OrderByField.builder().field("rank_value").desc(true).build())
                .build());
        assertThat(advanced.getOutput()).isNotEmpty();
        assertThat(advanced.getOutput().get(0).getVector()).isNotNull();
        assertThat(advanced.getOutput().get(0).getFields()).containsKeys("content", "rank_value");

        Response<List<Doc>> sparseHybrid = awaitQuery(store, collectionName, QueryDocRequest.builder()
                .vector(vector(ALPHA)).sparseVector(Map.of(1L, 1.0f)).topk(3).build());
        assertThat(sparseHybrid.getOutput()).extracting(Doc::getId).contains("alpha-1");

        Response<List<Group>> grouped = awaitGroupQuery(store, collectionName,
                QueryDocGroupByRequest.builder().vector(vector(ALPHA)).groupByField("group_key")
                        .groupCount(2).groupTopk(2).outputFields(List.of("content", "group_key")).build());
        assertThat(grouped.getOutput()).extracting(Group::getGroupId)
                .contains("group-a", "group-b");
        assertThat(grouped.getOutput()).allSatisfy(group -> assertThat(group.getDocs()).isNotEmpty());

        String partition = "adv_part";
        assertSuccess(store.createPartition(partition, 60), "createPartition");
        assertThat(assertSuccess(store.describePartition(partition), "describePartition").getOutput())
                .isEqualTo(Status.SERVING);
        assertThat(assertSuccess(store.listPartitions(), "listPartitions").getOutput()).contains(partition);
        assertWriteSuccess(store.insert(InsertDocRequest.builder().partition(partition)
                .doc(doc("partition-1", ALPHA, Map.of("content", "partition document", "tenant", "keep",
                        "group_key", "group-p", "rank_value", 4), Map.of(3L, 1.0f))).build()),
                "insertPartitionDocument");
        awaitFetchCount(store, collectionName, List.of("partition-1"), partition, 1);
        assertThat(assertSuccess(store.partitionStats(partition), "partitionStats").getOutput()
                .getTotalDocCount()).isGreaterThanOrEqualTo(1);
        assertSuccess(store.deletePartition(partition), "deletePartition");
        awaitPartitionDeleted(store, collectionName, partition);

        assertSuccess(store.delete(DeleteDocRequest.builder().deleteAll(true)
                .filter("tenant = 'delete'").build()), "deleteByFilter");
        awaitMissing(store, collectionName, "alpha-2", "default");
        assertCollectionStatsAvailable(store, collectionName);
    }

    private void verifyMultiVectorAndFusionApis(DashVectorVectorStore store,
                                                 String collectionName) throws Exception {
        Response<Void> created = store.createCollection(CreateCollectionRequest.builder()
                .name(collectionName).timeout(120)
                .vectors("title", VectorParam.builder().dimension(4)
                        .metric(CollectionInfo.Metric.dotproduct).build())
                .vectors("content", VectorParam.builder().dimension(4)
                        .metric(CollectionInfo.Metric.dotproduct).build())
                .sparseVectors("keywords", VectorParam.builder()
                        .metric(CollectionInfo.Metric.dotproduct).build())
                .filedSchema("category", FieldType.STRING)
                .build());
        assertSuccess(created, "createMultiVectorCollection");

        Response<List<DocOpResult>> inserted = store.insert(collectionName, InsertDocRequest.builder().docs(List.of(
                multiDoc("multi-alpha", ALPHA, List.of(0.8f, 0.2f, 0.0f, 0.0f),
                        Map.of(1L, 1.0f), "group-a"),
                multiDoc("multi-beta", BETA, List.of(0.1f, 0.9f, 0.0f, 0.0f),
                        Map.of(2L, 1.0f), "group-b"),
                multiDoc("multi-mixed", List.of(0.7f, 0.3f, 0.0f, 0.0f), ALPHA,
                        Map.of(1L, 0.8f), "group-a")))
                .build());
        assertWriteSuccess(inserted, "insertMultiVectorDocuments");
        awaitFetchCount(store, collectionName,
                List.of("multi-alpha", "multi-beta", "multi-mixed"), "default", 3);

        QueryDocRequest.QueryDocRequestBuilder multiQuery = QueryDocRequest.builder()
                .vectors("title", VectorQuery.builder().vector(vector(ALPHA)).numCandidates(3).ef(64).build())
                .vectors("content", VectorQuery.builder().vector(vector(ALPHA)).numCandidates(3).build())
                .sparseVectors("keywords", SparseVectorQuery.builder()
                        .vector(SparseVector.builder().value(Map.of(1L, 1.0f)).build())
                        .numCandidates(3).build())
                .topk(3).includeVector(true).outputField("category");

        Response<List<Doc>> rrf = awaitQuery(store, collectionName,
                multiQuery.ranker(RrfRanker.builder().rankConstant(60).build()).build());
        assertThat(rrf.getOutput()).isNotEmpty();
        assertThat(rrf.getOutput().get(0).getVectors()).isNotEmpty();

        Response<List<Doc>> weighted = awaitQuery(store, collectionName, QueryDocRequest.builder()
                .vectors("title", VectorQuery.builder().vector(vector(ALPHA)).numCandidates(3).build())
                .vectors("content", VectorQuery.builder().vector(vector(ALPHA)).numCandidates(3).build())
                .sparseVectors("keywords", SparseVectorQuery.builder()
                        .vector(SparseVector.builder().value(Map.of(1L, 1.0f)).build())
                        .numCandidates(3).build())
                .ranker(WeightedRanker.builder().weights(Map.of(
                        "title", 1.0f, "content", 1.0f, "keywords", 1.0f)).build())
                .topk(3).build());
        assertThat(weighted.getOutput()).isNotEmpty();

        Response<List<Group>> grouped = awaitGroupQuery(store, collectionName,
                QueryDocGroupByRequest.builder().vector(vector(ALPHA)).vectorField("title")
                        .groupByField("category").groupCount(2).groupTopk(2).build());
        assertThat(grouped.getOutput()).extracting(Group::getGroupId)
                .contains("group-a", "group-b");

        assertCollectionStatsAvailable(store, collectionName);
    }

    private static Doc doc(String id, List<Float> dense, Map<String, Object> fields,
                           Map<Long, Float> sparse) {
        return Doc.builder().id(id).vector(vector(dense)).fields(fields).sparseVector(sparse).build();
    }

    private static Doc multiDoc(String id, List<Float> title, List<Float> content,
                                Map<Long, Float> keywords, String category) {
        return Doc.builder().id(id)
                .vectors("title", vector(title)).vectors("content", vector(content))
                .sparseVectors("keywords", SparseVector.builder().value(keywords).build())
                .field("category", category).build();
    }

    private static Vector vector(List<Float> value) {
        return Vector.builder().value(value).build();
    }

    private Response<List<Doc>> awaitQuery(DashVectorVectorStore store, String collectionName,
                                            QueryDocRequest request) throws InterruptedException {
        long deadline = System.nanoTime() + VISIBILITY_TIMEOUT.toNanos();
        Response<List<Doc>> response;
        do {
            response = assertSuccess(store.query(collectionName, request), "query");
            if (response.getOutput() != null && !response.getOutput().isEmpty()) return response;
            Thread.sleep(1_000);
        } while (System.nanoTime() < deadline);
        throw new AssertionError("DashVector query returned no documents before visibility timeout");
    }

    private Response<List<Group>> awaitGroupQuery(DashVectorVectorStore store, String collectionName,
                                                   QueryDocGroupByRequest request) throws InterruptedException {
        long deadline = System.nanoTime() + VISIBILITY_TIMEOUT.toNanos();
        Response<List<Group>> response;
        do {
            response = assertSuccess(store.queryGroupBy(collectionName, request), "queryGroupBy");
            if (response.getOutput() != null && !response.getOutput().isEmpty()) return response;
            Thread.sleep(1_000);
        } while (System.nanoTime() < deadline);
        throw new AssertionError("DashVector group query returned no groups before visibility timeout");
    }

    private void awaitFetchCount(DashVectorVectorStore store, String collectionName, List<String> ids,
                                 String partition, int expected) throws InterruptedException {
        long deadline = System.nanoTime() + VISIBILITY_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            Response<Map<String, Doc>> response = assertSuccess(store.fetch(collectionName,
                    FetchDocRequest.builder().ids(ids).partition(partition).build()), "fetch");
            if (response.getOutput().size() == expected) return;
            Thread.sleep(1_000);
        }
        throw new AssertionError("DashVector documents did not become visible before timeout");
    }

    private void awaitFieldValue(DashVectorVectorStore store, String collectionName, String id,
                                 String field, Object expected) throws InterruptedException {
        long deadline = System.nanoTime() + VISIBILITY_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            Response<Map<String, Doc>> response = assertSuccess(store.fetch(collectionName,
                    FetchDocRequest.builder().id(id).build()), "fetchUpdated");
            Doc doc = response.getOutput().get(id);
            if (doc != null && expected.equals(doc.getFields().get(field))) return;
            Thread.sleep(1_000);
        }
        throw new AssertionError("DashVector update did not become visible before timeout");
    }

    private void awaitMissing(DashVectorVectorStore store, String collectionName, String id,
                              String partition) throws InterruptedException {
        long deadline = System.nanoTime() + VISIBILITY_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            Response<Map<String, Doc>> response = assertSuccess(store.fetch(collectionName,
                    FetchDocRequest.builder().id(id).partition(partition).build()), "fetchDeleted");
            if (!response.getOutput().containsKey(id)) return;
            Thread.sleep(1_000);
        }
        throw new AssertionError("DashVector document remained visible after delete timeout");
    }

    private void awaitPartitionDeleted(DashVectorVectorStore store, String collectionName,
                                       String partition) throws InterruptedException {
        long deadline = System.nanoTime() + VISIBILITY_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            Response<Status> response = store.describePartition(collectionName, partition);
            if (response.getCode() == ErrorCode.INEXISTENT_PARTITION.getCode()) return;
            Thread.sleep(1_000);
        }
        throw new AssertionError("Temporary DashVector partition remained visible after cleanup timeout");
    }

    private void assertCollectionStatsAvailable(DashVectorVectorStore store, String collectionName) {
        Response<com.aliyun.dashvector.models.CollectionStats> response =
                assertSuccess(store.collectionStats(collectionName), "collectionStats");
        assertThat(response.getOutput()).isNotNull();
        assertThat(response.getOutput().getTotalDocCount()).isNotNegative();
        assertThat(response.getOutput().getPartitions()).containsKey("default");
    }

    private void awaitCollectionDeleted(DashVectorClient client, String collectionName)
            throws InterruptedException {
        long deadline = System.nanoTime() + VISIBILITY_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (client.describe(collectionName).getCode() == ErrorCode.INEXISTENT_COLLECTION.getCode()) return;
            Thread.sleep(1_000);
        }
        throw new AssertionError("Temporary DashVector collection remained visible after cleanup timeout");
    }

    private void awaitCollectionDeleted(DashVectorVectorStore store, String collectionName)
            throws InterruptedException {
        long deadline = System.nanoTime() + VISIBILITY_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (store.describeCollection(collectionName).getCode()
                    == ErrorCode.INEXISTENT_COLLECTION.getCode()) return;
            Thread.sleep(1_000);
        }
        throw new AssertionError("Temporary DashVector collection remained visible after plugin delete timeout");
    }

    private static void assertWriteSuccess(Response<List<DocOpResult>> response, String operation) {
        assertSuccess(response, operation);
        assertThat(response.getOutput()).as(operation + " document results").isNotEmpty()
                .allSatisfy(result -> assertThat(result.getCode()).as(result.getId()).isZero());
    }

    private static <T> Response<T> assertSuccess(Response<T> response, String operation) {
        assertThat(response).as(operation + " response").isNotNull();
        assertThat(response.isSuccess())
                .as("%s failed: code=%s, message=%s, requestId=%s", operation,
                        response.getCode(), response.getMessage(), response.getRequestId())
                .isTrue();
        return response;
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
                    .map(ignored -> new Embedding(new float[]{1.0f, 0.0f, 0.0f, 0.0f}, 0)).toList();
            return new EmbeddingResponse(embeddings);
        }

        @Override public float[] embed(Document document) { return new float[]{1.0f, 0.0f, 0.0f, 0.0f}; }
        @Override public int dimensions() { return 4; }
    }
}
