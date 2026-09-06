package cn.richie696.ai.vectorstore.vikingdb.internal;

import cn.richie696.ai.vectorstore.vikingdb.VikingDbVectorStoreException;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbMediaInput;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbRerankHit;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbRerankRequest;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbRerankResponse;
import com.volcengine.vikingdb.runtime.exception.ApiClientException;
import com.volcengine.vikingdb.runtime.exception.VectorApiException;
import com.volcengine.vikingdb.runtime.vector.model.request.FullModalData;
import com.volcengine.vikingdb.runtime.vector.model.request.RerankRequest;
import com.volcengine.vikingdb.runtime.vector.model.response.DataApiResponse;
import com.volcengine.vikingdb.runtime.vector.model.response.RerankResult;
import com.volcengine.vikingdb.runtime.vector.service.VectorService;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;

/**
 * Executes the standalone VikingDB model-rerank API without exposing SDK DTOs.
 */
public final class VikingDbRerankExecutor {
    private final VectorService dataPlane;

    public VikingDbRerankExecutor(VectorService dataPlane) {
        this.dataPlane = dataPlane;
    }

    public VikingDbRerankResponse execute(VikingDbRerankRequest request) {
        Assert.notNull(request, "rerank request must not be null");
        Assert.notEmpty(request.data(), "rerank data must not be empty");
        Assert.notEmpty(request.query(), "rerank query must not be empty");
        RerankRequest nativeRequest = RerankRequest.builder()
                .projectName(request.projectName()).modelName(request.modelName()).modelVersion(request.modelVersion())
                .data(request.data().stream().map(this::modalData).toList())
                .query(modalData(request.query())).instruction(request.instruction())
                .returnOriginData(request.returnOriginData()).maxRetryTime(request.maxRetryTime()).build();
        try {
            DataApiResponse<RerankResult> response = dataPlane.rerank(nativeRequest);
            if (response == null || !"Success".equalsIgnoreCase(response.getCode())) {
                throw new VikingDbVectorStoreException("rerank", null,
                        new IllegalStateException(response == null ? "null response" : response.getMessage()));
            }
            RerankResult result = response.getResult();
            List<VikingDbRerankHit> hits = result == null || result.getData() == null ? List.of()
                    : result.getData().stream().map(item -> new VikingDbRerankHit(item.getId(), item.getScore(),
                    item.getOriginData() == null ? List.of() : item.getOriginData().stream()
                            .map(this::mediaData).filter(Objects::nonNull).toList())).toList();
            return new VikingDbRerankResponse(response.getRequestId(), hits,
                    result == null ? null : result.getTokenUsage());
        } catch (ApiClientException | VectorApiException ex) {
            throw new VikingDbVectorStoreException("rerank", null, ex);
        }
    }

    private List<FullModalData> modalData(List<VikingDbMediaInput> values) {
        return values.stream().map(value -> switch (value.kind()) {
            case TEXT -> new FullModalData(value.text(), null, null);
            case URI, BYTES -> new FullModalData(null, value.nativeValue(), null);
        }).toList();
    }

    private VikingDbMediaInput mediaData(com.volcengine.vikingdb.runtime.vector.model.response.FullModalData value) {
        if (value.getText() != null) return VikingDbMediaInput.text(value.getText());
        if (value.getImage() != null) return VikingDbMediaInput.uri(String.valueOf(value.getImage()));
        if (value.getVideo() != null) return VikingDbMediaInput.uri(String.valueOf(value.getVideo()));
        return null;
    }
}
