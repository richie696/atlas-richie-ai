package cn.richie696.ai.vectorstore.vikingdb;

import com.volcengine.vikingdb.runtime.vector.model.request.DeleteDataRequest;
import com.volcengine.vikingdb.runtime.vector.model.request.UpsertDataRequest;
import com.volcengine.vikingdb.runtime.vector.model.response.DataApiResponse;
import com.volcengine.vikingdb.runtime.vector.model.response.DeleteDataResult;
import com.volcengine.vikingdb.runtime.vector.model.response.UpsertDataResult;
import com.volcengine.vikingdb.runtime.vector.service.VectorService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VikingDbVectorStoreBatchingTests {

    @Test
    @SuppressWarnings("unchecked")
    void batchesEmbeddingAndUpsertAtTheDataPlaneLimit() throws Exception {
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        VectorService dataPlane = mock(VectorService.class);
        DataApiResponse<UpsertDataResult> response = mock(DataApiResponse.class);
        when(response.getCode()).thenReturn("Success");
        when(dataPlane.upsertData(any(UpsertDataRequest.class))).thenReturn(response);
        when(embeddingModel.embed(anyList(), any(EmbeddingOptions.class), any(TokenCountBatchingStrategy.class)))
                .thenAnswer(invocation -> ((List<?>) invocation.getArgument(0)).stream().map(ignored -> new float[] { 1f, 2f })
                        .toList());

        VikingDbVectorStore store = new VikingDbVectorStore.Builder(embeddingModel, dataPlane)
                .embeddingDimension(2).batchingStrategy(new TokenCountBatchingStrategy()).build();
        List<Document> documents = IntStream.range(0, 1_001)
                .mapToObj(index -> Document.builder().id("id-" + index).text("text").build()).toList();

        store.doAdd(documents);

        ArgumentCaptor<UpsertDataRequest> requests = ArgumentCaptor.forClass(UpsertDataRequest.class);
        verify(dataPlane, times(2)).upsertData(requests.capture());
        assertThat(requests.getAllValues()).extracting(request -> request.getData().size()).containsExactly(1_000, 1);
        verify(embeddingModel, times(2)).embed(anyList(), any(EmbeddingOptions.class), any(TokenCountBatchingStrategy.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void batchesDeleteIdsAtTheDataPlaneLimit() throws Exception {
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        VectorService dataPlane = mock(VectorService.class);
        DataApiResponse<DeleteDataResult> response = mock(DataApiResponse.class);
        when(response.getCode()).thenReturn("Success");
        when(dataPlane.deleteData(any(DeleteDataRequest.class))).thenReturn(response);
        VikingDbVectorStore store = new VikingDbVectorStore.Builder(embeddingModel, dataPlane).build();

        store.doDelete(IntStream.range(0, 1_001).mapToObj(index -> "id-" + index).toList());

        ArgumentCaptor<DeleteDataRequest> requests = ArgumentCaptor.forClass(DeleteDataRequest.class);
        verify(dataPlane, times(2)).deleteData(requests.capture());
        assertThat(requests.getAllValues()).extracting(request -> request.getIds().size()).containsExactly(1_000, 1);
    }
}
