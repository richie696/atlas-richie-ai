/* Copyright (c) 2026 Richie (https://www.github.com/richie696). Licensed under Apache-2.0. */
package cn.richie696.ai.vectorstore.vikingdb;

import com.volcengine.ApiException;
import com.volcengine.vikingdb.VikingdbApi;
import com.volcengine.vikingdb.model.CreateVikingdbCollectionRequest;
import com.volcengine.vikingdb.model.CreateVikingdbIndexRequest;
import com.volcengine.vikingdb.model.CreateVikingdbTaskRequest;
import com.volcengine.vikingdb.model.FieldForCreateVikingdbCollectionInput;
import com.volcengine.vikingdb.model.GetVikingdbCollectionRequest;
import com.volcengine.vikingdb.model.GetVikingdbCollectionResponse;
import com.volcengine.vikingdb.model.GetVikingdbIndexRequest;
import com.volcengine.vikingdb.model.TaskConfigForCreateVikingdbTaskInput;
import com.volcengine.vikingdb.model.VectorIndexForCreateVikingdbIndexInput;
import com.volcengine.vikingdb.runtime.exception.ApiClientException;
import com.volcengine.vikingdb.runtime.exception.VectorApiException;
import com.volcengine.vikingdb.runtime.vector.model.request.DeleteDataRequest;
import com.volcengine.vikingdb.runtime.vector.model.request.SearchByVectorRequest;
import com.volcengine.vikingdb.runtime.vector.model.request.UpsertDataRequest;
import com.volcengine.vikingdb.runtime.vector.model.response.DataApiResponse;
import com.volcengine.vikingdb.runtime.vector.model.response.SearchItem;
import com.volcengine.vikingdb.runtime.vector.model.response.SearchResult;
import com.volcengine.vikingdb.runtime.vector.service.VectorService;
import io.micrometer.observation.ObservationRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 生产级 Spring AI 适配器，对接 VikingDB 的外部嵌入（external-embedding）数据模型。
 *
 * <p>Metadata 字段被刻意设计为显式：每个 metadata 键都必须在 collection schema 中声明，
 * 防止静默丢数据与非法 upsert。</p>
 */
@Slf4j
public final class VikingDbVectorStore extends AbstractObservationVectorStore implements InitializingBean {

    public static final String DEFAULT_COLLECTION_NAME = "vector_store";
    public static final int OPENAI_EMBEDDING_DIMENSION_SIZE = 1536;
    public static final String VIKINGDB_PROVIDER_NAME = "vikingdb";
    public static final String DOC_ID_FIELD_NAME = "doc_id";
    public static final String CONTENT_FIELD_NAME = "content";
    public static final String EMBEDDING_FIELD_NAME = "embedding";
    /** VikingDB 数据面 upsert 上限。与嵌入批处理解耦。 */
    public static final int MAX_UPSERT_BATCH_SIZE = 1_000;
    /** VikingDB 数据面 delete 上限。 */
    public static final int MAX_DELETE_BATCH_SIZE = 1_000;

    /** 数据面 SDK，所有读写操作均使用。必填，非空。 */
    private final VectorService dataPlane;
    /**
     * 控制面 SDK，仅由 {@link #afterPropertiesSet()}（当 {@link #initializeSchema} 为 true 时）
     * 与 {@link #deleteByFilter(Filter.Expression)} 消费。可选 —— 接受 null，降级为日志告警。
     */
    private final VikingdbApi controlPlane;
    /** 本 store 操作的 VikingDB collection 名。 */
    @Getter private final String collectionName;
    /** {@link #collectionName} 内的 VikingDB index 名；默认与 collection 同名。 */
    @Getter private final String indexName;
    /** 嵌入向量维度。须与上游 {@code EmbeddingModel} 输出匹配。 */
    @Getter private final int embeddingDimension;
    /**
     * 若为 true，{@link #afterPropertiesSet()} 在启动期创建 collection 与 index。
     * 生产环境通常由外部预配 schema，保持 false。
     */
    private final boolean initializeSchema;
    /** 将 Spring AI 过滤器表达式翻译为 VikingDB 原生谓词。 */
    @Getter private final VikingDbFilterExpressionConverter filterExpressionConverter;
    /** 火山引擎 project 名；{@code null} 时走 SDK 默认 project。 */
    @Getter private final String projectName;
    /** 应用于 collection 与 index 的人类可读描述。 */
    @Getter private final String description;
    /** Index 分片数；{@code null} 时走 VikingDB 服务端默认。 */
    @Getter private final Integer shardCount;
    /** 除向量索引外还须以标量索引支撑的字段名集合。 */
    @Getter private final List<String> scalarIndex;
    /** 文档 metadata 的 schema 声明；未声明的 metadata 键在 upsert 时被拒绝。 */
    @Getter private final Map<String, FieldForCreateVikingdbCollectionInput.FieldTypeEnum> metadataFields;
    /** 预计算的 {@code output_fields} 列表，每个搜索请求都会带上（content + metadata keys）。 */
    @Getter private final List<String> outputFields;

