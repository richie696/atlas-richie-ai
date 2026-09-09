package cn.richie696.ai.vectorstore.dashvector;

import cn.richie696.ai.vectorstore.dashvector.api.DashVectorCollectionOperations;
import cn.richie696.ai.vectorstore.dashvector.api.DashVectorDocumentOperations;
import cn.richie696.ai.vectorstore.dashvector.api.DashVectorPartitionOperations;
import cn.richie696.ai.vectorstore.dashvector.api.DashVectorSearchOperations;
import com.aliyun.dashvector.DashVectorClient;
import com.aliyun.dashvector.DashVectorCollection;
import com.aliyun.dashvector.common.ErrorCode;
import com.aliyun.dashvector.models.CollectionMeta;
import com.aliyun.dashvector.models.CollectionStats;
import com.aliyun.dashvector.models.Doc;
import com.aliyun.dashvector.models.DocOpResult;
import com.aliyun.dashvector.models.Group;
import com.aliyun.dashvector.models.PartitionStats;
import com.aliyun.dashvector.models.Vector;
import com.aliyun.dashvector.models.requests.CreateCollectionRequest;
import com.aliyun.dashvector.models.requests.DeleteDocRequest;
import com.aliyun.dashvector.models.requests.FetchDocRequest;
import com.aliyun.dashvector.models.requests.InsertDocRequest;
import com.aliyun.dashvector.models.requests.QueryDocGroupByRequest;
import com.aliyun.dashvector.models.requests.QueryDocRequest;
import com.aliyun.dashvector.models.requests.UpdateDocRequest;
import com.aliyun.dashvector.models.requests.UpsertDocRequest;
import com.aliyun.dashvector.models.responses.Response;
import com.aliyun.dashvector.proto.CollectionInfo;
import com.aliyun.dashvector.proto.FieldType;
import com.aliyun.dashvector.proto.Status;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.vectorstore.AbstractVectorStoreBuilder;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.observation.AbstractObservationVectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Spring AI 2.0 VectorStore adapter for Alibaba Cloud DashVector. */
public final class DashVectorVectorStore extends AbstractObservationVectorStore implements InitializingBean,
        DashVectorCollectionOperations, DashVectorDocumentOperations, DashVectorSearchOperations,
        DashVectorPartitionOperations {

    public static final String PROVIDER_NAME = "dashvector";
    public static final String DEFAULT_COLLECTION_NAME = "vector_store";
    public static final String CONTENT_FIELD_NAME = "content";
    public static final int DEFAULT_EMBEDDING_DIMENSION = 1536;
    public static final int MAX_BATCH_SIZE = 1024;

    private final DashVectorClient client;
    private final String collectionName;
    private final int embeddingDimension;
    private final boolean initializeSchema;
    private final int schemaInitializationTimeoutSeconds;
    private final String partition;
    private final CollectionInfo.Metric metric;
    private final Map<String, FieldType> metadataFields;
    private final DashVectorFilterExpressionConverter filterConverter;
    private volatile DashVectorCollection collection;

    private DashVectorVectorStore(Builder builder) {
        super(builder);
        this.client = builder.client;
        this.collectionName = builder.collectionName;
        this.embeddingDimension = builder.embeddingDimension;
        this.initializeSchema = builder.initializeSchema;
        this.schemaInitializationTimeoutSeconds = builder.schemaInitializationTimeoutSeconds;
        this.partition = builder.partition;
        this.metric = builder.metric;
        this.metadataFields = Map.copyOf(builder.metadataFields);
        this.filterConverter = builder.filterConverter;
        Assert.hasText(collectionName, "collectionName must not be blank");
        Assert.hasText(partition, "partition must not be blank");
        Assert.isTrue(embeddingDimension > 0, "embeddingDimension must be positive");
        Assert.isTrue(schemaInitializationTimeoutSeconds > 0,
                "schemaInitializationTimeoutSeconds must be positive");
        Assert.isTrue(!metadataFields.containsKey(CONTENT_FIELD_NAME),
                "metadataFields must not contain reserved field 'content'");
    }

    @Override
    public void afterPropertiesSet() {
        Response<CollectionMeta> described = client.describe(collectionName);
        if (initializeSchema && described.getCode() == ErrorCode.INEXISTENT_COLLECTION.getCode()) {
            CreateCollectionRequest.CreateCollectionRequestBuilder request = CreateCollectionRequest.builder()
                    .name(collectionName).dimension(embeddingDimension).metric(metric)
                    .timeout(schemaInitializationTimeoutSeconds)
                    .filedSchema(CONTENT_FIELD_NAME, FieldType.STRING);
            metadataFields.forEach(request::filedSchema);
            assertSuccess(client.create(request.build()), "createCollection");
        } else if (!described.isSuccess()) {
            throw failure("describeCollection", described);
        } else {
            validateSchema(described.getOutput());
        }
        this.collection = client.get(collectionName);
        if (!collection.isSuccess()) {
            throw new DashVectorVectorStoreException("getCollection", collectionName,
                    collection.getCode(), collection.getRequestId(), collection.getMessage());
        }
    }

    private void validateSchema(CollectionMeta meta) {
        if (meta.getDimension() != embeddingDimension)
            throw new IllegalStateException("DashVector collection dimension does not match configured embeddingDimension");
        if (meta.getMetric() != metric)
            throw new IllegalStateException("DashVector collection metric does not match configured metric");
        if (meta.getFieldsSchema().get(CONTENT_FIELD_NAME) != FieldType.STRING)
            throw new IllegalStateException("DashVector collection must declare string field 'content'");
        metadataFields.forEach((name, type) -> {
            if (meta.getFieldsSchema().get(name) != type)
                throw new IllegalStateException("DashVector collection field mismatch: " + name);
        });
    }

    @Override
    public void doAdd(@Nonnull List<Document> documents) {
        Assert.notNull(documents, "documents must not be null");
        for (int from = 0; from < documents.size(); from += MAX_BATCH_SIZE) {
            int to = Math.min(from + MAX_BATCH_SIZE, documents.size());
            List<Document> batch = documents.subList(from, to);
            List<float[]> embeddings = embeddingModel.embed(batch, EmbeddingOptions.builder().build(), batchingStrategy);
            if (embeddings.size() != batch.size()) throw new IllegalStateException("Embedding count does not match document count");
            List<Doc> nativeDocs = new ArrayList<>(batch.size());
            for (int i = 0; i < batch.size(); i++) nativeDocs.add(toNativeDocument(batch.get(i), embeddings.get(i)));
            assertSuccess(upsert(UpsertDocRequest.builder().docs(nativeDocs).partition(partition).build()), "upsert");
        }
    }

    private Doc toNativeDocument(Document document, float[] embedding) {
        if (embedding.length != embeddingDimension)
            throw new IllegalArgumentException("Embedding dimension does not match configured dimension");
        Map<String, Object> fields = new LinkedHashMap<>(document.getMetadata());
        if (fields.containsKey(CONTENT_FIELD_NAME))
            throw new IllegalArgumentException("Document metadata contains reserved field 'content'");
        fields.put(CONTENT_FIELD_NAME, document.getText() == null ? "" : document.getText());
        return Doc.builder().id(document.getId()).vector(Vector.builder().value(floats(embedding)).build())
                .fields(fields).build();
    }

    @Override
    public void doDelete(@Nonnull List<String> ids) {
        Assert.notNull(ids, "ids must not be null");
        for (int from = 0; from < ids.size(); from += MAX_BATCH_SIZE) {
            int to = Math.min(from + MAX_BATCH_SIZE, ids.size());
            assertSuccess(delete(DeleteDocRequest.builder().ids(ids.subList(from, to)).partition(partition).build()), "delete");
        }
    }

    @Override
    protected void doDelete(@Nonnull Filter.Expression expression) {
        assertSuccess(delete(DeleteDocRequest.builder().deleteAll(true).partition(partition)
                .filter(filterConverter.convertExpression(expression)).build()), "deleteByFilter");
    }

    @Nonnull
    @Override
    public List<Document> doSimilaritySearch(@Nonnull SearchRequest request) {
        Assert.notNull(request, "search request must not be null");
        float[] embedding = embeddingModel.embed(request.getQuery());
        if (embedding.length != embeddingDimension)
            throw new IllegalArgumentException("Query embedding dimension does not match configured dimension");
        QueryDocRequest.QueryDocRequestBuilder query = QueryDocRequest.builder()
                .vector(Vector.builder().value(floats(embedding)).build()).topk(request.getTopK()).partition(partition);
        if (request.hasFilterExpression()) query.filter(filterConverter.convertExpression(request.getFilterExpression()));
        Response<List<Doc>> response = query(query.build());
        assertSuccess(response, "query");
        return response.getOutput().stream()
                .filter(doc -> similarityScore(doc.getScore()) >= request.getSimilarityThreshold())
                .map(this::toSpringDocument).toList();
    }

    private Document toSpringDocument(Doc doc) {
        Map<String, Object> fields = doc.getFields() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(doc.getFields());
        Object content = fields.remove(CONTENT_FIELD_NAME);
        return Document.builder().id(doc.getId()).text(content == null ? "" : String.valueOf(content))
                .metadata(fields).score(similarityScore(doc.getScore())).build();
    }

    double similarityScore(float providerScore) {
        return switch (metric) {
            case cosine -> clamp01(1.0 - providerScore / 2.0);
            case euclidean -> 1.0 / (1.0 + Math.max(0.0, providerScore));
            case dotproduct -> clamp01(providerScore);
            case UNRECOGNIZED -> throw new IllegalStateException("Unsupported DashVector metric: " + metric);
        };
    }

    private static double clamp01(double value) { return Math.max(0.0, Math.min(1.0, value)); }

    @Override public Response<Void> createCollection(CreateCollectionRequest request) { return client.create(request); }
    @Override public Response<List<String>> listCollections() { return client.list(); }
    @Override public Response<CollectionMeta> describeCollection(String name) { return client.describe(name); }
    @Override public Response<Void> deleteCollection(String name) { return client.delete(name); }
    @Override public Response<CollectionStats> collectionStats() { return boundCollection().stats(); }
    @Override public Response<CollectionStats> collectionStats(String name) { return collection(name).stats(); }
    @Override public Response<List<DocOpResult>> insert(InsertDocRequest request) { return boundCollection().insert(request); }
    @Override public Response<List<DocOpResult>> insert(String name, InsertDocRequest request) { return collection(name).insert(request); }
    @Override public Response<List<DocOpResult>> upsert(UpsertDocRequest request) { return boundCollection().upsert(request); }
    @Override public Response<List<DocOpResult>> upsert(String name, UpsertDocRequest request) { return collection(name).upsert(request); }
    @Override public Response<List<DocOpResult>> update(UpdateDocRequest request) { return boundCollection().update(request); }
    @Override public Response<List<DocOpResult>> update(String name, UpdateDocRequest request) { return collection(name).update(request); }
    @Override public Response<List<DocOpResult>> delete(DeleteDocRequest request) { return boundCollection().delete(request); }
    @Override public Response<List<DocOpResult>> delete(String name, DeleteDocRequest request) { return collection(name).delete(request); }
    @Override public Response<Map<String, Doc>> fetch(FetchDocRequest request) { return boundCollection().fetch(request); }
    @Override public Response<Map<String, Doc>> fetch(String name, FetchDocRequest request) { return collection(name).fetch(request); }
    @Override public Response<List<Doc>> query(QueryDocRequest request) { return boundCollection().query(request); }
    @Override public Response<List<Doc>> query(String name, QueryDocRequest request) { return collection(name).query(request); }
    @Override public Response<List<Group>> queryGroupBy(QueryDocGroupByRequest request) { return boundCollection().queryGroupBy(request); }
    @Override public Response<List<Group>> queryGroupBy(String name, QueryDocGroupByRequest request) { return collection(name).queryGroupBy(request); }
    @Override public Response<Void> createPartition(String name, Integer timeoutSeconds) { return boundCollection().createPartition(name, timeoutSeconds); }
    @Override public Response<Void> createPartition(String collectionName, String name, Integer timeoutSeconds) { return collection(collectionName).createPartition(name, timeoutSeconds); }
    @Override public Response<Status> describePartition(String name) { return boundCollection().describePartition(name); }
    @Override public Response<Status> describePartition(String collectionName, String name) { return collection(collectionName).describePartition(name); }
    @Override public Response<List<String>> listPartitions() { return boundCollection().listPartitions(); }
    @Override public Response<List<String>> listPartitions(String name) { return collection(name).listPartitions(); }
    @Override public Response<PartitionStats> partitionStats(String name) { return boundCollection().statsPartition(name); }
    @Override public Response<PartitionStats> partitionStats(String collectionName, String name) { return collection(collectionName).statsPartition(name); }
    @Override public Response<Void> deletePartition(String name) { return boundCollection().deletePartition(name); }
    @Override public Response<Void> deletePartition(String collectionName, String name) { return collection(collectionName).deletePartition(name); }

    public Optional<DashVectorClient> getNativeClient() { return Optional.of(client); }
    public String getCollectionName() { return collectionName; }
    public int getEmbeddingDimension() { return embeddingDimension; }

    private DashVectorCollection boundCollection() {
        DashVectorCollection value = collection;
        if (value == null) throw new IllegalStateException("DashVectorVectorStore has not been initialized");
        return value;
    }

    private DashVectorCollection collection(String name) {
        Assert.hasText(name, "collectionName must not be blank");
        return client.get(name);
    }

    private void assertSuccess(Response<?> response, String operation) {
        if (response == null || !response.isSuccess()) throw failure(operation, response);
    }

    private DashVectorVectorStoreException failure(String operation, Response<?> response) {
        return new DashVectorVectorStoreException(operation, collectionName,
                response == null ? null : response.getCode(), response == null ? null : response.getRequestId(),
                response == null ? "null response" : response.getMessage());
    }

    private static List<Float> floats(float[] values) {
        List<Float> result = new ArrayList<>(values.length);
        for (float value : values) result.add(value);
        return result;
    }

    @Nonnull
    @Override
    public VectorStoreObservationContext.Builder createObservationContextBuilder(@Nonnull String operationName) {
        return VectorStoreObservationContext.builder(PROVIDER_NAME, operationName)
                .collectionName(collectionName).dimensions(embeddingDimension);
    }

    public static final class Builder extends AbstractVectorStoreBuilder<Builder> {
        private final DashVectorClient client;
        private String collectionName = DEFAULT_COLLECTION_NAME;
        private int embeddingDimension = DEFAULT_EMBEDDING_DIMENSION;
        private boolean initializeSchema;
        private int schemaInitializationTimeoutSeconds = 120;
        private String partition = "default";
        private CollectionInfo.Metric metric = CollectionInfo.Metric.cosine;
        private Map<String, FieldType> metadataFields = Map.of();
        private DashVectorFilterExpressionConverter filterConverter = new DashVectorFilterExpressionConverter();

        public Builder(EmbeddingModel embeddingModel, DashVectorClient client) {
            super(embeddingModel);
            Assert.notNull(client, "client must not be null");
            this.client = client;
        }
        public Builder collectionName(String value) { collectionName = value; return this; }
        public Builder embeddingDimension(int value) { embeddingDimension = value; return this; }
        public Builder initializeSchema(boolean value) { initializeSchema = value; return this; }
        public Builder schemaInitializationTimeoutSeconds(int value) { schemaInitializationTimeoutSeconds = value; return this; }
        public Builder partition(String value) { partition = value; return this; }
        public Builder metric(CollectionInfo.Metric value) { metric = value; return this; }
        public Builder metadataFields(Map<String, FieldType> value) { metadataFields = value == null ? Map.of() : value; return this; }
        public Builder filterExpressionConverter(DashVectorFilterExpressionConverter value) { filterConverter = value; return this; }
        @Override public Builder observationRegistry(ObservationRegistry value) { return super.observationRegistry(value); }
        @Override public DashVectorVectorStore build() { return new DashVectorVectorStore(this); }
    }
}
