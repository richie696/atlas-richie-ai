package cn.richie696.ai.vectorstore.vikingdb;

import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbFilterValidationMode;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbSearchAdvanceOptions;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbVectorSearchRequest;
import com.volcengine.vikingdb.runtime.vector.model.request.SearchByVectorRequest;
import com.volcengine.vikingdb.runtime.vector.model.response.DataApiResponse;
import com.volcengine.vikingdb.runtime.vector.model.response.SearchItem;
import com.volcengine.vikingdb.runtime.vector.model.response.SearchResult;
import com.volcengine.vikingdb.runtime.vector.service.VectorService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VikingDbAdvancedSearchTests {

    @Test
    void sendsDenseAndSparseInOneHybridRequestAndPreservesEvidence() throws Exception {
        VectorService dataPlane = mock(VectorService.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        SearchResult result = new SearchResult(List.of(new SearchItem("id-1", Map.of("content", "ok"),
                0.9f, 0.8f, 0.7f, 0.1f)), Map.of(), null, 1, 1,
                null, null, null, null, null, 12, 23, 4);
        when(dataPlane.searchByVector(any(SearchByVectorRequest.class)))
                .thenReturn(new DataApiResponse<>("req-1", "Success", null, result));

        VikingDbVectorStore store = new VikingDbVectorStore.Builder(embeddingModel, dataPlane)
                .embeddingDimension(2)
                .metadataFields(Map.of("tenant", com.volcengine.vikingdb.model.FieldForCreateVikingdbCollectionInput.FieldTypeEnum.STRING))
                .scalarIndex(List.of("tenant"))
                .filterValidationMode(VikingDbFilterValidationMode.SCALAR_INDEXED_ONLY)
                .build();
        Filter.Expression mandatory = new Filter.Expression(Filter.ExpressionType.EQ,
                new Filter.Key("tenant"), new Filter.Value("acme"));
        VikingDbVectorSearchRequest request = VikingDbVectorSearchRequest.builder()
                .mode(VikingDbVectorSearchRequest.Mode.HYBRID)
                .denseVector(new float[]{1f, 2f})
                .sparseVector(Map.of("42", 0.5f))
                .mandatoryFilter(mandatory)
                .advance(VikingDbSearchAdvanceOptions.builder().denseWeight(0.6).build())
                .build();

        var response = store.search(request);
        var captor = org.mockito.ArgumentCaptor.forClass(SearchByVectorRequest.class);
        verify(dataPlane).searchByVector(captor.capture());
        SearchByVectorRequest nativeRequest = captor.getValue();
        assertThat(nativeRequest.getDenseVector()).containsExactly(1f, 2f);
        assertThat(nativeRequest.getSparseVector()).containsEntry("42", 0.5f);
        assertThat(nativeRequest.getAdvance().getDenseWeight()).isEqualTo(0.6);
        assertThat(nativeRequest.getFilter()).containsEntry("field", "tenant");
        assertThat(response.execution().requestId()).isEqualTo("req-1");
        assertThat(response.execution().mandatoryFilterApplied()).isTrue();
        assertThat(response.hits().get(0).annScore()).isCloseTo(0.8, org.assertj.core.data.Offset.offset(0.00001));
        assertThat(response.hits().get(0).originScore()).isCloseTo(0.7, org.assertj.core.data.Offset.offset(0.00001));
    }

    @Test
    void rejectsHybridRequestBeforeCallingProviderWhenVectorIsMissing() throws Exception {
        VectorService dataPlane = mock(VectorService.class);
        VikingDbVectorStore store = new VikingDbVectorStore.Builder(mock(EmbeddingModel.class), dataPlane)
                .embeddingDimension(2).build();

        VikingDbVectorSearchRequest request = VikingDbVectorSearchRequest.builder()
                .mode(VikingDbVectorSearchRequest.Mode.HYBRID)
                .denseVector(new float[]{1f, 2f}).build();

        assertThatThrownBy(() -> store.search(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sparseVector");
        org.mockito.Mockito.verifyNoInteractions(dataPlane);
    }
}