    private VikingDbVectorStore(Builder builder) {
        super(builder);
        this.dataPlane = builder.dataPlane;
        this.controlPlane = builder.controlPlane;
        this.collectionName = builder.collectionName;
        this.indexName = builder.indexName;
        this.embeddingDimension = builder.embeddingDimension;
        this.initializeSchema = builder.initializeSchema;
        this.filterExpressionConverter = builder.filterExpressionConverter;
        this.projectName = builder.projectName;
        this.description = builder.description;
        this.shardCount = builder.shardCount;
        this.scalarIndex = List.copyOf(builder.scalarIndex);
        this.metadataFields = Map.copyOf(builder.metadataFields);
        this.outputFields = outputFields(metadataFields);
        validateConfiguration();
    }

    /**
     * 构造期防御性校验，捕获 schema 设计错误。任何 VikingDB 调用之前完成。
     * 在 {@link #scalarIndex} 与 {@link #metadataFields} 取完防御副本后执行，
     * 保证校验看到的是稳定状态。
     *
     * @throws IllegalArgumentException 当 collection/index 名称为空、维度非正、
     *                                  保留名冲突、metadata 含 VECTOR 类型、或
     *                                  scalarIndex 未在 metadataFields 中声明。
     */
    private void validateConfiguration() {
        Assert.hasText(collectionName, "collectionName must not be blank");
        Assert.hasText(indexName, "indexName must not be blank");
        Assert.isTrue(embeddingDimension > 0, "embeddingDimension must be positive");
        Set<String> reserved = Set.of(DOC_ID_FIELD_NAME, CONTENT_FIELD_NAME, EMBEDDING_FIELD_NAME);
        Set<String> reservedClashes = new LinkedHashSet<>(metadataFields.keySet());
        reservedClashes.retainAll(reserved);
        if (!reservedClashes.isEmpty()) {
            throw new IllegalArgumentException(
                    "metadataFields must not declare reserved field names: " + reservedClashes);
        }
        Set<String> vectorFields = new LinkedHashSet<>();
        metadataFields.forEach((name, type) -> {
            // Vector fields require a per-field dimension that this adapter's metadata
            // schema cannot carry. Reject early so users get a clear error here rather
            // than a VikingDB service-side rejection at create-collection time.
            if (FieldForCreateVikingdbCollectionInput.FieldTypeEnum.VECTOR.equals(type)) {
                vectorFields.add(name);
            }
        });
        if (!vectorFields.isEmpty()) {
            throw new IllegalArgumentException(
                    "metadataFields cannot declare VECTOR type (vector fields require a dimension): "
                            + vectorFields);
        }
        if (!metadataFields.keySet().containsAll(scalarIndex)) {
            throw new IllegalArgumentException("Every scalarIndex field must be declared in metadataFields");
        }
    }

    /**
     * Spring 初始化钩子。当 {@link #initializeSchema} 为 {@code true} 时，
     * 探测控制面的 collection 与 index，按需创建（参见 {@link #ensureCollection()}
     * 与 {@link #ensureIndex()}）。已存在的 collection 会按 {@link #validateCollection} 校验。
     *
     * <p>{@code initializeSchema} 为 {@code false} 时为 no-op —— 这是典型生产姿态，
     * schema 由外部预配。</p>
     *
     * @throws IllegalStateException 当 {@code initializeSchema=true} 但未绑定控制面 SDK。
     * @throws VikingDbVectorStoreException 当任一控制面调用失败或校验拒绝 schema。
     */
    @Override
    public void afterPropertiesSet() {
        if (!initializeSchema) return;
        if (controlPlane == null) {
            throw new IllegalStateException("initializeSchema=true requires a VikingDB control-plane client");
        }
        try {
            ensureCollection();
            ensureIndex();
        } catch (ApiException ex) {
            throw new VikingDbVectorStoreException("schema initialization", collectionName, ex);
        }
    }

