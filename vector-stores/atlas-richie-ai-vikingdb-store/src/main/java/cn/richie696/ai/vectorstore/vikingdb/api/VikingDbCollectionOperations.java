package cn.richie696.ai.vectorstore.vikingdb.api;

import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbCollectionDefinition;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbCollectionInfo;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbResourceRef;

public interface VikingDbCollectionOperations {
    VikingDbCollectionInfo getCollection(VikingDbResourceRef target);

    VikingDbCollectionInfo createCollection(VikingDbCollectionDefinition definition);

    void deleteCollection(VikingDbResourceRef target);
}
