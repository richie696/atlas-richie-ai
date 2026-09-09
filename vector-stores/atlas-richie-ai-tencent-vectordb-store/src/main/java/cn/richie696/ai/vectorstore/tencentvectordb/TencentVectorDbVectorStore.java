package cn.richie696.ai.vectorstore.tencentvectordb;

import cn.richie696.ai.vectorstore.tencentvectordb.api.*;
import com.tencent.tcvectordb.client.VectorDBClient;
import com.tencent.tcvectordb.model.AIDatabase;
import com.tencent.tcvectordb.model.DocField;
import com.tencent.tcvectordb.model.param.collection.*;
import com.tencent.tcvectordb.model.param.dml.*;
import com.tencent.tcvectordb.model.param.entity.*;
import com.tencent.tcvectordb.model.param.user.*;
import io.micrometer.observation.ObservationRegistry;
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
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Spring AI 2.0 VectorStore adapter for Tencent Cloud VectorDB. */
public final class TencentVectorDbVectorStore extends AbstractObservationVectorStore implements InitializingBean,
        TencentVectorDbDatabaseOperations, TencentVectorDbCollectionOperations,
        TencentVectorDbDocumentOperations, TencentVectorDbSearchOperations,
        TencentVectorDbIndexOperations, TencentVectorDbPermissionOperations, TencentVectorDbAiOperations {

    public static final String PROVIDER_NAME = "tencent-vectordb";
    public static final String DEFAULT_DATABASE_NAME = "spring_ai";
    public static final String DEFAULT_COLLECTION_NAME = "vector_store";
    public static final String ID_FIELD_NAME = "id";
    public static final String VECTOR_FIELD_NAME = "vector";
    public static final String CONTENT_FIELD_NAME = "content";
    public static final int DEFAULT_EMBEDDING_DIMENSION = 1536;
    public static final int MAX_UPSERT_BATCH_SIZE = 1000;
    public static final int MAX_ID_BATCH_SIZE = 20;

    private final VectorDBClient client;
    private final String databaseName;
    private final String collectionName;
    private final int embeddingDimension;
    private final boolean initializeSchema;
    private final int shardNum;
    private final int replicaNum;
    private final String description;
    private final IndexType indexType;
    private final MetricType metricType;
    private final TencentVectorDbFilterExpressionConverter filterConverter;

    private TencentVectorDbVectorStore(Builder builder) {
        super(builder);
        this.client = builder.client; this.databaseName = builder.databaseName;
        this.collectionName = builder.collectionName; this.embeddingDimension = builder.embeddingDimension;
        this.initializeSchema = builder.initializeSchema; this.shardNum = builder.shardNum;
        this.replicaNum = builder.replicaNum; this.description = builder.description;
        this.indexType = builder.indexType; this.metricType = builder.metricType;
        this.filterConverter = builder.filterConverter;
        Assert.hasText(databaseName, "databaseName must not be blank");
        Assert.hasText(collectionName, "collectionName must not be blank");
        Assert.isTrue(embeddingDimension > 0, "embeddingDimension must be positive");
    }

    @Override
    public void afterPropertiesSet() {
        try {
            if (initializeSchema) {
                if (!client.listDatabase().contains(databaseName)) client.createDatabase(databaseName);
                boolean collectionExists = client.listCollections(databaseName).stream()
                        .anyMatch(value -> collectionName.equals(value.getCollection()));
                if (!collectionExists) client.createCollection(databaseName, collectionDefinition());
            }
            validateSchema(client.describeCollection(databaseName, collectionName));
        } catch (RuntimeException exception) {
            throw failure("schema initialization", exception);
        }
    }

    private CreateCollectionParam collectionDefinition() {
        CreateCollectionParam.Builder builder = CreateCollectionParam.newBuilder().withName(collectionName)
                .withShardNum(shardNum).withReplicaNum(replicaNum)
                .addField(new FilterIndex(ID_FIELD_NAME, FieldType.String, IndexType.PRIMARY_KEY))
                .addField(new FilterIndex(CONTENT_FIELD_NAME, FieldType.String, IndexType.FILTER))
                .addField(new VectorIndex(VECTOR_FIELD_NAME, embeddingDimension, indexType, metricType,
                        indexType == IndexType.HNSW ? new HNSWParams(16, 200) : null))
                .withFilterIndexConfig(FilterIndexConfig.newBuilder().withFilterAll(true)
                        .withFieldWithoutFilterIndex(List.of(CONTENT_FIELD_NAME)).withMaxStrLen(256).build());
        if (description != null) builder.withDescription(description);
        return builder.build();
    }

    private void validateSchema(com.tencent.tcvectordb.model.Collection collection) {
        IndexField vector = collection.getIndexes().stream()
                .filter(field -> VECTOR_FIELD_NAME.equals(field.getFieldName())).findFirst()
                .orElseThrow(() -> new IllegalStateException("Tencent VectorDB collection has no vector field"));
        if (!Integer.valueOf(embeddingDimension).equals(vector.getDimension()))
            throw new IllegalStateException("Tencent VectorDB vector dimension does not match configured dimension");
        if (vector.getMetricType() != metricType)
            throw new IllegalStateException("Tencent VectorDB metric does not match configured metric");
        boolean hasId = collection.getIndexes().stream().anyMatch(field -> ID_FIELD_NAME.equals(field.getFieldName()) && field.isPrimaryKey());
        boolean hasContent = collection.getIndexes().stream().anyMatch(field -> CONTENT_FIELD_NAME.equals(field.getFieldName()));
        if (!hasId || !hasContent) throw new IllegalStateException("Tencent VectorDB collection must contain id/content fields");
    }

    @Override
    public void doAdd(@Nonnull List<org.springframework.ai.document.Document> documents) {
        Assert.notNull(documents, "documents must not be null");
        for (int from = 0; from < documents.size(); from += MAX_UPSERT_BATCH_SIZE) {
            int to = Math.min(from + MAX_UPSERT_BATCH_SIZE, documents.size());
            List<org.springframework.ai.document.Document> batch = documents.subList(from, to);
            List<float[]> embeddings = embeddingModel.embed(batch, EmbeddingOptions.builder().build(), batchingStrategy);
            if (embeddings.size() != batch.size()) throw new IllegalStateException("Embedding count does not match document count");
            List<com.tencent.tcvectordb.model.Document> nativeDocuments = new ArrayList<>(batch.size());
            for (int i = 0; i < batch.size(); i++) nativeDocuments.add(toNativeDocument(batch.get(i), embeddings.get(i)));
            assertSuccess(upsert(InsertParam.newBuilder().withDocuments(nativeDocuments).build()), "upsert");
        }
    }

    private com.tencent.tcvectordb.model.Document toNativeDocument(org.springframework.ai.document.Document document,
                                                                   float[] embedding) {
        if (embedding.length != embeddingDimension) throw new IllegalArgumentException("Embedding dimension does not match configured dimension");
        if (document.getMetadata().containsKey(CONTENT_FIELD_NAME))
            throw new IllegalArgumentException("Document metadata contains reserved field 'content'");
        List<DocField> fields = new ArrayList<>();
        fields.add(new DocField(CONTENT_FIELD_NAME, document.getText() == null ? "" : document.getText()));
        document.getMetadata().forEach((name, value) -> fields.add(new DocField(name, value)));
        return com.tencent.tcvectordb.model.Document.newBuilder().withId(document.getId())
                .withVector(floats(embedding)).addDocFields(fields).build();
    }

    @Override
    public void doDelete(@Nonnull List<String> ids) {
        Assert.notNull(ids, "ids must not be null");
        for (int from = 0; from < ids.size(); from += MAX_ID_BATCH_SIZE) {
            int to = Math.min(from + MAX_ID_BATCH_SIZE, ids.size());
            assertSuccess(delete(DeleteParam.newBuilder().withDocumentIds(ids.subList(from, to)).build()), "delete");
        }
    }

    @Override
    protected void doDelete(@Nonnull Filter.Expression expression) {
        assertSuccess(delete(DeleteParam.newBuilder().withFilter(filterConverter.convertExpression(expression)).build()), "deleteByFilter");
    }

    @Nonnull
    @Override
    public List<org.springframework.ai.document.Document> doSimilaritySearch(@Nonnull SearchRequest request) {
        Assert.notNull(request, "search request must not be null");
        float[] embedding = embeddingModel.embed(request.getQuery());
        if (embedding.length != embeddingDimension) throw new IllegalArgumentException("Query embedding dimension does not match configured dimension");
        SearchByVectorParam.Builder search = SearchByVectorParam.newBuilder().addVector(floats(embedding)).withLimit(request.getTopK());
        if (request.hasFilterExpression()) search.withFilter(filterConverter.convertExpression(request.getFilterExpression()));
        List<List<com.tencent.tcvectordb.model.Document>> results = search(search.build());
        List<com.tencent.tcvectordb.model.Document> result = results.isEmpty() ? List.of() : results.get(0);
        return result.stream().filter(document -> matchesThreshold(document.getScore(), request.getSimilarityThreshold()))
                .map(this::toSpringDocument).toList();
    }

    private boolean matchesThreshold(Double score, double threshold) {
        return score == null || similarityScore(score) >= threshold;
    }

    double similarityScore(double providerScore) {
        return switch (metricType) {
            case L2, HAMMING -> 1.0 / (1.0 + Math.max(0.0, providerScore));
            case COSINE, IP -> clamp01(providerScore);
        };
    }

    private org.springframework.ai.document.Document toSpringDocument(com.tencent.tcvectordb.model.Document document) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (document.getDocFields() != null) document.getDocFields().forEach(field -> metadata.put(field.getName(), field.getValue()));
        Object content = metadata.remove(CONTENT_FIELD_NAME);
        return org.springframework.ai.document.Document.builder().id(document.getId())
                .text(content == null ? "" : String.valueOf(content)).metadata(metadata)
                .score(document.getScore() == null ? null : similarityScore(document.getScore())).build();
    }

    private static double clamp01(double value) { return Math.max(0.0, Math.min(1.0, value)); }

    @Override public com.tencent.tcvectordb.model.Database createDatabase(String name) { return client.createDatabase(name); }
    @Override public com.tencent.tcvectordb.model.Database createDatabaseIfNotExists(String name) { return client.createDatabaseIfNotExists(name); }
    @Override public boolean databaseExists(String name) { return client.IsExistsDatabase(name); }
    @Override public List<String> listDatabases() { return client.listDatabase(); }
    @Override public com.tencent.tcvectordb.model.Database dropDatabase(String name) { return client.dropDatabase(name); }
    @Override public boolean collectionExists(String database, String collection) { return client.IsExistsCollection(database, collection); }
    @Override public com.tencent.tcvectordb.model.Collection createCollection(String database, CreateCollectionParam param) { return client.createCollection(database, param); }
    @Override public com.tencent.tcvectordb.model.Collection createCollectionIfNotExists(String database, CreateCollectionParam param) { return client.createCollectionIfNotExists(database, param); }
    @Override public List<com.tencent.tcvectordb.model.Collection> listCollections(String database) { return client.listCollections(database); }
    @Override public com.tencent.tcvectordb.model.Collection describeCollection(String database, String collection) { return client.describeCollection(database, collection); }
    @Override public AffectRes truncateCollection(String database, String collection) { return client.truncateCollections(database, collection); }
    @Override public void dropCollection(String database, String collection) { client.dropCollection(database, collection); }
    @Override public AffectRes setAlias(String database, String collection, String alias) { return client.setAlias(database, collection, alias); }
    @Override public AffectRes deleteAlias(String database, String alias) { return client.deleteAlias(database, alias); }
    @Override public AffectRes upsert(InsertParam param) { return client.upsert(databaseName, collectionName, param); }
    @Override public List<com.tencent.tcvectordb.model.Document> query(QueryParam param) { return client.query(databaseName, collectionName, param); }
    @Override public AffectRes update(UpdateParam param, com.tencent.tcvectordb.model.Document document) { return client.update(databaseName, collectionName, param, document); }
    @Override public AffectRes update(UpdateParam param, JSONObject document) { return client.update(databaseName, collectionName, param, document); }
    @Override public AffectRes delete(DeleteParam param) { return client.delete(databaseName, collectionName, param); }
    @Override public BaseRes count(CountQueryParam param) { return client.count(databaseName, collectionName, param); }
    @Override public List<List<com.tencent.tcvectordb.model.Document>> search(SearchByVectorParam param) { return client.search(databaseName, collectionName, param); }
    @Override public List<List<com.tencent.tcvectordb.model.Document>> searchById(SearchByIdParam param) { return client.searchById(databaseName, collectionName, param); }
    @Override public SearchRes searchByText(SearchByEmbeddingItemsParam param) { return client.searchByEmbeddingItems(databaseName, collectionName, param); }
    @Override public FullTextSearchRes fullTextSearch(FullTextSearchParam param) { return client.fullTextSearch(databaseName, collectionName, param); }
    @Override public HybridSearchRes hybridSearch(HybridSearchParam param) { return client.hybridSearch(databaseName, collectionName, param); }
    @Override public BaseRes rebuildIndex(RebuildIndexParam param) { return client.rebuildIndex(databaseName, collectionName, param); }
    @Override public BaseRes addIndex(AddIndexParam param) { return client.addIndex(databaseName, collectionName, param); }
    @Override public BaseRes dropIndex(List<String> fieldNames) { return client.dropIndex(databaseName, collectionName, fieldNames); }
    @Override public BaseRes modifyVectorIndex(ModifyVectorIndexParam param) { return client.modifyVectorIndex(databaseName, collectionName, param); }
    @Override public BaseRes createUser(String username, String password) { return client.createUser(username, password); }
    @Override public UserDescribeRes describeUser(String username) { return client.describeUser(username); }
    @Override public UserListRes listUsers() { return client.listUser(); }
    @Override public BaseRes grant(UserGrantParam param) { return client.grantToUser(param); }
    @Override public BaseRes revoke(UserRevokeParam param) { return client.revokeFromUser(param); }
    @Override public BaseRes changePassword(String username, String password) { return client.changePassword(username, password); }
    @Override public BaseRes dropUser(String username) { return client.dropUser(username); }
    @Override public AIDatabase createAiDatabase(String name) { return client.createAIDatabase(name); }
    @Override public AIDatabase aiDatabase(String name) { return client.aiDatabase(name); }
    @Override public AffectRes dropAiDatabase(String name) { return client.dropAIDatabase(name); }
    @Override public void uploadFile(String database, String collection, UploadFileParam param, Map<String, Object> metadata) throws Exception { client.UploadFile(database, collection, param, metadata); }
    @Override public GetImageUrlRes getImageUrl(String database, String collection, GetImageUrlParam param) { return client.GetImageUrl(database, collection, param); }
    @Override public QueryFileDetailRes queryFileDetails(String database, String collection, QueryFileDetailParam param) { return client.queryFileDetails(database, collection, param); }
    @Override public AtomicEmbeddingRes atomicEmbedding(AtomicEmbeddingParam param) { return client.atomicEmbedding(param); }

    public Optional<VectorDBClient> getNativeClient() { return Optional.of(client); }
    public String getDatabaseName() { return databaseName; }
    public String getCollectionName() { return collectionName; }
    public int getEmbeddingDimension() { return embeddingDimension; }

    private void assertSuccess(BaseRes response, String operation) {
        if (response == null || response.getCode() != 0)
            throw failure(operation, new IllegalStateException(response == null ? "null response" : response.getMsg()));
    }
    private TencentVectorDbVectorStoreException failure(String operation, Throwable cause) {
        return new TencentVectorDbVectorStoreException(operation, databaseName, collectionName, cause);
    }
    private static List<Float> floats(float[] values) { List<Float> result = new ArrayList<>(values.length); for (float value : values) result.add(value); return result; }

    @Nonnull @Override
    public VectorStoreObservationContext.Builder createObservationContextBuilder(@Nonnull String operationName) {
        return VectorStoreObservationContext.builder(PROVIDER_NAME, operationName)
                .collectionName(databaseName + "." + collectionName).dimensions(embeddingDimension);
    }

    public static final class Builder extends AbstractVectorStoreBuilder<Builder> {
        private final VectorDBClient client;
        private String databaseName = DEFAULT_DATABASE_NAME;
        private String collectionName = DEFAULT_COLLECTION_NAME;
        private int embeddingDimension = DEFAULT_EMBEDDING_DIMENSION;
        private boolean initializeSchema;
        private int shardNum = 1;
        private int replicaNum = 2;
        private String description;
        private IndexType indexType = IndexType.HNSW;
        private MetricType metricType = MetricType.COSINE;
        private TencentVectorDbFilterExpressionConverter filterConverter = new TencentVectorDbFilterExpressionConverter();
        public Builder(EmbeddingModel embeddingModel, VectorDBClient client) { super(embeddingModel); Assert.notNull(client, "client must not be null"); this.client = client; }
        public Builder databaseName(String value) { databaseName = value; return this; }
        public Builder collectionName(String value) { collectionName = value; return this; }
        public Builder embeddingDimension(int value) { embeddingDimension = value; return this; }
        public Builder initializeSchema(boolean value) { initializeSchema = value; return this; }
        public Builder shardNum(int value) { shardNum = value; return this; }
        public Builder replicaNum(int value) { replicaNum = value; return this; }
        public Builder description(String value) { description = value; return this; }
        public Builder indexType(IndexType value) { indexType = value; return this; }
        public Builder metricType(MetricType value) { metricType = value; return this; }
        public Builder filterExpressionConverter(TencentVectorDbFilterExpressionConverter value) { filterConverter = value; return this; }
        @Override public Builder observationRegistry(ObservationRegistry value) { return super.observationRegistry(value); }
        @Override public TencentVectorDbVectorStore build() { return new TencentVectorDbVectorStore(this); }
    }
}