    /**
     * get-then-create 模式：探测控制面的 collection，若不存在则按配置的 schema 创建。
     * 已存在的 collection 按 {@link #validateCollection} 校验，杜绝静默漂移。
     *
     * @throws ApiException 当 SDK 调用因 "not found" 之外的任何原因失败时，
     *                      该方法落入创建分支。
     */
    private void ensureCollection() throws ApiException {
        try {
            validateCollection(controlPlane.getVikingdbCollection(collectionRequest()));
            log.info("VikingDB collection '{}' already exists", collectionName);
        } catch (ApiException notFound) {
            if (!isNotFound(notFound)) throw notFound;
            controlPlane.createVikingdbCollection(createCollectionRequest());
            log.info("Created VikingDB collection '{}'", collectionName);
        }
    }

    /**
     * 校验现有 VikingDB collection 是否包含本适配器所需的全部字段：
     * {@link #DOC_ID_FIELD_NAME}（string）、{@link #CONTENT_FIELD_NAME}（string）、
     * {@link #EMBEDDING_FIELD_NAME}（{@link #embeddingDimension} 维度的 vector）、
     * 以及 {@link #metadataFields} 中声明的每一项。
     *
     * @throws IllegalStateException 当任一必填字段缺失、类型错误，或（vector 字段）维度错误。
     */
    private void validateCollection(GetVikingdbCollectionResponse collection) {
        Map<String, com.volcengine.vikingdb.model.FieldForGetVikingdbCollectionOutput> actual = new LinkedHashMap<>();
        if (collection.getFields() != null) {
            collection.getFields().forEach(field -> actual.put(field.getFieldName(), field));
        }
        validateField(actual, DOC_ID_FIELD_NAME, "string", null);
        validateField(actual, CONTENT_FIELD_NAME, "string", null);
        validateField(actual, EMBEDDING_FIELD_NAME, "vector", embeddingDimension);
        metadataFields.forEach((name, type) -> validateField(actual, name, type.getValue(), null));
    }

    /**
     * {@link #validateCollection} 使用的单字段检查。
     *
     * @param dimension vector 字段所需的维度；非 vector 字段传 {@code null} 跳过维度校验。
     */
    private void validateField(Map<String, com.volcengine.vikingdb.model.FieldForGetVikingdbCollectionOutput> fields,
            String name, String type, Integer dimension) {
        var field = fields.get(name);
        if (field == null || field.getFieldType() == null || !type.equals(field.getFieldType().getValue())
                || (dimension != null && !dimension.equals(field.getDim()))) {
            throw new IllegalStateException("Existing VikingDB collection '" + collectionName
                    + "' does not match required field '" + name + "' (type=" + type
                    + (dimension == null ? "" : ", dimension=" + dimension) + ")");
        }
    }

    /**
     * {@link #ensureCollection} 的 index 对应版本。已存在的 index 不做参数校验
     * —— 历史创建的 index 可能合理地演化出本适配器当前不会构建的形式。
     */
    private void ensureIndex() throws ApiException {
        try {
            controlPlane.getVikingdbIndex(indexRequest());
            log.info("VikingDB index '{}/{}' already exists", collectionName, indexName);
        } catch (ApiException notFound) {
            if (!isNotFound(notFound)) throw notFound;
            controlPlane.createVikingdbIndex(createIndexRequest());
            log.info("Created VikingDB index '{}/{}'", collectionName, indexName);
        }
    }

    /**
     * 检测控制面 SDK 的"资源不存在"哨兵。组合结构化 HTTP code（404）与防御性的
     * message 子串检查，因为 SDK 并不总为 not-found 响应设置稳定的 error code。
     */
    private static boolean isNotFound(ApiException exception) {
        if (exception.getCode() == 404) return true;
        String message = exception.getMessage();
        return message != null && (message.contains("NotFound") || message.contains("not found"));
    }

