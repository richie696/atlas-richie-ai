package cn.richie696.ai.vectorstore.vikingdb.internal;

import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbIndexDefinition;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbIndexVectorOptions;
import com.volcengine.vikingdb.model.CreateVikingdbIndexRequest;
import com.volcengine.vikingdb.model.VectorIndexForCreateVikingdbIndexInput;

import java.util.Locale;

/**
 * Maps stable index definitions to the current control-plane SDK.
 */
public final class VikingDbIndexRequestMapper {
    private VikingDbIndexRequestMapper() {
    }

    public static CreateVikingdbIndexRequest mapCreate(VikingDbIndexDefinition definition) {
        var target = definition.target();
        CreateVikingdbIndexRequest request = new CreateVikingdbIndexRequest()
                .collectionName(target.collectionName()).indexName(target.indexName())
                .vectorIndex(mapVector(definition.vector()));
        if (target.projectName() != null) request.projectName(target.projectName());
        if (definition.description() != null) request.description(definition.description());
        if (definition.shardCount() != null) request.shardCount(definition.shardCount());
        if (definition.scalarIndexes() != null && !definition.scalarIndexes().isEmpty())
            request.scalarIndex(definition.scalarIndexes());
        if (definition.cpuQuota() != null) request.cpuQuota(definition.cpuQuota());
        if (definition.resourceId() != null) request.resourceId(definition.resourceId());
        if (definition.deletionProtection() != null) request.delProtection(definition.deletionProtection());
        if (definition.shardPolicy() != null)
            request.shardPolicy(CreateVikingdbIndexRequest.ShardPolicyEnum.fromValue(definition.shardPolicy()));
        return request;
    }

    public static VectorIndexForCreateVikingdbIndexInput mapVector(VikingDbIndexVectorOptions value) {
        VectorIndexForCreateVikingdbIndexInput result = new VectorIndexForCreateVikingdbIndexInput()
                .indexType(VectorIndexForCreateVikingdbIndexInput.IndexTypeEnum.fromValue(value.type().name().toLowerCase(Locale.ROOT)))
                .distance(VectorIndexForCreateVikingdbIndexInput.DistanceEnum.fromValue(value.distance().name().toLowerCase(Locale.ROOT)))
                .quant(VectorIndexForCreateVikingdbIndexInput.QuantEnum.fromValue(value.quantization().name().toLowerCase(Locale.ROOT)));
        if (value.hnswM() != null) result.hnswM(value.hnswM());
        if (value.hnswCef() != null) result.hnswCef(value.hnswCef());
        if (value.hnswSef() != null) result.hnswSef(value.hnswSef());
        if (value.diskannM() != null) result.diskannM(value.diskannM());
        if (value.diskannCef() != null) result.diskannCef(value.diskannCef());
        if (value.cacheRatio() != null) result.cacheRatio(value.cacheRatio());
        if (value.pqCodeRatio() != null) result.pqCodeRatio(value.pqCodeRatio());
        return result;
    }
}
