package cn.richie696.ai.vectorstore.vikingdb.api;

import cn.richie696.ai.vectorstore.vikingdb.model.*;

import java.time.Duration;
import java.util.Set;

public interface VikingDbIndexOperations {
    VikingDbIndexInfo getIndex(VikingDbResourceRef target);

    VikingDbIndexInfo createIndex(VikingDbIndexDefinition definition);

    VikingDbIndexInfo updateIndex(VikingDbResourceRef target, VikingDbIndexMutableOptions options);

    void enableIndex(VikingDbResourceRef target);

    void disableIndex(VikingDbResourceRef target);

    void deleteIndex(VikingDbResourceRef target);

    VikingDbIndexChangePlan planChange(VikingDbIndexInfo actual, VikingDbIndexDefinition desired);

    VikingDbIndexInfo awaitState(VikingDbResourceRef target, Set<String> acceptedStates,
                                 Duration timeout, Duration pollInterval);
}