    /** 构造控制面 "get collection" 探测请求；当 {@link #projectName} 非空时附加。 */
    private GetVikingdbCollectionRequest collectionRequest() {
        GetVikingdbCollectionRequest request = new GetVikingdbCollectionRequest().collectionName(collectionName);
        if (projectName != null) request.projectName(projectName);
        return request;
    }

    /** 构造控制面 "get index" 探测请求；当 {@link #projectName} 非空时附加。 */
    private GetVikingdbIndexRequest indexRequest() {
        GetVikingdbIndexRequest request = new GetVikingdbIndexRequest().collectionName(collectionName).indexName(indexName);
        if (projectName != null) request.projectName(projectName);
        return request;
    }

    /**
     * 构造全新的 collection create 请求，包含三个保留字段
     * （{@link #DOC_ID_FIELD_NAME} 主键、{@link #CONTENT_FIELD_NAME} string、
     * {@link #EMBEDDING_FIELD_NAME} {@link #embeddingDimension} 维度的 vector）
     * 以及 {@link #metadataFields} 中声明的每一项。
     */
    private CreateVikingdbCollectionRequest createCollectionRequest() {
        CreateVikingdbCollectionRequest request = new CreateVikingdbCollectionRequest().collectionName(collectionName)
                .addFieldsItem(field(DOC_ID_FIELD_NAME, FieldForCreateVikingdbCollectionInput.FieldTypeEnum.STRING, true))
                .addFieldsItem(field(CONTENT_FIELD_NAME, FieldForCreateVikingdbCollectionInput.FieldTypeEnum.STRING, false))
                .addFieldsItem(new FieldForCreateVikingdbCollectionInput().fieldName(EMBEDDING_FIELD_NAME)
                        .fieldType(FieldForCreateVikingdbCollectionInput.FieldTypeEnum.VECTOR).dim(embeddingDimension));
        metadataFields.forEach((name, type) -> request.addFieldsItem(field(name, type, false)));
        if (projectName != null) request.projectName(projectName);
        if (description != null) request.description(description);
        return request;
    }

    /**
     * collection 字段声明的单行工厂；保留字段与用户声明的 metadata 字段均使用。
     */
    private static FieldForCreateVikingdbCollectionInput field(String name,
            FieldForCreateVikingdbCollectionInput.FieldTypeEnum type, boolean primaryKey) {
        return new FieldForCreateVikingdbCollectionInput().fieldName(name).fieldType(type).isPrimaryKey(primaryKey);
    }

    /**
     * 构造全新的 index create 请求，使用 HNSW + cosine 距离。选择 cosine 与
     * {@link #doSimilaritySearch} 默认 score 解释一致；HNSW 是 VikingDB 对通用
     * 向量负载的推荐默认。
     */
    private CreateVikingdbIndexRequest createIndexRequest() {
        CreateVikingdbIndexRequest request = new CreateVikingdbIndexRequest().collectionName(collectionName).indexName(indexName)
                .vectorIndex(new VectorIndexForCreateVikingdbIndexInput()
                        .indexType(VectorIndexForCreateVikingdbIndexInput.IndexTypeEnum.HNSW)
                        .distance(VectorIndexForCreateVikingdbIndexInput.DistanceEnum.COSINE));
        if (projectName != null) request.projectName(projectName);
        if (description != null) request.description(description);
        if (shardCount != null) request.shardCount(shardCount);
        if (!scalarIndex.isEmpty()) request.scalarIndex(scalarIndex);
        return request;
    }

    /**
     * 嵌入并 upsert 一批文档。文档按 {@link #MAX_UPSERT_BATCH_SIZE} 分块，使每次
     * {@code upsertData} 调用都在 VikingDB 单次上限之内；每个 chunk 依次完成嵌入、
     * 构造记录、发送。
     *
     * @param documents 待摄入的 Spring AI 文档。{@code null} 或空输入为 no-op。
     * @throws IllegalArgumentException 当任一文档携带未声明的 metadata。
     * @throws IllegalStateException   当嵌入模型返回数量与 chunk 大小不匹配（调用方与 provider 不一致）。
     * @throws VikingDbVectorStoreException 当任一 batch upsert 失败。
     */
    @Override
    public void doAdd(@Nonnull List<Document> documents) {
        Assert.notNull(documents, "documents must not be null");
        if (documents.isEmpty()) return;
        for (int from = 0; from < documents.size(); from += MAX_UPSERT_BATCH_SIZE) {
            int to = Math.min(from + MAX_UPSERT_BATCH_SIZE, documents.size());
            List<Document> documentBatch = documents.subList(from, to);
            List<float[]> embeddings = embeddingModel.embed(documentBatch, EmbeddingOptions.builder().build(), batchingStrategy);
            if (embeddings.size() != documentBatch.size()) {
                throw new IllegalStateException("Embedding model returned " + embeddings.size()
                        + " vectors for batch [" + from + ',' + to + ')');
            }
            List<Map<String, Object>> dataBatch = new ArrayList<>(documentBatch.size());
            for (int index = 0; index < documentBatch.size(); index++) {
                dataBatch.add(toVikingRecord(documentBatch.get(index), embeddings.get(index)));
            }
            upsertBatch(dataBatch, from, to);
        }
    }

