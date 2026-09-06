package cn.richie696.ai.vectorstore.vikingdb.internal;

import cn.richie696.ai.vectorstore.vikingdb.VikingDbFilterExpressionConverter;
import cn.richie696.ai.vectorstore.vikingdb.model.*;
import com.volcengine.vikingdb.runtime.vector.model.request.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Maps stable library requests to the pinned VikingDB data-plane SDK.
 */
public final class VikingDbSearchRequestMapper {
    private final VikingDbFilterExpressionConverter filterConverter;

    public VikingDbSearchRequestMapper(VikingDbFilterExpressionConverter filterConverter) {
        this.filterConverter = filterConverter;
    }

    public SearchByVectorRequest map(VikingDbVectorSearchRequest request,
                                     VikingDbResourceRef target,
                                     VikingDbSearchCommonOptions common,
                                     VikingDbSearchAdvanceOptions advance,
                                     List<String> defaultOutputFields) {
        SearchByVectorRequest.SearchByVectorRequestBuilder<?, ?> builder = SearchByVectorRequest.builder()
                .collectionName(target.collectionName()).indexName(target.indexName());
        applyCommon(builder, common, defaultOutputFields);
        if (request.denseVector() != null) builder.denseVector(floats(request.denseVector()));
        if (!request.sparseVector().isEmpty()) builder.sparseVector(request.sparseVector());
        if (request.mandatoryFilter() != null || request.queryFilter() != null) {
            builder.filter(filterConverter.convert(filterConverter.combineAnd(
                    request.mandatoryFilter(), request.queryFilter())));
        }
        if (!advance.isEmpty()) builder.advance(toSdkAdvance(advance));
        if (request.tensorRerank() != null) builder.tensorRerank(toSdkTensorRerank(request.tensorRerank()));
        return builder.build();
    }

    public SearchByKeywordsRequest map(VikingDbKeywordSearchRequest request,
                                       VikingDbResourceRef target,
                                       VikingDbSearchCommonOptions common,
                                       VikingDbSearchAdvanceOptions advance,
                                       List<String> defaultOutputFields) {
        SearchByKeywordsRequest.SearchByKeywordsRequestBuilder<?, ?> builder = SearchByKeywordsRequest.builder()
                .collectionName(target.collectionName()).indexName(target.indexName())
                .mode(request.mode().wireValue()).query(request.query())
                .keywords(request.keywords()).fields(request.fields())
                .caseSensitive(request.caseSensitive()).bm25K1(request.bm25K1()).bm25B(request.bm25B());
        applyCommon(builder, common, defaultOutputFields);
        if (request.mandatoryFilter() != null || request.queryFilter() != null) {
            builder.filter(filterConverter.convert(filterConverter.combineAnd(
                    request.mandatoryFilter(), request.queryFilter())));
        }
        if (!advance.isEmpty()) builder.advance(toSdkAdvance(advance));
        return builder.build();
    }

    public SearchByMultiModalRequest map(VikingDbMultiModalSearchRequest request,
                                         VikingDbResourceRef target,
                                         VikingDbSearchCommonOptions common,
                                         VikingDbSearchAdvanceOptions advance,
                                         List<String> defaultOutputFields) {
        SearchByMultiModalRequest.SearchByMultiModalRequestBuilder<?, ?> builder = SearchByMultiModalRequest.builder()
                .collectionName(target.collectionName()).indexName(target.indexName()).text(request.text())
                .image(nativeValue(request.image())).video(nativeValue(request.video()));
        if (request.instruction() != null) builder.needInstruction(true)
                .instruction(new SearchInstruction(request.instruction().autoFill()));
        if (request.tensorRerank() != null) builder.tensorRerank(toSdkTensorRerank(request.tensorRerank()));
        if (request.modelRerank() != null) builder.rerank(toSdkModelRerank(request.modelRerank()));
        applyCommon(builder, common, defaultOutputFields);
        if (request.mandatoryFilter() != null || request.queryFilter() != null) {
            builder.filter(filterConverter.convert(filterConverter.combineAnd(
                    request.mandatoryFilter(), request.queryFilter())));
        }
        if (!advance.isEmpty()) builder.advance(toSdkAdvance(advance));
        return builder.build();
    }

    private static void applyCommon(BaseSearchRequest.BaseSearchRequestBuilder<?, ?> builder,
                                    VikingDbSearchCommonOptions common, List<String> defaults) {
        List<String> outputFields = common.outputFields() == null ? defaults : common.outputFields();
        if (outputFields != null) builder.outputFields(outputFields);
        if (common.limit() != null) builder.limit(common.limit());
        if (common.offset() != null) builder.offset(common.offset());
        if (common.partition() != null) builder.partition(common.partition());
        if (common.returnSchema() != null) builder.returnSchema(common.returnSchema());
        if (common.returnDownloadUrl() != null) builder.returnDownloadUrl(common.returnDownloadUrl());
        if (common.returnAnalyzedResult() != null) builder.returnAnalyzedResult(common.returnAnalyzedResult());
        if (common.returnDetailInfo() != null) builder.returnDetailInfo(common.returnDetailInfo());
    }

    private static SearchAdvance toSdkAdvance(VikingDbSearchAdvanceOptions value) {
        SearchAdvance advance = new SearchAdvance();
        advance.setDenseWeight(value.denseWeight());
        advance.setIdsIn(new ArrayList<>(value.idsIn()));
        advance.setIdsNotIn(new ArrayList<>(value.idsNotIn()));
        if (!value.postProcessOperations().isEmpty()) {
            advance.setPostProcessOps(value.postProcessOperations().stream()
                    .map(operation -> operation.toNativeMap()).map(Map::<String, Object>copyOf).toList());
        }
        advance.setPostProcessInputLimit(value.postProcessInputLimit());
        advance.setScaleK(value.scaleK());
        advance.setFilterPreAnnLimit(value.filterPreAnnLimit());
        advance.setFilterPreAnnRatio(value.filterPreAnnRatio());
        return advance;
    }

    private static TensorRerank toSdkTensorRerank(VikingDbTensorRerankOptions value) {
        return new TensorRerank(value.tensor(), value.inputLimit(), value.maxSimilarityAlgo());
    }

    private static ModelRerank toSdkModelRerank(VikingDbModelRerankOptions value) {
        return new ModelRerank(value.modelName(), value.modelVersion(), value.instruction(), value.inputLimit(),
                value.scoreThreshold(), value.failStrategy(), value.timeoutMs());
    }

    private static Object nativeValue(VikingDbMediaInput value) {
        return value == null ? null : value.nativeValue();
    }

    private static List<Float> floats(float[] values) {
        List<Float> result = new ArrayList<>(values.length);
        for (float value : values) result.add(value);
        return result;
    }
}
