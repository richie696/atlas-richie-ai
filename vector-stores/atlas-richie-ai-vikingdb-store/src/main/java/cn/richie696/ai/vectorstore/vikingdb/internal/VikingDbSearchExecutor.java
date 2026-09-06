package cn.richie696.ai.vectorstore.vikingdb.internal;

import cn.richie696.ai.vectorstore.vikingdb.VikingDbFilterExpressionConverter;
import cn.richie696.ai.vectorstore.vikingdb.VikingDbVectorStoreException;
import cn.richie696.ai.vectorstore.vikingdb.model.*;
import com.volcengine.vikingdb.runtime.exception.ApiClientException;
import com.volcengine.vikingdb.runtime.exception.VectorApiException;
import com.volcengine.vikingdb.runtime.vector.model.request.SearchByKeywordsRequest;
import com.volcengine.vikingdb.runtime.vector.model.request.SearchByMultiModalRequest;
import com.volcengine.vikingdb.runtime.vector.model.response.DataApiResponse;
import com.volcengine.vikingdb.runtime.vector.model.response.SearchResult;
import com.volcengine.vikingdb.runtime.vector.service.VectorService;
import org.springframework.util.Assert;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One execution path for dense, sparse and hybrid searches.
 */
public final class VikingDbSearchExecutor {
    private final VectorService dataPlane;
    private final VikingDbResourceRef boundTarget;
    private final List<String> outputFields;
    private final VikingDbSearchCommonOptions defaults;
    private final VikingDbSearchAdvanceOptions advanceDefaults;
    private final VikingDbSearchValidator validator;
    private final VikingDbSearchRequestMapper requestMapper;
    private final VikingDbSearchResponseMapper responseMapper = new VikingDbSearchResponseMapper();

    public VikingDbSearchExecutor(VectorService dataPlane, VikingDbResourceRef boundTarget,
                                  List<String> outputFields, VikingDbSearchCommonOptions defaults,
                                  VikingDbSearchAdvanceOptions advanceDefaults, int embeddingDimension,
                                  Map<String, ?> metadataFields, Set<String> scalarIndex,
                                  VikingDbFilterValidationMode filterMode,
                                  VikingDbFilterExpressionConverter converter) {
        this.dataPlane = dataPlane;
        this.boundTarget = boundTarget;
        this.outputFields = List.copyOf(outputFields);
        this.defaults = defaults == null ? VikingDbSearchCommonOptions.empty() : defaults;
        this.advanceDefaults = advanceDefaults == null ? VikingDbSearchAdvanceOptions.empty() : advanceDefaults;
        this.validator = new VikingDbSearchValidator(embeddingDimension, metadataFields, scalarIndex, filterMode);
        this.requestMapper = new VikingDbSearchRequestMapper(converter);
    }

    public VikingDbSearchResponse execute(VikingDbVectorSearchRequest request) {
        validator.validate(request);
        validateTarget(request.target());
        VikingDbSearchCommonOptions common = merge(defaults, request.common());
        VikingDbSearchAdvanceOptions advance = merge(advanceDefaults, request.advance());
        validator.validateCommon(common);
        validator.validateAdvance(advance, request.mode());
        var nativeRequest = requestMapper.map(request, boundTarget, common, advance, outputFields);
        Map<String, Object> effective = effectiveOptions(request, common, advance);
        try {
            DataApiResponse<SearchResult> response = dataPlane.searchByVector(nativeRequest);
            assertSuccess(response, "searchByVector");
            return responseMapper.map(response, request, effective);
        } catch (ApiClientException | VectorApiException ex) {
            throw new VikingDbVectorStoreException("searchByVector", boundTarget.collectionName(), ex);
        }
    }

    public VikingDbSearchResponse execute(VikingDbKeywordSearchRequest request) {
        Assert.notNull(request, "keyword search request must not be null");
        Assert.notNull(request.mode(), "keyword search mode must not be null");
        if (request.mode() == VikingDbKeywordSearchRequest.Mode.BM25) {
            Assert.hasText(request.query(), "BM25 query must not be blank");
        }
        validateTarget(request.target());
        VikingDbSearchCommonOptions common = merge(defaults, request.common());
        VikingDbSearchAdvanceOptions advance = merge(advanceDefaults, request.advance());
        validator.validateCommon(common);
        validator.validateKeywordFields(request.fields());
        validator.validateAdvance(advance, VikingDbVectorSearchRequest.Mode.DENSE);
        validator.validateFilters(request.mandatoryFilter(), request.queryFilter());
        SearchByKeywordsRequest nativeRequest = requestMapper.map(request, boundTarget, common, advance, outputFields);
        Map<String, Object> effective = effectiveOptions(request.mode().name(), request.mandatoryFilter(), common, advance);
        try {
            DataApiResponse<SearchResult> response = dataPlane.searchByKeywords(nativeRequest);
            assertSuccess(response, "searchByKeywords");
            return responseMapper.map(response, request.mode().name(), request.mandatoryFilter(),
                    request.queryFilter(), effective);
        } catch (ApiClientException | VectorApiException ex) {
            throw new VikingDbVectorStoreException("searchByKeywords", boundTarget.collectionName(), ex);
        }
    }