    /**
     * 为单个 batch 调用一次 {@code upsertData}，并把 SDK 失败翻译为
     * {@link VikingDbVectorStoreException}，错误消息嵌入 batch 区间以便日志定位失败的切片。
     *
     * @param from {@code dataBatch} 在整个文档列表中的起始下标（含）
     * @param to   结束下标（不含），仅用于错误消息
     */
    private void upsertBatch(List<Map<String, Object>> dataBatch, int from, int to) {
        try {
            assertSuccess(dataPlane.upsertData(UpsertDataRequest.builder().collectionName(collectionName)
                    .data(dataBatch).build()), "upsertData");
        } catch (ApiClientException | VectorApiException ex) {
            throw new VikingDbVectorStoreException("upsertData batch [" + from + ',' + to + ')', collectionName, ex);
        }
    }

    /**
     * 为单个文档构造 upsert 记录：保留字段加上文档 metadata 平铺。先对 metadata 键做 schema 校验，
     * 再构造记录，使失败在任何 SDK 调用之前抛出。
     *
     * @throws IllegalArgumentException 当 {@code embedding.length} 与 {@link #embeddingDimension} 不匹配，
     *                                  或任一 metadata 键未在 {@link #metadataFields} 中声明。
     */
    private Map<String, Object> toVikingRecord(Document document, float[] embedding) {
        if (embedding.length != embeddingDimension) {
            throw new IllegalArgumentException("Embedding dimension " + embedding.length + " does not match configured dimension " + embeddingDimension);
        }
        Map<String, Object> metadata = document.getMetadata();
        for (String fieldName : metadata.keySet()) {
            if (!metadataFields.containsKey(fieldName)) {
                throw new IllegalArgumentException("Metadata field is not declared in VikingDB schema: " + fieldName);
            }
        }
        Map<String, Object> record = new LinkedHashMap<>();
        record.put(DOC_ID_FIELD_NAME, document.getId());
        record.put(CONTENT_FIELD_NAME, document.getText() == null ? "" : document.getText());
        record.put(EMBEDDING_FIELD_NAME, floats(embedding));
        record.putAll(metadata);
        return record;
    }

    /**
     * 按主键批量删除文档，分块大小 {@link #MAX_DELETE_BATCH_SIZE}。
     *
     * @param ids 待删除的主键集合。{@code null} 或空输入为 no-op。
     * @throws VikingDbVectorStoreException 当任一 batch delete 失败；部分失败会让数据库进入未定义状态。
     */
    @Override
    public void doDelete(@Nonnull List<String> ids) {
        Assert.notNull(ids, "id list must not be null");
        if (ids.isEmpty()) return;
        for (int from = 0; from < ids.size(); from += MAX_DELETE_BATCH_SIZE) {
            int to = Math.min(from + MAX_DELETE_BATCH_SIZE, ids.size());
            try {
                assertSuccess(dataPlane.deleteData(DeleteDataRequest.builder().collectionName(collectionName)
                        .ids(new ArrayList<Object>(ids.subList(from, to))).build()), "deleteData");
            } catch (ApiClientException | VectorApiException ex) {
                throw new VikingDbVectorStoreException("deleteData batch [" + from + ',' + to + ')', collectionName, ex);
            }
        }
    }

