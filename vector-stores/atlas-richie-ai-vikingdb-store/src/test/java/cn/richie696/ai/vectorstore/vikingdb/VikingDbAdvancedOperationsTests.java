package cn.richie696.ai.vectorstore.vikingdb;

import cn.richie696.ai.vectorstore.vikingdb.model.*;
import com.volcengine.vikingdb.runtime.vector.model.request.FetchDataInIndexRequest;
import com.volcengine.vikingdb.runtime.vector.model.request.RerankRequest;
import com.volcengine.vikingdb.runtime.vector.model.request.SearchByKeywordsRequest;
import com.volcengine.vikingdb.runtime.vector.model.request.SearchByMultiModalRequest;
import com.volcengine.vikingdb.runtime.vector.model.response.*;
import com.volcengine.vikingdb.runtime.vector.service.VectorService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VikingDbAdvancedOperationsTests {
    @Test
    void exposesBm25ThroughKeywordSdkCall() throws Exception {
        VectorService dataPlane = mock(VectorService.class);
        when(dataPlane.searchByKeywords(any(SearchByKeywordsRequest.class)))
                .thenReturn(new DataApiResponse<>("kw-1", "Success", null, new SearchResult()));
        VikingDbVectorStore store = new VikingDbVectorStore.Builder(mock(EmbeddingModel.class), dataPlane).build();

        store.search(VikingDbKeywordSearchRequest.builder().mode(VikingDbKeywordSearchRequest.Mode.BM25)
                .query("vector tuning").bm25K1(1.2).bm25B(0.75).build());

        var captor = org.mockito.ArgumentCaptor.forClass(SearchByKeywordsRequest.class);
        verify(dataPlane).searchByKeywords(captor.capture());
        assertThat(captor.getValue().getMode()).isEqualTo("bm25");
        assertThat(captor.getValue().getQuery()).isEqualTo("vector tuning");
        assertThat(captor.getValue().getBm25K1()).isEqualTo(1.2);
        assertThat(captor.getValue().getBm25B()).isEqualTo(0.75);
    }

    @Test
    void exposesMultimodalRequestWithoutLeakingSdkObjectModel() throws Exception {
        VectorService dataPlane = mock(VectorService.class);
        when(dataPlane.searchByMultiModal(any(SearchByMultiModalRequest.class)))
                .thenReturn(new DataApiResponse<>("mm-1", "Success", null, new SearchResult()));
        VikingDbVectorStore store = new VikingDbVectorStore.Builder(mock(EmbeddingModel.class), dataPlane).build();

        store.search(VikingDbMultiModalSearchRequest.builder().text("a cat")
                .image(VikingDbMediaInput.uri("https://example.test/cat.jpg")).build());

        var captor = org.mockito.ArgumentCaptor.forClass(SearchByMultiModalRequest.class);
        verify(dataPlane).searchByMultiModal(captor.capture());
        assertThat(captor.getValue().getText()).isEqualTo("a cat");
        assertThat(captor.getValue().getImage()).isEqualTo("https://example.test/cat.jpg");
    }

    @Test
    void fetchPreservesMissingIds() throws Exception {
        VectorService dataPlane = mock(VectorService.class);
        FetchDataInIndexResult result = new FetchDataInIndexResult(
                List.of(new FetchInIndexItem("id-1", Map.of("content", "hello"), List.of(1f, 2f), 2,
                        Map.of("42", 0.5f))), List.of("missing"));
        when(dataPlane.fetchDataInIndex(any(FetchDataInIndexRequest.class)))
                .thenReturn(new DataApiResponse<>("fetch-1", "Success", null, result));
        VikingDbVectorStore store = new VikingDbVectorStore.Builder(mock(EmbeddingModel.class), dataPlane)
                .embeddingDimension(2).build();

        VikingDbFetchResponse response = store.fetchByIds(List.of("id-1", "missing"), List.of("content"), null);

        assertThat(response.requestId()).isEqualTo("fetch-1");
        assertThat(response.records()).hasSize(1);
        assertThat(response.idsNotExist()).containsExactly("missing");
    }

    @Test
    void exposesStandaloneRerankAndPreservesScore() throws Exception {
        VectorService dataPlane = mock(VectorService.class);
        when(dataPlane.rerank(any(RerankRequest.class))).thenReturn(new DataApiResponse<>("rr-1", "Success", null,
                new RerankResult(List.of(new RerankItem(3, 0.88f, List.of())), Map.of("total", 1))));
        VikingDbVectorStore store = new VikingDbVectorStore.Builder(mock(EmbeddingModel.class), dataPlane).build();

        var response = store.rerank(new VikingDbRerankRequest(null, "gte-rerank", null,
                List.of(List.of(VikingDbMediaInput.text("candidate"))),
                List.of(VikingDbMediaInput.text("query")), null, false, 1));

        assertThat(response.requestId()).isEqualTo("rr-1");
        assertThat(response.hits().get(0).providerIndex()).isEqualTo(3);
        assertThat(response.hits().get(0).score()).isEqualTo(0.88f);
    }
}