    public VikingDbSearchResponse execute(VikingDbMultiModalSearchRequest request) {
        Assert.notNull(request, "multimodal search request must not be null");
        Assert.isTrue(request.text() != null || request.image() != null || request.video() != null,
                "multimodal search requires text, image or video");
        validateTarget(request.target());
        VikingDbSearchCommonOptions common = merge(defaults, request.common());
        VikingDbSearchAdvanceOptions advance = merge(advanceDefaults, request.advance());
        validator.validateCommon(common);
        validator.validateAdvance(advance, VikingDbVectorSearchRequest.Mode.DENSE);
        validator.validateFilters(request.mandatoryFilter(), request.queryFilter());
        SearchByMultiModalRequest nativeRequest = requestMapper.map(request, boundTarget, common, advance, outputFields);
        Map<String, Object> effective = effectiveOptions("MULTIMODAL", request.mandatoryFilter(), common, advance);
        try {
            DataApiResponse<SearchResult> response = dataPlane.searchByMultiModal(nativeRequest);
            assertSuccess(response, "searchByMultiModal");
            return responseMapper.map(response, "MULTIMODAL", request.mandatoryFilter(),
                    request.queryFilter(), effective);
        } catch (ApiClientException | VectorApiException ex) {
            throw new VikingDbVectorStoreException("searchByMultiModal", boundTarget.collectionName(), ex);
        }
    }

    private void validateTarget(VikingDbResourceRef target) {
        if (target == null) return;
        if (!boundTarget.equals(target)) {
            throw new IllegalArgumentException("Search target does not match the bound VikingDB Store");
        }
    }

    private static VikingDbSearchCommonOptions merge(VikingDbSearchCommonOptions defaults,
                                                     VikingDbSearchCommonOptions request) {
        VikingDbSearchCommonOptions r = request == null ? VikingDbSearchCommonOptions.empty() : request;
        return VikingDbSearchCommonOptions.builder()
                .limit(r.limit() != null ? r.limit() : defaults.limit())
                .offset(r.offset() != null ? r.offset() : defaults.offset())
                .partition(r.partition() != null ? r.partition() : defaults.partition())
                .outputFields(r.outputFields() != null ? r.outputFields() : defaults.outputFields())
                .returnSchema(r.returnSchema() != null ? r.returnSchema() : defaults.returnSchema())
                .returnDownloadUrl(r.returnDownloadUrl() != null ? r.returnDownloadUrl() : defaults.returnDownloadUrl())
                .returnAnalyzedResult(r.returnAnalyzedResult() != null ? r.returnAnalyzedResult() : defaults.returnAnalyzedResult())
                .returnDetailInfo(r.returnDetailInfo() != null ? r.returnDetailInfo() : defaults.returnDetailInfo())
                .build();
    }

    private static VikingDbSearchAdvanceOptions merge(VikingDbSearchAdvanceOptions defaults,
                                                      VikingDbSearchAdvanceOptions request) {
        VikingDbSearchAdvanceOptions r = request == null ? VikingDbSearchAdvanceOptions.empty() : request;
        return VikingDbSearchAdvanceOptions.builder()
                .denseWeight(r.denseWeight() != null ? r.denseWeight() : defaults.denseWeight())
                .idsIn(!r.idsIn().isEmpty() ? r.idsIn() : defaults.idsIn())
                .idsNotIn(!r.idsNotIn().isEmpty() ? r.idsNotIn() : defaults.idsNotIn())
                .postProcessOperations(!r.postProcessOperations().isEmpty()
                        ? r.postProcessOperations() : defaults.postProcessOperations())
                .postProcessInputLimit(r.postProcessInputLimit() != null
                        ? r.postProcessInputLimit() : defaults.postProcessInputLimit())
                .scaleK(r.scaleK() != null ? r.scaleK() : defaults.scaleK())
                .filterPreAnnLimit(r.filterPreAnnLimit() != null
                        ? r.filterPreAnnLimit() : defaults.filterPreAnnLimit())
                .filterPreAnnRatio(r.filterPreAnnRatio() != null
                        ? r.filterPreAnnRatio() : defaults.filterPreAnnRatio())
                .build();
    }

    private static Map<String, Object> effectiveOptions(VikingDbVectorSearchRequest request,
                                                        VikingDbSearchCommonOptions common,
                                                        VikingDbSearchAdvanceOptions advance) {
        return effectiveOptions(request.mode().name(), request.mandatoryFilter(), common, advance);
    }

    private static Map<String, Object> effectiveOptions(String mode,
                                                        org.springframework.ai.vectorstore.filter.Filter.Expression mandatoryFilter,
                                                        VikingDbSearchCommonOptions common,
                                                        VikingDbSearchAdvanceOptions advance) {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("mode", mode);
        if (common.limit() != null) options.put("limit", common.limit());
        if (common.offset() != null) options.put("offset", common.offset());
        if (common.partition() != null) options.put("partition", common.partition());
        if (advance.denseWeight() != null) options.put("denseWeight", advance.denseWeight());
        if (!advance.idsIn().isEmpty()) options.put("idsInCount", advance.idsIn().size());
        if (!advance.idsNotIn().isEmpty()) options.put("idsNotInCount", advance.idsNotIn().size());
        if (!advance.postProcessOperations().isEmpty())
            options.put("postProcessCount", advance.postProcessOperations().size());
        if (advance.postProcessInputLimit() != null)
            options.put("postProcessInputLimit", advance.postProcessInputLimit());
        if (advance.scaleK() != null) options.put("scaleK", advance.scaleK());
        if (advance.filterPreAnnLimit() != null) options.put("filterPreAnnLimit", advance.filterPreAnnLimit());
        if (advance.filterPreAnnRatio() != null) options.put("filterPreAnnRatio", advance.filterPreAnnRatio());
        options.put("mandatoryFilterPresent", mandatoryFilter != null);
        return options;
    }

    private static void assertSuccess(DataApiResponse<?> response, String operation) {
        if (response == null || !"Success".equalsIgnoreCase(response.getCode())) {
            throw new VikingDbVectorStoreException(operation, null,
                    new IllegalStateException(response == null ? "null response" : response.getMessage()));
        }
    }
}