    /**
     * Spring AI filter-delete 入口。委托给 {@link #deleteByFilter(Filter.Expression)}；
     * 返回的 taskId 被丢弃，因为 Spring AI 契约为 {@code void}。
     *
     * @param expression 描述待删文档的 Spring AI filter。
     */
    @Override
    protected void doDelete(@Nonnull Filter.Expression expression) {
        deleteByFilter(expression);
    }

    /**
     * 提交服务端 filter-delete 任务并返回其 taskId。任务异步执行；需要跟踪完成度的调用方
     * 可通过控制面 client 查询该 id。
     *
     * @param expression 待删除文档的 Spring AI filter。
     * @return 服务端异步任务的 taskId。
     * @throws UnsupportedOperationException 当控制面 SDK 不可用。
     * @throws VikingDbVectorStoreException 当控制面调用失败。
     */
    public String deleteByFilter(@Nonnull Filter.Expression expression) {
        if (controlPlane == null) throw new UnsupportedOperationException("Deleting by filter requires a VikingDB control-plane client");
        try {
            CreateVikingdbTaskRequest request = new CreateVikingdbTaskRequest().collectionName(collectionName)
                    .taskType(CreateVikingdbTaskRequest.TaskTypeEnum.FILTER_DELETE)
                    .taskConfig(new TaskConfigForCreateVikingdbTaskInput()
                            .filterConds(List.of(filterExpressionConverter.convert(expression)))
                            // Spring AI's delete contract is fire-and-forget. Do not leave
                            // the VikingDB task pending server-side confirmation.
                            .needConfirm(false));
            if (projectName != null) request.projectName(projectName);
            String taskId = controlPlane.createVikingdbTask(request).getTaskId();
            log.info("Submitted VikingDB filter-delete task: collection={}, taskId={}, needConfirm=false", collectionName, taskId);
            return taskId;
        } catch (ApiException ex) {
            throw new VikingDbVectorStoreException("filterDelete", collectionName, ex);
        }
    }

    /**
     * 嵌入 query、运行向量搜索、按相似度阈值过滤，并将结果物化为 Spring AI {@link Document}。
     * 搜索响应携带 {@link #outputFields}（content + metadata keys），因此每个 hit 已完整填充。
     *
     * @param request Spring AI 搜索请求。必须非空；{@code query} 必须非空字符串。
     * @return 按相似度降序排列的文档，已按 {@link SearchRequest#getSimilarityThreshold()} 过滤。
     * @throws IllegalArgumentException      当 query 嵌入维度与 {@link #embeddingDimension} 不匹配。
     * @throws VikingDbVectorStoreException  当搜索请求失败或 SDK 返回非 Success 响应。
     */
    @Nonnull
    @Override
    public List<Document> doSimilaritySearch(@Nonnull SearchRequest request) {
        Assert.notNull(request, "search request must not be null");
        float[] embedding = embeddingModel.embed(request.getQuery());
        if (embedding.length != embeddingDimension) throw new IllegalArgumentException("Query embedding dimension does not match configured dimension");
        SearchByVectorRequest.SearchByVectorRequestBuilder<?, ?> builder = SearchByVectorRequest.builder()
                .collectionName(collectionName).indexName(indexName).denseVector(floats(embedding)).limit(request.getTopK())
                .outputFields(outputFields);
        if (request.hasFilterExpression()) builder.filter(filterExpressionConverter.convert(request.getFilterExpression()));
        try {
            DataApiResponse<SearchResult> response = dataPlane.searchByVector(builder.build());
            assertSuccess(response, "searchByVector");
            if (response.getResult() == null || response.getResult().getData() == null) return List.of();
            double threshold = request.getSimilarityThreshold();
            return response.getResult().getData().stream()
                    .filter(item -> item.getScore() == null || item.getScore() >= threshold)
                    .map(this::toDocument).toList();
        } catch (ApiClientException | VectorApiException ex) {
            throw new VikingDbVectorStoreException("searchByVector", collectionName, ex);
        }
    }

    /**
     * 构造每个搜索请求使用的不可变 {@code output_fields} 列表。content 排在最前以让调用方
     * 先看到它再是 metadata；使用 {@link LinkedHashSet} 去重，覆盖元数据键与
     * {@link #CONTENT_FIELD_NAME} 同名的极小概率场景。
     *
     * @return 适合缓存到 {@link #outputFields} 的不可变列表。
     */
    private static List<String> outputFields(Map<String, FieldForCreateVikingdbCollectionInput.FieldTypeEnum> metadataFields) {
        Set<String> fields = new LinkedHashSet<>();
        fields.add(CONTENT_FIELD_NAME);
        fields.addAll(metadataFields.keySet());
        return List.copyOf(fields);
    }

