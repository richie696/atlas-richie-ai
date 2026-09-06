package cn.richie696.ai.vectorstore.vikingdb.internal;

import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbSearchExecutionEvidence;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbSearchHit;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbSearchResponse;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbVectorSearchRequest;
import com.volcengine.vikingdb.runtime.vector.model.response.DataApiResponse;
import com.volcengine.vikingdb.runtime.vector.model.response.SearchItem;
import com.volcengine.vikingdb.runtime.vector.model.response.SearchResult;
import org.springframework.ai.vectorstore.filter.Filter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps SDK search results and creates redacted execution evidence.
 */
public final class VikingDbSearchResponseMapper {
    public VikingDbSearchResponse map(DataApiResponse<SearchResult> response,
                                      VikingDbVectorSearchRequest request,
                                      Map<String, Object> effectiveOptions) {
        return map(response, request.mode().name(), request.mandatoryFilter(), request.queryFilter(), effectiveOptions);
    }

    public VikingDbSearchResponse map(DataApiResponse<SearchResult> response,
                                      String searchMode, Filter.Expression mandatoryFilter,
                                      Filter.Expression queryFilter, Map<String, Object> effectiveOptions) {
        SearchResult result = response.getResult();
        List<VikingDbSearchHit> hits = result == null || result.getData() == null ? List.of()
                : result.getData().stream().map(this::hit).toList();
        VikingDbSearchExecutionEvidence evidence = new VikingDbSearchExecutionEvidence(
                response.getRequestId(), response.getCode(), response.getMessage(), searchMode,
                hits.size(), result == null ? null : result.getFilterMatchedCount(),
                result == null ? null : result.getTotalReturnCount(),
                result == null ? null : result.getEmbeddingTimeCostMs(),
                result == null ? null : result.getRecallTimeCostMs(),
                result == null ? null : result.getRerankTimeCostMs(),
                result == null ? null : result.getRerankError(),
                mandatoryFilter != null,
                digest(mandatoryFilter, queryFilter), effectiveOptions);
        return new VikingDbSearchResponse(hits, evidence);
    }

    private VikingDbSearchHit hit(SearchItem item) {
        Map<String, Object> fields = item.getFields() == null ? Map.of() : new LinkedHashMap<>(item.getFields());
        return new VikingDbSearchHit(String.valueOf(item.getId()), fields,
                number(item.getScore()), number(item.getAnnScore()),
                number(item.getOriginScore()), number(item.getAdditionScore()));
    }

    private static Double number(Float value) {
        return value == null ? null : value.doubleValue();
    }

    private static String digest(Filter.Expression mandatoryFilter, Filter.Expression queryFilter) {
        if (mandatoryFilter == null && queryFilter == null) return null;
        String value = String.valueOf(mandatoryFilter) + "|" + String.valueOf(queryFilter);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
