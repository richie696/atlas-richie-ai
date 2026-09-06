package cn.richie696.ai.vectorstore.vikingdb.index;

import cn.richie696.ai.vectorstore.vikingdb.VikingDbVectorStoreException;
import cn.richie696.ai.vectorstore.vikingdb.api.VikingDbIndexOperations;
import cn.richie696.ai.vectorstore.vikingdb.internal.VikingDbIndexRequestMapper;
import cn.richie696.ai.vectorstore.vikingdb.model.*;
import com.volcengine.ApiException;
import com.volcengine.vikingdb.VikingdbApi;
import com.volcengine.vikingdb.model.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Explicit, non-destructive-by-default VikingDB index lifecycle facade.
 */
public final class VikingDbIndexManager implements VikingDbIndexOperations {
    private final VikingdbApi controlPlane;

    public VikingDbIndexManager(VikingdbApi controlPlane) {
        if (controlPlane == null) throw new IllegalArgumentException("controlPlane must not be null");
        this.controlPlane = controlPlane;
    }

    @Override
    public VikingDbIndexInfo getIndex(VikingDbResourceRef target) {
        Objects.requireNonNull(target, "target must not be null");
        try {
            GetVikingdbIndexRequest request = new GetVikingdbIndexRequest()
                    .collectionName(target.collectionName()).indexName(target.indexName());
            if (target.projectName() != null) request.projectName(target.projectName());
            return map(target, controlPlane.getVikingdbIndex(request));
        } catch (ApiException ex) {
            throw new VikingDbVectorStoreException("getIndex", target.collectionName(), ex);
        }
    }

    @Override
    public VikingDbIndexInfo createIndex(VikingDbIndexDefinition definition) {
        if (definition.target() == null) throw new IllegalArgumentException("index target must not be null");
        try {
            controlPlane.createVikingdbIndex(VikingDbIndexRequestMapper.mapCreate(definition));
            return getIndex(definition.target());
        } catch (ApiException ex) {
            throw new VikingDbVectorStoreException("createIndex", definition.target().collectionName(), ex);
        }
    }

    @Override
    public VikingDbIndexInfo updateIndex(VikingDbResourceRef target, VikingDbIndexMutableOptions options) {
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(options, "options must not be null");
        try {
            UpdateVikingdbIndexRequest request = new UpdateVikingdbIndexRequest()
                    .collectionName(target.collectionName()).indexName(target.indexName());
            if (target.projectName() != null) request.projectName(target.projectName());
            if (options.cpuQuota() != null) request.cpuQuota(options.cpuQuota());
            if (options.deletionProtection() != null) request.delProtection(options.deletionProtection());
            if (options.description() != null) request.description(options.description());
            if (options.scalarIndexes() != null) request.scalarIndex(options.scalarIndexes());
            if (options.shardCount() != null) request.shardCount(options.shardCount());
            if (options.shardPolicy() != null)
                request.shardPolicy(UpdateVikingdbIndexRequest.ShardPolicyEnum.fromValue(options.shardPolicy()));
            controlPlane.updateVikingdbIndex(request);
            return getIndex(target);
        } catch (ApiException ex) {
            throw new VikingDbVectorStoreException("updateIndex", target.collectionName(), ex);
        }
    }

    @Override
    public void enableIndex(VikingDbResourceRef target) {
        Objects.requireNonNull(target, "target must not be null");
        try {
            EnableVikingdbIndexRequest request = new EnableVikingdbIndexRequest()
                    .collectionName(target.collectionName()).indexName(target.indexName());
            if (target.projectName() != null) request.projectName(target.projectName());
            controlPlane.enableVikingdbIndex(request);
        } catch (ApiException ex) {
            throw new VikingDbVectorStoreException("enableIndex", target.collectionName(), ex);
        }
    }

    @Override
    public void disableIndex(VikingDbResourceRef target) {
        Objects.requireNonNull(target, "target must not be null");
        try {
            DisableVikingdbIndexRequest request = new DisableVikingdbIndexRequest()
                    .collectionName(target.collectionName()).indexName(target.indexName());
            if (target.projectName() != null) request.projectName(target.projectName());
            controlPlane.disableVikingdbIndex(request);
        } catch (ApiException ex) {
            throw new VikingDbVectorStoreException("disableIndex", target.collectionName(), ex);
        }
    }

    @Override
    public void deleteIndex(VikingDbResourceRef target) {
        Objects.requireNonNull(target, "target must not be null");
        try {
            DeleteVikingdbIndexRequest request = new DeleteVikingdbIndexRequest()
                    .collectionName(target.collectionName()).indexName(target.indexName());
            if (target.projectName() != null) request.projectName(target.projectName());
            controlPlane.deleteVikingdbIndex(request);
        } catch (ApiException ex) {
            throw new VikingDbVectorStoreException("deleteIndex", target.collectionName(), ex);
        }
    }