    /**
     * 将单个 VikingDB 搜索命中物化为 Spring AI {@link Document}。
     * {@code content} 字段从 metadata 中抽出放入 {@link Document#getText()}，
     * 以便下游嵌入/分块组件消费。
     */
    private Document toDocument(SearchItem item) {
        Map<String, Object> fields = item.getFields() == null ? Map.of() : item.getFields();
        Map<String, Object> metadata = new LinkedHashMap<>(fields);
        Object content = metadata.remove(CONTENT_FIELD_NAME);
        return Document.builder().id(String.valueOf(item.getId())).text(content == null ? "" : String.valueOf(content))
                .metadata(metadata).score(item.getScore() == null ? null : item.getScore().doubleValue()).build();
    }

    /**
     * 将 {@code float[]} 装箱为 {@code List<Float>} 供 VikingDB SDK 使用。
     * SDK 的 {@code denseVector} 字段类型为 {@code List<Float>}，装箱不可避免；
     * 这里只通过精确预分配底层列表大小来避免一层额外分配。
     */
    private static List<Float> floats(float[] values) {
        List<Float> result = new ArrayList<>(values.length);
        for (float value : values) result.add(value);
        return result;
    }

    /**
     * 校验数据面 SDK 的响应封装。VikingDB SDK 在逻辑失败时仍返回 HTTP 200，
     * 因此必须检查 {@code Success} code。
     *
     * @throws VikingDbVectorStoreException 当响应为 null 或 code 非 Success。
     */
    private static void assertSuccess(DataApiResponse<?> response, String operation) {
        if (response == null || !"Success".equalsIgnoreCase(response.getCode())) {
            throw new VikingDbVectorStoreException(operation, null,
                    new IllegalStateException(response == null ? "null response" : response.getMessage()));
        }
    }

    /**
     * 构造 Micrometer observation context，附带 VikingDB provider 名、目标 collection
     * 与 embedding dimension —— 运维在观察 Spring AI vector-store 流量时最常聚合的字段。
     */
    @Nonnull @Override
    public VectorStoreObservationContext.Builder createObservationContextBuilder(@Nonnull String operationName) {
        return VectorStoreObservationContext.builder(VIKINGDB_PROVIDER_NAME, operationName)
                .collectionName(collectionName).dimensions(embeddingDimension);
    }

    /**
     * 暴露底层数据面 {@link VectorService}，供需要绕过本适配器的调用方使用
     * （例如调用 Spring AI 未覆盖的 SDK 专属操作）。
     *
     * @return 持有数据面 client 的非空 Optional。
     */
    @Nonnull public Optional<VectorService> getNativeClient() { return Optional.of(dataPlane); }

    /**
     * {@link VikingDbVectorStore} 的流式 Builder。除构造器要求的 {@code embeddingModel}
     * 与 {@code dataPlane} 外，所有 setter 可选。默认值与 {@link VikingDbVectorStore}
     * 字段文档一致；细节参见各 setter。
     */
    public static final class Builder extends AbstractVectorStoreBuilder<Builder> {
        /** 数据面 SDK；构造器设置，必填且非空。 */
        private final VectorService dataPlane;
        /** 可选控制面 SDK，用于 schema 初始化与 filter-delete。 */
        private VikingdbApi controlPlane;
        /** 默认 {@link VikingDbVectorStore#DEFAULT_COLLECTION_NAME}。 */
        private String collectionName = DEFAULT_COLLECTION_NAME;
        /** 默认与 {@link #collectionName} 相同。 */
        private String indexName = DEFAULT_COLLECTION_NAME;
        /** 默认 {@link VikingDbVectorStore#OPENAI_EMBEDDING_DIMENSION_SIZE}（1536）。 */
        private int embeddingDimension = OPENAI_EMBEDDING_DIMENSION_SIZE;
        /** 默认 {@code false}；设为 true 在启动期启用 schema 初始化。 */
        private boolean initializeSchema;
        /** 默认新建 {@link VikingDbFilterExpressionConverter}；可替换以自定义。 */
        private VikingDbFilterExpressionConverter filterExpressionConverter = new VikingDbFilterExpressionConverter();
        /** 可选 VikingDB project 名；{@code null} 表示默认 project。 */
        private String projectName;
        /** 应用于 collection 与 index 的可选描述。 */
        private String description;
        /** 可选 index 分片数；{@code null} 走 VikingDB 默认。 */
        private Integer shardCount;
        /** 默认空列表；复制在构造器中完成。 */
        private List<String> scalarIndex = List.of();
        /** 默认空 map；复制在构造器中完成。 */
        private Map<String, FieldForCreateVikingdbCollectionInput.FieldTypeEnum> metadataFields = Map.of();

