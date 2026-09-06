package cn.richie696.ai.vectorstore.vikingdb;

import cn.richie696.ai.vectorstore.vikingdb.internal.VikingDbIndexRequestMapper;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbIndexDefinition;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbIndexVectorOptions;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbResourceRef;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VikingDbIndexRequestMapperTests {
    @Test
    void mapsHybridDisklessIndexAndParametersToControlPlaneRequest() {
        var definition = VikingDbIndexDefinition.builder()
                .target(new VikingDbResourceRef("project", "collection", "index"))
                .vector(VikingDbIndexVectorOptions.builder().type(VikingDbIndexVectorOptions.Type.HNSW_HYBRID)
                        .distance(VikingDbIndexVectorOptions.Distance.IP).hnswM(32).hnswCef(128).hnswSef(64).build())
                .build();

        var request = VikingDbIndexRequestMapper.mapCreate(definition);
        assertThat(request.getVectorIndex().getIndexType().getValue()).isEqualTo("hnsw_hybrid");
        assertThat(request.getVectorIndex().getDistance().getValue()).isEqualTo("ip");
        assertThat(request.getVectorIndex().getHnswM()).isEqualTo(32);
        assertThat(request.getVectorIndex().getHnswCef()).isEqualTo(128);
        assertThat(request.getVectorIndex().getHnswSef()).isEqualTo(64);
    }

    @Test
    void rejectsHnswParameterOnFlatIndex() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> VikingDbIndexVectorOptions.builder()
                        .type(VikingDbIndexVectorOptions.Type.FLAT).hnswM(16).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HNSW");
    }
}