    @Override
    public VikingDbIndexChangePlan planChange(VikingDbIndexInfo actual, VikingDbIndexDefinition desired) {
        List<String> reasons = new ArrayList<>();
        if (actual == null || desired == null || desired.target() == null) {
            return new VikingDbIndexChangePlan(VikingDbIndexChangeKind.INCOMPATIBLE, List.of("missing index definition"));
        }
        var v = desired.vector();
        if (!same(actual.type(), v.type().name())) reasons.add("index type changed");
        if (!same(actual.distance(), v.distance().name())) reasons.add("distance changed");
        if (!sameQuantization(actual.quantization(), v.quantization())) reasons.add("quantization changed");
        if (!same(actual.hnswM(), v.hnswM())) reasons.add("hnswM changed");
        if (!same(actual.hnswCef(), v.hnswCef())) reasons.add("hnswCef changed");
        if (!same(actual.hnswSef(), v.hnswSef())) reasons.add("hnswSef changed");
        if (!same(actual.diskannM(), v.diskannM())) reasons.add("diskannM changed");
        if (!same(actual.diskannCef(), v.diskannCef())) reasons.add("diskannCef changed");
        if (!same(actual.cacheRatio(), v.cacheRatio())) reasons.add("cacheRatio changed");
        if (!same(actual.pqCodeRatio(), v.pqCodeRatio())) reasons.add("pqCodeRatio changed");
        if (!reasons.isEmpty()) return new VikingDbIndexChangePlan(VikingDbIndexChangeKind.REBUILD_REQUIRED, reasons);
        if (desired.shardCount() != null && !same(actual.shardCount(), desired.shardCount())) {
            return new VikingDbIndexChangePlan(VikingDbIndexChangeKind.ONLINE_UPDATE, List.of("shardCount changed"));
        }
        return new VikingDbIndexChangePlan(VikingDbIndexChangeKind.NOOP, List.of());
    }

    @Override
    public VikingDbIndexInfo awaitState(VikingDbResourceRef target, Set<String> acceptedStates,
                                        Duration timeout, Duration pollInterval) {
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(acceptedStates, "acceptedStates must not be null");
        if (acceptedStates.isEmpty()) throw new IllegalArgumentException("acceptedStates must not be empty");
        if (timeout == null || timeout.isNegative() || timeout.isZero())
            throw new IllegalArgumentException("timeout must be positive");
        if (pollInterval == null || pollInterval.isNegative() || pollInterval.isZero())
            throw new IllegalArgumentException("pollInterval must be positive");
        Instant deadline = Instant.now().plus(timeout);
        while (true) {
            VikingDbIndexInfo current = getIndex(target);
            if (acceptedStates.contains(current.status())) return current;
            if (Instant.now().isAfter(deadline))
                throw new VikingDbVectorStoreException("awaitIndexState", target.collectionName(),
                        new IllegalStateException("Timed out in state " + current.status()));
            try {
                Thread.sleep(Math.min(pollInterval.toMillis(), 30_000L));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new VikingDbVectorStoreException("awaitIndexState", target.collectionName(), ex);
            }
        }
    }

    private static boolean same(Object actual, Object desired) {
        return actual == null ? desired == null : actual.toString().equalsIgnoreCase(String.valueOf(desired));
    }

    private static boolean sameQuantization(String actual, VikingDbIndexVectorOptions.Quantization desired) {
        return actual == null && desired == VikingDbIndexVectorOptions.Quantization.FLOAT
                || same(actual, desired.name());
    }

    private static VikingDbIndexInfo map(VikingDbResourceRef target, GetVikingdbIndexResponse response) {
        var vector = response.getVectorIndex();
        return new VikingDbIndexInfo(target, response.getStatus(),
                vector == null || vector.getIndexType() == null ? null : vector.getIndexType().getValue(),
                vector == null || vector.getDistance() == null ? null : vector.getDistance().getValue(),
                vector == null || vector.getQuant() == null ? null : vector.getQuant().getValue(),
                vector == null ? null : vector.getHnswM(), vector == null ? null : vector.getHnswCef(),
                vector == null ? null : vector.getHnswSef(), vector == null ? null : vector.getDiskannM(),
                vector == null ? null : vector.getDiskannCef(), vector == null ? null : vector.getCacheRatio(),
                vector == null ? null : vector.getPqCodeRatio(), response.getShardCount());
    }
}