        /**
         * @param embeddingModel Spring AI 嵌入模型，upsert 与 search 使用。
         * @param dataPlane      已初始化的 VikingDB 数据面 SDK client。
         * @throws IllegalArgumentException 当 {@code dataPlane} 为 null。
         */
        public Builder(EmbeddingModel embeddingModel, VectorService dataPlane) {
            super(embeddingModel);
            Assert.notNull(dataPlane, "dataPlane must not be null");
            this.dataPlane = dataPlane;
        }
        /** @param value VikingDB collection 名；默认 {@code "vector_store"}。 */
        public Builder collectionName(String value) { this.collectionName = value; return this; }
        /** @param value VikingDB index 名；默认与 collection 同名。 */
        public Builder indexName(String value) { this.indexName = value; return this; }
        /** @param value 嵌入向量维度；须与上游模型匹配。 */
        public Builder embeddingDimension(int value) { this.embeddingDimension = value; return this; }
        /**
         * @param value {@code true} 在启动期创建 collection 与 index；{@code false} 假设 schema 已存在。
         *              默认 {@code false}。
         */
        public Builder initializeSchema(boolean value) { this.initializeSchema = value; return this; }
        /**
         * @param value 可选控制面 SDK，用于 schema 初始化与服务端 filter-delete。
         *              {@code null} 同时禁用两项能力，仅产生告警日志。
         */
        public Builder controlPlane(VikingdbApi value) { this.controlPlane = value; return this; }
        /** @param value 火山引擎 project 名；{@code null} 表示默认 project。 */
        public Builder projectName(String value) { this.projectName = value; return this; }
        /** @param value 应用于 collection 与 index 的描述。 */
        public Builder description(String value) { this.description = value; return this; }
        /** @param value index 分片数；{@code null} 走 VikingDB 默认。 */
        public Builder shardCount(Integer value) { this.shardCount = value; return this; }
        /**
         * @param value 除向量索引外还需标量索引支撑的字段名。每一项必须在
         *              {@link #metadataFields(Map)} 中声明；在
         *              {@link VikingDbVectorStore#validateConfiguration()} 校验。
         */
        public Builder scalarIndex(List<String> value) { this.scalarIndex = value == null ? List.of() : value; return this; }
        /**
         * @param value 文档 metadata 的 schema 声明。携带未声明键的文档会在 upsert 时被拒绝。
         *              VECTOR 类型条目被拒绝 —— vector 字段需要本 schema 无法承载的 dimension。
         */
        public Builder metadataFields(Map<String, FieldForCreateVikingdbCollectionInput.FieldTypeEnum> value) { this.metadataFields = value == null ? Map.of() : value; return this; }
        /** @param value 自定义 filter 转换器；默认新建 {@link VikingDbFilterExpressionConverter}。 */
        public Builder filterExpressionConverter(VikingDbFilterExpressionConverter value) { this.filterExpressionConverter = value; return this; }
        /** @param value 透传给父类 Spring AI Builder 的 Micrometer observation registry。 */
        @Override public Builder observationRegistry(@Nonnull ObservationRegistry value) { super.observationRegistry(value); return this; }
        /** @param value 透传给父类 Spring AI Builder 的批处理策略。 */
        @Override public Builder batchingStrategy(@Nonnull BatchingStrategy value) { super.batchingStrategy(value); return this; }
        /** @return 一个完整校验、不可变的 {@link VikingDbVectorStore}。 */
        public VikingDbVectorStore build() { return new VikingDbVectorStore(this); }
        /** @return 本 builder，用于满足 Spring AI 自类型父 Builder 契约。 */
        @Override protected Builder self() { return this; }
    }
}